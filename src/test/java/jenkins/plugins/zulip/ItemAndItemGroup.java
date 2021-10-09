package jenkins.plugins.zulip;

import hudson.model.Item;
import hudson.model.ItemGroup;

// @Mock(extraInterfaces = Item.class) won't work for some reason,
// so we define our own interface that combine the two we want to mock.
interface ItemAndItemGroup<I extends Item> extends Item, ItemGroup<I> {
}
