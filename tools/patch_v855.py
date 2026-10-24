from pathlib import Path
R=Path('app/src/main/java/ru/pgws/pocketpet')
def patch(file,changes):
 p=R/file;s=p.read_text()
 for old,new in changes:
  if s.count(old)!=1:raise RuntimeError(f'{file}: expected one match: {old[:60]!r}, got {s.count(old)}')
  s=s.replace(old,new,1)
 p.write_text(s)
patch('PetGame.java',[
 ('born, lastTick, hatch, diedAt;','born, lastTick, hatch, diedAt, revivalProtectedUntil;'),
 ('missed, strain, ticks;','missed, strain, ticks, knowledge;'),
 ('diedAt=0; finalStage=ADULT;','diedAt=0; revivalProtectedUntil=0; finalStage=ADULT;'),
 ('strain=0; ticks=0; genes=','strain=0; ticks=0; knowledge=0; genes='),
 ('''            int life=(18 + Math.min(4,discipline) - missed/4 - sickness/2);
            if (days>Math.max(7,life) || sickness>=17 || missed>=45) {''',
 '''            boolean naturalEnd=days>naturalLifeDays();
            boolean criticalCare=lastTick-hatch >= (3L+knowledgeBonusDays())*DAY
                    && (sickness>=17 || missed>=45);
            if (lastTick>=revivalProtectedUntil && (naturalEnd || criticalCare)) {'''),
 ('    /** Capture a frozen portrait', '''    public int knowledgeBonusDays(){return Math.min(3,Math.max(0,knowledge)/30);}
    public int naturalLifeDays(){return Math.max(7,18+Math.min(4,discipline)-missed/4-sickness/2)+knowledgeBonusDays();}
    public boolean revive(long now){
        if(alive || stage!=DEAD)return false;
        alive=true;stage=Math.max(BABY,Math.min(ADULT,finalStage));diedAt=0;lastTick=now;
        hunger=Math.max(2,Math.min(4,finalHunger));happy=Math.max(2,Math.min(4,finalHappy));
        weight=Math.max(5,finalWeight);dirty=0;sickness=0;missed=0;strain=0;lights=true;
        revivalProtectedUntil=now+3*DAY;say("ПИТОМЕЦ ВЕРНУЛСЯ!",now);
        return true;
    }
    /** Capture a frozen portrait''')])
patch('PetStore.java',[
 ('import android.content.SharedPreferences;','import android.content.SharedPreferences;\nimport java.util.Map;\nimport java.util.Set;'),
 ('    private PetStore(){}','''    private PetStore(){}
    private static final String MARK="revive_once_20261024_v1";
    private static final String BACKUP="pocketpet96_pre_revive_20261024";'''),
 ('g.gamesWon=p.getInt("games_won",0);','''g.gamesWon=p.getInt("games_won",0);
        g.knowledge=Math.max(0,Math.min(100,p.getInt("knowledge",0)));
        g.revivalProtectedUntil=p.getLong("revival_protected_until",0);'''),
 ('.putInt("games_won",g.gamesWon).putLong("last_pat",g.lastPat)',
 '''.putInt("games_won",g.gamesWon).putInt("knowledge",g.knowledge)
                .putLong("revival_protected_until",g.revivalProtectedUntil)
                .putLong("last_pat",g.lastPat)'''),
 ('\n}', '''
    public static boolean restoreOnePetOnce(Context ctx,PetGame g,long now){
        SharedPreferences prefs=ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(prefs.getBoolean(MARK,false))return false;
        if(!prefs.contains("born") || g.alive || g.stage!=PetGame.DEAD){
            prefs.edit().putBoolean(MARK,true).commit();return false;
        }
        if(!backup(prefs,ctx.getSharedPreferences(BACKUP,Context.MODE_PRIVATE)))return false;
        if(!g.revive(now))return false;
        save(ctx,g);
        if(!prefs.getBoolean("alive",false))return false;
        prefs.edit().putBoolean(MARK,true).commit();return true;
    }
    @SuppressWarnings("unchecked")
    private static boolean backup(SharedPreferences from,SharedPreferences to){
        SharedPreferences.Editor e=to.edit().clear();
        for(Map.Entry<String,?> item:from.getAll().entrySet()){
            String k=item.getKey();Object v=item.getValue();
            if(v instanceof String)e.putString(k,(String)v);
            else if(v instanceof Integer)e.putInt(k,(Integer)v);
            else if(v instanceof Long)e.putLong(k,(Long)v);
            else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);
            else if(v instanceof Float)e.putFloat(k,(Float)v);
            else if(v instanceof Set)e.putStringSet(k,(Set<String>)v);
            else throw new IllegalStateException("Unknown save field: "+k);
        }
        return e.commit();
    }
}''')])
patch('MainActivity.java',[('game=PetStore.load(this);','game=PetStore.load(this);\n        PetStore.restoreOnePetOnce(this,game,GameClock.now(this));')])
patch('PetView.java',[('game.discipline=Math.min(4,game.discipline+1);',
 'game.discipline=Math.min(4,game.discipline+1);\n                        game.knowledge=Math.min(100,game.knowledge+10);')])
g=Path('app/build.gradle');s=g.read_text()
assert s.count('versionCode 14')==1 and s.count("versionName '8.5.4'")==1
s=s.replace('versionCode 14','versionCode 15').replace("versionName '8.5.4'","versionName '8.5.5-hotfix'")
g.write_text(s)
print('PocketPet lifetime fix applied; source v8.5.5, NOT v8.9.x')
