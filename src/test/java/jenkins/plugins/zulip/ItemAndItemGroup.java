package jenkins.plugins.zulip;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;

import org.jvnet.hudson.reactor.ReactorException;

import hudson.model.Item;
import jenkins.model.Jenkins;

// For some reason, returning something different than a Jenkins instance
// when mocking getParent of Job fails.
// This is a very hacky aproach, that should be refactored
public abstract class ItemAndItemGroup extends Jenkins implements Item {
    public ItemAndItemGroup(File root, ServletContext context)
            throws IOException, InterruptedException, ReactorException {
        super(root, context);
    }
}
