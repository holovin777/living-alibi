package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** The same weather and resource effects survive a SQLite world restart. */
public final class WeatherDatabaseSmokeTest {
    private WeatherDatabaseSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path file=Files.createTempFile("living-alibi-weather-", ".db");
        try {
            int rainy=findRainySpringDay();
            try(var db=new WorldDatabase(file)) {
                var world=db.load();
                var town=db.loadTown(world);
                world.day=rainy-1;
                world.minute=1439;
                town.naturalResources.get("puddle-water").stock=0;
                var events=new ArrayList<TownLife.Event>();
                var seen=new ArrayList<TownLife.Observation>();
                LifeSimulation.advance(world,1,s->TownLife.tick(town,s,events,seen));
                check(world.day==rainy && world.minute==0,
                        "game clock advances across midnight");
                check(WeatherLife.at(world.day,world.minute).raining(),
                        "new day brings reproducible rain");
                check(town.naturalResources.get("puddle-water").stock==3,
                        "rain replenishes a depleted puddle");
                db.save(world,List.of(),List.of(),town,events,seen,List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var world=db.load();
                var town=db.loadTown(world);
                check(world.day==rainy && world.minute==0,
                        "world time is persisted");
                check(WeatherLife.at(world.day,world.minute).raining(),
                        "weather reconstructs from saved clock");
                check(town.naturalResources.get("puddle-water").stock==3,
                        "resource impact survives restart");
                check(town.player.incarnation==1 && town.player.contacts.isEmpty(),
                        "weather doesn't create identities or reset lives");
                db.save(world,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var town=db.loadTown(db.load());
                check(town.naturalResources.get("puddle-water").stock==3,
                        "reloading the same time does not duplicate rainfall");
            }
            System.out.println("WeatherDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {Files.deleteIfExists(file);}
    }

    private static int findRainySpringDay() {
        for(int day=2;day<=92;day++)
            if(WeatherLife.at(day,0).raining())return day;
        throw new AssertionError("Spring must have at least one rainy day.");
    }

    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError(label);
    }
}
