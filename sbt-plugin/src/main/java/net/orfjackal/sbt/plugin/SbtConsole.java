// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.console.ConsoleHistoryController;
import com.intellij.execution.console.LanguageConsoleImpl;
import com.intellij.execution.console.LanguageConsoleViewImpl;
import com.intellij.execution.filters.*;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ConsoleHistoryModel;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.AbstractConsoleRunnerWithHistory;
import com.intellij.execution.runners.ConsoleExecuteActionHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBScrollBar;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.ButtonlessScrollBarUI;
import net.orfjackal.sbt.plugin.sbtlang.SbtFileType;
import net.orfjackal.sbt.plugin.sbtlang.SbtLanguage;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
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
        this.consoleView = createConsoleView(project, this);
        this.runnerComponent = runnerComponent;
    }

    private static ConsoleView createConsoleView(Project project, SbtConsole sbtConsole) {
        boolean useClassicConsole = "true".equalsIgnoreCase(System.getProperty("idea.sbt.plugin.classic"));
        if (useClassicConsole) {
            return createTextConsole(project);
        } else {
            return createLanguageConsole(project, sbtConsole);
        }
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

    private static ConsoleView createLanguageConsole(final Project project, final SbtConsole sbtConsole) {
        final LanguageConsoleImpl sbtLanguageConsole = new LanguageConsoleImpl(project, "SBT", SbtLanguage.INSTANCE);
        sbtLanguageConsole.setShowSeparatorLine(false);
        enableLinkedHorizontalScrollFromHistoryViewer(sbtLanguageConsole);        

        // important to only have one history controller, even if SBT is restarted.
        final ConsoleHistoryController historyController = new ConsoleHistoryController("scala", null, sbtLanguageConsole, new ConsoleHistoryModel());
        historyController.install();

        LanguageConsoleViewImpl consoleView = new LanguageConsoleViewImpl(project, sbtLanguageConsole) {
            @Override
            public void attachToProcess(ProcessHandler processHandler) {
                super.attachToProcess(processHandler);
                ConsoleExecuteActionHandler executeActionHandler = new ConsoleExecuteActionHandler(processHandler, false) {
                    {
                        setConsoleHistoryModel(historyController.getModel());
                    }

                    @Override
                    public void runExecuteAction(final LanguageConsoleImpl languageConsole) {
                        EditorEx consoleEditor = languageConsole.getConsoleEditor();
                        consoleEditor.setCaretEnabled(false);
                        super.runExecuteAction(languageConsole);
                        // hide the prompts until the command has completed.
                        languageConsole.setPrompt("  ");
                    }
                };
                // SBT echos the command, don't add it to the output a second time.
                executeActionHandler.setAddCurrentToHistory(true);
                AnAction action = AbstractConsoleRunnerWithHistory.createConsoleExecAction(sbtLanguageConsole, processHandler, executeActionHandler);
                action.registerCustomShortcutSet(action.getShortcutSet(), sbtLanguageConsole.getComponent());
            }

            @Override
            public boolean hasDeferredOutput() {
                return super.hasDeferredOutput();
            }
        };
        sbtLanguageConsole.setPrompt("  ");
        addFilters(project, consoleView);

        return consoleView;
    }

    private static void enableLinkedHorizontalScrollFromHistoryViewer(final LanguageConsoleImpl sbtLanguageConsole) {
        JBScrollBar horizontalScrollBar = new JBScrollBar(Adjustable.HORIZONTAL);
        horizontalScrollBar.setUI(new BasicScrollBarUI() {
            @Override
            public Dimension getPreferredSize(JComponent c) {
                return new Dimension(0, 0);
            }
        });
        sbtLanguageConsole.getHistoryViewer().getScrollPane().setHorizontalScrollBar(horizontalScrollBar);
        sbtLanguageConsole.getHistoryViewer().setHorizontalScrollbarVisible(true);

        final VisibleAreaListener areaListener = new VisibleAreaListener() {
            public void visibleAreaChanged(VisibleAreaEvent e) {
                final int offset = sbtLanguageConsole.getHistoryViewer().getScrollingModel().getHorizontalScrollOffset();
                final ScrollingModel model = sbtLanguageConsole.getConsoleEditor().getScrollingModel();
                final int historyOffset = model.getHorizontalScrollOffset();
                if (historyOffset != offset) {
                    try {
                        model.disableAnimation();
                        model.scrollHorizontally(offset);
                    } finally {
                        model.enableAnimation();
                    }
                }
            }
        };
        sbtLanguageConsole.getHistoryViewer().getScrollingModel().addVisibleAreaListener(areaListener);
    }

    public void enablePrompt() {
        if (consoleView instanceof LanguageConsoleViewImpl) {
            final LanguageConsoleViewImpl languageConsoleView = (LanguageConsoleViewImpl) consoleView;
            final LanguageConsoleImpl console = languageConsoleView.getConsole();

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    languageConsoleView.flushDeferredText();
                    ApplicationManagerEx.getApplication().runWriteAction(new Runnable() {
                        public void run() {
                            Document document = console.getHistoryViewer().getDocument();
                            EditorEx consoleEditor = console.getConsoleEditor();
                            if (deleteTextFromEnd(document, "\n> ", "\n")) {
                                console.setPrompt("> ");
                            } else if (deleteTextFromEnd(document, "\nscala> ", "\n")) {
                                console.setPrompt("scala> ");
                                // not sure why this works, but it moves the caret to the end of the prompt.
                                // without this, it apppears between the `c` and `a`.
                                consoleEditor.getCaretModel().moveCaretRelatively(-1, 0, false, false, false);
                            }
                            consoleEditor.setCaretEnabled(true);
                        }
                    });
                }
            });
        }
    }

    private static boolean deleteTextFromEnd(final Document document, String lastPrompt, final String replacement) {
        final int startOffset = document.getTextLength() - lastPrompt.length();
        if (startOffset > 0) {
            String text = document.getText(TextRange.create(startOffset, document.getTextLength()));
            if (text.equals(lastPrompt)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        document.replaceString(startOffset, document.getTextLength(), replacement);
                    }
                });
                return true;
            }
        }
        return false;
    }

    private static void addFilters(Project project, LanguageConsoleViewImpl consoleView) {
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
            super("Start SBT", "Start SBT", IconLoader.getIcon("/general/toolWindowRun.png"));
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
