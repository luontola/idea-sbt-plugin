// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.openapi.application.*;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
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
            LOG.error("Failed to execute action: " + action, e);
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
        if (sbt == null) {
            sbt = new SbtRunner(projectDir(), launcherJar());
            printToMessageWindow();
            printToLogFile();
            sbt.start();
        }
    }

    private File projectDir() {
        VirtualFile baseDir = myProject.getBaseDir();
        assert baseDir != null;
        return new File(baseDir.getPath());
    }

    private File launcherJar() {
        return new File(System.getProperty("user.home"), "bin/sbt-launch.jar");
    }

    private void printToMessageWindow() {
        // org.jetbrains.idea.maven.execution.MavenExecutor#myConsole
        SbtConsole console = new SbtConsole(MessageBundle.message("sbt.tasks.action"), myProject);
        SbtProcessHandler process = new SbtProcessHandler(sbt);
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

    private void destroyProcess() {
        if (sbt != null) {
            sbt.destroy();
            sbt = null;
        }
    }
}
