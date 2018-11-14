package hudson.plugins.humbug;

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Sends arbitrary message to Zulip stream
 */
public class HumbugSendStep extends Builder implements SimpleBuildStep {

    private static final Logger logger = Logger.getLogger(HumbugSendStep.class.getName());

    private String stream;
    private String topic;
    private String message;

    @DataBoundConstructor
    public HumbugSendStep() {
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        hudson.plugins.humbug.DescriptorImpl globalConfig = Jenkins.getInstance().getDescriptorByType(hudson.plugins.humbug.DescriptorImpl.class);
        Humbug humbug = new Humbug(globalConfig.getUrl(), globalConfig.getEmail(), globalConfig.getApiKey());
        String stream = HumbugUtil.getDefaultValue(getStream(), globalConfig.getStream());
        String topic = HumbugUtil.getDefaultValue(HumbugUtil.getDefaultValue(getTopic(), globalConfig.getTopic()), run.getParent().getDisplayName());
        String expandedMessage = getMessage();
        try {
            expandedMessage = run.getEnvironment(listener).expand(expandedMessage);
        } catch (IOException ex) {
            logger.severe("Failed to expand message variables: " + ex.getMessage());
        }
        humbug.sendStreamMessage(stream, topic, expandedMessage);
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

    public String getMessage() {
        return message;
    }

    @DataBoundSetter
    public void setMessage(String message) {
        this.message = message;
    }

    @Symbol("zulipSend")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Zulip Send";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/humbug/help.html";
        }
    }

}
