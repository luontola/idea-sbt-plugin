// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import java.io.*;
import java.util.*;

public class OutputReader extends FilterReader {

    public static final boolean FOUND = true;
    public static final boolean END_OF_OUTPUT = false;
    private static final int BUFFER_SIZE = 1024;
    private CyclicCharBuffer buffer = new CyclicCharBuffer(BUFFER_SIZE);

    public OutputReader(Reader output) {
        super(output);
    }

    public boolean waitForOutput(String expected) throws IOException {
        return waitForOutput(Arrays.asList(expected));
    }

    public boolean waitForOutput(Collection<String> expected) throws IOException {
        int max = 0;
        for (String s : expected) {
            checkExpectedLength(s);
            max = Math.max(max, s.length());
        }
        int ch;
        while ((ch = read()) != -1) {
            buffer.append((char) ch);
            for (String s : expected) {
                if (buffer.contentEndsWith(s)) {
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

    public boolean endOfOutputContains(String expected) {
        checkExpectedLength(expected);
        return buffer.toString().contains(expected);
    }

    private void checkExpectedLength(String expected) {
        if (expected.length() > BUFFER_SIZE) {
            throw new IllegalArgumentException("expected string is too long.");
        }
    }
}
