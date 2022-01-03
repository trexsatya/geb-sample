import geb.Page

class HomePage extends Page {

//    static at = { title == "C Guitar Chord - Guitar Chords Chart - 8notes.com" }

//    static content = {
////        manualsMenu { module(ManualsMenuModule) }
//        header { $("h1", text: contains("Guitar Chords Chart")) }
//        chordList { header.next("div").next("div") }
//        chords { chordList.find("a.alphabet2") }
//        allVariantsOfChord { chordList.find("div#hid").find("a.guitarx") }
//    }

    def scrap(url) {
        browser.go(url)
        browser.waitFor { browser.title.contains("Guitar Chord - Guitar Chords Chart - 8notes.com") }
//        def header = browser.$("h1", text: contains("Guitar Chords Chart"))
//        def chordList = header.next("div").next("div")
//        def chords = chordList.find("a.alphabet2")
//        def allVariantsOfChord =  chordList.find("div#hid").find("a.guitarx")
//
//        allVariantsOfChord.collect { it.attr("href")}.each {
//            println(it)
//        }

        def variants = browser.$("table.piano_table").collect { it.find("tr").last().find("td").collect { it.text() }.findAll { it != '-'}.collect {"'$it'"}} as Set


        def chordName = browser.$("h2").first().text().split(":")[1].trim().replace("\n", " ").replace("Also known as", "a.k.a")
//        println(chordName)
//        println(variants)
//        def distinctNotes = variants.flatten() as Set
        println("'${chordName}': $variants" )
//        println()
    }
}
