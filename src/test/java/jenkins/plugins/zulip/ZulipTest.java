package jenkins.plugins.zulip;

import java.net.URLEncoder;
import java.nio.charset.Charset;

import com.google.common.net.HttpHeaders;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.NameValuePair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Body;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.exact;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Jenkins.class)
public class ZulipTest {

    private static ClientAndServer mockServer;

    @Mock
    private Jenkins jenkins;

    @BeforeClass
    public static void startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(1080);
    }

    @AfterClass
    public static void stopMockServer() {
        mockServer.stop();
    }

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);
    }

    @Test
    public void testSendStreamMessage() throws Exception {
        mockServer.reset();
        mockServer.when(request().withPath("/api/v1/messages")).respond(response().withStatusCode(200));
        Zulip zulip = new Zulip("http://localhost:1080", "jenkins-bot@zulip.com", "secret");
        zulip.sendStreamMessage("testStreamůř", "testTopic", "testMessage");
        mockServer.verify(
                request()
                        .withMethod("POST")
                        .withPath("/api/v1/messages")
                        .withBody(
                                buildBody(
                                        new NameValuePair("api-key", "secret"),
                                        new NameValuePair("email", "jenkins-bot@zulip.com"),
                                        new NameValuePair("type", "stream"),
                                        new NameValuePair("to", "testStreamůř"),
                                        new NameValuePair("subject", "testTopic"),
                                        new NameValuePair("content", "testMessage")
                                )

                        )
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                        .withHeader(HttpHeaders.AUTHORIZATION, "Basic amVua2lucy1ib3RAenVsaXAuY29tOnNlY3JldA==")
        );
    }

    @Test
    public void testFailGracefullyOnError() {
        mockServer.reset();
        mockServer.when(request().withPath("/api/v1/messages")).respond(response().withStatusCode(500));
        Zulip zulip = new Zulip("http://localhost:1080", "jenkins-bot@zulip.com", "secret");
        // Test that this does not throw exception
        zulip.sendStreamMessage("testStream", "testTopic", "testMessage");
    }

    @Test
    public void testFailGracefullyWhenUnreachable() {
        Zulip zulip = new Zulip("http://localhost:1081", "jenkins-bot@zulip.com", "secret");
        // Test that this does not throw exception
        zulip.sendStreamMessage("testStream", "testTopic", "testMessage");
    }

    @Test
    public void testFailGracefullyUnknonwHost() {
        Zulip zulip = new Zulip("http://unreachable:1080", "jenkins-bot@zulip.com", "secret");
        // Test that this does not throw exception
        zulip.sendStreamMessage("testStream", "testTopic", "testMessage");
    }

    private Body buildBody(NameValuePair... valuePairs) throws Exception {
        StringBuilder text = new StringBuilder();
        for (NameValuePair pair : valuePairs) {
            if (text.length() != 0) {
                text.append("&");
            }
            text.append(pair.getName()).append("=").append(URLEncoder.encode(pair.getValue(), Charset.defaultCharset().name()));
        }
        return exact(text.toString());
    }

}
