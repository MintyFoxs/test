package ru.pgws.pocketpet;

import android.content.Context;
import android.content.SharedPreferences;

/** Maintains compatibility with saves made by version 1/2/3. */
public final class PetStore {
    public static final String PREFS="pocketpet96_save_v1";
    private PetStore(){}
    public static PetGame load(Context ctx){
        PetGame g=new PetGame();
        SharedPreferences p=ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(!p.contains("born")){g.reset(System.currentTimeMillis());return g;}
        g.born=p.getLong("born",0);g.lastTick=p.getLong("last",0);
        g.hatch=p.getLong("hatch",g.born+5*PetGame.MINUTE);
        g.stage=p.getInt("stage",PetGame.EGG);g.hunger=p.getInt("hunger",4);
        g.happy=p.getInt("happy",4);g.discipline=p.getInt("discipline",0);
        g.weight=p.getInt("weight",5);g.dirty=p.getInt("dirty",0);
        g.sickness=p.getInt("sickness",0);g.missed=p.getInt("missed",0);
        g.strain=p.getInt("strain",0);g.ticks=p.getInt("ticks",0);
        g.genes=p.getInt("genes",0);g.personality=p.getInt("personality",0);
        g.gamesWon=p.getInt("games_won",0);
        g.lights=p.getBoolean("lights",true);g.alive=p.getBoolean("alive",true);
        g.name=p.getString("pet_name","");
        return g;
    }
    public static void save(Context ctx,PetGame g){
        ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
                .putLong("born",g.born).putLong("last",g.lastTick).putLong("hatch",g.hatch)
                .putInt("stage",g.stage).putInt("hunger",g.hunger).putInt("happy",g.happy)
                .putInt("discipline",g.discipline).putInt("weight",g.weight)
                .putInt("dirty",g.dirty).putInt("sickness",g.sickness).putInt("missed",g.missed)
                .putInt("strain",g.strain).putInt("ticks",g.ticks)
                .putInt("genes",g.genes).putInt("personality",g.personality)
                .putInt("games_won",g.gamesWon)
                .putBoolean("lights",g.lights).putBoolean("alive",g.alive)
                .putString("pet_name",g.name).commit();
    }
}
