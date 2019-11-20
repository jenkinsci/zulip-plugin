package jenkins.plugins.zulip;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmartNotificationTest {

    @Test
    public void testIsSmartNotifyEnabled() {
        assertTrue(SmartNotification.isSmartNotifyEnabled(null, true));
        assertTrue(SmartNotification.isSmartNotifyEnabled("", true));
        assertTrue(SmartNotification.isSmartNotifyEnabled("foo", true));
        assertTrue(SmartNotification.isSmartNotifyEnabled("global", true));
        assertTrue(SmartNotification.isSmartNotifyEnabled("enabled", true));
        assertFalse(SmartNotification.isSmartNotifyEnabled("DISABLED", true));
        //
        assertFalse(SmartNotification.isSmartNotifyEnabled(null, false));
        assertFalse(SmartNotification.isSmartNotifyEnabled("", false));
        assertFalse(SmartNotification.isSmartNotifyEnabled("foo", false));
        assertFalse(SmartNotification.isSmartNotifyEnabled("global", false));
        assertTrue(SmartNotification.isSmartNotifyEnabled("enabled", false));
        assertFalse(SmartNotification.isSmartNotifyEnabled("DISABLED", false));
    }

}
