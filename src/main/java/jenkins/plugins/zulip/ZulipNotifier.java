package jenkins.plugins.zulip;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.test.AbstractTestResultAction;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Sends build result notification to stream based on the configuration
 */
public class ZulipNotifier extends Publisher implements SimpleBuildStep {

    private static final Logger logger = Logger.getLogger(ZulipNotifier.class.getName());

    private String stream;
    private String topic;
    private String smartNotification;

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public ZulipNotifier() {
    }

    public String getStream() {
        return stream;
    }

    @DataBoundSetter
    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getTopic() {
        return topic;
    }

    @DataBoundSetter
    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSmartNotification() {
        return smartNotification;
    }

    @DataBoundSetter
    public void setSmartNotification(String smartNotification) {
        this.smartNotification = smartNotification;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) throws InterruptedException, IOException {
        return publish(build, listener);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        publish(run, listener);
    }

    private boolean publish(@Nonnull Run<?, ?> build, @Nonnull TaskListener listener) throws InterruptedException {
        if (shouldPublish(build)) {
            String configuredTopic = ZulipUtil.getDefaultValue(topic, DESCRIPTOR.getTopic());
            Result result = getBuildResult(build);
            String changeString = "";
            try {
                changeString = getChangeSet(build);
            } catch (Exception e) {
                logger.log(Level.WARNING,
                        "Exception while computing changes since last build:\n"
                                + ExceptionUtils.getStackTrace(e));
                changeString += "\nError determining changes since last build - please contact support@zulip.com.";
            }
            String resultString = result.toString();
            String message = "";
            // If we are sending to fixed topic, we will want to add project name into the message
            if (ZulipUtil.isValueSet(configuredTopic)) {
                message += hundsonUrlMesssage("Project: ", build.getParent().getDisplayName(), build.getParent().getUrl(), DESCRIPTOR) + " : ";
            }
            message += hundsonUrlMesssage("Build: ", build.getDisplayName(), build.getUrl(), DESCRIPTOR);
            message += ": ";
            message += "**" + resultString + "**";
            if (result == Result.SUCCESS) {
                message += " :check_mark:";
            }
            else if (result == Result.UNSTABLE) {
                message += " :warning:";
                AbstractTestResultAction testResultAction = build.getAction(AbstractTestResultAction.class);
                String failCount = testResultAction != null ? Integer.toString(testResultAction.getFailCount()) : "?";
                message += " (" + failCount + " broken tests)";
            } else {
                message += " :cross_mark:";
            }
            if (changeString.length() > 0) {
                message += "\n\n";
                message += changeString;
            }
            String destinationStream =
                    ZulipUtil.expandVariables(build, listener, ZulipUtil.getDefaultValue(stream, DESCRIPTOR.getStream()));
            String destinationTopic = ZulipUtil.expandVariables(build, listener,
                    ZulipUtil.getDefaultValue(configuredTopic, build.getParent().getDisplayName()));
            Zulip zulip = new Zulip(DESCRIPTOR.getUrl(), DESCRIPTOR.getEmail(), DESCRIPTOR.getApiKey());
            zulip.sendStreamMessage(destinationStream, destinationTopic, message);
        }
        return true;
    }

    private String getChangeSet(Run<?, ?> build) {
        StringBuilder changeString = new StringBuilder();
        RunChangeSetWrapper wrapper = new RunChangeSetWrapper(build);
        if (!wrapper.hasChangeSetComputed()) {
            changeString.append("Could not determine changes since last build.");
        } else if (wrapper.hasChangeSet()) {
            // If there seems to be a commit message at all, try to list all the changes.
            changeString.append("Changes since last build:\n");
            for (ChangeLogSet<? extends ChangeLogSet.Entry> changeLogSet : wrapper.getChangeSets()) {
                for (ChangeLogSet.Entry e : changeLogSet) {
                    String commitMsg = e.getMsg().trim();
                    if (commitMsg.length() > 47) {
                        commitMsg = commitMsg.substring(0, 46) + "...";
                    }
                    String author = e.getAuthor().getDisplayName();
                    changeString.append("\n* `").append(author).append("` ").append(commitMsg);
                }
            }
        }
        return changeString.toString();
    }

    /**
     * Tests if the build should actually published<br/>
     * If SmartNotify is enabled, only notify if:
     * <ol>
     * <li>There was no previous build</li>
     * <li>The current build did not succeed</li>
     * <li>The previous build failed and the current build succeeded.</li>
     * </ol>
     *
     * @return true if build should be published
     */
    private boolean shouldPublish(Run<?, ?> build) {
        if (SmartNotification.isSmartNotifyEnabled(smartNotification, DESCRIPTOR.isSmartNotify())) {
            Run<?, ?> previousBuild = build.getPreviousBuild();
            
            if (previousBuild == null || getBuildResult(build) != getBuildResult(previousBuild)) {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * Helper method to format build parameter as link to Jenkins with prefix
     *
     * @param prefix       The prefix to add to the parameter (will be formatted as bold)
     * @param display      The build parameter
     * @param url          The Url to the Jenkins item
     * @param globalConfig Global step config
     * @return Formatted parameter
     */
    private static String hundsonUrlMesssage(String prefix, String display, String url, DescriptorImpl globalConfig) {
        String message = display;
        String jenkinsUrl = ZulipUtil.getJenkinsUrl(globalConfig);
        if (ZulipUtil.isValueSet(jenkinsUrl)) {
            message = "[" + message + "](" + jenkinsUrl + url + ")";
        }
        message = "**" + prefix + "**" + message;
        return message;
    }

    /**
     * Helper method to get build result from {@link Run}<br/>
     * <i>Since scripted pipeline have no post build concept, the Result variable of successful builds will<br/>
     * not be set yet. In that case we simply assume the build is a success</i>
     *
     * @param build The run to get build result from
     * @return The build result
     */
    private static Result getBuildResult(Run<?, ?> build) {
        return build.getResult() != null ? build.getResult() : Result.SUCCESS;
    }

}
