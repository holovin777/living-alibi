package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class LinkedElectionTest {
    @TempDir Path folder;
    @Test void linkedResidentVotesOnceAndCanHoldOffice()throws Exception{
        try(var db=new WorldDatabase(folder.resolve("election.db"))){
            var s=db.load();var t=db.loadTown(s);
            var c=PopulationGenerator.add(t,1).getFirst();
            c.age=35;c.place=TownLife.Place.OFFICE;c.travelRemaining=0;c.activity="FREE_TIME";
            t.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
            t.industryStocks.put(RebirthLife.CHILD,c.generationIndex);
            DemographyLife.set(t,c,"played-incarnation",t.player.incarnation);
            ElectionLife.ensure(t);ElectionLife.set(t,ElectionLife.NEXT,31);
            ElectionLife.act(t,"election-register-2",2,600);
            assertEquals(c.name,ElectionLife.report(t,2).candidates().getLast().name());
            ElectionLife.act(t,"election-vote-4",2,600);
            assertThrows(IllegalArgumentException.class,()->ElectionLife.act(t,"election-vote-4",2,600));
            ElectionLife.tick(t,31,360);
            assertEquals(1,ElectionLife.get(t,ElectionLife.votes(4)));
            assertTrue(ElectionLife.playerMayor(t));
            ElectionLife.act(t,"election-policy-3",31,600);
            assertEquals(ElectionLife.INFRASTRUCTURE,ElectionLife.policy(t));
            c.alive=false;
            assertFalse(ElectionLife.playerMayor(t));
            assertEquals(ElectionLife.BALANCED,ElectionLife.policy(t));
        }
    }
}
