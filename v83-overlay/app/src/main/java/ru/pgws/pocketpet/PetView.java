package ru.pgws.pocketpet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/** Edge-to-edge pixel LCD. Interactive scenes are played by touches and swipes, not virtual A/B/C keys. */
public final class PetView extends View {
    private static final int LCD=Color.rgb(183,211,161),PAPER=Color.rgb(217,231,184),INK=Color.rgb(40,69,51);
    private static final int MID=Color.rgb(104,143,100),OFF=Color.rgb(149,179,139),NIGHT=Color.rgb(28,43,42);
    private static final String[] MENU={"ЕДА","ИГРЫ","СВЕТ","ЛЕЧИТЬ","УЧИТЬ","СЕТЬ"};
    private static final String[] RU_ROWS={"ЙЦУКЕНГШЩЗХ","ФЫВАПРОЛДЖЭ","ЯЧСМИТЬБЮЁЪ"};
    private static final String[] EN_ROWS={"QWERTYUIOP","ASDFGHJKL","ZXCVBNM"};
    private final PetGame game;
    private final MainActivity host;
    private final PixelFont font;
    private final Paint paint=new Paint();
    private final SimpleDateFormat clock=new SimpleDateFormat("HH:mm",new Locale("ru","RU"));
    private final Random random=new Random();
    private MiniGames mini;
    // 0 home, 1 food, 2 game catalog, 3 minigame, 4 name keyboard, 5 treatment, 6 training, 7 admin, 8 network
    private int screen=0,feedDrag=-1,medicineDrops=0;
    private Lessons lessons;
    private float scale,dx,dy,downX,downY,lastX,lastY,pointerX,pointerY,stroke;
    private boolean petStroke,patAwarded,dragging,keyboardEnglish,miniReward;
    private String typed="";
    private int keyPage=0;
    private long lastResetConfirm;
    private int geneCache=-1,stageCache=-1,personalityCache=-1,moodCache=-1,frameCache=-1;
    private String[] spriteCache;
    public PetView(Context context,PetGame g,MainActivity host){
        super(context);this.game=g;this.host=host;this.font=new PixelFont(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        setContentDescription("Пиксельный питомец. Погладь его пальцем, нажимай на грязь, перетаскивай предметы и играй.");
    }
    private void c(int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);paint.setStrokeWidth(1);paint.setAntiAlias(false);paint.setFilterBitmap(false);}
    private void rect(Canvas cn,float x,float y,float w,float h,int color){c(color);cn.drawRect(x,y,x+w,y+h,paint);}
    private void border(Canvas cn,float x,float y,float w,float h,int color,int weight){rect(cn,x,y,w,weight,color);rect(cn,x,y+h-weight,w,weight,color);rect(cn,x,y,weight,h,color);rect(cn,x+w-weight,y,weight,h,color);}
    private void t(Canvas cn,String s,float x,float top,float height,int color,boolean center){font.draw(cn,s,x,top,height,color,center);}
    private void panel(Canvas cn,float x,float y,float w,float h){rect(cn,x,y,w,h,PAPER);border(cn,x,y,w,h,INK,2);}
    private void btn(Canvas cn,float x,float y,float w,float h,String s,boolean active){rect(cn,x,y,w,h,active?INK:PAPER);border(cn,x,y,w,h,INK,2);t(cn,s,x+w/2,y+(h-17)/2,17,active?PAPER:INK,true);}
    private void circle(Canvas cn,float x,float y,float r,int color){c(color);cn.drawCircle(x,y,r,paint);}
    @Override protected void onDraw(Canvas raw){
        super.onDraw(raw);int width=getWidth(),height=getHeight();if(width<1||height<1)return;
        scale=Math.min(width/400f,height/800f);dx=(width-400*scale)/2f;dy=(height-800*scale)/2f;
        raw.drawColor(LCD);raw.save();raw.translate(dx,dy);raw.scale(scale,scale);
        drawBackdrop(raw);drawHeader(raw);
        if(screen==0){drawHud(raw);drawHome(raw);drawToolbar(raw);}
        else{
            drawSceneTitle(raw);
            switch(screen){
                case 1:drawFood(raw);break;
                case 2:drawCatalog(raw);break;
                case 3:drawMini(raw);break;
                case 4:drawKeyboard(raw);break;
                case 5:drawMedicine(raw);break;
                case 6:drawTraining(raw);break;
                case 7:drawAdmin(raw);break;
                case 8:drawNetwork(raw);break;
            }
            drawBack(raw);
        }
        raw.restore();
    }
    private void drawBackdrop(Canvas cn){
        rect(cn,0,0,400,800,LCD);
        // Flat, readable LCD rather than a decorative texture or simulated glass.
    }
    private void drawHeader(Canvas cn){
        rect(cn,0,0,400,64,INK);
        t(cn,"КАРМАННЫЙ ПИТОМЕЦ",14,12,17,PAPER,false);
        t(cn,clock.format(new Date(System.currentTimeMillis())),335,14,16,PAPER,true);
        String n=game.name==null||game.name.isEmpty()?"БЕЗ ИМЕНИ":game.name.toUpperCase(Locale.ROOT);
        if(screen==0)t(cn,n.length()>16?n.substring(0,16):n,200,44,12,PAPER,true);
        else t(cn,"НАЖМИ СТРЕЛКУ НАЗАД",200,44,12,PAPER,true);
    }
    private void drawHud(Canvas cn){
        // Always-on mini HUD; intentionally no separate status screen.
        rect(cn,11,76,378,102,PAPER);border(cn,11,76,378,102,INK,3);
        drawMeter(cn,24,90,"ЕДА",game.hunger,4);
        drawMeter(cn,209,90,"РАДОСТЬ",game.happy,4);
        drawMeter(cn,24,130,"ЗДОРОВЬЕ",Math.max(0,4-Math.min(4,game.sickness)),4);
        drawMeter(cn,209,130,"ЧИСТОТА",Math.max(0,4-game.dirty),4);
    }
    private void drawMeter(Canvas cn,int x,int y,String label,int value,int max){
        t(cn,label,x,y,13,INK,false);
        for(int i=0;i<max;i++){
            rect(cn,x+i*21,y+20,17,9,i<value?INK:OFF);
            if(i>=value)border(cn,x+i*21,y+20,17,9,MID,1);
        }
        t(cn,value+"/"+max,x+135,y+16,12,INK,false);
    }
    private void pixelSprite(Canvas cn,String[] rows,int left,int top,int size,boolean sleeping){
        if(rows==null)return;
        for(int y=0;y<rows.length;y++)for(int x=0;x<rows[y].length();x++){
            char ch=rows[y].charAt(x);if(ch=='.')continue;
            int color=ch=='#'?INK:ch=='o'?MID:PAPER;
            rect(cn,left+x*size,top+y*size,size,size,color);
        }
        if(sleeping){t(cn,"z Z",left+rows[0].length()*size-8,top+35,24,INK,false);}
    }
    private String[] creature(){
        int mood=game.mood(GameClock.now(getContext()));
        int frame=(int)((System.currentTimeMillis()/340)%16);
        if(spriteCache==null||geneCache!=game.genes||stageCache!=game.stage||personalityCache!=game.personality||moodCache!=mood||frameCache!=frame){
            geneCache=game.genes;stageCache=game.stage;personalityCache=game.personality;
            moodCache=mood;frameCache=frame;
            spriteCache=PetSprites.create(game.genes,game.stage,game.personality,mood,frame);
        }
        return spriteCache;
    }
    private void drawEgg(Canvas cn){
        for(int y=0;y<19;y++)for(int x=0;x<18;x++){
            double q=Math.pow((x-9)/7.7,2)+Math.pow((y-10.5)/9.4,2);
            if(q>1)continue;
            rect(cn,110+x*10,280+y*10,10,10,q>.76?INK:PAPER);
        }
        t(cn,"скоро появлюсь!",200,523,20,INK,true);
        int min=(int)Math.max(0,(game.hatch-GameClock.now(getContext())+59_999)/60_000);
        t(cn,"ЕЩЁ "+min+" МИН.",200,556,16,INK,true);
    }
    private void drawHome(Canvas cn){
        long now=GameClock.now(getContext());
        panel(cn,14,187,372,457);
        if(!game.alive){
            t(cn,"НА ПАМЯТЬ О ДРУГЕ",200,211,23,INK,true);
            String dear=game.name==null||game.name.isEmpty()?"ПИТОМЕЦ":game.name;
            t(cn,dear,200,258,22,INK,true);
            int stage=game.portraitStage();
            pixelSprite(cn,PetSprites.create(game.genes,stage,game.personality),128,300,6,false);
            t(cn,"ПРОЖИЛ: "+(game.finalAgeHours/24)+" ДН. "+(game.finalAgeHours%24)+" Ч.",200,459,16,INK,true);
            btn(cn,52,495,296,51,"СОХРАНИТЬ ФОТО",false);
            btn(cn,52,563,296,51,"НОВОЕ ЯЙЦО",false);
            return;
        }
        if(game.stage==PetGame.EGG){drawEgg(cn);return;}
        t(cn,"№"+String.format(Locale.US,"%04d",game.genes)+"  ·  "+game.ageDays(now)+" ДН.",200,202,17,INK,true);
        t(cn,game.lifeStage(),200,230,16,MID,true);
        rect(cn,37,543,326,3,MID);rect(cn,52,551,294,2,OFF);
        int mood=game.mood(now);
        long anim=System.currentTimeMillis();
        int bob=mood==PetSprites.SICK?(int)((anim/170)%2==0?3:-3)
                :mood==PetSprites.HUNGRY?0:(int)((anim/340)%3==0?5:0);
        int growth=game.stage==PetGame.BABY?5:game.stage==PetGame.CHILD?6:game.stage==PetGame.TEEN?7:8;
        int cx=200-12*growth,cy=412-12*growth;
        // Distinct life-stage silhouettes with blink, breathing and discomfort reactions.
        pixelSprite(cn,creature(),cx,cy+bob,growth,game.sleeping(now));
        drawEmotionEffects(cn,mood,anim,cx,cy,growth);
        for(int i=0;i<game.dirty;i++){
            float x=pooX(i),y=pooY(i);
            drawDirt(cn,x,y);
        }
        if(game.needsHelp(now)){t(cn,"!",61,313,28,INK,true);}
        if(!game.lights){
            // LCD visibly switches to moonlit darkness, while HUD and toolbar stay usable.
            rect(cn,17,190,366,450,Color.argb(205,15,39,34));
            for(int k=0;k<18;k++){
                float sx=42+(k*97)%318,sy=245+(k*61)%290;
                rect(cn,sx,sy,3,3,Color.rgb(147,186,142));
            }
            circle(cn,316,267,20,PAPER);circle(cn,327,257,19,NIGHT);
            t(cn,"НОЧНОЙ РЕЖИМ",200,584,19,PAPER,true);
            t(cn,"СВЕТ ПОГАШЕН",200,609,13,PAPER,true);
        }else{
            String emotion=game.noticeUntil>now?game.notice:game.emotion(now);
            t(cn,emotion,200,566,emotion.length()>29?12:15,INK,true);
            t(cn,game.dirty>0?"НАЖМИ НА ГРЯЗЬ, ЧТОБЫ УБРАТЬ":
                game.sickness>0?"НАЖМИ ЛЕЧИТЬ И ДАЙ ТРИ ДОЗЫ":
                game.hunger<=1?"НАЖМИ ЕДА И ПОКОРМИ МЕНЯ":"ГЛАДЬ, КОРМИ И ИГРАЙ СО МНОЙ",200,604,12,MID,true);
        }
    }
    /** State-dependent, frame-animated LCD effects, not just an exclamation mark. */
    private void drawEmotionEffects(Canvas cn,int mood,long animation,int x,int y,int pixel){
        float phase=(animation%1400)/1400f;
        if(mood==PetSprites.SICK){
            t(cn,"+",x+24*pixel+9,y+5,28,INK,true);
            t(cn,"~",x-18,y+48+(int)(phase*19),24,INK,true);
            circle(cn,x+19*pixel,y+6*pixel+phase*14,3+phase*3,MID);
            t(cn,"ОХ...",200,269,17,INK,true);
        }else if(mood==PetSprites.HUNGRY){
            // Hunger pulls the sprite downward and the stomach visibly rumbles.
            for(int i=0;i<3;i++)rect(cn,x+7*pixel+i*12,y+19*pixel+(int)(phase*9),7,3,MID);
            t(cn,(animation/700)%2==0?"УРРР...":"ХОЧУ КУШАТЬ",200,272,17,INK,true);
        }else if(mood==PetSprites.SAD){
            circle(cn,x+17*pixel,y+13*pixel+phase*30,4,MID);
            t(cn,"СЫГРАЕМ?",200,270,16,INK,true);
        }else if(mood==PetSprites.SLEEPY){
            t(cn,"z",x+24*pixel-9,y-8-(int)(phase*14),25,INK,true);
        }else if(mood==PetSprites.NORMAL&&!game.sleeping(GameClock.now(getContext()))){
            if((animation/1500)%3==0){drawPixelHeart(cn,x+24*pixel-1,y+32-(int)(phase*12),2,MID);}
        }
    }
    private float pooX(int i){return new float[]{46,304,175,274}[i%4];}
    private float pooY(int i){return new float[]{508,510,540,551}[i%4];}
    private void drawDirt(Canvas cn,float x,float y){
        rect(cn,x+4,y+7,32,20,INK);rect(cn,x+10,y,20,13,INK);rect(cn,x+15,y-9,10,15,INK);
        rect(cn,x+10,y+13,5,5,PAPER);rect(cn,x+24,y+13,5,5,PAPER);
    }
    private void drawToolbar(Canvas cn){
        for(int i=0;i<6;i++){
            int col=i%3,row=i/3;float x=12+col*128,y=660+row*62;
            btn(cn,x,y,120,54,MENU[i],i==2&&!game.lights);
        }
        t(cn,"ПРИКОСНИСЬ К ПИТОМЦУ",200,789,12,INK,true);
    }
    private void drawSceneTitle(Canvas cn){
        String label=screen==1?"КОРМЛЕНИЕ":screen==2?"МИНИ-ИГРЫ":screen==3?mini==null?"ИГРА":mini.title():
            screen==4?"ИМЯ ПИТОМЦА":screen==5?"ЛЕЧЕНИЕ":screen==6?"УЧЁБА":"ОТЛАДКА";
        t(cn,label,200,103,label.length()>18?18:23,INK,true);
        rect(cn,15,142,370,3,MID);
    }
    private void drawBack(Canvas cn){
        btn(cn,20,695,166,61,"< НАЗАД",false);
        if(screen==3 && mini!=null)btn(cn,214,695,166,61,mini.complete?"ЕЩЁ РАЗ":"ЗАНОВО",false);
        else if(screen==4)btn(cn,214,695,166,61,"ГОТОВО >",false);
        
    }
    private void drawFood(Canvas cn){
        t(cn,"ПЕРЕТАЩИ ЕДУ К ПИТОМЦУ",200,177,15,INK,true);
        panel(cn,28,224,156,151);panel(cn,217,224,156,151);
        // plate and dessert
        rect(cn,74,280,58,9,INK);rect(cn,84,269,38,11,INK);rect(cn,95,254,17,15,INK);
        rect(cn,261,263,43,39,INK);rect(cn,268,252,29,12,MID);rect(cn,278,249,9,9,INK);
        t(cn,"ОБЕД",106,335,18,INK,true);t(cn,"ДЕСЕРТ",294,335,18,INK,true);
        pixelSprite(cn,creature(),139,415,5,false);
        border(cn,116,405,168,161,MID,2);
        t(cn,"ОТПУСТИ ЕДУ НА ПИТОМЦА",200,593,14,INK,true);
        if(dragging&&feedDrag>=0){circle(cn,pointerX,pointerY,16,INK);circle(cn,pointerX,pointerY,8,PAPER);}
    }
    private void drawCatalog(Canvas cn){
        t(cn,"ВЫБЕРИ ИГРУ",200,164,16,INK,true);
        for(int i=0;i<10;i++){
            int col=i%2,row=i/2;int x=21+col*190,y=193+row*94;
            panel(cn,x,y,174,83);
            t(cn,String.format(Locale.US,"%02d",i+1),x+14,y+12,22,INK,false);
            String label=MiniGames.NAMES[i];
            float height=label.length()>15?10:label.length()>12?12:15;
            t(cn,label,x+89,y+50,height,INK,true);
        }
    }
    private void drawMedicine(Canvas cn){
        panel(cn,22,172,356,485);
        if(!game.canCare()){
            t(cn,"СНАЧАЛА ВЫЛУПИТСЯ!",200,345,20,INK,true);
            btn(cn,44,553,312,76,"ВЕРНУТЬСЯ",false);
            return;
        }
        if(game.sickness<=0){
            t(cn,"УРА! ПИТОМЕЦ ЗДОРОВ!",200,236,20,INK,true);
            pixelSprite(cn,PetSprites.create(game.genes,game.stage,game.personality,PetSprites.NORMAL,0),104,290,8,false);
            t(cn,"ЛЕКАРСТВО НЕ НУЖНО",200,533,16,MID,true);
            btn(cn,44,565,312,70,"ВЕРНУТЬСЯ К ПИТОМЦУ",false);
            return;
        }
        t(cn,"ПОМОГИ ПИТОМЦУ ВЫЗДОРОВЕТЬ",200,188,17,INK,true);
        t(cn,"КАСАЙСЯ ФЛАКОНА ИЛИ КНОПКИ",200,216,12,MID,true);
        // Animated medicine bottle: visible as soon as treatment is opened.
        rect(cn,63,332,88,112,INK);
        rect(cn,83,311,48,26,INK);
        rect(cn,72,358,70,60,PAPER);
        t(cn,"+",107,368,34,INK,true);
        t(cn,"МИКСТУРА",107,460,13,INK,true);
        int frame=(int)((System.currentTimeMillis()/330)%16);
        pixelSprite(cn,PetSprites.create(game.genes,game.stage,game.personality,PetSprites.SICK,frame),189,300,7,false);
        for(int i=0;i<3;i++){
            int left=101+i*70;
            rect(cn,left,498,51,24,i<medicineDrops?INK:OFF);
            border(cn,left,498,51,24,INK,2);
        }
        t(cn,"ДОЗА "+medicineDrops+" / 3",200,536,18,INK,true);
        btn(cn,36,566,328,73,"ДАТЬ ЛЕКАРСТВО",true);
        if(medicineDrops>0)t(cn,"СТАНОВИТСЯ ЛЕГЧЕ!",200,646,11,INK,true);
    }
    private void applyMedicine(long now){
        if(!game.canCare())return;
        if(game.sickness<=0){host.feedback("Питомец уже здоров");return;}
        medicineDrops++;
        if(medicineDrops>=3){
            game.medicine(now);medicineDrops=0;screen=0;
            host.feedback("Питомец выздоровел!");
        }else{
            game.say("ЛЕКАРСТВО: "+medicineDrops+" / 3",now);
        }
        host.persist();
    }
    /** Four learn-by-playing mechanics instead of repeatedly tapping a highlighted symbol. */
    private void drawTraining(Canvas cn){
        if(lessons==null){lessons=new Lessons(System.currentTimeMillis());}
        long now=System.currentTimeMillis();
        panel(cn,20,161,360,499);
        if(lessons.finished){
            t(cn,lessons.won?"УРОК ПРОЙДЕН":"УРОК ОКОНЧЕН",200,215,22,INK,true);
            t(cn,"РЕШЕНО: "+lessons.solved+" / 5",200,284,19,INK,true);
            t(cn,"ОШИБКИ: "+lessons.errors+" / 3",200,324,17,INK,true);
            drawStudyIcon(cn,lessons.won?3:0,200,403,4);
            t(cn,lessons.won?"ТВОЙ ПИТОМЕЦ НАУЧИЛСЯ!":"МОЖНО ПОПРОБОВАТЬ СНОВА",200,477,14,INK,true);
            btn(cn,47,538,306,77,"К ПИТОМЦУ",false);
            return;
        }
        t(cn,lessons.instruction(now),200,178,21,INK,true);
        t(cn,"ВОПРОС "+(lessons.solved+1)+"/5     ОШИБКИ "+lessons.errors+"/3",200,217,16,INK,true);
        for(int j=0;j<5;j++)rect(cn,79+j*50,250,40,7,j<lessons.solved?INK:OFF);
        if(lessons.kind==Lessons.SEQUENCE){
            for(int i=0;i<3;i++)drawStudyIcon(cn,lessons.clues[i],119+i*80,334,2);
            t(cn,"?",344,314,23,INK,true);
        }else if(lessons.kind==Lessons.MEMORY){
            if(lessons.showing(now)){
                for(int i=0;i<3;i++)drawStudyIcon(cn,lessons.clues[i],119+i*80,334,2);
                t(cn,"ЗАПОМНИ",200,379,15,INK,true);
            }else{
                for(int i=0;i<3;i++)border(cn,97+i*79,296,48,64,MID,2);
                t(cn,"ВЫБЕРИ ПОСЛЕДНИЙ",200,376,15,INK,true);
            }
        }else if(lessons.kind==Lessons.COUNT){
            for(int i=0;i<lessons.clues[0];i++){
                int count=lessons.clues[0];int xx=200-(count-1)*24+i*48;
                drawStudyIcon(cn,lessons.clues[1],xx,333,2);
            }
        }else{
            for(int i=0;i<4;i++)drawStudyIcon(cn,lessons.choices[i],92+i*73,333,2);
        }
        for(int i=0;i<4;i++){
            int x=27+(i%2)*183,y=411+(i/2)*111;
            rect(cn,x,y,163,97,PAPER);border(cn,x,y,163,97,INK,2);
            if(lessons.kind==Lessons.COUNT){
                t(cn,""+lessons.choices[i],x+82,y+27,32,INK,true);
            }else{
                drawStudyIcon(cn,lessons.choices[i],x+82,y+47,3);
            }
        }
        if(lessons.showing(now)){
            rect(cn,27,411,345,208,LCD);
            border(cn,27,411,345,208,MID,2);
            t(cn,"СМОТРИ И ЗАПОМИНАЙ",200,496,18,INK,true);
        }
        t(cn,"5 ЗАДАНИЙ  /  3 ПОПЫТКИ",200,636,13,MID,true);
    }
    private void drawPixelHeart(Canvas cn,float x,float y,int size,int color){
        int[] rows={0b0110110,0b1111111,0b1111111,0b0111110,0b0011100,0b0001000};
        for(int yy=0;yy<rows.length;yy++)for(int xx=0;xx<7;xx++){
            if((rows[yy]&(1<<(6-xx)))!=0)rect(cn,x+size*(xx-3),y+size*(yy-3),size,size,color);
        }
    }
    /** LCD pixel objects: no Unicode icons, emoji, antialiasing or scalable stock symbols. */
    private void drawStudyIcon(Canvas cn,int type,float cx,float cy,int size){
        int[][][] icons={
           { // apple
             {0,0,0,1,0,0,0},{0,0,1,1,1,0,0},{0,1,1,1,1,1,0},{1,1,1,1,1,1,1},
             {1,1,1,1,1,1,1},{1,1,1,1,1,1,1},{0,1,1,1,1,1,0},{0,0,1,0,1,0,0}},
           { // house
             {0,0,0,1,0,0,0},{0,0,1,1,1,0,0},{0,1,1,1,1,1,0},{1,1,1,1,1,1,1},
             {0,1,0,0,0,1,0},{0,1,0,1,0,1,0},{0,1,0,1,0,1,0},{0,1,1,1,1,1,0}},
           { // fish
             {0,0,0,0,0,0,0},{0,0,1,1,1,0,1},{0,1,1,1,1,1,1},{1,1,1,1,1,1,1},
             {0,1,1,1,1,1,1},{0,0,1,1,1,0,1},{0,0,0,0,0,0,0},{0,0,0,0,0,0,0}},
           { // flower
             {0,0,1,0,1,0,0},{0,1,1,1,1,1,0},{0,1,1,0,1,1,0},{1,1,0,1,0,1,1},
             {0,1,1,0,1,1,0},{0,1,1,1,1,1,0},{0,0,1,0,1,0,0},{0,0,0,1,0,0,0}}
        };
        int[][] pixel=icons[((type%4+4)%4)];
        for(int row=0;row<8;row++)for(int col=0;col<7;col++){
            if(pixel[row][col]!=0)rect(cn,cx+(col-3)*size,cy+(row-4)*size,size,size,INK);
        }
    }
    private void drawKeyboard(Canvas cn){
        t(cn,"ВВЕДИ ИМЯ В ИГРОВОМ СТИЛЕ",200,177,16,INK,true);
        panel(cn,29,205,342,64);
        String cursor=(System.currentTimeMillis()/550)%2==0?"_":" ";
        String preview=(typed+cursor).toUpperCase(Locale.ROOT);
        t(cn,preview,200,225,24,INK,true);
        String[] rows=keyboardEnglish?EN_ROWS:RU_ROWS;
        for(int row=0;row<3;row++){
            String line=rows[row];int len=line.length();float spacing=344f/len;
            for(int col=0;col<len;col++){
                float x=28+col*spacing,y=300+row*66;
                btn(cn,x,y,spacing-2,55,""+line.charAt(col),false);
            }
        }
        String digit="1234567890";
        for(int i=0;i<10;i++)btn(cn,28+i*34.5f,503,32,44,""+digit.charAt(i),false);
        btn(cn,28,558,103,48,keyboardEnglish?"РУС":"EN",false);
        btn(cn,142,558,106,48,"ПРОБЕЛ",false);
        btn(cn,259,558,112,48,"СТЕРЕТЬ",false);
        t(cn,"ДО 12 СИМВОЛОВ",200,617,14,MID,true);
        
    }
    private void drawNetwork(Canvas cn){
        t(cn,"СЕРВЕР И ОБНОВЛЕНИЯ",200,155,22,INK,true);
        String user=NetworkSettings.nickname(getContext());
        if(user.isEmpty())user="НЕ УКАЗАН";
        btn(cn,26,202,348,79,"НИК: "+user,false);
        String url=NetworkSettings.endpoint(getContext());
        btn(cn,26,303,348,79,url.isEmpty()?"АДРЕС СЕРВЕРА":"ИЗМЕНИТЬ СЕРВЕР",false);
        t(cn,url.isEmpty()?"СЕРВЕР НЕ НАСТРОЕН":url,200,389,12,INK,true);
        btn(cn,26,404,348,79,NetworkSettings.consent(getContext())?"СТАТИСТИКА: ВКЛ":"СТАТИСТИКА: ВЫКЛ",false);
        btn(cn,26,505,348,79,"ПРОВЕРИТЬ ОБНОВЛЕНИЯ",false);
        t(cn,"СОГЛАСИЕ МОЖНО ОТОЗВАТЬ",200,607,14,INK,true);
        t(cn,"IP И МОДЕЛЬ ТОЛЬКО С СОГЛАСИЯ",200,636,13,MID,true);
    }
    private void drawAdmin(Canvas cn){
        t(cn,"ОТЛАДКА // ДОСТУП РАЗРЕШЁН",200,162,14,INK,true);
        int[] speeds={1,5,20,60,240};
        for(int i=0;i<5;i++)btn(cn,11+i*78,200,72,47,"x"+speeds[i],GameClock.speed(getContext())==speeds[i]);
        t(cn,"СКОРОСТЬ СИМУЛЯЦИИ",200,259,14,MID,true);
        String[] tasks={"+ 1 ЧАС","+ 1 ДЕНЬ","ГОЛОД","ГРЯЗЬ","БОЛЕЗНЬ","ИСЦЕЛИТЬ", "ВЗРОСЛЕТЬ","НОВОЕ ЯЙЦО","ТЕСТ УВЕД.","ИД ПИТОМЦА"};
        for(int i=0;i<tasks.length;i++){
            int col=i%2,row=i/2;btn(cn,16+col*190,285+row*67,178,57,tasks[i],false);
        }
        t(cn,"ГЕНЫ "+game.genes+"  ТИКИ "+game.ticks+"  ОШ. "+game.missed,200,637,14,INK,true);
        if(lastResetConfirm>GameClock.now(getContext()))t(cn,"ПОВТОРИ НАЖАТИЕ",200,664,16,INK,true);
    }
    private void drawMini(Canvas cn){
        if(mini==null){screen=2;return;}
        long now=System.currentTimeMillis();mini.tick(now);
        if(mini.complete&&!miniReward){miniReward=true;game.rewardGame(mini.won,GameClock.now(getContext()));host.persist();}
        t(cn,mini.message(now),200,162,15,INK,true);
        t(cn,"СЧЁТ "+mini.score+"/"+mini.goal()+"   ОШИБКИ "+mini.errors+"/3",200,195,14,INK,true);
        if(mini.complete){
            panel(cn,38,300,324,280);t(cn,mini.won?"ПОБЕДА!":"НЕ ПОЛУЧИЛОСЬ",200,379,25,INK,true);
            t(cn,mini.won?"РАДОСТЬ ПИТОМЦА +1":"ПОПРОБУЙ ДРУГОЙ ПУТЬ",200,439,14,INK,true);
            t(cn,"ЕЩЁ РАЗ — ВНИЗУ",200,505,14,MID,true);return;
        }
        switch(mini.type){
            case 0:drawBerries(cn,now);break;
            case 1:drawPairs(cn);break;
            case 2:drawMaze(cn);break;
            case 3:drawPixelCopy(cn);break;
            case 4:drawBeat(cn,now);break;
            case 5:drawNumbers(cn);break;
            case 6:drawTic(cn);break;
            case 7:drawRunner(cn,now);break;
            case 8:drawDiff(cn);break;
            case 9:drawSort(cn);break;
        }
    }
    private void drawBerries(Canvas cn,long now){
        float progress=Math.max(0,Math.min(1,1f-(mini.deadline-now)/(float)mini.period));
        for(int col=0;col<3;col++)border(cn,18+col*126,248,116,359,OFF,2);
        float bx=80+mini.target*126,by=266+progress*263;
        circle(cn,bx,by,20,INK);rect(cn,bx-3,by-27,6,11,MID);circle(cn,bx-6,by-6,4,PAPER);
        float cx=80+mini.cursor*126;
        rect(cn,cx-49,565,98,10,INK);rect(cn,cx-38,546,76,19,INK);
        t(cn,"ВОДИ ПАЛЬЦЕМ ПО ПОЛОСКАМ",200,621,13,MID,true);
    }
    private void drawPairs(Canvas cn){
        long now=System.currentTimeMillis();
        t(cn,"4 ПАРЫ / БЕЗ ОГРАНИЧЕНИЯ ОШИБОК",200,276,14,INK,true);
        for(int i=0;i<8;i++){
            int x=38+(i%4)*80,y=330+(i/4)*94;
            boolean open=now<mini.previewUntil||mini.selection==i||mini.second==i||mini.cells[i]==0;
            rect(cn,x,y,73,85,open?PAPER:INK);border(cn,x,y,73,85,INK,2);
            if(mini.cells[i]==0)t(cn,"+",x+37,y+26,23,INK,true);
            else if(open)drawStudyIcon(cn,mini.cells[i]-1,x+37,y+45,5);
            else t(cn,"?",x+37,y+28,27,PAPER,true);
        }
        t(cn,now<mini.previewUntil?"СМОТРИ 2.5 СЕКУНДЫ":"ОТКРОЙ ДВЕ ОДИНАКОВЫЕ",200,560,15,INK,true);
    }
    private void drawMaze(Canvas cn){
        for(int y=0;y<7;y++)for(int x=0;x<7;x++){
            int xx=64+x*39,yy=285+y*39;
            rect(cn,xx,yy,38,38,mini.cells[y*7+x]!=0?INK:PAPER);
            if(x==5&&y==5)drawPixelHeart(cn,xx+19,yy+19,2,MID);
            if(x==mini.playerX&&y==mini.playerY)circle(cn,xx+19,yy+19,12,MID);
        }
        t(cn,"<   ВЛЕВО       ВНИЗ       ВПРАВО   >",200,580,12,INK,true);
        t(cn,"ИЛИ ПРОВОДИ ПАЛЬЦЕМ",200,620,13,MID,true);
    }
    private void drawPixelCopy(Canvas cn){
        t(cn,"ОБРАЗЕЦ",124,260,15,INK,true);t(cn,"ТВОЙ РИСУНОК",277,260,15,INK,true);
        for(int i=0;i<25;i++){
            int col=i%5,row=i/5;
            rect(cn,54+col*25,350+row*30,24,29,mini.cells[i]==1?INK:PAPER);
            border(cn,54+col*25,350+row*30,24,29,MID,1);
            rect(cn,204+col*30,350+row*30,28,28,mini.aux[i]==1?INK:PAPER);
            border(cn,204+col*30,350+row*30,28,28,MID,1);
        }
        t(cn,"КОСНИСЬ КВАДРАТОВ СПРАВА",200,558,14,INK,true);
    }
    private void drawBeat(Canvas cn,long now){
        int left=(int)Math.max(0,mini.nextAt-now);
        circle(cn,200,415,110,INK);circle(cn,200,415,92,PAPER);
        boolean flash=Math.abs(mini.nextAt-now)<250;
        circle(cn,200,415,67,flash?INK:MID);
        t(cn,flash?"БЕЙ!":"ЖДИ",200,397,29,flash?PAPER:INK,true);
        t(cn,"ЧЕРЕЗ "+String.format(Locale.US,"%.1f",left/1000f)+" СЕК.",200,552,18,INK,true);
    }
    private void drawNumbers(Canvas cn){
        for(int i=0;i<16;i++){
            int x=39+(i%4)*80,y=286+(i/4)*74;
            rect(cn,x,y,74,67,mini.cells[i]==0?OFF:PAPER);border(cn,x,y,74,67,INK,2);
            if(mini.cells[i]!=0)t(cn,""+mini.cells[i],x+37,y+21,28,INK,true);
        }
    }
    private void drawTic(Canvas cn){
        for(int i=0;i<9;i++){
            int x=65+(i%3)*90,y=299+(i/3)*90;
            border(cn,x,y,90,90,INK,2);
            String str=mini.cells[i]==0?"":mini.cells[i]==1?"X":"O";
            t(cn,str,x+45,y+27,39,INK,true);
        }
        t(cn,"НИЧЬЯ = НОВЫЙ РАУНД",200,601,14,INK,true);
    }
    private void drawRunner(Canvas cn,long now){
        rect(cn,22,537,358,7,INK);
        float t=Math.max(0,Math.min(1,(mini.deadline-now)/(float)mini.period));
        float x=110+270*t;
        rect(cn,x,489,29,48,INK);
        float jump=mini.jumped?35:0;
        rect(cn,94,492-jump,42,40,MID);rect(cn,102,482-jump,25,18,INK);
        t(cn,"ЖМИ В НУЖНЫЙ МОМЕНТ",200,599,16,INK,true);
    }
    private void drawDiff(Canvas cn){
        t(cn,"ОБРАЗЕЦ",100,286,15,INK,true);t(cn,"НАЙДИ ТОЧКУ",289,286,15,INK,true);
        for(int i=0;i<25;i++){
            int x=i%5,y=i/5;
            int l=mini.cells[i];int r=(i==mini.difference?1-l:l);
            rect(cn,41+x*27,350+y*27,26,26,l==1?INK:PAPER);
            rect(cn,222+x*27,350+y*27,26,26,r==1?INK:PAPER);
        }
        t(cn,"НАЖМИ ОТЛИЧАЮЩУЮСЯ КЛЕТКУ",200,549,13,INK,true);
    }
    private void drawSymbol(Canvas cn,int symbol,float x,float y){
        if(symbol==0){circle(cn,x,y,25,INK);circle(cn,x,y,11,PAPER);}
        else if(symbol==1){rect(cn,x-24,y-24,48,48,INK);rect(cn,x-12,y-12,24,24,PAPER);}
        else { // diamond
            for(int a=0;a<5;a++)rect(cn,x-6-a*5,y-6-a*5,12+a*10,12+a*10,INK);
            circle(cn,x,y,10,PAPER);
        }
    }
    private void drawSort(Canvas cn){
        panel(cn,115,296,170,149);
        drawSymbol(cn,mini.sourceType,200,357);
        t(cn,"ПЕРЕТАЩИ",200,409,15,INK,true);
        for(int i=0;i<3;i++){
            int x=16+i*127;
            border(cn,x,482,116,117,INK,3);
            drawSymbol(cn,mini.bins[i],x+58,534);
        }
        if(mini.dragging)drawSymbol(cn,mini.sourceType,mini.dragX,468);
    }
    public void showName(){typed=game.name==null?"":game.name.equals("Питомец")?"":game.name;keyboardEnglish=false;screen=4;invalidate();}
    public boolean naming(){return screen==4;}
    public void back(){if(screen==3){screen=2;mini=null;}else screen=0;invalidate();}
    private boolean isAdmin(){return "lvx20uwu".equalsIgnoreCase(game.name);}
    private void leaveName(){
        String name=typed.trim();game.name=name.isEmpty()?"Питомец":name;
        if(!isAdmin()&&GameClock.speed(getContext())!=1)GameClock.setSpeed(getContext(),1);
        screen=0;host.persist();PetNotifier.schedule(getContext());host.beep();
    }
    private void keyboard(float x,float y){
        String[] rows=keyboardEnglish?EN_ROWS:RU_ROWS;
        if(y>=300&&y<488){int row=(int)((y-300)/66);
            if(row>=0&&row<3){String keys=rows[row];float spacing=344f/keys.length();
                int col=(int)((x-28)/spacing);
                if(x>=28&&col>=0&&col<keys.length()&&typed.length()<12)typed+=keys.charAt(col);
            }
        }else if(y>=503&&y<549){
            int col=(int)((x-28)/34.5f);
            if(x>=28&&col>=0&&col<10&&typed.length()<12)typed+="1234567890".charAt(col);
        }else if(y>=558&&y<608){
            if(x<136)keyboardEnglish=!keyboardEnglish;
            else if(x<253){if(typed.length()<12)typed+=" ";}
            else if(typed.length()>0)typed=typed.substring(0,typed.length()-1);
        }
    }
    private void adminTap(float x,float y,long now){
        if(!isAdmin()){screen=0;return;}
        if(y>=200&&y<249){int n=Math.max(0,Math.min(4,(int)((x-11)/78)));GameClock.setSpeed(getContext(),new int[]{1,5,20,60,240}[n]);return;}
        if(y<285||y>=615)return;
        int col=x<200?0:1,row=(int)((y-285)/67),index=row*2+col;
        if(index<0||index>9)return;
        switch(index){
            case 0:GameClock.skip(getContext(),PetGame.HOUR);break;
            case 1:GameClock.skip(getContext(),PetGame.DAY);break;
            case 2:game.hunger=0;break;
            case 3:game.dirty=4;break;
            case 4:game.sickness=3;break;
            case 5:game.sickness=0;game.hunger=4;game.happy=4;game.dirty=0;break;
            case 6:game.stage=Math.min(PetGame.ADULT,game.stage+1);game.hatch=GameClock.now(getContext())-PetGame.DAY*game.stage;break;
            case 7:
                if(lastResetConfirm>now){game.reset(GameClock.now(getContext()));GameClock.setSpeed(getContext(),1);screen=0;lastResetConfirm=0;}
                else lastResetConfirm=now+5000;break;
            case 8:PetNotifier.debugNotify(getContext(),game);break;
            case 9:game.genes=(game.genes+1+random.nextInt(999))%1000;break;
        }
        game.say("ОТЛАДКА ИЗМЕНЕНА",GameClock.now(getContext()));
    }
    private void toolbar(float x,float y,long now){
        int i=TouchRegions.toolbarIndex(x,y);
        if(i<0)return;
        if(i==5){screen=8;return;}
        if(!game.canCare()){game.say("СНАЧАЛА ВЫЛУПЛЮСЬ!",now);return;}
        switch(i){
            case 0:screen=1;break;
            case 1:screen=2;break;
            case 2:game.light(now);break;
            case 3:
                medicineDrops=0;
                screen=5; // Always show the treatment scene, even if the pet is healthy.
                game.say(game.sickness>0?"НУЖНО ЛЕКАРСТВО!":"Я ЗДОРОВ!",now);
                break;
            case 4:lessons=new Lessons(System.currentTimeMillis());screen=6;break;
        }
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        float x=(e.getX()-dx)/scale,y=(e.getY()-dy)/scale;
        int action=e.getActionMasked();long now=GameClock.now(getContext()),wall=System.currentTimeMillis();
        if(action==MotionEvent.ACTION_DOWN){
            downX=lastX=x;downY=lastY=y;pointerX=x;pointerY=y;stroke=0;
            petStroke=screen==0&&game.canCare()&&game.lights&&x>90&&x<310&&y>273&&y<492;patAwarded=false;
            dragging=false;feedDrag=-1;
            if(screen==1&&y>=224&&y<376){feedDrag=x<200?0:1;dragging=true;}
            if(screen==5&&TouchRegions.medicineTap(x,y)&&game.sickness>0)dragging=true;
            if(screen==3&&mini!=null)mini.touch(0,x,y,wall);
            return true;
        }
        if(action==MotionEvent.ACTION_MOVE){
            pointerX=x;pointerY=y;
            if(petStroke&&x>88&&x<319&&y>272&&y<515){
                stroke+=Math.abs(x-lastX)+Math.abs(y-lastY);
                if(stroke>125&&!patAwarded){boolean purred=game.pet(now);patAwarded=true;stroke=0;if(purred)host.purr();host.persist();}
            }
            if(screen==3&&mini!=null){mini.touch(1,x,y,wall);}
            lastX=x;lastY=y;invalidate();return true;
        }
        if(action!=MotionEvent.ACTION_UP&&action!=MotionEvent.ACTION_CANCEL)return true;
        int originalScreen=screen; // Never treat the same touch as a tap in the newly opened scene.
        boolean did=false;
        if(action==MotionEvent.ACTION_CANCEL){dragging=false;invalidate();return true;}
        try{performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);}catch(Exception ignored){}
        if(screen==3&&mini!=null&&y<677){
            mini.touch(2,x,y,wall);did=true;
        }else if(screen==0){
            if(y>=659&&y<781){toolbar(x,y,now);did=true;}
            else if(y>0&&y<65&&x<300){if(isAdmin())screen=7;else showName();did=true;}
            else if(!game.alive&&y>=488&&y<555){host.saveMemorial();did=true;}
            else if(!game.alive&&y>=559&&y<620){game.reset(now);did=true;}
            else if(game.stage==PetGame.EGG){game.say("Я ЕЩЁ В ЯЙЦЕ!",now);did=true;}
            else if(y>477&&y<592){
                for(int i=0;i<game.dirty;i++){
                    if(Math.abs(x-(pooX(i)+20))<28&&Math.abs(y-(pooY(i)+12))<30){game.removeDirt(now);did=true;break;}
                }
            }
            if(!did&&petStroke&&!patAwarded&&stroke<100&&y>276&&y<500){game.say("ПОГЛАДЬ МЕНЯ ЖЕСТОМ!",now);did=true;}
        }else if(screen==1){
            if(dragging&&feedDrag>=0&&x>108&&x<293&&y>=399&&y<577){
                if(feedDrag==0)game.meal(now);else game.snack(now);screen=0;did=true;
            }
        }else if(screen==2){
            if(x>=21&&x<390&&y>=193&&y<661){
                int col=x<201?0:1,row=(int)((y-193)/94),idx=row*2+col;
                if(idx>=0&&idx<10){mini=new MiniGames(idx,wall);miniReward=false;screen=3;did=true;}
            }
        }else if(screen==4){if(y>=292&&y<608){keyboard(x,y);did=true;}}
        else if(screen==5){
            // Tap the vial or the wounded pet; a drag from the vial is also accepted.
            // Old v5 accepted only a tiny drop zone, causing apparently broken treatment.
            if(game.sickness<=0){
                if(TouchRegions.medicineTap(x,y)){screen=0;did=true;}
            } else if(TouchRegions.medicineTap(x,y)){
                // Broad touch surface; no small invisible drop target to miss.
                applyMedicine(now);did=true;
            }
        }else if(screen==6){
            if(lessons!=null&&lessons.finished){
                if(y>=530&&y<635){screen=0;did=true;}
            }else if(lessons!=null&&y>=411&&y<619&&x>=27&&x<374){
                int col=x<200?0:1,row=y<522?0:1;
                int result=lessons.choose(row*2+col,wall);
                if(result!=Lessons.WAIT){
                    if(result==Lessons.WON){
                        game.discipline=Math.min(4,game.discipline+1);
                        game.happy=Math.min(4,game.happy+1);
                        game.say("НОВОЕ УМЕНИЕ ОСВОЕНО",now);
                    }else if(result==Lessons.INCORRECT){game.say("НЕВЕРНО. ПОДУМАЙ ЕЩЁ",now);}
                    else if(result==Lessons.CORRECT){game.say("ВЕРНО",now);}
                    else if(result==Lessons.LOST){game.say("ПОПРОБУЕМ ПОЗЖЕ",now);}
                    did=true;
                }
            }
        }else if(screen==7){adminTap(x,y,now);did=true;}
        else if(screen==8){
            if(y>=202&&y<285){host.editPlayerNickname();did=true;}
            else if(y>=303&&y<386){host.editServer();did=true;}
            else if(y>=404&&y<487){host.toggleStatistics();did=true;}
            else if(y>=505&&y<588){host.checkUpdates();did=true;}
        }
        // Swipe over maze tiles moves the character rather than requiring precisely tapped arrows.
        if(screen==3&&mini!=null&&mini.type==2&&Math.hypot(x-downX,y-downY)>22){
            float vx=x-downX,vy=y-downY;
            if(Math.abs(vx)>Math.abs(vy))mini.move(vx>0?1:-1,0);
            else mini.move(0,vy>0?1:-1);
        }
        if(TouchRegions.sceneFooter(originalScreen,x,y)){
            if(x<199){if(screen==3){screen=2;mini=null;}else screen=0;did=true;}
            else if(screen==3&&mini!=null){mini=new MiniGames(mini.type,wall);miniReward=false;did=true;}
            else if(screen==4){leaveName();did=true;}
        }
        if(did)host.beep();
        game.advance(GameClock.now(getContext()));host.persist();invalidate();
        dragging=false;petStroke=false;
        return true;
    }
}
