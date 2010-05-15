// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public class MulticastPipeTest {
    private final MulticastPipe pipe = new MulticastPipe();

    @Test
    public void readers_see_what_is_written_to_the_pipe() throws IOException {
        Reader reader = pipe.subscribe();

        pipe.write('A');
        assertEquals('A', reader.read());
    }

    @Test
    public void multiple_concurrent_readers_will_all_see_the_same_data() throws IOException {
        Reader reader1 = pipe.subscribe();
        Reader reader2 = pipe.subscribe();

        pipe.write('A');
        assertEquals('A', reader1.read());
        assertEquals('A', reader2.read());
    }

    @Test
    public void readers_cannot_see_what_was_written_before_they_subscribed() throws IOException {
        pipe.write('A');
        Reader reader = pipe.subscribe();
        assertFalse("should not see data, but did", reader.ready());
    }

    @Test
    public void closed_readers_cannot_see_any_new_data_but_others_are_not_affected() throws IOException {
        Reader closedReader = pipe.subscribe();
        Reader openReader = pipe.subscribe();
        closedReader.close();

        pipe.write('A');

        try {
            closedReader.read();
            fail("reader not closed");
        } catch (IOException e) {
        }
        assertEquals('A', openReader.read());
    }
}
