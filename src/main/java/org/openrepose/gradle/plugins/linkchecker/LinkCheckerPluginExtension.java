package org.openrepose.gradle.plugins.linkchecker;

public class LinkCheckerPluginExtension {

    /**
     * The file to start from. Links from the file will be checked. Non-URL links (i.e. local files) will be taken for
     * further link checking (feels like recursion)
     */
    String startFileName;

    /**
     * The file name to be used as the default, in case a (non-URL) link points to a folder. It's what happens on a web
     * server: requests for {@code http://foo/bar} will serve you (typically) {@code http://foo/bar/index.html}.
     */
    String defaultFile = "index.html";

    /**
     * Should this plugin make your build fail if it encounters links to {@code localhost}. Typically, depending on
     * something local to the build would hamper the portability of the build
     */
    boolean failOnLocalHost = true;

    /**
     * Should this plugin make your build fail if it encounters bad URLs. This is not the default, in appreciation of
     * the fact that (non-local) URLs are out of our control. Typically, validating (non-local) URLs would hamper the
     * reproducibility of the build
     */
    boolean failOnBadUrls = false;

    /**
     * Should this plugin make your build fail altogether, or only report its findings.
     */
    boolean reportOnly;

    public String getStartFileName() {
        return startFileName;
    }

    public void setStartFileName(String startFileName) {
        this.startFileName = startFileName;
    }

    public String getDefaultFile() {
        return defaultFile;
    }

    public void setDefaultFile(String defaultFile) {
        this.defaultFile = defaultFile;
    }

    public boolean isFailOnLocalHost() {
        return failOnLocalHost;
    }

    public void setFailOnLocalHost(boolean failOnLocalHost) {
        this.failOnLocalHost = failOnLocalHost;
    }

    public boolean isFailOnBadUrls() {
        return failOnBadUrls;
    }

    public void setFailOnBadUrls(boolean failOnBadUrls) {
        this.failOnBadUrls = failOnBadUrls;
    }

    public boolean isReportOnly() {
        return reportOnly;
    }

    public void setReportOnly(boolean reportOnly) {
        this.reportOnly = reportOnly;
    }
}
