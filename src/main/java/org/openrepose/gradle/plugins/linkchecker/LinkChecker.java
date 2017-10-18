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
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

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
     * @param startFileName            The file to start from.
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
            String startFileName,
            String defaultFile,
            boolean failOnLocalHost,
            boolean failOnIgnoredHost,
            boolean failOnBadUrls,
            int httpURLConnectionTimeout,
            Collection<String> ignoreHostRegexs,
            Multimap<String, File> linksToSourceFiles,
            List<String> badLinks
    ) throws IllegalArgumentException, IOException {
        if (startFileName == null) {
            throw new IllegalArgumentException("'startFileName' can NOT be NULL");
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

        File startFile = new File(startFileName);
        if (!startFile.exists()) {
            throw new IllegalArgumentException("Starting Dir/File '" + startFileName + "' does NOT exist");
        }
        log.warn("Checking links starting from: {}", startFile.getAbsolutePath());

        List<String> todoListLinks = new ArrayList<>();

        todoListLinks.add(startFile.getAbsolutePath());
        // don't use foreach as this is a growing list
        for (int i = 0; i < todoListLinks.size(); i++) {
            String link = todoListLinks.get(i);
            if (URL_VALIDATOR.isValid(link)) {
                processLinkAsUrl(link, failOnLocalHost, failOnIgnoredHost, failOnBadUrls, httpURLConnectionTimeout, ignoreHostRegexs, badLinks);
            } else {
                processLinkAsFile(link, defaultFile, todoListLinks, linksToSourceFiles, badLinks);
            }
        }
        return todoListLinks.size();
    }

    private static void processLinkAsUrl(
            String link,
            boolean failOnLocalHost,
            boolean failOnIgnoredHost,
            boolean failOnBadUrls,
            int httpURLConnectionTimeout,
            Collection<String> ignoreHostRegexs,
            List<String> badLinks
    ) {
        log.info("Processing URL: {}", link);
        try {
            URL url = new URL(link);
            // note that this also matches https
            if (url.getProtocol().startsWith("http")) {
                // note that this is ignoring 127.0.0.1 altogether, gotta draw a line somewhere
                String host = url.getHost();
                if (host.equals("localhost")) {
                    log.warn("URL for localhost indicates suspicious environment dependency: {}", url);
                    if (failOnLocalHost) {
                        badLinks.add(link);
                    }
                } else if(!(ignoreHostRegexs.stream().filter(host::matches).collect(Collectors.toList()).isEmpty())) {
                    log.info("The host destination is configured to be ignored: {}", host);
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
                            log.info("Got response code {} for URL: {}", responseCode, url);
                        } else if (responseCode != HttpURLConnection.HTTP_OK) {
                            log.warn("Got response code {} for URL: {}", responseCode, url);
                            addBadUrlIfConfigured(link, failOnBadUrls, badLinks);
                        }
                    } catch (InterruptedIOException | ConnectException | UnknownHostException exception) {
                        log.warn("Cannot connect to URL: {}", url);
                        log.debug("Source:", exception);
                        addBadUrlIfConfigured(link, failOnBadUrls, badLinks);
                    } catch (IOException exception) {
                        log.warn("Problem with URL: {}", url);
                        log.debug("Source:", exception);
                        addBadUrlIfConfigured(link, failOnBadUrls, badLinks);
                    }
                }
            } else {
                log.warn("Only http* supported; not handling URL: {}", url);
            }
        } catch (MalformedURLException exception) {
            log.warn("Bad URL: {}", link, exception);
            addBadUrlIfConfigured(link, failOnBadUrls, badLinks);
        }
    }

    private static void addBadUrlIfConfigured(String link, boolean failOnBadUrls, List<String> badLinks) {
        if (failOnBadUrls) {
            badLinks.add(link);
        }
    }

    private static void processLinkAsFile(
            String fileLink,
            String defaultFile,
            List<String> todoListLinks,
            Multimap<String, File> linksToSourceFiles,
            List<String> badLinks
    ) throws IOException {
        log.info("Processing File: {}", fileLink);
        File file = new File(fileLink);
        log.debug("file = {}", file.getAbsolutePath());
        if (file.isDirectory()) {
            file = new File(file, defaultFile);
        }
        log.debug("file = {}", file.getAbsolutePath());
        if (file.exists()) {
            log.debug("file does exist");
            try {
                Document document = Jsoup.parse(file, "UTF-8", file.getParentFile().getAbsolutePath());
                for (String elementName : ELEMENTS_TO_ATTRIBUTES.keySet()) {
                    log.trace("elementName = {}", elementName);
                    Elements elements = document.select(elementName);
                    String attributeName = ELEMENTS_TO_ATTRIBUTES.get(elementName);
                    for (Element element : elements) {
                        String link = element.attr(attributeName);
                        if (link.startsWith("javascript:") || link.startsWith("mailto:")) {
                            log.debug("Ignoring: {}", link);
                        } else {
                            // IF this is a local resource link,
                            // THEN make it relative to the starting directory.
                            if (!URL_VALIDATOR.isValid(link)) {
                                String linkWithoutFragment = link.replaceFirst("#.*", "");
                                link = new File(file.getParent(), linkWithoutFragment).getCanonicalPath();
                            }
                            if (todoListLinks.contains(link)) {
                                log.debug("Already marked: {}", link);
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
            log.debug("file does NOT exist");
            badLinks.add(fileLink);
        }
    }
}
