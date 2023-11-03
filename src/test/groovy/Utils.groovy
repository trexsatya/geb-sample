import groovy.json.JsonSlurper
import groovy.sql.Sql
import wslite.http.auth.HTTPBasicAuthorization
import wslite.rest.ContentType
import wslite.rest.RESTClient

class Utils {
    static def processedFile = new File("/Users/satyendra.kumar/IdeaProjects/geb-sample/src/SrtDownloadAndTranslation/groovy/processed")

    static def processed() {
        processedFile.readLines()
    }

    static def addToProcessed(items) {
        items.each { processedFile.append(it + "\n") }
    }

    static void main(String[] args) {

        def data = readCSV(downloadedFile("BundleToReenrich.txt"))
        def client = new RESTClient()
        client.defaultAcceptHeader = ContentType.JSON
        client.authorization = new HTTPBasicAuthorization("", getPasswordFromScriptsRepo())
        //        client.post([])
        def expected = """
""".split("\n").collect { it.split(",") }.collectEntries {[(it[0]): it[1]] }

    }

    static def paginate(int total, int pageSize) {
        return paginate((0..total), pageSize)
    }

    static def paginate(List items, int pageSize) {
        int offset = 0
        def res = []
        for (offset = 0; offset < items.size(); offset += pageSize) {
            res << (items[offset])
        }
        return [res, res.drop(1).collect { it -1 } + items.last()].transpose()
    }

    private static List<Object> combineFiles(String directory) {
        def combined = []

        new File(directory).eachFile {
            def data = readCSV(it)
            combined += data
        }

//        combined = combined.toSorted { it.age_in_days }.reverse()
        combined
    }

    static def writeCSV(List records, File file) {
        String eol = System.getProperty("line.separator");
        def headers = records[0].keySet().join(",")

        try (Writer writer = new FileWriter(file)) {
            writer.println(headers)
            records.each { rec ->
                writer.append(rec.values().join(",")).append(eol)
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }

    static def readCSV(File file) {
        def lines = file.readLines()
        def namesUsed = [:]
        def headers = lines.get(0).split(",").collect { it.trim() }
        def finalHeaderNames = []
        headers.each {
            if(namesUsed[it]) {
                finalHeaderNames.add("${it}_${namesUsed[it] + 1}")
                namesUsed[it] = namesUsed[it] + 1
            } else {
                finalHeaderNames.add(it)
                namesUsed[it] = 1
            }
        }
        def data = []
        lines.drop(1).each { ln ->
            def values = ln.split(",")
            def record = [:]
            for (int i = 0; i < values.length; i++) {
                record[finalHeaderNames[i]] = values[i].replaceAll("\"", "").trim()
            }
            data.push(record)
        }
        return data
    }

    static downloadedFile(String name) {
        def DOWNLOAD_FOLDER = "/Users/satyendra.kumar/Downloads"
        new File("$DOWNLOAD_FOLDER/$name")
    }

    static String getPasswordFromScriptsRepo() {
        return new JsonSlurper().parse(new File("/Users/satyendra.kumar/IdeaProjects/scripts/api-requests/http-client.private.env.json"))['eu-production']['ldap_password']
    }

    static def getDomiciles() {
        return readCSV(new File("/Users/satyendra.kumar/Documents/domiciles.txt"))
    }
}
