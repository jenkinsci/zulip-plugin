package hudson.plugins.humbug;

import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Descriptor for {@link HumbugNotifier}
 */
@Symbol("zulipNotification")
public class DescriptorImpl extends BuildStepDescriptor<Publisher> {
    private boolean enabled = false;
    private String url;
    private String email;
    private String apiKey;
    private String stream;
    private String topic;
    private String hudsonUrl;
    private boolean smartNotify;

    public DescriptorImpl() {
        super(HumbugNotifier.class);
        load();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
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

    public String getHudsonUrl() {
        return hudsonUrl;
    }

    public void setHudsonUrl(String hudsonUrl) {
        this.hudsonUrl = hudsonUrl;
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
        url = req.getParameter("humbugUrl");
        email = req.getParameter("humbugEmail");
        apiKey = req.getParameter("humbugApiKey");
        stream = req.getParameter("humbugStream");
        topic = req.getParameter("humbugTopic");
        hudsonUrl = req.getParameter("humbugHudsonUrl");
        smartNotify = req.getParameter("humbugSmartNotify") != null;
        save();
        return super.configure(req, json);
    }

    @Override
    public String getDisplayName() {
        return "Zulip Notification";
    }

    @Override
    public String getHelpFile() {
        return "/plugin/humbug/help.html";
    }

}
