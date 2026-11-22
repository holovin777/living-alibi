package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

/** Uses real SQLite on the user's machine / GitHub CI. */
public final class SocialDatabaseSmokeTest {
    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("living-alibi-social-test-");
        Path path=dir.resolve("world.db");
        try {
            // Simulate a minimal v1.1 database before upgrading to v1.2.
            try(var sql=DriverManager.getConnection("jdbc:sqlite:"+path.toAbsolutePath());
                var st=sql.createStatement()) {
                st.execute("CREATE TABLE town_meta(id INTEGER PRIMARY KEY,together INTEGER NOT NULL)");
                st.execute("INSERT INTO town_meta(id,together) VALUES(1,0)");
            }
            try(var db=new WorldDatabase(path)) {
                var state=db.load();
                var town=db.loadTown(state);
                town.relationship.affinity=74;
                town.relationship.trust=70;
                town.relationship.encounters=4;
                town.relationship.conversations=3;
                town.relationship.lastInteractionTick=1119;
                town.meetingSinceTick=1105;
                town.together=true;
                var chat=new SocialLife.Interaction(1,1119,"marco","elena",TownLife.Place.PARK,
                        SocialLife.Kind.FRIENDLY_CHAT,"Marco and Elena talked at the park.");
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of(chat));
            }
            try(var db=new WorldDatabase(path)) {
                var town=db.loadTown(db.load());
                check(town.relationship.affinity==74 && town.relationship.trust==70,"relationship survived relaunch");
                check(town.relationship.encounters==4 && town.relationship.conversations==3,"social counters survived");
                check(town.relationship.lastInteractionTick==1119,"interaction clock survived");
                check(town.meetingSinceTick==1105,"meeting start survived");
                var history=db.socialEvents(20);
                check(history.size()==1 && history.get(0).kind()==SocialLife.Kind.FRIENDLY_CHAT,
                        "social history survived relaunch");
                // Prove additive migration remains idempotent on subsequent openings.
            }
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(dir);
        }
        System.out.println("SocialDatabaseSmokeTest: ALL CHECKS PASSED");
    }
    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError("Failed: "+message);
    }
}
