package ru.pgws.pocketpet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** One fullscreen LCD, direct taps on the icons: no virtual case or A/B/C buttons. */
public final class PetView extends View {
    private final PetGame g;
    private final MainActivity activity;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final int INK = Color.rgb(44, 64, 48);
    private static final int LCD = Color.rgb(177, 202, 154);
    private static final int LIGHT = Color.rgb(193, 218, 170);
    private static final int DIM = Color.rgb(124, 154, 115);
    private static final String[] ICON_LABELS = {
            "СТАТУС", "ЕДА", "СВЕТ", "ИГРЫ", "ЛЕЧИТЬ", "УБОРКА", "ВОСПИТ.", "СИГНАЛ"
    };
    private int screen=0, statPage=0;
    private MiniGames mini;
    private int gamePage=0;
    private boolean rewardPaid=false;
    private long resetConfirmUntil;
    private int cachedGene=-1, cachedStage=-1, cachedPersonality=-1;
    private String[] cachedPet;
    private float scale,dx,dy;
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final String[] EGG={
      ".....######.....", "....########....", "...##########...", "..############..",
      "..############..", ".##############.", ".##############.", ".##############.",
      ".##############.", ".##############.", ".##############.", "..############..",
      "..############..", "...##########...", "....########....", ".....######....."};
    private static final String[] BABY={
      "................", "................", ".....######.....", "...##########...",
      "..############..", "..############..", ".##############.", ".###..####..###.",
      ".###..####..###.", ".##############.", ".#####.##.#####.", ".######..######.",
      "..############..", "...##########...", "....##....##....", "................"};
    private static final String[] CHILD={
      "..###......###..", "..####....####..", "...###....###...", "...##########...",
      "..############..", ".##############.", ".###..####..###.", ".###..####..###.",
      ".##############.", ".#####....#####.", ".######..######.", ".##############.",
      "..############..", "...###.##.###...", "....##....##....", "....##....##...."};
    private static final String[] TEEN={
      ".##..........##.", ".###........###.", "..###......###..", "..############..",
      ".##############.", ".##############.", ".###..####..###.", ".###..####..###.",
      ".##############.", ".#####....#####.", ".######..######.", ".##############.",
      "..############..", "...###.##.###...", "...##......##...", "..###......###.."};
    private static final String[] ADULT_A={
      "####........####", ".####......####.", "..############..", ".##############.",
      "################", "####..####..####", "####..####..####", "################",
      "######.##.######", "#######..#######", ".##############.", ".##############.",
      "..############..", ".###..####..###.", ".##....##....##.", "###..........###"};
    private static final String[] ADULT_B={
      ".....##..##.....", "....########....", "..############..", ".##############.",
      "################", "###..######..###", "###..######..###", "################",
      "######....######", "#######..#######", "################", ".##############.",
      "..############..", "...##########...", "..###......###..", "..##........##.."};
    private static final String[] ADULT_C={
      ".###........###.", ".####......####.", ".##############.", "################",
      "#####..##..#####", "#####..##..#####", "################", "##..########..##",
      "###.########.###", "####........####", ".##############.", "..############..",
      "..############..", ".###..####..###.", "..##..####..##..", "..###......###.."};
    private static final String[] GHOST={
      "................", ".....######.....", "...##########...", "..############..",
      ".##############.", ".##############.", ".###..####..###.", ".###..####..###.",
      ".##############.", ".##############.", ".##############.", ".##############.",
      ".###.##.##.###..", "..#..#..#..#...", "................", "................"};


    public PetView(Context ctx, PetGame game, MainActivity host) {
        super(ctx);
        this.g = game;
        this.activity = host;
        setContentDescription("Виртуальный питомец. Нажимайте на значки меню и игровые клетки.");
    }

