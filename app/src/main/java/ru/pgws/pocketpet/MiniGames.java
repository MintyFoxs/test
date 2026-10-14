package ru.pgws.pocketpet;

import java.util.Arrays;
import java.util.Random;

/** Ten distinct randomized touch challenges.  Coordinates are the 400x800 LCD canvas. */
public final class MiniGames {
    public static final String[] NAMES={"ЛОВИ ЯГОДЫ","ПАРНЫЕ КАРТЫ","СЛУЧАЙНЫЙ ЛАБИРИНТ",
        "ПИКСЕЛЬ-АРТ","БАРАБАННЫЙ РИТМ","ПО ПОРЯДКУ","КРЕСТИКИ-НОЛИКИ",
        "ПРЫЖКИ","НАЙДИ ОТЛИЧИЕ","РАЗЛОЖИ ФИГУРЫ"};
    public static final String[] HELP={"ВЕДИ КОРЗИНУ ПОД ЯГОДОЙ","ОТКРОЙ 6 ПАР",
        "ПРОВЕДИ К ВЫХОДУ","СКОПИРУЙ УЗОР СПРАВА","БЕЙ В РИТМ МЕТКИ",
        "НАЖМИ ЧИСЛА 1...9","ВЫИГРАЙ У КОМПЬЮТЕРА","ПРЫГАЙ ПЕРЕД ЯЩИКОМ",
        "НАЙДИ ЛИШНЮЮ ТОЧКУ СПРАВА","ПЕРЕТАЩИ ФИГУРУ В КОРЗИНУ"};
    public final int type;
    public final int[] cells=new int[49];
    public final int[] aux=new int[49];
    public final int[] bins={0,1,2};
    public int score,errors,target,selection=-1,second=-1,round,cursor=1,playerX=1,playerY=1,dragX=200;
    public int sourceType=0,difference=0,pattern=0;
    public boolean complete,won,dragging,jumped;
    public long started,deadline,nextAt,revealUntil,jumpUntil,period=1600;
    private final Random random;
    public MiniGames(int type,long now){this(type,now,new Random());}
    public MiniGames(int type,long now,Random random){
        if(type<0||type>=10)throw new IllegalArgumentException("type");
        this.type=type;this.random=random;this.started=now;
        switch(type){
            case 0:nextFruit(now);break;
            case 1:for(int i=0;i<12;i++)cells[i]=i/2+1;shuffle(cells,12);break;
            case 2:maze();break;
            case 3:pattern();break;
            case 4:nextBeat(now);break;
            case 5:for(int i=0;i<16;i++)cells[i]=i<9?i+1:0;shuffle(cells,16);break;
            case 6:break;
            case 7:nextJump(now);break;
            case 8:difference();break;
            case 9:shuffle(bins,3);sourceType=random.nextInt(3);break;
        }
    }
    public String title(){return NAMES[type];}
    public String instruction(){return HELP[type];}
    public int goal(){switch(type){case 1:return 6;case 2:case 3:case 6:return 1;case 4:case 9:return 8;case 5:return 9;case 8:return 4;default:return 5;}}
    private void finish(boolean good){won=good;complete=true;}
    private void hit(){score++;if(score>=goal())finish(true);}
    private void miss(){errors++;if(errors>=3)finish(false);}
    private void shuffle(int[] arr,int size){for(int i=size-1;i>0;i--){int j=random.nextInt(i+1),t=arr[i];arr[i]=arr[j];arr[j]=t;}}
    private void nextFruit(long now){target=random.nextInt(3);period=1450+random.nextInt(1000);nextAt=now;deadline=now+period;}
    private void nextBeat(long now){period=600+random.nextInt(650);nextAt=now+period;deadline=nextAt+260;}
    private void nextJump(long now){period=1400+random.nextInt(1000);nextAt=now;deadline=now+period;jumped=false;}
    private void maze(){
        Arrays.fill(cells,1);int[] stack=new int[9];boolean[] seen=new boolean[9];int pos=0,depth=0;
        seen[0]=true;stack[0]=0;cells[1*7+1]=0;
        while(depth>=0){
            int x=pos%3,y=pos/3,n=0;int[] options=new int[4];
            if(x>0&&!seen[pos-1])options[n++]=pos-1;
            if(x<2&&!seen[pos+1])options[n++]=pos+1;
            if(y>0&&!seen[pos-3])options[n++]=pos-3;
            if(y<2&&!seen[pos+3])options[n++]=pos+3;
            if(n==0){depth--;if(depth>=0)pos=stack[depth];continue;}
            int to=options[random.nextInt(n)];int nx=to%3,ny=to/3;
            cells[(1+y*2)*7+(1+x*2)]=0;
            cells[(1+y*2+ny*2+1)/2*7+(1+x*2+nx*2+1)/2]=0;
            cells[(1+ny*2)*7+(1+nx*2)]=0;
            seen[to]=true;pos=to;stack[++depth]=pos;
        }
        // Optional extra corridors so maze is not a pure long winding route.
        for(int i=0;i<2;i++){int idx=1+random.nextInt(5),idy=1+random.nextInt(5);if((idx+idy)%2==1)cells[idy*7+idx]=0;}
    }
    private void pattern(){
        Arrays.fill(cells,0);Arrays.fill(aux,0);int mode=random.nextInt(4);
        // 5x5 vertically symmetric stamp with genuinely random target pixels.
        for(int y=0;y<5;y++)for(int x=0;x<3;x++){
            int bit=(random.nextInt(5)<(mode==0?3:mode==1?2:mode==2?1:4))?1:0;
            cells[y*5+x]=bit;cells[y*5+4-x]=bit;
        }
        cells[12]=1;
    }
    private void difference(){
        for(int i=0;i<25;i++)cells[i]=random.nextBoolean()?1:0;
        difference=random.nextInt(25);round++;
    }
    public void tick(long now){
        if(complete)return;
        if(type==0){while(now>deadline&&!complete){if(cursor==target)hit();else miss();if(!complete)nextFruit(deadline);}}
        if(type==1&&second>=0&&now>=revealUntil){selection=-1;second=-1;}
        if(type==4){while(now>deadline&&!complete){miss();if(!complete)nextBeat(deadline);}}
        if(type==7){while(now>deadline&&!complete){if(jumped)hit();else miss();if(!complete)nextJump(deadline);}}
    }
    public String message(long now){
        if(complete)return won?"ПОБЕДА! + РАДОСТЬ":"ПОПРОБУЙ ЕЩЁ";
        switch(type){
            case 0:return "ПЕРЕДВИГАЙ КОРЗИНУ";
            case 1:return "ИЩИ ДВЕ ОДИНАКОВЫЕ";
            case 2:return "ПАЛЬЦЕМ ВЕДИ ПО ДОРОГЕ";
            case 3:return "ПОВТОРИ КАРТИНКУ";
            case 4:return now>=nextAt&&now<=deadline?"БЕЙ!":"СЛУШАЙ РИТМ...";
            case 5:return "СЛЕДУЮЩЕЕ: "+(score+1);
            case 6:return "ТВОЙ ХОД: Х";
            case 7:return "НАЖМИ ПЕРЕД ПРЕГРАДОЙ";
            case 8:return "ОТЛИЧИЕ "+(score+1)+" / 4";
            default:return "ПЕРЕНЕСИ СИМВОЛ";
        }
    }
    /** Action 0 down,1 move,2 up. Called with scaled design coords. */
    public void touch(int action,float x,float y,long now){
        tick(now);if(complete)return;
        switch(type){
            case 0:
                if(y>345&&y<620){cursor=Math.max(0,Math.min(2,(int)(x/134)));dragX=(int)x;}
                break;
            case 1:
                if(action==2){int col=(int)((x-38)/80),row=(int)((y-280)/80);
                    if(col>=0&&col<4&&row>=0&&row<3)pair(row*4+col,now);}
                break;
            case 2:
                if(action==2){int cx=(int)((x-64)/39),cy=(int)((y-285)/39);
                    if(cx>=0&&cy>=0&&cx<7&&cy<7 && Math.abs(cx-playerX)+Math.abs(cy-playerY)==1)move(cx-playerX,cy-playerY);
                    else if(y>=568&&y<626){if(x<140)move(-1,0);else if(x>260)move(1,0);else move(0,1);}
                }break;
            case 3:
                if(action==2){int col=(int)((x-204)/30),row=(int)((y-350)/30);
                    if(col>=0&&col<5&&row>=0&&row<5){aux[row*5+col]^=1;boolean match=true;
                        for(int i=0;i<25;i++)if(aux[i]!=cells[i]){match=false;break;}if(match)finish(true);}}
                break;
            case 4:
                if(action==2&&y>330&&y<560){if(now>=nextAt-230&&now<=deadline){hit();if(!complete)nextBeat(now);}else{miss();if(!complete)nextBeat(now);}}
                break;
            case 5:
                if(action==2){int col=(int)((x-39)/80),row=(int)((y-286)/74);
                    if(col>=0&&col<4&&row>=0&&row<4){if(cells[row*4+col]==score+1)hit();else miss();}}
                break;
            case 6:
                if(action==2){int col=(int)((x-65)/90),row=(int)((y-299)/90);
                    if(col>=0&&col<3&&row>=0&&row<3)tic(row*3+col);}
                break;
            case 7:
                if(action==2&&y>260&&y<615){if(now>=deadline-780&&now<deadline && !jumped){jumped=true;jumpUntil=deadline+180;}else if(!jumped)miss();}
                break;
            case 8:
                if(action==2){int col=(int)((x-222)/27),row=(int)((y-350)/27);
                    if(col>=0&&col<5&&row>=0&&row<5){if(row*5+col==difference){hit();if(!complete)difference();}else miss();}}
                break;
            case 9:
                if(action==0){dragging=(x>148&&x<255&&y>317&&y<423);dragX=(int)x;}
                else if(action==1&&dragging)dragX=(int)x;
                else if(action==2){if(dragging&&y>470&&y<605){int bin=Math.min(2,Math.max(0,(int)(x/134)));
                    if(bins[bin]==sourceType){hit();sourceType=random.nextInt(3);}else miss();}dragging=false;}
                break;
        }
    }
    private void pair(int at,long now){
        if(second>=0||cells[at]==0||at==selection)return;
        if(selection<0){selection=at;return;}
        second=at;
        if(cells[selection]==cells[second]){cells[selection]=0;cells[second]=0;selection=-1;second=-1;hit();}
        else{revealUntil=now+800;miss();}
    }
    public void move(int dx,int dy){
        if(type!=2||complete)return;
        int x=playerX+dx,y=playerY+dy;
        if(x<0||y<0||x>=7||y>=7||cells[y*7+x]!=0)return;
        playerX=x;playerY=y;if(x==5&&y==5)finish(true);
    }
    private void tic(int at){
        if(cells[at]!=0)return;
        cells[at]=1;
        if(winLine(1)){finish(true);return;}
        if(boardFull()){round++;Arrays.fill(cells,0);return;}
        int move=-1;
        if(random.nextInt(4)!=0){for(int i=0;i<9;i++)if(cells[i]==0){cells[i]=2;boolean w=winLine(2);cells[i]=0;if(w){move=i;break;}}}
        if(move<0&&random.nextBoolean()){for(int i=0;i<9;i++)if(cells[i]==0){cells[i]=1;boolean w=winLine(1);cells[i]=0;if(w){move=i;break;}}}
        if(move<0){int empty=0;for(int i=0;i<9;i++)if(cells[i]==0)empty++;int nth=random.nextInt(empty);
            for(int i=0;i<9;i++)if(cells[i]==0){if(nth--==0){move=i;break;}}}
        cells[move]=2;
        if(winLine(2)){miss();Arrays.fill(cells,0);round++;}
        else if(boardFull()){Arrays.fill(cells,0);round++;}
    }
    private boolean boardFull(){for(int i=0;i<9;i++)if(cells[i]==0)return false;return true;}
    private boolean winLine(int mark){int[][] lines={{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
        for(int[] l:lines)if(cells[l[0]]==mark&&cells[l[1]]==mark&&cells[l[2]]==mark)return true;return false;}
}
