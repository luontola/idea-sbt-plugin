// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.filters.*;
import com.intellij.execution.process.*;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class SbtConsole {

    private static final Key<SbtConsole> CONSOLE_KEY = Key.create("SBT_CONSOLE_KEY");

    public static final String CONSOLE_FILTER_REGEXP =
            "\\s" + RegexpFilter.FILE_PATH_MACROS + ":" + RegexpFilter.LINE_MACROS + ":\\s";

    private final String title;
    private final Project project;
    private final ConsoleView consoleView;
    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private boolean finished = false;

    public SbtConsole(String title, Project project) {
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
                MessageView messageView = MessageView.SERVICE.getInstance(project);

                Content content = ContentFactory.SERVICE.getInstance().createContent(consoleView.getComponent(), title, true);
                content.putUserData(CONSOLE_KEY, SbtConsole.this);
                messageView.getContentManager().addContent(content);
                messageView.getContentManager().setSelectedContent(content);

                removeUnusedTabs(messageView, content);
                activateMessagesWindow();
            }
        });
    }

    private void removeUnusedTabs(MessageView messageView, Content content) {
        for (Content each : messageView.getContentManager().getContents()) {
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
                messageView.getContentManager().removeContent(each, false);
            }
        }
    }

    private void activateMessagesWindow() {
        ToolWindow messagesWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
        if (!messagesWindow.isActive()) {
            messagesWindow.activate(null, false);
        }
    }
}
