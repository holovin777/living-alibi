package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PayrollLifeTest {
    @TempDir Path folder;
    @Test void registeredPayIsConservedAndTfrPaidOnlyOnce() throws Exception{
        var file=folder.resolve("payroll.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);s.day=2;s.minute=600;
            t.player.privateName="Alex Test";t.player.place=TownLife.Place.OFFICE;
            t.player.destination=t.player.place;
            CivilRegistryLife.act(t,s,"registry-identity");
            t.player.place=TownLife.Place.MARKET;t.player.destination=t.player.place;
            CareerLife.apply(t,"shop-assistant",s.day);
            int wallet=t.player.money,city=MunicipalLife.balance(t,MunicipalLife.CITY);
            int employer=t.employerFunds.get("shop-assistant");
            CareerLife.work(t,s.day,600);
            var report=EmploymentContractLife.report(t,s.day);
            assertEquals(24,report.lastPay());assertEquals(20,report.lastNet());
            assertEquals(2,report.lastTax());assertEquals(1,report.lastPension());
            assertEquals(1,report.lastTfr());assertEquals(1,report.tfrBalance());
            assertEquals(wallet+20,t.player.money);
            assertEquals(city+2,MunicipalLife.balance(t,MunicipalLife.CITY));
            assertEquals(1,t.employerFunds.get(EmploymentContractLife.PENSION_ACCOUNT));
            assertEquals(employer-24,t.employerFunds.get("shop-assistant"));
            assertThrows(IllegalArgumentException.class,()->CareerLife.work(t,s.day,600));
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            var report=EmploymentContractLife.report(t,s.day);
            assertEquals(1,report.tfrBalance());assertEquals(1,report.pensionTotal());
            assertEquals(1,t.employerFunds.get(EmploymentContractLife.PENSION_ACCOUNT));
            int wallet=t.player.money;
            EmploymentContractLife.resign(t,s.day);
            assertEquals(wallet+1,t.player.money);
            assertEquals(0,EmploymentContractLife.report(t,s.day).tfrBalance());
            assertEquals(0,EmploymentContractLife.releaseTfr(t));
            CareerLife.apply(t,"shop-assistant",3);
            CareerLife.work(t,3,600);
            s.day=33;s.minute=0;
            wallet=t.player.money;
            EmploymentContractLife.tick(t,s);
            assertNull(t.player.jobId);assertEquals(wallet+1,t.player.money);
            assertEquals(2,EmploymentContractLife.report(t,33).pensionTotal());
        }
    }
}
