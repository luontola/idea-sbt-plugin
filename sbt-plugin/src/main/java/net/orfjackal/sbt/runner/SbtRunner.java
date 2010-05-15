// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import java.io.*;
import java.util.Scanner;

public class SbtRunner {

    private static final String PROMPT = "\n> ";
    private static final String PROMPT_AFTER_EMPTY_COMMAND = "> ";

    private final ProcessRunner sbt;

    public SbtRunner(File workingDir, File launcherJar) {
        sbt = new ProcessRunner(workingDir, getCommand(launcherJar));
    }

    private static String[] getCommand(File launcherJar) {
        return new String[]{
                "java",
                "-Dsbt.log.noformat=true",
                "-Djline.terminal=jline.UnsupportedTerminal",
                "-jar", launcherJar.getAbsolutePath()
        };
    }

    public OutputReader subscribeToOutput() throws IOException {
        return sbt.subscribeToOutput();
    }

    public void start() throws IOException {
        // TODO: detect if the directory does not have a project
        OutputReader out = sbt.subscribeToOutput();
        sbt.start();
        out.waitForOutput(PROMPT);
        out.close();
    }

    public void destroy() {
        sbt.destroy();
    }

    public void execute(String action) throws IOException {
        OutputReader out = sbt.subscribeToOutput();
        sbt.writeInput(action + "\n");

        if (action.trim().isEmpty()) {
            out.waitForOutput(PROMPT_AFTER_EMPTY_COMMAND);
        } else {
            out.waitForOutput(PROMPT);
        }
        out.close();
    }

    public static void main(String[] args) throws Exception {
        File launcherJar = new File(System.getProperty("user.home"), "bin/sbt-launch.jar");
        final SbtRunner sbt = new SbtRunner(new File("/tmp"), launcherJar);

        final OutputReader output = sbt.subscribeToOutput();
        sbt.start();

        Thread printer = new Thread(new Runnable() {
            public void run() {
                try {
                    int ch;
                    while ((ch = output.read()) != -1) {
                        System.out.print((char) ch);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        printer.start();

        Scanner in = new Scanner(System.in);
        while (true) {
            final String action = in.nextLine();

            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        sbt.execute(action);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }
    }
}
