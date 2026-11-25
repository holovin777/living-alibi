package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** A first launch must create a playable world without any pre-existing save. */
public final class FreshWorldSmokeTest {
    private FreshWorldSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path directory=Files.createTempDirectory("living-alibi-fresh-");
        Path file=directory.resolve("new-game.db");
        try {
            check(!Files.exists(file),"no database before new game");
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(Files.isRegularFile(file),"world database created automatically");
                check(state.day==1 && state.minute==450,"world opens on day one");
                check(town.player.alive && town.player.incarnation==1,
                        "fresh visitor enters world alive");
                check(town.player.contacts.isEmpty() && town.player.privateName==null,
                        "no fabricated introductions");
                check(town.get("marco").alive && town.get("elena").alive,
                        "starter residents generated from scratch");
                check(!town.naturalResources.isEmpty(),"nature initialized without save");
                var changes=new ArrayList<TownLife.Event>();
                var observations=new ArrayList<TownLife.Observation>();
                LifeSimulation.advance(state,25,
                        s->TownLife.tick(town,s,changes,observations));
                db.save(state,List.of(),List.of(),town,changes,observations,List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(state.day==1 && state.minute==475,"first gameplay saved successfully");
                check(town.player.alive && town.player.incarnation==1,
                        "existing new game reopens without reset");
                check(town.get("marco").alive && town.get("elena").alive,
                        "starter residents remain in saved world");
            }
            System.out.println("FreshWorldSmokeTest: ALL CHECKS PASSED");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static void check(boolean condition,String name) {
        if(!condition)throw new AssertionError(name);
    }
}
