package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FireRecoveryLifeTest {
    @Test void damagePersistsAndRestorationConsumesMaterialsAndBudget(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        var p=TownLife.Place.MARKET;
        t.player.place=p;t.player.destination=p;t.player.engineeringSkill=3;t.player.money=20;
        t.employerFunds.put("city-maintenance",100);
        FireRecoveryLife.damage(t,p,50);
        assertTrue(FireRecoveryLife.closed(t.copy(),p));
        assertFalse(FireRecoveryLife.canRepair(t,s,p));
        FactoryNetworkLife.change(t,"building-material",2);
        FireRecoveryLife.playerRepair(t,s,p);
        assertEquals(70,FireRecoveryLife.condition(t,p));assertFalse(FireRecoveryLife.closed(t,p));
        assertEquals(1,FactoryNetworkLife.stock(t,"building-material"));
        assertEquals(90,t.employerFunds.get("city-maintenance").intValue());assertEquals(26,t.player.money);
        FireRecoveryLife.playerRepair(t,s,p);assertEquals(90,FireRecoveryLife.condition(t,p));
        assertFalse(FireRecoveryLife.canRepair(t,s,p));
    }
}
