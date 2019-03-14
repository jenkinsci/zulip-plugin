package jenkins.plugins.zulip;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.util.EncodingUtil;

/**
 * Sends message to Zulip stream
 */
public class Zulip {

    private static final Charset encodingCharset = Charset.forName("UTF-8");

    private String url;
    private String email;
    private String apiKey;
    private static final Logger LOGGER = Logger.getLogger(Zulip.class.getName());

    public Zulip(String url, String email, String apiKey) {
        super();
        if (url != null && url.length() > 0 && !url.endsWith("/") ) {
            url = url + "/";
        }
        this.url = url;
        this.email = email;
        this.apiKey = apiKey;
    }

    /**
     * Configures proxy connection on {@link HttpClient} based on Jenkins settings
     *
     * @param httpClient
     */
    protected void configureProxy(HttpClient httpClient) throws MalformedURLException {
        LOGGER.log(Level.FINE, "Setting up HttpClient proxy");
        ProxyConfiguration proxyConfiguration = Jenkins.getInstance().proxy;
        if (proxyConfiguration != null && ZulipUtil.isValueSet(proxyConfiguration.name)) {
            URL urlObj = new URL(url);
            Proxy proxy = proxyConfiguration.createProxy(urlObj.getHost());
            if (proxy != Proxy.NO_PROXY) {
                // Set proxy on http client
                InetSocketAddress addr = (InetSocketAddress) proxy.address();
                LOGGER.log(Level.FINE, "Using configured Jenkins proxy host: {0}, port: {1}", new Object[] {addr.getHostName(), addr.getPort()} );
                httpClient.getHostConfiguration().setProxy(addr.getHostName(), addr.getPort());
                // Setup user name password credentials
                if (ZulipUtil.isValueSet(proxyConfiguration.getUserName())) {
                    LOGGER.log(Level.FINE, "Using proxy authentication username: {0}, password: ******", proxyConfiguration.getUserName());
                    httpClient.getState().setProxyCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(proxyConfiguration.getUserName(), proxyConfiguration.getPassword()));
                }
            } else {
                LOGGER.log(Level.FINE, "Target url {0} is a no proxy host", url);
            }
        } else {
            LOGGER.fine("Proxy not configured for the Jenkins instance");
        }
    }

    protected HttpClient getClient() throws MalformedURLException {
      HttpClient client = new HttpClient();
      // TODO: It would be nice if this version number read from the Maven XML file
      // (which is possible, but annoying)
      // http://stackoverflow.com/questions/8829147/maven-version-number-in-java-file
      client.getParams().setParameter("http.useragent", "ZulipJenkins/0.1.2");
      configureProxy(client);
      return client;
    }

    protected String getApiEndpoint() {
        if (this.url.length() > 0) {
            return this.url + "api/v1/";
        }
        return "https://api.zulip.com/v1/";
    }

    public String getApiKey() {
      return this.apiKey;
    }

    public String getEmail() {
        return this.email;
    }

    public String post(String method, NameValuePair[] parameters) {
        PostMethod post = new PostMethod(getApiEndpoint() + method);
        post.setRequestHeader("Content-Type", PostMethod.FORM_URL_ENCODED_CONTENT_TYPE);
        String auth_info = this.getEmail() + ":" + this.getApiKey();
        String encoded_auth = new String(Base64.encodeBase64(auth_info.getBytes(encodingCharset)), encodingCharset);
        post.setRequestHeader("Authorization", "Basic " + encoded_auth);

        try {
            post.setRequestBody(EncodingUtil.formUrlEncode(parameters, Charset.defaultCharset().name()));
            HttpClient client = getClient();

            client.executeMethod(post);
            String response = post.getResponseBodyAsString();
            if (post.getStatusCode() != HttpStatus.SC_OK) {
                StringBuilder params = new StringBuilder();
                for (NameValuePair pair: parameters) {
                    params.append("\n").append(pair.getName()).append(":").append(pair.getValue());
                }
                LOGGER.log(Level.SEVERE, "Error sending Zulip message:\n" + response + "\n\n" +
                                         "We sent:" + params.toString());
            }
            return response;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending Zulip message: ", e);
        } finally {
            post.releaseConnection();
        }
        return null;
    }

    public String sendStreamMessage(String stream, String subject, String message) {
        NameValuePair[] body = {new NameValuePair("api-key", this.getApiKey()),
                                new NameValuePair("email",   this.getEmail()),
                                new NameValuePair("type",    "stream"),
                                new NameValuePair("to",      stream),
                                new NameValuePair("subject", subject),
                                new NameValuePair("content", message)};
        return post("messages", body);
    }
}
