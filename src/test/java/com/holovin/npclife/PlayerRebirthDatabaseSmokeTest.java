package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;

/** Contact privacy, historical deaths, and world resources persist across lives. */
public final class PlayerRebirthDatabaseSmokeTest {
    private PlayerRebirthDatabaseSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path file=Files.createTempFile("living-alibi-generations-",".db");
        try {
            int resourceStock;
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                var marco=town.get("marco");
                marco.place=TownLife.Place.PARK;
                marco.travelRemaining=0;
                marco.activity="WORKING";
                PlayerLife.setPrivateName(town.player,"Private One");
                PlayerLife.introduce(town,"marco","Alias One",state.day,state.minute);
                resourceStock=town.naturalResources.get("river-water").stock;
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.player.incarnation==1 && town.player.contacts.size()==1,
                        "first visitor's historical contacts remain readable");
                check("Alias One".equals(town.player.contacts.get("marco").knownName),
                        "first generation knows its alias");
                town.player.health=1;
                town.player.place=TownLife.Place.RIVER;
                town.player.destination=TownLife.Place.RIVER;
                PlayerLife.sleep(town.player,8);
                PlayerLife.beginNewLife(town,state.day,state.minute);
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.player.incarnation==2 && town.player.contacts.isEmpty(),
                        "second visitor has no inherited contacts");
                check(town.player.privateName==null && town.player.alive,
                        "second visitor is new, anonymous, and alive");
                check(town.pastLives.size()==1 &&
                        "Private One".equals(town.pastLives.get(1).privateName()),
                        "first death stored privately");
                check(town.naturalResources.get("river-water").stock==resourceStock,
                        "nature stock untouched by death and rebirth");
                check(town.get("marco").alive,"NPC survives visitor death");
                var marco=town.get("marco");
                marco.place=TownLife.Place.PARK;
                marco.travelRemaining=0;
                marco.activity="WORKING";
                PlayerLife.introduce(town,"marco","Alias Two",state.day,state.minute);
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.player.incarnation==2 &&
                        "Alias Two".equals(town.player.contacts.get("marco").knownName),
                        "generation two keeps its own introduction");
                town.player.health=1;
                town.player.place=TownLife.Place.RIVER;
                town.player.destination=TownLife.Place.RIVER;
                PlayerLife.sleep(town.player,8);
                PlayerLife.beginNewLife(town,state.day,state.minute);
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.player.incarnation==3 && town.player.contacts.isEmpty(),
                        "third visitor has no inherited acquaintance");
                check(town.pastLives.size()==2 &&
                        town.pastLives.containsKey(1) && town.pastLives.containsKey(2),
                        "both past lives retained through restart");
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var conn=DriverManager.getConnection("jdbc:sqlite:"+file.toAbsolutePath());
                var query=conn.createStatement()) {
                try(var old=query.executeQuery("""
                        SELECT known_name FROM player_contacts WHERE resident_id='marco'
                        """)) {
                    check(old.next() && "Alias One".equals(old.getString(1)),
                            "first visitor's disclosed name never overwritten");
                }
                try(var later=query.executeQuery("""
                        SELECT known_name FROM player_life_contacts
                        WHERE incarnation=2 AND resident_id='marco'
                        """)) {
                    check(later.next() && "Alias Two".equals(later.getString(1)),
                            "second visitor's disclosed name stored separately");
                }
                try(var count=query.executeQuery("SELECT COUNT(*) FROM player_lives")) {
                    check(count.next() && count.getInt(1)==2,
                            "death history is idempotent");
                }
            }
            System.out.println("PlayerRebirthDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {Files.deleteIfExists(file);}
    }

    private static void check(boolean ok,String what) {
        if(!ok)throw new AssertionError(what);
    }
}
