package jenkins.plugins.zulip;

import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Logger;

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
     * Gets Url pointing to Jenkins instance. By default use value from Zulip
     * config, if it's blank, fallback to Jenkins instance settings
     *
     * @param globalConfig Zulip global configuration
     * @return The Jenkins instance Url
     */
    public static String getJenkinsUrl(DescriptorImpl globalConfig) {
        String jenkinsUrl = getDefaultValue(globalConfig.getJenkinsUrl(), Jenkins.get().getRootUrl());
        if (jenkinsUrl != null && jenkinsUrl.length() > 0 && !jenkinsUrl.endsWith("/")) {
            jenkinsUrl = jenkinsUrl + "/";
        }
        return jenkinsUrl;
    }

    /**
     * Expands the variables in the given value by using environment variables from
     * the build process
     *
     * @param run      A build this is running as part of
     * @param listener A place to send output
     * @param value    A value to expand variables in
     * @return The value with expanded variables
     */
    public static String expandVariables(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener, String value)
            throws InterruptedException {
        String expandedMessage = value;
        try {
            expandedMessage = run.getEnvironment(listener).expand(expandedMessage);
        } catch (IOException ex) {
            logger.severe("Failed to expand message variables: " + ex.getMessage());
        }
        return expandedMessage;
    }

    /**
     * Helper method to display an item, optionally with links.
     *
     * @param item         The item to display.
     * @param globalConfig Zulip global configuration.
     * @param fullPath     Whether to display the full path including the display
     *                     name of parent items ({@code true}) or just this item's
     *                     display name ({@code false}).
     * @param displayLinks Whether to display links to the item and (if relevant)
     *                     its parent items.
     * @return A string representing the item.
     */
    public static String displayItem(Item item, DescriptorImpl globalConfig, boolean fullPath, boolean displayLinks) {
        StringBuilder builder = new StringBuilder();
        // Don't call getUrl() unless necessary: the logic behind that method is
        // complex.
        displayObject(builder, item, displayLinks ? item.getUrl() : null, globalConfig, fullPath, displayLinks);
        return builder.toString();
    }

    /**
     * Helper method to display a model object, optionally with links.
     *
     * @param builder      The builder to append to.
     * @param object       The object to display.
     * @param globalConfig Zulip global configuration.
     * @param fullPath     Whether to display the full path including the display
     *                     name of parent items ({@code true}) or just this object's
     *                     display name ({@code false}).
     * @param displayLinks Whether to display links to the object and (if relevant)
     *                     its parent items.
     */
    private static void displayObject(StringBuilder builder, ModelObject object, String url,
            DescriptorImpl globalConfig,
            boolean fullPath, boolean displayLinks) {
        // We never display Jenkins; it's implicit.
        if (object == Jenkins.get()) {
            return;
        }
        // The only common interface between Item and ItemGroup is ModelObject, which
        // doesn't define getParent, so we need to resort to instanceof + cast to crawl
        // up the item tree.
        if (fullPath && object instanceof Item) {
            ItemGroup<?> parent = ((Item) object).getParent();
            int lengthBefore = builder.length();
            // Don't call getUrl() unless necessary: the logic behind that method is
            // complex.
            displayObject(builder, parent, displayLinks ? parent.getUrl() : null, globalConfig, fullPath, displayLinks);
            if (builder.length() != lengthBefore) {
                builder.append(" » ");
            }
        }
        String displayName = object.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            builder.append(displayObjectWithLink(object, displayLinks ? url : null, globalConfig));
        }
    }

    /**
     * Helper method to display a Jenkins model object with a link.
     *
     * @param object       The Jenkins model object (item, run, ...)
     * @param url          The Url to the Jenkins model object
     * @param globalConfig Zulip global configuration
     * @return A string representing the Jenkins model object, with a link if
     *         possible.
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
