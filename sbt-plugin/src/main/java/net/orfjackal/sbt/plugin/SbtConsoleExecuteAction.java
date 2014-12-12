// Copyright © 2010-2014, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.console.LanguageConsoleImpl;
import com.intellij.execution.process.ConsoleHistoryModel;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author pfnguyen
 */
public class SbtConsoleExecuteAction extends AnAction {
    private static final Logger logger = Logger.getInstance(SbtConsoleExecuteAction.class.getName());
    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null || !(editor instanceof EditorEx)) {
            e.getPresentation().setEnabled(false);
            return;
        }
        LanguageConsoleImpl console = SbtConsoleInfo.getConsole(editor);
        if (console == null)  {
            e.getPresentation().setEnabled(false);
            return;
        }
        boolean isEnabled = !((EditorEx)editor).isRendererMode() &&
                !SbtConsoleInfo.getHandler(editor).isProcessTerminated();

        e.getPresentation().setEnabled(isEnabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        final LanguageConsoleImpl console = SbtConsoleInfo.getConsole(editor);
        final ProcessHandler processHandler = SbtConsoleInfo.getHandler(editor);
        final ConsoleHistoryModel model = SbtConsoleInfo.getModel(editor);
        if (console != null && processHandler != null && model != null) {
            final Document document = console.getEditorDocument();
            final String text = document.getText();

            // Process input and add to history

            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                public void run() {
                    TextRange range = new TextRange(0, document.getTextLength());
                    editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
                    console.addToHistory(range, console.getConsoleEditor(), true);
                    model.addToHistory(text);

                    editor.getCaretModel().moveToOffset(0);
                    editor.getDocument().setText("");
                }
            });

            for (String line : text.split("\n")) {
                if (!line.equals("")) {
                    OutputStream outputStream = processHandler.getProcessInput();
                    try {
                        byte[] bytes = (line + "\n").getBytes("utf-8");
                        outputStream.write(bytes);
                        outputStream.flush();
                    } catch (IOException ex) {
                        // noop
                    }
                }
//                console.textSent(line + "\n");
            }
        } else {
            logger.info(new Throwable("Enter action in console failed: $editor, " +
                    "$console"));
        }
    }
}
