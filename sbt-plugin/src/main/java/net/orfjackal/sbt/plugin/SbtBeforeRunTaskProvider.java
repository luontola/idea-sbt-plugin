// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.*;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.concurrency.Semaphore;

import java.util.concurrent.atomic.AtomicBoolean;

public class SbtBeforeRunTaskProvider extends BeforeRunTaskProvider<SbtBeforeRunTask> {

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
        SelectSbtActionDialog dialog = new SelectSbtActionDialog(project);

        dialog.show();
        if (!dialog.isOK()) {
            return false;
        }

        task.setAction(dialog.getAction());
        return true;
    }

    public boolean executeTask(DataContext dataContext, RunConfiguration runConfiguration, final SbtBeforeRunTask task) {
        final Semaphore done = new Semaphore();
        final AtomicBoolean success = new AtomicBoolean(false);

        try {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                public void run() {
                    executeInBackground(task, done, success);
                }
            }, ModalityState.NON_MODAL);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        done.waitFor();
        return success.get();
    }

    private void executeInBackground(SbtBeforeRunTask task, final Semaphore done, final AtomicBoolean success) {
        done.down();
        new Task.Backgroundable(project, MessageBundle.message("sbt.tasks.executing"), true) {
            public void run(ProgressIndicator indicator) {
                try {

                    System.out.println("SbtBeforeRunTaskProvider.run");
                    // TODO: send command and wait until done
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    success.set(true);
                } finally {
                    done.up();
                }
            }
        }.queue();
    }
}
