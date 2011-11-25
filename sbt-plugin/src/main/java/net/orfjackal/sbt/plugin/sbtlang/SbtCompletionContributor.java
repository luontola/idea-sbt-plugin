// Copyright © 2010-2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin.sbtlang;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageWordCompletion;
import com.intellij.openapi.project.DumbService;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PlainTextTokenTypes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.patterns.StandardPatterns.character;

// TODO get completion variants from SBT over a telnet-like interface
// TODO target this contributor to the SBT console in a more principled manner (rather than using the getContainingFile.getName == SbtLanguage.INSTACNCE.getID)

// Adapted from WordCompletionContributor in IDEA core.
public class SbtCompletionContributor extends CompletionContributor {

    @Override
    public void fillCompletionVariants(final CompletionParameters parameters, final CompletionResultSet result) {
        if (parameters.getCompletionType() == CompletionType.BASIC && shouldPerformWordCompletion(parameters)) {
            addWordCompletionVariants(result, parameters, Collections.<String>emptySet());
        }
    }

    public static void addWordCompletionVariants(CompletionResultSet result, CompletionParameters parameters, Set<String> excludes) {
        int startOffset = parameters.getOffset();
        PsiElement insertedElement = parameters.getPosition();
        // "test-only *A<CTRL-SPACE> -- opta" gives a prefix of "test-only *A"
        String prefix = insertedElement.getText().substring(0, startOffset);

        final CompletionResultSet plainResultSet = result.withPrefixMatcher(prefix);
        List<String> strings = Arrays.asList("compile", "run", "test:compile", "test", "project", "projects", "inspect", "show", "last", "last-grep");
        for (String string : strings) {
            final LookupElement item = LookupElementBuilder.create(string);
            plainResultSet.addElement(item);
        }
    }

    private static boolean shouldPerformWordCompletion(CompletionParameters parameters) {
        final PsiElement insertedElement = parameters.getPosition();
        final boolean dumb = DumbService.getInstance(insertedElement.getProject()).isDumb();
        if (dumb) {
            return true;
        }
        // TODO Hackedy-hack.
        if (!insertedElement.getContainingFile().getName().equals("SBT")) {
            return false;
        }

        if (parameters.getInvocationCount() == 0) {
            return false;
        }

        final PsiFile file = insertedElement.getContainingFile();

        final int startOffset = parameters.getOffset();

        final PsiElement element = file.findElementAt(startOffset - 1);

        ASTNode textContainer = element != null ? element.getNode() : null;
        while (textContainer != null) {
            final IElementType elementType = textContainer.getElementType();
            if (LanguageWordCompletion.INSTANCE.isEnabledIn(elementType) || elementType == PlainTextTokenTypes.PLAIN_TEXT) {
                return true;
            }
            textContainer = textContainer.getTreeParent();
        }
        return false;
    }
}
