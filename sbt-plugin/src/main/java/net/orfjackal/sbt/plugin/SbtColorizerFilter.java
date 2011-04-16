// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package net.orfjackal.sbt.plugin;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.highlighter.custom.CustomHighlighterColors;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Rather than reading the ANSI output of SBT, we just highlight with some regexes.
 */
public class SbtColorizerFilter implements Filter, DumbAware {

    private static final String ERROR_PREFIX = "[error]";
    private static final String INFO_PREFIX = "[info]";
    private static final String SUCCESS_PREFIX = "[success]";

    // TODO Add a custom ColorSettingsPage for the SBT console.
    private static final TextAttributesKey GREEN = CustomHighlighterColors.CUSTOM_STRING_ATTRIBUTES;
    private static final TextAttributesKey BLUE = CodeInsightColors.TODO_DEFAULT_ATTRIBUTES;
    private static final TextAttributesKey RED = CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES;
    private static final TextAttributesKey YELLOW = CodeInsightColors.WARNINGS_ATTRIBUTES;

    public Result applyFilter(final String line, final int textEndOffset) {
        if (line == null) {
            return null;
        }
        final int textStartOffset = textEndOffset - line.length();
        if (line.startsWith(ERROR_PREFIX)) {
            if (line.matches("\\Q" + ERROR_PREFIX + "\\E\\s+x \\S.*\\n")) {
                return new Result(textStartOffset, textEndOffset, null, getAttributes(RED));
            } else {
                return new Result(textStartOffset + 1, textStartOffset + ERROR_PREFIX.length() - 1, null, getAttributes(RED));
            }
        } else if (line.startsWith(SUCCESS_PREFIX)) {
            return new Result(textStartOffset + 1, textStartOffset + SUCCESS_PREFIX.length() - 1, null, getAttributes(GREEN));
        } else if (line.startsWith(INFO_PREFIX)) {
            if (line.matches("\\Q" + INFO_PREFIX + "\\E ==.*==.*\\n")) {
                return new Result(textStartOffset + INFO_PREFIX.length() + 1, textEndOffset, null, getAttributes(BLUE));
            } else if (line.matches("\\Q" + INFO_PREFIX + "\\E\\s+\\+ \\S.*\\n")) {
                return new Result(textStartOffset + INFO_PREFIX.length() + 1, textEndOffset, null, getAttributes(GREEN));
            } else if (line.matches("\\Q" + INFO_PREFIX + "\\E\\s+o \\S.*\\n")) {
                return new Result(textStartOffset + INFO_PREFIX.length() + 1, textEndOffset, null, getAttributes(YELLOW));
            }
        }
        return null;
    }

    private TextAttributes getAttributes(TextAttributesKey green) {
        return EditorColorsManager.getInstance().getGlobalScheme().getAttributes(green);
    }
}