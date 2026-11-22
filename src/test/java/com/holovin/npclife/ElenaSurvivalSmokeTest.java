package com.holovin.npclife;

import java.util.ArrayList;

/** Regression: weekend supply shortages must not kill a solvent, otherwise healthy NPC. */
public final class ElenaSurvivalSmokeTest {
    private ElenaSurvivalSmokeTest() { }
    public static void main(String[] args) {
        WorldState marco=new WorldState();
        TownLife.Town town=TownLife.create(marco);
        var truth=new ArrayList<TownLife.Event>();
        var witnessed=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(marco,35L*1440L,
                s->TownLife.tick(town,s,truth,witnessed));
        var elena=town.get("elena");
        check(elena.alive, "Elena stays alive with steady employment and grocery access");
        check(elena.health>=70, "Health is stable across weekends");
        check(truth.stream().anyMatch(e->
                e.actorId().equals("elena") && (e.day()-1)%7>=5 &&
                        e.text().contains("purchased")),
                "Elena shops for essential supplies on weekends");
        var deceased=town.copy();
        deceased.get("elena").alive=false;
        deceased.get("elena").health=0;
        LifeSimulation.advance(marco,120,
                s->TownLife.tick(deceased,s,new ArrayList<>(),new ArrayList<>()));
        check(!deceased.get("elena").alive,
                "Deceased NPCs are not automatically revived by the shopping fix");
        System.out.println("ElenaSurvivalSmokeTest: ALL CHECKS PASSED");
    }
    private static void check(boolean success,String message) {
        if(!success)throw new AssertionError("Failed: "+message);
    }
}
