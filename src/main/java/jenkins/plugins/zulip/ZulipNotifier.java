package jenkins.plugins.zulip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // Read this great article about matching e-mails with regexps.
    // https://fightingforalostcause.net/content/misc/2006/compare-email-regex.php
    // In general, it always depends on what you need and doing it the most generic way is hard.
    // However, for this particular task we want to allow as many email addresses as possible.
    // Even if the address that we match is incorrect we better let it through as we not
    // a Zulip server and it's not our responsibility to validate those addresses.
    // BTW, Zulip uses django.core.validators.validate_email for validating email addresses.
    private static final String NAME_CHAR_GROUP = "[\\w!#$%&'*+/=?^`{|}~-]+";
    public static final Pattern EMAIL_PATTERN =
            Pattern.compile("((<?\\s*)(?<email>" + NAME_CHAR_GROUP + "(?:\\." + NAME_CHAR_GROUP + ")*@" +
                            "(?!-)(?:[\\w-]+\\.)*[\\w-]+)(\\s*>)?)");

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
            List<CommitInfo> changes = null;
            try {
                changes = getChangeSet(build);
                changeString = buildChangeString(changes);
            } catch (Exception e) {
                logger.log(Level.WARNING,
                        "Exception while computing changes since the last build:\n"
                                + ExceptionUtils.getStackTrace(e));
                changeString = "\nError determining changes since the last build - please contact support@zulip.com.";
            }

            StringBuilder message = new StringBuilder();
            // If we are sending to fixed topic, we will want to add project name into the message
            if (ZulipUtil.isValueSet(configuredTopic)) {
                message.append(hundsonUrlMesssage("Project: ", build.getParent().getDisplayName(), build.getParent().getUrl(), DESCRIPTOR));
                message.append(" : ");
            }
            message.append(hundsonUrlMesssage("Build: ", build.getDisplayName(), build.getUrl(), DESCRIPTOR));
            message.append(": **").append(result.toString()).append("**");
            if (result == Result.SUCCESS) {
                message.append(" :check_mark:");
            } else if (result == Result.UNSTABLE) {
                message.append(" :warning:");
                AbstractTestResultAction testResultAction = build.getAction(AbstractTestResultAction.class);
                String failCount = testResultAction != null ? Integer.toString(testResultAction.getFailCount()) : "?";
                message.append(" (").append(failCount).append(" broken tests)");
            } else {
                message.append(" :cross_mark:");
            }

            if (changeString.length() > 0) {
                message.append("\n\n").append(changeString);
            }

            String destinationStream =
                    ZulipUtil.expandVariables(build, listener, ZulipUtil.getDefaultValue(stream, DESCRIPTOR.getStream()));
            String destinationTopic = ZulipUtil.expandVariables(build, listener,
                    ZulipUtil.getDefaultValue(configuredTopic, build.getParent().getDisplayName()));
            Zulip zulip = new Zulip(DESCRIPTOR.getUrl(), DESCRIPTOR.getEmail(), DESCRIPTOR.getApiKey());
            zulip.sendStreamMessage(destinationStream, destinationTopic, message.toString());

            if (DESCRIPTOR.isPersonalNotify() && changes != null && !changes.isEmpty()) {
                Set<String> notifiedAuthors = new HashSet<String>();
                for (CommitInfo change : changes) {
                    if (ZulipUtil.isValueSet(change.email) && !notifiedAuthors.contains(change.email)) {
                        notifiedAuthors.add(change.email);
                        zulip.sendPrivateMessage(change.email, message.toString());
                    }
                }
            }
        }
        return true;
    }

    private String buildChangeString(List<CommitInfo> changes) {
        StringBuilder changeString = new StringBuilder();
        if (changes == null) {
            changeString.append("Could not determine changes since the last build.");
        } else if (!changes.isEmpty()) {
            // If there seems to be a commit message at all, try to list all the changes.
            changeString.append("Changes since the last build:\n");
            for (CommitInfo change : changes) {
                changeString.append("\n* `").append(change.author).append("` ").append(change.message);
            }
        }
        return changeString.toString();
    }

    private List<CommitInfo> getChangeSet(Run<?, ?> build) {
        List<CommitInfo> result = new ArrayList<CommitInfo>();
        RunChangeSetWrapper wrapper = new RunChangeSetWrapper(build);
        if (!wrapper.hasChangeSetComputed()) {
            return null;
        } else if (wrapper.hasChangeSet()) {
            for (ChangeLogSet<? extends ChangeLogSet.Entry> changeLogSet : wrapper.getChangeSets()) {
                for (ChangeLogSet.Entry e : changeLogSet) {
                    String commitMsg = e.getMsg().trim();
                    if (commitMsg.length() > 47) {
                        commitMsg = commitMsg.substring(0, 46) + "...";
                    }
                    String email = getEmail(e);
                    String author = e.getAuthor().getDisplayName();
                    result.add(new CommitInfo(author, email.toLowerCase(), commitMsg));
                }
            }
        }
        return result;
    }

    private static String getEmail(ChangeLogSet.Entry entry) {
        String email = "";
        // GitChangeSet uses email as author's ID.
        if (ZulipUtil.isValueSet(entry.getAuthor().getId())) {
            email = entry.getAuthor().getId();
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        if (!matcher.find()) {
            // If ID doesn't look like an email then reset it.
            email = "";
        }
        // MercurialChangeSet leaves email in the display name.
        matcher = EMAIL_PATTERN.matcher(entry.getAuthor().getDisplayName());
        if (matcher.find()) {
            email = matcher.group("email");
        }
        // Subversion doesn't normally use emails for authors identification, so "email" can be empty.
        return email;
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
