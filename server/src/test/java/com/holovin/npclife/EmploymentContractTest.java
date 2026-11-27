package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class EmploymentContractTest {
    @TempDir Path folder;
    @Test void signedContractsExpireOrConvertAndTrackActualPay() throws Exception{
        var file=folder.resolve("contracts.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);s.day=2;s.minute=540;
            t.player.place=TownLife.Place.MARKET;t.player.destination=t.player.place;
            CareerLife.apply(t,"shop-assistant",s.day);
            assertEquals("PROBATION",EmploymentContractLife.report(t,2).status());
            assertEquals("FIXED_TERM",EmploymentContractLife.report(t,9).status());
            int before=t.player.money;
            for(int day=2;day<12;day++)CareerLife.work(t,day,540);
            var view=EmploymentContractLife.report(t,11);
            assertEquals("PERMANENT",view.status());
            assertEquals(10,view.paidShifts());
            assertEquals(240,view.totalPaid());
            assertEquals(before+240,t.player.money);
            s.day=11;
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            assertEquals("PERMANENT",EmploymentContractLife.report(t,s.day).status());
            assertEquals(240,EmploymentContractLife.report(t,s.day).totalPaid());
            EmploymentContractLife.resign(t,s.day);
            assertNull(t.player.jobId);
            CareerLife.apply(t,"shop-assistant",12);
            s.day=42;s.minute=0;
            EmploymentContractLife.tick(t,s);
            assertNull(t.player.jobId);
        }
    }
}
