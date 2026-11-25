package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;

/** Upgraded worlds keep player life status and never resurrect a deceased visitor. */
public final class PlayerSleepDatabaseSmokeTest {
    private PlayerSleepDatabaseSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path file=Files.createTempFile("living-alibi-sleep-",".db");
        try {
            try(var conn=DriverManager.getConnection("jdbc:sqlite:"+file.toAbsolutePath());
                var statement=conn.createStatement()) {
                // Simulate a player_state table from the earlier v1.16 draft.
                statement.execute("""
                        CREATE TABLE player_state (
                            id INTEGER PRIMARY KEY CHECK(id=1), place TEXT NOT NULL,
                            destination TEXT NOT NULL, travel_remaining INTEGER NOT NULL,
                            private_name TEXT, hunger INTEGER NOT NULL DEFAULT 0,
                            thirst INTEGER NOT NULL DEFAULT 0, fatigue INTEGER NOT NULL DEFAULT 0,
                            health INTEGER NOT NULL DEFAULT 100)
                        """);
                statement.execute("""
                        INSERT INTO player_state(id,place,destination,travel_remaining,
                                                 private_name,hunger,thirst,fatigue,health)
                        VALUES(1,'PARK','PARK',0,'Private Person',17,22,78,9)
                        """);
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.player.alive,"legacy players start alive");
                check(town.player.health==9 && town.player.fatigue==78,
                        "migration retains player needs");
                check("Private Person".equals(town.player.privateName),
                        "migration retains private identity");
                town.player.place=TownLife.Place.RIVER;
                town.player.destination=TownLife.Place.RIVER;
                var result=PlayerLife.sleep(town.player,8);
                check(!result.survived(),"unprotected river sleep can be fatal");
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(!town.player.alive && town.player.health==0,
                        "player death survives restart");
                check(town.player.place==TownLife.Place.RIVER,
                        "scene survives restart");
                check(town.player.contacts.isEmpty(),
                        "death does not disclose player identity");
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var town=db.loadTown(db.load());
                check(!town.player.alive,"subsequent loads do not resurrect visitor");
                check("Private Person".equals(town.player.privateName),
                        "private identity retained separately from NPC knowledge");
            }
            System.out.println("PlayerSleepDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {Files.deleteIfExists(file);}
    }

    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError(label);
    }
}
