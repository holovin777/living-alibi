package com.holovin.npclife;

import java.util.ArrayList;

/** Natural resources are shared finite stocks, with location and renewal rules. */
public final class NatureSmokeTest {
    private NatureSmokeTest() { }

    public static void main(String[] args) {
        WorldState state=new WorldState();
        TownLife.Town town=TownLife.create(state);
        var water=town.naturalResources.get("river-water");
        var puddle=town.naturalResources.get("puddle-water");
        var berries=town.naturalResources.get("wild-berries");
        check(water.stock==120 && puddle.stock==5 && berries.stock==16,
                "finite natural sources generated");
        check(NatureLife.available(town).size()==1 &&
                NatureLife.available(town).containsKey("puddle-water"),
                "only local resources are accessible");

        boolean refused=false;
        try {NatureLife.drink(town,"river-water",state.day,state.minute);}
        catch(IllegalArgumentException ex) {refused=true;}
        check(refused && water.stock==120,"remote drinking does not create water");

        var events=new ArrayList<TownLife.Event>();
        var obs=new ArrayList<TownLife.Observation>();
        int duration=PlayerLife.startTravel(town.player,TownLife.Place.RIVER);
        LifeSimulation.advance(state,duration,s->TownLife.tick(town,s,events,obs));
        check(town.player.place==TownLife.Place.RIVER,"visitor reaches the river");
        town.player.thirst=90;
        int oldHealth=town.player.health;
        NatureLife.drink(town,"river-water",state.day,state.minute);
        check(water.stock==119 && town.player.thirst==50,
                "drinking consumes one unit and lowers thirst");
        check(town.player.health<=oldHealth && town.player.health>=oldHealth-4,
                "river health effects are bounded");

        TownLife.Town snapshot=town.copy();
        snapshot.naturalResources.get("river-water").stock=0;
        check(water.stock==119,"town copies do not share physical stocks");

        town.player.place=TownLife.Place.WOODS;
        town.player.destination=TownLife.Place.WOODS;
        town.player.travelRemaining=0;
        town.player.hunger=60;
        NatureLife.forage(town,"wild-berries");
        check(town.player.hunger==35 && berries.stock==15,
                "foraging consumes finite local food");

        town.player.place=TownLife.Place.PARK;
        town.player.destination=TownLife.Place.PARK;
        for(int i=0;i<5;i++)NatureLife.drink(town,"puddle-water",state.day,state.minute+i);
        refused=false;
        try {NatureLife.drink(town,"puddle-water",state.day,state.minute+5);}
        catch(IllegalArgumentException ex) {refused=true;}
        check(refused && puddle.stock==0,"puddle cannot be consumed forever");

        NatureLife.tick(town,1);
        check(water.stock==119 && puddle.stock==0 && berries.stock==15,
                "no regeneration between midnights");
        NatureLife.tick(town,0);
        check(water.stock==120 && puddle.stock==0 && berries.stock==16,
                "flowing water and berries recover within capacity; puddle stays dry");
        check(town.player.contacts.isEmpty(),"resource use never discloses identity");
        System.out.println("NatureSmokeTest: ALL CHECKS PASSED");
    }

    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError(label);
    }
}
