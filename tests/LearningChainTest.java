package ru.devops.interview;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class LearningChainTest {
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        require(LearningChain.build(null,new Random()).isEmpty(),"null");
        require(LearningChain.build("   ",new Random()).isEmpty(),"empty");
        String bank=Files.readString(Paths.get(args[0]));
        Pattern headings=Pattern.compile("(?m)^### (Q-\\d+) · .+$");
        Matcher matcher=headings.matcher(bank);
        List<Integer> offsets=new ArrayList<>();List<String> ids=new ArrayList<>();
        while(matcher.find()){offsets.add(matcher.end());ids.add(matcher.group(1));}
        offsets.add(bank.length());
        int count=0,steps=0;
        for(int i=0;i<ids.size();i++){
            String raw=bank.substring(offsets.get(i),offsets.get(i+1));
            StringBuilder answer=new StringBuilder();boolean reading=false;
            for(String line:raw.split("\\R")){
                if(line.startsWith("## ")||line.startsWith("### "))break;
                if(line.startsWith("**Вопрос:**"))continue;
                if(line.startsWith("**"))reading=true;
                if(reading)answer.append(line).append('\n');
            }
            List<LearningChain.Step> chain=LearningChain.build(answer.toString().replace("**","").replace("`","").trim(),new Random(i));
            require(!chain.isEmpty(),ids.get(i)+" no steps");
            for(LearningChain.Step step:chain){
                require(step.options.size()<=4&&step.options.contains(step.correct),"invalid options");
                require(step.prompt.contains("[…]"),"missing blank");
                require(step.prompt.replace("[…]",step.correct).equals(step.explanation),"invalid reconstruction");
                Set<String> unique=new HashSet<>();
                for(String option:step.options)require(unique.add(option.toLowerCase(Locale.ROOT)),"duplicate option");
                steps++;
            }
            count++;
        }
        require(count==1355,"Unexpected question count: "+count);
        System.out.println("Verified "+count+" questions, "+steps+" quiz steps.");
    }
}
