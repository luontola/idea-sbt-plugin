// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import java.io.*;

public class ReaderToWriterCopier implements Runnable {
    private final Reader source;
    private final Writer target;

    public ReaderToWriterCopier(Reader source, Writer target) {
        this.source = source;
        this.target = target;
    }

    public void run() {
        try {
            char[] buf = new char[1024];
            int len;
            while ((len = source.read(buf)) != -1) {
                target.write(buf, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                target.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
