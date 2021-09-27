package jenkins.plugins.zulip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.FakeChangeLogSCM;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, User.class, ZulipNotifier.class, DescriptorImpl.class,
        AbstractBuild.class, Job.class, Secret.class, SmartNotification.class})
public class ZulipNotifierFullJobPathTest {

    @Mock
    private Jenkins jenkins;

    @Mock
    private Secret secret;

    @Mock
    private Zulip zulip;

    @Mock
    private DescriptorImpl descMock;

    @Mock
    private AbstractBuild build;

    @Mock
    private Job job;

    @Mock
    private ItemGroup<?> folder;

    @Mock
    private BuildListener buildListener;

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
        PowerMockito.mockStatic(User.class);
        when(User.get(anyString())).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                String arg = (String) invocation.getArguments()[0];
                User userMock = PowerMockito.mock(User.class);
                when(userMock.getDisplayName()).thenReturn(arg);
                return userMock;
            }
        });
        when(descMock.getUrl()).thenReturn("zulipUrl");
        when(descMock.getEmail()).thenReturn("jenkins-bot@zulip.com");
        when(descMock.getApiKey()).thenReturn(secret);
        when(descMock.getStream()).thenReturn("defaultStream");
        when(descMock.getTopic()).thenReturn("defaultTopic");
        when(descMock.isFullJobPathAsDefaultTopic()).thenReturn(true);
        when(descMock.isFullJobPathInMessage()).thenReturn(true);
        PowerMockito.whenNew(DescriptorImpl.class).withAnyArguments().thenReturn(descMock);
        when(build.getParent()).thenReturn(job);
        when(build.getDisplayName()).thenReturn("#1");
        when(build.getUrl()).thenReturn("job/Folder/TestJob/1");
        when(build.hasChangeSetComputed()).thenReturn(true);
        when(build.getChangeSet()).thenReturn(ChangeLogSet.createEmpty((Run<?, ?>) build));
        when(job.getDisplayName()).thenReturn("TestJob");
        when(job.getUrl()).thenReturn("job/Folder/TestJob");
        when(job.getParent()).thenReturn(folder);
        when(folder.getDisplayName()).thenReturn("Folder");
        when(folder.getUrl()).thenReturn("job/Folder");
        when(build.getEnvironment(buildListener)).thenReturn(envVars);
        when(envVars.expand(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return (String) invocation.getArguments()[0];
            }
        });
        PowerMockito.mockStatic(SmartNotification.class);
        when(SmartNotification.isSmartNotifyEnabled(anyString(), anyBoolean())).thenReturn(false);
    }

    @Test
    public void testShouldUseDefaults() throws Exception {
        ZulipNotifier notifier = new ZulipNotifier();
        notifier.perform(build, null, buildListener);
        verifyNew(Zulip.class).withArguments(eq("zulipUrl"), eq("jenkins-bot@zulip.com"), any(Secret.class));
        verify(envVars, times(2)).expand(expandCaptor.capture());
        assertThat("Should expand stream, topic and message", expandCaptor.getAllValues(),
                is(Arrays.asList("defaultStream", "defaultTopic")));
        verify(zulip).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
        assertEquals("Should use default stream", "defaultStream", streamCaptor.getValue());
        assertEquals("Should use default topic", "defaultTopic", topicCaptor.getValue());
        assertEquals("Message should be successful build", "**Project: **Folder » TestJob : **Build: **#1: **SUCCESS** :check_mark:", messageCaptor.getValue());
        // Test with blank values
        reset(zulip);
        notifier.setStream("");
        notifier.setTopic("");
        notifier.perform(build, null, buildListener);
        verify(zulip).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
        assertEquals("Should use default stream", "defaultStream", streamCaptor.getValue());
        assertEquals("Should use default topic", "defaultTopic", topicCaptor.getValue());
    }

    @Test
    public void testShouldUseProjectConfig() throws Exception {
        ZulipNotifier notifier = new ZulipNotifier();
        notifier.setStream("projectStream");
        notifier.setTopic("projectTopic");
        notifier.perform(build, null, buildListener);
        verify(zulip).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
        assertEquals("Should use project stream", "projectStream", streamCaptor.getValue());
        assertEquals("Should use topic stream", "projectTopic", topicCaptor.getValue());
    }

    @Test
    public void testShouldUseFullJobPathAsTopic() throws Exception {
        try {
            ZulipNotifier notifier = new ZulipNotifier();
            when(descMock.getTopic()).thenReturn("");
            Whitebox.setInternalState(ZulipNotifier.class, descMock);
            notifier.perform(build, null, buildListener);
            verify(zulip).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
            assertEquals("Topic should be project's full job path", "Folder » TestJob", topicCaptor.getValue());
            assertEquals("Message should not contain project name", "**Build: **#1: **SUCCESS** :check_mark:", messageCaptor.getValue());
        } finally {
            // Be sure to return global setting back to original setup so other tests dont fail
            when(descMock.getTopic()).thenReturn("defaultTopic");
            Whitebox.setInternalState(ZulipNotifier.class, descMock);
        }
    }

    @Test
    public void testJenkinsUrl() throws Exception {
        try {
            ZulipNotifier notifier = new ZulipNotifier();
            when(descMock.getJenkinsUrl()).thenReturn("JenkinsUrl");
            Whitebox.setInternalState(ZulipNotifier.class, descMock);
            notifier.perform(build, null, buildListener);
            verify(zulip).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
            assertEquals("Message should contain links to Jenkins",
                    "**Project: **[Folder](JenkinsUrl/job/Folder) » [TestJob](JenkinsUrl/job/Folder/TestJob) : **Build: **[#1](JenkinsUrl/job/Folder/TestJob/1): **SUCCESS** :check_mark:",
                    messageCaptor.getValue());
        } finally {
            // Be sure to return global setting back to original setup so other tests dont fail
            when(descMock.getJenkinsUrl()).thenReturn("");
            Whitebox.setInternalState(ZulipNotifier.class, descMock);
        }
    }

}
