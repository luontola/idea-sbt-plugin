// Copyright © 2010-2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.util.PathUtil;

import java.io.File;

public class IO {
    public static String canonicalPathTo(File file) {
        return PathUtil.getCanonicalPath(file.getAbsolutePath());
    }

    public static String absolutePath(String path) {
        return new File(path).getAbsolutePath();
    }
}
