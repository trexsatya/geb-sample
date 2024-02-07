package language

import groovy.json.JsonOutput

import static language.VTTToSRT.listFilesRecursively

class PrepareSrtIndex {
    static void main(String[] args) {
        def path = new File("/Users/satyendra.kumar/Documents/Swedish_Media/All_Subs/Youtube");
        def out = []
        listFilesRecursively(path, ".sv.srt").each { file ->
            out << [link: getLinkFromFileName(file), name: (file.name - ".sv.srt")]
        }

        println(out.join("\n"))
        outputFile(path.path + "/index.json").text = JsonOutput.toJson(out)
    }

    static File outputFile(String path) {
        def f = new File(path);
        if(!f.exists()) {
            f.createNewFile();
        }
        return f
    }

    static String getLinkFromFileName(File file) {
        def n = file.name - ".sv.srt"

        n.split(" \\|| ").last()
    }
}
