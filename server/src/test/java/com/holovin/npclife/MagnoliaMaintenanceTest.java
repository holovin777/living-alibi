package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MagnoliaMaintenanceTest {
    @TempDir Path folder;
    @Test void faultsWearRepairsConsumeStockAndPersist() throws Exception {
        var file=folder.resolve("magnolia.db");
        try(var db=new WorldDatabase(file)){
            var clock=db.load();var town=db.loadTown(clock);
            assertTrue(TechnicalWorkLife.magnolia(town).liftWorking());
            assertFalse(TechnicalWorkLife.magnolia(town).lightingWorking());
            for(int day=2;day<=14;day++)TechnicalWorkLife.tick(town,day,0);
            assertFalse(TechnicalWorkLife.magnolia(town).liftWorking());
            assertEquals(TechnicalWorkLife.Phase.OPEN,town.technicalOrders.get("magnolia-lift").phase);
            town.player.place=TownLife.Place.NORTH_RESIDENCES;
            town.player.travelRemaining=0;town.player.engineeringSkill=3;
            assertThrows(IllegalArgumentException.class,()->TechnicalWorkLife.inspect(town,"magnolia-lift",540));
            town.player.jobId="technician";
            int stock=town.technicalMaterials.get("spare-parts");
            TechnicalWorkLife.inspect(town,"magnolia-lift",540);
            TechnicalWorkLife.complete(town,"magnolia-lift",14,570);
            assertEquals(stock-2,town.technicalMaterials.get("spare-parts"));
            assertTrue(TechnicalWorkLife.magnolia(town).liftWorking());
            assertFalse(TechnicalWorkLife.magnolia(town).lightingWorking());
            clock.day=14;clock.minute=660;
            db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var town=db.loadTown(db.load());
            assertTrue(TechnicalWorkLife.magnolia(town).liftWorking());
            assertFalse(TechnicalWorkLife.magnolia(town).lightingWorking());
            assertEquals(4,town.technicalMaterials.get("spare-parts"));
        }
    }
}
