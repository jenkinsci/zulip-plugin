package jenkins.plugins.zulip;

import hudson.model.Item;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Jenkins.class)
public class ZulipUtilTest {

    @Mock
    private Jenkins jenkins;

    @Mock
    private DescriptorImpl descMock;

    @Mock
    private Item itemMock;

    @Before
    public void setUp() {
        // Mock Jenkins Instance
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
        PowerMockito.when(jenkins.getRootUrl()).thenReturn("http://JenkinsConfigUrl");
    }

    @Test
    public void testIsValueSet() {
        assertTrue(ZulipUtil.isValueSet("Test"));
        assertFalse(ZulipUtil.isValueSet(""));
        assertFalse(ZulipUtil.isValueSet(null));
    }

    @Test
    public void testGetDefaultValue() {
        assertEquals("Expect value", "Test", ZulipUtil.getDefaultValue("Test", "Default"));
        assertEquals("Expect default", "Default", ZulipUtil.getDefaultValue("", "Default"));
        assertEquals("Expect default", "Default", ZulipUtil.getDefaultValue(null, "Default"));
    }

    @Test
    public void testGetJenkinsUrl() {
        assertEquals("Expect Jenkins Configured Url", "http://JenkinsConfigUrl/", ZulipUtil.getJenkinsUrl(descMock));
        when(descMock.getJenkinsUrl()).thenReturn("http://ZulipConfigUrl/");
        assertEquals("Expect Zulip config Url", "http://ZulipConfigUrl/", ZulipUtil.getJenkinsUrl(descMock));
    }

    @Test
    public void testDisplayObjectWithLink() {
        when(itemMock.getDisplayName()).thenReturn("MyJobName");

        // Default Jenkins root URL from the Jenkins class
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayObjectWithLink(itemMock, "job/MyJob", descMock));
        assertEquals("MyJobName", ZulipUtil.displayObjectWithLink(itemMock, null, descMock));

        // Custom Jenkins root URL from plugin config
        when(descMock.getJenkinsUrl()).thenReturn("http://ZulipConfigUrl/");
        assertEquals("[MyJobName](http://ZulipConfigUrl/job/MyJob)", ZulipUtil.displayObjectWithLink(itemMock, "job/MyJob", descMock));
        assertEquals("MyJobName", ZulipUtil.displayObjectWithLink(itemMock, null, descMock));

        // No Jenkins root URL at all
        PowerMockito.when(jenkins.getRootUrl()).thenReturn(null);
        when(descMock.getJenkinsUrl()).thenReturn(null);
        assertEquals("MyJobName", ZulipUtil.displayObjectWithLink(itemMock, "job/MyJob", descMock));
        assertEquals("MyJobName", ZulipUtil.displayObjectWithLink(itemMock, null, descMock));
    }

}
