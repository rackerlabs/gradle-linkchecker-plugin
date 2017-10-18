package org.openrepose.gradle.plugins.linkchecker

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.junit.Test

import static org.junit.Assert.assertEquals

class LinkCheckerTest {
    @Test
    public void linkCheckerExampleBasic() {
        def linksToSourceFiles = HashMultimap.create() as Multimap<String, File>
        def ignoreHostRegexs = new ArrayList<String>()
        def badLinks = new ArrayList<String>()

        int total = LinkChecker.checkLinks(
                './examples/basic/src/main/resources/html',
                'index.html',
                true,
                false,
                false,
                1,
                ignoreHostRegexs,
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
        def ignoreHostRegexs = [/www\.google\.com/]
        def badLinks = new ArrayList<String>()

        int total = LinkChecker.checkLinks(
                './examples/basic/src/main/resources/html',
                'index.html',
                true,
                false,
                false,
                1,
                ignoreHostRegexs,
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
        def ignoreHostRegexs = [/.*\.google\.com/]
        def badLinks = new ArrayList<String>()

        int total = LinkChecker.checkLinks(
                './examples/basic/src/main/resources/html',
                'index.html',
                true,
                false,
                false,
                1,
                ignoreHostRegexs,
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
        def ignoreHostRegexs = [/www\.google\.com/]
        def badLinks = new ArrayList<String>()

        int total = LinkChecker.checkLinks(
                './examples/basic/src/main/resources/html',
                'index.html',
                true,
                true,
                false,
                1,
                ignoreHostRegexs,
                linksToSourceFiles,
                badLinks
        )

        assertEquals("total", 10, total)
        assertEquals("linksToSourceFiles", 9, linksToSourceFiles.size())
        assertEquals("badLinks", 2, badLinks.size())
    }
}
