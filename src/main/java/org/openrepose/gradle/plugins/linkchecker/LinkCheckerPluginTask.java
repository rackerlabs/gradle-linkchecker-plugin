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

        log.info("Checking links relative to: " + linkCheckerPluginExtension.startFileName + " (recursively)");

        Multimap<String, File> linksToSourceFiles = HashMultimap.create();
        List<String> badLinks = new ArrayList<>();

        int total = LinkChecker.checkLinks(
                linkCheckerPluginExtension.startFileName,
                linkCheckerPluginExtension.defaultFile,
                linkCheckerPluginExtension.failOnLocalHost,
                linkCheckerPluginExtension.failOnBadUrls,
                linksToSourceFiles,
                badLinks
        );

        log.info("");
        if (badLinks.isEmpty()) {
            log.info("no bad links");
        } else {
            log.info(badLinks.size() + " bad links:");
            for (String badLink : badLinks) {
                log.info("\t" + badLink);
                Collection<File> sourceFiles = linksToSourceFiles.get(badLink);
                if (!sourceFiles.isEmpty()) {
                    log.info("\tbad link referenced from:");
                    for (File sourceFile : sourceFiles) {
                        log.info("\t\t" + sourceFile);
                    }
                }
            }
        }
        log.info("");
        log.info("Processed " + total + " files");

        if (!badLinks.isEmpty()) {
            log.warn(badLinks.size() + " bad links.");
            if(!log.isInfoEnabled()) {
                log.warn("Run with --info for the list.");
            }
            if (linkCheckerPluginExtension.reportOnly) {
                log.warn("Not failing build for bad links as configured");
            } else {
                throw new LinkCheckerPluginException("Failing build for bad links as configured.");
            }
        }
    }
}
