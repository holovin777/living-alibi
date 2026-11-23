package com.holovin.npclife;

import java.util.ArrayList;

/** The ordinary NPC simulation continues while the player walks and lives without missions. */
public final class PlayerPresenceIntegrationSmokeTest {
    private PlayerPresenceIntegrationSmokeTest() { }
    public static void main(String[] args) {
        WorldState state=new WorldState();TownLife.Town town=TownLife.create(state);
        PopulationGenerator.add(town,12);
        PlayerLife.setPrivateName(town.player,"Visitor");
        int minutes=PlayerLife.startTravel(town.player,TownLife.Place.CAFE);
        var truth=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(state,minutes,s->TownLife.tick(town,s,truth,observations));
        check(town.player.place==TownLife.Place.CAFE && town.player.travelRemaining==0,"arrival in cafe");
        check(town.people.size()==14,"player does not replace or create an NPC");
        check(town.player.contacts.isEmpty(),"NPCs cannot infer player's name just from proximity");
        LifeSimulation.advance(state,7L*1440L,s->TownLife.tick(town,s,truth,observations));
        check(town.player.place==TownLife.Place.CAFE,"free exploration has no scripted missions or mandatory moves");
        check(town.player.contacts.isEmpty(),"world advancing does not auto-share identity");
        check("Visitor".equals(town.player.privateName),"private player identity survives world ticks");
        System.out.println("PlayerPresenceIntegrationSmokeTest: ALL CHECKS PASSED (14 residents, 7 days, no missions)");
    }
    private static void check(boolean ok,String label) {if(!ok)throw new AssertionError(label);}
}
