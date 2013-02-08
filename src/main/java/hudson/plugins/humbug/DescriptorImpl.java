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
    private String token;
    private String room;
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

    public String getToken() {
        return token;
    }

    public String getRoom() {
        return room;
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
        String projectSubdomain = req.getParameter("humbugSubdomain");
        String projectToken = req.getParameter("humbugToken");
        String projectRoom = req.getParameter("humbugRoom");
        if ( projectRoom == null || projectRoom.trim().length() == 0 ) {
            projectRoom = room;
        }
        if ( projectToken == null || projectToken.trim().length() == 0 ) {
            projectToken = token;
        }
        if ( projectSubdomain == null || projectSubdomain.trim().length() == 0 ) {
            projectSubdomain = subdomain;
        }
        try {
            return new HumbugNotifier(projectSubdomain, projectToken, projectRoom, hudsonUrl, smartNotify);
        } catch (Exception e) {
            String message = "Failed to initialize humbug notifier - check your humbug notifier configuration settings: " + e.getMessage();
            LOGGER.log(Level.WARNING, message, e);
            throw new FormException(message, e, "");
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        subdomain = req.getParameter("humbugSubdomain");
        token = req.getParameter("humbugToken");
        room = req.getParameter("humbugRoom");
        hudsonUrl = req.getParameter("humbugHudsonUrl");
        if ( hudsonUrl != null && !hudsonUrl.endsWith("/") ) {
            hudsonUrl = hudsonUrl + "/";
        }
        smartNotify = req.getParameter("humbugSmartNotify") != null;
        try {
            new HumbugNotifier(subdomain, token, room, hudsonUrl, smartNotify);
        } catch (Exception e) {
            String message = "Failed to initialize humbug notifier - check your global humbug notifier configuration settings: " + e.getMessage();
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
        return "Humbug Notification";
    }

    /**
     * @see hudson.model.Descriptor#getHelpFile()
     */
    @Override
    public String getHelpFile() {
        return "/plugin/humbug/help.html";
    }
}
