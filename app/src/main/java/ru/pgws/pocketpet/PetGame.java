package ru.pgws.pocketpet;

import java.util.Calendar;
import java.util.Random;

/** Pure Java simulation: no services, wakelocks or background permissions needed. */
public final class PetGame {
    public static final long MINUTE = 60_000L;
    public static final long HOUR = 60L * MINUTE;
    public static final long DAY = 24L * HOUR;
    private static final long STEP = 10 * MINUTE;
    public static final int EGG=0, BABY=1, CHILD=2, TEEN=3, ADULT=4, DEAD=5;
    public long born, lastTick, hatch;
    public int stage, hunger, happy, discipline, weight, dirty, sickness, missed, strain, ticks;
    public int genes=-1, personality, gamesWon;
    public boolean lights, alive;
    public String notice = "ПРИВЕТ!";
    public String name = "";
    public long noticeUntil;
    private final Random random = new Random();

    public void reset(long now) {
        born=now; lastTick=now; hatch=now+5*MINUTE;
        stage=EGG; hunger=4; happy=4; discipline=0; weight=5; dirty=0;
        sickness=0; missed=0; strain=0; ticks=0; genes=(genes<0||genes>=PetSprites.COUNT) ? random.nextInt(PetSprites.COUNT) : (genes+1+random.nextInt(PetSprites.COUNT-1))%PetSprites.COUNT;
        personality=0; gamesWon=0; lights=true; alive=true; name="";
        say("НОВОЕ ЯЙЦО!",now);
    }

    public void advance(long now) {
        if (born<=0) { reset(now); return; }
        if (now<lastTick) { lastTick=now; return; } // device clock moved back
        if (!alive) { lastTick=now; return; }
        if (stage==EGG && now>=hatch) {
            stage=BABY; hunger=4; happy=4; weight=5;
            say("Я ВЫЛУПИЛСЯ!",now);
        }
        // Process elapsed time in 10-minute slices for persistence across restarts.
        // Very long absences end with natural aging rather than a CPU-intensive loop.
        if (now-lastTick > 35*DAY) {
            alive=false; stage=DEAD; lastTick=now; say("ДО СВИДАНИЯ...",now); return;
        }
        int total=(int)Math.min((now-lastTick)/STEP,5200L);
        for (int i=0;i<total && alive;i++) {
            lastTick += STEP;
            ticks++;
            if (lastTick>=hatch && stage==EGG) stage=BABY;
            if (stage==EGG) continue;
            int days=ageDays(lastTick);
            if (stage==BABY && lastTick-hatch >= HOUR) { stage=CHILD; weight=Math.max(8,weight); say("РАСТУ!",now); }
            if (stage==CHILD && lastTick-hatch >= DAY) { stage=TEEN; weight=Math.max(15,weight); say("НОВЫЙ ОБЛИК!",now); }
            if (stage==TEEN && lastTick-hatch >= 3*DAY) {
                stage=ADULT; weight=Math.max(20,weight);
                personality = (missed<=5 && discipline>=2) ? 0 : (missed<=14 ? 1 : 2);
                say("НОВЫЙ ОБЛИК!",now);
            }
            boolean asleep=isSleepTime(lastTick);
            if (ticks % (asleep ? 14:6)==0) hunger=Math.max(0,hunger-1);
            if (ticks% (asleep?18:9)==0) happy=Math.max(0,happy-1);
            if (ticks%11==0 && !asleep) dirty=Math.min(4,dirty+1);
            if (dirty>=3 && ticks%6==0) sickness=Math.min(20,sickness+1);
            if (sickness>0 && ticks%12==0) sickness=Math.min(20,sickness+1);
            boolean unmet=hunger==0 || happy==0 || dirty>=3 || sickness>0 || (asleep && lights);
            if (unmet) { strain++; if (strain>=9) { missed++; strain=0; } }
            else strain=0;
            if (ticks%12==0 && weight>7) weight--;
            int life=(18 + Math.min(4,discipline) - missed/4 - sickness/2);
            if (days>Math.max(7,life) || sickness>=17 || missed>=45) {
                alive=false; stage=DEAD; say("ДО СВИДАНИЯ...",now);
            }
        }
        if (alive && now-lastTick < STEP) { /* fractional step preserved */ }
        if (!alive) lastTick=now;
    }

    public void say(String s,long now) { notice=s; noticeUntil=now+2300; }
    public boolean sleeping(long now) { return stage!=EGG && stage!=DEAD && isSleepTime(now); }
    private boolean isSleepTime(long time) {
        Calendar c=Calendar.getInstance(); c.setTimeInMillis(time);
        int h=c.get(Calendar.HOUR_OF_DAY);
        // Baby goes to bed earlier; adult is awake longer.
        int bedtime=stage==BABY?20:stage==CHILD?21:22;
        return h>=bedtime || h<8;
    }
    public int ageDays(long now) { return (int)Math.max(0,(now-hatch)/DAY); }
    public int ageHours(long now) { return (int)Math.max(0,(now-hatch)/HOUR); }
    public boolean needsHelp(long now) { return alive && stage>EGG &&
          (hunger==0||happy==0||dirty>=3||sickness>0||(sleeping(now)&&lights)); }
    public boolean canCare() {return alive && stage!=EGG;}
    public void meal(long now) {
        if(!canCare())return;
        if(hunger==4) { say("Я СЫТ!",now); return; }
        hunger++; weight=Math.min(99,weight+1); say("ВКУСНО!",now);
    }
    public void snack(long now) {
        if(!canCare())return;
        happy=Math.min(4,happy+1); weight=Math.min(99,weight+2);
        if(weight>40 && random.nextInt(5)==0) sickness++;
        say("СЛАДКО!",now);
    }
    public void clean(long now) {
        if(!canCare())return;
        if (dirty==0) {say("УЖЕ ЧИСТО!",now);return;}
        dirty=0; strain=0; say("ЧИСТО!",now);
    }
    public void medicine(long now) {
        if(!canCare())return;
        if(sickness==0) {say("Я ЗДОРОВ!",now);return;}
        sickness=0; say("МНЕ ЛУЧШЕ!",now);
    }
    public void light(long now) {
        if(!canCare())return;
        lights=!lights;
        if(sleeping(now)&&!lights)strain=0;
        say(lights?"СВЕТ ВКЛ.":"СВЕТ ВЫКЛ.",now);
    }
    public void scold(long now) {
        if(!canCare())return;
        if(needsHelp(now)) { discipline=Math.min(4,discipline+1); strain=0; say("ХОРОШО!",now); }
        else { happy=Math.max(0,happy-1); say("НЕЧЕСТНО!",now); }
    }
    public void rewardGame(boolean won,long now) {
        if(!canCare())return;
        if(won){ happy=Math.min(4,happy+1); gamesWon++; }
        weight=Math.max(5,weight-1);
        say(won?"УРА! МНЕ ВЕСЕЛО!":"ПОИГРАЕМ ЕЩЁ!",now);
    }
    public boolean play(int guess,long now) {
        if(!canCare())return false;
        int answer=random.nextInt(2);
        boolean win=answer==guess;
        if(win)happy=Math.min(4,happy+1);
        else happy=Math.max(0,happy-1);
        weight=Math.max(5,weight-1);
        say(win?"ПОБЕДА!":"ПОПРОБУЙ ЕЩЁ!",now);
        return win;
    }
}
