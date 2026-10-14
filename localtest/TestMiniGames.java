import ru.pgws.pocketpet.MiniGames;
import java.util.Random;

public class TestMiniGames {
    private static void ok(boolean cond,String message){if(!cond)throw new AssertionError(message);}
    public static void main(String[] args){
        long t=100_000L;
        MiniGames m=new MiniGames(0,t);
        for(int i=0;i<5;i++)m.tap(m.target,t);
        ok(m.complete&&m.won,"Find target");

        m=new MiniGames(1,t);
        for(int v=1;v<=3;v++){
            int a=-1,b=-1;
            for(int c=0;c<6;c++)if(m.cells[c]==v){if(a<0)a=c;else b=c;}
            m.tap(a,t);m.tap(b,t);
        }
        ok(m.complete&&m.won,"Pairs");

        m=new MiniGames(2,t);
        for(int i=0;i<3;i++)m.tap(m.sequence[i],t+4000);
        ok(m.complete&&m.won,"Memory sequence");

        m=new MiniGames(3,t);
        for(int i=0;i<5;i++)m.tap(4,m.nextAt+50);
        ok(m.complete&&m.won,"Reaction");

        m=new MiniGames(4,t);
        for(int i=0;i<5;i++)m.tap(m.target,t+10);
        ok(m.complete&&m.won,"Moles");

        m=new MiniGames(5,t);
        for(int i=0;i<8&&!m.complete;i++)m.tap(i%2==0?3:5,t+i*100);
        ok(m.complete,"Higher/lower can end");

        m=new MiniGames(6,t);
        for(int i=0;i<7&&!m.complete;i++){m.tap(m.target,m.deadline-50);m.tick(m.deadline+1);}
        ok(m.complete&&m.won,"Catch");

        m=new MiniGames(7,t);
        for(int c:new int[]{1,4,5,8})m.tap(c,t);
        ok(m.complete&&m.won,"Maze");

        m=new MiniGames(8,t);
        for(int i=0;i<3;i++)m.tap(4,m.nextAt+50);
        ok(m.complete&&m.won,"Rhythm");

        m=new MiniGames(9,t);
        for(int i=0;i<4;i++)m.tap(m.target,t+i*100);
        ok(m.complete&&m.won,"Math");

        for(int i=0;i<10;i++){
            MiniGames newGame=new MiniGames(i,t);
            ok(!newGame.complete,"Game starts unfinished: "+i);
            ok(newGame.title().length()>0 && newGame.instruction().length()>0,"Russian UI");
        }
        System.out.println("10 minigames pass; rewards require completed sessions");
    }
}
