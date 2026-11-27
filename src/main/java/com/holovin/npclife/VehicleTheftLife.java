package com.holovin.npclife;

import java.util.List;

/** Fictional civilian-car custody, separate from the player's legal property. */
final class VehicleTheftLife {
    private VehicleTheftLife(){}
    private static String key(TownLife.Town t,String field){return "combat:"+t.player.incarnation+":vehicle-theft:"+field;}
    static int get(TownLife.Town t,String field){return t.industryStocks.getOrDefault(key(t,field),0);}
    static void set(TownLife.Town t,String field,int value){t.industryStocks.put(key(t,field),Math.max(0,value));}
    static boolean active(TownLife.Town t){return get(t,"active")==1;}
    static boolean owner(TownLife.Citizen c){
        return c.alive&&c.age>=18&&c.generationIndex>0&&c.generationIndex%3==0&&!PoliceLife.staff(c);
    }
    static boolean missing(TownLife.Town t,TownLife.Citizen c){
        return active(t)&&get(t,"owner")==c.generationIndex;
    }
    static void take(TownLife.Town t,WorldState s,List<TownLife.Event> events,List<TownLife.Observation> observations){
        take(t,s,null,events,observations);
    }
    static void take(TownLife.Town t,WorldState s,TownLife.Citizen owner,List<TownLife.Event> events,List<TownLife.Observation> observations){
        TransportLife.ready(t.player);JusticeLife.requireFree(t);
        if(active(t)||RebirthLife.active(t))throw new IllegalArgumentException("A stolen vehicle is already in your custody.");
        if(owner!=null&&(!(NpcTransportLife.enabled(t)?NpcTransportLife.owns(t,owner):owner(owner))||(NpcTransportLife.enabled(t)?NpcTransportLife.mode(t,owner)==NpcTransportLife.CAR||NpcTransportLife.parked(t,owner)!=t.player.place:owner.travelRemaining>0||owner.place!=t.player.place)))
            throw new IllegalArgumentException("That resident vehicle is not parked here.");
        set(t,"active",1);set(t,"fuel",owner!=null&&NpcTransportLife.enabled(t)&&NpcTransportLife.get(t,owner,"initialized")==1?NpcTransportLife.get(t,owner,"fuel"):6);
        set(t,"condition",owner!=null&&NpcTransportLife.enabled(t)&&NpcTransportLife.get(t,owner,"initialized")==1?NpcTransportLife.get(t,owner,"condition"):75);
        set(t,"owner",owner==null?0:owner.generationIndex);
        set(t,"origin",t.player.place.ordinal()+1);
        boolean officer=PlayerLife.visible(t).stream().anyMatch(PoliceLife::officer);
        TownLife.publish(t,s,"visitor:"+t.player.incarnation,t.player.place,"A civilian vehicle was reported stolen.",events,observations);
        if(owner!=null)events.add(new TownLife.Event(s.day,s.minute,owner.id,owner.place,owner.currentSpace(),"The resident vehicle was reported missing."));
        JusticeLife.reportTheft(t,s,officer,events,observations);
    }
    static void recover(TownLife.Town t){if(active(t)&&NpcTransportLife.enabled(t))NpcTransportLife.returned(t,get(t,"owner"),get(t,"fuel"),get(t,"condition"));set(t,"active",0);set(t,"fuel",0);set(t,"condition",0);set(t,"origin",0);set(t,"owner",0);set(t,"stolen-remainder",0);}
}
