package language

import geb.Browser
import geb.navigator.Navigator
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field

import java.util.concurrent.CompletableFuture

System.setProperty("webdriver.gecko.driver", "/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/lib/geckodriver")
System.setProperty("webdriver.chrome.driver", "/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/lib/chromedriver")
System.setProperty("geb.env", "firefox")


//extractLinks("https://sv.wiktionary.org/w/index.php?title=Kategori:Svenska/Vardagligt")

@Field
DbService db = new DbService("bolt://localhost:7687", "satya", "Alpha_1234")

private Browser extractLinks(String url) {
    final outputFile = "/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/src/test/groovy/language/wiki_sv_vardagligt.json"
    Browser.drive {
        go url
        waitFor(10) { $("a", text: "nästa sida").size() > 0 }

        Thread.sleep(4000)

        def links = $("#mw-pages").find("a").findAll {
            $(it).attr('href').contains("/wiki/")
        }.collect {
            """ "${it.attr("href")}" """
        }

        def outFile = new File(outputFile)
        List<String> wikiLinks = new JsonSlurper().parse(outFile) as List<String>
        wikiLinks.addAll(links)

        outFile.withPrintWriter {
            it.println(JsonOutput.toJson(wikiLinks))
            it.flush()
        }

        println links
        def nextPage = $("a", text: "nästa sida").first()

        def nextPageLink = nextPage.attr("href")
        if (nextPageLink) {
            extractLinks(nextPageLink)
        }
    }
}

def fetchWikiForWord(String word) {
    def link = "https://sv.wiktionary.org/wiki/$word"
    Browser.drive {
        go link
        waitFor(10) { $("#content").size() > 0 }
        def categories = $('#mw-normal-catlinks li').collect { it.text().trim() }
//                .findAll { !it.contains("/") || it.contains("Svenska/")}

        def translations = [] as Set
        def swedishHeader = $('h2').findAll { it.text().contains("Svenska") }.first()
        def allSwedishParts = swedishHeader.nextUntil("h2")
        def translationElements = allSwedishParts.findAll { it.hasClass("översättningar") }
        if (translationElements.size() == 0) {
            translationElements = allSwedishParts.findAll { it.find('div.översättningar').size() > 0 }
        }
        translationElements?.each {
            it.click()
            def ww = it.find("li").collect { it.text() }
                       .findAll { it.contains("engelska:") }
                       .collect {
                           it.replaceAll("engelska:", "").replaceAll("\\(en\\)", "").split(",").collect { it.trim() }
                       }.flatten()
            translations.addAll(ww)
        }

        def  extract = { text, colon = true ->
            def t1 = text +  (colon ? ":" : "")

            List<String>  output = allSwedishParts.collect { it.find(text: t1).parent().collect { it.text()} }
                                          .flatten()
                                          .findAll { it }
                                          .collect { it.replaceAll("$t1", "").split(",") }
                                          .flatten().collect { it.replaceAll("\\(.*mål\\)", "").trim() }
            if (!output) output = []

            def header = allSwedishParts.collect { it.find(text: text).parent() }
                                               .findAll { it.first().is("h4") }
            if (!header.isEmpty()) {
                List<String>  otherMatches = header
                        .first().nextUntil("h4")
                        .find("li").collect { it.text() }
                        .flatten().collect { it.replaceAll("\\(.*mål\\)", "").trim() }
                output.addAll(otherMatches)
            }

            return output
        }

        // synonyms.............
        List<String> relatedWords = extract("Besläktade ord")
        relatedWords.addAll(
                extract("böjningsform av", false)
                        .collect { it.split("\n")[0]?.split("Omdirigerar till")[0]?.trim()}
        )
        def synonyms = extract("Synonymer")
        def antonyms = extract("Antonymer")
        def usualConstructions = extract("Vanliga konstruktioner")

        def similarSemantics = extract("Se även")
        similarSemantics.addAll(extract('Jämför'))

        db.addWikiEntry(new WikiEntry(word, translations as List<String>, synonyms, antonyms, relatedWords, usualConstructions, similarSemantics, categories))
        println("Categories: $categories \n Translations: $translations \n Synonyms: $synonyms \n " +
                        "Antonyms: $antonyms \n Related: $relatedWords \n usualConstructions: $usualConstructions \n " +
                        "Similar semantics: $similarSemantics ")
    }
}

db.getWordsToProcess()
//['brutit', 'blev']
        .each {
    String word = it
    word = word.replaceAll('"', '')
    println(word)
    fetchWikiForWord(word)
}