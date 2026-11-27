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
        for(int j=0;j<candidates.size()&&newborns.size()<limit;j++){
            var a=candidates.get(j);
            if(DemographyLife.get(t,a,"partner")!=0||ResidentSexLife.sex(t,a)!=ResidentSexLife.MALE)continue;
            var b=candidates.stream().filter(x->ResidentSexLife.sex(t,x)==ResidentSexLife.FEMALE&&DemographyLife.get(t,x,"partner")==0
                &&Collections.disjoint(DemographyLife.ancestors(t,a),DemographyLife.ancestors(t,x))
                &&(!t.peerRelationships.containsKey(SocialNetwork.key(a.id,x.id))||DemographyLife.canPair(t,a,x))).findFirst().orElse(null);
            if(b==null)continue;
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
