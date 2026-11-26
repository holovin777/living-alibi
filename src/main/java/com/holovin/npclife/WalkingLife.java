package com.holovin.npclife;

import java.util.*;

/** Read-only social outing view. Never overrides work, travel, or essential needs. */
final class WalkingLife {
    private WalkingLife(){}
    private static final Set<TownLife.Place> PLACES=Set.of(TownLife.Place.PARK,TownLife.Place.CENTRO,TownLife.Place.PORTICI,TownLife.Place.RIVER);
    private static final Set<String> ACTIVITIES=Set.of("FREE_TIME","WALKING","WANDERING","STROLLING","RELAXING","RETIRED");
    private record Pair(TownLife.Citizen a,TownLife.Citizen b,int score){}
    static Map<String,String> companions(TownLife.Town town,List<TownLife.Citizen> visible,int minute){
        if(minute<480||minute>=1200)return Map.of();
        var candidates=visible.stream().filter(c->eligible(town,c)).sorted(Comparator.comparing(c->c.id)).toList();
        var pairs=new ArrayList<Pair>();
        for(int i=0;i<candidates.size();i++)for(int j=i+1;j<candidates.size();j++){
            var a=candidates.get(i);var b=candidates.get(j);
            if(a.place!=b.place||!a.currentSpace().equals(b.currentSpace()))continue;
            var r=town.existingRelationship(a.id,b.id);
            if(r==null||r.conversations<2||r.trust<55||r.affinity<35||r.tension>25)continue;
            pairs.add(new Pair(a,b,r.affinity+r.trust-r.tension));
        }
        pairs.sort(Comparator.comparingInt(Pair::score).reversed().thenComparing(p->p.a.id).thenComparing(p->p.b.id));
        Map<String,String> result=new LinkedHashMap<>();
        for(var p:pairs){
            if(result.containsKey(p.a.id)||result.containsKey(p.b.id))continue;
            result.put(p.a.id,p.b.id);result.put(p.b.id,p.a.id);
        }
        return Map.copyOf(result);
    }
    private static boolean eligible(TownLife.Town town,TownLife.Citizen c){
        return c.alive&&c.travelRemaining==0&&c.age>=18&&c.sociability>=40&&
            !RebirthLife.linkedAdult(town,c)&&PLACES.contains(c.place)&&
            c.currentSpace().equals(TownLife.publicSpace(c.place))&&ACTIVITIES.contains(c.activity)&&
            c.hunger<70&&c.thirst<70&&c.fatigue<80;
    }
}
