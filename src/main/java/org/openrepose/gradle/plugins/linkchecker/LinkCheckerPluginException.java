package org.openrepose.gradle.plugins.linkchecker;

public class LinkCheckerPluginException extends RuntimeException {
    public LinkCheckerPluginException(String message) {
        super(message);
    }

    public LinkCheckerPluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
