package jenkins.plugins.zulip;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.Secret;
import hudson.util.XStream2;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Descriptor for {@link ZulipNotifier}
 */
@Symbol("zulipNotification")
public class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    private static final Logger logger = Logger.getLogger(DescriptorImpl.class.getName());

    private static final String OLD_CONFIG_FILE_NAME = "hudson.plugins.humbug.HumbugNotifier.xml";

    private String url;
    private String email;
    private Secret apiKey;
    private String stream;
    private String topic;
    private boolean fullJobPathAsDefaultTopic;
    private boolean fullJobPathInMessage;
    private transient String hudsonUrl; // backwards compatibility
    private String jenkinsUrl;
    private boolean smartNotify;

    public DescriptorImpl() {
        super(ZulipNotifier.class);
        XmlFile newConfig = getConfigFile();
        if (newConfig.exists()) {
            load();
        } else {
            XStream2 xstream = new XStream2();
            xstream.alias("hudson.plugins.humbug.DescriptorImpl", DescriptorImpl.class);
            XmlFile oldConfig = new XmlFile(xstream, new File(Jenkins.getInstance().getRootDir(), OLD_CONFIG_FILE_NAME));
            if (oldConfig.exists()) {
                try {
                    oldConfig.unmarshal(this);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to load " + oldConfig, e);
                }
            }
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Secret getApiKey() {
        return apiKey;
    }

    public void setApiKey(Secret apiKey) {
        this.apiKey = apiKey;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public boolean isFullJobPathAsDefaultTopic() {
        return fullJobPathAsDefaultTopic;
    }

    public void setFullJobPathAsDefaultTopic(boolean fullJobPathAsDefaultTopic) {
        this.fullJobPathAsDefaultTopic = fullJobPathAsDefaultTopic;
    }

    public boolean isFullJobPathInMessage() {
        return fullJobPathInMessage;
    }

    public void setFullJobPathInMessage(boolean fullJobPathInMessage) {
        this.fullJobPathInMessage = fullJobPathInMessage;
    }

    public String getJenkinsUrl() {
        return jenkinsUrl != null ? jenkinsUrl : hudsonUrl;
    }

    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }

    public boolean isSmartNotify() {
        return smartNotify;
    }

    public void setSmartNotify(boolean smartNotify) {
        this.smartNotify = smartNotify;
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        return true;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        url = (String) json.get("url");
        email = (String) json.get("email");
        apiKey = Secret.fromString((String) json.get("apiKey"));
        stream = (String) json.get("stream");
        topic = (String) json.get("topic");
        jenkinsUrl = (String) json.get("jenkinsUrl");
        smartNotify = Boolean.TRUE.equals(json.get("smartNotify"));
        save();

        // Cleanup the configuration file from previous plugin id - humbug
        File oldConfig = new File(Jenkins.getInstance().getRootDir(), OLD_CONFIG_FILE_NAME);
        if (oldConfig.exists()) {
            if (oldConfig.delete()) {
                logger.log(Level.INFO, "Old humbug configuration file successfully cleaned up.");
            } else {
                logger.log(Level.WARNING, "Failed to cleanup old humbug configuration file.");
            }
        }

        return super.configure(req, json);
    }

    @Override
    public String getDisplayName() {
        return "Zulip Notification";
    }

    @Override
    public String getHelpFile() {
        return "/plugin/zulip/help.html";
    }

}
