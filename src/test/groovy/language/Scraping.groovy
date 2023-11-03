import com.google.common.base.Splitter
import geb.Browser
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture

System.setProperty("webdriver.gecko.driver", "/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/lib/geckodriver")
System.setProperty("webdriver.chrome.driver", "/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/lib/chromedriver")
System.setProperty("geb.env", "firefox")

static def extract(String redditLink) {
    def future = new CompletableFuture<String>()
    Browser.drive {
        go redditLink
        waitFor(10) { $("div[slot=\"comment\"]").size() > 0 }
        Thread.sleep(4000)
        if($("button", text: "Read more").size() > 0) {
            $("button", text: "Read more").click()
        }

        6.times {
            browser.driver.executeScript("window.scrollTo(0, document.body.scrollHeight);")
            Thread.sleep(2000)
            if($("button", text: "View more comments").size() > 0) {
                $("button", text: "View more comments").click()
            }
        }

        def text = "-----[$redditLink]-----\n"
        $("p").each {
            text += it.text() + "\n"
        }
        future.complete(text)
    }
    return future.get()
}

def redditLinks = new JsonSlurper().parse(new File("/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/src/test/groovy/redditLinks.json"))
(redditLinks.toScrape as Set).each {
    scrapeIfNeeded(it, redditLinks)
}

private void scrapeIfNeeded(it, redditLinks) {
    if(redditLinks.scraped.contains(it)) {
        return
    }
    def out = extract(it).replace("Reddit and its partners use cookies and similar technologies to provide you with a better experience.By accepting all cookies, you agree to our use of cookies to deliver and maintain our services and site, improve the quality of Reddit, personalize Reddit content and advertising, and measure the effectiveness of advertising.By rejecting non-essential cookies, Reddit may still use certain cookies to ensure the proper functionality of our platform.For more information, please see our Cookie Notice and our Privacy Policy.", "")
    println(out)

    def outFile = new File("/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/src/test/groovy/Reddit.txt")
    outFile.text = outFile.text + "\n" + out

    redditLinks.scraped.add(it)
    new File("/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/src/test/groovy/redditLinks.json").text = JsonOutput.toJson(redditLinks)
}


