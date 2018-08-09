package org.openrepose.gradle.plugins.linkchecker

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.junit.Assert.assertEquals

class LinkCheckerTest extends Specification {

    @Shared
    LinkCheckerPluginExtension linkCheckerPluginExtension

    void setup() {
        linkCheckerPluginExtension = new LinkCheckerPluginExtension()
        linkCheckerPluginExtension.with {
            defaultFile = 'index.html'
            failOnLocalHost = true
            failOnIgnoreHost = false
            failOnBadUrls = false
            httpURLConnectionTimeout = 1
        }
    }

    def "linkCheckerExampleBasic"() {
        given:
        def linksToSourceFiles = HashMultimap.create() as Multimap<String, File>
        def badLinks = new ArrayList<String>()

        linkCheckerPluginExtension.ignoreHostRegexs = new ArrayList<String>()

        when:
        int total = LinkChecker.checkLinks(
                null,
                new File('./example/src/main/resources/html'),
                linkCheckerPluginExtension,
                linksToSourceFiles,
                badLinks
        )

        then:
        assertEquals("total", 10, total)
        assertEquals("linksToSourceFiles", 9, linksToSourceFiles.size())
        assertEquals("badLinks", 1, badLinks.size())
    }

    def "linkCheckerExampleIgnore"() {
        given:
        def linksToSourceFiles = HashMultimap.create() as Multimap<String, File>
        def badLinks = new ArrayList<String>()

        linkCheckerPluginExtension.ignoreHostRegexs = [/www\.google\.com/]

        when:
        int total = LinkChecker.checkLinks(
                null,
                new File('./example/src/main/resources/html'),
                linkCheckerPluginExtension,
                linksToSourceFiles,
                badLinks
        )

        then:
        assertEquals("total", 10, total)
        assertEquals("linksToSourceFiles", 9, linksToSourceFiles.size())
        assertEquals("badLinks", 1, badLinks.size())
    }

    def "linkCheckerExampleIgnoreWildcard"() {
        given:
        def linksToSourceFiles = HashMultimap.create() as Multimap<String, File>
        def badLinks = new ArrayList<String>()

        linkCheckerPluginExtension.ignoreHostRegexs = [/.*\.google\.com/]

        when:
        int total = LinkChecker.checkLinks(
                null,
                new File('./example/src/main/resources/html'),
                linkCheckerPluginExtension,
                linksToSourceFiles,
                badLinks
        )

        then:
        assertEquals("total", 10, total)
        assertEquals("linksToSourceFiles", 9, linksToSourceFiles.size())
        assertEquals("badLinks", 1, badLinks.size())
    }

    def "linkCheckerExampleIgnoreFail"() {
        given:
        def linksToSourceFiles = HashMultimap.create() as Multimap<String, File>
        def badLinks = new ArrayList<String>()

        linkCheckerPluginExtension.failOnIgnoreHost = true
        linkCheckerPluginExtension.ignoreHostRegexs = [/www\.google\.com/]

        when:
        int total = LinkChecker.checkLinks(
                null,
                new File('./example/src/main/resources/html'),
                linkCheckerPluginExtension,
                linksToSourceFiles,
                badLinks
        )

        then:
        assertEquals("total", 10, total)
        assertEquals("linksToSourceFiles", 9, linksToSourceFiles.size())
        assertEquals("badLinks", 2, badLinks.size())
    }

    @Unroll
    def "checkUrlWithMultipleRequestMethods (#url)"() {
        when:
        boolean valid = LinkChecker.checkUrl(
                ['HEAD', 'GET'],
                url.toURL(),
                -1,
                new PrintWriter(System.out))

        then:
        assertEquals("url did${valid ? "" : " NOT"} checkout", expected, valid)

        where:
        url                                                                               | expected
        "https://www.linkedin.com/groups/39757"                                           | true
        "http://plugins.grails.org"                                                       | true
        "https://github.com/grails/grails-core/releases/download/v3.3.6/grails-3.3.6.zip" | true
        "https://github.com/grails/grails-core/releases/download/v0.0.0/grails-0.0.0.zip" | false
    }

    @Unroll
    def "checkUrlWithSingleRequestMethods (#method)"() {
        when:
        boolean valid = LinkChecker.checkUrl(
                method,
                "https://www.linkedin.com/groups/39757".toURL(),
                -1,
                new PrintWriter(System.out))

        then:
        assertEquals("method did${valid ? "" : " NOT"} checkout", expected, valid)

        where:
        method | expected
        'HEAD' | false
        'GET'  | true
    }
}
