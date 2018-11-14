package hudson.plugins.humbug;

import jenkins.model.Jenkins;

/**
 * Static helper methods
 */
public class HumbugUtil {

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
        String jenkinsUrl = getDefaultValue(globalConfig.getHudsonUrl(), Jenkins.getInstance().getRootUrl());
        if (jenkinsUrl != null && jenkinsUrl.length() > 0 && !jenkinsUrl.endsWith("/")) {
            jenkinsUrl = jenkinsUrl + "/";
        }
        return jenkinsUrl;
    }

}