    private void color(int col) {
        p.setShader(null); p.setColor(col); p.setStyle(Paint.Style.FILL);
        p.setStrokeWidth(1f); p.clearShadowLayer();
    }
    private void fill(Canvas c,int col,float x,float y,float x2,float y2) {
        color(col);c.drawRect(x,y,x2,y2,p);
    }
    private void outline(Canvas c,int col,float x,float y,float x2,float y2,float width) {
        color(col);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);
        c.drawRect(x,y,x2,y2,p);p.setStyle(Paint.Style.FILL);
    }
    private void round(Canvas c,int col,float x,float y,float x2,float y2,float r) {
        color(col);c.drawRoundRect(new RectF(x,y,x2,y2),r,r,p);
    }
    private void txt(Canvas c,String s,float x,float y,int size,int col,boolean center) {
        color(col);p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(size);p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);
        c.drawText(s,x,y,p);
    }
    private void line(Canvas c,int col,float x,float y,float xx,float yy,float width) {
        color(col);p.setStrokeWidth(width);c.drawLine(x,y,xx,yy,p);
    }
    @Override protected void onDraw(Canvas raw) {
        super.onDraw(raw);
        raw.drawColor(LCD);
        int w=getWidth(), h=getHeight();
        if(w<=0 || h<=0)return;
        scale=Math.min(w/400f,h/800f);
        dx=(w-400*scale)/2f;dy=(h-800*scale)/2f;
        raw.save();raw.translate(dx,dy);raw.scale(scale,scale);
        drawLCD(raw);
        drawHeader(raw);
        drawIcons(raw);
        drawContent(raw);
        raw.restore();
    }
    private void drawLCD(Canvas c) {
        fill(c,LCD,0,0,400,800);
        // Flat LCD surface, not a plastic Tamagotchi shell or hardware bezel.
        for(int y=4;y<796;y+=9)fill(c,Color.argb(11,41,76,44),0,y,400,y+1);
        for(int x=16;x<399;x+=20)
            for(int y=169;y<646;y+=20)fill(c,Color.argb(10,47,83,43),x,y,x+2,y+2);
        line(c,INK,8,157,392,157,2);
        line(c,INK,8,647,392,647,2);
    }
    private void drawHeader(Canvas c) {
        txt(c,"КАРМАННЫЙ ПИТОМЕЦ",15,35,17,INK,false);
        txt(c,clock.format(new Date(System.currentTimeMillis())),376,34,17,INK,true);
        txt(c,"96 / СЕНСОРНОЕ УПРАВЛЕНИЕ",17,57,10,INK,false);
        txt(c,g.alive ? "ВОЗРАСТ "+g.ageDays(System.currentTimeMillis())+" Д" : "ПАМЯТЬ",376,56,11,INK,true);
        line(c,DIM,16,67,384,67,1);
    }
    private void drawIcons(Canvas c) {
        for(int i=0;i<8;i++){
            boolean top=i<4;
            float x=50+(i%4)*100;
            float cy=top?115:697;
            float left=x-44, right=x+44;
            if((i==7 && g.needsHelp(System.currentTimeMillis())) && (System.currentTimeMillis()/600)%2==0)
                fill(c,INK,left,cy-41,right,cy+39);
            else outline(c,DIM,left,cy-41,right,cy+39,2);
            boolean alert=i==7 && g.needsHelp(System.currentTimeMillis())
                    && (System.currentTimeMillis()/600)%2==0;
            drawIcon(c,i,x,cy-11,alert?LCD:INK);
            txt(c,ICON_LABELS[i],x,cy+30,11,alert?LCD:INK,true);
        }
        txt(c,"НАЖМИ НА ЗНАЧОК",200,785,11,INK,true);
    }
    private void drawIcon(Canvas c,int index,float x,float y,int col) {
        // Monochrome icons intentionally use 90-degree pixel geometry.
        switch(index){
            case 0: // weight/health meter
                outline(c,col,x-14,y-13,x+14,y+12,2);
                fill(c,col,x-8,y+3,x-3,y+7);
                fill(c,col,x-1,y-4,x+4,y+7);
                fill(c,col,x+6,y-9,x+11,y+7);
                break;
            case 1: // food bowl
                fill(c,col,x-15,y+2,x+15,y+6);
                fill(c,col,x-11,y+7,x+11,y+12);
                fill(c,col,x-5,y-10,x+1,y-4);
                fill(c,col,x+5,y-7,x+10,y-2);
                break;
            case 2: // lightbulb
                round(c,col,x-8,y-14,x+8,y+2,8);
                fill(c,col,x-6,y+3,x+6,y+7);
                fill(c,col,x-4,y+8,x+4,y+12);
                fill(c,col,x-15,y-6,x-11,y-2);
                fill(c,col,x+11,y-6,x+15,y-2);
                break;
            case 3: // play arrows
                fill(c,col,x-15,y-4,x-2,y);
                fill(c,col,x-10,y-9,x-6,y+5);
                fill(c,col,x+3,y-4,x+16,y);
                fill(c,col,x+9,y-9,x+13,y+5);
                break;
            case 4: // medicine
                fill(c,col,x-4,y-15,x+4,y+13);
                fill(c,col,x-15,y-5,x+15,y+4);
                break;
            case 5: // bathroom/sweep
                fill(c,col,x-12,y+1,x+12,y+11);
                fill(c,col,x-8,y-5,x+9,y+2);
                fill(c,col,x-3,y-11,x+6,y-4);
                fill(c,LCD,x-6,y+4,x-3,y+7);
                fill(c,LCD,x+4,y+4,x+7,y+7);
                break;
            case 6: // discipline
                fill(c,col,x-4,y-15,x+5,y+5);
                fill(c,col,x-4,y+10,x+5,y+15);
                break;
            default: // attention bell
                fill(c,col,x-10,y-5,x+10,y+7);
                fill(c,col,x-6,y-12,x+6,y-3);
                fill(c,col,x-14,y+8,x+14,y+11);
                fill(c,col,x-3,y+12,x+3,y+15);
        }
    }
    private void drawContent(Canvas c) {
        long now=System.currentTimeMillis();
        if(!g.alive){
            sprite(c,GHOST,120,252,10,false);
            txt(c,"ДО СВИДАНИЯ...",200,447,21,INK,true);
            txt(c,"НАЖМИ ДЛЯ НОВОГО ЯЙЦА",200,483,14,INK,true);
            return;
        }
        if(g.stage==PetGame.EGG){
            sprite(c,EGG,120,(now/550)%2==0?254:259,10,false);
            txt(c,"ЯЙЦО СОГРЕВАЕТСЯ",200,453,18,INK,true);
            int left=(int)Math.max(0,(g.hatch-now+59999)/PetGame.MINUTE);
            txt(c,"ДО ВЫЛУПЛЕНИЯ "+left+" МИН",200,481,15,INK,true);
            txt(c,"ВАРИАНТ №"+String.format(Locale.US,"%04d",g.genes),200,525,13,INK,true);
            return;
        }
        switch(screen){
            case 1:drawFood(c);return;
            case 2:drawGamesMenu(c);return;
            case 3:drawStats(c,now);return;
            case 4:drawClock(c,now);return;
            case 5:drawMiniGame(c,now);return;
            default:drawPet(c,now);
        }
    }
    private void drawPet(Canvas c,long now) {
        txt(c,(g.name==null||g.name.isEmpty())?"ПИТОМЕЦ":g.name,200,210,22,INK,true);
        line(c,DIM,55,504,345,504,2);
        line(c,DIM,83,512,317,512,1);
        if(g.sleeping(now)&&!g.lights){
            txt(c,"Z z Z",200,338,39,INK,true);
            txt(c,"СПИТ",200,404,20,INK,true);
        }else{
            int bob=((now/400)%3==0)?5:0;
            sprite(c,chooseSprite(),100,245+bob,10,(now/3700)%8==0);
            if(g.dirty>0)drawPoo(c,70,469);
            if(g.dirty>1)drawPoo(c,301,477);
            if(g.sickness>0)txt(c,"+",326,278,25,INK,true);
            if(g.sleeping(now))txt(c,"Zz",308,304,20,INK,true);
            if(g.needsHelp(now))txt(c,"!",68,300,32,INK,true);
        }
        if(g.noticeUntil>now)txt(c,g.notice,200,568,18,INK,true);
        else txt(c,g.sleeping(now)?"ПОРА СПАТЬ":"ЗАБОТЬСЯ ОБО МНЕ!",200,563,17,INK,true);
        txt(c,"НАЖИМАЙ НА ИКОНКИ",200,600,13,DIM,true);
        txt(c,"ПИТОМЕЦ №"+String.format(Locale.US,"%04d",g.genes)+" / 1000",200,624,12,INK,true);
    }
    private void drawFood(Canvas c) {
        txt(c,"КОРМЛЕНИЕ",200,224,25,INK,true);
        choice(c,"ОБЕД","УТОЛИТ ГОЛОД",330);
        choice(c,"СЛАДОСТЬ","ПОВЫСИТ РАДОСТЬ",430);
        txt(c,"ВЫБЕРИ ЕДУ НАЖАТИЕМ",200,577,13,INK,true);
        txt(c,"НАЖМИ МИМО, ЧТОБЫ ВЫЙТИ",200,608,12,DIM,true);
    }
    private void choice(Canvas c,String name,String subtitle,int cy){
        outline(c,INK,34,cy-35,366,cy+48,3);
        txt(c,name,200,cy+1,20,INK,true);
        txt(c,subtitle,200,cy+25,12,INK,true);
    }
    private void drawGamesMenu(Canvas c) {
        txt(c,"ВЫБЕРИ МИНИ-ИГРУ",200,207,22,INK,true);
        txt(c,"ИГРЫ "+(gamePage*5+1)+"–"+(gamePage*5+5)+" ИЗ 10",200,238,12,DIM,true);
        for(int i=0;i<5;i++){
            int y=266+i*61;
            outline(c,INK,26,y,374,y+52,2);
            txt(c,(gamePage*5+i+1)+". "+MiniGames.NAMES[gamePage*5+i],200,y+32,17,INK,true);
        }
        outline(c,INK,30,582,370,631,2);
        txt(c,gamePage==0?"СЛЕДУЮЩИЕ 5  >":"<  ПЕРВЫЕ 5",200,613,16,INK,true);
    }
    private void drawMiniGame(Canvas c,long now){
        if(mini==null){screen=2;return;}
        mini.tick(now);
        if(mini.complete && !rewardPaid){
            rewardPaid=true;
            g.rewardGame(mini.won,now);activity.persist();
        }
        txt(c,(mini.type+1)+". "+mini.title(),200,203,23,INK,true);
        txt(c,mini.instruction(),200,236,12,DIM,true);
        txt(c,mini.prompt(now),200,280,17,INK,true);
        if(mini.complete){
            txt(c,mini.won?"★":"×",200,392,65,INK,true);
            txt(c,mini.won?"НАСТРОЕНИЕ +1":"ПОПРОБУЙ ЕЩЁ!",200,454,22,INK,true);
            outline(c,INK,30,515,370,567,3);
            txt(c,"ИГРАТЬ СНОВА",200,549,18,INK,true);
            txt(c,"ВЫХОД: НАЖМИ НИЖЕ",200,608,12,DIM,true);
            return;
        }
        int n=mini.type==1?6:9;
        if(mini.type==5){
            outline(c,INK,28,327,191,502,3);
            outline(c,INK,209,327,372,502,3);
            txt(c,"МЕНЬШЕ",110,415,20,INK,true);
            txt(c,"БОЛЬШЕ",290,415,20,INK,true);
        }else if(mini.type==9){
            for(int i=0;i<3;i++){
                int y=320+78*i;
                outline(c,INK,42,y,358,y+65,3);
                txt(c,""+mini.cells[i],200,y+42,28,INK,true);
            }
        }else{
            for(int i=0;i<n;i++){
                int col=i%3,row=i/3;
                int x=32+col*113,y=307+row*80;
                boolean lit=(i==mini.currentDisplayCell(now));
                if(mini.type==1){lit=(mini.first==i||mini.second==i);}
                if(mini.type==7 && i==mini.cursor)lit=true;
                if(lit)fill(c,INK,x,y,x+106,y+72);
                else outline(c,INK,x,y,x+106,y+72,2);
                int textCol=lit?LCD:INK;
                if(mini.type==1){
                    if(mini.cells[i]==0)txt(c,"✓",x+53,y+49,30,INK,true);
                    else txt(c,lit?""+mini.cells[i]:"?",x+53,y+49,32,textCol,true);
                }else if(mini.type==6){
                    if(i==mini.target+3)txt(c,"●",x+53,y+47,29,textCol,true);
                    if(i==mini.cursor+6)txt(c,"⌂",x+53,y+48,30,textCol,true);
                }else if(mini.type==7){
                    if(mini.cells[i]==1)txt(c,"×",x+53,y+49,30,INK,true);
                    else if(i==8)txt(c,"Ф",x+53,y+48,25,textCol,true);
                    else if(i==mini.cursor)txt(c,"●",x+53,y+48,25,textCol,true);
                    else txt(c,"·",x+53,y+48,21,textCol,true);
                }else if(lit)txt(c,mini.type==4?"●":"!",x+53,y+49,28,textCol,true);
            }
        }
        txt(c,"УСПЕХ: "+mini.score+"/"+mini.progressGoal()+"  ОШИБОК: "+mini.errors+"/3",200,590,15,INK,true);
        txt(c,"НАЖМИ НИЖЕ ДЛЯ ВЫХОДА",200,620,12,DIM,true);
    }
    private void drawStats(Canvas c,long now) {
        txt(c,"СОСТОЯНИЕ",200,223,24,INK,true);
        txt(c,"СТРАНИЦА "+(statPage+1)+" / 3",200,255,13,DIM,true);
        if(statPage==0){
            txt(c,"ИМЯ: "+(g.name==null||g.name.isEmpty()?"ПИТОМЕЦ":g.name),200,310,18,INK,true);
            txt(c,"ВОЗРАСТ: "+g.ageDays(now)+" ДНЕЙ",200,355,18,INK,true);
            txt(c,"ВЕС: "+g.weight+" Г",200,400,18,INK,true);
            txt(c,"ПОБЕД В ИГРАХ: "+g.gamesWon,200,446,15,INK,true);
            outline(c,INK,60,464,340,517,2);
            txt(c,"ПЕРЕИМЕНОВАТЬ",200,496,18,INK,true);
        } else if(statPage==1){
            txt(c,"СЫТОСТЬ",200,321,18,INK,true);
            hearts(c,g.hunger,144,344);
            txt(c,"НАСТРОЕНИЕ",200,409,18,INK,true);
            hearts(c,g.happy,144,431);
        } else {
            txt(c,"ВОСПИТАНИЕ",200,324,18,INK,true);
            hearts(c,g.discipline,144,349);
            txt(c,"ЗДОРОВЬЕ: "+(g.sickness>0?"БОЛЕЕТ":"ХОРОШО"),200,422,18,INK,true);
            outline(c,INK,73,466,327,526,2);
            txt(c,resetConfirmUntil>now?"ПОДТВЕРДИ НАЖАТИЕМ":"НОВОЕ ЯЙЦО",200,502,15,INK,true);
        }
        txt(c,"<  НАЗАД       ДАЛЬШЕ  >",200,562,16,INK,true);
        txt(c,"НАЖМИ СЛЕВА ИЛИ СПРАВА",200,603,12,DIM,true);
    }
    private void drawClock(Canvas c,long now) {
        txt(c,"ВРЕМЯ",200,300,22,INK,true);
        txt(c,clock.format(new Date(now)),200,386,47,INK,true);
        txt(c,"НАЖМИ, ЧТОБЫ ВЕРНУТЬСЯ",200,555,14,INK,true);
    }
    private void drawPoo(Canvas c,int x,int y) {
        fill(c,INK,x,y+8,x+23,y+18);fill(c,INK,x+4,y+2,x+19,y+11);
        fill(c,INK,x+9,y-3,x+15,y+5);
        fill(c,LCD,x+6,y+9,x+9,y+12);fill(c,LCD,x+15,y+9,x+18,y+12);
    }
    private void hearts(Canvas c,int count,int x,int y) {
        for(int i=0;i<4;i++) {
            int px=x+(i-2)*40;
            int col=i<count?INK:DIM;
            fill(c,col,px,y+3,px+6,y+14);fill(c,col,px+14,y+3,px+20,y+14);
            fill(c,col,px+5,y,px+16,y+13);
            fill(c,col,px+5,y+12,px+17,y+18);fill(c,col,px+9,y+17,px+14,y+23);
        }
    }
    private String[] chooseSprite() {
        if(cachedPet==null || cachedGene!=g.genes || cachedStage!=g.stage || cachedPersonality!=g.personality) {
            cachedGene=g.genes; cachedStage=g.stage; cachedPersonality=g.personality;
            cachedPet=PetSprites.create(g.genes,g.stage,g.personality);
        }
        return cachedPet;
    }
    private void sprite(Canvas c,String[] rows,float x,float y,float s,boolean blink) {
        for(int yy=0;yy<rows.length;yy++){
            String row=rows[yy];
            for(int xx=0;xx<row.length();xx++)if(row.charAt(xx)=='#')
                fill(c,INK,x+xx*s,y+yy*s,x+(xx+1)*s,y+(yy+1)*s);
        }
        if(blink&&g.stage>PetGame.EGG){
            fill(c,INK,x+3*s,y+7*s,x+6*s,y+8*s);
            fill(c,INK,x+10*s,y+7*s,x+13*s,y+8*s);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getAction()!=MotionEvent.ACTION_UP)return true;
        float x=(e.getX()-dx)/scale;
        float y=(e.getY()-dy)/scale;
        if(x<0||x>400||y<0||y>800)return true;
        try {performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);}catch(Exception ignored){}
        long now=System.currentTimeMillis();
        g.advance(now);
        if(y>=73&&y<=155){
            hitIcon((int)Math.min(3,x/100),now);
        } else if(y>=653&&y<=740){
            hitIcon(4+(int)Math.min(3,x/100),now);
        } else if(y<69){
            screen=screen==4?0:4;
        } else if(!g.alive){
            if(y>170&&y<646){g.reset(now);screen=0;activity.beep();}
        } else if(g.stage==PetGame.EGG){
            g.say("СКОРО ВЫЛУПЛЮСЬ!",now);
        } else if(screen==1){
            if(y>=295&&y<=390){g.meal(now);screen=0;activity.beep();}
            else if(y>=397&&y<=492){g.snack(now);screen=0;activity.beep();}
            else screen=0;
        } else if(screen==2){
            if(y>=266&&y<570){
                int i=((int)y-266)/61;
                if(i>=0&&i<5){mini=new MiniGames(gamePage*5+i,now);screen=5;rewardPaid=false;activity.beep();}
            }else if(y>=582&&y<=637){gamePage=1-gamePage;activity.beep();}
            else if(y>172&&y<645)screen=0;
        } else if(screen==5){
            if(mini==null){screen=2;}
            else if(mini.complete){
                if(y>=515&&y<=570){mini=new MiniGames(mini.type,now);rewardPaid=false;}
                else if(y>575&&y<646)screen=2;
            }else if(y>=307&&y<=548){
                int cell=-1;
                if(mini.type==5){if(y>=327&&y<=503)cell=x<200?3:5;}
                else if(mini.type==9){if(x>=42&&x<=358)cell=Math.min(2,((int)y-320)/78);}
                else if(x>=32&&x<=371){
                    int col=Math.min(2,((int)x-32)/113);
                    int row=Math.min(2,((int)y-307)/80);
                    cell=row*3+col;
                    if(mini.type==6)cell=col;
                }
                if(cell>=0){
                    mini.tap(cell,now);
                    activity.beep();
                    if(mini.complete&&!rewardPaid){rewardPaid=true;g.rewardGame(mini.won,now);}
                }
            }else if(y>580&&y<644)screen=2;
        } else if(screen==3){
            if(statPage==0&&y>=464&&y<=517){activity.namePetAgain();}
            else if(statPage==2&&y>=466&&y<=526&&x>73&&x<327){
                if(resetConfirmUntil>now){g.reset(now);screen=0;mini=null;resetConfirmUntil=0;activity.beep();}
                else resetConfirmUntil=now+5000L;
            } else if(y>175&&y<644){statPage=(statPage+(x<200?2:1))%3;resetConfirmUntil=0;}
            else screen=0;
        } else if(screen==4){screen=0;}
        else if(y>172&&y<636){g.say("ПРИВЕТ!",now);activity.beep();}
        activity.persist();invalidate();return true;
    }
    private void hitIcon(int idx,long now){
        if(idx==7){
            if(g.needsHelp(now))g.say("НУЖНА ПОМОЩЬ!",now);
            else g.say("ВСЁ ХОРОШО!",now);
            activity.beep();return;
        }
        if(!g.alive){g.say("НАЖМИ ДЛЯ НОВОГО ЯЙЦА",now);return;}
        if(g.stage==PetGame.EGG){g.say("СКОРО ВЫЛУПЛЮСЬ!",now);return;}
        screen=0;
        switch(idx){
            case 0:screen=3;statPage=0;break;
            case 1:screen=1;break;
            case 2:g.light(now);break;
            case 3:screen=2;gamePage=0;break;
            case 4:g.medicine(now);break;
            case 5:g.clean(now);break;
            case 6:g.scold(now);break;
            default:break;
        }
        activity.beep();
    }
    public void back(){
        if(screen==5)screen=2;
        else if(screen!=0)screen=0;
        else screen=4;
        invalidate();
    }
}
