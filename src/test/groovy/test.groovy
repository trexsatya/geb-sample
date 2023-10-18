import geb.Browser

System.setProperty("webdriver.gecko.driver", "/Users/satyendra.kumar/Documents/MyProjects/lib/geckodriver")

def download(youtubeUrl) {
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

[]
        .forEach {
            try {
                download(it)
            } catch (Error e) {
                println("Error processing $it")
            }
        }
