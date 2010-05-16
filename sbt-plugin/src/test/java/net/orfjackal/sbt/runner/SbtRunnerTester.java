// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import java.io.*;
import java.util.Scanner;

public class SbtRunnerTester {

    private static final File LAUNCHER_JAR = new File(System.getProperty("user.home"), "bin/sbt-launch.jar");
    private static final File WORKING_DIR = new File("/tmp");

    private static SbtRunner sbt;

    public static void main(String[] args) throws Exception {
        sbt = new SbtRunner(WORKING_DIR, LAUNCHER_JAR);
        OutputReader output = sbt.subscribeToOutput();
        sbt.start();

        Thread t = new Thread(new Runnable() {
            public void run() {
                inputLoop(System.in);
            }
        });
        t.setDaemon(true);
        t.start();

        printLoop(output, System.out);
    }

    private static void inputLoop(InputStream source) {
        Scanner in = new Scanner(source);
        while (true) {
            String action = in.nextLine();
            if (action.equals("force-exit")) {
                System.exit(1);
            }
            executeAsynchronously(action);
        }
    }

    private static void executeAsynchronously(final String action) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    sbt.execute(action);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static void printLoop(Reader source, PrintStream target) throws IOException {
        int ch;
        while ((ch = source.read()) != -1) {
            target.print((char) ch);
        }
    }
}
