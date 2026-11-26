package com.holovin.npclife;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShootingLifeTest {
    @Test void practiceIsLegalButPublicDischargeRevokesPermit(){
        var s=new WorldState();var t=TownLife.create(s);s.minute=720;t.player.money=200;
        var truth=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
        assertThrows(IllegalArgumentException.class,()->CombatLife.fire(t,s,"",truth,observations));
        t.player.place=TownLife.Place.OFFICE;WeaponLicenseLife.apply(t,s);
        t.player.place=TownLife.Place.MARKET;WeaponLicenseLife.buySport(t,s);
        assertThrows(IllegalArgumentException.class,()->WeaponLicenseLife.practice(t,s));
        t.player.place=TownLife.Place.WORKSHOP;WeaponLicenseLife.practice(t,s);
        assertEquals(1,WeaponLicenseLife.view(t,s).practiceShots());
        assertTrue(WeaponLicenseLife.view(t,s).valid());
        assertEquals(0,CombatLife.reports(t));
        t.player.place=TownLife.Place.PARK;
        CombatLife.fire(t,s,"",truth,observations);
        assertEquals(1,JusticeLife.discharges(t));
        assertEquals(0,JusticeLife.view(t,s).assaults());
        assertEquals(1,CombatLife.reports(t));
        assertFalse(WeaponLicenseLife.view(t,s).ownsSportWeapon());
        assertThrows(IllegalArgumentException.class,()->CombatLife.fire(t,s,"",truth,observations));
    }
    @Test void finitePracticeSupplyAndRefill(){
        var s=new WorldState();var t=TownLife.create(s);s.minute=720;t.player.money=200;
        t.player.place=TownLife.Place.OFFICE;WeaponLicenseLife.apply(t,s);
        t.player.place=TownLife.Place.MARKET;WeaponLicenseLife.buySport(t,s);
        t.player.place=TownLife.Place.WORKSHOP;
        assertThrows(IllegalArgumentException.class,()->WeaponLicenseLife.refill(t,s));
        for(int i=0;i<WeaponLicenseLife.CAPACITY;i++)WeaponLicenseLife.practice(t,s);
        var report=WeaponLicenseLife.view(t,s);
        assertEquals(0,report.rounds());assertEquals(1,report.practiceSessions());
        assertEquals(0,WeaponLicenseLife.view(t.copy(),s).rounds());
        assertThrows(IllegalArgumentException.class,()->WeaponLicenseLife.practice(t,s));
        assertTrue(report.canRefill());
        int money=t.player.money;WeaponLicenseLife.refill(t,s);
        assertEquals(money-WeaponLicenseLife.REFILL_PRICE,t.player.money);
        assertEquals(WeaponLicenseLife.CAPACITY,WeaponLicenseLife.view(t,s).rounds());
    }
    @Test void residentHitUsesExistingNonlethalJusticeFlow(){
        var s=new WorldState();var t=TownLife.create(s);s.minute=720;t.player.money=200;
        t.player.place=TownLife.Place.OFFICE;WeaponLicenseLife.apply(t,s);
        t.player.place=TownLife.Place.MARKET;WeaponLicenseLife.buySport(t,s);
        t.player.place=TownLife.Place.PARK;
        var victim=t.get("elena");victim.alive=true;victim.age=35;victim.health=100;
        victim.place=TownLife.Place.PARK;victim.travelRemaining=0;victim.activity="FREE_TIME";
        var truth=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
        CombatLife.fire(t,s,victim.id,truth,observations);
        assertEquals(82,victim.health);assertTrue(victim.alive);
        assertEquals(1,JusticeLife.view(t,s).assaults());
        assertFalse(WeaponLicenseLife.view(t,s).valid());
    }
}
