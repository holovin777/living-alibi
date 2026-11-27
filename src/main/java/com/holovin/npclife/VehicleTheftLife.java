package com.holovin.npclife;

import java.util.List;

/** Fictional civilian-car custody, separate from the player's legal property. */
final class VehicleTheftLife {
    private VehicleTheftLife(){}
    private static String key(TownLife.Town t,String field){return "combat:"+t.player.incarnation+":vehicle-theft:"+field;}
    static int get(TownLife.Town t,String field){return t.industryStocks.getOrDefault(key(t,field),0);}
    static void set(TownLife.Town t,String field,int value){t.industryStocks.put(key(t,field),Math.max(0,value));}
    static boolean active(TownLife.Town t){return get(t,"active")==1;}
    static void take(TownLife.Town t,WorldState s,List<TownLife.Event> events,List<TownLife.Observation> observations){
        TransportLife.ready(t.player);JusticeLife.requireFree(t);
        if(active(t)||RebirthLife.active(t))throw new IllegalArgumentException("A stolen vehicle is already in your custody.");
        set(t,"active",1);set(t,"fuel",6);set(t,"condition",75);
        set(t,"origin",t.player.place.ordinal()+1);
        boolean officer=PlayerLife.visible(t).stream().anyMatch(PoliceLife::officer);
        TownLife.publish(t,s,"visitor:"+t.player.incarnation,t.player.place,"A civilian vehicle was reported stolen.",events,observations);
        JusticeLife.reportTheft(t,s,officer,events,observations);
    }
    static void recover(TownLife.Town t){set(t,"active",0);set(t,"fuel",0);set(t,"condition",0);set(t,"origin",0);set(t,"stolen-remainder",0);}
}
