// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.openapi.application.*;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.orfjackal.sbt.runner.*;

import java.io.*;
import java.util.Scanner;

@State(name = "SbtRunner", storages = {@Storage(id = "default", file = "$WORKSPACE_FILE$")})
public class SbtRunnerComponent extends AbstractProjectComponent implements PersistentStateComponent<SbtRunnerSettings> {

    private static final Logger LOG = Logger.getInstance(SbtRunnerComponent.class.getName());

    private SbtRunnerSettings settings;
    private SbtRunner sbt;

    public static SbtRunnerComponent getInstance(Project project) {
        return project.getComponent(SbtRunnerComponent.class);
    }

    protected SbtRunnerComponent(Project project) {
        super(project);
    }

    public SbtRunnerSettings getState() {
        return settings;
    }

    public void loadState(SbtRunnerSettings settings) {
        this.settings = settings;
    }

    public CompletionSignal executeInBackground(final String action) {
        final CompletionSignal signal = new CompletionSignal();
        signal.begin();

        queue(new Task.Backgroundable(myProject, MessageBundle.message("sbt.tasks.executing"), false) {
            public void run(ProgressIndicator indicator) {
                try {
                    LOG.info("Begin executing: " + action);
                    executeAndWait(action);
                    LOG.info("Done executing: " + action);

                    // TODO: detect if there was a compile error or similar failure, so that the following task would not be started

                    signal.success();
                } catch (IOException e) {
                    LOG.error("Failed to execute action \"" + action + "\". Maybe SBT was closed?", e);
                } finally {
                    signal.finished();
                }
            }
        });

        return signal;
    }

    private void queue(final Task.Backgroundable task) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            task.queue();
        } else {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                public void run() {
                    task.queue();
                }
            }, ModalityState.NON_MODAL);
        }
    }

    public void executeAndWait(String action) throws IOException {
        saveAllDocuments();
        startIfNotStarted();
        try {
            sbt.execute(action);

            // TODO: update target folders
            // org.jetbrains.idea.maven.project.MavenProjectsManager#updateProjectFolders
            // org.jetbrains.idea.maven.execution.MavenRunner#runBatch
            // org.jetbrains.idea.maven.execution.MavenRunner#updateTargetFolders

            // TODO: synchronize changes to file system

        } catch (IOException e) {
            destroyProcess();
            throw e;
        }
    }

    private static void saveAllDocuments() {
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            public void run() {
                FileDocumentManager.getInstance().saveAllDocuments();
            }
        }, ModalityState.NON_MODAL);
    }

    private void startIfNotStarted() throws IOException {
        if (sbt == null || !sbt.isAlive()) {
            sbt = new SbtRunner(projectDir(), launcherJar());
            printToMessageWindow();
//            printToLogFile();
            sbt.start();
        }
    }

    private File projectDir() {
        VirtualFile baseDir = myProject.getBaseDir();
        assert baseDir != null;
        return new File(baseDir.getPath());
    }

    private File launcherJar() {
        // TODO: make this configurable
        return new File(System.getProperty("user.home"), "bin/sbt-launch.jar");
    }

    private void printToMessageWindow() {
        // org.jetbrains.idea.maven.execution.MavenExecutor#myConsole
        SbtConsole console = new SbtConsole(MessageBundle.message("sbt.tasks.action"), myProject);
        SbtProcessHandler process = new SbtProcessHandler(this, sbt.subscribeToOutput());
        console.attachToProcess(process);
        process.startNotify();
    }

    private void printToLogFile() {
        final OutputReader output = sbt.subscribeToOutput();
        Thread t = new Thread(new Runnable() {
            public void run() {
                Scanner scanner = new Scanner(output);
                while (scanner.hasNextLine()) {
                    LOG.info(scanner.nextLine());
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void destroyProcess() {
        if (sbt != null) {
            sbt.destroy();
            sbt = null;
        }
    }
}
