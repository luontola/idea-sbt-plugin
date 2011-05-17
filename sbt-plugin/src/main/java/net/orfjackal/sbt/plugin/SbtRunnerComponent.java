// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import net.orfjackal.sbt.plugin.settings.SbtApplicationSettingsComponent;
import net.orfjackal.sbt.plugin.settings.SbtProjectSettingsComponent;
import net.orfjackal.sbt.runner.OutputReader;
import net.orfjackal.sbt.runner.SbtRunner;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

public class SbtRunnerComponent extends AbstractProjectComponent implements DumbAware {

    private static final Logger logger = Logger.getInstance(SbtRunnerComponent.class.getName());
    private static final boolean DEBUG = false;
    private static final String SBT_CONSOLE_TOOL_WINDOW_ID = "SBT Console";

    private SbtRunner sbt;
    private SbtConsole console;
    private final SbtProjectSettingsComponent projectSettings;
    private final SbtApplicationSettingsComponent applicationSettings;

    public static SbtRunnerComponent getInstance(Project project) {
        return project.getComponent(SbtRunnerComponent.class);
    }

    protected SbtRunnerComponent(Project project,
                                 SbtProjectSettingsComponent projectSettings,
                                 SbtApplicationSettingsComponent applicationSettings) {
        super(project);
        this.projectSettings = projectSettings;
        this.applicationSettings = applicationSettings;
        console = createConsole(project);
    }

    public CompletionSignal executeInBackground(final String action) {
        final CompletionSignal signal = new CompletionSignal();
        signal.begin();

        queue(new Task.Backgroundable(myProject, MessageBundle.message("sbt.tasks.executing"), false) {
            public void run(ProgressIndicator indicator) {
                try {
                    logger.info("Begin executing: " + action);
                    if (executeAndWait(action)) {
                        signal.success();
                        logger.info("Done executing: " + action);
                    } else {
                        logger.info("Error executing: " + action);
                    }
                } catch (IOException e) {
                    logger.error("Failed to execute action \"" + action + "\". Maybe SBT failed to start?", e);
                } finally {
                    signal.finished();
                }
            }
        });

        return signal;
    }

    @Override
    public void projectOpened() {
        final StartupManager manager = StartupManager.getInstance(myProject);
        manager.registerPostStartupActivity(new DumbAwareRunnable() {
            public void run() {
                registerToolWindow();
            }
        });
    }

    @Override
    public void disposeComponent() {
        unregisterToolWindow();
        destroyProcess();
    }

    public void recreateToolWindow() {
        unregisterToolWindow();
        console = createConsole(myProject);
        registerToolWindow();
    }

    private SbtConsole createConsole(Project project) {
        return new SbtConsole(MessageBundle.message("sbt.tasks.action"), project, this);
    }

