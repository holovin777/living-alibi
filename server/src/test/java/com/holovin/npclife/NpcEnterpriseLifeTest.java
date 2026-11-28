package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NpcEnterpriseLifeTest {
    @Test void residentBuysWithOwnSavingsAndBlocksPlayerPurchase(){
        var s=new WorldState();s.day=10;s.minute=390;
        var t=TownLife.create(s);t.industryStocks.put(StaffingLife.ENABLED,1);
        var c=PopulationGenerator.add(t,1).getFirst();
        c.age=35;c.carefulness=90;c.curiosity=90;c.money=500;c.sickUntilDay=0;
        String a="shop-assistant";t.employerFunds.put(a,0);
        for(int day=3;day<=9;day++){s.day=day;BusinessSolvencyLife.tick(t,s);}
        s.day=10;s.minute=660;
        int price=BusinessOwnershipLife.total(t,a),city=t.employerFunds.getOrDefault("city-maintenance",0);
        assertTrue(NpcEnterpriseLife.acquire(t,s,a,c));
        assertEquals(500-price,c.money);assertEquals(city+BusinessOwnershipLife.PRICE,t.employerFunds.get("city-maintenance"));
        assertEquals(c.id,NpcEnterpriseLife.owner(t.copy(),a).id);
        assertFalse(BusinessOwnershipLife.listed(t,a));assertFalse(BusinessOwnershipLife.yours(t,a));
        assertFalse(NpcEnterpriseLife.acquire(t,s,a,c));
        c.alive=false;s.minute=390;NpcEnterpriseLife.tick(t,s);
        assertNull(NpcEnterpriseLife.owner(t,a));
        assertEquals(0,BusinessOwnershipLife.owner(t,a));
    }
    @Test void clientsCannotBuyTheSameWeeklyItServiceFromTwoOwners(){
        var t=TownLife.create(new WorldState());var people=PopulationGenerator.add(t,2);
        t.industryStocks.put("it-market:last:retail",5);
        assertFalse(ITBusinessLife.eligibleWeek(t,people.get(0),"retail",10));
        assertFalse(ITBusinessLife.eligibleWeek(t,people.get(1),"retail",10));
        assertTrue(ITBusinessLife.eligibleWeek(t,people.get(1),"retail",12));
    }
}
