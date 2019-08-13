package jenkins.plugins.zulip;

import java.util.Arrays;

import hudson.EnvVars;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, ZulipSendStep.class})
public class ZulipSendStepTest {

    @Mock
    private Jenkins jenkins;

    @Mock
    private Zulip zulip;

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

    @Before
    public void setUp() throws Exception {
        PowerMockito.whenNew(Zulip.class).withAnyArguments().thenReturn(zulip);
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);
        when(jenkins.getDescriptorByType(DescriptorImpl.class)).thenReturn(descMock);
        when(descMock.getUrl()).thenReturn("zulipUrl");
        when(descMock.getEmail()).thenReturn("jenkins-bot@zulip.com");
        when(descMock.getApiKey()).thenReturn("secret");
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

    @Test
    public void testShouldUseDefaults() throws Exception {
        ZulipSendStep sendStep = new ZulipSendStep();
        sendStep.setMessage("message");
        sendStep.perform(run, null, null, taskListener);
        verifyNew(Zulip.class).withArguments("zulipUrl", "jenkins-bot@zulip.com", "secret");
        verify(envVars, times(3)).expand(expandCaptor.capture());
        assertThat("Should expand stream, topic and message", expandCaptor.getAllValues(),
                is(Arrays.asList("defaultStream", "defaultTopic", "message")));
        verify(zulip).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
        assertEquals("Should be default stream", "defaultStream", streamCaptor.getValue());
        assertEquals("Should be default topic", "defaultTopic", topicCaptor.getValue());
        assertEquals("message", messageCaptor.getValue());
        //
        reset(zulip);
        sendStep.setStream("");
        sendStep.setTopic("");
        sendStep.perform(run, null, null, taskListener);
        verify(zulip).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
        assertEquals("Should be default stream", "defaultStream", streamCaptor.getValue());
        assertEquals("Should be default topic", "defaultTopic", topicCaptor.getValue());
    }

    @Test
    public void testShouldUseProjectConfig() throws Exception {
        ZulipSendStep sendStep = new ZulipSendStep();
        sendStep.setStream("projectStream");
        sendStep.setTopic("projectTopic");
        sendStep.perform(run, null, null, taskListener);
        verify(zulip).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
        assertEquals("Should be project stream", "projectStream", streamCaptor.getValue());
        assertEquals("Should be project topic", "projectTopic", topicCaptor.getValue());
        assertNull("Should be null message", messageCaptor.getValue());
    }

    public void testShouldUseProjectNameAsTopic() throws Exception {
        ZulipSendStep sendStep = new ZulipSendStep();
        // Override default topic config
        when(descMock.getTopic()).thenReturn("");
        sendStep.perform(run, null, null, taskListener);
        verify(zulip).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
        assertEquals("Topic should be project display name", "TestJob", topicCaptor.getValue());
    }

}
