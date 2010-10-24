// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;

import java.io.IOException;

public class SbtToolWindowFactory implements ToolWindowFactory {
    private static final Logger logger = Logger.getInstance(SbtToolWindowFactory.class.getName());

    public void createToolWindowContent(final Project project, final ToolWindow toolWindow) {
        new Task.Backgroundable(project, MessageBundle.message("sbt.tasks.executing"), false) {
            public void run(ProgressIndicator indicator) {
                try {
                    SbtRunnerComponent.getInstance(project).startIfNotStarted(toolWindow);
                } catch (IOException e) {
                    logger.error("Failed to start SBT", e);
                }
            }
        }.queue();
    }
}
