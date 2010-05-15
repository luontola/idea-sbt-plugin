// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import org.junit.*;

import java.io.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class ProcessRunnerTest {

    private ProcessRunner process;

    @Before
    public void newProcess() throws IOException {
        process = new ProcessRunner(new File("."), "java", "-version");
    }

    @After
    public void killProcess() {
        if (process != null) {
            process.destroy();
        }
    }

    @Test
    public void waits_until_the_expected_output_is_printed() throws IOException {
        OutputReader output = process.subscribeToOutput();
        process.start();

        assertTrue(output.waitForOutput("Runtime Environment"));
    }

    @Test
    public void returns_false_if_the_expected_output_is_not_printed() throws IOException {
        OutputReader output = process.subscribeToOutput();
        process.start();

        assertFalse(output.waitForOutput("will not be found"));
    }

    @Test
    public void continues_parsing_the_output_from_where_it_was_left() throws IOException {
        OutputReader output = process.subscribeToOutput();
        process.start();

        assertTrue(output.waitForOutput("Runtime"));
        assertTrue(output.waitForOutput(" Environment"));
    }

    @Test
    public void can_skip_buffered_output_without_processing_it() throws IOException {
        OutputReader output = process.subscribeToOutput();
        process.start();

        assertTrue(output.waitForOutput("Runtime"));
        output.skipBufferedOutput();
        assertFalse(output.waitForOutput("Environment"));
    }

    @Test
    public void an_observer_can_see_what_the_process_prints_while_there_are_other_readers() throws IOException {
        OutputReader observer = process.subscribeToOutput();
        OutputReader otherReader = process.subscribeToOutput();
        process.start();

        otherReader.waitForOutput("Environ");
        otherReader.skipBufferedOutput();

        String output = readFullyAsString(observer);
        assertTrue(output, output.contains("Runtime Environment"));
    }

    @Test
    public void if_two_threads_wait_concurrently_then_both_of_them_will_read_the_same_output() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Boolean> t1 = executor.submit(new WaitForOutput("Runtime", process.subscribeToOutput()));
        Future<Boolean> t2 = executor.submit(new WaitForOutput("Runtime", process.subscribeToOutput()));
        process.start();

        assertTrue("Thread 1 did not read it", t1.get());
        assertTrue("Thread 2 did not read it", t2.get());
    }


    private static String readFullyAsString(Reader source) throws IOException {
        StringWriter result = new StringWriter();
        char[] buf = new char[1024];
        int len;
        while ((len = source.read(buf)) != -1) {
            result.write(buf, 0, len);
        }
        return result.toString();
    }

    private static class WaitForOutput implements Callable<Boolean> {
        private final String expected;
        private final OutputReader handle;

        public WaitForOutput(String expected, OutputReader handle) {
            this.expected = expected;
            this.handle = handle;
        }

        public Boolean call() throws Exception {
            return handle.waitForOutput(expected);
        }
    }
}
