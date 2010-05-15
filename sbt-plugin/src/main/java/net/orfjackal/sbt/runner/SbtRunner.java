// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import java.io.*;
import java.util.Scanner;

public class SbtRunner {

    private static final String PROMPT = "\n> ";

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

    public void setOutputListener(OutputListener listener) {
        sbt.setOutputListener(listener);
    }

    public void start() throws IOException {
        // TODO: detect if the directory does not have a project
        sbt.start();
        sbt.waitForOutput(PROMPT);
    }

    public void destroy() {
        sbt.destroy();
    }

    public void execute(String action) throws IOException {
        sbt.skipBufferedOutput();
        sbt.writeInput(action + "\n");
        System.out.println("<wait>");
        sbt.waitForOutput(PROMPT); // TODO: can hang when executing empty commands or ~ commands
        System.out.println("</wait>");
        sbt.skipBufferedOutput();
    }

    public static void main(String[] args) throws Exception {
        File launcherJar = new File(System.getProperty("user.home"), "bin/sbt-launch.jar");
        final SbtRunner sbt = new SbtRunner(new File("/tmp"), launcherJar);

        sbt.setOutputListener(new OutputListener() {
            public void append(char c) {
                System.out.print(c);
            }
        });
        sbt.start();

        Scanner in = new Scanner(System.in);
        while (true) {
            // TODO: figure out a reliable way to send commands
            final String action = in.nextLine();

            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        System.out.println("action = " + action);
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