    private void registerToolWindow() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
        if (toolWindowManager != null) {
            ToolWindow toolWindow =
                    toolWindowManager.registerToolWindow(SBT_CONSOLE_TOOL_WINDOW_ID, false, ToolWindowAnchor.BOTTOM, myProject, true);
            SbtRunnerComponent.getInstance(myProject).getConsole().attachToToolWindow(toolWindow);
        }
    }

    private void unregisterToolWindow() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
        if (toolWindowManager != null && toolWindowManager.getToolWindow(SBT_CONSOLE_TOOL_WINDOW_ID) != null) {
          toolWindowManager.unregisterToolWindow(SBT_CONSOLE_TOOL_WINDOW_ID);
        }
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

    /**
     * @param action the SBT action to run
     * @return false if an error was detected, true otherwise
     * @throws IOException
     */
    public boolean executeAndWait(String action) throws IOException {
        saveAllDocuments();
        startIfNotStarted(true);
        boolean success;
        try {
            success = sbt.execute(action);

            // TODO: update target folders (?)
            // org.jetbrains.idea.maven.project.MavenProjectsManager#updateProjectFolders
            // org.jetbrains.idea.maven.execution.MavenRunner#runBatch
            // org.jetbrains.idea.maven.execution.MavenRunner#updateTargetFolders
        } catch (IOException e) {
            destroyProcess();
            throw e;
        }

        VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);

        if (projectSettings.getState().isUseSbtOutputDirs()) {
            configureOutputDirs();
        }
        return success;
    }

    private static void saveAllDocuments() {
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            public void run() {
                FileDocumentManager.getInstance().saveAllDocuments();
            }
        }, ModalityState.NON_MODAL);
    }

    public final SbtConsole getConsole() {
        return console;
    }

    public final void startIfNotStarted(boolean wait) throws IOException {
        if (!isSbtAlive()) {
            sbt = new SbtRunner(projectDir(), launcherJar(), vmParameters());
            printToMessageWindow();
            if (DEBUG) {
                printToLogFile();
            }
            sbt.start(wait);
        }
    }

    public final boolean isSbtAlive() {
        return sbt != null && sbt.isAlive();
    }

    private File projectDir() {
        VirtualFile baseDir = myProject.getBaseDir();
        assert baseDir != null;
        return new File(baseDir.getPath());
    }

    private File launcherJar() {
        return new File(applicationSettings.getState().getSbtLauncherJarPath());
    }

    private String[] vmParameters() {
        return applicationSettings.getState().getSbtLauncherVmParameters().split("\\s");
    }

    private void printToMessageWindow() {
        // org.jetbrains.idea.maven.execution.MavenExecutor#myConsole
        SbtProcessHandler process = new SbtProcessHandler(this, sbt.subscribeToOutput());
        console.attachToProcess(process, this);
        process.startNotify();
    }

    private void printToLogFile() {
        final OutputReader output = sbt.subscribeToOutput();
        Thread t = new Thread(new Runnable() {
            public void run() {
                Scanner scanner = new Scanner(output);
                while (scanner.hasNextLine()) {
                    logger.info(scanner.nextLine());
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void configureOutputDirs() {
        // org.jetbrains.idea.maven.importing.MavenFoldersImporter#configOutputFolders
        VirtualFile buildConfig = getBuildPropertiesFile();

        /* The SBT Console can be used to configure SBT for projects that don't use it yet,
           so the build.properties file may not exist */
        if (buildConfig != null && buildConfig.exists()) {
            String scalaVersion = getScalaVersion(buildConfig);

            for (Module module : ModuleManager.getInstance(myProject).getModules()) {
                String moduleBaseDir = getModuleBaseDir(module);
                String compilerOutput = moduleBaseDir + "/target/scala_" + scalaVersion + "/classes";
                String compilerTestOutput = moduleBaseDir + "/target/scala_" + scalaVersion + "/test-classes";

                setModuleOutputDirs(module, compilerOutput, compilerTestOutput);
                // TODO: folders to exclude: project/boot, project/build/target, lib_managed, src_managed, target
            }
        }

        /*
        if (myImportingSettings.isUseMavenOutput()) {
            myModel.useModuleOutput(myMavenProject.getOutputDirectory(), myMavenProject.getTestOutputDirectory());
        }
        myModel.addExcludedFolder(myMavenProject.getOutputDirectory());
        myModel.addExcludedFolder(myMavenProject.getTestOutputDirectory());
        */
    }

    private String getModuleBaseDir(Module module) {
        VirtualFile moduleFile = module.getModuleFile();
        if (moduleFile == null) {
            throw new IllegalArgumentException("Module file not found: " + module);
        }
        return moduleFile.getParent().getUrl();
    }

    private VirtualFile getBuildPropertiesFile() {
        VirtualFile baseDir = myProject.getBaseDir();
        if (baseDir == null) {
            throw new IllegalArgumentException("Project base directory not found");
        }
        return baseDir.findFileByRelativePath("project/build.properties");
    }

    private String getScalaVersion(VirtualFile buildConfig) {
        if (!(buildConfig != null && buildConfig.exists())) {
            throw new IllegalArgumentException("project/build.properties does not exist at " + buildConfig);
        }

        Properties p = new Properties();
        try {
            p.load(buildConfig.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + buildConfig, e);
        }

        String scalaVersion = p.getProperty("build.scala.versions");
        if (scalaVersion == null) {
            throw new IllegalArgumentException("Scala version not found from " + buildConfig);
        }
        return scalaVersion;
    }

    private void setModuleOutputDirs(final Module module, final String compilerOutput, final String compilerTestOutput) {
        final Runnable configureModule = new Runnable() {
            public void run() {
                // org.jetbrains.idea.maven.importing.MavenRootModelAdapter#getCompilerExtension
                ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
                CompilerModuleExtension compiler = rootModel.getModuleExtension(CompilerModuleExtension.class);

                // org.jetbrains.idea.maven.importing.MavenRootModelAdapter#useModuleOutput
                compiler.inheritCompilerOutputPath(false);
                compiler.setCompilerOutputPath(compilerOutput);
                compiler.setCompilerOutputPathForTests(compilerTestOutput);

                rootModel.commit();
            }
        };
        final Application app = ApplicationManager.getApplication();
        app.invokeAndWait(new Runnable() {
            public void run() {
                app.runWriteAction(configureModule);
            }
        }, ModalityState.NON_MODAL);
    }

    public void destroyProcess() {
        if (sbt != null) {
            sbt.destroy();
            sbt = null;
        }
    }
}
