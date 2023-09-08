package jenkins.plugins.zulip;

import com.google.common.net.HttpHeaders;

import hudson.ProxyConfiguration;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.NottableString;
import org.mockserver.verify.VerificationTimes;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.exact;

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

    private MockedStatic<Jenkins> jenkinsStatic;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        jenkinsStatic = Mockito.mockStatic(Jenkins.class);
        jenkinsStatic.when(Jenkins::get).thenReturn(jenkins);
    }

    @After
    public void tearDown() {
        jenkinsStatic.close();
        mockServer.reset();
        jenkins.proxy = null;
    }

    @Test
    public void testSendStreamMessage() throws Exception {
        mockServer.when(request().withPath("/api/v1/messages")).respond(response().withStatusCode(200));
        Zulip zulip = new Zulip("http://localhost:1080", "jenkins-bot@zulip.com", Secret.fromString("secret"));
        zulip.sendStreamMessage("testStreamůř", "testTopic", "testMessage");
        mockServer.verify(
                request()
                        .withMethod("POST")
                        .withPath("/api/v1/messages")
                        .withBody(exact(
                                "api-key=secret&subject=testTopic&to=testStream%C5%AF%C5%99&type=stream&email=jenkins-bot%40zulip.com&content=testMessage"))
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"));
    }

    @Test
    public void testFailGracefullyOnError() {
        mockServer.when(request().withPath("/api/v1/messages")).respond(response().withStatusCode(500));
        Zulip zulip = new Zulip("http://localhost:1080", "jenkins-bot@zulip.com", Secret.fromString("secret"));
        // Test that this does not throw exception
        zulip.sendStreamMessage("testStream", "testTopic", "testMessage");
    }

    @Test
    public void testFailGracefullyWhenUnreachable() {
        Zulip zulip = new Zulip("http://localhost:1081", "jenkins-bot@zulip.com", Secret.fromString("secret"));
        // Test that this does not throw exception
        zulip.sendStreamMessage("testStream", "testTopic", "testMessage");
    }

    @Test
    public void testFailGracefullyUnknonwHost() {
        Zulip zulip = new Zulip("http://unreachable:1080", "jenkins-bot@zulip.com", Secret.fromString("secret"));
        // Test that this does not throw exception
        zulip.sendStreamMessage("testStream", "testTopic", "testMessage");
    }

    @Test
    public void testSendsAuthorizationWhenRequested() {
        mockServer.when(request().withPath("/api/v1/messages").withHeader(NottableString.not("Authorization")))
                .respond(response().withStatusCode(401).withHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic"));
        mockServer.when(request().withPath("/api/v1/messages"))
                .respond(response().withStatusCode(200));

        Zulip zulip = new Zulip("http://localhost:1080", "jenkins-bot@zulip.com", Secret.fromString("secret"));
        zulip.sendStreamMessage("testStream", "testTopic", "testMessage");

        mockServer.verify(request(), VerificationTimes.exactly(2));
        mockServer.verify(
                request().withHeader(HttpHeaders.AUTHORIZATION, "Basic amVua2lucy1ib3RAenVsaXAuY29tOnNlY3JldA=="),
                VerificationTimes.once());
    }

    @Test
    public void testSendsProxyAuthorizationWhenRequested() {
        jenkins.proxy = new ProxyConfiguration("localhost", 1080, "proxy-user", "proxy-password");

        mockServer
                .when(request().withPath("/api/v1/messages")
                        .withHeader(NottableString.not(HttpHeaders.PROXY_AUTHORIZATION)))
                .respond(response().withStatusCode(407).withHeader(HttpHeaders.PROXY_AUTHENTICATE, "Basic"));
        mockServer.when(request().withPath("/api/v1/messages")).respond(response().withStatusCode(200));

        Zulip zulip = new Zulip("http://localhost:5000", "jenkins-bot@zulip.com", Secret.fromString("secret"));
        zulip.sendStreamMessage("testStream", "testTopic", "testMessage");

        mockServer.verify(request(), VerificationTimes.exactly(2));
        mockServer.verify(
                request().withHeader(HttpHeaders.PROXY_AUTHORIZATION, "Basic cHJveHktdXNlcjpwcm94eS1wYXNzd29yZA=="),
                VerificationTimes.once());
    }

}
