package jenkins.plugins.zulip;

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
public class ZulipSendStep extends Builder implements SimpleBuildStep {

    private static final Logger logger = Logger.getLogger(ZulipSendStep.class.getName());

    private String stream;
    private String topic;
    private String message;

    @DataBoundConstructor
    public ZulipSendStep() {
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        jenkins.plugins.zulip.DescriptorImpl globalConfig = Jenkins.getInstance().getDescriptorByType(jenkins.plugins.zulip.DescriptorImpl.class);
        Zulip zulip = new Zulip(globalConfig.getUrl(), globalConfig.getEmail(), globalConfig.getApiKey());
        String stream = ZulipUtil.getDefaultValue(getStream(), globalConfig.getStream());
        String topic = ZulipUtil.getDefaultValue(ZulipUtil.getDefaultValue(getTopic(), globalConfig.getTopic()), run.getParent().getDisplayName());
        String expandedMessage = getMessage();
        try {
            expandedMessage = run.getEnvironment(listener).expand(expandedMessage);
        } catch (IOException ex) {
            logger.severe("Failed to expand message variables: " + ex.getMessage());
        }
        zulip.sendStreamMessage(stream, topic, expandedMessage);
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
            return "/plugin/zulip/help.html";
        }
    }

}
