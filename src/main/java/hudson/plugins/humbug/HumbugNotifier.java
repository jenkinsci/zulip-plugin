package hudson.plugins.humbug;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Sends build result notification to stream based on the configuration
 */
public class HumbugNotifier extends Notifier {

    private Humbug humbug;
    private String stream;
    private String hudsonUrl;
    private boolean smartNotify;

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private static final Logger LOGGER = Logger.getLogger(HumbugNotifier.class.getName());

    public HumbugNotifier() {
        super();
        initialize();
    }

    public HumbugNotifier(String url, String email, String apiKey, String stream, String hudsonUrl, boolean smartNotify) {
        super();
        initialize(url, email, apiKey, stream, hudsonUrl, smartNotify);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    private void publish(AbstractBuild<?, ?> build) throws IOException {
        // We call this every time in case our settings have changed
        // between the last time this was run and now.
        initialize();
        Result result = build.getResult();
        String changeString = "";
        try {
            if (!build.hasChangeSetComputed()) {
                changeString = "Could not determine changes since last build.";
            } else if (build.getChangeSet().iterator().hasNext()) {
                if (!build.getChangeSet().isEmptySet()) {
                    // If there seems to be a commit message at all, try to list all the changes.
                    changeString = "Changes since last build:\n";
                    for (ChangeLogSet.Entry e: build.getChangeSet()) {
                        String commitMsg = e.getMsg().trim();
                        if (commitMsg.length() > 47) {
                            commitMsg = commitMsg.substring(0, 46)  + "...";
                        }
                        String author = e.getAuthor().getDisplayName();
                        changeString += "\n* `"+ author + "` " + commitMsg;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                      "Exception while computing changes since last build:\n"
                       + ExceptionUtils.getStackTrace(e));
            changeString += "\nError determining changes since last build - please contact support@zulip.com.";
        }
        String resultString = result.toString();
        String message = "Build " + build.getDisplayName();
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
        if (changeString.length() > 0 ) {
            message += "\n\n";
            message += changeString;
        }
        humbug.sendStreamMessage(stream, build.getProject().getName(), message);
    }

    private void initialize()  {
        initialize(DESCRIPTOR.getUrl(), DESCRIPTOR.getEmail(), DESCRIPTOR.getApiKey(), DESCRIPTOR.getStream(), DESCRIPTOR.getHudsonUrl(), DESCRIPTOR.isSmartNotify());
    }

    private void initialize(String url, String email, String apiKey, String streamName, String hudsonUrl, boolean smartNotify) {
        humbug = new Humbug(url, email, apiKey);
        this.stream = streamName;
        if (hudsonUrl.length() > 0 && !hudsonUrl.endsWith("/") ) {
            hudsonUrl = hudsonUrl + "/";
        }
        this.hudsonUrl = hudsonUrl;
        this.smartNotify = smartNotify;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        // If SmartNotify is enabled, only notify if:
        //  (1) there was no previous build, or
        //  (2) the current build did not succeed, or
        //  (3) the previous build failed and the current build succeeded.
        smartNotify = DESCRIPTOR.isSmartNotify();
        if (smartNotify) {
            AbstractBuild previousBuild = build.getPreviousBuild();
            if (previousBuild == null ||
                build.getResult() != Result.SUCCESS ||
                previousBuild.getResult() != Result.SUCCESS)
            {
                publish(build);
            }
        } else {
            publish(build);
        }
        return true;
    }

}
