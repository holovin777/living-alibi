package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PensionLifeTest {
    @TempDir Path folder;
    @Test void limitedPensionsAreFairConservedAndPersistent() throws Exception{
        Path file=folder.resolve("pensions.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            for(var c:t.people.values())if("Retired".equals(c.occupation))c.occupation="Job seeker";
            var a=new TownLife.Citizen("resident-9001","Test A","Retired",TownLife.Place.SOUTH_RESIDENCES);
            var b=new TownLife.Citizen("resident-9002","Test B","Retired",TownLife.Place.SOUTH_RESIDENCES);
            a.generationIndex=9001;b.generationIndex=9002;
            for(var c:List.of(a,b)){c.age=70;c.alive=true;c.employed=false;c.money=20;t.people.put(c.id,c);}
            t.employerFunds.put(EmploymentContractLife.PENSION_ACCOUNT,20);
            t.employerFunds.put(MunicipalLife.CITY,MunicipalLife.RESERVE+10);
            s.day=8;s.minute=360;
            PensionLife.tick(t,s);
            assertEquals(35,a.money);assertEquals(35,b.money);
            assertEquals(20,PensionLife.outstanding(t,a));assertEquals(20,PensionLife.outstanding(t,b));
            assertEquals(40L,PensionLife.report(t).arrears());
            assertEquals(0,PensionLife.report(t).fund());
            assertEquals(MunicipalLife.RESERVE,MunicipalLife.balance(t,MunicipalLife.CITY));
            PensionLife.tick(t,s);
            assertEquals(35,a.money);
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            assertEquals(40L,PensionLife.report(t).arrears());
            assertEquals(20,PensionLife.report(t).fundPaid());
            assertEquals(10,PensionLife.report(t).municipalPaid());
            t.employerFunds.put(EmploymentContractLife.PENSION_ACCOUNT,110);
            s.day=15;s.minute=360;
            PensionLife.tick(t,s);
            assertEquals(90,t.get("resident-9001").money);assertEquals(90,t.get("resident-9002").money);
            assertEquals(0L,PensionLife.report(t).arrears());
            assertEquals(0,PensionLife.report(t).fund());
        }
    }
}
