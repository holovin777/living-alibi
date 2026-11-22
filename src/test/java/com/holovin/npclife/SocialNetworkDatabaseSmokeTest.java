package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** SQLite regression: a new relationship and its encounter state survive restart. */
public final class SocialNetworkDatabaseSmokeTest {
    private SocialNetworkDatabaseSmokeTest() { }
    public static void main(String[] args) throws Exception {
        Path database=Files.createTempFile("living-alibi-social-network-",".db");
        try {
            try(var db=new WorldDatabase(database)) {
                WorldState state=db.load();
                TownLife.Town town=db.loadTown(state);
                var residents=PopulationGenerator.add(town,2);
                var a=residents.get(0);var b=residents.get(1);
                a.place=TownLife.Place.CAFE;b.place=TownLife.Place.CAFE;
                a.destination=a.place;b.destination=b.place;
                a.travelRemaining=0;b.travelRemaining=0;
                a.activity="FREE_TIME";b.activity="FREE_TIME";
                var truth=new ArrayList<TownLife.Event>();
                var known=new ArrayList<TownLife.Observation>();
                var social=new ArrayList<SocialLife.Interaction>();
                state.minute=1019;
                for(int minute=1020;minute<=1080;minute++) {
                    state.minute=minute;
                    SocialNetwork.tick(town,state,truth,known,social);
                    if(minute==1020)town.existingRelationship(a.id,b.id).rng=0;
                }
                check(town.existingRelationship(a.id,b.id)!=null,"pair created");
                db.save(state,List.of(),List.of(),town,truth,known,social);
                check(db.socialEvents(20).size()==social.size(),"conversations persisted");
            }
            try(var db=new WorldDatabase(database)) {
                var state=db.load();var town=db.loadTown(state);
                var r=town.existingRelationship("resident-0001","resident-0002");
                check(r!=null && r.encounters==1 && r.conversations==1,"pair counters restored");
                check(r.meetingSinceTick>0 && r.lastSeenTick>0,"meeting boundary restored");
                int original=town.peerRelationships.size();
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
                check(town.peerRelationships.size()==original,"save is idempotent");
            }
            System.out.println("SocialNetworkDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {Files.deleteIfExists(database);}
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
