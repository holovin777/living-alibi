package com.holovin.npclife;

import java.util.ArrayList;

/** Sleep is possible in any public scene, but exposure carries consequences. */
public final class PlayerSleepSmokeTest {
    private PlayerSleepSmokeTest() { }

    public static void main(String[] args) {
        WorldState state=new WorldState();
        TownLife.Town town=TownLife.create(state);
        var player=town.player;
        player.fatigue=95;
        player.health=95;

        var slept=PlayerLife.sleep(player,8); // The visitor begins in the park.
        check(slept.minutes()==480,"eight hours of simulated sleep");
        check(slept.fatigueRecovered()==88 && player.fatigue==7,
                "sleep recovers fatigue");
        check(slept.healthChange()==-8 && player.health==87,
                "exposed park sleep costs health");
        var truth=new ArrayList<TownLife.Event>();
        var observed=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(state,slept.minutes(),
                s->TownLife.tick(town,s,truth,observed));
        check(state.minute==930,"whole world advances while player sleeps");
        check(player.contacts.isEmpty(),"sleep does not reveal player identity");

        player.place=TownLife.Place.CAFE;
        player.destination=TownLife.Place.CAFE;
        player.fatigue=60;
        var sheltered=PlayerLife.sleep(player,8);
        check(sheltered.healthChange()==2 && player.health==89,
                "covered public area is less harmful and permits slow recovery");
        check(player.fatigue==0,"sleep cannot reduce fatigue below zero");

        player.place=TownLife.Place.RIVER;
        player.destination=TownLife.Place.RIVER;
        player.health=6;
        var exposed=PlayerLife.sleep(player,8);
        check(!exposed.survived() && player.health==0 && !player.alive,
                "repeated exposure can cause mortality");
        boolean refused=false;
        try {PlayerLife.startTravel(player,TownLife.Place.CLINIC);}
        catch(IllegalArgumentException ignored) {refused=true;}
        check(refused,"deceased visitor cannot travel");
        refused=false;
        try {PlayerLife.sleep(player,1);}
        catch(IllegalArgumentException ignored) {refused=true;}
        check(refused,"deceased visitor cannot sleep again");

        var snapshot=town.copy();
        check(!snapshot.player.alive,"mortality is included in copies");
        check(town.get("marco").alive,"visitor death does not kill NPCs");

        // Invalid sleep must never modify the character.
        var fresh=new PlayerLife.Player();
        refused=false;
        try {PlayerLife.sleep(fresh,0);}
        catch(IllegalArgumentException ignored) {refused=true;}
        check(refused && fresh.alive && fresh.health==100,
                "invalid sleep has no side effects");
        int travel=PlayerLife.startTravel(fresh,TownLife.Place.WOODS);
        check(travel==10,"travel from park");
        refused=false;
        try {PlayerLife.sleep(fresh,8);}
        catch(IllegalArgumentException ignored) {refused=true;}
        check(refused && fresh.travelRemaining==10,
                "sleep is rejected while traveling");

        System.out.println("PlayerSleepSmokeTest: ALL CHECKS PASSED");
    }

    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError(message);
    }
}
