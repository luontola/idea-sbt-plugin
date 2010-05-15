// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import java.io.*;

public class OutputReader extends FilterReader {

    public static final boolean FOUND = true;
    public static final boolean END_OF_OUTPUT = false;

    public OutputReader(Reader output) {
        super(output);
    }

    public boolean waitForOutput(String expected) throws IOException {
        CyclicCharBuffer buffer = new CyclicCharBuffer(expected.length());
        int ch;
        while ((ch = read()) != -1) {
            buffer.append((char) ch);

            if (buffer.contentEquals(expected)) {
                return FOUND;
            }
        }
        return END_OF_OUTPUT;
    }

    public void skipBufferedOutput() throws IOException {
        while (ready()) {
            skip(1);
        }
    }
}
