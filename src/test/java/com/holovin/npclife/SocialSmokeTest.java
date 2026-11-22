package com.holovin.npclife;

import java.util.ArrayList;

/** Deterministic social tests: co-presence, isolation, relationship updates and long time jumps. */
public final class SocialSmokeTest {
    private SocialSmokeTest() { }

    public static void main(String[] args) {
        WorldState state=new WorldState();
        state.day=1;state.minute=1019;state.activity="EXPLORING_TOWN";
        TownLife.Town town=TownLife.create(state);
        for(var c:town.people.values()) {
            c.place=TownLife.Place.PARK;
            c.destination=TownLife.Place.PARK;
            c.travelRemaining=0;
        }
        town.together=true;
        town.meetingSinceTick=980;
        SocialLife.onEncounter(town);
        var truth=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();
        var interactions=new ArrayList<SocialLife.Interaction>();
        for(int minute=1020;minute<=1140;minute++) {
            state.minute=minute;
            SocialLife.tick(town,state,truth,observations,interactions);
        }
        check(interactions.size()==1,"a co-located meeting produces one bounded social conversation");
        check(town.relationship.conversations==1,"conversation counter increments");
        check(town.relationship.encounters==1,"encounter counter increments");
        check(truth.size()==1,"social event recorded as world truth");
        check(observations.stream().anyMatch(o->o.witnessId().equals("marco")),"Marco observes a local conversation");
        check(observations.stream().anyMatch(o->o.witnessId().equals("elena")),"Elena observes a local conversation");
        check(town.relationship.affinity>=0 && town.relationship.affinity<=100,"bounded affinity");
        check(town.relationship.trust>=0 && town.relationship.trust<=100,"bounded trust");
        check(town.relationship.tension>=0 && town.relationship.tension<=100,"bounded tension");
        var copied=town.copy();
        check(copied.relationship.conversations==1,"copy retains social progress");
        copied.relationship.affinity=0;
        check(town.relationship.affinity!=0,"copy has independent relationship state");
        var absentTown=town.copy();
        absentTown.get("elena").place=TownLife.Place.MARKET;
        absentTown.together=false;
        absentTown.meetingSinceTick=-1;
        int already=interactions.size();
        SocialLife.tick(absentTown,state,truth,observations,interactions);
        check(interactions.size()==already,"people in different places cannot converse");

        // Social simulation continues autonomously during time skipping.
        WorldState longRun=new WorldState();
        TownLife.Town livingTown=TownLife.create(longRun);
        var social=new ArrayList<SocialLife.Interaction>();
        var allTruth=new ArrayList<TownLife.Event>();
        var allKnown=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(longRun,14*1440L,
                s->TownLife.tick(livingTown,s,allTruth,allKnown,social));
        check(longRun.day==15,"calendar keeps advancing with social simulation");
        check(livingTown.relationship.conversations==social.size(),"all conversations are counted");
        check(social.stream().allMatch(e->e.firstId().equals("marco")&&e.secondId().equals("elena")),
                "social record names the actual participants");
        check(social.stream().allMatch(e->allTruth.stream().anyMatch(t->t.day()==e.day()&&
                t.minute()==e.minute()&&t.place()==e.place()&&t.text().equals(e.description()))),
                "every social event has corresponding world truth");
        System.out.println("SocialSmokeTest: ALL CHECKS PASSED ("+social.size()+" conversations in 14 days)");
    }
    private static void check(boolean success,String message) {
        if(!success)throw new AssertionError("Failed: "+message);
    }
}
