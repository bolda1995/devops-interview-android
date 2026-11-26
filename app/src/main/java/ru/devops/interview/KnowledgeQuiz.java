package ru.devops.interview;

import java.io.*;
import java.util.*;
final class KnowledgeQuiz {
    static final class Item {
        final String topic,question,correct,explanation;
        final List<String> options;
        Item(String topic,String[] fields){
            this.topic=topic;question=fields[0];correct=fields[1];explanation=fields[5];
            options=Collections.unmodifiableList(Arrays.asList(fields[1],fields[2],fields[3],fields[4]));
        }
    }
    static List<Item> read(BufferedReader reader)throws IOException{
        ArrayList<Item> bank=new ArrayList<>();String line,topic="";int n=0;
        while((line=reader.readLine())!=null){
            n++;line=line.trim();if(line.isEmpty())continue;
            if(line.startsWith("# ")){topic=line.substring(2);continue;}
            String[] values=line.split("\\|",-1);
            if(topic.isEmpty()||values.length!=6)throw new IOException("Quiz row "+n);
            for(String value:values)if(value.trim().isEmpty())throw new IOException("Empty quiz field "+n);
            Item item=new Item(topic,values);
            if(new HashSet<>(item.options).size()!=4||item.question.contains("[…]"))throw new IOException("Invalid quiz options "+n);
            bank.add(item);
        }
        return Collections.unmodifiableList(bank);
    }
}
