package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

/** A v1.3 schema and prior logs upgrade without losing data; new spaces persist. */
public final class PrivateLivesDatabaseSmokeTest {
    private PrivateLivesDatabaseSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path file=Files.createTempFile("living-alibi-private-lives-",".db");
        try {
            // Simulate the relevant v1.3 tables before the new columns existed.
            try(var conn=DriverManager.getConnection("jdbc:sqlite:"+file.toAbsolutePath());
                var statement=conn.createStatement()) {
                statement.execute("""
                    CREATE TABLE town_people (
                        id TEXT PRIMARY KEY, place TEXT NOT NULL, destination TEXT NOT NULL,
                        travel_remaining INTEGER NOT NULL, activity TEXT NOT NULL,
                        hunger INTEGER NOT NULL, thirst INTEGER NOT NULL, fatigue INTEGER NOT NULL,
                        health INTEGER NOT NULL, money INTEGER NOT NULL, food INTEGER NOT NULL,
                        water INTEGER NOT NULL, age INTEGER NOT NULL, alive INTEGER NOT NULL)
                    """);
                statement.execute("""
                    CREATE TABLE town_events (id INTEGER PRIMARY KEY AUTOINCREMENT,
                        day INTEGER NOT NULL, minute INTEGER NOT NULL, actor_id TEXT NOT NULL,
                        place TEXT NOT NULL, text TEXT NOT NULL)
                    """);
                statement.execute("""
                    CREATE TABLE town_observations (id INTEGER PRIMARY KEY AUTOINCREMENT,
                        witness_id TEXT NOT NULL, day INTEGER NOT NULL, minute INTEGER NOT NULL,
                        place TEXT NOT NULL, text TEXT NOT NULL)
                    """);
                statement.execute("""
                    INSERT INTO town_events (day,minute,actor_id,place,text)
                    VALUES(1,450,'marco','BAKERY','A legacy event')
                    """);
            }
            try(var db=new WorldDatabase(file)) {
                WorldState state=db.load();
                var town=db.loadTown(state);
                var generated=PopulationGenerator.add(town,2);
                var first=generated.get(0);var second=generated.get(1);
                first.place=first.home=TownLife.Place.NORTH_RESIDENCES;
                second.place=second.home=TownLife.Place.NORTH_RESIDENCES;
                var events=new ArrayList<TownLife.Event>();
                var observations=new ArrayList<TownLife.Observation>();
                TownLife.publish(town,state,first.id,first.place,"An apartment event",events,observations);
                db.save(state,List.of(),List.of(),town,events,observations,List.of());
                check(db.townEvents(20).stream().anyMatch(e->e.text().equals("A legacy event")
                        && e.spaceId().isEmpty()),"Old events retained without invented room IDs");
                check(db.observations(second.id,20).isEmpty(),"No leaked private observations");
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();var town=db.loadTown(state);
                check(town.get("resident-0001").homeUnit.equals("unit:resident-0001"),
                        "First unit persisted");
                check(town.get("resident-0002").homeUnit.equals("unit:resident-0002"),
                        "Neighbor unit persisted");
                var logs=db.observations("resident-0001",20);
                check(logs.size()==1 && logs.getFirst().spaceId().equals("unit:resident-0001"),
                        "Private witness observation persisted");
                check(db.observations("resident-0002",20).isEmpty(),
                        "Isolation preserved after reopening database");
            }
            System.out.println("PrivateLivesDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {Files.deleteIfExists(file);}
    }

    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError("Failed: "+message);
    }
}
