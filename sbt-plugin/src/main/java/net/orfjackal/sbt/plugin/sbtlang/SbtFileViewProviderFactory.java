// Copyright © 2010-2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin.sbtlang;

import com.intellij.lang.Language;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.FileViewProviderFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;

public class SbtFileViewProviderFactory implements FileViewProviderFactory {

  public FileViewProvider createFileViewProvider(VirtualFile file, Language language, PsiManager manager, boolean physical) {
    return new SingleRootFileViewProvider(manager, file, physical, language.getBaseLanguage()) {


    };
  }
}
