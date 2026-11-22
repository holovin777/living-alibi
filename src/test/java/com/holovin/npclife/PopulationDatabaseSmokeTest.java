package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** SQLite regression: generated identities, personal state and logs survive a restart. */
public final class PopulationDatabaseSmokeTest {
    private PopulationDatabaseSmokeTest() { }
    public static void main(String[] args) throws Exception {
        Path path=Files.createTempFile("living-alibi-population-",".db");
        try {
            var names=new ArrayList<String>();
            try(var db=new WorldDatabase(path)) {
                WorldState state=db.load();
                var town=db.loadTown(state);
                var generated=PopulationGenerator.add(town,8);
                for(var c:generated)names.add(c.name);
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
                var truth=new ArrayList<TownLife.Event>();
                var observations=new ArrayList<TownLife.Observation>();
                var social=new ArrayList<SocialLife.Interaction>();
                var changes=LifeSimulation.advance(state,1440L,
                        s->TownLife.tick(town,s,truth,observations,social));
                db.save(state,changes,List.of(),town,truth,observations,social);
                check(db.townEvents(100).size()>0,"town events written");
            }
            try(var db=new WorldDatabase(path)) {
                WorldState state=db.load();
                var town=db.loadTown(state);
                check(town.people.size()==10,"ten residents including Marco and Elena");
                check(town.get("resident-0001").name.equals(names.getFirst()),"identity persisted");
                check(town.get("resident-0008").generationIndex==8,"generation index persisted");
                check(town.get("resident-0001").workplace!=null,"job location persisted");
                check(town.get("resident-0001").personalSeed!=0,"personal seed persisted");
                check(state.day==2,"world time persisted");
                PopulationGenerator.add(town,2);
                check(town.get("resident-0009")!=null,"unique IDs after restarting");
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            System.out.println("PopulationDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {
            Files.deleteIfExists(path);
        }
    }
    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError("Failed: "+label);
    }
}
