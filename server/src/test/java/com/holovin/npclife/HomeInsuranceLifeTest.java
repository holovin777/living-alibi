package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HomeInsuranceLifeTest {
    @Test void preexistingPolicyCoversNewDamageAndPartialPaymentOnly(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        t.player.place=TownLife.Place.OFFICE;t.player.destination=t.player.place;t.player.money=100;t.player.ownsHouse=true;
        var home=HousingLife.HOUSE_DISTRICT;
        FireServiceLife.start(t,s,home);
        assertFalse(HomeInsuranceLife.canBuy(t,s));
        s.minute+=120;FireServiceLife.tick(t,s);
        assertEquals(0,HomeInsuranceLife.view(t,s).pending());
        HomeInsuranceLife.buy(t,s);assertEquals(12,t.employerFunds.get(HomeInsuranceLife.ACCOUNT).intValue());
        int before=t.player.money;
        s.minute+=5;assertTrue(FireServiceLife.start(t,s,home));
        int condition=FireRecoveryLife.condition(t,home);
        FireRecoveryLife.damage(t,home,30);
        assertEquals(0,HomeInsuranceLife.view(t,s).pending());
        s.minute+=120;FireServiceLife.tick(t,s);
        int claim=HomeInsuranceLife.view(t,s).pending();
        assertEquals(Math.min(60,Math.max(0,condition-FireRecoveryLife.condition(t,home)-5)),claim);
        assertTrue(claim>12);
        HomeInsuranceLife.collect(t,s);
        assertEquals(before+12,t.player.money);assertEquals(claim-12,HomeInsuranceLife.view(t.copy(),s).pending());
        assertFalse(HomeInsuranceLife.canCollect(t,s));
        t.employerFunds.put(HomeInsuranceLife.ACCOUNT,100);
        HomeInsuranceLife.collect(t,s);assertEquals(0,HomeInsuranceLife.view(t,s).pending());
        assertEquals(before+claim,t.player.money);
        assertFalse(HomeInsuranceLife.canCollect(t,s));
    }
}
