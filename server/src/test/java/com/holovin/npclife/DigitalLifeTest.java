package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class DigitalLifeTest {
    @TempDir Path dir;
    @Test void digitalBillPaymentUsesResidentWalletAndExistingLedger()throws Exception{
        try(var db=new WorldDatabase(dir.resolve("digital.db"))){
            var s=db.load();var t=db.loadTown(s);var c=PopulationGenerator.add(t,1).getFirst();
            c.age=30;c.money=50;c.unpaidBills=70;c.activity="FREE_TIME";c.travelRemaining=0;
            t.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
            t.industryStocks.put(RebirthLife.CHILD,c.generationIndex);
            DemographyLife.set(t,c,"played-incarnation",t.player.incarnation);
            assertTrue(DigitalLife.view(t,s).canPay());
            DigitalLife.pay(t,s);
            assertEquals(20,c.money);assertEquals(40,c.unpaidBills);
            assertThrows(IllegalArgumentException.class,()->DigitalLife.pay(t,s));
            assertFalse(DigitalLife.view(t.copy(),s).canPay());
        }
    }
}
