package net.orfjackal.sbt.plugin.sbtlang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SbtFileType extends LanguageFileType {
    public static final LanguageFileType INSTANCE = new SbtFileType();

    private SbtFileType() {
        super(SbtLanguage.INSTANCE);
    }

    @NotNull
    public String getName() {
        return "_SBT";
    }

    @NotNull
    public String getDescription() {
        return "Internal file type for idea-sbt-plugin";
    }

    @NotNull
    public String getDefaultExtension() {
        return "__sbt";
    }

    @Nullable
    public Icon getIcon() {
        return null;
    }
}
