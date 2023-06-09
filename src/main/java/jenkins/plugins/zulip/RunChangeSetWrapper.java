package jenkins.plugins.zulip;

import java.util.Collections;
import java.util.List;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import jenkins.scm.RunWithSCM;

/**
 * Wrapper that pulls change set since last build
 */
public class RunChangeSetWrapper {

    private final Run<?, ?> build;

    public RunChangeSetWrapper(Run<?, ?> build) {
        this.build = build;
    }

    public boolean hasChangeSetComputed() {
        if (build instanceof AbstractBuild) {
            return ((AbstractBuild<?, ?>) build).hasChangeSetComputed();
        }
        return true;
    }

    public boolean hasChangeSet() {
        if (build instanceof AbstractBuild) {
            return !((AbstractBuild<?, ?>) build).getChangeSet().isEmptySet();
        } else if (build instanceof RunWithSCM) {
            return !((RunWithSCM<?, ?>) build).getChangeSets().isEmpty();
        }
        return false;
    }

    public List<? extends ChangeLogSet<? extends ChangeLogSet.Entry>> getChangeSets() {
        if (build instanceof AbstractBuild) {
            return Collections.singletonList(((AbstractBuild<?, ?>) build).getChangeSet());
        } else if (build instanceof RunWithSCM) {
            return ((RunWithSCM<?, ?>) build).getChangeSets();
        }
        return null;
    }

}
