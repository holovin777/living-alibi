package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static com.holovin.npclife.TownLife.Place.*;
class NpcPropertyCrimeLifeTest {
    private TownLife.Citizen resident(TownLife.Town t,String id,int index,TownLife.Place place){
        var c=new TownLife.Citizen(id,id,"Baker",PARK);
        c.generationIndex=index;c.age=35;c.home=PARK;c.place=place;c.destination=place;c.carefulness=85;c.money=100;
        t.people.put(id,c);return c;
    }
    @Test void shopLossAndRestitutionPersistWithoutChargingPlayer(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);t.industryStocks.put(NpcTransportLife.ENABLED,1);
        var actor=resident(t,"actor",301,MARKET);resident(t,"witness",302,MARKET);
        t.employerFunds.put("shop-assistant",100);int playerMoney=t.player.money;
        NpcPropertyCrimeLife.record(t,s,NpcPropertyCrimeLife.Kind.SHOP,actor,null,new ArrayList<>(),new ArrayList<>());
        assertEquals(80,t.employerFunds.get("shop-assistant"));assertEquals(120,actor.money);
        assertTrue(NpcPropertyCrimeLife.active(t.copy()));assertEquals(playerMoney,t.player.money);
        NpcPropertyCrimeLife.set(t,"examined",1);s.day=2;
        NpcPropertyCrimeLife.tick(t,s,new ArrayList<>(),new ArrayList<>());
        assertEquals(100,t.employerFunds.get("shop-assistant"));assertEquals(100,actor.money);
        assertEquals(20,NpcPropertyCrimeLife.view(t).restitution());assertEquals(1,NpcJusticeLife.convictions(t,actor));
        assertFalse(NpcPropertyCrimeLife.active(t));assertEquals(playerMoney,t.player.money);
    }
    @Test void bankDepositsRemainProtectedAndUnconfirmedCaseDoesNotConvict(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);t.industryStocks.put(NpcTransportLife.ENABLED,1);
        var actor=resident(t,"actor",301,OFFICE);
        BankLife.set(t,BankLife.SAVINGS,95);BankLife.cash(t,100);
        NpcPropertyCrimeLife.record(t,s,NpcPropertyCrimeLife.Kind.BANK,actor,null,new ArrayList<>(),new ArrayList<>());
        assertEquals(95,BankLife.cash(t));assertEquals(95,BankLife.get(t,BankLife.SAVINGS));
        s.day=5;NpcPropertyCrimeLife.tick(t,s,new ArrayList<>(),new ArrayList<>());
        assertEquals(0,NpcJusticeLife.convictions(t,actor));assertFalse(NpcPropertyCrimeLife.active(t));
        assertEquals(5,NpcPropertyCrimeLife.view(t).totalLoss());
    }
    @Test void privateBusinessAndHouseholdFundsAreSeparate(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);
        var actor=resident(t,"actor",301,WORKSHOP);t.business.registered=true;t.business.cash=40;
        NpcPropertyCrimeLife.record(t,s,NpcPropertyCrimeLife.Kind.WORKSHOP,actor,null,new ArrayList<>(),new ArrayList<>());
        assertEquals(20,t.business.cash);NpcPropertyCrimeLife.resolve(t,s,false);
        actor.place=NORTH_RESIDENCES;actor.destination=actor.place;
        var victim=resident(t,"victim",302,MARKET);victim.home=NORTH_RESIDENCES;
        NpcPropertyCrimeLife.record(t,s,NpcPropertyCrimeLife.Kind.HOME,actor,victim,new ArrayList<>(),new ArrayList<>());
        assertEquals(80,victim.money);assertEquals(140,actor.money);
    }
}
