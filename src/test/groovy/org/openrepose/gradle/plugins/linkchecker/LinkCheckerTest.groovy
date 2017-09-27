package org.openrepose.gradle.plugins.linkchecker

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.junit.Test

import static org.junit.Assert.assertEquals

class LinkCheckerTest {
    @Test
    public void linkCheckerExampleBasic() {
        Multimap<String, File> linksToSourceFiles = HashMultimap.create()
        List<String> badLinks = new ArrayList<>()

        int total = LinkChecker.checkLinks(
                './examples/basic/src/main/resources/html',
                'index.html',
                true,
                false,
                linksToSourceFiles,
                badLinks
        )

        assertEquals("total", 9, total)
        assertEquals("linksToSourceFiles", 8, linksToSourceFiles.size())
        assertEquals("badLinks", 1, badLinks.size())
    }
}
