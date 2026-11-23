package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** A saved name is known only to the intended resident and persists across restarts. */
public final class PlayerPresenceDatabaseSmokeTest {
    private PlayerPresenceDatabaseSmokeTest() { }
    public static void main(String[] args) throws Exception {
        Path file=Files.createTempFile("living-alibi-player-",".db");
        try {
            try(var db=new WorldDatabase(file)) {
                var state=db.load();var town=db.loadTown(state);
                check(town.player.contacts.isEmpty(),"upgraded worlds start without invented NPC memories");
                check(town.player.privateName==null,"no assumed private name");
                var marco=town.get("marco");marco.place=TownLife.Place.PARK;
                var elena=town.get("elena");elena.place=TownLife.Place.PARK;
                PlayerLife.setPrivateName(town.player,"Real Name");
                PlayerLife.speak(town,"marco","Hello",state.day,state.minute);
                PlayerLife.introduce(town,"elena","Traveler",state.day,state.minute);
                PlayerLife.startTravel(town.player,TownLife.Place.LIBRARY);
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();var town=db.loadTown(state);
                check("Real Name".equals(town.player.privateName),"private name persisted");
                check(town.player.contacts.size()==2,"only contacted NPCs present");
                check(town.player.contacts.get("marco").knownName==null,"Marco did not learn the name");
                check("Traveler".equals(town.player.contacts.get("elena").knownName),"Elena remembers her alias");
                check(town.player.destination==TownLife.Place.LIBRARY && town.player.travelRemaining==10,
                        "travel position persisted");
                check(town.cases.isEmpty(),"no detective case was required to meet someone");
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var town=db.loadTown(db.load());
                check(town.player.contacts.size()==2,"idempotent save produces no duplicated identities");
            }
            System.out.println("PlayerPresenceDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {Files.deleteIfExists(file);}
    }
    private static void check(boolean ok,String label) {if(!ok)throw new AssertionError(label);}
}
