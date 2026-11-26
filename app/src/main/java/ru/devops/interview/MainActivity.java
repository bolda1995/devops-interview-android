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
    AnswerAnalyzer analyzer; SharedPreferences prefs; LinearLayout root, body; String category="Все темы", level="Все уровни", query=""; boolean favorites=false; int bankFilter=0; int index=0; String page="home", questionOrigin="home";
    int d(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG); prefs=getSharedPreferences("progress",MODE_PRIVATE); if(b!=null)speechQuestionId=b.getString("speechQuestionId"); loadCourse(); load(); loadAnswers(); loadKnowledge(); analyzer=new AnswerAnalyzer(answers.values()); home();}

    static final String COURSE_PREFIX="2026 · ";
    boolean course(Question q){return q.category.startsWith(COURSE_PREFIX);}
    boolean inBank(Question q){
        return bankFilter==0||(bankFilter==1&&course(q)&&!q.type.equals("Практика"))||
            (bankFilter==2&&course(q)&&q.type.equals("Практика"))||(bankFilter==3&&!course(q));
    }
    void resetBrowse(int filter){bankFilter=filter;category="Все темы";level="Все уровни";query="";favorites=false;}
    void loadCourse(){
        try(InputStream in=getAssets().open("course2026.json");ByteArrayOutputStream data=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)data.write(buf,0,n);
            org.json.JSONArray records=new org.json.JSONArray(new String(data.toByteArray(),StandardCharsets.UTF_8));
            for(int i=0;i<records.length();i++){
                org.json.JSONObject record=records.getJSONObject(i);
                Question q=new Question();q.id=record.getString("id");q.level=record.getString("level");q.type=record.getString("type");q.category=record.getString("topic");
                String task=record.optString("task","");q.text=record.getString("question")+(task.isEmpty()?"":"\n\n"+task);
                all.add(q);Integer count=categories.get(q.category);categories.put(q.category,count==null?1:count+1);
                answers.put(q.id,record.getString("answer"));
            }
        }catch(Exception e){Toast.makeText(this,"Не удалось загрузить курс DevOps 2026",Toast.LENGTH_LONG).show();}
    }
    void showAnswerGuide(){
        page="answerGuide";init("Как отвечать","Методика курса DevOps Interview 2026");
        addButton(body,"← На главную",false,()->home());
        body.addView(text("Технический вопрос · 4 части",20,ACCENT,true));space(body,10);
        body.addView(text("1. Определение — что это, одним предложением.\n\n2. Механизм — как и почему работает.\n\n3. Пример — реальная ситуация или явно обозначенный гипотетический пример.\n\n4. Ограничения — риски, компромиссы, типичные ошибки.",17,TEXT,false));space(body,20);
        body.addView(text("Практическая задача",20,ACCENT,true));space(body,10);
        body.addView(text("Уточни требования → выдвини гипотезы → назови команды и действия → проверь результат → продумай риски и откат.",17,TEXT,false));space(body,20);
        body.addView(text("Если не знаешь",20,ACCENT,true));space(body,10);
        body.addView(text("Честно обозначь пробел и объясни, как стал бы искать решение. Не выдумывай опыт.",17,TEXT,false));space(body,20);
        body.addView(text("Soft skills · STAR",20,ACCENT,true));space(body,10);
        body.addView(text("Ситуация → задача → действие → результат. Используй свои реальные истории и цифры.",17,TEXT,false));space(body,20);
        body.addView(text("Источник: justxor/devops-interview-2026 (MIT указан в README)",13,MUTED,false));
        addButton(body,"Открыть источник",false,()->{
            try{startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/justxor/devops-interview-2026")));}catch(Exception e){Toast.makeText(this,"Не удалось открыть ссылку",Toast.LENGTH_SHORT).show();}
        });
    }
    String answerTemplate(Question q){
        if(q.type.equals("Практика"))return "Требования и гипотезы:\n\nШаги и команды:\n\nПроверка результата:\n\nРиски и откат:";
        return "Определение:\n\nКак работает:\n\nПример:\n\nОграничения и компромиссы:";
    }

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
        LinearLayout hero=column();hero.setPadding(d(20),d(20),d(20),d(20));hero.setBackground(shape(PANEL,20));hero.addView(text("ТВОЯ ПОДГОТОВКА",12,ACCENT,true));space(hero,12);hero.addView(text("DevOps 2026",26,TEXT,true));space(hero,6);hero.addView(text("150 вопросов · 55 практических задач",16,TEXT,false));space(hero,6);hero.addView(text(categories.size()+" тем  ·  "+known()+" изучено  ·  "+starred()+" в избранном",14,MUTED,false));body.addView(hero);space(body,20);
        addButton(body,"Начать интервью 2026 →",true,()->{resetBrowse(1);startDeck(true);});
        addButton(body,"150 вопросов курса",false,()->{resetBrowse(1);browse();});
        addButton(body,"55 практических задач",false,()->{resetBrowse(2);browse();});
        addButton(body,"Как отвечать на собеседовании",false,()->showAnswerGuide());
        addButton(body,"Тесты",true,()->showKnowledgeTopics());
        addButton(body,"Цепочки обучения",true,()->openLearningTopics(false));
        addButton(body,"Дополнительная база · 1355 вопросов",false,()->{resetBrowse(3);browse();});
        addButton(body,"Избранное  ·  "+starred(),false,()->{resetBrowse(0);favorites=true;browse();});
        addButton(body,"Повторить слабые вопросы  ·  "+weakQuestions().size(),true,()->repeatWeak());
        addButton(body,"История AI-проверок",false,()->showHistory());
        addButton(body,"Настройки AI · OpenRouter",false,()->configureAI(null));
        space(body,14);body.addView(text("Как это работает",18,TEXT,true));space(body,8);body.addView(text("Основной курс: justxor/devops-interview-2026. Ответь по структуре, сравни с опорным ответом и получи AI-оценку. Старые вопросы и прогресс сохранены. Материал курса не заменяет актуальную документацию; практику выполняй только в песочнице.",14,MUTED,false));space(body,24);
    }
    ArrayList<Question> filtered(){ArrayList<Question> list=new ArrayList<>();String term=query.trim().toLowerCase(Locale.ROOT);for(Question q:all)if(inBank(q)&&(category.equals("Все темы")||q.category.equals(category))&&(level.equals("Все уровни")||q.level.equals(level))&&(!favorites||prefs.getBoolean("fav_"+q.id,false))&&(term.isEmpty()||(q.text+" "+q.id+" "+q.category).toLowerCase(Locale.ROOT).contains(term)))list.add(q);return list;}
    void browse(){page="browse";init(favorites?"Избранное":bankFilter==1?"150 вопросов курса":bankFilter==2?"Практикум 2026":"Дополнительная база","Выбери тему, уровень или найди вопрос");
        addButton(body,"← На главную",false,()->home());
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(TEXT);search.setHintTextColor(MUTED);search.setTextSize(15);search.setPadding(d(14),0,d(14),0);search.setBackground(shape(PANEL,12));search.setHint("Поиск по вопросам");search.setText(query);body.addView(search,new LinearLayout.LayoutParams(-1,d(50)));space(body,12);
        Spinner cat=new Spinner(this);ArrayList<String> cats=new ArrayList<>();cats.add("Все темы");for(String topic:categories.keySet()){for(Question q:all)if(inBank(q)&&q.category.equals(topic)){cats.add(topic);break;}}setSpinner(cat,cats,Math.max(0,cats.indexOf(category)));body.addView(cat);space(body,10);
        Spinner lev=new Spinner(this);ArrayList<String> levels=new ArrayList<>(Arrays.asList("Все уровни","Junior","Middle","Middle+","Senior","Практика"));setSpinner(lev,levels,Math.max(0,levels.indexOf(level)));body.addView(lev);space(body,16);
        TextView count=text("",14,MUTED,false);body.addView(count);space(body,12);LinearLayout results=column();body.addView(results);
        Runnable refresh=()->{ArrayList<Question> list=filtered();count.setText("Найдено: "+list.size());results.removeAllViews();if(list.isEmpty()){results.addView(text("Ничего не найдено. Попробуй изменить фильтры.",15,MUTED,false));return;}addButton(results,"Тренировать выборку  →",true,()->startDeck(true));int max=Math.min(100,list.size());for(int i=0;i<max;i++){Question q=list.get(i);LinearLayout card=column();card.setPadding(d(16),d(14),d(16),d(14));card.setBackground(shape(PANEL,14));card.addView(text(q.id+"  ·  "+q.level+"  ·  "+q.type,12,ACCENT,true));space(card,7);TextView qt=text(q.text,15,TEXT,false);qt.setMaxLines(3);card.addView(qt);space(card,5);card.addView(text(q.category,12,MUTED,false));card.setOnClickListener(v->{questionOrigin="browse";deck.clear();deck.addAll(filtered());index=deck.indexOf(q);showQuestion();});results.addView(card);space(results,8);}if(list.size()>max)results.addView(text("Показаны первые 100. Уточни поиск или начни тренировку всей выборки.",13,MUTED,false));};
        cat.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){category=cats.get(pos);refresh.run();}});
        lev.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){level=levels.get(pos);refresh.run();}});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();refresh.run();}public void afterTextChanged(Editable e){}});refresh.run();
    }
    void setSpinner(Spinner spinner,ArrayList<String> values,int selected){ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,values){@Override public View getView(int p,View c,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,c,parent);t.setTextColor(TEXT);t.setPadding(d(14),d(12),d(14),d(12));return t;}};adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);spinner.setBackground(shape(PANEL,12));spinner.setSelection(selected);}
    void startDeck(boolean shuffle){questionOrigin=page;deck.clear();deck.addAll(filtered());if(deck.isEmpty()){Toast.makeText(this,"Нет вопросов для тренировки",Toast.LENGTH_SHORT).show();return;}if(shuffle)Collections.shuffle(deck);index=0;showQuestion();}
    void leaveQuestion(){if(questionOrigin.equals("browse"))browse();else if(questionOrigin.equals("history"))showHistory();else if(questionOrigin.equals("chainList")){showChainList();}else home();}


    private static final int SPEECH_REQUEST=742;
    private String speechQuestionId;
    private EditText activeDraft;
    private final Set<String> pendingChecks=new HashSet<>();
    void voiceInput(Question q){
        android.content.Intent intent=new android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE,"ru-RU");
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT,"Продиктуй ответ на вопрос");
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS,1);
        speechQuestionId=q.id;
        try{startActivityForResult(intent,SPEECH_REQUEST);}catch(android.content.ActivityNotFoundException e){speechQuestionId=null;Toast.makeText(this,"На устройстве нет сервиса распознавания речи. Используй клавиатуру.",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int request,int result,android.content.Intent data){
        super.onActivityResult(request,result,data);
        if(request!=SPEECH_REQUEST)return;
        String id=speechQuestionId;speechQuestionId=null;
        if(result!=RESULT_OK||data==null||id==null)return;
        ArrayList<String> words=data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
        if(words==null||words.isEmpty()||words.get(0).trim().isEmpty())return;
        String existing=prefs.getString("draft_"+id,"");
        String combined=existing.trim().isEmpty()?words.get(0):existing+" "+words.get(0);
        prefs.edit().putString("draft_"+id,combined).apply();
        if(page.equals("question")&&!deck.isEmpty()&&deck.get(index).id.equals(id)&&activeDraft!=null){activeDraft.setText(combined);activeDraft.setSelection(combined.length());}
        else Toast.makeText(this,"Голосовой ответ сохранён в черновик",Toast.LENGTH_SHORT).show();
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("speechQuestionId",speechQuestionId);}
    ArrayList<Question> weakQuestions(){
        ArrayList<Question> list=new ArrayList<>();
        for(Question q:all)if(PracticeHistory.isWeak(prefs,q.id))list.add(q);
        Collections.sort(list,(a,b)->Integer.compare(PracticeHistory.priority(prefs,a.id),PracticeHistory.priority(prefs,b.id)));
        return list;
    }
    void repeatWeak(){
        ArrayList<Question> list=weakQuestions();
        if(list.isEmpty()){Toast.makeText(this,"Пока нет слабых вопросов. Пройди AI-проверку или открой ответ через «Не знаю».",Toast.LENGTH_LONG).show();return;}
        questionOrigin="home";deck.clear();deck.addAll(list);index=0;showQuestion();
    }
    void showHistory(){
        page="history";init("История AI-проверок","Последние 200 успешных проверок");
        addButton(body,"← На главную",false,()->home());
        ArrayList<PracticeHistory.Entry> entries=PracticeHistory.entries(prefs);
        if(entries.isEmpty()){body.addView(text("Пока нет оценок. Ответь на вопрос и запусти AI-проверку.",15,MUTED,false));return;}
        int scored=0,total=0;for(PracticeHistory.Entry e:entries)if(e.score>=0){scored++;total+=e.score;}
        body.addView(text("Проверок: "+entries.size()+"  ·  Средний балл: "+(scored==0?"нет оценки":String.format(Locale.ROOT,"%.1f/10",(double)total/scored)),15,ACCENT,true));space(body,12);
        for(PracticeHistory.Entry e:entries){
            LinearLayout item=column();item.setPadding(d(16),d(14),d(16),d(14));item.setBackground(shape(PANEL,14));
            item.addView(text((e.score<0?"Без балла":e.score+"/10")+"  ·  "+new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(new Date(e.time)),14,ACCENT,true));space(item,8);
            item.addView(text(e.question,15,TEXT,true));space(item,6);
            addButton(item,"Подробнее",false,()->new android.app.AlertDialog.Builder(this).setTitle(e.id+" · "+(e.score<0?"Без балла":e.score+"/10")).setMessage(e.question+"\n\nТвой ответ:\n"+e.answer+"\n\n"+e.feedback).setPositiveButton("Закрыть",null).show());
            addButton(item,"Повторить вопрос",false,()->{for(Question q:all)if(q.id.equals(e.id)){questionOrigin="history";deck.clear();deck.add(q);index=0;showQuestion();return;}});
            body.addView(item);space(body,10);
        }
        addButton(body,"Очистить историю и список слабых вопросов",false,()->new android.app.AlertDialog.Builder(this).setTitle("Очистить историю?").setMessage("Оценки и отметки «Не знаю» будут удалены. Черновики и избранное останутся.").setNegativeButton("Отмена",null).setPositiveButton("Очистить",(dialog,which)->{PracticeHistory.clear(prefs);showHistory();}).show());
    }
    void configureAI(Runnable after){
        EditText key=new EditText(this);key.setSingleLine(true);key.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setHint("sk-or-v1-...");key.setText(OpenRouterClient.loadKey(this));
        new android.app.AlertDialog.Builder(this).setTitle("OpenRouter — API-ключ")
            .setMessage("Создай ключ на openrouter.ai/keys. Используется только openrouter/free. При проверке вопрос, ответ и пример отправляются в OpenRouter и выбранному провайдеру. Не вводи конфиденциальные данные.")
            .setView(key).setNegativeButton("Отмена",null)
            .setNeutralButton("Удалить ключ",(dialog,which)->{OpenRouterClient.clearKey(this);Toast.makeText(this,"Ключ удалён",Toast.LENGTH_SHORT).show();})
            .setPositiveButton("Сохранить",(dialog,which)->{String value=key.getText().toString().trim();if(value.isEmpty()){Toast.makeText(this,"Введи API-ключ",Toast.LENGTH_SHORT).show();return;}try{OpenRouterClient.saveKey(this,value);Toast.makeText(this,"Ключ сохранён",Toast.LENGTH_SHORT).show();if(after!=null)after.run();}catch(Exception e){Toast.makeText(this,"Не удалось безопасно сохранить ключ",Toast.LENGTH_LONG).show();}}).show();
    }
    void checkAI(Question q,EditText draft,String reference,TextView result,TextView trigger){
        String response=draft.getText().toString().trim();
        if(response.isEmpty()||response.equals(answerTemplate(q))){Toast.makeText(this,"Сначала напиши ответ",Toast.LENGTH_SHORT).show();return;}
        String key=OpenRouterClient.loadKey(this);
        if(key.isEmpty()){configureAI(()->checkAI(q,draft,reference,result,trigger));return;}
        if(!pendingChecks.add(q.id)){Toast.makeText(this,"Этот вопрос уже проверяется",Toast.LENGTH_SHORT).show();return;}
        trigger.setEnabled(false);trigger.setText("Проверяем через LLM…");result.setVisibility(View.VISIBLE);result.setText("Запрос отправлен. Ожидаем оценку.");
        new Thread(()->{String message;try{message=OpenRouterClient.evaluate(key,q.text,q.level,response,reference);PracticeHistory.record(prefs,q.id,q.text,response,message);}catch(Exception e){message="Не удалось выполнить AI-проверку: "+e.getMessage()+"\nМожно воспользоваться локальным сравнением.";}
            final String output=message;runOnUiThread(()->{pendingChecks.remove(q.id);if(isFinishing()||isDestroyed())return;trigger.setEnabled(true);trigger.setText("Проверить ответ через AI");result.setText(output);result.post(()->result.requestRectangleOnScreen(new android.graphics.Rect(0,0,result.getWidth(),Math.min(result.getHeight(),d(200))),false));});
        }).start();
    }
    void showQuestion(){page="question";Question q=deck.get(index);init("Тренировка",(index+1)+" / "+deck.size()+"  ·  "+q.category);
        addButton(body,questionOrigin.equals("browse")?"← К вопросам":questionOrigin.equals("history")?"← К истории":"← На главную",false,()->leaveQuestion());
        LinearLayout card=column();card.setPadding(d(20),d(22),d(20),d(22));card.setBackground(shape(PANEL,20));card.addView(text(q.id+"  ·  "+q.level+"  ·  "+q.type,12,ACCENT,true));space(card,18);card.addView(text(q.text,21,TEXT,true));space(card,18);card.addView(text("Напиши ответ или ответь вслух. Если не знаешь, открой эталонный ответ.",14,MUTED,false));body.addView(card);space(body,16);
        addButton(body,"Тесты по этой теме",true,()->startKnowledge(q.category,true,false));
        addButton(body,"Выучить ответ по цепочке",false,()->{startChain(q,true);});
        body.addView(text("ТВОЙ ОТВЕТ",12,ACCENT,true));space(body,8);
        body.addView(text(q.type.equals("Практика")?"Уточни требования → шаги и команды → проверка → риски и откат. Команды выполняй только в тестовой среде.":"Определение → механизм → пример → ограничения. Если опыта нет, обозначь гипотетический пример.",14,MUTED,false));space(body,10);
        EditText draft=new EditText(this);draft.setTextColor(TEXT);draft.setHintTextColor(MUTED);draft.setTextSize(16);draft.setGravity(Gravity.TOP|Gravity.START);draft.setPadding(d(16),d(14),d(16),d(14));draft.setBackground(shape(PANEL,14));draft.setHint("Напиши свой ответ здесь");draft.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);draft.setMinLines(4);draft.setText(prefs.getString("draft_"+q.id,""));body.addView(draft,new LinearLayout.LayoutParams(-1,-2));space(body,16);
        draft.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){prefs.edit().putString("draft_"+q.id,s.toString()).apply();}public void afterTextChanged(Editable e){}});
        activeDraft=draft;
        addButton(body,"Вставить структуру ответа",false,()->{if(draft.getText().toString().trim().isEmpty()){String template=answerTemplate(q);draft.setText(template);draft.setSelection(template.length());}else Toast.makeText(this,"Черновик уже содержит текст — структура не заменит его",Toast.LENGTH_SHORT).show();});
        addButton(body,"🎙 Ответить голосом",false,()->voiceInput(q));
        String answer=answers.get(q.id);
        TextView aiResult=text("",15,TEXT,false);aiResult.setPadding(d(16),d(14),d(16),d(14));aiResult.setBackground(shape(PANEL,14));aiResult.setTextIsSelectable(true);aiResult.setVisibility(View.GONE);
        TextView aiButton=button("Проверить ответ через AI",true,()->{});aiButton.setOnClickListener(v->checkAI(q,draft,answer,aiResult,aiButton));body.addView(aiButton,new LinearLayout.LayoutParams(-1,-2));space(body,10);body.addView(aiResult);space(body,10);
        if(answer!=null && !answer.isEmpty()){
            TextView analysis=text("",14,TEXT,false);analysis.setPadding(d(16),d(14),d(16),d(14));analysis.setBackground(shape(PANEL,14));analysis.setVisibility(View.GONE);
            addButton(body,"Сравнить с примером",false,()->{analysis.setText(analyzer.compare(answer,draft.getText().toString()));analysis.setVisibility(View.VISIBLE);});
            body.addView(analysis);space(body,10);
            TextView toggle=button("Не знаю ответ — показать  ↓",true,()->{});
            body.addView(toggle,new LinearLayout.LayoutParams(-1,-2));space(body,10);
            LinearLayout answerCard=column();answerCard.setPadding(d(20),d(18),d(20),d(18));answerCard.setBackground(shape(PANEL,20));
            answerCard.addView(text(course(q)?"ОПОРНЫЙ ОТВЕТ · DEVOPS 2026":"ОТВЕТ",12,ACCENT,true));space(answerCard,10);TextView answerText=text(answer,16,TEXT,false);answerText.setTextIsSelectable(true);answerText.setLineSpacing(d(3),1f);answerCard.addView(answerText);answerCard.setVisibility(View.GONE);body.addView(answerCard);space(body,10);
            toggle.setOnClickListener(v->{boolean visible=answerCard.getVisibility()==View.VISIBLE;answerCard.setVisibility(visible?View.GONE:View.VISIBLE);toggle.setText(visible?"Не знаю ответ — показать  ↓":"Скрыть ответ  ↑");if(!visible){PracticeHistory.markUnknown(prefs,q.id);((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(draft.getWindowToken(),0);draft.clearFocus();answerCard.post(()->{android.graphics.Rect rect=new android.graphics.Rect(0,0,answerCard.getWidth(),Math.min(answerCard.getHeight(),d(200)));answerCard.requestRectangleOnScreen(rect,false);});}});
        }else{addButton(body,"Не знаю ответ",true,()->{PracticeHistory.markUnknown(prefs,q.id);Toast.makeText(this,"Для этого вопроса эталонный ответ пока не добавлен.",Toast.LENGTH_LONG).show();});body.addView(text("Для этого вопроса ответ не найден.",14,MUTED,false));space(body,10);}
        boolean fav=prefs.getBoolean("fav_"+q.id,false), known=prefs.getBoolean("known_"+q.id,false);
        addButton(body,fav?"★  В избранном":"☆  В избранное",false,()->{prefs.edit().putBoolean("fav_"+q.id,!fav).apply();showQuestion();});
        addButton(body,known?"✓  Изучено — снять отметку":"✓  Отметить изученным",true,()->{prefs.edit().putBoolean("known_"+q.id,!known).apply();showQuestion();});
        LinearLayout nav=new LinearLayout(this);TextView prev=button("← Назад",false,()->{if(index>0){index--;showQuestion();}});TextView next=button(index==deck.size()-1?"Завершить":"Далее →",false,()->{if(index+1<deck.size()){index++;showQuestion();}else leaveQuestion();});nav.addView(prev,new LinearLayout.LayoutParams(0,-2,1));View gap=new View(this);nav.addView(gap,new LinearLayout.LayoutParams(d(10),1));nav.addView(next,new LinearLayout.LayoutParams(0,-2,1));body.addView(nav);space(body,20);
    }

    Question chainQuestion;
    List<LearningChain.Step> chainSteps=Collections.emptyList();
    int chainIndex,chainCorrect;
    boolean chainFromQuestion;
    static final int CORRECT=Color.rgb(26,112,76), WRONG=Color.rgb(151,49,55);
    String learningKey(){return "chain_";}
    void openLearningTopics(boolean tests){showChainTopics();}
    String chainCategory="Все темы";
    void showChainTopics(){
        page="chainTopics";init("Цепочки обучения","Запоминай ответ небольшими фрагментами");
        addButton(body,"← На главную",false,()->home());
        addButton(body,"Все темы",true,()->{chainCategory="Все темы";showChainList();});
        for(String topic:categories.keySet())addButton(body,topic,false,()->{chainCategory=topic;showChainList();});
    }
    void showChainList(){
        page="chainList";init("Цепочки — выбери вопрос",chainCategory);
        addButton(body,"← К темам",false,()->showChainTopics());
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(TEXT);search.setHintTextColor(MUTED);search.setHint("Поиск вопроса");body.addView(search);space(body,12);
        LinearLayout list=column();body.addView(list);
        Runnable refresh=()->{
            list.removeAllViews();int count=0;String term=search.getText().toString().trim().toLowerCase(Locale.ROOT);
            for(Question q:all){
                if(!chainCategory.equals("Все темы")&&!q.category.equals(chainCategory))continue;
                if(!(q.id+" "+q.text).toLowerCase(Locale.ROOT).contains(term))continue;
                if(!answers.containsKey(q.id))continue;
                String best=prefs.getString(learningKey()+"best_"+q.id,"");
                String last=prefs.getString(learningKey()+"last_"+q.id,"");
                if(count++<100)addButton(list,(last.isEmpty()?"":"Последний: "+last+" · ")+(best.isEmpty()?"":"Лучший: "+best+" · ")+q.text,false,()->startChain(q,false));
            }
            if(count==0)list.addView(text("Нет вопросов с готовым ответом.",15,MUTED,false));
            if(count>100)list.addView(text("Показаны первые 100. Используй поиск.",13,MUTED,false));
        };
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){refresh.run();}public void afterTextChanged(Editable e){}});refresh.run();
    }
    void startChain(Question q,boolean fromQuestion){
        List<LearningChain.Step> steps=LearningChain.build(answers.get(q.id),new Random());
        if(steps.isEmpty()){Toast.makeText(this,"Нет готового материала для цепочки",Toast.LENGTH_LONG).show();return;}
        chainQuestion=q;chainSteps=steps;chainIndex=0;chainCorrect=0;chainFromQuestion=fromQuestion;
        showChainStudy();
    }
    void leaveChain(){if(chainFromQuestion)showQuestion();else showChainList();}
    void chainHeader(String title){
        page="chain";init(title,"Шаг "+(chainIndex+1)+" из "+chainSteps.size()+" · "+chainQuestion.category+" · Верно: "+chainCorrect);
        addButton(body,"← Выйти из цепочки",false,()->leaveChain());
        body.addView(text(chainQuestion.text,18,TEXT,true));space(body,15);
    }
    void showChainStudy(){
        chainHeader("Запомни фрагмент");
        body.addView(text(chainSteps.get(chainIndex).explanation,18,TEXT,false));space(body,20);
        addButton(body,"Запомнил — пройти тест",true,()->showChainQuiz());
    }
    interface QuizSubmit { void accept(String selected); }
    void showChainQuiz(){
        chainHeader("Мини-тест");
        LearningChain.Step step=chainSteps.get(chainIndex);
        body.addView(text("Восстанови пропущенное слово в изученном фрагменте.",14,MUTED,false));space(body,10);
        body.addView(text(step.prompt,18,TEXT,true));space(body,16);
        LinearLayout choices=column();body.addView(choices);
        ArrayList<TextView> optionViews=new ArrayList<>();
        LinearLayout feedback=column();
        final boolean[] answered={false};
        QuizSubmit submit=selected->{
            if(answered[0])return;
            answered[0]=true;
            boolean correct=selected!=null&&selected.equalsIgnoreCase(step.correct);
            if(correct)chainCorrect++;
            for(int i=0;i<optionViews.size();i++){
                TextView view=optionViews.get(i);String option=step.options.get(i);
                view.setEnabled(false);
                if(option.equalsIgnoreCase(step.correct)){
                    view.setText("✓ "+option+" — правильно");view.setBackground(shape(CORRECT,14));
                }else if(selected!=null&&option.equalsIgnoreCase(selected)){
                    view.setText("✗ "+option+" — неправильно");view.setBackground(shape(WRONG,14));
                }
            }
            for(int i=0;i<choices.getChildCount();i++)choices.getChildAt(i).setEnabled(false);
            TextView verdict=text(correct?"✓ Правильно":selected==null?"✗ Не знаю":"✗ Неправильно",19,TEXT,true);
            verdict.setPadding(d(14),d(12),d(14),d(12));verdict.setBackground(shape(correct?CORRECT:WRONG,12));
            feedback.addView(verdict);space(feedback,10);
            if(selected!=null&&!correct)feedback.addView(text("Твой ответ: "+selected,15,TEXT,false));
            feedback.addView(text("Правильный ответ: "+step.correct,17,ACCENT,true));space(feedback,10);
            feedback.addView(text(step.explanation,16,TEXT,false));space(feedback,15);
            addButton(feedback,chainIndex+1==chainSteps.size()?"Посмотреть результат":"Следующий фрагмент →",true,()->{
                chainIndex++;if(chainIndex==chainSteps.size())showChainResult();else showChainStudy();
            });
            feedback.post(()->feedback.requestRectangleOnScreen(new android.graphics.Rect(0,0,feedback.getWidth(),Math.min(feedback.getHeight(),d(200))),false));
        };
        if(step.options.size()>=2){
            for(String option:step.options){
                TextView view=button(option,false,()->submit.accept(option));optionViews.add(view);
                choices.addView(view,new LinearLayout.LayoutParams(-1,-2));space(choices,10);
            }
        }else{
            EditText input=new EditText(this);input.setSingleLine(true);input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setHint("Пропущенное слово");choices.addView(input);space(choices,10);
            addButton(choices,"Проверить",true,()->submit.accept(input.getText().toString().trim()));
        }
        addButton(choices,"Не знаю",false,()->submit.accept(null));
        body.addView(feedback);space(body,20);
    }
    void showChainResult(){
        page="chain";int total=chainSteps.size();int percent=100*chainCorrect/total;
        String id=chainQuestion.id;int previous=prefs.getInt(learningKey()+"percent_"+id,-1);
        SharedPreferences.Editor progress=prefs.edit().putString(learningKey()+"last_"+id,chainCorrect+"/"+total);
        if(percent>=previous)progress.putInt(learningKey()+"percent_"+id,percent).putString(learningKey()+"best_"+id,chainCorrect+"/"+total);
        progress.apply();
        init("Цепочка завершена",chainQuestion.text);
        body.addView(text(chainCorrect+" / "+total+" правильных · "+percent+"%",25,ACCENT,true));space(body,12);
        body.addView(text("✓ Правильно: "+chainCorrect+"   ·   ✗ Неправильно: "+(total-chainCorrect),16,TEXT,true));space(body,12);
        body.addView(text("Теперь попробуй воспроизвести весь ответ своими словами. Мини-тесты проверяют запоминание текста, а не глубину знаний.",16,TEXT,false));space(body,20);
        addButton(body,"Ответить на полный вопрос",true,()->{
            if(!chainFromQuestion){questionOrigin="chainList";deck.clear();deck.add(chainQuestion);index=0;}
            showQuestion();
        });
        addButton(body,"Повторить цепочку",false,()->startChain(chainQuestion,chainFromQuestion));
        addButton(body,"← К вопросам",false,()->leaveChain());
    }


    List<KnowledgeQuiz.Item> knowledgeBank=Collections.emptyList(), knowledgeSession=Collections.emptyList();
    final ArrayList<KnowledgeQuiz.Item> knowledgeMistakes=new ArrayList<>();
    int knowledgeIndex,knowledgeCorrect;
    String knowledgeTopic="Все темы";
    boolean knowledgeFromQuestion;
    void loadKnowledge(){
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(getAssets().open("knowledge_quiz.txt"),StandardCharsets.UTF_8))){
            knowledgeBank=KnowledgeQuiz.read(reader);
        }catch(IOException e){Toast.makeText(this,"Не удалось загрузить тесты",Toast.LENGTH_LONG).show();}
    }

    boolean knowledgeMatches(String topic,String itemTopic){
        if(topic.equals("Все темы")||topic.equals(itemTopic))return true;
        if(!topic.startsWith(COURSE_PREFIX))return false;
        String name=topic.substring(COURSE_PREFIX.length());
        switch(name){
            case "Linux":return itemTopic.startsWith("Linux:");
            case "Сети":return itemTopic.startsWith("Сети:");
            case "Docker":return itemTopic.startsWith("Docker");
            case "Kubernetes":return itemTopic.startsWith("Kubernetes:");
            case "CI/CD":return itemTopic.startsWith("CI/CD")||itemTopic.startsWith("GitLab CI")||itemTopic.startsWith("Helm");
            case "IaC":return itemTopic.startsWith("Terraform")||itemTopic.startsWith("Ansible");
            case "Облака":return itemTopic.startsWith("Облака")||itemTopic.startsWith("Storage");
            case "Observability":return itemTopic.startsWith("Observability:");
            case "Security":return itemTopic.startsWith("Security");
            case "SRE":return itemTopic.startsWith("SRE:");
            case "Инциденты":return itemTopic.startsWith("SRE:")||itemTopic.startsWith("IT Operations");
            case "Bash и Python":return itemTopic.startsWith("Автоматизация");
            case "System Design":return itemTopic.startsWith("Архитектура");
            default:return false;
        }
    }

    void showKnowledgeTopics(){
        page="knowledgeTopics";init("Тесты знаний","Вопросы по теории и практические ситуации · "+knowledgeBank.size()+" заданий");
        addButton(body,"← На главную",false,()->home());
        addButton(body,"Смешанный тест · 10 вопросов",true,()->startKnowledge("Все темы",false,false));
        for(String topic:categories.keySet()){
            int count=0;for(KnowledgeQuiz.Item item:knowledgeBank)if(knowledgeMatches(topic,item.topic))count++;
            if(count==0)continue;
            String last=prefs.getString("knowledge_last_"+topic,"");
            addButton(body,topic+" · "+count+(last.isEmpty()?"":" · Последний: "+last),false,()->startKnowledge(topic,false,false));
        }
    }
    void startKnowledge(String topic,boolean fromQuestion,boolean mistakesOnly){
        ArrayList<KnowledgeQuiz.Item> pool=new ArrayList<>();
        if(mistakesOnly)pool.addAll(knowledgeMistakes);
        else for(KnowledgeQuiz.Item item:knowledgeBank)if(knowledgeMatches(topic,item.topic))pool.add(item);
        if(pool.isEmpty()){Toast.makeText(this,"Нет вопросов в этой выборке",Toast.LENGTH_SHORT).show();return;}
        Collections.shuffle(pool);
        if(!mistakesOnly&&topic.equals("Все темы")&&pool.size()>10)pool=new ArrayList<>(pool.subList(0,10));
        knowledgeSession=pool;knowledgeTopic=topic;knowledgeFromQuestion=fromQuestion;knowledgeIndex=0;knowledgeCorrect=0;knowledgeMistakes.clear();
        knowledgeRetry=mistakesOnly;
        showKnowledgeQuestion();
    }
    boolean knowledgeRetry;
    void leaveKnowledge(){if(knowledgeFromQuestion)showQuestion();else showKnowledgeTopics();}
    void showKnowledgeQuestion(){
        page="knowledge";KnowledgeQuiz.Item item=knowledgeSession.get(knowledgeIndex);
        init("Тест знаний",(knowledgeIndex+1)+" / "+knowledgeSession.size()+" · "+item.topic);
        addButton(body,"← Выйти из теста",false,()->leaveKnowledge());
        body.addView(text(item.question,21,TEXT,true));space(body,16);
        body.addView(text("Выбери один правильный ответ",14,MUTED,false));space(body,12);
        ArrayList<String> options=new ArrayList<>(item.options);Collections.shuffle(options);
        ArrayList<TextView> views=new ArrayList<>();
        LinearLayout choices=column(),feedback=column();body.addView(choices);body.addView(feedback);
        final boolean[] answered={false};
        QuizSubmit submit=selected->{
            if(answered[0])return;answered[0]=true;
            boolean correct=item.correct.equals(selected);
            if(correct)knowledgeCorrect++;else knowledgeMistakes.add(item);
            for(int i=0;i<views.size();i++){
                TextView view=views.get(i);String option=options.get(i);
                if(item.correct.equals(option)){view.setText("✓ "+option);view.setBackground(shape(CORRECT,14));}
                else if(option.equals(selected)){view.setText("✗ "+option);view.setBackground(shape(WRONG,14));}
            }
            for(int i=0;i<choices.getChildCount();i++)choices.getChildAt(i).setEnabled(false);
            TextView verdict=text(correct?"✓ Правильно":"✗ Неправильно",19,TEXT,true);
            verdict.setPadding(d(14),d(12),d(14),d(12));verdict.setBackground(shape(correct?CORRECT:WRONG,12));feedback.addView(verdict);space(feedback,10);
            feedback.addView(text("Правильный ответ: "+item.correct,17,ACCENT,true));space(feedback,10);
            feedback.addView(text(item.explanation,16,TEXT,false));space(feedback,15);
            addButton(feedback,knowledgeIndex+1==knowledgeSession.size()?"Результат":"Следующий вопрос →",true,()->{
                knowledgeIndex++;if(knowledgeIndex==knowledgeSession.size())showKnowledgeResult();else showKnowledgeQuestion();
            });
            feedback.post(()->feedback.requestRectangleOnScreen(new android.graphics.Rect(0,0,feedback.getWidth(),Math.min(feedback.getHeight(),d(200))),false));
        };
        for(String option:options){TextView view=button(option,false,()->submit.accept(option));views.add(view);choices.addView(view,new LinearLayout.LayoutParams(-1,-2));space(choices,10);}
        addButton(choices,"Не знаю",false,()->submit.accept(null));
        space(body,20);
    }
    void showKnowledgeResult(){
        page="knowledge";int total=knowledgeSession.size(),percent=100*knowledgeCorrect/total;
        if(!knowledgeRetry){
            String prefix="knowledge_";
            SharedPreferences.Editor edit=prefs.edit().putString(prefix+"last_"+knowledgeTopic,knowledgeCorrect+"/"+total);
            if(percent>=prefs.getInt(prefix+"best_"+knowledgeTopic,-1))edit.putInt(prefix+"best_"+knowledgeTopic,percent);
            edit.apply();
        }
        init("Результат теста",knowledgeTopic);
        body.addView(text(knowledgeCorrect+" из "+total+" · "+percent+"%",27,ACCENT,true));space(body,10);
        body.addView(text("✓ Правильно: "+knowledgeCorrect+"   ·   ✗ Ошибок: "+(total-knowledgeCorrect),17,TEXT,true));space(body,16);
        if(!knowledgeMistakes.isEmpty()){
            addButton(body,"Повторить ошибки · "+knowledgeMistakes.size(),true,()->startKnowledge(knowledgeTopic,knowledgeFromQuestion,true));
            for(KnowledgeQuiz.Item item:knowledgeMistakes){body.addView(text(item.question,16,TEXT,true));body.addView(text("✓ "+item.correct,15,ACCENT,false));space(body,10);}
        }
        addButton(body,"Новый тест",false,()->startKnowledge(knowledgeTopic,knowledgeFromQuestion,false));
        addButton(body,"← Назад",false,()->leaveKnowledge());
    }

    @Override public void onBackPressed(){if(page.equals("knowledge")){leaveKnowledge();}else if(page.equals("knowledgeTopics")||page.equals("answerGuide")){home();}else if(page.equals("chain")){leaveChain();}else if(page.equals("chainList")){showChainTopics();}else if(page.equals("chainTopics")){home();}else if(page.equals("question")){leaveQuestion();}else if(page.equals("browse")||page.equals("history")){home();}else super.onBackPressed();}
}
