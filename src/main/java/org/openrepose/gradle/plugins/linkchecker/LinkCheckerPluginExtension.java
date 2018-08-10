package org.openrepose.gradle.plugins.linkchecker;

import org.gradle.api.Project;
import org.gradle.internal.impldep.org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class LinkCheckerPluginExtension {

    /**
     * The directory that will be used for UP-TO-DATE checks.
     * If the start file is not absolute, then it is assumed to be relative to here.
     */
    public File inputDir;

    /**
     * The file name to be used as the default, in case a (non-URL) link points to a folder. It's what happens on a web
     * server: requests for {@code http://foo/bar} will serve you (typically) {@code http://foo/bar/index.html}.
     */
    public String defaultFile = "index.html";

    /**
     * This is the starting point of link checking.
     * Non-URL links (i.e. local files) will be added for further link checking (feels like recursion).
     */
    public File startFile = new File(defaultFile);

    /**
     * Should this plugin make your build fail if it encounters links to {@code localhost}. Typically, depending on
     * something local to the build would hamper the portability of the build
     */
    public boolean failOnLocalHost = true;

    /**
     * Should this plugin make your build fail if it encounters links to an ignored host. This is not the default and is
     * typically only enabled if there are troublesome links that would normally require multi-party authentication to
     * access (e.g. SAML, OAuth).
     */
    public boolean failOnIgnoreHost = false;

    /**
     * Should this plugin make your build fail if it encounters bad URLs. This is not the default, in appreciation of
     * the fact that (non-local) URLs are out of our control. Typically, validating (non-local) URLs would hamper the
     * reproducibility of the build
     */
    public boolean failOnBadUrls = false;

    /**
     * Sets a specified timeout value, in milliseconds, to be used when opening a communications link to a non-local URL.
     * A timeout of zero is interpreted as an infinite timeout.
     * A timeout of less than zero is interpreted to use the system timeout.
     */
    public int httpURLConnectionTimeout = -1;

    /**
     * A list of regular expressions of hosts to not even attempt communications with.
     */
    public Collection<String> ignoreHostRegexs = new ArrayList<>();

    /**
     * Should this plugin make your build fail altogether, or only report its findings.
     */
    public boolean reportOnly;

    /**
     * This is the file where results are logged.
     */
    public File logFile;

    /**
     * HTTP request methods to check the URL against. Defaults to ['HEAD']
     */
    public List<String> requestMethods = Arrays.asList("HEAD");

    /**
     * This class should not be instantiated without any arguments.
     */
    private LinkCheckerPluginExtension() {
    }

    /**
     * Constructor that sets required defaults based on the project.
     */
    public LinkCheckerPluginExtension(@NotNull Project project) {
        logFile = new File(project.getBuildDir(), "reports/linkchecker.txt");
    }
}
