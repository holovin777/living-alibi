package com.holovin.npclife;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CombatLifeTest {
    @TempDir Path directory;
    @Test
    void ownedBatonAndPoliceReportSurviveReload() throws Exception {
        Path file=directory.resolve("combat.db");
        try(var db=new WorldDatabase(file)){
            var clock=db.load();var town=db.loadTown(clock);
            clock.minute=720;
            town.player.place=TownLife.Place.MARKET;
            town.player.money=50;
            assertThrows(IllegalArgumentException.class,()->CombatLife.strike(town,clock,"marco","baton",new ArrayList<>(),new ArrayList<>()));
            assertThrows(IllegalArgumentException.class,()->CombatLife.buyBaton(town,clock));
            town.player.place=TownLife.Place.OFFICE;
            WeaponLicenseLife.apply(town,clock);
            town.player.place=TownLife.Place.MARKET;
            assertTrue(CombatLife.buyBaton(town,clock).contains("12 EUR"));
            assertEquals(28,town.player.money);
            assertThrows(IllegalArgumentException.class,()->CombatLife.buyBaton(town,clock));
            town.player.place=TownLife.Place.PARK;
            var victim=town.get("marco");var officer=town.get("elena");
            for(var c:List.of(victim,officer)){
                c.alive=true;c.age=35;c.place=TownLife.Place.PARK;c.travelRemaining=0;c.activity="FREE_TIME";c.health=100;
            }
            officer.employed=true;officer.occupation=PoliceLife.OFFICER;
            clock.minute=PoliceLife.shiftStart(officer)+10;
            var truth=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
            var result=CombatLife.strike(town,clock,victim.id,"baton",truth,observations);
            assertEquals(30,result.minutes());
            assertEquals(88,victim.health);
            assertEquals(88,clock.health);
            assertFalse(CombatLife.ownsBaton(town));
            assertEquals(1,CombatLife.reports(town));
            assertTrue(observations.stream().anyMatch(o->o.witnessId().equals(officer.id)));
            assertTrue(SocialMemoryLife.unresolved(town,victim.id));
            assertNull(town.player.contacts.get(officer.id).knownName);
            db.save(clock,List.of(),List.of(),town,truth,observations,List.of());
        }
        try(var db=new WorldDatabase(file)){
            var clock=db.load();var town=db.loadTown(clock);
            assertFalse(CombatLife.ownsBaton(town));
            assertEquals(1,CombatLife.reports(town));
            assertEquals(88,clock.health);
            assertTrue(SocialMemoryLife.unresolved(town,"marco"));
        }
    }

    @Test
    void strikesRequirePublicPresenceAndAreNonlethal(){
        var clock=new WorldState();var town=TownLife.create(clock);
        town.player.place=TownLife.Place.PARK;
        var victim=town.get("elena");
        victim.age=35;victim.health=25;victim.alive=true;victim.travelRemaining=0;
        victim.place=TownLife.Place.CAFE;
        var truth=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
        assertThrows(IllegalArgumentException.class,()->CombatLife.strike(town,clock,victim.id,"fists",truth,observations));
        victim.place=TownLife.Place.PARK;victim.activity="FREE_TIME";
        assertEquals(2,CombatLife.strike(town,clock,victim.id,"fists",truth,observations).minutes());
        assertEquals(20,victim.health);assertTrue(victim.alive);
        assertThrows(IllegalArgumentException.class,()->CombatLife.strike(town,clock,victim.id,"fists",truth,observations));
    }
}
