package hudson.plugins.humbug;

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
public class HumbugUtilTest {

    @Mock
    private Jenkins jenkins;

    @Mock
    private DescriptorImpl descMock;

    @Before
    public void setUp() {
        // Mock Jenkins Instance
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
        PowerMockito.when(jenkins.getRootUrl()).thenReturn("http://JenkinsConfigUrl");
    }

    @Test
    public void testIsValueSet() {
        assertTrue(HumbugUtil.isValueSet("Test"));
        assertFalse(HumbugUtil.isValueSet(""));
        assertFalse(HumbugUtil.isValueSet(null));
    }

    @Test
    public void testGetDefaultValue() {
        assertEquals("Expect value", "Test", HumbugUtil.getDefaultValue("Test", "Default"));
        assertEquals("Expect default", "Default", HumbugUtil.getDefaultValue("", "Default"));
        assertEquals("Expect default", "Default", HumbugUtil.getDefaultValue(null, "Default"));
    }

    @Test
    public void testGetJenkinsUrl() {
        assertEquals("Expect Jenkins Configured Url", "http://JenkinsConfigUrl/", HumbugUtil.getJenkinsUrl(descMock));
        when(descMock.getHudsonUrl()).thenReturn("http://ZulipConfigUrl/");
        assertEquals("Expect Zulip config Url", "http://ZulipConfigUrl/", HumbugUtil.getJenkinsUrl(descMock));
    }

}
