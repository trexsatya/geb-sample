import dorkbox.notify.Notify
import dorkbox.notify.Pos
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.time.Duration
import java.time.Instant

class GroovyTimerTask extends TimerTask {
    Closure closure
    void run() {
        closure()
    }
}

class TimerMethods {
    static TimerTask runEvery(Timer timer, long delay, long period, Closure codeToRun) {
        TimerTask task = new GroovyTimerTask(closure: codeToRun)
        timer.schedule task, delay, period
        task
    }
}

class LearningTool {
    static File db = null
    static File swedishText = null

    static JsonSlurper jsonSlurper = new JsonSlurper()
    public static final int MINUTES = 60000

    static def notifications = []
    static void main(String[] args) {
        def filePath = "/Users/satyendra.kumar/IdeaProjects/geb-sample/src/main/resources"
        db = new File("$filePath/db.json")
        swedishText = new File(filePath + "/swedish.txt")
        def timer = new TimerMethods()

        def task = timer.runEvery(new Timer(), 1000, 30*MINUTES) {
            closePopups()
            def popup = showPopup()
        }
    }

    private static List<Object> closePopups() {
        try {
            notifications?.findAll { it }?.each { it?.close() }
        } catch (e) {

        }
    }

    private static def showPopup() {
        def text = getText()

        def notify = Notify.create()
                .title("")
                .position(Pos.TOP_RIGHT)
                .hideAfter(30 * MINUTES)
                .text(text)
                .onAction {
                    def popup = showPopup()
                    closePopups()
                    notifications?.clear()
                    notifications?.add(popup)
                }
        notify.show();
        println "${Instant.now()} $text \n"
        return notify
    }

    static String getText() {
        List data = jsonSlurper.parse(db)
        def (int selectedLine, String toShow) = getRandomLine(data)

        markLastRead(data, selectedLine)
        saveTimestamp(data)

        return toShow
    }

    static options = [[name: 'NEW'],
                      [name: 'REPEAT', hours: [0, 3]],
                      [name: 'REPEAT', hours: [3, 15]],
                      [name: 'REPEAT', hours: [15, 26]],
                      [name: 'REPEAT', hours: [26, 36]],
                      [name: 'REPEAT', hours: [36, 42]],
                      [name: 'REPEAT', hours: [42, 64]],
                      [name: 'REPEAT', hours: [64, 128]],
                      [name: 'REPEAT', hours: [128, 256]],
                      [name: 'REPEAT', hours: [256, 512]],
                      [name: 'REPEAT', hours: [512, 1024]],
                      [name: 'REPEAT', hours: [1024, 2048]],
                      [name: 'REPEAT', hours: [2048, 4096]],
                      [name: 'REPEAT', hours: [4096, Integer.MAX_VALUE]]]

    static selectedOption = 0

    static randomFromList(list) {
        def rand = new Random()
        def randomNum = rand.nextInt(list.size())
        return list[randomNum]
    }

    static boolean isAround(int num, int compareAgainst, int plusMinus) {
        def abs = Math.abs(num - compareAgainst)
        0 <= abs && abs <= plusMinus
    }

    static hoursSinceLastRead(entry) {
        if(!entry.lastRead) return Integer.MAX_VALUE - 1
        Duration.between(Instant.now(), Instant.parse(entry.lastRead)).toHours()
    }

    private static List getRandomLine(db) {
        def entries = [:]
        def lines = swedishText.text.split("\\n\\n+").eachWithIndex { String entry, int i ->
            entries[i] = [lineNumber: i, text: entry]
        }

        def filtered = []

        def option = options[selectedOption]
        if(option.name == 'NEW') {
            filtered = entries.values().findAll { !it.lastRead }
        }

        if(filtered.isEmpty() || option.name == 'REPEAT') {
            def hh = option.hours
            filtered = entries.values().findAll { def h = hoursSinceLastRead(it); hh[0] <= h && h <= hh[1] }
        }

        if(filtered.isEmpty()) {
            filtered = entries.values()
        }

        def selected = entries[randomFromList(filtered)['lineNumber']]

        incrementOption()

        [selected['lineNumber'], selected['text']]
    }

    private static void incrementOption() {
        selectedOption += 1
        if (selectedOption >= options.size()) {
            selectedOption = 0
        }
    }

    private static void markLastRead(List data, selectedLine) {
        def entry = data.find { it.lineNumber == selectedLine }
        if (!entry) {
            entry = [lineNumber: selectedLine]
            data.push(entry)
        }
        entry.lastRead = Instant.now().toString()
    }

    private static void saveTimestamp(List data) {
        def writer = db.newPrintWriter()
        writer.println(JsonOutput.toJson(data))
        writer.flush()
        writer.close()
    }
}
