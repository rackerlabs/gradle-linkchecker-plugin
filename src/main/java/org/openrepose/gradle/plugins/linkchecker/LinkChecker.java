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
     * @param linkCheckerPluginExtension configuration options encapsulated in {@link LinkCheckerPluginExtension}
     * @param linksToSourceFiles       Populated with all the files processed.
     * @param badLinks                 Populated with all the bad links that could not be processed.
     * @return the total number of files processed
     * @throws IllegalArgumentException if the startFileName is null or the file does not exist
     * @throws IOException              if anything goes wrong while trying to access a file
     */
    public static int checkLinks(
            PrintWriter printWriter,
            File startFile,
            LinkCheckerPluginExtension linkCheckerPluginExtension,
            Multimap<String, File> linksToSourceFiles,
            List<String> badLinks
    ) throws IllegalArgumentException, IOException {
        if (startFile == null) {
            throw new IllegalArgumentException("'startFile' can NOT be NULL");
        }
        if (linkCheckerPluginExtension.defaultFile == null) {
            linkCheckerPluginExtension.defaultFile = "index.html";
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
                processLinkAsUrl(printWriter, link, linkCheckerPluginExtension, badLinks);
            } else {
                processLinkAsFile(printWriter, link, linkCheckerPluginExtension.defaultFile, todoListLinks, linksToSourceFiles, badLinks);
            }
        }
        return todoListLinks.size();
    }

    /**
     *
     * @param printWriter The file to log to.
     * @param link The link being validated
     * @param linkCheckerPluginExtension configuration options encapsulated in {@link LinkCheckerPluginExtension}
     * @param badLinks Populated with all the bad links that could not be processed.
     */
    private static void processLinkAsUrl(
            PrintWriter printWriter,
            String link,
            LinkCheckerPluginExtension linkCheckerPluginExtension,
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
                    if (linkCheckerPluginExtension.failOnLocalHost) {
                        badLinks.add(link);
                    }
                } else if (!(linkCheckerPluginExtension.ignoreHostRegexs.stream().filter(host::matches).collect(Collectors.toList()).isEmpty())) {
                    logMsg(INFO, printWriter, "The host destination is configured to be ignored: {}", host);
                    if (linkCheckerPluginExtension.failOnIgnoreHost) {
                        badLinks.add(link);
                    }
                } else {

                    boolean valid = checkUrl(linkCheckerPluginExtension.requestMethods, url, linkCheckerPluginExtension.httpURLConnectionTimeout, printWriter);
                    if(!valid) {
                        addBadUrlIfConfigured(link, linkCheckerPluginExtension.failOnBadUrls, badLinks);
                    }
                }
            } else {
                logMsg(INFO, printWriter, "Only http* supported; not handling URL: {}", url);
            }
        } catch (MalformedURLException exception) {
            logMsg(WARN, printWriter, "Bad URL: {}", link, exception);
            addBadUrlIfConfigured(link, linkCheckerPluginExtension.failOnBadUrls, badLinks);
        }
    }

    /**
     * @param requestMethods HTTP Request Methods (e.g. GET, HEAD) used to validate the URL
     * @param url The URL being validated
     * @param httpURLConnectionTimeout {@link LinkCheckerPluginExtension#httpURLConnectionTimeout}
     * @param printWriter The file to log to.
     */
    public static boolean checkUrl(List<String> requestMethods,
                                   URL url,
                                   int httpURLConnectionTimeout,
                                   PrintWriter printWriter) {
        for (String requestMethod : requestMethods) {
            boolean valid = checkUrl(requestMethod, url, httpURLConnectionTimeout, printWriter);
            if (valid) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param requestMethod HTTP Request Method (e.g. GET, HEAD) used to validate the URL
     * @param url The URL being validated
     * @param httpURLConnectionTimeout {@link LinkCheckerPluginExtension#httpURLConnectionTimeout}
     * @param printWriter The file to log to.
     * @return true if (300 <= response code < 400) or responseCode == 200, false if other code or exception
     */
    public static boolean checkUrl(String requestMethod, URL url, int httpURLConnectionTimeout, PrintWriter printWriter) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(requestMethod);
            if (httpURLConnectionTimeout >= 0) {
                connection.setConnectTimeout(httpURLConnectionTimeout);
            }
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (300 <= responseCode && responseCode < 400) {
                logMsg(INFO, printWriter, "Got response code {} for URL: {} => {}", responseCode, requestMethod, url);
            } else if (responseCode != HttpURLConnection.HTTP_OK) {
                logMsg(WARN, printWriter, "Got response code {} for URL: {} => {}", responseCode, requestMethod, url);
                return false;
            } else {
                logMsg(INFO, printWriter, "Got response code {} for URL: {} => {}", responseCode, requestMethod, url);
            }
        } catch (InterruptedIOException | ConnectException | UnknownHostException exception) {
            logMsg(WARN, printWriter, "Cannot connect to URL: {}", url);
            logMsg(DEBUG, printWriter, "Source:", exception);
            return false;
        } catch (IOException exception) {
            logMsg(WARN, printWriter, "Problem with URL: {}", url);
            logMsg(DEBUG, printWriter, "Source:", exception);
            return false;
        }
        return true;
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
