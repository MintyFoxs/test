package ru.pgws.pocketpet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Every visual is drawn locally; scales without XML layout or external image assets. */
public final class PetView extends View {
    private final PetGame g;
    private final MainActivity activity;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final int INK=Color.rgb(45,66,50);
    private static final int LCD=Color.rgb(178,202,155);
    private static final int CREAM=Color.rgb(255,235,244);
    private static final int PINK=Color.rgb(242,150,186);
    private int selected=0,screen=0,food=0,guess=0,statPage=0;
    private boolean lastWin;
    private long resultUntil;
    private float scale, dx,dy;
    private final SimpleDateFormat clock=new SimpleDateFormat("HH:mm",Locale.getDefault());
    private static final String[] ITEMS={"FOOD","LIGHT","GAME","MED","CLEAN","STATS","SCOLD"};
    private static final String[] SIGNS={"+","*","?","x","~","i","!"};

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

    public PetView(Context ctx,PetGame game,MainActivity host) {
        super(ctx);g=game;activity=host;setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        setContentDescription("Pocket Pet 96, three-button virtual pet game");
    }
    private void color(int col) {p.setShader(null);p.setColor(col);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(1);p.clearShadowLayer();}
    private void fill(Canvas c,int col,float l,float t,float r,float b) {color(col);c.drawRect(l,t,r,b,p);}
    private void oval(Canvas c,int col,float l,float t,float r,float b) {color(col);c.drawOval(new RectF(l,t,r,b),p);}
    private void round(Canvas c,int col,float l,float t,float r,float b,float radius) {
        color(col);c.drawRoundRect(new RectF(l,t,r,b),radius,radius,p);
    }
    private void txt(Canvas c,String s,float x,float y,float size,int col,boolean center) {
        color(col);p.setTypeface(android.graphics.Typeface.create("monospace",android.graphics.Typeface.BOLD));
        p.setTextSize(size);p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);
        c.drawText(s,x,y,p);
    }
    private void line(Canvas c,int col,float x,float y,float xx,float yy,float width) {
        color(col);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(x,y,xx,yy,p);
    }

    @Override protected void onDraw(Canvas raw) {
        super.onDraw(raw);
        int w=getWidth(),h=getHeight();
        raw.drawColor(Color.rgb(20,22,33));
        scale=Math.min(w/400f,h/800f); dx=(w-400*scale)/2f;dy=(h-800*scale)/2f;
        raw.save();raw.translate(dx,dy);raw.scale(scale,scale);
        drawBackdrop(raw);
        drawShell(raw);
        drawScreen(raw);
        drawButtons(raw);
        raw.restore();
    }
    private void drawBackdrop(Canvas c) {
        p.setShader(new LinearGradient(0,0,400,800,Color.rgb(40,37,58),Color.rgb(14,19,29),Shader.TileMode.CLAMP));
        c.drawRect(0,0,400,800,p);p.setShader(null);
        for(int i=0;i<34;i++) {
            float x=(i*91+37)%390+5,y=(i*167+91)%780+10;
            oval(c,Color.rgb(68,63,87),x,y,x+1.7f,y+1.7f);
        }
        txt(c,"P O C K E T   P E T",200,60,17,CREAM,true);
        txt(c,"RETRO DIGITAL COMPANION",200,82,9,Color.rgb(183,167,195),true);
        txt(c,"96  /  ORIGINAL PIXEL PET",200,743,10,Color.rgb(163,148,181),true);
    }
    private void drawShell(Canvas c) {
        // Thick shading builds an egg-shaped translucent plastic case without image files.
        oval(c,Color.rgb(10,10,18),25,124,377,712);
        oval(c,Color.rgb(131,71,109),20,110,380,699);
        oval(c,Color.rgb(255,203,222),24,107,374,689);
        oval(c,PINK,29,114,370,688);
        oval(c,Color.rgb(255,184,211),38,119,353,679);
        oval(c,Color.rgb(255,216,232),42,122,331,667);
        oval(c,Color.rgb(247,175,205),72,132,356,654);
        // Horizontal highlight curve and decorative stars.
        line(c,Color.rgb(255,239,246),78,151,123,131,4);
        oval(c,Color.rgb(255,244,246),81,151,88,158);
        txt(c,"*",308,176,30,Color.rgb(255,246,231),true);
        txt(c,"*",105,548,21,Color.rgb(255,236,235),true);
        txt(c,"*",296,549,19,Color.rgb(255,237,237),true);
        txt(c,"PET  96",200,207,18,Color.rgb(128,63,96),true);
        txt(c,"- DIGITAL FRIEND -",200,224,9,Color.rgb(157,83,111),true);
        // Recessed LCD bezel.
        round(c,Color.rgb(126,81,103),56,239,344,505,24);
        round(c,Color.rgb(92,66,82),61,244,339,497,18);
        round(c,Color.rgb(226,226,207),65,249,335,493,14);
        round(c,Color.rgb(68,84,67),70,253,330,487,9);
        round(c,LCD,75,258,325,482,5);
    }
    private void drawScreen(Canvas c) {
        c.save();c.clipRect(75,258,325,482);
        // Very subtle LCD horizontal scan lines.
        for (int y=262;y<481;y+=5) fill(c,Color.argb(21,70,95,62),75,y,325,y+1);
        drawMenuIcons(c);
        long now=System.currentTimeMillis();
        if(!g.alive) {
            sprite(c,GHOST,161,324,5f,false);
            txt(c,"GOODBYE",200,414,15,INK,true);
            txt(c,"PRESS B: NEW EGG",200,433,11,INK,true);
        } else if(g.stage==PetGame.EGG) {
            boolean bob=(now/600)%2==0;
            sprite(c,EGG,161,bob?320:324,5f,false);
            txt(c,"EGG IS WARMING",200,415,13,INK,true);
            int left=(int)Math.max(0,(g.hatch-now+59999)/PetGame.MINUTE);
            txt(c,"HATCH IN "+left+" MIN",200,435,10,INK,true);
        } else if(screen==1) {
            txt(c,"SELECT FOOD",200,305,14,INK,true);
            round(c,INK,101,323+food*34,299,352+food*34,4);
            txt(c,"MEAL   +HUNGER",200,344,food==0?12:11,food==0?LCD:INK,true);
            txt(c,"SNACK  +HAPPY",200,378,food==1?12:11,food==1?LCD:INK,true);
            txt(c,"A:CHOOSE  B:FEED",200,418,10,INK,true);
        } else if(screen==2) {
            txt(c,"GUESS THE SIDE",200,306,13,INK,true);
            txt(c,"<       >",200,370,43,INK,true);
            if(resultUntil>now) txt(c,lastWin?"CORRECT!":"WRONG!",200,408,16,INK,true);
            else {
                round(c,INK,guess==0?107:238,386,guess==0?165:297,393,3);
                txt(c,"A:SWITCH  B:GO",200,425,10,INK,true);
            }
        } else if(screen==3) {
            txt(c,"PET STATUS  "+(statPage+1)+"/3",200,301,12,INK,true);
            if(statPage==0) {
                txt(c,"AGE: "+g.ageDays(now)+" DAYS",200,337,17,INK,true);
                txt(c,"WEIGHT: "+g.weight+" G",200,367,14,INK,true);
                txt(c,"CARE MISTAKES: "+g.missed,200,398,10,INK,true);
            } else if(statPage==1) {
                txt(c,"HUNGER",200,327,13,INK,true);hearts(c,g.hunger,150,343);
                txt(c,"HAPPINESS",200,389,13,INK,true);hearts(c,g.happy,150,402);
            } else {
                txt(c,"DISCIPLINE",200,329,13,INK,true);hearts(c,g.discipline,150,347);
                txt(c,"HEALTH: "+(g.sickness>0?"SICK":"OK"),200,394,13,INK,true);
            }
            txt(c,"A:NEXT  C:BACK",200,432,10,INK,true);
        } else if(screen==4) {
            txt(c,"CLOCK",200,316,15,INK,true);
            txt(c,clock.format(new Date(now)),200,372,39,INK,true);
            txt(c,"C:BACK",200,415,10,INK,true);
        } else {
            if(g.sleeping(now) && !g.lights) {
                txt(c,"Z z Z",200,349,25,INK,true);
                txt(c,"SLEEPING",200,398,13,INK,true);
            } else {
                String[] pic= chooseSprite();
                int offset=(((now/350)%4)==0)?3:0;
                sprite(c,pic,161,318+offset,5f,(now/5000)%9==0);
                if(g.dirty>0)drawPoo(c,124,391);
                if(g.dirty>1)drawPoo(c,270,396);
                if(g.sickness>0)txt(c,"+",277,334,25,INK,true);
                if(g.sleeping(now))txt(c,"Zz",275,338,15,INK,true);
                if(g.needsHelp(now))txt(c,"!",109,328,20,INK,true);
            }
            if(g.noticeUntil>now)txt(c,g.notice,200,438,13,INK,true);
            else txt(c,clock.format(new Date(now)),200,438,15,INK,true);
        }
        if(g.sleeping(now)&&!g.lights)fill(c,Color.argb(58,28,48,35),75,283,325,443);
        c.restore();
    }
    private void drawMenuIcons(Canvas c) {
        for(int i=0;i<ITEMS.length;i++) {
            float x=i<4? 103+i*63:137+(i-4)*63;
            float y=i<4?274:465;
            boolean active=i==selected && screen==0 && g.stage>PetGame.EGG && g.alive;
            round(c,active?INK:Color.rgb(141,169,124),x-20,y-13,x+20,y+14,4);
            txt(c,SIGNS[i],x,y+7,18,active?LCD:INK,true);
        }
    }
    private void drawPoo(Canvas c,int x,int y) {
        fill(c,INK,x,y+8,x+23,y+18);
        fill(c,INK,x+4,y+2,x+19,y+11);
        fill(c,INK,x+9,y-3,x+15,y+5);
        fill(c,LCD,x+6,y+9,x+9,y+12);fill(c,LCD,x+15,y+9,x+18,y+12);
    }
    private void hearts(Canvas c,int count,int x,int y) {
        for(int i=0;i<4;i++) {
            int px=x+(i-2)*33;
            int col=i<count?INK:Color.rgb(129,154,120);
            fill(c,col,px,y+3,px+5,y+13);fill(c,col,px+12,y+3,px+17,y+13);
            fill(c,col,px+4,y,px+13,y+12);
            fill(c,col,px+4,y+12,px+14,y+16);
            fill(c,col,px+7,y+15,px+12,y+20);
        }
    }
    private String[] chooseSprite() {
        switch(g.stage) {
            case PetGame.BABY:return BABY;
            case PetGame.CHILD:return CHILD;
            case PetGame.TEEN:return TEEN;
            case PetGame.ADULT:return g.personality==0?ADULT_A:(g.personality==1?ADULT_B:ADULT_C);
            default:return EGG;
        }
    }
    private void sprite(Canvas c,String[] rows,float x,float y,float s,boolean blink) {
        // 16x16 monochrome sprite, with animated eye blink.
        for(int yy=0;yy<rows.length;yy++) {
            String row=rows[yy];
            for(int xx=0;xx<row.length();xx++) if(row.charAt(xx)=='#') {
                fill(c,INK,x+xx*s,y+yy*s,x+(xx+1)*s,y+(yy+1)*s);
            }
        }
        if(blink && g.stage>PetGame.EGG) {
            fill(c,INK,x+3*s,y+7*s,x+6*s,y+8*s);
            fill(c,INK,x+10*s,y+7*s,x+13*s,y+8*s);
        }
    }
    private void drawButtons(Canvas c) {
        txt(c,"A",103,575,12,Color.rgb(133,70,102),true);
        txt(c,"B",200,607,12,Color.rgb(133,70,102),true);
        txt(c,"C",297,575,12,Color.rgb(133,70,102),true);
        button(c,103,603,29);button(c,200,633,29);button(c,297,603,29);
        txt(c,"SELECT",103,658,9,Color.rgb(137,72,109),true);
        txt(c,"OK",200,689,9,Color.rgb(137,72,109),true);
        txt(c,"BACK",297,658,9,Color.rgb(137,72,109),true);
    }
    private void button(Canvas c,int x,int y,int rad) {
        oval(c,Color.rgb(142,79,115),x-rad-2,y-rad+4,x+rad+2,y+rad+6);
        oval(c,Color.rgb(255,241,246),x-rad,y-rad-2,x+rad,y+rad-2);
        oval(c,Color.rgb(165,94,129),x-rad+3,y-rad+1,x+rad-3,y+rad-5);
        oval(c,Color.rgb(255,220,235),x-rad+6,y-rad+4,x+rad-9,y+rad-13);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()!=MotionEvent.ACTION_UP) return true;
        float x=(event.getX()-dx)/scale,y=(event.getY()-dy)/scale;
        if(y>563 && y<682) {
            if(x>60&&x<145) press(0);
            else if(x>157&&x<244) press(1);
            else if(x>254&&x<344) press(2);
        }
        return true;
    }
    private void press(int which) {
        activity.beep();
        long now=System.currentTimeMillis();g.advance(now);
        if(!g.alive) {
            if(which==1) {g.reset(now);screen=0;selected=0;}
        } else if(g.stage==PetGame.EGG) {
            if(which==1)g.say("HATCHING SOON!",now);
        } else if(screen==1) {
            if(which==0)food=1-food;
            if(which==1) {if(food==0)g.meal(now);else g.snack(now);screen=0;}
            if(which==2)screen=0;
        } else if(screen==2) {
            if(which==0)guess=1-guess;
            if(which==1) {lastWin=g.play(guess,now);resultUntil=now+1800;}
            if(which==2)screen=0;
        } else if(screen==3) {
            if(which==0)statPage=(statPage+1)%3;
            if(which==2||which==1)screen=0;
        } else if(screen==4) {
            if(which==2||which==1)screen=0;
        } else {
            if(which==0)selected=(selected+1)%ITEMS.length;
            if(which==2)screen=4;
            if(which==1) {
                switch(selected) {
                    case 0:screen=1;food=0;break;
                    case 1:g.light(now);break;
                    case 2:screen=2;guess=0;break;
                    case 3:g.medicine(now);break;
                    case 4:g.clean(now);break;
                    case 5:screen=3;statPage=0;break;
                    case 6:g.scold(now);break;
                }
            }
        }
        activity.persist();invalidate();
    }
    public void back() {if(screen!=0){screen=0;invalidate();} else {screen=4;invalidate();}}
}
