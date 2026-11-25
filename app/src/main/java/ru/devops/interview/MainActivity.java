package ru.devops.interview;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import android.text.Editable;
import android.text.TextWatcher;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    static final int BG=Color.rgb(12,18,29), PANEL=Color.rgb(23,32,47), TEXT=Color.rgb(235,242,249), MUTED=Color.rgb(151,169,186), ACCENT=Color.rgb(91,225,190);
    static class Question { String id, level, type, text, category; }
    final ArrayList<Question> all=new ArrayList<>(), deck=new ArrayList<>();
    final LinkedHashMap<String,Integer> categories=new LinkedHashMap<>();
    final HashMap<String,String> answers=new HashMap<>();
    AnswerAnalyzer analyzer; SharedPreferences prefs; LinearLayout root, body; String category="Все темы", level="Все уровни", query=""; boolean favorites=false; int index=0; String page="home", questionOrigin="home";
    int d(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG); prefs=getSharedPreferences("progress",MODE_PRIVATE); load(); loadAnswers(); analyzer=new AnswerAnalyzer(answers.values()); home();}
    void load(){
        Pattern heading=Pattern.compile("^## (.+) \\((\\d+)\\)$");
        Pattern item=Pattern.compile("^- \\*\\*(Q-\\d+)\\*\\* · ([^·]+) · ([^:]+): (.+)$");
        try(BufferedReader r=new BufferedReader(new InputStreamReader(getAssets().open("questions.md"),StandardCharsets.UTF_8))){String s,c=""; while((s=r.readLine())!=null){Matcher h=heading.matcher(s), m=item.matcher(s); if(h.matches()){c=h.group(1); categories.put(c,0);} else if(m.matches()&&!c.isEmpty()){Question q=new Question(); q.id=m.group(1); q.level=m.group(2).trim(); q.type=m.group(3).trim(); q.text=m.group(4); q.category=c; all.add(q); categories.put(c,categories.get(c)+1);}}}catch(IOException e){Toast.makeText(this,"Не удалось загрузить вопросы",Toast.LENGTH_LONG).show();}
    }
    void loadAnswers(){
        Pattern heading=Pattern.compile("^### (Q-\\d+) · .+$");
        try(BufferedReader r=new BufferedReader(new InputStreamReader(getAssets().open("answers.md"),StandardCharsets.UTF_8))){
            String line,id=null; StringBuilder answer=new StringBuilder(); boolean reading=false;
            while((line=r.readLine())!=null){
                Matcher m=heading.matcher(line);
                if(m.matches()||line.startsWith("## ")){
                    if(id!=null && answer.length()>0) answers.put(id,answer.toString().trim().replace("**","").replace("`",""));
                    id=m.matches()?m.group(1):null; answer.setLength(0); reading=false;
                } else if(id!=null){
                    if(line.startsWith("**Вопрос:**")) continue;
                    if(!reading && line.startsWith("**")) reading=true;
                    if(reading) answer.append(line).append('\n');
                }
            }
            if(id!=null && answer.length()>0) answers.put(id,answer.toString().trim().replace("**","").replace("`",""));
        } catch(IOException e){Toast.makeText(this,"Не удалось загрузить ответы",Toast.LENGTH_LONG).show();}
    }
    GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(d(radius));return g;}
    TextView text(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color); if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    void space(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,d(h)));}
    void init(String title,String subtitle){root=column();root.setPadding(d(20),d(20),d(20),0);root.setBackgroundColor(BG);setContentView(root); TextView t=text(title,28,TEXT,true);root.addView(t);if(subtitle!=null){TextView s=text(subtitle,14,MUTED,false);s.setPadding(0,d(6),0,0);root.addView(s);}space(root,20); ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body=column();scroll.addView(body);}
    TextView button(String label,boolean primary,Runnable action){TextView b=text(label,15,primary?BG:TEXT,true);b.setGravity(Gravity.CENTER);b.setPadding(d(16),d(14),d(16),d(14));b.setBackground(shape(primary?ACCENT:PANEL,14));b.setOnClickListener(v->action.run());return b;}
    void addButton(LinearLayout parent,String label,boolean primary,Runnable action){parent.addView(button(label,primary,action),new LinearLayout.LayoutParams(-1,-2));space(parent,10);}
    int known(){int n=0;for(Question q:all)if(prefs.getBoolean("known_"+q.id,false))n++;return n;}
    int starred(){int n=0;for(Question q:all)if(prefs.getBoolean("fav_"+q.id,false))n++;return n;}
    void home(){page="home";init("DevOps Interview","Тренируйся отвечать уверенно");
        LinearLayout hero=column();hero.setPadding(d(20),d(20),d(20),d(20));hero.setBackground(shape(PANEL,20));hero.addView(text("ТВОЯ ПОДГОТОВКА",12,ACCENT,true));space(hero,12);hero.addView(text(all.size()+" вопросов",26,TEXT,true));space(hero,6);hero.addView(text(categories.size()+" тем  ·  "+known()+" изучено  ·  "+starred()+" в избранном",14,MUTED,false));body.addView(hero);space(body,20);
        addButton(body,"Начать тренировку  →",true,()->{category="Все темы";level="Все уровни";query="";favorites=false;startDeck(true);});
        addButton(body,"Категории и поиск",false,()->{category="Все темы";level="Все уровни";query="";favorites=false;browse();});
        addButton(body,"Избранное  ·  "+starred(),false,()->{category="Все темы";level="Все уровни";query="";favorites=true;browse();});
        space(body,14);body.addView(text("Как это работает",18,TEXT,true));space(body,8);body.addView(text("Напиши ответ или ответь вслух, открой пример и оцени себя. Прогресс и избранное сохраняются на устройстве. Ответы Q-00001—Q-00080 подробные; остальные — краткие опорные материалы, которые могут требовать уточнения.",14,MUTED,false));space(body,24);
    }
    ArrayList<Question> filtered(){ArrayList<Question> list=new ArrayList<>();String term=query.trim().toLowerCase(Locale.ROOT);for(Question q:all)if((category.equals("Все темы")||q.category.equals(category))&&(level.equals("Все уровни")||q.level.equals(level))&&(!favorites||prefs.getBoolean("fav_"+q.id,false))&&(term.isEmpty()||(q.text+" "+q.id+" "+q.category).toLowerCase(Locale.ROOT).contains(term)))list.add(q);return list;}
    void browse(){page="browse";init(favorites?"Избранное":"Банк вопросов","Выбери тему, уровень или найди вопрос");
        addButton(body,"← На главную",false,()->home());
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(TEXT);search.setHintTextColor(MUTED);search.setTextSize(15);search.setPadding(d(14),0,d(14),0);search.setBackground(shape(PANEL,12));search.setHint("Поиск по вопросам");search.setText(query);body.addView(search,new LinearLayout.LayoutParams(-1,d(50)));space(body,12);
        Spinner cat=new Spinner(this);ArrayList<String> cats=new ArrayList<>();cats.add("Все темы");cats.addAll(categories.keySet());setSpinner(cat,cats,Math.max(0,cats.indexOf(category)));body.addView(cat);space(body,10);
        Spinner lev=new Spinner(this);ArrayList<String> levels=new ArrayList<>(Arrays.asList("Все уровни","Junior","Middle","Middle+","Senior"));setSpinner(lev,levels,Math.max(0,levels.indexOf(level)));body.addView(lev);space(body,16);
        TextView count=text("",14,MUTED,false);body.addView(count);space(body,12);LinearLayout results=column();body.addView(results);
        Runnable refresh=()->{ArrayList<Question> list=filtered();count.setText("Найдено: "+list.size());results.removeAllViews();if(list.isEmpty()){results.addView(text("Ничего не найдено. Попробуй изменить фильтры.",15,MUTED,false));return;}addButton(results,"Тренировать выборку  →",true,()->startDeck(true));int max=Math.min(100,list.size());for(int i=0;i<max;i++){Question q=list.get(i);LinearLayout card=column();card.setPadding(d(16),d(14),d(16),d(14));card.setBackground(shape(PANEL,14));card.addView(text(q.id+"  ·  "+q.level+"  ·  "+q.type,12,ACCENT,true));space(card,7);TextView qt=text(q.text,15,TEXT,false);qt.setMaxLines(3);card.addView(qt);space(card,5);card.addView(text(q.category,12,MUTED,false));card.setOnClickListener(v->{questionOrigin="browse";deck.clear();deck.addAll(filtered());index=deck.indexOf(q);showQuestion();});results.addView(card);space(results,8);}if(list.size()>max)results.addView(text("Показаны первые 100. Уточни поиск или начни тренировку всей выборки.",13,MUTED,false));};
        cat.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){category=cats.get(pos);refresh.run();}});
        lev.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){level=levels.get(pos);refresh.run();}});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();refresh.run();}public void afterTextChanged(Editable e){}});refresh.run();
    }
    void setSpinner(Spinner spinner,ArrayList<String> values,int selected){ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,values){@Override public View getView(int p,View c,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,c,parent);t.setTextColor(TEXT);t.setPadding(d(14),d(12),d(14),d(12));return t;}};adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);spinner.setBackground(shape(PANEL,12));spinner.setSelection(selected);}
    void startDeck(boolean shuffle){questionOrigin=page;deck.clear();deck.addAll(filtered());if(deck.isEmpty()){Toast.makeText(this,"Нет вопросов для тренировки",Toast.LENGTH_SHORT).show();return;}if(shuffle)Collections.shuffle(deck);index=0;showQuestion();}
    void leaveQuestion(){if(questionOrigin.equals("browse"))browse();else home();}
    void showQuestion(){page="question";Question q=deck.get(index);init("Тренировка",(index+1)+" / "+deck.size()+"  ·  "+q.category);
        addButton(body,questionOrigin.equals("browse")?"← К вопросам":"← На главную",false,()->leaveQuestion());
        LinearLayout card=column();card.setPadding(d(20),d(22),d(20),d(22));card.setBackground(shape(PANEL,20));card.addView(text(q.id+"  ·  "+q.level+"  ·  "+q.type,12,ACCENT,true));space(card,18);card.addView(text(q.text,21,TEXT,true));space(card,18);card.addView(text("Напиши ответ или ответь вслух. Если не знаешь, открой эталонный ответ.",14,MUTED,false));body.addView(card);space(body,16);
        body.addView(text("ТВОЙ ОТВЕТ",12,ACCENT,true));space(body,8);
        EditText draft=new EditText(this);draft.setTextColor(TEXT);draft.setHintTextColor(MUTED);draft.setTextSize(16);draft.setGravity(Gravity.TOP|Gravity.START);draft.setPadding(d(16),d(14),d(16),d(14));draft.setBackground(shape(PANEL,14));draft.setHint("Напиши свой ответ здесь");draft.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);draft.setMinLines(4);draft.setText(prefs.getString("draft_"+q.id,""));body.addView(draft,new LinearLayout.LayoutParams(-1,-2));space(body,16);
        draft.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){prefs.edit().putString("draft_"+q.id,s.toString()).apply();}public void afterTextChanged(Editable e){}});
        String answer=answers.get(q.id);
        if(answer!=null && !answer.isEmpty()){
            TextView analysis=text("",14,TEXT,false);analysis.setPadding(d(16),d(14),d(16),d(14));analysis.setBackground(shape(PANEL,14));analysis.setVisibility(View.GONE);
            addButton(body,"Сравнить с примером",false,()->{analysis.setText(analyzer.compare(answer,draft.getText().toString()));analysis.setVisibility(View.VISIBLE);});
            body.addView(analysis);space(body,10);
            TextView toggle=button("Не знаю ответ — показать  ↓",true,()->{});
            body.addView(toggle,new LinearLayout.LayoutParams(-1,-2));space(body,10);
            LinearLayout answerCard=column();answerCard.setPadding(d(20),d(18),d(20),d(18));answerCard.setBackground(shape(PANEL,20));
            answerCard.addView(text("ОТВЕТ",12,ACCENT,true));space(answerCard,10);TextView answerText=text(answer,16,TEXT,false);answerText.setTextIsSelectable(true);answerText.setLineSpacing(d(3),1f);answerCard.addView(answerText);answerCard.setVisibility(View.GONE);body.addView(answerCard);space(body,10);
            toggle.setOnClickListener(v->{boolean visible=answerCard.getVisibility()==View.VISIBLE;answerCard.setVisibility(visible?View.GONE:View.VISIBLE);toggle.setText(visible?"Не знаю ответ — показать  ↓":"Скрыть ответ  ↑");if(!visible){((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(draft.getWindowToken(),0);draft.clearFocus();answerCard.post(()->{android.graphics.Rect rect=new android.graphics.Rect(0,0,answerCard.getWidth(),Math.min(answerCard.getHeight(),d(200)));answerCard.requestRectangleOnScreen(rect,false);});}});
        }else{addButton(body,"Не знаю ответ",true,()->Toast.makeText(this,"Для этого вопроса эталонный ответ пока не добавлен.",Toast.LENGTH_LONG).show());body.addView(text("Для этого вопроса ответ не найден.",14,MUTED,false));space(body,10);}
        boolean fav=prefs.getBoolean("fav_"+q.id,false), known=prefs.getBoolean("known_"+q.id,false);
        addButton(body,fav?"★  В избранном":"☆  В избранное",false,()->{prefs.edit().putBoolean("fav_"+q.id,!fav).apply();showQuestion();});
        addButton(body,known?"✓  Изучено — снять отметку":"✓  Отметить изученным",true,()->{prefs.edit().putBoolean("known_"+q.id,!known).apply();showQuestion();});
        LinearLayout nav=new LinearLayout(this);TextView prev=button("← Назад",false,()->{if(index>0){index--;showQuestion();}});TextView next=button(index==deck.size()-1?"Завершить":"Далее →",false,()->{if(index+1<deck.size()){index++;showQuestion();}else leaveQuestion();});nav.addView(prev,new LinearLayout.LayoutParams(0,-2,1));View gap=new View(this);nav.addView(gap,new LinearLayout.LayoutParams(d(10),1));nav.addView(next,new LinearLayout.LayoutParams(0,-2,1));body.addView(nav);space(body,20);
    }
    @Override public void onBackPressed(){if(page.equals("question")){leaveQuestion();}else if(page.equals("browse")){home();}else super.onBackPressed();}
}
