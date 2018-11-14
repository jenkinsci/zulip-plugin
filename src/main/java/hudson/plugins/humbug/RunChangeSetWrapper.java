package hudson.plugins.humbug;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.Collections;
import java.util.List;

/**
 * Wrapper that is allowed to pull change set since last build for both {@link AbstractBuild} and {@link WorkflowRun}
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
        } else if (build instanceof WorkflowRun) {
            return !((WorkflowRun) build).getChangeSets().isEmpty();
        }
        return false;
    }

    public List<? extends ChangeLogSet<? extends ChangeLogSet.Entry>> getChangeSets() {
        if (build instanceof AbstractBuild) {
            return Collections.singletonList(((AbstractBuild<?, ?>) build).getChangeSet());
        } else if (build instanceof WorkflowRun) {
            return ((WorkflowRun) build).getChangeSets();
        }
        return null;
    }

}
