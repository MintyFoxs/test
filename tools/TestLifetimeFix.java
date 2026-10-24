import ru.pgws.pocketpet.*;
public class TestLifetimeFix {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args) {
        long t=1_800_000_000_000L;
        PetGame young=new PetGame();young.reset(t);
        young.advance(t+PetGame.DAY);
        check(young.alive,"One-day-old pet unexpectedly dead");
        for(int i=0;i<=100;i++){
            young.knowledge=i;
            check(young.knowledgeBonusDays()==Math.min(3,i/30),"Bad education threshold "+i);
        }
        young.knowledge=90;
        int bonusLife=young.naturalLifeDays();young.knowledge=0;
        check(bonusLife-young.naturalLifeDays()==3,"Study does not add 3 days");
        young.knowledge=90;
        young.sickness=20;
        young.advance(t+5*PetGame.DAY);
        check(young.alive,"Education bonus did not protect critical illness");
        young.advance(t+7*PetGame.DAY);
        check(!young.alive,"Severely uncared pet should not be immortal");
        int genes=young.genes, knowledge=young.knowledge, finalStage=young.finalStage;
        young.name="Original";
        check(young.revive(t+7*PetGame.DAY),"Couldn't revive dead pet");
        check(young.alive && young.stage==finalStage,"Last living stage was lost");
        check(young.genes==genes&&young.knowledge==knowledge&&young.name.equals("Original"),"Progress was lost");
        check(!young.revive(t+7*PetGame.DAY),"Revived twice");
        young.advance(t+9*PetGame.DAY);
        check(young.alive,"Grace for revived pet ignored");
        young.reset(t+10*PetGame.DAY);
        check(young.knowledge==0&&young.revivalProtectedUntil==0,"New egg inherited old state");
        System.out.println("PASS: lifespan bonus, grace, recovery, idempotence, preserved identity");
    }
}
