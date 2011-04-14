// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;

public class OutputReader extends FilterReader {

    public static final boolean FOUND = true;
    public static final boolean END_OF_OUTPUT = false;

    public OutputReader(Reader output) {
        super(output);
    }

    public boolean waitForOutput(String expected) throws IOException {
        return waitForOutput(Arrays.asList(expected));
    }

    public boolean waitForOutput(Collection<String> expected) throws IOException {
        int max = 0;
        for (String s : expected) {
            max = Math.max(max, s.length());
        }
        CyclicCharBuffer buffer = new CyclicCharBuffer(max);
        int ch;
        while ((ch = read()) != -1) {
            buffer.append((char) ch);
            for (String s : expected) {
                if (buffer.contentEquals(s)) {
                    return FOUND;
                }
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
