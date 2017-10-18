package org.openrepose.gradle.plugins.linkchecker;

import java.util.ArrayList;
import java.util.Collection;

public class LinkCheckerPluginExtension {

    /**
     * The file to start from. Links from the file will be checked. Non-URL links (i.e. local files) will be taken for
     * further link checking (feels like recursion)
     */
    public String startFileName;

    /**
     * The file name to be used as the default, in case a (non-URL) link points to a folder. It's what happens on a web
     * server: requests for {@code http://foo/bar} will serve you (typically) {@code http://foo/bar/index.html}.
     */
    public String defaultFile = "index.html";

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
}
