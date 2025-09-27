package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;

public class LemmaFinder {

    private final LuceneMorphology luceneMorphology;

    private static final String VALID_RUSSIAN_WORD = "^[а-яё-]+$";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};


    private static class Holder {
        static final LemmaFinder INSTANCE;
        static {
            try {
                INSTANCE = new LemmaFinder(new RussianLuceneMorphology());
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    public static LemmaFinder getInstance() {
        return Holder.INSTANCE;
    }

    private LemmaFinder(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    public Map<String, Integer> collectLemmas(String text) {
        String[] words = arrayContainsRussianWords(text);
        Map<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank() || !isCorrectWordForm(word)) {
                continue;
            }

            List<String> morphInfo = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(morphInfo)) continue;

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) continue;

            String normalWord = normalForms.get(0);
            lemmas.put(normalWord, lemmas.getOrDefault(normalWord, 0) + 1);
        }
        return lemmas;
    }

    public Set<String> getLemmaSet(String text) {
        String[] textArray = arrayContainsRussianWords(text);
        Set<String> lemmaSet = new HashSet<>();
        for (String word : textArray) {
            if (!word.isEmpty() && isCorrectWordForm(word)) {
                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) continue;
                lemmaSet.addAll(luceneMorphology.getNormalForms(word));
            }
        }
        return lemmaSet;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        String upper = wordBase.toUpperCase(Locale.ROOT);
        for (String property : particlesNames) {
            if (upper.contains(property)) {
                return true;
            }
        }
        return false;
    }

    private String[] arrayContainsRussianWords(String text) {
        return text
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^а-яё\\s-]", " ")
                .trim()
                .split("\\s+");
    }

    private boolean isCorrectWordForm(String word) {
        return word.matches(VALID_RUSSIAN_WORD);
    }
}



