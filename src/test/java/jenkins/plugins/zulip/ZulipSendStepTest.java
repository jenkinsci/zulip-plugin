package jenkins.plugins.zulip;

import java.util.Arrays;

import hudson.EnvVars;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZulipSendStepTest {

    @Mock
    private Jenkins jenkins;

    @Mock
    private Secret secret;

    @Mock
    private DescriptorImpl descMock;

    @Mock
    private Run run;

    @Mock
    private Job job;

    @Mock
    private TaskListener taskListener;

    @Mock
    private EnvVars envVars;

    @Captor
    private ArgumentCaptor<String> expandCaptor;

    @Captor
    private ArgumentCaptor<String> streamCaptor;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    private MockedConstruction<Zulip> zulipConstruction;
    private MockedStatic<Jenkins> jenkinsStatic;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        zulipConstruction = Mockito.mockConstruction(Zulip.class, (zulio, context) -> {
            assertEquals("zulipUrl", context.arguments().get(0));
            assertEquals("jenkins-bot@zulip.com", context.arguments().get(1));
        });

        jenkinsStatic = Mockito.mockStatic(Jenkins.class);
        jenkinsStatic.when(Jenkins::get).thenReturn(jenkins);

        when(jenkins.getDescriptorByType(DescriptorImpl.class)).thenReturn(descMock);
        when(descMock.getUrl()).thenReturn("zulipUrl");
        when(descMock.getEmail()).thenReturn("jenkins-bot@zulip.com");
        when(descMock.getApiKey()).thenReturn(secret);
        when(descMock.getStream()).thenReturn("defaultStream");
        when(descMock.getTopic()).thenReturn("defaultTopic");
        when(run.getParent()).thenReturn(job);
        when(job.getDisplayName()).thenReturn("TestJob");
        when(run.getEnvironment(taskListener)).thenReturn(envVars);
        when(envVars.expand(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return (String) invocation.getArguments()[0];
            }
        });
    }

    @After
    public void tearDown() {
        zulipConstruction.close();
        jenkinsStatic.close();
        // descConstruction.close();
    }

    @Test
    public void testShouldUseDefaults() throws Exception {
        ZulipSendStep sendStep = new ZulipSendStep();
        sendStep.setMessage("message");
        sendStep.perform(run, null, null, taskListener);

        verify(envVars, times(3)).expand(expandCaptor.capture());
        assertThat("Should expand stream, topic and message", expandCaptor.getAllValues(),
                is(Arrays.asList("defaultStream", "defaultTopic", "message")));
        verify(zulipConstruction.constructed().get(0)).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(),
                messageCaptor.capture());
        assertEquals("Should be default stream", "defaultStream", streamCaptor.getValue());
        assertEquals("Should be default topic", "defaultTopic", topicCaptor.getValue());
        assertEquals("message", messageCaptor.getValue());
        //
        sendStep.setStream("");
        sendStep.setTopic("");
        sendStep.perform(run, null, null, taskListener);
        verify(zulipConstruction.constructed().get(1)).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(),
                messageCaptor.capture());
        assertEquals("Should be default stream", "defaultStream", streamCaptor.getValue());
        assertEquals("Should be default topic", "defaultTopic", topicCaptor.getValue());
    }

    @Test
    public void testShouldUseProjectConfig() throws Exception {
        ZulipSendStep sendStep = new ZulipSendStep();
        sendStep.setStream("projectStream");
        sendStep.setTopic("projectTopic");
        sendStep.perform(run, null, null, taskListener);
        verify(zulipConstruction.constructed().get(0)).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(),
                messageCaptor.capture());
        assertEquals("Should be project stream", "projectStream", streamCaptor.getValue());
        assertEquals("Should be project topic", "projectTopic", topicCaptor.getValue());
        assertNull("Should be null message", messageCaptor.getValue());
    }

    @Test
    public void testShouldUseProjectNameAsTopic() throws Exception {
        ZulipSendStep sendStep = new ZulipSendStep();
        // Override default topic config
        when(descMock.getTopic()).thenReturn("");
        sendStep.perform(run, null, null, taskListener);
        verify(zulipConstruction.constructed().get(0)).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(),
                messageCaptor.capture());
        assertEquals("Topic should be project display name", "TestJob", topicCaptor.getValue());
    }

}
