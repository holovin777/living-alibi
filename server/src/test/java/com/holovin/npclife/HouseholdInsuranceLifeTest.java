package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HouseholdInsuranceLifeTest {
    @Test void householdPaysOnePremiumAndReceivesOnlyFiniteClaim(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        PopulationGenerator.add(t,12);
        var people=t.people.values().stream().filter(c->c.generationIndex>0&&c.alive).limit(3).toList();
        assertEquals(3,people.size());
        for(var c:t.people.values())if(c.generationIndex>0)c.home=TownLife.Place.PARK;
        var a=people.get(0);var b=people.get(1);var agent=people.get(2);
        for(var c:java.util.List.of(a,b)){c.age=30;c.home=TownLife.Place.NORTH_RESIDENCES;c.homeUnit="insured-test";c.money=100;c.carefulness=80;}
        agent.age=35;agent.employed=true;agent.occupation=HouseholdInsuranceLife.ROLE;agent.workplace=TownLife.Place.OFFICE;
        agent.place=agent.workplace;agent.destination=agent.place;agent.travelRemaining=0;agent.activity="WORKING";agent.sickUntilDay=0;
        int agentCash=agent.money;
        var homes=HouseholdInsuranceLife.households(t);assertEquals(1,homes.size());
        var h=homes.getFirst();var payer=h.payer();
        assertTrue(HouseholdInsuranceLife.sell(t,s,h,agent));
        assertFalse(HouseholdInsuranceLife.sell(t,s,h,agent));
        assertEquals(88,payer.money);assertEquals(agentCash+1,agent.money);
        assertEquals(11,t.employerFunds.get(HomeInsuranceLife.ACCOUNT).intValue());
        assertTrue(FireServiceLife.start(t,s,h.district()));
        FireRecoveryLife.damage(t,h.district(),30);
        HouseholdInsuranceLife.incidentEnded(t);
        assertEquals(25,HouseholdInsuranceLife.get(t,h,"pending"));
        assertTrue(HouseholdInsuranceLife.payClaim(t,s,h,agent));
        assertEquals(14,HouseholdInsuranceLife.get(t,h,"pending"));
        assertFalse(HouseholdInsuranceLife.payClaim(t,s,h,agent));
        assertEquals(99,payer.money);
        assertEquals(14,HouseholdInsuranceLife.view(t.copy(),s).pending());
    }
}
