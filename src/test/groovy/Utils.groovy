import groovy.json.JsonSlurper
import groovy.sql.Sql
import wslite.http.auth.HTTPBasicAuthorization
import wslite.rest.ContentType
import wslite.rest.RESTClient

class Utils {
    static def processedFile = new File("/Users/satyendra.kumar/IdeaProjects/geb-sample/src/test/groovy/processed")

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

//        readCSV(downloadedFile("output (9).txt")).each {
//            def matches = expected[it.aggregate_krn] == it.dics_count
//            if(!matches) {
//                println("doesn't match for $it")
//            } else {
//                println("matches for $it")
//            }
//        }

        print()
        def domiciles = getDomiciles()
        def out = []

//        readCSV(downloadedFile("Coeo_DuplicateDICs_OpenClaims.csv")).each { rec ->
//            def cc = domiciles.find { it.id == rec.domicile}.country_code
//            rec['domicile'] = cc
//            out << rec
//        }
//
//        out = out.toSorted { it.domicile }
//        println out
//        writeCSV(out, downloadedFile("1_Coeo_DuplicateDICs_OpenClaims.csv"))


//        writeCSV(combineFiles("/Users/satyendra.kumar/Downloads/duplicated_dics_2"), downloadedFile("duplicate_dics.csv"))

//        println combineFiles("/Users/satyendra.kumar/Downloads/duplicate_dics").take(2)


//        paginate((11296665..34005660), 20000).each {
//            def query = """
//with dics as
//(
//  select * from
//   ( select SE.key business_event_id, DIC.debt_item di_id, DIC.id dic_id, aggregate_id
//                       , row_number() over(partition by SE.aggregate_version, SE.aggregate_id, DIC.debt_item, DIC.type, change_amount, transaction_id, dca_reference) rn
//                  from debt_item_change DIC
//                           join sal_event SE on DIC.sal_event = SE.id
//                  where dic.id >= ${it[0]} and dic.id <= ${it[1]}
// ) T where rn > 1
//)
//select C.key, C.state, DG.key, dca.key, business_event_id, DI.key debt_item, dics.dic_id from
//   Claim C
//   join debt D on D.claim = C.id
//   join debt_item DI on DI.debt = D.id
//   join DICs on DICs.di_id = DI.id
//   join assignment A on A.claim = C.id
//   join dca_client DC on A.dca_client = DC.id
//   join dca on DC.dca = dca.id
//   join dca_group DG on dca.dca_group = DG.id
//   where not C.is_dry_run
//;
//with dics as
//(
//  select * from
//   ( select SE.key business_event_id, DIC.debt_item di_id, DIC.id dic_id, aggregate_id
//                       , row_number() over(partition by SE.aggregate_version, SE.aggregate_id, DIC.debt_item, DIC.type, change_amount, transaction_id, dca_reference) rn
//                  from debt_item_change DIC
//                           join sal_event SE on DIC.sal_event = SE.id
//                  where dic.id >= ${it[1] - 100} and dic.id <= ${it[1] + 100}
// ) T where rn > 1
//)
//select C.key, C.state, DG.key, dca.key, business_event_id, DI.key debt_item, dics.dic_id from
//   Claim C
//   join debt D on D.claim = C.id
//   join debt_item DI on DI.debt = D.id
//   join DICs on DICs.di_id = DI.id
//   join assignment A on A.claim = C.id
//   join dca_client DC on A.dca_client = DC.id
//   join dca on DC.dca = dca.id
//   join dca_group DG on dca.dca_group = DG.id
//   where not C.is_dry_run
//;
//"""
//
//            println(query)
//        }

//        def multipleAssignedClaims = readCSV(downloadedFile("mutiple_assigned_claims.txt")).collect { it['key']}
//
//        def duplicates = readCSV(downloadedFile("coeo_duplicate_dics.csv"))
//
//        def claimsWithDuplicateDICs = duplicates.collect { "'${it.claim_key}'"} as Set
//
//        claimsWithDuplicateDICs.collate(2000).each {
//            println(claimsWithDuplicateDICs.join(","))
//        }

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
