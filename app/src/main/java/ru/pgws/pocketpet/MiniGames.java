package ru.pgws.pocketpet;

import java.util.Random;

/** Ten standalone interactive games. Coordinates used by the renderer are 0..8 (3x3).
 *  No rewards are granted until a complete challenge is won. */
public final class MiniGames {
    public static final String[] NAMES={"Найди цель","Пары","Запомни","Реакция","Кроты",
            "Больше/меньше","Лови","Лабиринт","Ритм","Считай"};
    public static final String[] HELP={
            "Нажимай на отмеченную клетку", "Открой и найди три пары",
            "Запомни три клетки, затем повтори", "Жди сигнала и нажми вовремя",
            "Лови крота, пока он не спрятался", "Угадай следующее число",
            "Передвигай корзину под падающий мяч", "Доберись до выхода, минуя стены",
            "Нажми, когда появится сигнал", "Выбери правильную сумму"};
    public final int type;
    public int round=0, score=0, errors=0, target=0, cursor=1, value=5;
    public int selected=-1, first=-1, second=-1;
    public final int[] cells=new int[9];
    public final int[] sequence=new int[3];
    public boolean complete=false, won=false;
    public long started, deadline, revealUntil, nextAt;
    private final Random rng;
    public MiniGames(int gameType,long now){this(gameType,now,new Random());}
    MiniGames(int gameType,long now,Random random){
        if(gameType<0||gameType>=10)throw new IllegalArgumentException("game type");
        type=gameType; started=now; rng=random;
        target=rng.nextInt(9);
        if(type==1){
            int[] pairs={1,1,2,2,3,3};
            for(int i=pairs.length-1;i>0;i--){int j=rng.nextInt(i+1);int t=pairs[i];pairs[i]=pairs[j];pairs[j]=t;}
            System.arraycopy(pairs,0,cells,0,6);
        }
        if(type==2){for(int i=0;i<3;i++)sequence[i]=rng.nextInt(9);revealUntil=now+3500;}
        if(type==3){nextAt=now+1500+rng.nextInt(1700);deadline=nextAt+1000;}
        if(type==4){deadline=now+2500;}
        if(type==6){target=rng.nextInt(3);deadline=now+1250;}
        if(type==7){cursor=0;cells[2]=1;cells[6]=1;}
        if(type==8){nextAt=now+1100;deadline=nextAt+650;}
        if(type==9){question();}
    }
    public String title(){return NAMES[type];}
    public String instruction(){return HELP[type];}
    private void finish(boolean result){complete=true;won=result;}
    private void penalty(){errors++;if(errors>=3)finish(false);}
    private void nextTarget(){int old=target; target=(old+1+rng.nextInt(8))%9;}
    private void question(){
        int a=1+rng.nextInt(9),b=1+rng.nextInt(9);
        value=a*100+b; target=rng.nextInt(3);
        for(int i=0;i<3;i++) cells[i]=a+b+(i-target)*2;
    }
    public String prompt(long now){
        if(complete)return won?"ПОБЕДА! ПИТОМЕЦ РАД!":"НЕ ПОЛУЧИЛОСЬ";
        switch(type){
            case 0:return "Поймай: "+(score+1)+" / 5";
            case 1:return "Открыто пар: "+score+" / 3";
            case 2:return now<revealUntil?"ЗАПОМИНАЙ!":"Повтори: "+round+" / 3";
            case 3:return now<nextAt?"ЖДИ...":now<=deadline?"ЖМИ!":"ПОЗДНО!";
            case 4:return "Попаданий: "+score+" / 5";
            case 5:return "Число "+value+". Следующее?";
            case 6:return "Лови мяч! "+score+" / 4";
            case 7:return "Путь к выходу: "+cursor+" / 8";
            case 8:return now>=nextAt&&now<=deadline?"ЖМИ В РИТМ!":"Приготовься...";
            default:return (value/100)+" + "+(value%100)+" = ?";
        }
    }
    public int progressGoal(){switch(type){case 1:case 2:case 8:return 3;case 6:case 9:return 4;default:return 5;}}
    public void tick(long now){
        if(complete)return;
        if(type==1 && second>=0 && now>=revealUntil){first=-1;second=-1;}
        if(type==3 && now>deadline){penalty();if(!complete)nextReaction(now);}
        if(type==4 && now>deadline){penalty();if(!complete){nextTarget();deadline=now+2500;}}
        if(type==6 && now>deadline){
            if(cursor==target)score++;else penalty();
            round++;
            if(score>=4)finish(true);
            else if(round>=7 || score+(7-round)<4)finish(false);
            else {target=rng.nextInt(3);deadline=now+1250;}
        }
        if(type==8 && now>deadline){penalty();if(!complete)nextRhythm(now);}
    }
    private void nextReaction(long now){nextAt=now+1000+rng.nextInt(1700);deadline=nextAt+1000;}
    private void nextRhythm(long now){nextAt=now+900;deadline=nextAt+650;}
    /** Accepts a cell index 0..8. Timed challenges never complete automatically on touch. */
    public void tap(int cell,long now){
        tick(now);
        if(complete||cell<0||cell>8)return;
        switch(type){
            case 0:
                if(cell==target){score++;if(score>=5)finish(true);else nextTarget();}
                else penalty();break;
            case 1:
                if(cell>=6||cells[cell]==0||second>=0)return;
                if(first<0){first=cell;return;}
                if(first==cell)return;
                second=cell;
                if(cells[first]==cells[second]){
                    cells[first]=0;cells[second]=0;first=-1;second=-1;
                    score++;if(score>=3)finish(true);
                } else {revealUntil=now+850;penalty();}
                break;
            case 2:
                if(now<revealUntil)return;
                if(cell==sequence[round]){round++;score=round;if(round>=3)finish(true);}
                else {round=0;score=0;penalty();}
                break;
            case 3:
                if(now<nextAt){penalty();if(!complete)nextReaction(now);}
                else if(now<=deadline){score++;if(score>=5)finish(true);else nextReaction(now);}
                break;
            case 4:
                if(cell==target && now<=deadline){score++;if(score>=5)finish(true);else {nextTarget();deadline=now+2500;}}
                else penalty();break;
            case 5:
                if(cell!=3&&cell!=5)return;
                int prev=value;
                int next=1+rng.nextInt(9);
                while(next==prev)next=1+rng.nextInt(9);
                if((cell==3&&next<prev)||(cell==5&&next>prev))score++;
                else penalty();
                value=next;round++;
                if(score>=5)finish(true);
                else if(round>=7||score+(7-round)<5)finish(false);
                break;
            case 6:
                if(cell<=2)cursor=cell;
                break;
            case 7:
                if(cells[cell]==1)return;
                if(Math.abs(cell/3-cursor/3)+Math.abs(cell%3-cursor%3)==1){
                    cursor=cell;round++;
                    if(cursor==8)finish(true);
                }else penalty();
                break;
            case 8:
                if(now<nextAt){penalty();if(!complete)nextRhythm(now);}
                else if(now<=deadline){score++;if(score>=3)finish(true);else nextRhythm(now);}
                break;
            case 9:
                if(cell>2)return;
                if(cell==target)score++;else penalty();
                round++;
                if(score>=4)finish(true);
                else if(round>=6||score+(6-round)<4)finish(false);
                else question();break;
        }
    }
    public int currentDisplayCell(long now){
        if(complete)return -1;
        if(type==0||type==4)return target;
        if(type==2 && now<revealUntil)return sequence[((int)((now-started)/1100))%3];
        if(type==3)return now>=nextAt&&now<=deadline?4:-1;
        if(type==6)return -1;
        if(type==8)return now>=nextAt&&now<=deadline?4:-1;
        return -1;
    }
}
