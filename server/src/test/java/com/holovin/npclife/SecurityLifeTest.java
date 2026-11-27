package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static com.holovin.npclife.TownLife.Place.*;
class SecurityLifeTest {
    @Test void fundedProtectionAndInsuranceRespectOwnershipAndBalances(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);
        t.player.place=WORKSHOP;t.business.registered=true;t.business.ownerIncarnation=t.player.incarnation;t.business.cash=100;
        int payroll=t.employerFunds.getOrDefault(SecurityLife.JOB,0);
        SecurityLife.purchase(t,s,false);SecurityLife.purchase(t,s,true);
        assertEquals(80,t.business.cash);assertEquals(payroll+12,t.employerFunds.get(SecurityLife.JOB));
        assertEquals(8,t.employerFunds.get(SecurityLife.INSURER));
        assertTrue(SecurityLife.view(t,s).workshopProtected());
        assertThrows(IllegalArgumentException.class,()->SecurityLife.purchase(t,s,true));
        var actor=new TownLife.Citizen("actor","Actor","Baker",PARK);actor.generationIndex=301;actor.age=35;actor.place=WORKSHOP;actor.money=100;t.people.put(actor.id,actor);
        NpcPropertyCrimeLife.record(t,s,NpcPropertyCrimeLife.Kind.WORKSHOP,actor,null,new ArrayList<>(),new ArrayList<>());
        assertEquals(60,t.business.cash);
        NpcPropertyCrimeLife.resolve(t,s,false);
        assertEquals(68,t.business.cash);assertEquals(0,t.employerFunds.get(SecurityLife.INSURER));
        assertEquals(8,SecurityLife.view(t,s).compensation());
        assertTrue(SecurityLife.view(t.copy(),s).workshopInsured());
        s.day+=7;assertFalse(SecurityLife.view(t,s).workshopProtected());
    }
    @Test void guardReceivesOnlyExistingPayroll(){
        var s=new WorldState();var t=TownLife.create(s);var c=new TownLife.Citizen("guard","Guard",SecurityLife.OCCUPATION,PARK);c.money=20;
        t.employerFunds.put(SecurityLife.JOB,3);SecurityLife.pay(t,c);
        assertEquals(23,c.money);assertEquals(0,t.employerFunds.get(SecurityLife.JOB));SecurityLife.pay(t,c);assertEquals(23,c.money);
    }
}
