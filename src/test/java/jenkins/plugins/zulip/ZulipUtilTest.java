package jenkins.plugins.zulip;

import hudson.model.Item;
import hudson.model.ItemGroup;
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

    // @Mock(extraInterfaces = Item.class) won't work for some reason,
    // so we define our own interface that combine the two we want to mock.
    interface ItemAndItemGroup<I extends Item> extends Item, ItemGroup<I> {
    }

    @Mock
    private Jenkins jenkins;

    @Mock
    private DescriptorImpl descMock;

    @Mock
    private ItemGroup<?> rootItemGroupMock;

    @Mock
    private ItemAndItemGroup<?> nonRootItemGroupMock;

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

    @Test
    @SuppressWarnings("unchecked") // We need unchecked casts to ItemGroup (raw type) in order to mock methods that return ItemGroup<? extends Item>
    public void testDisplayJob() {
        when(itemMock.getDisplayName()).thenReturn("MyJobName");
        when(itemMock.getUrl()).thenReturn("job/MyJob");

        // Ancestors do have a name => Display full path when requested
        when(itemMock.getParent()).thenReturn((ItemGroup)nonRootItemGroupMock);
        when(nonRootItemGroupMock.getParent()).thenReturn((ItemGroup)rootItemGroupMock);
        when(rootItemGroupMock.getDisplayName()).thenReturn("RootName");
        when(rootItemGroupMock.getUrl()).thenReturn("view/RootUrl");
        when(nonRootItemGroupMock.getDisplayName()).thenReturn("NonRootName");
        when(nonRootItemGroupMock.getUrl()).thenReturn("job/NonRootUrl");
        assertEquals("[RootName](http://JenkinsConfigUrl/view/RootUrl) » [NonRootName](http://JenkinsConfigUrl/job/NonRootUrl) » [MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("RootName » NonRootName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // Parent without a URL => don't add a link
        when(itemMock.getParent()).thenReturn((ItemGroup)rootItemGroupMock);
        when(rootItemGroupMock.getDisplayName()).thenReturn("RootName");
        when(rootItemGroupMock.getUrl()).thenReturn(null);
        assertEquals("RootName » [MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("RootName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // Parent with an empty name => display the job only
        // This can happen: see hudson.model.AbstractItem.getFullDisplayName
        when(itemMock.getParent()).thenReturn((ItemGroup)rootItemGroupMock);
        when(rootItemGroupMock.getDisplayName()).thenReturn("");
        when(rootItemGroupMock.getUrl()).thenReturn("view/RootUrl");
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // Parent without a name => display the job only
        // Not sure this can happen, but let's be safe
        when(itemMock.getParent()).thenReturn((ItemGroup)rootItemGroupMock);
        when(rootItemGroupMock.getDisplayName()).thenReturn(null);
        when(rootItemGroupMock.getUrl()).thenReturn("view/RootUrl");
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // Custom Jenkins root URL from plugin config
        when(descMock.getJenkinsUrl()).thenReturn("http://ZulipConfigUrl/");
        when(itemMock.getParent()).thenReturn((ItemGroup)nonRootItemGroupMock);
        when(((Item)nonRootItemGroupMock).getParent()).thenReturn((ItemGroup)rootItemGroupMock);
        when(rootItemGroupMock.getDisplayName()).thenReturn("RootName");
        when(rootItemGroupMock.getUrl()).thenReturn("view/RootUrl");
        when(nonRootItemGroupMock.getDisplayName()).thenReturn("NonRootName");
        when(nonRootItemGroupMock.getUrl()).thenReturn("job/NonRootUrl");
        assertEquals("[RootName](http://ZulipConfigUrl/view/RootUrl) » [NonRootName](http://ZulipConfigUrl/job/NonRootUrl) » [MyJobName](http://ZulipConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("RootName » NonRootName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://ZulipConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // No Jenkins root URL at all
        PowerMockito.when(jenkins.getRootUrl()).thenReturn(null);
        when(descMock.getJenkinsUrl()).thenReturn(null);
        when(itemMock.getParent()).thenReturn((ItemGroup)nonRootItemGroupMock);
        when(((Item)nonRootItemGroupMock).getParent()).thenReturn((ItemGroup)rootItemGroupMock);
        when(rootItemGroupMock.getDisplayName()).thenReturn("RootName");
        when(rootItemGroupMock.getUrl()).thenReturn("view/RootUrl");
        when(nonRootItemGroupMock.getDisplayName()).thenReturn("NonRootName");
        when(nonRootItemGroupMock.getUrl()).thenReturn("job/NonRootUrl");
        assertEquals("RootName » NonRootName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("RootName » NonRootName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));
    }

}
