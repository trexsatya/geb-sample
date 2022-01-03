import geb.Browser

System.setProperty("webdriver.gecko.driver", "/Users/satyendra.kumar/Documents/MyProjects/geb-sample/lib/geckodriver")
Browser.drive {
    go "https://www.8notes.com/guitar_chord_chart/C.asp"

    assert $("h1").text() == "Guitar Chords Chart"


}
