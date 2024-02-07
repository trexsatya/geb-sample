package language

import groovy.io.FileType

import java.util.regex.Matcher
import java.util.regex.Pattern


class VTTToSRT {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    static void main(String[] args) throws IOException {
        def path = new File("/Users/satyendra.kumar/Documents/Swedish_Media/All_Subs/");
        listFilesRecursively(path, ".vtt").each { file ->
            translateVTTToSRT(file)
        }

//        removeVTTFiles(path)
    }

    static void translateVTTToSRT(File file) {
        def vtt = file.text.split("\n")
        def item = ""
        def timestampPattern = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) --> (\\d{2}:\\d{2}:\\d{2}\\.\\d{3})")
        def srtItems = []
        for (int i = 1; i< vtt.length; i++) {
            def line = vtt[i]
            def prev = vtt[i-1]
            def matcher = timestampPattern.matcher(line)
            if(matcher.find()) {
                item = startNewSrtItem(prev, item, srtItems, matcher)
            } else {
                line = line.replace("<c.teletext>", "").replace("</c>", "")
                .replace("<c.urfjarran>", "").replace("<c.teletext.skylt>", "")
                .replace("<i>", "").replace("</i>", "").replace("<c>", "")
                item += line + "\n"
            }
        }
        srtItems << item
        srtItems = srtItems.findAll { notAHeader(it) && it.trim().length() > 1}
        def srt = removeExtension(file) + ".srt"
        srtItems.withIndex().each { itm, index ->
            srtItems[index] = (index + 1) + "\n" + itm
        }

        new File(srt).withPrintWriter {
            it.println(srtItems.join("\n"))
        }
    }

    private static boolean notAHeader(it) {
        !it.trim().startsWith("WEBVTT") && !it.trim().startsWith("Kind: ") && !it.trim().startsWith("Language: ")
        && !it.trim().startsWith("STYLE")
    }

    private static String startNewSrtItem(String prev, String item, ArrayList srtItems, Matcher matcher) {
        if (prev.trim().length() > 1) {
            item = item.replace(prev, "")
        }
        srtItems << item
        item = matcher.group(1) + " --> " + matcher.group(2) + "\n"
        item
    }

    private static def removeExtension(File file) {
        def absPath = file.getAbsolutePath()
        return absPath.take(absPath.lastIndexOf('.'))
    }

    static List<File> listFilesRecursively(File directoryPath, String extensions) {
        def res = []
        directoryPath.eachFileRecurse(FileType.FILES) {
            if(it.name.endsWith(extensions)) {
                res << it
            }
        }
        return res
    }

    private static File [] getVTT(File directoryPath, boolean recursive) {
        String[] extensions = new String[]{"vtt", "VTT"};
        List<File> files = listFilesRecursively(directoryPath, ".vtt");
        File[] tmp = (File[]) files.toArray(new File[0]);
        return tmp;
    }

    private static void removeVTTFiles(File path) {
        listFilesRecursively(path, ".vtt").each {
            it.delete()
        }
    }
}