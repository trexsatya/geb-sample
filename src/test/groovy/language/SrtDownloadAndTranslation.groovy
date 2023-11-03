import geb.Browser

import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.util.concurrent.CompletableFuture

System.setProperty("webdriver.gecko.driver", "/Users/satyendra.kumar/Documents/MyProjects/lib/geckodriver")

def downloadVideoAndSubtitle(youtubeUrl) {
    Browser.drive {
        go "https://downsub.com/?url=$youtubeUrl"

        def videoWithSubtitle = { $("a", text: "Download Full Video With Subtitle") }
        waitFor {
            videoWithSubtitle()
        }

        def mainSubtitleContainer = videoWithSubtitle().parent().parent().parent()
        def swedishSubtitle = mainSubtitleContainer.find("span", text: "Swedish")
        if (!swedishSubtitle) {
            swedishSubtitle = mainSubtitleContainer.next().find("span", text: "Swedish")
        }

        if(!swedishSubtitle) {
            mainSubtitleContainer.next().find("span", text: "Swedish")
            println("No Swedish sub found")
            println("$youtubeUrl Done!")
            return
        }

        def subtitles = swedishSubtitle.parent()
        def srtButton = subtitles.find("button", text: "SRT")
        srtButton.click()

        sleep(3000)

        go "https://y2down.cc/en/youtube-playlist.html"
        waitFor { $("#link")}
        $("#link").value("$youtubeUrl")
        $("#format").click()
        $("#format").find("option").find{ it.value() == "mp3" }.click()
        $('#load').click()
        waitFor(8) { $('.download-card') }

        def downloadMp3Button = {
            $('.download-card').find("button", text: "Download")
        }
        waitFor(300) { downloadMp3Button() }
        waitFor(300) { downloadMp3Button().attr("disabled") != "" }
        waitFor(300) { downloadMp3Button().attr("disabled") != "true" }

        $('.download-card').find("button", text: "Download").click()
        sleep(3000)

        println("$youtubeUrl Done!")
    }
}

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

def dir = "/Users/satyendra.kumar/Documents/Swedish_Media"
def files = []

new File(dir).eachFileRecurse {
    if(it.name.endsWith(".srt")) {
        files.add(it.path)
    }
}


//println String.format("%02d:%02d:%02d,%03d", 0, 8, 5, 6)

files.take(0).forEach { file ->
    println("File: $file")
    File svSrt = new File(file)
    File enSrt = new File(svSrt.parent + "/"  + svSrt.name.replace(".srt", "_EN.srt"))

    if(!enSrt.exists())
        translateSrt(file, enSrt)
    else println("$enSrt exists")
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
