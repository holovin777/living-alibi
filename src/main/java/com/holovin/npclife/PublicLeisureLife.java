package com.holovin.npclife;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Read-only presentation of existing social simulation; no invented conversations. */
final class PublicLeisureLife {
    private PublicLeisureLife(){}
    static boolean leisure(TownLife.Citizen c){
        return c.alive&&c.travelRemaining==0&&(c.place==TownLife.Place.PARK||c.place==TownLife.Place.CENTRO)
            &&List.of("FREE_TIME","RETIRED","WALKING","WANDERING","STROLLING").contains(c.activity);
    }
    static boolean wantsRest(TownLife.Citizen c){
        return leisure(c)&&c.age>=6&&c.fatigue>=45;
    }
    static Map<String,String> recentPartners(TownLife.Town t,WorldState clock,List<TownLife.Citizen> visible){
        long now=(long)(clock.day-1)*1440L+clock.minute;
        Map<String,String> result=new LinkedHashMap<>();
        for(int i=0;i<visible.size();i++)for(int j=i+1;j<visible.size();j++){
            var a=visible.get(i);var b=visible.get(j);
            if(!leisure(a)||!leisure(b)||!a.currentSpace().equals(b.currentSpace())||
                result.containsKey(a.id)||result.containsKey(b.id))continue;
            var relation=t.peerRelationships.get(SocialNetwork.key(a.id,b.id));
            if(relation==null||relation.lastInteractionTick<0||relation.lastInteractionTick>now||
                now-relation.lastInteractionTick>=8||relation.tension>35)continue;
            result.put(a.id,b.id);result.put(b.id,a.id);
        }
        return result;
    }
}
