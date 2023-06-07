package jenkins.plugins.zulip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jvnet.hudson.test.FakeChangeLogSCM;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZulipNotifierTest {

    private static final int TOTAL_TEST_COUNT = 100;
    private static final int FAILED_TEST_COUNT = 50;

    @Mock
    private Jenkins jenkins;

    @Mock
    private AbstractBuild build;

    @Mock
    private AbstractBuild previousBuild;

    @Mock
    private Job job;

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
    private static MockedConstruction<DescriptorImpl> descConstruction;
    private static DescriptorImpl descMock;

    @BeforeClass
    public static void setupAll() {
        descConstruction = Mockito.mockConstruction(DescriptorImpl.class, (descMock, context) -> {
            ZulipNotifierTest.descMock = descMock;

            setupDescMock(descMock);
        });
    }

    @AfterClass
    public static void tearDownAll() {
        descConstruction.close();
    }

    // Default descriptor setup used for all tests
    private static void setupDescMock(DescriptorImpl descMock) {
        when(descMock.getUrl()).thenReturn("zulipUrl");
        when(descMock.getJenkinsUrl()).thenReturn("");
        when(descMock.getEmail()).thenReturn("jenkins-bot@zulip.com");
        when(descMock.getApiKey()).thenReturn(Mockito.mock(Secret.class));
        when(descMock.getStream()).thenReturn("defaultStream");
        when(descMock.getTopic()).thenReturn("defaultTopic");
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
        when(build.getUrl()).thenReturn("job/TestJob/1");
        when(build.hasChangeSetComputed()).thenReturn(true);
        when(build.getChangeSet()).thenReturn(ChangeLogSet.createEmpty((Run<?, ?>) build));
        when(job.getDisplayName()).thenReturn("TestJob");
        when(job.getUrl()).thenReturn("job/TestJob");
        when(build.getEnvironment(buildListener)).thenReturn(envVars);
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
        userStatic.close();

        // Reset descriptor to initial state
        setupDescMock(this.descMock);
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
                "**Project: **TestJob : **Build: **#1: **SUCCESS** :check_mark:", messageCaptor.getValue());
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
    public void testFailedBuild() throws Exception {
        ZulipNotifier notifier = new ZulipNotifier();
        when(build.getResult()).thenReturn(Result.FAILURE);
        notifier.perform(build, (Launcher) null, buildListener);
        verify(zulipConstruction.constructed().get(0)).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(),
                messageCaptor.capture());
        assertEquals("Message should be failed build", "**Project: **TestJob : **Build: **#1: **FAILURE** :cross_mark:",
                messageCaptor.getValue());
    }

    @Test
    public void testUnstableBuild() throws Exception {
        ZulipNotifier notifier = new ZulipNotifier();
        when(build.getResult()).thenReturn(Result.UNSTABLE);
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(new FakeTestResultAction());
        notifier.perform(build, (Launcher) null, buildListener);
        verify(zulipConstruction.constructed().get(0)).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(),
                messageCaptor.capture());
        assertEquals("Message should be unstable build",
                "**Project: **TestJob : **Build: **#1: **UNSTABLE** :warning: (50 broken tests)",
                messageCaptor.getValue());
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
    public void testShouldUseProjectNameAsTopic() throws Exception {
        ZulipNotifier notifier = new ZulipNotifier();
        when(descMock.getTopic()).thenReturn("");
        notifier.perform(build, (Launcher) null, buildListener);
        verify(zulipConstruction.constructed().get(0)).sendStreamMessage(streamCaptor.capture(),
                topicCaptor.capture(), messageCaptor.capture());
        assertEquals("Topic should be project display name", "TestJob", topicCaptor.getValue());
        assertEquals("Message should not contain project name", "**Build: **#1: **SUCCESS** :check_mark:",
                messageCaptor.getValue());
    }

    @Test
    public void testChangeLogSet() throws Exception {
        List<FakeChangeLogSCM.EntryImpl> changes = new ArrayList<>();
        changes.add(createChange("Author 1", "Short Commit Msg"));
        changes.add(createChange("Author 2",
                "This is a very long commit message that will get truncated in the Zulip message"));
        FakeChangeLogSCM.FakeChangeLogSet changeLogSet = new FakeChangeLogSCM.FakeChangeLogSet(build, changes);
        when(build.getChangeSet()).thenReturn(changeLogSet);
        ZulipNotifier notifier = new ZulipNotifier();
        notifier.perform(build, (Launcher) null, buildListener);
        verify(zulipConstruction.constructed().get(0)).sendStreamMessage(streamCaptor.capture(), topicCaptor.capture(),
                messageCaptor.capture());
        assertEquals("Message should contain change log",
                "**Project: **TestJob : **Build: **#1: **SUCCESS** :check_mark:\n" +
                        "\n" +
                        "Changes since last build:\n" +
                        "\n" +
                        "* `Author 1` Short Commit Msg\n" +
                        "* `Author 2` This is a very long commit message that will g...",
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
                "**Project: **[TestJob](JenkinsUrl/job/TestJob) : **Build: **[#1](JenkinsUrl/job/TestJob/1): **SUCCESS** :check_mark:",
                messageCaptor.getValue());
    }

    @Test
    public void testSmartNotify() throws Exception {
        ZulipNotifier notifier = new ZulipNotifier();
        when(descMock.isSmartNotify()).thenReturn(true);
        // If there was no previous build, notification should be sent no matter the
        // result
        when(build.getPreviousBuild()).thenReturn(null);
        when(build.getResult()).thenReturn(Result.SUCCESS);
        notifier.perform(build, (Launcher) null, buildListener);
        when(build.getResult()).thenReturn(Result.FAILURE);
        notifier.perform(build, (Launcher) null, buildListener);
        assertEquals(2, zulipConstruction.constructed().size());

        // If the previous build was a failure, notification should be sent no matter
        // what
        when(build.getPreviousBuild()).thenReturn(previousBuild);
        when(previousBuild.getResult()).thenReturn(Result.FAILURE);
        when(build.getResult()).thenReturn(Result.SUCCESS);
        notifier.perform(build, (Launcher) null, buildListener);
        when(build.getResult()).thenReturn(Result.FAILURE);
        notifier.perform(build, (Launcher) null, buildListener);
        assertEquals(4, zulipConstruction.constructed().size());

        // If the previous build was a success, notification should be sent only for
        // failed builds
        when(build.getPreviousBuild()).thenReturn(previousBuild);
        when(previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(build.getResult()).thenReturn(Result.SUCCESS);
        notifier.perform(build, (Launcher) null, buildListener);
        when(build.getResult()).thenReturn(Result.FAILURE);
        notifier.perform(build, (Launcher) null, buildListener);
        assertEquals(5, zulipConstruction.constructed().size());
    }

    private FakeChangeLogSCM.EntryImpl createChange(String author, String msg) {
        return new FakeChangeLogSCM.EntryImpl().withAuthor(author).withMsg(msg);
    }

    private class FakeTestResultAction extends AbstractTestResultAction {
        @Override
        public int getFailCount() {
            return FAILED_TEST_COUNT;
        }

        @Override
        public int getTotalCount() {
            return TOTAL_TEST_COUNT;
        }

        @Override
        public Object getResult() {
            return null;
        }
    }

}
