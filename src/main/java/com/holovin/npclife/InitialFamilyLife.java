package com.holovin.npclife;

import java.util.*;

/** Initial households predate day one. The newborns are ordinary residents;
 * only after generating them does the world attach a waiting first life.
 */
final class InitialFamilyLife {
    private InitialFamilyLife(){}
    static boolean start(TownLife.Town t,WorldState s){
        if(s.day!=1||s.minute<360||!RebirthLife.randomBirth(t)||RebirthLife.phase(t)!=RebirthLife.CHOICE
                ||t.player.incarnation!=1||!t.pastLives.isEmpty())return false;
        // Existing newborns have priority; a restart cannot create another
        // family if a first life has already been assigned.
        var existing=t.people.values().stream().filter(c->c.alive&&c.age==0
                &&DemographyLife.get(t,c,"born")==1
                &&DemographyLife.get(t,c,"played-incarnation")==0).toList();
        if(RebirthLife.acceptNaturalBirths(t,s,existing))return true;
        if(DemographyLife.get(t,DemographyLife.BIRTHS)>0)return false;
        var candidates=new ArrayList<>(t.people.values().stream().filter(c->DemographyLife.parentAge(c)&&c.money>=20
                &&DemographyLife.get(t,c,"partner")==0&&DemographyLife.get(t,c,"children")==0).toList());
        // New saves vary even though the base map and generated occupations are deterministic.
        Collections.shuffle(candidates,new Random());
        var newborns=new ArrayList<TownLife.Citizen>();
        int limit=Math.min(4,PopulationGenerator.MAX_GENERATED-(int)DemographyLife.living(t));
        for(int i=0;i+1<candidates.size()&&newborns.size()<limit;i+=2){
            var a=candidates.get(i);var b=candidates.get(i+1);
            if(!Collections.disjoint(DemographyLife.ancestors(t,a),DemographyLife.ancestors(t,b)))continue;
            String key=SocialNetwork.key(a.id,b.id);
            if(t.peerRelationships.containsKey(key)&&!DemographyLife.canPair(t,a,b))continue;
            var relation=t.peerRelationships.computeIfAbsent(key,k->{
                var r=new SocialLife.Relationship(a.id,b.id);
                r.affinity=80;r.trust=75;r.tension=10;r.conversations=4;r.encounters=6;
                return r;
            });
            if(!DemographyLife.pair(t,a,b,0))continue;
            RentalLife.moveHome(t,b,a.home,a.homeUnit);
            var child=DemographyLife.initialBirth(t,a,b);
            if(child!=null)newborns.add(child);
        }
        return RebirthLife.acceptNaturalBirths(t,s,newborns);
    }
}
