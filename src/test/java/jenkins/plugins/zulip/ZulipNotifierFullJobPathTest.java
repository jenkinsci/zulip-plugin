package jenkins.plugins.zulip;

import java.util.Arrays;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZulipNotifierFullJobPathTest {

    @Mock
    private Jenkins jenkins;

    @Mock
    private AbstractBuild build;

    @Mock
    private Job job;

    @Mock
    private ItemAndItemGroup folder;

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

    private MockedConstruction<Zulip> zulipConstruction;
    private MockedStatic<Jenkins> jenkinsStatic;
    private MockedStatic<User> userStatic;
    private MockedStatic<SmartNotification> smartNotificationStatic;

    private static MockedConstruction<DescriptorImpl> descConstruction;
    private static DescriptorImpl descMock;

    @BeforeClass
    public static void setupAll() {
        descConstruction = Mockito.mockConstruction(DescriptorImpl.class, (descMock, context) -> {
            ZulipNotifierFullJobPathTest.descMock = descMock;
            setupDescMock(descMock);
        });
    }

    @AfterClass
    public static void tearDownAll() {
        descConstruction.close();
    }

    private static void setupDescMock(DescriptorImpl descMock) {
        when(descMock.getUrl()).thenReturn("zulipUrl");
        when(descMock.getJenkinsUrl()).thenReturn("");
        when(descMock.getEmail()).thenReturn("jenkins-bot@zulip.com");
        when(descMock.getApiKey()).thenReturn(Mockito.mock(Secret.class));
        when(descMock.getStream()).thenReturn("defaultStream");
        when(descMock.getTopic()).thenReturn("defaultTopic");
        when(descMock.isFullJobPathAsDefaultTopic()).thenReturn(true);
        when(descMock.isFullJobPathInMessage()).thenReturn(true);
        when(descMock.isSmartNotify()).thenReturn(false);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        zulipConstruction = Mockito.mockConstruction(Zulip.class, (zulip, context) -> {
            assertEquals("zulipUrl", context.arguments().get(0));
            assertEquals("jenkins-bot@zulip.com", context.arguments().get(1));
        });

        jenkinsStatic = Mockito.mockStatic(Jenkins.class);
        jenkinsStatic.when(Jenkins::getInstance).thenReturn(jenkins);
        when(jenkins.getDisplayName()).thenReturn("Jenkins");

        userStatic = Mockito.mockStatic(User.class);
        userStatic.when(() -> User.get(anyString())).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                String arg = (String) invocation.getArguments()[0];
                User userMock = Mockito.mock(User.class);
                when(userMock.getDisplayName()).thenReturn(arg);
                return userMock;
            }
        });

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
        when(folder.getParent()).thenReturn((ItemGroup) jenkins);
        when(build.getEnvironment(buildListener)).thenReturn(envVars);
        when(envVars.expand(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return (String) invocation.getArguments()[0];
            }
        });
        smartNotificationStatic = Mockito.mockStatic(SmartNotification.class);
        smartNotificationStatic.when(() -> SmartNotification.isSmartNotifyEnabled(anyString(), anyBoolean()))
                .thenReturn(false);
    }

    @After
    public void tearDown() {
        zulipConstruction.close();
        jenkinsStatic.close();
        userStatic.close();
        smartNotificationStatic.close();

        // Reset descriptor to default after each test
        setupDescMock(descMock);
    }

    @Test
    public void testShouldUseDefaults() throws Exception {
        ZulipNotifier notifier = new ZulipNotifier();
        notifier.perform(build, (Launcher) null, buildListener);
        verify(envVars, times(2)).expand(expandCaptor.capture());
        assertThat("Should expand stream, topic and message", expandCaptor.getAllValues(),
                is(Arrays.asList("defaultStream", "defaultTopic")));
        verify(zulipConstruction.constructed().get(0)).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(),
                messageCaptor.capture());
        assertEquals("Should use default stream", "defaultStream", streamCaptor.getValue());
        assertEquals("Should use default topic", "defaultTopic", topicCaptor.getValue());
        assertEquals("Message should be successful build",
                "**Project: **Folder » TestJob : **Build: **#1: **SUCCESS** :check_mark:", messageCaptor.getValue());
        // Test with blank values
        notifier.setStream("");
        notifier.setTopic("");
        notifier.perform(build, (Launcher) null, buildListener);
        verify(zulipConstruction.constructed().get(1)).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(),
                messageCaptor.capture());
        assertEquals("Should use default stream", "defaultStream", streamCaptor.getValue());
        assertEquals("Should use default topic", "defaultTopic", topicCaptor.getValue());
    }

    @Test
    public void testShouldUseProjectConfig() throws Exception {
        ZulipNotifier notifier = new ZulipNotifier();
        notifier.setStream("projectStream");
        notifier.setTopic("projectTopic");
        notifier.perform(build, (Launcher) null, buildListener);
        verify(zulipConstruction.constructed().get(0)).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(),
                messageCaptor.capture());
        assertEquals("Should use project stream", "projectStream", streamCaptor.getValue());
        assertEquals("Should use topic stream", "projectTopic", topicCaptor.getValue());
    }

    @Test
    public void testShouldUseFullJobPathAsTopic() throws Exception {
        ZulipNotifier notifier = new ZulipNotifier();
        when(descMock.getTopic()).thenReturn("");
        notifier.perform(build, (Launcher) null, buildListener);
        verify(zulipConstruction.constructed().get(0)).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(),
                messageCaptor.capture());
        assertEquals("Topic should be project's full job path", "Folder » TestJob", topicCaptor.getValue());
        assertEquals("Message should not contain project name", "**Build: **#1: **SUCCESS** :check_mark:",
                messageCaptor.getValue());
    }

    @Test
    public void testJenkinsUrl() throws Exception {
        ZulipNotifier notifier = new ZulipNotifier();
        when(descMock.getJenkinsUrl()).thenReturn("JenkinsUrl");
        notifier.perform(build, (Launcher) null, buildListener);
        verify(zulipConstruction.constructed().get(0)).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(),
                messageCaptor.capture());
        assertEquals("Message should contain links to Jenkins",
                "**Project: **[Folder](JenkinsUrl/job/Folder) » [TestJob](JenkinsUrl/job/Folder/TestJob) : **Build: **[#1](JenkinsUrl/job/Folder/TestJob/1): **SUCCESS** :check_mark:",
                messageCaptor.getValue());
    }

}
