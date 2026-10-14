import java.util.*;
import ru.pgws.pocketpet.*;
public class TestPets {
    public static void main(String[] args) {
        for(int stage=1;stage<=4;stage++) {
            Set<String> seen=new HashSet<>();
            for(int id=0;id<1000;id++) {
                String[] rows=PetSprites.create(id,stage,0);
                if(rows.length!=20)throw new AssertionError("row count");
                for(String r:rows)if(r.length()!=20)throw new AssertionError("wrong row length");
                if(!seen.add(String.join("",rows))) throw new AssertionError("Duplicate ID " + id + " at stage " + stage);
            }
            System.out.println("Stage "+stage+": "+seen.size()+" unique sprites OK");
        }
        PetGame g = new PetGame(); long now=System.currentTimeMillis();
        Set<Integer> randomIds=new HashSet<>();
        int previous=-1;
        for(int i=0;i<10000;i++){
            g.reset(now+i);
            if(g.genes==previous)throw new AssertionError("Consecutive repeated pet");
            if(g.genes<0||g.genes>=1000)throw new AssertionError("Out of range");
            randomIds.add(g.genes);previous=g.genes;
        }
        if(randomIds.size()<990)throw new AssertionError("poor variety: "+randomIds.size());
        System.out.println("New-egg RNG produces "+randomIds.size()+" / 1000 different IDs in 10000 eggs, no immediate repeats");
        g.reset(now);g.advance(now+6*PetGame.MINUTE);
        if(g.stage!=PetGame.BABY)throw new AssertionError("Hatching failed");
        System.out.println("Hatching OK, pet #"+g.genes);
    }
}
