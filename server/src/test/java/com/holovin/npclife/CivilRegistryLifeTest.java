package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CivilRegistryLifeTest {
    @TempDir Path folder;
    @Test void identityIsPrivatePaidAndPersistsAcrossReload() throws Exception{
        Path file=folder.resolve("registry.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);s.day=2;s.minute=600;
            t.player.privateName="Alex Test";t.player.money=50;
            assertThrows(IllegalArgumentException.class,()->CivilRegistryLife.act(t,s,"registry-identity"));
            t.player.place=TownLife.Place.OFFICE;t.player.destination=t.player.place;
            int before=t.employerFunds.get(MunicipalLife.CITY);
            CivilRegistryLife.act(t,s,"registry-identity");
            assertEquals(45,t.player.money);
            assertEquals(before+5,t.employerFunds.get(MunicipalLife.CITY));
            assertThrows(IllegalArgumentException.class,()->CivilRegistryLife.act(t,s,"registry-identity"));
            assertThrows(IllegalArgumentException.class,()->CivilRegistryLife.act(t,s,"registry-residence"));
            t.player.ownsHouse=true;
            CivilRegistryLife.act(t,s,"registry-residence");
            t.player.privateName="Other alias";
            assertEquals("Alex Test",CivilRegistryLife.report(t,s).legalName());
            assertTrue(CivilRegistryLife.report(t,s).residenceRegistered());
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            assertTrue(CivilRegistryLife.report(t,s).identityValid());
            assertEquals("Alex Test",CivilRegistryLife.report(t,s).legalName());
            assertTrue(CivilRegistryLife.report(t,s).residenceRegistered());
            t.player.incarnation++;
            assertFalse(CivilRegistryLife.report(t,s).identityValid());
            assertFalse(CivilRegistryLife.report(t,s).residenceRegistered());
        }
    }
}
