package hudson.plugins.humbug;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import hudson.model.Hudson;
import hudson.ProxyConfiguration;

public class Humbug {
    private String email;
    private String apiKey;
    private String subdomain;

    public Humbug(String email, String apiKey, String subdomain) {
        super();
        this.email = email;
        this.apiKey = apiKey;
        this.subdomain = subdomain;
    }

    protected HttpClient getClient() {
      HttpClient client = new HttpClient();
      Credentials defaultcreds = new UsernamePasswordCredentials(this.apiKey, "x");
      client.getState().setCredentials(new AuthScope(getHost(), -1, AuthScope.ANY_REALM), defaultcreds);
      client.getParams().setAuthenticationPreemptive(true);
      client.getParams().setParameter("http.useragent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_4; en-us) AppleWebKit/533.16 (KHTML, like Gecko) Version/5.0 Safari/533.16");
      ProxyConfiguration proxy = Hudson.getInstance().proxy;
      if (proxy != null) {
          client.getHostConfiguration().setProxy(proxy.name, proxy.port);
      }
      return client;
    }

    protected String getHost() {
      return this.subdomain + ".humbughq.com";
    }

    public String getSubdomain() {
      return this.subdomain;
    }

    public String getApiKey() {
      return this.apiKey;
    }

    public String getEmail() {
        return this.email;
      }


    public int post(String url, String body) {
        PostMethod post = new PostMethod("https://" + getHost() + "/" + url);
        post.setRequestHeader("Content-Type", "application/xml");
        try {
            post.setRequestEntity(new StringRequestEntity(body, "application/xml", "UTF8"));
            return getClient().executeMethod(post);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            post.releaseConnection();
        }
    }

    public String get(String url) {
        GetMethod get = new GetMethod("https://" + getHost() + "/" + url);
        get.setFollowRedirects(true);
        get.setRequestHeader("Content-Type", "application/xml");
        try {
            getClient().executeMethod(get);
            verify(get.getStatusCode());
            return get.getResponseBodyAsString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            get.releaseConnection();
        }
    }

    public boolean verify(int returnCode) {
        if (returnCode != 200) {
            throw new RuntimeException("Unexpected response code: " + Integer.toString(returnCode));
        }
        return true;
    }

    public boolean sendStreamMessage(String stream, String subject, String message) {
        // TODO
        return true;
    }
}
