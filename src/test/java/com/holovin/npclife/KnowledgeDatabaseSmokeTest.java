package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** SQLite round-trip: observations, hearsay and their attribution survive restarts. */
public final class KnowledgeDatabaseSmokeTest {
    private KnowledgeDatabaseSmokeTest() { }
    public static void main(String[] args) throws Exception {
        Path path=Files.createTempFile("living-alibi-beliefs-",".db");
        String eventKey;
        try {
            try(var db=new WorldDatabase(path)) {
                var state=db.load();
                var town=db.loadTown(state);
                state.minute=1050;
                var marco=town.get("marco");var elena=town.get("elena");
                position(marco,TownLife.Place.PARK);
                position(elena,TownLife.Place.MARKET);
                var events=new ArrayList<TownLife.Event>();
                var witnesses=new ArrayList<TownLife.Observation>();
                TownLife.publish(town,state,"marco",TownLife.Place.PARK,
                        "Marco Bianchi found a stolen bicycle at Park.",events,witnesses);
                eventKey=KnowledgeLife.eventKey(events.getFirst());
                position(elena,TownLife.Place.PARK);
                KnowledgeLife.exchange(town,state,"marco","elena");
                check(town.pendingTransfers.size()==1,"share recorded");
                db.save(state,List.of(),List.of(),town,events,witnesses,List.of());
                check(town.pendingTransfers.isEmpty(),"pending transfer cleared after commit");
                check(db.informationTransfers(10).size()==1,"transfer persisted");
            }
            try(var db=new WorldDatabase(path)) {
                var state=db.load();var town=db.loadTown(state);
                var direct=town.beliefs.get("marco").get(eventKey);
                var heard=town.beliefs.get("elena").get(eventKey);
                check(direct!=null && direct.origin().equals("DIRECT"),"direct witness restored");
                check(heard!=null && heard.origin().equals("HEARD"),"heard report restored");
                check(heard.learnedFrom().equals("marco") && heard.hops()==1,"source and hops preserved");
                check(db.beliefs("elena",10).size()==1,"query is resident-scoped");
                check(db.informationTransfers(10).size()==1,"one exchange in journal");
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
                check(db.informationTransfers(10).size()==1,"re-save does not duplicate exchanges");
            }
        } finally {Files.deleteIfExists(path);}
        System.out.println("KnowledgeDatabaseSmokeTest: ALL CHECKS PASSED");
    }
    private static void position(TownLife.Citizen c,TownLife.Place place) {
        c.place=place;c.destination=place;c.travelRemaining=0;c.activity="FREE_TIME";
    }
    private static void check(boolean ok,String msg) {if(!ok)throw new AssertionError(msg);}
}
