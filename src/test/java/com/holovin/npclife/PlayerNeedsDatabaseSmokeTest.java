package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;

/** Player needs survive saves, restarts, and migration from a v1.15 player table. */
public final class PlayerNeedsDatabaseSmokeTest {
    private PlayerNeedsDatabaseSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path file = Files.createTempFile("living-alibi-player-needs-", ".db");
        try {
            // Simulate a database created before the four player needs existed.
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
                 var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE player_state (
                            id INTEGER PRIMARY KEY CHECK(id=1),
                            place TEXT NOT NULL,
                            destination TEXT NOT NULL,
                            travel_remaining INTEGER NOT NULL,
                            private_name TEXT)
                        """);
                statement.execute("""
                        INSERT INTO player_state(id,place,destination,travel_remaining,private_name)
                        VALUES(1,'CAFE','CAFE',0,'Existing Visitor')
                        """);
            }

            try (var db = new WorldDatabase(file)) {
                var state = db.load();
                var town = db.loadTown(state);
                check("Existing Visitor".equals(town.player.privateName),
                        "migration retains existing visitor identity");
                check(town.player.place == TownLife.Place.CAFE,
                        "migration retains visitor location");
                checkNeeds(town.player, 0, 0, 0, 100, "migration defaults");

                town.player.hunger = 17;
                town.player.thirst = 28;
                town.player.fatigue = 39;
                town.player.health = 91;
                db.save(state, List.of(), List.of(), town, List.of(), List.of(), List.of());
            }

            try (var db = new WorldDatabase(file)) {
                var state = db.load();
                var town = db.loadTown(state);
                checkNeeds(town.player, 17, 28, 39, 91, "saved condition");
                check("Existing Visitor".equals(town.player.privateName),
                        "save preserves existing identity");

                town.player.hunger = 0;
                town.player.thirst = 100;
                town.player.fatigue = 100;
                town.player.health = 50;
                db.save(state, List.of(), List.of(), town, List.of(), List.of(), List.of());
            }

            try (var db = new WorldDatabase(file)) {
                var town = db.loadTown(db.load());
                checkNeeds(town.player, 0, 100, 100, 50, "updated condition");
                check("Existing Visitor".equals(town.player.privateName),
                        "repeated save preserves existing identity");
            }
            System.out.println("PlayerNeedsDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void checkNeeds(PlayerLife.Player player, int hunger, int thirst,
                                   int fatigue, int health, String label) {
        check(player.hunger == hunger && player.thirst == thirst
                && player.fatigue == fatigue && player.health == health, label);
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
