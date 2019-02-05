package jenkins.plugins.zulip;

import hudson.Plugin;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Items;

public class PluginImpl extends Plugin {

  @Initializer(before = InitMilestone.PLUGINS_STARTED)
  public static void addAliases() {
    Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.humbug.HumbugNotifier", ZulipNotifier.class);
  }

}
