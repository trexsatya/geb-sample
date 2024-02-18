package language

import com.google.common.base.CharMatcher
import groovy.json.JsonSlurper
import org.apache.commons.lang3.StringUtils

import javax.script.*
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Query

import java.nio.charset.Charset
import java.text.Normalizer

public class DbService implements AutoCloseable {
    private final Driver driver;

    static def outputFile = new File("/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/src/test/groovy/language/output.log")

    public DbService(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password),
                                      Config.builder()
                                            .withoutEncryption()
                                            .withMaxConnectionPoolSize(50).build());

    }

    @Override
    public void close() throws RuntimeException {
        driver.close();
    }

    public void addWikiEntry(WikiEntry wikiEntry) {
        def word = wikiEntry.word()
        word = org.apache.commons.lang3.StringUtils.strip(word, "'")

        try (var session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(new Query("MERGE (a:SingleWord {text: \$word})  ", [word: word]));
                wikiEntry.synonyms().each {
                    tx.run(new Query("MERGE (a:SingleWord {text: \$synonym})  ", [synonym: it]));
                    tx.run(new Query(""" MATCH (x:SingleWord {text: \$word}) MATCH (y:SingleWord {text: \$synonym}) MERGE (x)-[:SYNONYM]-(y)   """, [word: word, synonym: it]));
                }
                wikiEntry.antonyms().each {
                    tx.run(new Query("MERGE (a:SingleWord {text: \$antonym})  ", [antonym: it]));
                    tx.run(new Query(""" MATCH (x:SingleWord {text: \$word}) MATCH (y:SingleWord {text: \$antonym}) MERGE (x)-[:ANTONYM]-(y)   """, [word: word, antonym: it]));
                }
                wikiEntry.relatedWords().each {
                    tx.run(new Query("MERGE (a:SingleWord {text: \$related})  ", [related: it]));
                    tx.run(new Query(""" MATCH (x:SingleWord {text: \$word}) MATCH (y:SingleWord {text: \$related}) MERGE (x)-[:RELATED]-(y)   """, [word: word, related: it]));
                }

                wikiEntry.similarSemantics().each {
                    tx.run(new Query("MERGE (a:SingleWord {text: \$similar})  ", [similar: it]));
                    tx.run(new Query(""" MATCH (x:SingleWord {text: \$word}) MATCH (y:SingleWord {text: \$similar}) MERGE (x)-[:SIMILAR]-(y)   """, [word: word, similar: it]));
                }

                tx.run(new Query(""" MATCH (n {text: \$word}) set n.translations = \$list """, [word: word, list: wikiEntry.translations().collect { "'$it'".toString() }]))
                tx.run(new Query(""" MATCH (n {text: \$word}) set n.usualConstructions = \$list """, [word: word, list: wikiEntry.usualConstructions().collect { "'$it'".toString() }]))
                tx.run(new Query(""" MATCH (n {text: \$word}) set n.categories = \$list """, [word: word, list: wikiEntry.categories().collect { "'$it'".toString() }]))
                tx.run(new Query(""" MATCH (n {text: \$word}) set n.wikiProcessed = true """, [word: word]))
            })
        }
    }


    List getWordsToTranslate() {
        try (var session = driver.session()) {
            var greeting = session.executeWrite(tx -> {
                var query = new Query(
                        "match (n) where n.wikiProcessed=true and isEmpty(n.translations) return n.text as word"
                );
                var result = tx.run(query);
                return result.list()
            });
            return greeting.collect { it.get(0) }
        }
    }

    void addTranslation(String word, List<String> translations) {
        addTranslations([(word): translations])
    }

    void addTranslations(Map<String, List<String>> translationsMap) {
        def sanitize = { String word, List<String> translations ->
            word = StringUtils.strip(word, "\"")
            translations = translations.collect { StringUtils.strip(it, "\"").replaceAll("\"", "") }

            return [word, translations]
        }

        try (var session = driver.session()) {
            var greeting = session.executeWrite(tx -> {
                translationsMap.each {
                    def (word, translations) = sanitize(it.key, it.value)

                    var query = new Query(
                            "match (n) where n.text=\$word set n.translations = coalesce(n.translations, []) + [\$translations]",
                            [word: "$word".toString(), translations: translations.join(",")]
                    );
                    tx.run(query);
                }
            });
        }
    }

    List getWordsToProcessWiki() {
        try (var session = driver.session()) {
            var greeting = session.executeWrite(tx -> {
                var query = new Query(
                        "MATCH (n) where n.wikiProcessed is null RETURN n.text"
                );
                var result = tx.run(query);
                return result.list()
            });
            return greeting.collect { it.get(0) }
        }
    }

    public void addWordIfNotExists(String text) {
        text = text.toLowerCase();
        try (var session = driver.session()) {
            String finalText = text;
            var greeting = session.executeWrite(tx -> {
                var query = new Query(
                        "MERGE (a:SingleWord {text: '$text'}) RETURN a.text"
                );
                var result = tx.run(query);
                return result.single().get(0).asString();
            });
        }
    }

    public static void main(String... args) {
        def file = new File("/Users/satyendra.kumar/Documents/PersonalProjects/geb-sample/src/test/groovy/language/wiki_sv_verbs.json")
        List<String> links = new JsonSlurper().parse(file) as List<String>

        int count = 0
        try (var dictionary = getDb()) {
            links.each {
                def word = it.split("/wiki/")[-1]
                word.replaceAll('"', '')
                word = word.trim()
                word = URLDecoder.decode(word, Charset.forName("UTF-8"))
                word = Normalizer.normalize(word, Normalizer.Form.NFC)
                try {
                    dictionary.addWordIfNotExists(word)
                } catch (e) {
                    e.printStackTrace()
                }
                println("${count++} done out of ${links.size()}")
            }
        }
    }

    private static void addWordsToDictionaryFromSubtitles() {
        def dir = "/Users/satyendra.kumar/Documents/Swedish_Media/All_Subs/YouTube";
        try (var dictionary = getDb()) {
            dictionary.addWordIfNotExists("med")


            new File(dir).eachFileRecurse { File file ->
                if (isSwedishSrt(file)) {
                    try {
                        addSwedishWords(file, dictionary)
                        outputFile.append("Done: $file\n")
                    } catch (e) {
                        outputFile.append("Error file: $file: $e\n")
                    }
                }
            }
        }
    }

    private static DbService getDb() {
        new DbService("bolt://localhost:7687", "satya", System.getenv("DB_PWD"))
    }

    private static void addSwedishWords(File file, DbService dictionary) {
        def srt = SRTParser.getSubtitlesFromFile(file.path)
        addWordsForFile(srt, dictionary)
    }

    private static List addWordsForFile(ArrayList<Subtitle> srt, DbService dictionary) {
        srt.findAll { it?.text }.each { Subtitle subtitle ->
            tokenize(subtitle.text).each {
                try {
                    dictionary.addWordIfNotExists(it)
                } catch (e) {
                    println("Error: $e")
                }
            }
        }
    }

    private static String[] tokenize(String text) {
//        def bindings = [text: text, response:[:]]

        def scriptText = """
                const segmentor = new Intl.Segmenter([], { granularity: 'word' });
                const segmentedText = segmentor.segment(text);
                let words = Array.from(segmentedText, ({ segment }) => segment).filter(it => it.trim().length > 1);
                console.log(words);
"""

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

        ScriptContext context = engine.getContext();
        StringWriter writer = new StringWriter();
        context.setWriter(writer);

        bindings.put("text", text);
        bindings.put("result", [:]);
        CompiledScript script = ((Compilable) engine).compile(scriptText);
        script.eval();

        String output = writer.toString();

        return output.split(",").collect { it.trim() }
    }

    private static boolean isSwedishSrt(File it) {
        it.name.endsWith(".sv.srt")
    }
}
