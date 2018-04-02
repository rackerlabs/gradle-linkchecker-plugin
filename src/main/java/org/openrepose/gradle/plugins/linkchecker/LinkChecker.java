package org.openrepose.gradle.plugins.linkchecker;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.validator.routines.UrlValidator;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.gradle.api.logging.LogLevel.*;
import static org.openrepose.gradle.plugins.linkchecker.LinkCheckerPluginTask.logMsg;

public class LinkChecker {

    private static final Logger log = Logging.getLogger(LinkChecker.class);

    /**
     * The elements and their attributes that we want to validate.
     */
    private static final Map<String, String> ELEMENTS_TO_ATTRIBUTES;

    static {
        Map<String, String> elementsToAttributes = new HashMap<>();
        elementsToAttributes.put("a", "href");
        elementsToAttributes.put("frame", "src");
        elementsToAttributes.put("img", "src");
        ELEMENTS_TO_ATTRIBUTES = Collections.unmodifiableMap(elementsToAttributes);
    }

    private static final UrlValidator URL_VALIDATOR = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);

    private LinkChecker() {
        // This class should not be instantiated.
    }

    /**
     * Recursively checks the links starting from the startFile.
     *
     * @param printWriter              The file to log to.
     * @param startFile                The file to start from.
     *                                 Links from the file will be checked.
     *                                 Non-URL links (i.e. local files) will be taken for further link checking (feels like recursion).
     * @param defaultFile              The file name to be used as the default, in case a (non-URL) link points to a folder.
     *                                 It's what happens on a web server: requests for {@code http://foo/bar} will serve you (typically) {@code http://foo/bar/index.html}.
     * @param failOnLocalHost          Should this plugin make your build fail if it encounters links to {@code localhost}.
     *                                 Typically, depending on something local to the build would hamper the portability of the build.
     * @param failOnIgnoredHost        Should this plugin make your build fail if it encounters links to an ignored host.
     *                                 This is not the default and is typically only enabled if there are troublesome links that would normally require multi-party authentication to access (e.g. SAML, OAuth).
     * @param failOnBadUrls            Should this plugin make your build fail if it encounters bad URLs.
     *                                 This is not the default, in appreciation of the fact that (non-local) URLs are out of our control.
     *                                 Typically, validating (non-local) URLs would hamper the reproducibility of the build.
     * @param httpURLConnectionTimeout The specified timeout value, in milliseconds, to be used when opening a communications link to a non-local URL.
     *                                 A timeout of zero is interpreted as an infinite timeout.
     *                                 A timeout of less than zero is interpreted to use the system timeout.
     * @param ignoreHostRegexs         A list of regular expressions of hosts to not even attempt communications with.
     * @param linksToSourceFiles       Populated with all the files processed.
     * @param badLinks                 Populated with all the bad links that could not be processed.
     * @return the total number of files processed
     * @throws IllegalArgumentException if the startFileName is null or the file does not exist
     * @throws IOException              if anything goes wrong while trying to access a file
     */
    public static int checkLinks(
            PrintWriter printWriter,
            File startFile,
            String defaultFile,
            boolean failOnLocalHost,
            boolean failOnIgnoredHost,
            boolean failOnBadUrls,
            int httpURLConnectionTimeout,
            Collection<String> ignoreHostRegexs,
            Multimap<String, File> linksToSourceFiles,
            List<String> badLinks
    ) throws IllegalArgumentException, IOException {
        if (startFile == null) {
            throw new IllegalArgumentException("'startFile' can NOT be NULL");
        }
        if (defaultFile == null) {
            defaultFile = "index.html";
        }
        if (linksToSourceFiles == null) {
            linksToSourceFiles = HashMultimap.create();
        }
        if (badLinks == null) {
            badLinks = new ArrayList<>();
        }

        if (!startFile.exists()) {
            throw new IllegalArgumentException("Starting Dir/File '" + startFile.getAbsolutePath() + "' does NOT exist");
        }
        logMsg(INFO, printWriter, "Checking links starting from: {}", startFile.getAbsolutePath());

        List<String> todoListLinks = new ArrayList<>();

        todoListLinks.add(startFile.getAbsolutePath());
        // don't use foreach as this is a growing list
        for (int i = 0; i < todoListLinks.size(); i++) {
            String link = todoListLinks.get(i);
            if (URL_VALIDATOR.isValid(link)) {
                processLinkAsUrl(printWriter, link, failOnLocalHost, failOnIgnoredHost, failOnBadUrls, httpURLConnectionTimeout, ignoreHostRegexs, badLinks);
            } else {
                processLinkAsFile(printWriter, link, defaultFile, todoListLinks, linksToSourceFiles, badLinks);
            }
        }
        return todoListLinks.size();
    }

    private static void processLinkAsUrl(
            PrintWriter printWriter,
            String link,
            boolean failOnLocalHost,
            boolean failOnIgnoredHost,
            boolean failOnBadUrls,
            int httpURLConnectionTimeout,
            Collection<String> ignoreHostRegexs,
            List<String> badLinks
    ) {
        logMsg(INFO, printWriter, "Processing URL: {}", link);
        try {
            URL url = new URL(link);
            // note that this also matches https
            if (url.getProtocol().startsWith("http")) {
                // note that this is ignoring 127.0.0.1 altogether, gotta draw a line somewhere
                String host = url.getHost();
                if (host.equals("localhost")) {
                    logMsg(INFO, printWriter, "URL of localhost indicates suspicious environment dependency: {}", url);
                    if (failOnLocalHost) {
                        badLinks.add(link);
                    }
                } else if (!(ignoreHostRegexs.stream().filter(host::matches).collect(Collectors.toList()).isEmpty())) {
                    logMsg(INFO, printWriter, "The host destination is configured to be ignored: {}", host);
                    if (failOnIgnoredHost) {
                        badLinks.add(link);
                    }
                } else {
                    try {
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("HEAD");
                        if (httpURLConnectionTimeout >= 0) {
                            connection.setConnectTimeout(httpURLConnectionTimeout);
                        }
                        connection.connect();
                        int responseCode = connection.getResponseCode();
                        if (300 <= responseCode && responseCode < 400) {
                            logMsg(INFO, printWriter, "Got response code {} for URL: {}", responseCode, url);
                        } else if (responseCode != HttpURLConnection.HTTP_OK) {
                            logMsg(WARN, printWriter, "Got response code {} for URL: {}", responseCode, url);
                            addBadUrlIfConfigured(link, failOnBadUrls, badLinks);
                        }
                    } catch (InterruptedIOException | ConnectException | UnknownHostException exception) {
                        logMsg(WARN, printWriter, "Cannot connect to URL: {}", url);
                        logMsg(DEBUG, printWriter, "Source:", exception);
                        addBadUrlIfConfigured(link, failOnBadUrls, badLinks);
                    } catch (IOException exception) {
                        logMsg(WARN, printWriter, "Problem with URL: {}", url);
                        logMsg(DEBUG, printWriter, "Source:", exception);
                        addBadUrlIfConfigured(link, failOnBadUrls, badLinks);
                    }
                }
            } else {
                logMsg(INFO, printWriter, "Only http* supported; not handling URL: {}", url);
            }
        } catch (MalformedURLException exception) {
            logMsg(WARN, printWriter, "Bad URL: {}", link, exception);
            addBadUrlIfConfigured(link, failOnBadUrls, badLinks);
        }
    }

    private static void addBadUrlIfConfigured(String link, boolean failOnBadUrls, List<String> badLinks) {
        if (failOnBadUrls) {
            badLinks.add(link);
        }
    }

    private static void processLinkAsFile(
            PrintWriter printWriter,
            String fileLink,
            String defaultFile,
            List<String> todoListLinks,
            Multimap<String, File> linksToSourceFiles,
            List<String> badLinks
    ) throws IOException {
        logMsg(INFO, printWriter, "Processing File: {}", fileLink);
        File file = new File(fileLink);
        logMsg(DEBUG, printWriter, "file = {}", file.getAbsolutePath());
        if (file.isDirectory()) {
            file = new File(file, defaultFile);
        }
        logMsg(DEBUG, printWriter, "file = {}", file.getAbsolutePath());
        if (file.exists()) {
            logMsg(DEBUG, printWriter, "file does exist");
            try {
                Document document = Jsoup.parse(file, "UTF-8", file.getParentFile().getAbsolutePath());
                for (String elementName : ELEMENTS_TO_ATTRIBUTES.keySet()) {
                    log.trace("elementName = {}", elementName);
                    Elements elements = document.select(elementName);
                    String attributeName = ELEMENTS_TO_ATTRIBUTES.get(elementName);
                    for (Element element : elements) {
                        String link = element.attr(attributeName);
                        if (link.startsWith("javascript:") || link.startsWith("mailto:")) {
                            logMsg(DEBUG, printWriter, "Ignoring: {}", link);
                        } else {
                            // IF this is a local resource link,
                            // THEN make it relative to the starting directory.
                            if (!URL_VALIDATOR.isValid(link)) {
                                String linkWithoutFragment = link.replaceFirst("#.*", "");
                                link = new File(file.getParent(), linkWithoutFragment).getCanonicalPath();
                            }
                            if (todoListLinks.contains(link)) {
                                logMsg(DEBUG, printWriter, "Already marked: {}", link);
                            } else {
                                todoListLinks.add(link);
                            }
                            linksToSourceFiles.put(link, file);
                        }
                    }
                }
            } catch (IOException e) {
                throw new IOException("file cannot be read: " + file, e);
            }
        } else {
            logMsg(DEBUG, printWriter, "file does NOT exist");
            badLinks.add(fileLink);
        }
    }
}
