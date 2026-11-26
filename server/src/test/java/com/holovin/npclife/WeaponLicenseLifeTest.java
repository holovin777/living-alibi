package com.holovin.npclife;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WeaponLicenseLifeTest {
    @TempDir Path dir;
    @Test void purchaseRequiresPermitAndRevocationPersists() throws Exception {
        Path file=dir.resolve("permit.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);s.minute=720;t.player.money=150;
            t.player.place=TownLife.Place.MARKET;
            assertThrows(IllegalArgumentException.class,()->WeaponLicenseLife.buySport(t,s));
            assertThrows(IllegalArgumentException.class,()->CombatLife.buyBaton(t,s));
            assertEquals(150,t.player.money);
            t.player.place=TownLife.Place.OFFICE;WeaponLicenseLife.apply(t,s);
            assertTrue(WeaponLicenseLife.view(t,s).valid());
            t.player.place=TownLife.Place.MARKET;WeaponLicenseLife.buySport(t,s);
            assertEquals(60,t.player.money);
            assertThrows(IllegalArgumentException.class,()->WeaponLicenseLife.buySport(t,s));
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            assertTrue(WeaponLicenseLife.view(t,s).valid());
            assertTrue(WeaponLicenseLife.view(t,s).ownsSportWeapon());
            JusticeLife.report(t,s,false,false,new ArrayList<>(),new ArrayList<>());
            assertFalse(WeaponLicenseLife.view(t,s).valid());
            assertFalse(WeaponLicenseLife.view(t,s).ownsSportWeapon());
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            assertFalse(WeaponLicenseLife.view(t,s).valid());
            assertFalse(WeaponLicenseLife.view(t,s).ownsSportWeapon());
        }
    }
    @Test void expiresAndDoesNotTransferToAnotherIncarnation(){
        var s=new WorldState();var t=TownLife.create(s);s.minute=600;t.player.place=TownLife.Place.OFFICE;
        WeaponLicenseLife.apply(t,s);
        t.player.incarnation++;
        assertFalse(WeaponLicenseLife.view(t,s).valid());
        t.player.incarnation--;
        s.day+=WeaponLicenseLife.VALID_DAYS;
        assertFalse(WeaponLicenseLife.view(t,s).valid());
    }
}
