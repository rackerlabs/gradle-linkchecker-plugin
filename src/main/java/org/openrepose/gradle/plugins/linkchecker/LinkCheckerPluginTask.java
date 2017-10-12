package org.openrepose.gradle.plugins.linkchecker;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LinkCheckerPluginTask extends DefaultTask {

    static final Logger log = Logging.getLogger(LinkCheckerPluginTask.class);

    /**
     * Executes this task.
     */
    @TaskAction
    void linkcheckerTask() throws IOException, LinkCheckerPluginException {
        LinkCheckerPluginExtension linkCheckerPluginExtension = (LinkCheckerPluginExtension) (getProject().getExtensions().getByName("linkchecker"));

        log.warn("This task can take some time to complete.");
        log.warn("Run with --info for more information.");

        Multimap<String, File> linksToSourceFiles = HashMultimap.create();
        List<String> badLinks = new ArrayList<>();

        int total = LinkChecker.checkLinks(
                linkCheckerPluginExtension.startFileName,
                linkCheckerPluginExtension.defaultFile,
                linkCheckerPluginExtension.failOnLocalHost,
                linkCheckerPluginExtension.failOnBadUrls,
                linkCheckerPluginExtension.httpURLConnectionTimeout,
                linksToSourceFiles,
                badLinks
        );

        log.warn("");
        if (badLinks.isEmpty()) {
            log.warn("no bad links");
        } else {
            log.warn(badLinks.size() + " bad links:");
            for (String badLink : badLinks) {
                log.warn("\t" + badLink);
                Collection<File> sourceFiles = linksToSourceFiles.get(badLink);
                if (!sourceFiles.isEmpty()) {
                    log.warn("\tbad link referenced from:");
                    for (File sourceFile : sourceFiles) {
                        log.warn("\t\t" + sourceFile);
                    }
                }
            }
        }

        log.warn("Processed {} files", total);
        log.warn(badLinks.size() + " bad links.");

        if (!badLinks.isEmpty()) {
            if (linkCheckerPluginExtension.reportOnly) {
                log.warn("Not failing build for bad links as configured");
            } else {
                throw new LinkCheckerPluginException("Failing build for bad links as configured.");
            }
        }
    }
}
