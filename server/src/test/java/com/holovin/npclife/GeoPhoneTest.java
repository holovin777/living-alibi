package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class GeoPhoneTest {
    @TempDir Path folder;
    @Test void phoneUsesSharedAccountsAndPublicJobs()throws Exception{
        var graph=folder.resolve("roads.tsv");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nE\t1\t2\n");
        var map=new GeoProjection(graph);
        var s=new WorldState();var t=TownLife.create(s);
        BankLife.set(t,BankLife.SAVINGS,25);
        var phone=map.phone(t,s);
        assertTrue(phone.report().available());
        assertEquals(25,phone.bank().personal());
        assertEquals(CareerLife.offers().size(),phone.jobs().size());
        assertEquals("OFFICE",phone.bankLocation().id());
        assertTrue(phone.actions().isEmpty());
        t.player.alive=false;
        assertFalse(map.phone(t,s).report().available());
        assertNull(map.phone(t,s).bank());
        assertTrue(map.phone(t,s).jobs().isEmpty());
    }
}
