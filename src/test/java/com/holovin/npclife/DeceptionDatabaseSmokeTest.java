package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Secrets, deceptive statements and direct corrections survive an SQLite restart. */
public final class DeceptionDatabaseSmokeTest {
    private DeceptionDatabaseSmokeTest() { }
    public static void main(String[] args) throws Exception {
        Path file=Files.createTempFile("living-alibi-deception-", ".db");
        String eventKey;
        String truth="Marco Bianchi and Elena Rossi crossed paths at Park.";
        try {
            try(var db=new WorldDatabase(file)) {
                var state=db.load();state.day=4;state.minute=1020;
                var town=db.loadTown(state);
                var marco=town.get("marco");var elena=town.get("elena");
                place(marco,TownLife.Place.PARK);place(elena,TownLife.Place.MARKET);
                var events=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
                TownLife.publish(town,state,"marco",TownLife.Place.PARK,truth,events,observations);
                eventKey=KnowledgeLife.eventKey(events.getFirst());
                DeceptionLife.mark(town,"marco",eventKey,4,1020,"PERSONAL_INCIDENT");
                marco.carefulness=90;
                place(elena,TownLife.Place.PARK);
                KnowledgeLife.exchange(town,state,"marco","elena");
                db.save(state,List.of(),List.of(),town,events,observations,List.of());
                check(town.pendingDecisions.isEmpty(),"pending decisions cleared only after commit");
                check(db.deceptions(10).size()==1,"one deliberate lie committed");
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();var town=db.loadTown(state);
                check(town.secrets.get("marco").containsKey(eventKey),"private secret restored");
                check(db.secrets("marco",10).size()==1,"scoped secret query");
                var heard=town.beliefs.get("elena").get(eventKey);
                check(!heard.statement().equals(truth) && heard.origin().equals("HEARD"),"lie loaded as hearsay");
                // Verify v1.7 conflict update persists corrected statement, not just corrected origin.
                var original=town.beliefs.get("marco").get(eventKey);
                town.beliefs.get("elena").put(eventKey,new KnowledgeLife.Belief("elena",eventKey,
                        original.eventDay(),original.eventMinute(),original.actorId(),original.sceneId(),
                        truth,"DIRECT","elena",state.day,state.minute,0,100));
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
                check(db.deceptions(10).size()==1,"saving unchanged state does not duplicate decision");
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();var town=db.loadTown(state);
                var corrected=town.beliefs.get("elena").get(eventKey);
                check(corrected.origin().equals("DIRECT") && corrected.statement().equals(truth),
                        "truthful direct correction persisted across restart");
                check(db.deceptions(10).getFirst().kind().equals("LIE"),"lie audit intact");
            }
        } finally {Files.deleteIfExists(file);}
        System.out.println("DeceptionDatabaseSmokeTest: ALL CHECKS PASSED");
    }
    private static void place(TownLife.Citizen c,TownLife.Place p) {
        c.place=p;c.destination=p;c.travelRemaining=0;c.activity="FREE_TIME";
    }
    private static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
}
