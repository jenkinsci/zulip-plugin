package hudson.plugins.humbug;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.verify.VerificationTimes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class IntegrationTest {

    private static ClientAndServer mockServer;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @BeforeClass
    public static void startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(1080);
    }

    @AfterClass
    public static void stopMockServer() {
        mockServer.stop();
    }

    @Before
    public void setUp() throws Exception {
        j.getInstance().getDescriptorByType(DescriptorImpl.class).setUrl("http://localhost:1080");
        mockServer.reset();
        mockServer.when(request().withPath("/api/v1/messages")).respond(response().withStatusCode(200));
    }

    @Test
    public void testGlobalConfig() throws Exception {
        HtmlPage p = j.createWebClient().goTo("configure");
        HtmlForm f = p.getFormByName("config");
        f.getInputByName("humbugUrl").setValueAttribute("ZulipUrl");
        f.getInputByName("humbugEmail").setValueAttribute("jenkins-bot@zulip.com");
        f.getInputByName("humbugApiKey").setValueAttribute("secret");
        f.getInputByName("humbugStream").setValueAttribute("defaultStream");
        f.getInputByName("humbugTopic").setValueAttribute("defaultTopic");
        f.getInputByName("humbugSmartNotify").setChecked(true);
        f.getInputByName("humbugHudsonUrl").setValueAttribute("HudsonUrl");
        j.submit(f);
        verifyGlobalConfig();
        // Do a round-trip to verify settings load & save correctly
        p = j.createWebClient().goTo("configure");
        f = p.getFormByName("config");
        j.submit(f);
        verifyGlobalConfig();
    }

    @Test
    public void testFreestyle() throws Exception {
        // Initialize project with send and notification step
        FreeStyleProject project = j.createFreeStyleProject();
        HumbugSendStep sendStep = new HumbugSendStep();
        sendStep.setStream("testStream");
        sendStep.setTopic("testTopic");
        sendStep.setMessage("testMessage");
        project.getBuildersList().add(sendStep);
        HumbugNotifier notifier = new HumbugNotifier();
        notifier.setStream("testStream");
        notifier.setTopic("testTopic");
        project.getPublishersList().add(notifier);
        // Do a round-trip to verify project settings load & save correctly
        JenkinsRule.WebClient webClient = j.createWebClient();
        HtmlPage p = webClient.getPage(project, "configure");
        HtmlForm f = p.getFormByName("config");
        j.submit(f);
        HumbugSendStep assertStep = project.getBuildersList().get(HumbugSendStep.class);
        assertEquals("testStream", assertStep.getStream());
        assertEquals("testTopic", assertStep.getTopic());
        assertEquals("testMessage", assertStep.getMessage());
        HumbugNotifier assertNotifier = project.getPublishersList().get(HumbugNotifier.class);
        assertEquals("testStream", assertNotifier.getStream());
        assertEquals("testTopic", assertNotifier.getTopic());
        // Perform build and verify it's successful
        j.buildAndAssertSuccess(project);
        verifyNotificationsSent(2);
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "step([$class: 'HumbugSendStep', stream: 'someStream', topic: 'someTopic', message: 'Hello Zulip'])\n" +
                        "step([$class: 'HumbugNotifier', stream: 'someStream', topic: 'someTopic'])\n" +
                        "}", true));
        j.buildAndAssertSuccess(project);
        verifyNotificationsSent(2);
    }

    private void verifyGlobalConfig() {
        DescriptorImpl globalConfig = j.jenkins.getDescriptorByType(DescriptorImpl.class);
        assertEquals("ZulipUrl", globalConfig.getUrl());
        assertEquals("jenkins-bot@zulip.com", globalConfig.getEmail());
        assertEquals("secret", globalConfig.getApiKey());
        assertEquals("defaultStream", globalConfig.getStream());
        assertEquals("defaultTopic", globalConfig.getTopic());
        assertTrue(globalConfig.isSmartNotify());
        assertEquals("HudsonUrl", globalConfig.getHudsonUrl());
    }

    private void verifyNotificationsSent(int count) {
        mockServer.verify(request().withPath("/api/v1/messages"), VerificationTimes.exactly(count));
    }

}
