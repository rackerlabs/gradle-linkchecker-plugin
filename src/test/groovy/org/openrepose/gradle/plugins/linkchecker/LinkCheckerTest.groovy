package org.openrepose.gradle.plugins.linkchecker

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.junit.Test

import static org.junit.Assert.assertEquals

class LinkCheckerTest {
    @Test
    public void linkCheckerExampleBasic() {
        def linksToSourceFiles = HashMultimap.create() as Multimap<String, File>
        def badLinks = new ArrayList<String>()

        LinkCheckerPluginExtension linkCheckerPluginExtension = new LinkCheckerPluginExtension()
        linkCheckerPluginExtension.with {
            defaultFile = 'index.html'
            failOnLocalHost = true
            failOnIgnoreHost = false
            failOnBadUrls = false
            httpURLConnectionTimeout = 1
            ignoreHostRegexs = new ArrayList<String>()
        }

        int total = LinkChecker.checkLinks(
                null,
                new File('./example/src/main/resources/html'),
                linkCheckerPluginExtension,
                linksToSourceFiles,
                badLinks
        )

        assertEquals("total", 10, total)
        assertEquals("linksToSourceFiles", 9, linksToSourceFiles.size())
        assertEquals("badLinks", 1, badLinks.size())
    }

    @Test
    public void linkCheckerExampleIgnore() {
        def linksToSourceFiles = HashMultimap.create() as Multimap<String, File>
        def badLinks = new ArrayList<String>()

        LinkCheckerPluginExtension linkCheckerPluginExtension = new LinkCheckerPluginExtension()
        linkCheckerPluginExtension.with {
            defaultFile = 'index.html'
            failOnLocalHost = true
            failOnIgnoreHost = false
            failOnBadUrls = false
            httpURLConnectionTimeout = 1
            ignoreHostRegexs = [/www\.google\.com/]
        }

        int total = LinkChecker.checkLinks(
                null,
                new File('./example/src/main/resources/html'),
                linkCheckerPluginExtension,
                linksToSourceFiles,
                badLinks
        )

        assertEquals("total", 10, total)
        assertEquals("linksToSourceFiles", 9, linksToSourceFiles.size())
        assertEquals("badLinks", 1, badLinks.size())
    }

    @Test
    public void linkCheckerExampleIgnoreWildcard() {
        def linksToSourceFiles = HashMultimap.create() as Multimap<String, File>
        def badLinks = new ArrayList<String>()

        LinkCheckerPluginExtension linkCheckerPluginExtension = new LinkCheckerPluginExtension()
        linkCheckerPluginExtension.with {
            defaultFile = 'index.html'
            failOnLocalHost = true
            failOnIgnoreHost = false
            failOnBadUrls = false
            httpURLConnectionTimeout = 1
            ignoreHostRegexs = [/.*\.google\.com/]
        }

        int total = LinkChecker.checkLinks(
                null,
                new File('./example/src/main/resources/html'),
                linkCheckerPluginExtension,
                linksToSourceFiles,
                badLinks
        )

        assertEquals("total", 10, total)
        assertEquals("linksToSourceFiles", 9, linksToSourceFiles.size())
        assertEquals("badLinks", 1, badLinks.size())
    }

    @Test
    public void linkCheckerExampleIgnoreFail() {
        def linksToSourceFiles = HashMultimap.create() as Multimap<String, File>
        def badLinks = new ArrayList<String>()

        LinkCheckerPluginExtension linkCheckerPluginExtension = new LinkCheckerPluginExtension()
        linkCheckerPluginExtension.with {
            defaultFile = 'index.html'
            failOnLocalHost = true
            failOnIgnoreHost = true
            failOnBadUrls = false
            httpURLConnectionTimeout = 1
            ignoreHostRegexs = [/www\.google\.com/]
        }

        int total = LinkChecker.checkLinks(
                null,
                new File('./example/src/main/resources/html'),
                linkCheckerPluginExtension,
                linksToSourceFiles,
                badLinks
        )

        assertEquals("total", 10, total)
        assertEquals("linksToSourceFiles", 9, linksToSourceFiles.size())
        assertEquals("badLinks", 2, badLinks.size())
    }
}
