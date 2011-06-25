// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.ui.layout.ViewContext;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompilerTopics;
import com.intellij.openapi.compiler.DummyCompileContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.Topic;

public class SbtBeforeRunTaskProvider extends BeforeRunTaskProvider<SbtBeforeRunTask> {

    private static final Logger logger = Logger.getInstance(SbtBeforeRunTaskProvider.class.getName());

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
        SelectSbtActionDialog dialog = new SelectSbtActionDialog(project, task.getAction(), task.isRunInCurrentModule());

        dialog.show();
        if (!dialog.isOK()) {
            return false;
        }

        task.setRunInCurrentModule(dialog.isRunInCurrentModule());
        task.setAction(dialog.getSelectedAction());
        return true;
    }

    public boolean executeTask(DataContext dataContext, RunConfiguration runConfiguration, final SbtBeforeRunTask task) {
        final String action = task.getAction();
        if (action == null) {
            return false;
        }

        boolean runResult = run(runConfiguration, task, action);
        if (!runResult) {
            final Project project = PlatformDataKeys.PROJECT.getData(dataContext);
            if (project != null) {
                notifyFailureAndActivateToolWindow(action, project);
            }
        }
        return runResult;
    }

    private boolean run(RunConfiguration runConfiguration, SbtBeforeRunTask task, String action) {
        if (task.isRunInCurrentModule() && runConfiguration instanceof ModuleBasedConfiguration<?>) {
            ModuleBasedConfiguration<?> moduleBasedConfiguration = (ModuleBasedConfiguration<?>) runConfiguration;
            Module[] modules = moduleBasedConfiguration.getModules();
            if (modules.length == 1) {
                Module module = modules[0];
                return runInModule(action, module.getName());
            }
        }

        return runDirectly(action);
    }

    private boolean runInModule(String action, String moduleName) {
        runAndWait("project " + moduleName);
        try {
            return runAndWait(action);
        } catch (Exception e) {
            logger.error(e);
            return false;
        } finally {
            try {
                runAndWait("project /");
            } catch (Exception e1) {
                // ignore
            }
        }
    }

    private boolean runDirectly(String action) {
        try {
            return runAndWait(action);
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
    }

    private boolean runAndWait(String action) {
        return SbtRunnerComponent.getInstance(project)
                .executeInBackground(action)
                .waitForResult();
    }

    private void notifyFailureAndActivateToolWindow(final String action, final Project project) {
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            public void run() {
                String toolWindowId = MessageBundle.message("sbt.console.id");
                String message = "SBT action \"" + action + "\" failed.";
                ToolWindowManager.getInstance(project).notifyByBalloon(toolWindowId, MessageType.ERROR, message);
                final ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(toolWindowId);
                if (window != null && !window.isVisible()) {
                    window.activate(null, false);
                }
                SbtRunnerComponent.getInstance(project).getConsole().scrollToEnd();
            }
        }, ModalityState.NON_MODAL);
    }
}
