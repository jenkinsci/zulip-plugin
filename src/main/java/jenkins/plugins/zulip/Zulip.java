package jenkins.plugins.zulip;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import hudson.ProxyConfiguration;
import hudson.util.Secret;
import jenkins.model.Jenkins;

/**
 * Sends message to Zulip stream
 */
public class Zulip {

    private static final Charset encodingCharset = StandardCharsets.UTF_8;

    private String url;
    private String email;
    private String apiKey;
    private static final Logger LOGGER = Logger.getLogger(Zulip.class.getName());

    public Zulip(String url, String email, Secret apiKey) {
        super();
        if (url != null && url.length() > 0 && !url.endsWith("/")) {
            url = url + "/";
        }
        this.url = url;
        this.email = email;
        this.apiKey = Secret.toString(apiKey);
    }

    /**
     * Configures proxy connection on {@link HttpClient} based on Jenkins settings
     *
     * @param httpClient
     */
    protected void configureProxy(HttpClient.Builder httpClientBuilder) throws MalformedURLException {
        LOGGER.log(Level.FINE, "Setting up HttpClient proxy");
        ProxyConfiguration proxyConfiguration = Jenkins.getInstance().proxy;

        if (proxyConfiguration != null && ZulipUtil.isValueSet(proxyConfiguration.name)) {
            URL urlObj = new URL(url);
            Proxy proxy = proxyConfiguration.createProxy(urlObj.getHost());

            if (proxy != Proxy.NO_PROXY) {
                // Set proxy on http client
                InetSocketAddress addr = (InetSocketAddress) proxy.address();
                LOGGER.log(Level.FINE, "Using configured Jenkins proxy host: {0}, port: {1}",
                        new Object[] { addr.getHostName(), addr.getPort() });
                httpClientBuilder.proxy(
                        ProxySelector.of(InetSocketAddress.createUnresolved(addr.getHostName(), addr.getPort())));
            } else {
                LOGGER.log(Level.FINE, "Target url {0} is a no proxy host", url);
            }
        } else {
            LOGGER.fine("Proxy not configured for the Jenkins instance");
        }
    }

    protected void configureAuthenticator(HttpClient.Builder httpClientBuilder) {
        httpClientBuilder.authenticator(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                switch (getRequestorType()) {
                    case PROXY:
                        ProxyConfiguration proxyConfiguration = Jenkins.getInstance().proxy;

                        if (ZulipUtil.isValueSet(proxyConfiguration.getUserName())) {
                            LOGGER.log(Level.FINE, "Using proxy authentication username: {0}, password: ******",
                                    proxyConfiguration.getUserName());
                            return new PasswordAuthentication(proxyConfiguration.getUserName(),
                                    Secret.toString(proxyConfiguration.getSecretPassword()).toCharArray());
                        }

                        return null;
                    default:
                        return null;
                }
            }
        });
    }

    protected HttpClient getClient() throws MalformedURLException {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        configureProxy(httpClientBuilder);
        configureAuthenticator(httpClientBuilder);
        return httpClientBuilder.build();
    }

    protected URI getApiEndpoint(String method) {
        StringBuilder uri = new StringBuilder();

        if (this.url.length() > 0) {
            uri.append(this.url);
            uri.append("api/v1/");
        } else {
            uri.append("https://api.zulip.com/v1/");
        }

        uri.append(method);

        return URI.create(uri.toString());
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public String getEmail() {
        return this.email;
    }

    public HttpResponse<String> post(String method, Map<String, String> parameters) {
        try {
            String body = parameters.entrySet()
                    .stream()
                    .map(e -> encodeValue(e))
                    .collect(Collectors.joining("&"));

            String auth_info = this.getEmail() + ":" + this.getApiKey();
            String encoded_auth = new String(Base64.getEncoder().encodeToString(auth_info.getBytes(encodingCharset)));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(getApiEndpoint(method))
                    // TODO: It would be nice if this version number read from the Maven XML file
                    // (which is possible, but annoying)
                    // http://stackoverflow.com/questions/8829147/maven-version-number-in-java-file
                    .header("User-Agent", "ZulipJenkins/0.1.2")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + encoded_auth)
                    .POST(HttpRequest.BodyPublishers.ofString(body, encodingCharset))
                    .build();

            HttpClient client = getClient();

            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                LOGGER.log(Level.SEVERE, "Error sending Zulip message:\n" + httpResponse.body() + "\n\n" +
                        "We sent:" + body);
            }

            return httpResponse;
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error sending Zulip message: ", e);
        }

        return null;
    }

    public HttpResponse<String> sendStreamMessage(String stream, String subject, String message) {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("api-key", this.getApiKey());
        parameters.put("email", this.getEmail());
        parameters.put("type", "stream");
        parameters.put("to", stream);
        parameters.put("subject", subject);
        parameters.put("content", message);

        return post("messages", parameters);
    }

    private String encodeValue(Map.Entry<String, String> value) {
        String toEncode = value.getValue() != null ? value.getValue() : "";
        String encodedValue = URLEncoder.encode(toEncode, encodingCharset);

        return value.getKey() + "=" + encodedValue;
    }
}
