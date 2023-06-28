package jenkins.plugins.zulip;

import com.google.common.net.HttpHeaders;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.exact;

public class ZulipTest {

    private static ClientAndServer mockServer;

    @Mock
    private Jenkins jenkins;

    @Mock
    private Secret secret;

    @BeforeClass
    public static void startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(1080);
    }

    @AfterClass
    public static void stopMockServer() {
        mockServer.stop();
    }

    private MockedStatic<Jenkins> jenkinsStatic;
    private MockedStatic<Secret> secretStatic;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        jenkinsStatic = Mockito.mockStatic(Jenkins.class);
        jenkinsStatic.when(Jenkins::get).thenReturn(jenkins);

        secretStatic = Mockito.mockStatic(Secret.class);
        secretStatic.when(() -> Secret.toString(any(Secret.class))).thenReturn("secret");
    }

    @After
    public void tearDown() {
        jenkinsStatic.close();
        secretStatic.close();
    }

    @Test
    public void testSendStreamMessage() throws Exception {
        mockServer.reset();
        mockServer.when(request().withPath("/api/v1/messages")).respond(response().withStatusCode(200));
        Zulip zulip = new Zulip("http://localhost:1080", "jenkins-bot@zulip.com", secret);
        zulip.sendStreamMessage("testStreamůř", "testTopic", "testMessage");
        mockServer.verify(
                request()
                        .withMethod("POST")
                        .withPath("/api/v1/messages")
                        .withBody(exact(
                                "api-key=secret&subject=testTopic&to=testStream%C5%AF%C5%99&type=stream&email=jenkins-bot%40zulip.com&content=testMessage"))
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                        .withHeader(HttpHeaders.AUTHORIZATION, "Basic amVua2lucy1ib3RAenVsaXAuY29tOnNlY3JldA=="));
    }

    @Test
    public void testFailGracefullyOnError() {
        mockServer.reset();
        mockServer.when(request().withPath("/api/v1/messages")).respond(response().withStatusCode(500));
        Zulip zulip = new Zulip("http://localhost:1080", "jenkins-bot@zulip.com", secret);
        // Test that this does not throw exception
        zulip.sendStreamMessage("testStream", "testTopic", "testMessage");
    }

    @Test
    public void testFailGracefullyWhenUnreachable() {
        Zulip zulip = new Zulip("http://localhost:1081", "jenkins-bot@zulip.com", secret);
        // Test that this does not throw exception
        zulip.sendStreamMessage("testStream", "testTopic", "testMessage");
    }

    @Test
    public void testFailGracefullyUnknonwHost() {
        Zulip zulip = new Zulip("http://unreachable:1080", "jenkins-bot@zulip.com", secret);
        // Test that this does not throw exception
        zulip.sendStreamMessage("testStream", "testTopic", "testMessage");
    }

}
