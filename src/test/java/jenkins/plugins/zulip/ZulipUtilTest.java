package jenkins.plugins.zulip;

import hudson.model.Item;
import hudson.model.ItemGroup;
import jenkins.model.Jenkins;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class ZulipUtilTest {

    @Mock
    private Jenkins jenkins;

    @Mock
    private DescriptorImpl descMock;

    @Mock
    private ItemGroup<?> rootItemGroupMock;

    @Mock
    private ItemAndItemGroup parentItemGroupMock;

    @Mock
    private Item itemMock;

    private MockedStatic<Jenkins> jenkinsStatic;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        jenkinsStatic = Mockito.mockStatic(Jenkins.class);
        jenkinsStatic.when(Jenkins::get).thenReturn(jenkins);
        when(jenkins.getRootUrl()).thenReturn("http://JenkinsConfigUrl/");
    }

    @After
    public void tearDown() {
        jenkinsStatic.close();
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
        assertEquals("Expect Jenkins Configured Url", "http://JenkinsConfigUrl/",
                ZulipUtil.getJenkinsUrl(descMock));

        when(descMock.getJenkinsUrl()).thenReturn("http://ZulipConfigUrl/");
        assertEquals("Expect Zulip config Url", "http://ZulipConfigUrl/", ZulipUtil.getJenkinsUrl(descMock));
    }

    @Test
    public void testDisplayObjectWithLink() {
        when(jenkins.getRootUrl()).thenReturn("http://JenkinsConfigUrl/");
        when(itemMock.getDisplayName()).thenReturn("MyJobName");

        // Default Jenkins root URL from the Jenkins class
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)",
                ZulipUtil.displayObjectWithLink(itemMock, "job/MyJob", descMock));
        assertEquals("MyJobName", ZulipUtil.displayObjectWithLink(itemMock, null, descMock));

        // Custom Jenkins root URL from plugin config
        when(descMock.getJenkinsUrl()).thenReturn("http://ZulipConfigUrl/");
        assertEquals("[MyJobName](http://ZulipConfigUrl/job/MyJob)",
                ZulipUtil.displayObjectWithLink(itemMock, "job/MyJob", descMock));
        assertEquals("MyJobName", ZulipUtil.displayObjectWithLink(itemMock, null, descMock));

        // No Jenkins root URL at all
        setupDescriptorImplGetJenkinsUrlAndGetRootUrlStubs();
        assertEquals("MyJobName", ZulipUtil.displayObjectWithLink(itemMock, "job/MyJob", descMock));
        assertEquals("MyJobName", ZulipUtil.displayObjectWithLink(itemMock, null, descMock));
    }

    @Test
    // We need unchecked casts to ItemGroup (raw type) in order to mock methods that
    // return ItemGroup<? extends Item>
    @SuppressWarnings("unchecked")
    public void testDisplayJob() {
        jenkinsStatic.when(Jenkins::get).thenReturn(jenkins);
        setupItemGetUrlAndGetDisplayNameStubs(itemMock, "job/MyJob", "MyJobName");
        when(itemMock.getParent()).thenReturn((ItemGroup) parentItemGroupMock);
        // Jenkins is always at the root of the job tree
        when(parentItemGroupMock.getParent()).thenReturn((ItemGroup) jenkins);
        // Make sure Jenkins has a name, otherwise we might miss a bug where Jenkins is
        // displayed unexpectedly.
        when(jenkins.getDisplayName()).thenReturn("Jenkins");

        // Parent with a name => Display full path when requested
        setupItemGetUrlAndGetDisplayNameStubs(parentItemGroupMock, "job/Parent", "ParentName");
        assertEquals(
                "[ParentName](http://JenkinsConfigUrl/job/Parent) » [MyJobName](http://JenkinsConfigUrl/job/MyJob)",
                ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("ParentName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)",
                ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // Parent without a URL => don't add a link
        setupItemGetUrlAndGetDisplayNameStubs(parentItemGroupMock, null, "ParentName");
        assertEquals("ParentName » [MyJobName](http://JenkinsConfigUrl/job/MyJob)",
                ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("ParentName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)",
                ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // Parent with an empty name => display the job only
        // This can happen: see hudson.model.AbstractItem.getFullDisplayName
        setupItemGetUrlAndGetDisplayNameStubs(parentItemGroupMock, "view/RootUrl", "");
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)",
                ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)",
                ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // Parent without a name => display the job only
        // Not sure this can happen, but let's be safe
        setupItemGetUrlAndGetDisplayNameStubs(parentItemGroupMock, "view/RootUrl", null);
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)",
                ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)",
                ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // Custom Jenkins root URL from plugin config
        when(descMock.getJenkinsUrl()).thenReturn("http://ZulipConfigUrl/");
        setupItemGetUrlAndGetDisplayNameStubs(parentItemGroupMock, "job/Parent", "ParentName");
        assertEquals(
                "[ParentName](http://ZulipConfigUrl/job/Parent) » [MyJobName](http://ZulipConfigUrl/job/MyJob)",
                ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("ParentName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://ZulipConfigUrl/job/MyJob)",
                ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // No Jenkins root URL at all
        setupDescriptorImplGetJenkinsUrlAndGetRootUrlStubs();
        setupItemGetUrlAndGetDisplayNameStubs(parentItemGroupMock, "job/Parent", "ParentName");
        assertEquals("ParentName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("ParentName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));
    }

    private void setupItemGetUrlAndGetDisplayNameStubs(Item item, String getUrlReturnValue, String getDisplayNameReturnValue) {
        when(item.getDisplayName()).thenReturn(getDisplayNameReturnValue);
        when(item.getUrl()).thenReturn(getUrlReturnValue);
    }

    private void setupDescriptorImplGetJenkinsUrlAndGetRootUrlStubs() {
        when(jenkins.getRootUrl()).thenReturn(null);
        when(descMock.getJenkinsUrl()).thenReturn(null);
    }

}
