package jenkins.plugins.zulip;

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

/**
 * Static helper methods
 */
public class ZulipUtil {

    private static final Logger logger = Logger.getLogger(ZulipUtil.class.getName());

    /**
     * Tests if value is filled (not null or blank)
     *
     * @param value The value to test
     * @return true if value is set
     */
    public static boolean isValueSet(String value) {
        return (value != null && value.length() > 0);
    }

    /**
     * Gets either supplied value or default value (usually from global config)
     *
     * @param value        Thee supplied value
     * @param defaultValue The default value
     * @return The value if filled, otherwise default value
     */
    public static String getDefaultValue(String value, String defaultValue) {
        if (isValueSet(value)) {
            return value;
        }
        return defaultValue;
    }

    /**
     * Gets Url pointing to Jenkins instance. By default use value from Zulip config,
     * if it's blank, fallback to Jenkins instance settings
     *
     * @param globalConfig Zulip global configuration
     * @return The Jenkins instance Url
     */
    public static String getJenkinsUrl(DescriptorImpl globalConfig) {
        String jenkinsUrl = getDefaultValue(globalConfig.getJenkinsUrl(), Jenkins.getInstance().getRootUrl());
        if (jenkinsUrl != null && jenkinsUrl.length() > 0 && !jenkinsUrl.endsWith("/")) {
            jenkinsUrl = jenkinsUrl + "/";
        }
        return jenkinsUrl;
    }

    /**
     * Expands the variables in the given value by using environment variables from the build process
     *
     * @param run A build this is running as part of
     * @param listener A place to send output
     * @param value A value to expand variables in
     * @return The value with expanded variables
     */
    public static String expandVariables(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener, String value) throws InterruptedException {
        String expandedMessage = value;
        try {
            expandedMessage = run.getEnvironment(listener).expand(expandedMessage);
        } catch (IOException ex) {
            logger.severe("Failed to expand message variables: " + ex.getMessage());
        }
        return expandedMessage;
    }

    /**
     * Helper method to display a Jenkins model object with a link.
     *
     * @param object       The Jenkins model object (item, run, ...)
     * @param url          The Url to the Jenkins model object
     * @param globalConfig Zulip global configuration
     * @return A string representing the Jenkins model object, with a link if possible.
     */
    public static String displayObjectWithLink(ModelObject object, String url, DescriptorImpl globalConfig) {
        String message = object.getDisplayName();
        if (url != null) {
            String jenkinsUrl = ZulipUtil.getJenkinsUrl(globalConfig);
            if (ZulipUtil.isValueSet(jenkinsUrl)) {
                message = "[" + message + "](" + jenkinsUrl + url + ")";
            }
        }
        return message;
    }

}
