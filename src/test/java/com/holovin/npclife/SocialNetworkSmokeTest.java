package com.holovin.npclife;

import java.util.ArrayList;

/** Regression: relationships are local, bounded, per-pair and automatic. */
public final class SocialNetworkSmokeTest {
    private SocialNetworkSmokeTest() { }
    public static void main(String[] args) {
        WorldState s=new WorldState();
        TownLife.Town town=TownLife.create(s);
        var generated=PopulationGenerator.add(town,12);
        var a=generated.get(0);
        var b=generated.get(1);
        a.home=TownLife.Place.NORTH_RESIDENCES;
        b.home=TownLife.Place.NORTH_RESIDENCES;
        a.place=a.home;b.place=b.home;
        a.destination=a.place;b.destination=b.place;
        a.travelRemaining=0;b.travelRemaining=0;
        a.activity="FREE_TIME";b.activity="FREE_TIME";
        var truth=new ArrayList<TownLife.Event>();
        var seen=new ArrayList<TownLife.Observation>();
        var chat=new ArrayList<SocialLife.Interaction>();
        // Being in neighboring apartments must not count as meeting someone.
        for(int m=990;m<=1080;m++){
            s.minute=m;
            SocialNetwork.tick(town,s,truth,seen,chat);
        }
        check(town.existingRelationship(a.id,b.id)==null,"private homes are distinct scenes");
        a.place=TownLife.Place.PARK;b.place=TownLife.Place.PARK;
        a.destination=a.place;b.destination=b.place;
        for(int m=1081;m<=1199;m++) {
            s.minute=m;
            SocialNetwork.tick(town,s,truth,seen,chat);
            if(m==1081)town.existingRelationship(a.id,b.id).rng=0; // force a positive roll
        }
        var r=town.existingRelationship(a.id,b.id);
        check(r!=null && r.encounters==1,"public co-presence creates one encounter");
        check(r.conversations==1,"at most one conversation per encounter/day");
        check(chat.stream().anyMatch(c->c.firstId().equals(r.firstId)&&c.secondId().equals(r.secondId)),
                "conversation stores canonical pair IDs");
        check(seen.stream().anyMatch(o->o.witnessId().equals(a.id)),"participant witnesses their own talk");
        check(seen.stream().anyMatch(o->o.witnessId().equals(b.id)),"other participant witnesses the talk");
        var copy=town.copy();
        copy.existingRelationship(a.id,b.id).trust=4;
        check(town.existingRelationship(a.id,b.id).trust!=4,"copies isolate relationships");
        workplaceConversation();
        // Full 14-day simulation: the generated population makes new connections.
        WorldState longRun=new WorldState();
        TownLife.Town livingTown=TownLife.create(longRun);
        PopulationGenerator.add(livingTown,12);
        var interactions=new ArrayList<SocialLife.Interaction>();
        LifeSimulation.advance(longRun,14L*1440L,
                state->TownLife.tick(livingTown,state,new ArrayList<>(),new ArrayList<>(),interactions));
        check(!livingTown.peerRelationships.isEmpty(),"non-legacy social connections form");
        check(interactions.stream().anyMatch(c->c.firstId().startsWith("resident-")||
                        c.secondId().startsWith("resident-")),"generated citizens talk autonomously");
        int maximum=livingTown.peerRelationships.values().stream().mapToInt(rel->rel.conversations).max().orElse(0);
        check(livingTown.peerRelationships.values().stream().allMatch(rel->
                rel.conversations<=15 && rel.conversations>=0),"conversations are bounded per day");
        check(livingTown.peerRelationships.values().stream().allMatch(rel->
                rel.affinity>=0&&rel.affinity<=100&&rel.trust>=0&&rel.trust<=100),
                "social attributes remain bounded");
        System.out.println("SocialNetworkSmokeTest: ALL CHECKS PASSED ("+
                livingTown.peerRelationships.size()+" new pairs, "+interactions.size()+" conversations)");
    }
    private static void workplaceConversation() {
        var town=new TownLife.Town();
        var a=new TownLife.Citizen("work-a","Ada","Programmer",TownLife.Place.OFFICE);
        var b=new TownLife.Citizen("work-b","Bruno","Programmer",TownLife.Place.OFFICE);
        a.home=TownLife.Place.NORTH_RESIDENCES;b.home=a.home;
        a.activity="WORKING";b.activity="WORKING";
        town.people.put(a.id,a);town.people.put(b.id,b);
        check(SocialNetwork.coworkersOnShift(a,b,1),"same role on shift");
        b.sickUntilDay=2;
        check(!SocialNetwork.coworkersOnShift(a,b,1),"sick worker excluded");
        b.sickUntilDay=0;b.activity="FREE_TIME";
        check(!SocialNetwork.coworkersOnShift(a,b,1),"off duty excluded");
        b.activity="WORKING";b.travelRemaining=3;
        check(!SocialNetwork.coworkersOnShift(a,b,1),"travelling excluded");
        b.travelRemaining=0;b.occupation="Teacher";
        check(!SocialNetwork.coworkersOnShift(a,b,1),"other role excluded");
        b.occupation="Programmer";
        var s=new WorldState();s.day=1;
        var truth=new ArrayList<TownLife.Event>();
        var seen=new ArrayList<TownLife.Observation>();
        var chat=new ArrayList<SocialLife.Interaction>();
        for(int minute=600;minute<=660;minute++){
            s.minute=minute;
            SocialNetwork.tick(town,s,truth,seen,chat);
            if(minute==600)town.existingRelationship(a.id,b.id).rng=0;
        }
        check(chat.size()==1,"workplace talk respects existing daily limit");
        check(chat.get(0).description().contains("ticket board and code review"),"work talk fits occupation");
        check(town.existingRelationship(a.id,b.id).trust>45,"work talk builds persistent trust");
        check(seen.stream().anyMatch(o->o.witnessId().equals(b.id)),"coworker witnesses talk");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
