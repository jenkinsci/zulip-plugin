package hudson.plugins.humbug;

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
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Sends build result notification to stream based on the configuration
 */
public class HumbugNotifier extends Publisher implements SimpleBuildStep {

    private static final Logger logger = Logger.getLogger(HumbugNotifier.class.getName());

    private Humbug humbug;
    private String stream;
    private String hudsonUrl;
    private boolean smartNotify;

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public HumbugNotifier() {
        super();
        initialize();
    }

    public HumbugNotifier(String url, String email, String apiKey, String stream, String hudsonUrl, boolean smartNotify) {
        super();
        initialize(url, email, apiKey, stream, hudsonUrl, smartNotify);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) throws InterruptedException, IOException {
        return publish(build);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
        throws InterruptedException, IOException {
    }

    private boolean publish(Run<?, ?> build) {
        if (shouldPublish(build)) {
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
            String message = "Build " + build.getDisplayName();	            message += ": ";
            if (hudsonUrl != null && hudsonUrl.length() > 1) {
                message = "[" + message + "](" + hudsonUrl + build.getUrl() + ")";
            }
            message += ": ";
            message += "**" + resultString + "**";
            if (result == Result.SUCCESS) {
                message += " :check_mark:";
            } else {
                message += " :x:";
            }
            if (changeString.length() > 0) {
                message += "\n\n";
                message += changeString;
            }
            humbug.sendStreamMessage(stream, build.getParent().getDisplayName(), message);
        }
        return true;
    }

    private void initialize()  {
        initialize(DESCRIPTOR.getUrl(), DESCRIPTOR.getEmail(), DESCRIPTOR.getApiKey(), DESCRIPTOR.getStream(), HumbugUtil.getJenkinsUrl(DESCRIPTOR), DESCRIPTOR.isSmartNotify());
    }

    private void initialize(String url, String email, String apiKey, String streamName, String hudsonUrl, boolean smartNotify) {
        humbug = new Humbug(url, email, apiKey);
        this.stream = streamName;
    }

    private String getChangeSet(Run<?, ?> build) {
        String changeString = "";
        RunChangeSetWrapper wrapper = new RunChangeSetWrapper(build);
        if (!wrapper.hasChangeSetComputed()) {
            changeString = "Could not determine changes since last build.";
        } else if (wrapper.hasChangeSet()) {
            // If there seems to be a commit message at all, try to list all the changes.
            changeString = "Changes since last build:\n";
            for (ChangeLogSet<? extends ChangeLogSet.Entry> changeLogSet : wrapper.getChangeSets()) {
                for (ChangeLogSet.Entry e : changeLogSet) {
                    String commitMsg = e.getMsg().trim();
                    if (commitMsg.length() > 47) {
                        commitMsg = commitMsg.substring(0, 46) + "...";
                    }
                    String author = e.getAuthor().getDisplayName();
                    changeString += "\n* `" + author + "` " + commitMsg;
                }
            }
        }
        return changeString;
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
    private static boolean shouldPublish(Run<?, ?> build) {
        if (DESCRIPTOR.isSmartNotify()) {
            Run<?, ?> previousBuild = build.getPreviousBuild();
            if (previousBuild == null ||
                    getBuildResult(build) != Result.SUCCESS ||
                    getBuildResult(previousBuild) != Result.SUCCESS) {
                return true;
            }
        } else {
            return true;
        }
        return false;
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
