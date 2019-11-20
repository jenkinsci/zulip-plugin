package jenkins.plugins.zulip;

public class SmartNotification {

    private static final String SMART_NOTIFICATION_GLOBAL = "global";
    private static final String SMART_NOTIFICATION_ENABLED = "enabled";
    private static final String SMART_NOTIFICATION_DISABLED = "disabled";

    /**
     * Evaluates whether smart notification are enabled based on the project and global settings
     *
     * @return true if smart notifications are enabled
     */
    public static boolean isSmartNotifyEnabled(String projectSmartNotify, boolean globalSmartNotify) {
        if (SMART_NOTIFICATION_ENABLED.equalsIgnoreCase(projectSmartNotify)) {
            return true;
        } else if (SMART_NOTIFICATION_DISABLED.equalsIgnoreCase(projectSmartNotify)) {
            return false;
        }
        return globalSmartNotify;
    }

}
