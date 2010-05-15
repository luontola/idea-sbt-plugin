// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import java.io.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MulticastPipe extends Writer {

    private final List<PipedWriter> subscribers = new CopyOnWriteArrayList<PipedWriter>();

    public Reader subscribe() throws IOException {
        PipedReader r = new PipedReader();
        subscribers.add(new PipedWriter(r));
        return r;
    }

    private void unsubscribe(PipedWriter w) throws IOException {
        subscribers.remove(w);
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        for (PipedWriter w : subscribers) {
            try {
                w.write(cbuf, off, len);
            } catch (IOException e) {
                e.printStackTrace();
                unsubscribe(w);
            }
        }
    }

    public void flush() throws IOException {
        for (PipedWriter w : subscribers) {
            try {
                w.flush();
            } catch (IOException e) {
                e.printStackTrace();
                unsubscribe(w);
            }
        }
    }

    public void close() throws IOException {
        for (PipedWriter w : subscribers) {
            try {
                w.close();
            } catch (IOException e) {
                e.printStackTrace();
                unsubscribe(w);
            }
        }
    }
}
