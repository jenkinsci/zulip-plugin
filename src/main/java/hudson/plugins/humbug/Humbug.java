package hudson.plugins.humbug;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.util.EncodingUtil;

/**
 * Sends message to Zulip stream
 */
public class Humbug {
    private String url;
    private String email;
    private String apiKey;
    private static final Logger LOGGER = Logger.getLogger(Humbug.class.getName());

    public Humbug(String url, String email, String apiKey) {
        super();
        if (url != null && url.length() > 0 && !url.endsWith("/") ) {
            url = url + "/";
        }
        this.url = url;
        this.email = email;
        this.apiKey = apiKey;
    }

    protected HttpClient getClient() {
      HttpClient client = new HttpClient();
      // TODO: It would be nice if this version number read from the Maven XML file
      // (which is possible, but annoying)
      // http://stackoverflow.com/questions/8829147/maven-version-number-in-java-file
      client.getParams().setParameter("http.useragent", "ZulipJenkins/0.1.2");
      ProxyConfiguration proxy = Jenkins.getInstance().proxy;
      if (proxy != null) {
          client.getHostConfiguration().setProxy(proxy.name, proxy.port);
      }
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
        String encoded_auth = new String(Base64.encodeBase64(auth_info.getBytes()));
        post.setRequestHeader("Authorization", "Basic " + encoded_auth);

        try {
            post.setRequestBody(EncodingUtil.formUrlEncode(parameters, Charset.defaultCharset().name()));
            HttpClient client = getClient();

            client.executeMethod(post);
            String response = post.getResponseBodyAsString();
            if (post.getStatusCode() != HttpStatus.SC_OK) {
                String params = "";
                for (NameValuePair pair: parameters) {
                    params += "\n" + pair.getName() + ":" + pair.getValue();
                }
                LOGGER.log(Level.SEVERE, "Error sending Zulip message:\n" + response + "\n\n" +
                                         "We sent:" + params);
            }
            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            post.releaseConnection();
        }
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
