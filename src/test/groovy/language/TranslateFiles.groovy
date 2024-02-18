import com.google.common.base.CharMatcher
import geb.Browser
import geb.Configuration
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field
import language.DbService
import language.SRTParser
import language.Subtitle
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.util.concurrent.CompletableFuture

System.setProperty("webdriver.gecko.driver",
                   "/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/lib/geckodriver")
System.setProperty("webdriver.chrome.driver", "/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/lib/chromedriver")

def synchronized copyString() {

}

def translateSvToEn(sv) {
    sv  = removeEmojis(sv)
//    println("Sv\n$sv")
    def serviceUrl = "https://translate.google.com/?sl=sv&tl=en"
    def future = new CompletableFuture<String>()

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new");
    WebDriver driver = new ChromeDriver(options)
    def config = new Configuration()
    config.setDriver(driver)

    def runnable = {
                Browser.drive(config) {
                    go serviceUrl

                    try {
                        waitFor(5) { $('span', text: 'Accept all') }
                        $('span', text: 'Accept all').click()
                    } catch (Error e) {
                        println(e)
                    }

                    waitFor { $('textarea[aria-label="Source text"]') }

                    $('textarea[aria-label="Source text"]').value(sv)

                    sleep(5000)

                    waitFor { $('button[aria-label="Copy translation"]').attr("data-tooltip-enabled") == "true" }
//                    $('button[aria-label="Copy translation"]').click()
                    String res = $('button[aria-label="Copy translation"]').parent().parent().parent().parent().first().text()

//                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
//                    String res = clipboard.getContents(null).getTransferData(DataFlavor.stringFlavor)

                    println("result: $res")

                    future.complete(res)
                    driver.close()
                }.quit()
            }

    new Thread({
        try {
            runnable()
        } catch (Throwable e) {
            println "Error $e"
            future.completeExceptionally(e)
        }
    }).start()

    return future
}

private static String srtResult(String res) {
    res = res.split("\n\n").collect {
        def (id, time, rest) = it.split("\n")
        time = time.replaceAll(" ", "")
        time = time.replaceAll("->", " --> ")

        return "$id\n$time\n$rest"
    }.join("\n\n")
    res
}

private static boolean existsInEnglish(File it) {
    new File(it.parent + "/" + it.name.replace(".sv.srt", ".en.srt")).exists()
}

private static boolean isSwedishSrt(File it) {
    it.name.endsWith(".sv.srt")
}

def translateSRTFiles(List<String> files) {
    println("Files to translate: ${files.size()}, ${files.join("\n")}")

    int done = 0
    files
//            .findAll { it.contains("/Users/satyendra.kumar/Documents/Swedish_Media/Livet Utan Jobb.text") }
            .forEach { String file ->
                println("File (${files.size() - done}): $file")
                File svSrt = new File(file)
                File enSrt = new File(svSrt.parent + "/" + svSrt.name.replace(".sv.srt", ".en.srt"))

                def  sv = SRTParser.getSubtitlesFromFile(svSrt.absolutePath)

                if(!enSrt.exists()) enSrt.createNewFile()

                def subtitleItemsToTranslate = sv
                def textToTranslate = subtitleItemsToTranslate.collect {it.text }

                def chunks = chunks(textToTranslate, 4500)

                def separator = "\n_\n"

                CompletableFuture<String>[] futures = chunks.collect {translateSvToEn(it.join(separator)) }.toArray(new CompletableFuture[0])
                CompletableFuture.allOf(futures).join()
                def result = futures.collect { it.get().split(separator) }.flatten()
                if(sv.size() != result.size()) {
                    throw new RuntimeException("Something is missing")
                }
                for(int i=0; i < result.size(); i++){
                    sv.get(i).text = result[i]
                }
                result = sv.collect { it.toString() }.join("")
//                println(result)

                enSrt.text = result
                println("Done ${done++}")
            }
}

private static ArrayList<String> filesToTranslate() {
    def dir = "/Users/satyendra.kumar/Documents/Swedish_Media/All_Subs/YouTube/"
    List<String> files = []

    new File(dir).eachFileRecurse {
        if (isSwedishSrt(it) && !existsInEnglish(it)) {
            files.add(it.path)
        }
    }
    files
}

static def srtToString(List<Subtitle> list) {
    return list.collect { "${it.id}\n${it.startTime} --> ${it.endTime}\n${it.text}\n" }.join("\n")
}

static List<List> chunks(List list, int maxChars) {
    def res = []
    def currentChunk = []
    int charsInCurrentChunk = 0

    list.each { line ->
        if (charsInCurrentChunk + line.toString().length() > maxChars) {
            charsInCurrentChunk = 0
            res << currentChunk
            currentChunk = []
        }
        charsInCurrentChunk += (line.toString().length() + 1)
        currentChunk << line
    }

    if (!currentChunk.isEmpty()) res.add(currentChunk)
    return res
}

private def translateText(String text) {
    def _chunks = chunks(text.split("\n") as List, 4500)
    def futures = _chunks.collect { translateSvToEn(it.join("\n"))}
   CompletableFuture.allOf(futures as CompletableFuture[]).join()

   def translated =  futures.collect { it.get().split("\n") }
    println("Future contents: $translated")
    return translated.flatten()
}

@Field
DbService db = new DbService("bolt://localhost:7687", "satya", System.getenv("DB_PWD"))

private void translateWordsFromDb(DbService db) {
    def wordsToTranslate = db.getWordsToTranslate().collect { it.toString() }
    println("Words to translate: ${wordsToTranslate.size()}")
    wordsToTranslate.collate(5000).each {
        def input = it
        input = input.collect {return removeEmojis(it)}
        def mappings = [:]
        def result = translateText(input.join("\n"))
        if (input.size() != result.size()) {
            throw new RuntimeException("Something is missing")
        }

        println("Input $input")
        for (int i = 0; i < input.size(); i++) {
            mappings[input[i]] = [result[i].toString()]
        }
        db.addTranslations(mappings)

        println("Done!")
    }
}

private String removeEmojis(String it) {
    String characterFilter = "[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]";
    return it.replaceAll(characterFilter, "");
}

//translateWordsFromDb(db)


translateSRTFiles(filesToTranslate() - "/Users/satyendra.kumar/Documents/Swedish_Media/All_Subs/YouTube/Lund University || Debatt i Lund： Hur rättvis är meritokratin？ || c-7bkULJuak.sv.srt")


//translateSRTFiles(["/Users/satyendra.kumar/Documents/Swedish_Media/All_Subs/YouTube/Försvarsmakten || TUAV-pilot： Johan || XdTBNkuxRTI.sv.srt"])
