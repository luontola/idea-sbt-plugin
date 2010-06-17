// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.filters.*;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public class SbtConsole {
    // org.jetbrains.idea.maven.embedder.MavenConsoleImpl

    private static final Key<SbtConsole> CONSOLE_KEY = Key.create("SBT_CONSOLE_KEY");

    public static final String CONSOLE_FILTER_REGEXP =
            "\\s" + RegexpFilter.FILE_PATH_MACROS + ":" + RegexpFilter.LINE_MACROS + ":\\s";

    private final String title;
    private final Project project;
    private final ConsoleView consoleView;
    private final ToolWindow messagesWindow;
    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private boolean finished = false;

    public SbtConsole(String title, Project project, @Nullable ToolWindow messagesWindow) {
        this.messagesWindow = messagesWindow;
        this.title = title;
        this.project = project;
        this.consoleView = createConsoleView(project);
    }

    private static ConsoleView createConsoleView(Project project) {
        return createConsoleBuilder(project).getConsole();
    }

    public static TextConsoleBuilder createConsoleBuilder(Project project) {
        TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);

        Filter[] filters = {new ExceptionFilter(project), new RegexpFilter(project, CONSOLE_FILTER_REGEXP)};
        for (Filter filter : filters) {
            builder.addFilter(filter);
        }
        return builder;
    }

    public boolean isFinished() {
        return finished;
    }

    public void finish() {
        finished = true;
    }

    public void attachToProcess(ProcessHandler processHandler) {
        consoleView.attachToProcess(processHandler);
        processHandler.addProcessListener(new ProcessAdapter() {
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                ensureAttachedToToolWindow();
            }

            public void processTerminated(ProcessEvent event) {
                finish();
            }
        });
    }

    private void ensureAttachedToToolWindow() {
        if (!isOpen.compareAndSet(false, true)) {
            return;
        }

        // org.jetbrains.idea.maven.embedder.MavenConsoleImpl#ensureAttachedToToolWindow
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                ToolWindow window;
                if (messagesWindow == null)
                    window = ToolWindowManager.getInstance(project).getToolWindow(MessageBundle.message("sbt.console.id"));
                else
                    window = messagesWindow;

                Content content = ContentFactory.SERVICE.getInstance().createContent(consoleView.getComponent(), title, true);
                content.putUserData(CONSOLE_KEY, SbtConsole.this);
                window.getContentManager().addContent(content);
                window.getContentManager().setSelectedContent(content);

                removeUnusedTabs(window, content);
                if (!window.isActive())
                    window.activate(null, false);
            }
        });
    }

    private void removeUnusedTabs(ToolWindow window, Content content) {
        for (Content each : window.getContentManager().getContents()) {
            if (each.isPinned()) {
                continue;
            }
            if (each == content) {
                continue;
            }

            SbtConsole console = each.getUserData(CONSOLE_KEY);
            if (console == null) {
                continue;
            }

            if (!title.equals(console.title)) {
                continue;
            }

            if (console.isFinished()) {
                window.getContentManager().removeContent(each, false);
            }
        }
    }
}
