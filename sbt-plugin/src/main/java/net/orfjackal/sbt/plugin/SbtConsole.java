// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.console.LanguageConsoleImpl;
import com.intellij.execution.filters.*;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import javax.swing.*;
import java.awt.*;
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
        // TODO can we figure out how to make this a LanguageConsole with IDEA 14.1+
        //      We need that for console history
        ConsoleView consoleView = createTextConsole(project);
        addFilters(project, consoleView);
        return consoleView;
    }

    private static ConsoleView createTextConsole(final Project project) {
        TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);

        final SbtColorizerFilter logLevelFilter = new SbtColorizerFilter();
        final ExceptionFilter exceptionFilter = new ExceptionFilter(GlobalSearchScope.allScope(project));
        final RegexpFilter regexpFilter = new RegexpFilter(project, CONSOLE_FILTER_REGEXP);
        for (Filter filter : Arrays.asList(exceptionFilter, regexpFilter, logLevelFilter)) {
            builder.addFilter(filter);
        }
        return builder.getConsole();
    }

    private static void addFilters(Project project, ConsoleView consoleView) {
        consoleView.addMessageFilter(new ExceptionFilter(GlobalSearchScope.allScope(project)));
        consoleView.addMessageFilter(new RegexpFilter(project, CONSOLE_FILTER_REGEXP));
        consoleView.addMessageFilter(new SbtColorizerFilter());
    }

    public boolean isFinished() {
        return finished;
    }

    public void finish() {
        finished = true;
    }

    public void attachToProcess(ProcessHandler processHandler, final SbtRunnerComponent runnerComponent) {
        consoleView.print(runnerComponent.getFormattedCommand() + "\n\n", ConsoleViewContentType.SYSTEM_OUTPUT);
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
                        ensureAttachedToToolWindow(window, true);
                    }
                });
            }

            public void processTerminated(ProcessEvent event) {
                finish();
            }
        });
    }

    public final void ensureAttachedToToolWindow(ToolWindow window, boolean activate) {
        if (!isOpen.compareAndSet(false, true)) {
            return;
        }
        attachToToolWindow(window);
        if (activate) {
            if (!window.isActive()) {
                window.activate(null, false);
            }
        }
    }

    public void attachToToolWindow(ToolWindow window) {
        // org.jetbrains.idea.maven.embedder.MavenConsoleImpl#ensureAttachedToToolWindow
        SimpleToolWindowPanel toolWindowPanel = new SimpleToolWindowPanel(false, true);
        JComponent consoleComponent = consoleView.getComponent();
        toolWindowPanel.setContent(consoleComponent);
        StartSbtAction startSbtAction = new StartSbtAction();
        toolWindowPanel.setToolbar(createToolbar(startSbtAction));
        startSbtAction.registerCustomShortcutSet(CommonShortcuts.getRerun(), consoleComponent);

        Content content = ContentFactory.SERVICE.getInstance().createContent(toolWindowPanel, title, true);
        content.putUserData(CONSOLE_KEY, SbtConsole.this);

        window.getContentManager().addContent(content);
        window.getContentManager().setSelectedContent(content);

        removeUnusedTabs(window, content);
    }

    private JComponent createToolbar(AnAction startSbtAction) {
        JPanel toolbarPanel = new JPanel(new GridLayout());

        DefaultActionGroup group = new DefaultActionGroup();

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

    public void scrollToEnd() {
        (((ConsoleViewImpl) consoleView)).scrollToEnd();
    }

    private class StartSbtAction extends DumbAwareAction {
        public StartSbtAction() {
            super("Start SBT", "Start SBT", IconLoader.getIcon("/toolwindows/toolWindowRun.png"));
        }

        @Override
        public void actionPerformed(AnActionEvent event) {
            runnerComponent.startIfNotStartedSafe(false);
        }

        @Override
        public void update(AnActionEvent event) {
            event.getPresentation().setEnabled(!runnerComponent.isSbtAlive());
        }
    }

    private class KillSbtAction extends DumbAwareAction {
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
