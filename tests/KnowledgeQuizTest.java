package ru.devops.interview;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
public class KnowledgeQuizTest {
    public static void main(String[] args)throws Exception{
        List<KnowledgeQuiz.Item> items;
        try(BufferedReader r=Files.newBufferedReader(Paths.get(args[0]))){items=KnowledgeQuiz.read(r);}
        Set<String> expected=new HashSet<>(), actual=new HashSet<>(), questions=new HashSet<>();
        Matcher matcher=Pattern.compile("(?m)^## (.+) \\(\\d+\\)$").matcher(Files.readString(Paths.get(args[1])));
        while(matcher.find())expected.add(matcher.group(1));
        for(KnowledgeQuiz.Item item:items){
            actual.add(item.topic);
            if(!questions.add(item.question)||item.options.size()!=4||!item.options.contains(item.correct))throw new AssertionError(item.question);
        }
        if(!actual.containsAll(expected)||items.size()<86)throw new AssertionError("Coverage mismatch "+expected+" vs "+actual);
        System.out.println("Verified "+items.size()+" knowledge questions across "+actual.size()+" topics");
    }
}
