package ru.devops.interview;

import java.util.*;
import java.util.regex.*;

final class LearningChain {
    private static final Pattern WORD=Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}_-]{3,}");
    static final class Step {
        final String explanation, prompt, correct;
        final List<String> options;
        Step(String explanation,String prompt,String correct,List<String> options){
            this.explanation=explanation;this.prompt=prompt;this.correct=correct;this.options=options;
        }
    }
    static List<Step> build(String reference,Random random){
        List<Step> steps=new ArrayList<>();
        if(reference==null||reference.trim().isEmpty())return steps;
        List<String> fragments=new ArrayList<>();
        for(String line:reference.replaceFirst("^(Эталонный ответ|Опорный ответ|Опорный план диагностики):\\s*", "").split("\\n+")){
            line=line.trim();
            if(line.isEmpty()||line.equals("---")||line.startsWith("\u0060\u0060\u0060"))continue;
            for(String sentence:line.split("(?<=[.!?;])\\s+")){
                String remaining=sentence.trim();
                while(remaining.length()>350){
                    int cut=remaining.lastIndexOf(' ',350);
                    if(cut<=0)break;
                    fragments.add(remaining.substring(0,cut).trim());
                    remaining=remaining.substring(cut).trim();
                }
                if(!remaining.isEmpty())fragments.add(remaining);
            }
        }
        // Keep neighboring short fragments together, preserving their original order.
        List<String> chunks=new ArrayList<>();
        StringBuilder pending=new StringBuilder();
        for(String fragment:fragments){
            if(pending.length()>0&&pending.length()+fragment.length()>350){
                chunks.add(pending.toString());pending.setLength(0);
            }
            if(pending.length()>0)pending.append('\n');
            pending.append(fragment);
            if(pending.length()>=130){chunks.add(pending.toString());pending.setLength(0);}
        }
        if(pending.length()>0)chunks.add(pending.toString());
        List<String> vocabulary=new ArrayList<>();
        Matcher vocab=WORD.matcher(reference);
        while(vocab.find())if(!containsIgnoreCase(vocabulary,vocab.group()))vocabulary.add(vocab.group());
        for(String chunk:chunks){
            Matcher matcher=WORD.matcher(chunk);
            List<int[]> positions=new ArrayList<>();
            while(matcher.find())positions.add(new int[]{matcher.start(),matcher.end()});
            if(positions.isEmpty())continue;
            // Prefer substantial terms, rather than very short connecting words.
            Collections.sort(positions,(a,b)->Integer.compare(termWeight(chunk.substring(b[0],b[1])),termWeight(chunk.substring(a[0],a[1]))));
            int[] chosen=positions.get(random.nextInt(Math.min(3,positions.size())));
            String correct=chunk.substring(chosen[0],chosen[1]);
            String prompt=chunk.substring(0,chosen[0])+"[…]"+chunk.substring(chosen[1]);
            List<String> pool=new ArrayList<>(vocabulary);
            Collections.shuffle(pool,random);
            List<String> options=new ArrayList<>();options.add(correct);
            for(String candidate:pool){
                if(options.size()==4)break;
                if(!containsIgnoreCase(options,candidate))options.add(candidate);
            }
            Collections.shuffle(options,random);
            steps.add(new Step(chunk,prompt,correct,options));
        }
        return steps;
    }
    private static int termWeight(String word){
        return word.length()+ (word.matches(".*[A-Za-z0-9_-].*")?8:0);
    }
    private static boolean containsIgnoreCase(List<String> list,String value){
        for(String item:list)if(item.equalsIgnoreCase(value))return true;
        return false;
    }
}
