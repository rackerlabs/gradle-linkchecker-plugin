package org.openrepose.gradle.plugins.linkchecker;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.gradle.api.logging.LogLevel.WARN;

public class LinkCheckerPluginTask extends DefaultTask {

    static final Logger log = Logging.getLogger(LinkCheckerPluginTask.class);

    private LinkCheckerPluginExtension linkCheckerPluginExtension;

    public void setExtension(LinkCheckerPluginExtension extension) {
        linkCheckerPluginExtension = extension;
    }

    @InputDirectory
    public File getInputDirectory() {
        return linkCheckerPluginExtension.inputDir;
    }

    @OutputFile
    public File getLogFile() {
        return linkCheckerPluginExtension.logFile;
    }

    /**
     * Executes this task.
     */
    @TaskAction
    void linkcheckerTask() throws IOException, LinkCheckerPluginException {
        try (PrintWriter printWriter = new PrintWriter(new FileWriter(linkCheckerPluginExtension.logFile))) {
            logMsg(WARN, printWriter, "This task can take some time to complete.");
            logMsg(WARN, printWriter, "Run with --info or --debug for more information.");
            logMsg(WARN, printWriter, "");

            Multimap<String, File> linksToSourceFiles = HashMultimap.create();
            List<String> badLinks = new ArrayList<>();

            File absoluteStart = linkCheckerPluginExtension.startFile;
            if (!absoluteStart.isAbsolute()) {
                absoluteStart = new File(linkCheckerPluginExtension.inputDir, linkCheckerPluginExtension.startFile.getPath());
            }

            int total = LinkChecker.checkLinks(
                    printWriter,
                    absoluteStart,
                    linkCheckerPluginExtension.defaultFile,
                    linkCheckerPluginExtension.failOnLocalHost,
                    linkCheckerPluginExtension.failOnIgnoreHost,
                    linkCheckerPluginExtension.failOnBadUrls,
                    linkCheckerPluginExtension.httpURLConnectionTimeout,
                    linkCheckerPluginExtension.ignoreHostRegexs,
                    linksToSourceFiles,
                    badLinks
            );

            logMsg(WARN, printWriter, "");
            logMsg(WARN, printWriter, "Processed {} files with {} bad links.", total, badLinks.size());
            if (!badLinks.isEmpty()) {
                for (String badLink : badLinks) {
                    logMsg(WARN, printWriter, "\t" + badLink);
                    Collection<File> sourceFiles = linksToSourceFiles.get(badLink);
                    if (!sourceFiles.isEmpty()) {
                        logMsg(WARN, printWriter, "\tbad link referenced from:");
                        for (File sourceFile : sourceFiles) {
                            logMsg(WARN, printWriter, "\t\t" + sourceFile);
                        }
                    }
                }
                if (linkCheckerPluginExtension.reportOnly) {
                    logMsg(WARN, printWriter, "Not failing build for bad links as configured");
                } else {
                    throw new LinkCheckerPluginException("Failing build for bad links as configured.");
                }
                printWriter.close();
            }
        } catch (IOException e) {
            log.info("Failed to write output!", e);
            throw new LinkCheckerPluginException(
                    String.format(
                            "Failing build for bad log file: %s",
                            linkCheckerPluginExtension.logFile.getAbsolutePath()
                    ), e);
        }
    }

    static void logMsg(LogLevel level, PrintWriter printWriter, String msg) {
        log.log(level, msg);
        if (printWriter != null && log.isEnabled(level)) {
            printWriter.println(msg);
        }
    }

    static void logMsg(LogLevel level, PrintWriter printWriter, String format, Object... args) {
        log.log(level, format, args);
        if (printWriter != null && log.isEnabled(level)) {
            printWriter.printf(format.replace("{}", "%s"), args);
            printWriter.println();
        }
    }
}
