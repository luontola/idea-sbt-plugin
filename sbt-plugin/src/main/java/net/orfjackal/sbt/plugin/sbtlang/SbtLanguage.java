// Copyright © 2010-2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin.sbtlang;


import com.intellij.lang.Language;
import com.intellij.lang.StdLanguages;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A language for input in the SBT console */
public class SbtLanguage extends Language {
    public SbtLanguage() {
        super(StdLanguages.TEXT, "SBT");
    }

    public static final Language INSTANCE = new SbtLanguage();
}
