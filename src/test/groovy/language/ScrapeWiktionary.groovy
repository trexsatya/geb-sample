package language


import geb.Browser
import geb.Configuration
import groovy.transform.Field
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

System.setProperty("webdriver.gecko.driver", "/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/lib/geckodriver")
System.setProperty("webdriver.chrome.driver", "/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/lib/chromedriver")
System.setProperty("geb.env", "firefox")


//extractLinks("https://sv.wiktionary.org/w/index.php?title=Kategori:Svenska/Vardagligt")

@Field
DbService db = new DbService("bolt://localhost:7687", "satya", System.getenv("DB_PWD"))

@Field
File errorFile = new File("/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/errors.log")

@Field
def threadPool = Executors.newFixedThreadPool(30)

def fetchWikiForWord(String word) {
    def link = "https://sv.wiktionary.org/wiki/$word"

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new");
    WebDriver driver = new ChromeDriver(options)
    def config = new Configuration()
    config.setDriver(driver)

    def future = new CompletableFuture()

    def runnable = {
        Browser.drive(config) {
            try {
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

                def extract = { text, colon = true ->
                    def t1 = text + (colon ? ":" : "")

                    List<String> output = allSwedishParts.collect { it.find(text: t1).parent().collect { it.text() } }
                                                         .flatten()
                                                         .findAll { it }
                                                         .collect { it.replaceAll("$t1", "").split(",") }
                                                         .flatten().collect { it.replaceAll("\\(.*mål\\)", "").trim() }
                    if (!output) output = []

                    def header = allSwedishParts.collect { it.find(text: text).parent() }
                                                .findAll { it.first().is("h4") }
                    if (!header.isEmpty()) {
                        List<String> otherMatches = header
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
                                .collect { it.split("\n")[0]?.split("Omdirigerar till")[0]?.trim() }
                )
                def synonyms = extract("Synonymer")
                def antonyms = extract("Antonymer")
                def usualConstructions = extract("Vanliga konstruktioner")

                def similarSemantics = extract("Se även")
                similarSemantics.addAll(extract('Jämför'))

                db.addWikiEntry(new WikiEntry(word, translations as List<String>, synonyms, antonyms, relatedWords, usualConstructions, similarSemantics, categories))
                println("$word\nCategories: $categories \n Translations: $translations \n Synonyms: $synonyms \n " +
                                "Antonyms: $antonyms \n Related: $relatedWords \n usualConstructions: $usualConstructions \n " +
                                "Similar semantics: $similarSemantics ")

                future.complete(word)
            } catch (Throwable e) {
                println "Error for $word: $e"
                errorFile.append("Error for $word: $e\n\n")
                future.complete(e)
            }

            driver.close()
        }.quit()
    }

    new Thread({
        try {
            runnable()
        } catch (Throwable e) {
            println "Error for $word: $e"
            errorFile.append("Error for $word: $e\n\n")
            future.complete(e)
        }
    }).start()

    return future
}

def words = getData(db)
.findAll { it.split(" ").length == 1}

def size = words.size()

private processInParallel(List<String> words) {
    CompletableFuture[] xx = words.collect { fetchWikiForWord(it)}.toArray(new CompletableFuture[0])
    CompletableFuture.allOf(xx).join()
}

words.collate(10).each {processInParallel(it)}

private static List<String> getData(DbService db) {
    getDataFromDb(db)
//    return  ['polismyndighetenss']
}

private static List<String> getDataFromDb(DbService db) {
    db.getWordsToProcessWiki()
      .collect {
          String word = it
          word.replaceAll('"', '')
      }
};
