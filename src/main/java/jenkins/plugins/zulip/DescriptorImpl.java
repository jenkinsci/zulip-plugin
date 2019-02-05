package jenkins.plugins.zulip;

import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Descriptor for {@link ZulipNotifier}
 */
@Symbol("zulipNotification")
public class DescriptorImpl extends BuildStepDescriptor<Publisher> {
    private boolean enabled = false;
    private String url;
    private String email;
    private String apiKey;
    private String stream;
    private String topic;
    private String jenkinsUrl;
    private boolean smartNotify;

    public DescriptorImpl() {
        super(ZulipNotifier.class);
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

    public String getJenkinsUrl() {
        return jenkinsUrl;
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
        url = req.getParameter("url");
        email = req.getParameter("email");
        apiKey = req.getParameter("apiKey");
        stream = req.getParameter("stream");
        topic = req.getParameter("topic");
        jenkinsUrl = req.getParameter("jenkinsUrl");
        smartNotify = req.getParameter("smartNotify") != null;
        save();
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
