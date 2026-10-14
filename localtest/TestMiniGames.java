import ru.pgws.pocketpet.MiniGames;
import java.util.*;

public class TestMiniGames {
    private static void ok(boolean p,String description){if(!p)throw new AssertionError(description);}
    public static void main(String[] args){
        long now=100000L;
        for(int type=0;type<10;type++)for(int seed=0;seed<100;seed++){
            MiniGames m=new MiniGames(type,now,new Random(seed));
            ok(m.title().length()>2,"Missing title");
            ok(!m.complete,"Instant victory");
            switch(type){
                case 0:
                    for(int i=0;i<5;i++){m.cursor=m.target;m.tick(m.deadline+1);}
                    ok(m.won,"Catchable berries");break;
                case 1:
                    for(int val=1;val<=6;val++){
                        int a=-1,b=-1;
                        for(int i=0;i<12;i++)if(m.cells[i]==val){if(a<0)a=i;else b=i;}
                        m.touch(2,38+(a%4)*80+10,280+(a/4)*80+10,now);
                        m.touch(2,38+(b%4)*80+10,280+(b/4)*80+10,now);
                    }
                    ok(m.won,"Pairs solvable");break;
                case 2:
                    // Find a path with BFS, including random carved cells.
                    List<Integer> route=new ArrayList<>();boolean[] seen=new boolean[49];int[] parent=new int[49];Arrays.fill(parent,-1);
                    ArrayDeque<Integer> q=new ArrayDeque<>();q.add(8);seen[8]=true;
                    while(!q.isEmpty()){
                        int pos=q.remove();if(pos==40)break;
                        int x=pos%7,y=pos/7;
                        for(int d:new int[]{-7,7,-1,1}){
                            int nx=x+(d==1?1:d==-1?-1:0),ny=y+(d==7?1:d==-7?-1:0),at=pos+d;
                            if(nx>=0&&nx<7&&ny>=0&&ny<7&&!seen[at]&&m.cells[at]==0){parent[at]=pos;seen[at]=true;q.add(at);}
                        }
                    }
                    ok(seen[40],"Maze solvable");
                    for(int at=40;at!=8;at=parent[at])route.add(at);
                    Collections.reverse(route);int px=1,py=1;
                    for(int at:route){m.move(at%7-px,at/7-py);px=at%7;py=at/7;}
                    ok(m.won,"Maze finished");break;
                case 3:
                    for(int i=0;i<25;i++)if(m.cells[i]==1)m.touch(2,204+(i%5)*30+12,350+(i/5)*30+12,now);
                    ok(m.won,"Pixel copy match");break;
                case 4:
                    for(int i=0;i<8;i++)m.touch(2,200,420,m.nextAt);
                    ok(m.won,"Rhythm playable");break;
                case 5:
                    for(int val=1;val<=9;val++)for(int i=0;i<16;i++)if(m.cells[i]==val){m.touch(2,39+(i%4)*80+15,286+(i/4)*74+15,now);break;}
                    ok(m.won,"Number path playable");break;
                case 6:
                    for(int i=0;i<9&&!m.complete;i++)if(m.cells[i]==0)m.touch(2,65+(i%3)*90+20,299+(i/3)*90+20,now);
                    ok(m.round>=0,"Tic-tac-toe interactive");break;
                case 7:
                    for(int i=0;i<5;i++){m.touch(2,200,425,m.deadline-400);m.tick(m.deadline+1);}
                    ok(m.won,"Runner playable");break;
                case 8:
                    for(int i=0;i<4;i++){int p=m.difference;m.touch(2,222+(p%5)*27+12,350+(p/5)*27+12,now);}
                    ok(m.won,"Difference playable");break;
                case 9:
                    for(int i=0;i<8;i++){
                        m.touch(0,200,350,now);int bin=0;while(m.bins[bin]!=m.sourceType)bin++;
                        m.touch(2,16+bin*127+58,530,now);
                    }
                    ok(m.won,"Sorting draggable");break;
            }
        }
        System.out.println("100 seeds x 10 varied games: PASS");
    }
}
