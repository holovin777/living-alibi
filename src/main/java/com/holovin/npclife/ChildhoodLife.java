package com.holovin.npclife;

import java.util.*;

/** A small daily shared recess, distinct from adult conversation and employment. */
final class ChildhoodLife {
    private ChildhoodLife(){}
    static final int RECESS=660;
    private static String key(TownLife.Citizen a,TownLife.Citizen b,String field){
        return "childhood:"+SocialNetwork.key(a.id,b.id)+":"+field;
    }
    static int playDays(TownLife.Town t,TownLife.Citizen a,TownLife.Citizen b){
        return t.industryStocks.getOrDefault(key(a,b,"days"),0);
    }
    static int firstDay(TownLife.Town t,TownLife.Citizen a,TownLife.Citizen b){
        return t.industryStocks.getOrDefault(key(a,b,"first"),0);
    }
    private static boolean eligible(TownLife.Town t,TownLife.Citizen c,int day){
        return c.alive&&DemographyLife.child(c)&&c.age>=6&&c.presentAt(TownLife.Place.SCHOOL)&&
            c.currentSpace().equals(TownLife.publicSpace(c.place))&&"STUDYING".equals(c.activity)&&
            !EmploymentLife.sick(c,day)&&c.hunger<75&&c.thirst<75&&c.fatigue<80&&
            t.industryStocks.getOrDefault("childhood:recess:"+c.id,0)<day;
    }
    private record Pair(TownLife.Citizen a,TownLife.Citizen b,int score){}
    static void tick(TownLife.Town t,WorldState clock){
        if(clock.minute!=RECESS||(clock.day-1)%7>=5)return;
        var pupils=t.people.values().stream().filter(c->eligible(t,c,clock.day))
            .sorted(Comparator.comparing(c->c.id)).toList();
        var pairs=new ArrayList<Pair>();
        for(int i=0;i<pupils.size();i++)for(int j=i+1;j<pupils.size();j++){
            var a=pupils.get(i);var b=pupils.get(j);
            if(Math.abs(a.age-b.age)>3)continue;
            var r=t.existingRelationship(a.id,b.id);
            if(r!=null&&(r.tension>=45||r.affinity<30))continue;
            int score=(r==null?50:r.affinity)+Math.min(a.sociability,b.sociability)/4;
            // Rotate close scores on different days without using wall-clock randomness.
            score+=Math.floorMod((int)(a.personalSeed^b.personalSeed^clock.day),12);
            pairs.add(new Pair(a,b,score));
        }
        pairs.sort(Comparator.comparingInt(Pair::score).reversed().thenComparing(p->p.a.id).thenComparing(p->p.b.id));
        Set<String> used=new HashSet<>();
        for(var pair:pairs){
            var a=pair.a;var b=pair.b;
            if(used.contains(a.id)||used.contains(b.id))continue;
            used.add(a.id);used.add(b.id);
            var r=t.peerRelationships.computeIfAbsent(SocialNetwork.key(a.id,b.id),k->new SocialLife.Relationship(a.id,b.id));
            if(r.encounters==0)r.encounters=1;
            r.affinity=Math.min(100,r.affinity+3);
            r.trust=Math.min(100,r.trust+2);
            r.tension=Math.max(0,r.tension-1);
            r.conversations++;
            r.lastInteractionTick=(long)(clock.day-1)*1440+clock.minute;
            r.lastAttemptDay=clock.day;
            t.industryStocks.put(key(a,b,"days"),Math.addExact(playDays(t,a,b),1));
            t.industryStocks.putIfAbsent(key(a,b,"first"),clock.day);
            for(var c:List.of(a,b)){
                t.industryStocks.put("childhood:recess:"+c.id,clock.day);
                EducationLife.learn(t,c,"social",1);
            }
        }
    }
}
