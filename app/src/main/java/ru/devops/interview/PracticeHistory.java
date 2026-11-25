package ru.devops.interview;

import android.content.SharedPreferences;
import java.util.*;
import java.util.regex.*;
import org.json.*;

final class PracticeHistory {
    private static final String KEY="ai_history_v1";
    private static final Pattern SCORE=Pattern.compile("(?im)^\\s*[*#_]*\\s*(?:Оценка|Балл)\\s*[:* _-]*\\s*(10|[0-9])\\s*(?:/\\s*10|из\\s*10)\\b");
    static class Entry {String id,question,answer,feedback;int score;long time;}
    static int parseScore(String feedback){Matcher m=SCORE.matcher(feedback);return m.find()?Integer.parseInt(m.group(1)):-1;}
    static synchronized void markUnknown(SharedPreferences p,String id){p.edit().putBoolean("weak_"+id,true).apply();}
    static synchronized boolean isWeak(SharedPreferences p,String id){return p.getBoolean("weak_"+id,false);}
    static int priority(SharedPreferences p,String id){return p.getInt("ai_score_"+id,0);}
    private static JSONArray raw(SharedPreferences p){try{return new JSONArray(p.getString(KEY,"[]"));}catch(JSONException e){return new JSONArray();}}
    static synchronized void record(SharedPreferences p,String id,String question,String answer,String feedback){
        int score=parseScore(feedback);
        try{
            JSONArray old=raw(p),next=new JSONArray();
            next.put(new JSONObject().put("id",id).put("question",question).put("answer",answer).put("feedback",feedback).put("score",score).put("time",System.currentTimeMillis()));
            for(int i=0;i<Math.min(199,old.length());i++)next.put(old.getJSONObject(i));
            SharedPreferences.Editor edit=p.edit().putString(KEY,next.toString());
            if(score>=0)edit.putInt("ai_score_"+id,score).putBoolean("weak_"+id,score<7);
            edit.apply();
        }catch(JSONException ignored){}
    }
    static synchronized ArrayList<Entry> entries(SharedPreferences p){
        JSONArray a=raw(p);ArrayList<Entry> list=new ArrayList<>();
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;Entry e=new Entry();e.id=o.optString("id");e.question=o.optString("question");e.answer=o.optString("answer");e.feedback=o.optString("feedback");e.score=o.optInt("score",-1);e.time=o.optLong("time");list.add(e);}
        return list;
    }
    static synchronized void clear(SharedPreferences p){SharedPreferences.Editor edit=p.edit().remove(KEY);for(String k:p.getAll().keySet())if(k.startsWith("weak_")||k.startsWith("ai_score_"))edit.remove(k);edit.apply();}
}
