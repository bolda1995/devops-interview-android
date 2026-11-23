package ru.devops.interview;

import java.util.*;
import java.util.regex.*;

/** Lightweight term comparison; it does not determine semantic correctness. */
public final class AnswerAnalyzer {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}_+.-]*");
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
        "ответ", "эталонный", "опорный", "проверка", "практическая", "контекст", "действие", "данные", "сначала", "после", "через", "которые", "который", "также", "если", "этого", "этом", "быть", "может", "для", "или", "при", "как", "что", "это", "the", "and", "with", "that", "from"));
    private final Map<String,Integer> documentFrequency = new HashMap<>();
    private final int documents;
    public AnswerAnalyzer(Collection<String> references) {
        documents = references.size();
        for (String reference : references) for (String term : terms(reference).keySet())
            documentFrequency.put(term, documentFrequency.containsKey(term) ? documentFrequency.get(term) + 1 : 1);
    }
    private static Map<String,String> terms(String value) {
        Map<String,String> found = new LinkedHashMap<>();
        Matcher matcher = WORD.matcher(value.toLowerCase(Locale.ROOT).replace('ё','е'));
        while (matcher.find()) {
            String word = matcher.group().replaceAll("[.+-]+$", "");
            if (word.length() < 3 || STOP.contains(word) || word.matches("[0-9]+")) continue;
            String key = word.matches("[а-я]{6,}") ? word.substring(0, 5) : word;
            if (!found.containsKey(key)) found.put(key, word);
        }
        return found;
    }
    public String compare(String reference, String response) {
        if (response.trim().isEmpty()) return "Сначала напиши ответ.";
        Map<String,String> ref = terms(reference), actual = terms(response);
        List<String> keys = new ArrayList<>(ref.keySet());
        Collections.sort(keys, (a,b) -> Integer.compare(documentFrequency.containsKey(a) ? documentFrequency.get(a) : documents, documentFrequency.containsKey(b) ? documentFrequency.get(b) : documents));
        List<String> matched = new ArrayList<>(), examples = new ArrayList<>();
        for (String key : keys) {
            if (actual.containsKey(key) && matched.size() < 8) matched.add(ref.get(key));
            else if (!actual.containsKey(key) && examples.size() < 8) examples.add(ref.get(key));
        }
        String result = matched.isEmpty() ? "Совпадений среди выделенных слов не найдено." : "Совпавшие слова: " + join(matched);
        if (!examples.isEmpty()) result += "\n\nДругие слова из примера для сравнения: " + join(examples);
        return result + "\n\nЭто сравнение слов, а не оценка правильности: синонимы, отрицания и смысл могут не учитываться. Сверь ответ с примером.";
    }
    private static String join(List<String> words) { StringBuilder s=new StringBuilder(); for(String word:words){if(s.length()>0)s.append(", ");s.append(word);}return s.toString(); }
}
