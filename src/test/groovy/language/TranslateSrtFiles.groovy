import geb.Browser
import language.SRTParser
import language.Subtitle

import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.util.concurrent.CompletableFuture

System.setProperty("webdriver.gecko.driver",
                   "/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/lib/geckodriver")


def translateSvToEn(sv) {
    def serviceUrl = "https://translate.google.com/?sl=sv&tl=en"
    def future = new CompletableFuture<String>()
    Browser.drive {
        go serviceUrl

        try {
            waitFor(5) { $('span', text: 'Accept all')}
            $('span', text: 'Accept all').click()
        } catch (Error e) {
            println(e)
        }

        waitFor { $('textarea[aria-label="Source text"]')}

        $('textarea[aria-label="Source text"]').value(sv)

        sleep(5000)

        waitFor { $('button[aria-label="Copy translation"]').attr("data-tooltip-enabled") == "true" }
        $('button[aria-label="Copy translation"]').click()

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
        String res = clipboard.getContents(null).getTransferData(DataFlavor.stringFlavor)

        res = res.split("\n\n").collect {
            def (id, time, rest) = it.split("\n")
            time = time.replaceAll(" ", "")
            time = time.replaceAll("->", " --> ")

            return "$id\n$time\n$rest"
        }.join("\n\n")

        println("result: $res")

        future.complete(res)
    }

    return future
}

def links = [
        "https://skojig.com/img/normal/20231026/BSIE/20231026.jpg"
];

def dir = "/Users/satyendra.kumar/Documents/Swedish_Media/All_Subs/YouTube"
def files = []

new File(dir).eachFileRecurse {
    if(isSwedishSrt(it) && !existsInEnglish(it)) {
        files.add(it.path)
    }
}

println("Files to translate: ${files.size()}, ${files.join("\n")}")

private static boolean existsInEnglish(File it) {
    new File(it.parent + "/"  + it.name.replace(".sv.srt", ".en.srt")).exists()
}

private static boolean isSwedishSrt(File it) {
    it.name.endsWith(".sv.srt")
}


//println String.format("%02d:%02d:%02d,%03d", 0, 8, 5, 6)

int done = 0
files.forEach { String file ->
    println("File (${files.size() - done}): $file")
    File svSrt = new File(file)
    File enSrt = new File(svSrt.parent + "/"  + svSrt.name.replace(".sv.srt", ".en.srt"))

    if(!enSrt.exists())
        translateSrt(file, enSrt)
    else println("$enSrt exists")

    done++
}

private def translateSrt(file, File saveHere) {
    int numChars
    def srt = SRTParser.getSubtitlesFromFile(file)
    def chunks = chunks(srt, 5000)

    def en = ""

    List<CompletableFuture> futures = []

    for (def chunk in chunks) {
        def sv = srtToString(chunk)

        try {
             futures << translateSvToEn(sv)
        } catch (Error e) {
            println("Error processing $file : $e")
//            futures.each { it}
            en = ""
            break
        }
    }

    if(futures.isEmpty()) return

    String enSrt = futures.stream()
            .reduce((chunk1Future, chunk2Future) ->
                            chunk1Future.thenCompose { chunk1 -> CompletableFuture.supplyAsync { chunk1 + "\n\n" + chunk2Future.get() }}).get().get()

    println(enSrt)
    saveHere.withPrintWriter {
        it.write(enSrt)
    }
}

static def srtToString(List<Subtitle> list) {
    return list.collect { "${it.id}\n${it.startTime} --> ${it.endTime}\n${it.text}\n"}.join("\n")
}

static def chunks(List<Subtitle> list, int maxChars) {
    def res = []
    def currentChunk = []
    int charsInCurrentChunk = 0

    list.each {line ->
        if(charsInCurrentChunk + line.toString().length() > maxChars) {
            charsInCurrentChunk = 0
            res << currentChunk
            currentChunk = []
        }
        charsInCurrentChunk += line.toString().length()
        currentChunk << line
    }

    if(!currentChunk.isEmpty()) res.add(currentChunk)
    return res
}
