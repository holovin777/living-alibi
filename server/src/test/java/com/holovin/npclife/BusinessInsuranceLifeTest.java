package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BusinessInsuranceLifeTest {
    @Test void businessPremiumAndPartialClaimUseActualAccounts(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        PopulationGenerator.add(t,12);
        var agent=t.people.values().stream().filter(c->c.generationIndex>0&&c.alive).findFirst().orElseThrow();
        agent.age=35;agent.employed=true;agent.occupation=HouseholdInsuranceLife.ROLE;agent.workplace=TownLife.Place.OFFICE;
        agent.place=agent.workplace;agent.destination=agent.place;agent.travelRemaining=0;agent.activity="WORKING";agent.sickUntilDay=0;
        var b=BusinessInsuranceLife.businesses(t).stream().filter(x->x.account().equals("shop-assistant")).findFirst().orElseThrow();
        t.employerFunds.put(b.account(),100);t.employerFunds.put(HomeInsuranceLife.ACCOUNT,0);
        int cash=agent.money;
        assertTrue(BusinessInsuranceLife.sell(t,s,b,agent));
        assertFalse(BusinessInsuranceLife.sell(t,s,b,agent));
        assertEquals(76,t.employerFunds.get(b.account()).intValue());
        assertEquals(22,t.employerFunds.get(HomeInsuranceLife.ACCOUNT).intValue());
        assertEquals(cash+2,agent.money);
        assertTrue(FireServiceLife.start(t,s,b.district()));
        FireRecoveryLife.damage(t,b.district(),30);
        BusinessInsuranceLife.incidentEnded(t);
        assertEquals(50,BusinessInsuranceLife.get(t,b,"pending"));
        assertTrue(BusinessInsuranceLife.payClaim(t,s,b,agent));
        assertEquals(98,t.employerFunds.get(b.account()).intValue());
        assertEquals(28,BusinessInsuranceLife.get(t,b,"pending"));
        assertFalse(BusinessInsuranceLife.payClaim(t,s,b,agent));
        assertEquals(28,BusinessInsuranceLife.view(t.copy(),s).pending());
    }
}
