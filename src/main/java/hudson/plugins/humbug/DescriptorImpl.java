package hudson.plugins.humbug;

import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DescriptorImpl extends BuildStepDescriptor<Publisher> {
    private boolean enabled = false;
    private String subdomain;
    private String email;
    private String apiKey;
    private String stream;
    private String hudsonUrl;
    private boolean smartNotify;
    private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());

    public DescriptorImpl() {
        super(HumbugNotifier.class);
        load();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getEmail() {
        return email;
    }

    public String getStream() {
        return stream;
    }

    public String getHudsonUrl() {
        return hudsonUrl;
    }

    public boolean getSmartNotify() {
        return smartNotify;
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        return true;
    }

    /**
     * @see hudson.model.Descriptor#newInstance(org.kohsuke.stapler.StaplerRequest)
     */
    @Override
    public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        try {
            return new HumbugNotifier();
        } catch (Exception e) {
            String message = "Failed to initialize zulip notifier - check your zulip notifier configuration settings: " + e.getMessage();
            LOGGER.log(Level.WARNING, message, e);
            throw new FormException(message, e, "");
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        email = req.getParameter("humbugEmail");
        apiKey = req.getParameter("humbugApiKey");
        subdomain = req.getParameter("humbugSubdomain");
        stream = req.getParameter("humbugStream");
        hudsonUrl = req.getParameter("humbugHudsonUrl");
        if ( hudsonUrl != null && !hudsonUrl.endsWith("/") ) {
            hudsonUrl = hudsonUrl + "/";
        }
        smartNotify = req.getParameter("humbugSmartNotify") != null;
        try {
            new HumbugNotifier();
        } catch (Exception e) {
            String message = "Failed to initialize zulip notifier - check your global zulip notifier configuration settings: " + e.getMessage();
            LOGGER.log(Level.WARNING, message, e);
            throw new FormException(message, e, "");
        }
        save();
        return super.configure(req, json);
    }

    /**
     * @see hudson.model.Descriptor#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return "Zulip Notification";
    }

    /**
     * @see hudson.model.Descriptor#getHelpFile()
     */
    @Override
    public String getHelpFile() {
        return "/plugin/humbug/help.html";
    }
}
