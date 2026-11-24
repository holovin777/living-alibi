package com.holovin.npclife;

import java.util.ArrayList;

/** A successor is a new person; the town, NPCs and world clock remain continuous. */
public final class PlayerRebirthSmokeTest {
    private PlayerRebirthSmokeTest() { }

    public static void main(String[] args) {
        var state=new WorldState();
        var town=TownLife.create(state);
        var marco=town.get("marco");
        marco.place=TownLife.Place.PARK;
        marco.travelRemaining=0;
        marco.activity="WORKING";
        PlayerLife.setPrivateName(town.player,"Private First");
        PlayerLife.introduce(town,"marco","Known First",state.day,state.minute);
        check(town.player.incarnation==1,"first visitor begins as generation 1");

        int resourceStock=town.naturalResources.get("river-water").stock;
        town.player.place=TownLife.Place.RIVER;
        town.player.destination=TownLife.Place.RIVER;
        town.player.health=5;
        check(!PlayerLife.sleep(town.player,8).survived(),"river exposure kills first visitor");
        var record=PlayerLife.beginNewLife(town,state.day,state.minute);
        check(record.incarnation()==1 && "Private First".equals(record.privateName()),
                "first life archived privately");
        check(town.player.incarnation==2 && town.player.alive && town.player.health==100,
                "new visitor has an independent healthy body");
        check(town.player.privateName==null && town.player.contacts.isEmpty(),
                "new visitor receives neither old name nor memories");
        check(town.get("marco")==marco && marco.alive,
                "existing NPC remains the same resident");
        check(town.naturalResources.get("river-water").stock==resourceStock,
                "new life does not replenish nature");
        check(town.pastLives.size()==1,"only one death archived");

        boolean rejected=false;
        try {PlayerLife.beginNewLife(town,state.day,state.minute);}
        catch(IllegalArgumentException expected) {rejected=true;}
        check(rejected && town.player.incarnation==2,
                "cannot replace a living visitor");

        town.player.health=1;
        town.player.place=TownLife.Place.RIVER;
        town.player.destination=TownLife.Place.RIVER;
        check(!PlayerLife.sleep(town.player,8).survived(),"second visitor can die independently");
        PlayerLife.beginNewLife(town,state.day,state.minute);
        check(town.player.incarnation==3 && town.pastLives.size()==2,
                "multiple deaths create successive lives without erasing history");

        var clone=town.copy();
        clone.player.incarnation=10;
        clone.pastLives.clear();
        check(town.player.incarnation==3 && town.pastLives.size()==2,
                "copied world cannot mutate the real visitor history");

        var truth=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(state,30,s->TownLife.tick(town,s,truth,observations));
        check(state.minute==480 && town.player.incarnation==3,
                "town clock continues after successor appears");
        System.out.println("PlayerRebirthSmokeTest: ALL CHECKS PASSED");
    }

    private static void check(boolean ok,String what) {
        if(!ok)throw new AssertionError(what);
    }
}
