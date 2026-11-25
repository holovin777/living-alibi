package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Shared resource depletion survives SQLite restarts without inventing supplies. */
public final class NatureDatabaseSmokeTest {
    private NatureDatabaseSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path file=Files.createTempFile("living-alibi-nature-",".db");
        try {
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.naturalResources.get("river-water").stock==120,
                        "new world starts with bounded river supply");
                town.player.place=TownLife.Place.RIVER;
                town.player.destination=TownLife.Place.RIVER;
                town.player.thirst=80;
                NatureLife.drink(town,"river-water",state.day,state.minute);
                town.naturalResources.get("wild-berries").stock=3;
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.naturalResources.get("river-water").stock==119,
                        "river quantity persisted after restart");
                check(town.naturalResources.get("wild-berries").stock==3,
                        "wild food stock persisted after restart");
                check(town.player.thirst==40,"consumed water changed player hydration");
                check(town.player.place==TownLife.Place.RIVER,"natural location persisted");
                town.naturalResources.get("river-water").stock=0;
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var town=db.loadTown(db.load());
                check(town.naturalResources.get("river-water").stock==0,
                        "reopening does not magically restore depleted stocks");
                check(town.naturalResources.get("wild-berries").stock==3,
                        "repeated save preserves unrelated resources");
                check(town.player.contacts.isEmpty(),"resource data does not invent contacts");
            }
            System.out.println("NatureDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError(label);
    }
}
