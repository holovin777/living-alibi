package com.holovin.npclife;

import java.util.List;

/** Player-owned residents cannot acquire a new partner through autonomous simulation.
 * Autonomous separation is limited to NPC-only partnerships.
 */
final class RelationshipBoundaryLife {
    private RelationshipBoundaryLife(){}
    static final int STRAIN_DAYS=7;
    static boolean controlled(TownLife.Town t,TownLife.Citizen c){
        return RebirthLife.linkedAdult(t,c)||FirstLifeChoice.accountOwned(t,c);
    }
    static boolean canAutoPair(TownLife.Town t,TownLife.Citizen a,TownLife.Citizen b){
        return !controlled(t,a)&&!controlled(t,b);
    }
    private static String key(TownLife.Citizen a,TownLife.Citizen b,String field){
        return "partnership:"+SocialNetwork.key(a.id,b.id)+":"+field;
    }
    static void tick(TownLife.Town t,WorldState s,List<TownLife.Citizen> residents){
        if(s.minute!=360)return;
        for(var a:residents){
            if(!a.alive)continue;
            var b=RebirthLife.partner(t,a);
            if(b==null||a.generationIndex>=b.generationIndex)continue;
            if(!canAutoPair(t,a,b)){
                t.industryStocks.put(key(a,b,"strain"),0);
                continue;
            }
            var rel=t.existingRelationship(a.id,b.id);
            String strain=key(a,b,"strain"),last=key(a,b,"last");
            if(t.industryStocks.getOrDefault(last,0)>=s.day)continue;
            int previous=t.industryStocks.getOrDefault(last,0);
            t.industryStocks.put(last,s.day);
            boolean serious=rel!=null&&rel.tension>=70&&(rel.trust<=30||rel.affinity<=25);
            if(!serious){t.industryStocks.put(strain,0);continue;}
            int days=(previous==s.day-1?t.industryStocks.getOrDefault(strain,0):0)+1;
            t.industryStocks.put(strain,days);
            if(days>=STRAIN_DAYS)separate(t,a,b,s.day,s.minute);
        }
    }
    static void separate(TownLife.Town t,TownLife.Citizen a,TownLife.Citizen b,int day,int minute){
        if(a==null||b==null||RebirthLife.partner(t,a)!=b)throw new IllegalArgumentException("No mutual partnership.");
        FamilyBudgetLife.releaseEstate(t,a);FamilyBudgetLife.releaseEstate(t,b);
        for(var c:List.of(a,b)){
            DemographyLife.set(t,c,"partner",0);DemographyLife.set(t,c,"union-day",0);
        }
        t.industryStocks.put(key(a,b,"strain"),0);
        // Keep parenting links, personal assets, and existing housing. A move requires its own decision.
        ArchiveLife.record(t,"separation:"+SocialNetwork.key(a.id,b.id)+":"+day+":"+minute,
                day,minute,"Population",a.name+" and "+b.name+" ended their partnership.");
    }
}
