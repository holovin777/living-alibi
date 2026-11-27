package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HealthRegistryTest {
    @TempDir Path folder;
    @Test void fiscalIdentityAndSubsidizedCarePersistWithoutMintingMoney() throws Exception{
        var file=folder.resolve("health-registry.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);s.day=2;s.minute=600;
            var p=t.player;p.place=TownLife.Place.OFFICE;p.destination=p.place;
            p.privateName="Alex Test";p.money=50;p.health=60;
            assertThrows(IllegalArgumentException.class,()->CivilRegistryLife.act(t,s,"registry-health"));
            CivilRegistryLife.act(t,s,"registry-identity");
            String code=CivilRegistryLife.report(t,s).fiscalCode();
            assertNotNull(code);
            CivilRegistryLife.act(t,s,"registry-health");
            assertEquals(45,p.money);
            assertThrows(IllegalArgumentException.class,()->CivilRegistryLife.act(t,s,"registry-health"));
            p.place=TownLife.Place.CLINIC;p.destination=p.place;
            t.employerFunds.put(MunicipalLife.CLINIC,20);
            int care=MunicipalLife.balance(t,MunicipalLife.CARE);
            assertEquals(5,CivilRegistryLife.consultPrice(t,s));
            CivilRegistryLife.consult(t,s);
            assertEquals(40,p.money);assertEquals(70,p.health);
            assertEquals(10,MunicipalLife.balance(t,MunicipalLife.CLINIC));
            assertEquals(care+15,MunicipalLife.balance(t,MunicipalLife.CARE));
            assertThrows(IllegalArgumentException.class,()->CivilRegistryLife.consult(t,s));
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            assertTrue(CivilRegistryLife.report(t,s).healthCardValid());
            assertEquals("LA-CF-V-1",CivilRegistryLife.report(t,s).fiscalCode());
            s.day=3;s.minute=600;t.player.health=60;
            t.employerFunds.put(MunicipalLife.CLINIC,0);
            assertEquals(15,CivilRegistryLife.consultPrice(t,s));
            assertTrue(CivilRegistryLife.canConsult(t,s));
            s.day=4000;
            assertFalse(CivilRegistryLife.report(t,s).healthCardValid());
            assertEquals("LA-CF-V-1",CivilRegistryLife.report(t,s).fiscalCode());
            t.player.incarnation++;
            assertNull(CivilRegistryLife.report(t,s).fiscalCode());
        }
    }
}
