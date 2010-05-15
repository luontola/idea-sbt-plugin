// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import org.junit.*;

import java.io.*;

import static org.junit.Assert.*;

public class ProcessRunnerTest {

    private ProcessRunner process;

    @Before
    public void startProcess() throws IOException {
        process = new ProcessRunner(new File("."), "java", "-version");
        process.start();
    }

    @After
    public void killProcess() {
        if (process != null) {
            process.destroy();
        }
    }

    @Test
    public void waits_until_the_expected_output_is_printed() throws IOException {
        assertTrue(process.waitForOutput("Runtime Environment"));
    }

    @Test
    public void returns_false_if_the_expected_output_is_not_printed() throws IOException {
        assertFalse(process.waitForOutput("will not be found"));
    }

    @Test
    public void continues_parsing_the_output_from_where_it_was_left() throws IOException {
        assertTrue(process.waitForOutput("Runtime"));
        assertTrue(process.waitForOutput(" Environment"));
    }

    @Test
    public void can_skip_buffered_output_without_processing_it() throws IOException {
        assertTrue(process.waitForOutput("Runtime"));
        process.skipBufferedOutput();
        assertFalse(process.waitForOutput("Environment"));
    }

    @Test
    public void an_output_listener_can_see_what_the_process_prints() throws IOException {
        final StringBuilder listener = new StringBuilder();
        process.setOutputListener(new OutputListener() {
            public void append(char c) {
                listener.append(c);
            }
        });

        process.waitForOutput("Environ");
        process.skipBufferedOutput();

        String output = listener.toString();
        assertTrue(output, output.contains("Runtime Environment"));
    }
}
