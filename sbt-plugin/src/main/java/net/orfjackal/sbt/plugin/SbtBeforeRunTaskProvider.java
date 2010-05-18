// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.concurrency.Semaphore;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SbtBeforeRunTaskProvider extends BeforeRunTaskProvider<SbtBeforeRunTask> {

    private static final Logger LOG = Logger.getInstance(SbtBeforeRunTaskProvider.class.getName());

    private static final Key<SbtBeforeRunTask> TASK_ID = Key.create("SBT.BeforeRunTask");
    private final Project project;

    public SbtBeforeRunTaskProvider(Project project) {
        this.project = project;
    }

    public Key<SbtBeforeRunTask> getId() {
        return TASK_ID;
    }

    public String getDescription(RunConfiguration runConfiguration, SbtBeforeRunTask task) {
        String desc = task.getAction();
        return desc == null
                ? MessageBundle.message("sbt.tasks.before.run.empty")
                : MessageBundle.message("sbt.tasks.before.run", desc);
    }

    public boolean hasConfigurationButton() {
        return true;
    }

    public SbtBeforeRunTask createTask(RunConfiguration runConfiguration) {
        return new SbtBeforeRunTask();
    }

    public boolean configureTask(RunConfiguration runConfiguration, SbtBeforeRunTask task) {
        SelectSbtActionDialog dialog = new SelectSbtActionDialog(project, task.getAction());

        dialog.show();
        if (!dialog.isOK()) {
            return false;
        }

        task.setAction(dialog.getSelectedAction());
        return true;
    }

    public boolean executeTask(DataContext dataContext, RunConfiguration runConfiguration, final SbtBeforeRunTask task) {
        final String action = task.getAction();
        if (action == null) {
            return false;
        }

        CompletionSignal signal = new CompletionSignal();
        try {
            executeInBackground(action, signal);
        } catch (Exception e) {
            LOG.error(e);
            return false;
        }
        return signal.waitForResult();
    }

    private void executeInBackground(final String action, final CompletionSignal signal) {
        signal.begin();

        final Task.Backgroundable task = new Task.Backgroundable(project, MessageBundle.message("sbt.tasks.executing"), false) {
            public void run(ProgressIndicator indicator) {
                try {
                    LOG.info("Begin executing: " + action);
                    SbtRunnerComponent sbt = SbtRunnerComponent.getInstance(myProject);
                    sbt.executeAndWait(action);
                    LOG.info("Done executing: " + action);

                    signal.success();
                } catch (IOException e) {
                    LOG.error("Failed to execute action: " + action, e);
                    throw new RuntimeException("Failed to execute action: " + action, e);
                } finally {
                    signal.finished();
                }
            }
        };

        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            public void run() {
                task.queue();
            }
        }, ModalityState.NON_MODAL);
    }

    private static class CompletionSignal {
        private final Semaphore done = new Semaphore();
        private final AtomicBoolean result = new AtomicBoolean(false);

        public void begin() {
            done.down();
        }

        public void success() {
            result.set(true);
        }

        public void finished() {
            done.up();
        }

        public boolean waitForResult() {
            done.waitFor();
            return result.get();
        }
    }
}
