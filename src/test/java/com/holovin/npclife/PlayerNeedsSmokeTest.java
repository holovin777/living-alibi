package com.holovin.npclife;

import java.util.ArrayList;

/** Player metabolism follows simulation time without creating NPC memories or missions. */
public final class PlayerNeedsSmokeTest {
    private PlayerNeedsSmokeTest() { }

    public static void main(String[] args) {
        WorldState clock=new WorldState(); // Day 1, 07:30.
        TownLife.Town town=TownLife.create(clock);
        var player=town.player;
        player.fatigue=40;
        player.privateName="Only I know";
        check(player.contacts.isEmpty(),"no invented contacts");

        var events=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(clock,60,s->TownLife.tick(town,s,events,observations));
        check(player.hunger==2,"hunger rises twice in one hour");
        check(player.thirst==3,"thirst rises three times in one hour");
        check(player.fatigue==42,"fatigue rises twice in one hour");
        check(player.health==100,"short wait does not damage health");
        check("Only I know".equals(player.privateName) && player.contacts.isEmpty(),
                "metabolism does not disclose identity");

        int recovered=PlayerLife.rest(player,60);
        check(recovered==10 && player.fatigue==32,"one hour break restores 10 fatigue");
        boolean refused=false;
        try {PlayerLife.rest(player,14);} catch(IllegalArgumentException ex) {refused=true;}
        check(refused && player.fatigue==32,"invalid rest is rejected without changing state");

        int travel=PlayerLife.startTravel(player,TownLife.Place.LIBRARY);
        refused=false;
        try {PlayerLife.rest(player,60);} catch(IllegalArgumentException ex) {refused=true;}
        check(refused,"cannot rest while traveling");
        LifeSimulation.advance(clock,travel,s->TownLife.tick(town,s,events,observations));
        check(player.place==TownLife.Place.LIBRARY && player.travelRemaining==0,
                "metabolism must not interfere with travel");

        var snapshot=town.copy();
        snapshot.player.hunger=100;
        snapshot.player.thirst=100;
        snapshot.player.fatigue=100;
        PlayerLife.tick(snapshot.player,clock.minute+1);
        check(snapshot.player.hunger==100 && snapshot.player.thirst==100 &&
                        snapshot.player.fatigue==100,"needs cannot exceed 100");
        check(player.hunger<100 && player.thirst<100 && player.fatigue<100,
                "town snapshots keep the original player state isolated");
        System.out.println("PlayerNeedsSmokeTest: ALL CHECKS PASSED");
    }

    private static void check(boolean condition,String label) {
        if(!condition)throw new AssertionError(label);
    }
}
