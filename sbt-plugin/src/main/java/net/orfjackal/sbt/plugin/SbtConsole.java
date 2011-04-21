// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.filters.*;
import com.intellij.execution.process.*;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.*;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.*;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class SbtConsole {
    // org.jetbrains.idea.maven.embedder.MavenConsoleImpl

    private static final Logger logger = Logger.getInstance(SbtConsole.class.getName());

    private static final Key<SbtConsole> CONSOLE_KEY = Key.create("SBT_CONSOLE_KEY");

    public static final String CONSOLE_FILTER_REGEXP =
            "\\s" + RegexpFilter.FILE_PATH_MACROS + ":" + RegexpFilter.LINE_MACROS + ":\\s";

    private final String title;
    private final Project project;
    private final ConsoleView consoleView;
    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private final SbtRunnerComponent runnerComponent;
    private boolean finished = false;

    public SbtConsole(String title, Project project, SbtRunnerComponent runnerComponent) {
        this.title = title;
        this.project = project;
        this.consoleView = createConsoleView(project);
        this.runnerComponent = runnerComponent;
    }

    private static ConsoleView createConsoleView(Project project) {
        return createConsoleBuilder(project).getConsole();
    }

    public static TextConsoleBuilder createConsoleBuilder(Project project) {
        TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);

        final SbtColorizerFilter logLevelFilter = new SbtColorizerFilter();
        final ExceptionFilter exceptionFilter = new ExceptionFilter(project);
        final RegexpFilter regexpFilter = new RegexpFilter(project, CONSOLE_FILTER_REGEXP);
        for (Filter filter : Arrays.asList(exceptionFilter, regexpFilter, logLevelFilter)) {
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

    public void attachToProcess(ProcessHandler processHandler, final SbtRunnerComponent runnerComponent) {
        consoleView.attachToProcess(processHandler);
        processHandler.addProcessListener(new ProcessAdapter() {
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    public void run() {
                        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(MessageBundle.message("sbt.console.id"));
                        /* When we retrieve a window from ToolWindowManager before SbtToolWindowFactory is called,
                         * we get an undesirable Content */
                        for (Content each : window.getContentManager().getContents()) {
                            if (each.getUserData(CONSOLE_KEY) == null) {
                                window.getContentManager().removeContent(each, false);
                            }
                        }
                        ensureAttachedToToolWindow(window);
                    }
                });
            }

            public void processTerminated(ProcessEvent event) {
                finish();
            }
        });
    }

    public final void ensureAttachedToToolWindow(ToolWindow window) {
        if (!isOpen.compareAndSet(false, true)) {
            return;
        }

        // org.jetbrains.idea.maven.embedder.MavenConsoleImpl#ensureAttachedToToolWindow
        SimpleToolWindowPanel toolWindowPanel = new SimpleToolWindowPanel(false, true);
        JComponent consoleComponent = consoleView.getComponent();
        toolWindowPanel.setContent(consoleComponent);
        toolWindowPanel.setToolbar(createToolbar());
        Content content = ContentFactory.SERVICE.getInstance().createContent(toolWindowPanel, title, true);
        content.putUserData(CONSOLE_KEY, SbtConsole.this);

        window.getContentManager().addContent(content);
        window.getContentManager().setSelectedContent(content);

        removeUnusedTabs(window, content);

        if (!window.isActive()) {
            window.activate(null, false);
        }
    }

    private JComponent createToolbar() {
        JPanel toolbarPanel = new JPanel(new GridLayout());

        DefaultActionGroup group = new DefaultActionGroup();
        AnAction startSbtAction = new StartSbtAction();
        // TODO #22 get this working.
        // startSbtAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke("control F5")), ancestor);

        AnAction killSbtAction = new KillSbtAction();

        group.add(startSbtAction);
        group.add(killSbtAction);

        // Adds "Next/Prev hyperlink", "Use Soft Wraps", and "Scroll to End"
        AnAction[] actions = consoleView.createConsoleActions();
        for (AnAction action : actions) {
            group.add(action);
        }

        toolbarPanel.add(ActionManager.getInstance().createActionToolbar("SbtConsoleToolbar", group, false).getComponent());
        return toolbarPanel;
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

    private class StartSbtAction extends AnAction {
        public StartSbtAction() {
            super("Start SBT", "Start SBT", IconLoader.getIcon("/general/toolWindowRun.png"));
        }

        @Override
        public void actionPerformed(AnActionEvent event) {
            try {
                runnerComponent.startIfNotStarted(false);
            } catch (IOException e) {
                logger.error("Failed to start SBT", e);
            }
        }

        @Override
        public void update(AnActionEvent event) {
            event.getPresentation().setEnabled(!runnerComponent.isSbtAlive());
        }
    }

    private class KillSbtAction extends AnAction {
        public KillSbtAction() {
            super("Kill SBT", "Forcibly kill the SBT process", IconLoader.getIcon("/debugger/killProcess.png"));
        }

        @Override
        public void actionPerformed(AnActionEvent event) {
            runnerComponent.destroyProcess();
        }

        @Override
        public void update(AnActionEvent event) {
            event.getPresentation().setEnabled(runnerComponent.isSbtAlive());
        }
    }
}
