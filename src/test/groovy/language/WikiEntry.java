package language;

import java.util.List;

public record WikiEntry(String word,
                        List<String> translations,
                        List<String> synonyms,
                        List<String> antonyms,
                        List<String> relatedWords,
                        List<String> usualConstructions,
                        List<String> similarSemantics,
                        List<String> categories
                        ) {
}
