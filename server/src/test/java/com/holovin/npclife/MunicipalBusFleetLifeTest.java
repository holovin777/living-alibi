package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MunicipalBusFleetLifeTest {
 @Test void purchaseAndBoardingAreConservedAndBounded(){
    var s=new WorldState();s.day=10;s.minute=480;var t=TownLife.create(s);
    t.industryStocks.put(StaffingLife.ENABLED,1);
    t.employerFunds.put(MunicipalLife.CITY,40);
    assertFalse(MunicipalBusFleetLife.buy(t,1));
    t.employerFunds.put(MunicipalLife.CITY,200);
    assertTrue(MunicipalBusFleetLife.buy(t,1));assertFalse(MunicipalBusFleetLife.buy(t,1));
    assertEquals(110,t.employerFunds.get(MunicipalLife.CITY));assertEquals(30,MunicipalBusFleetLife.cash(t));
    assertFalse(MunicipalBusFleetLife.depart(t,s,1));
    MunicipalBusFleetLife.set(t,1,"from",TownLife.Place.STAZIONE.ordinal()+1);
    MunicipalBusFleetLife.set(t,1,"to",TownLife.Place.PARK.ordinal()+1);
    MunicipalBusFleetLife.set(t,1,"start",MunicipalBusFleetLife.now(s));
    MunicipalBusFleetLife.set(t,1,"end",MunicipalBusFleetLife.now(s)+10);
    for(int i=0;i<12;i++){
        assertEquals(1,MunicipalBusFleetLife.boarding(t,s,TownLife.Place.STAZIONE,TownLife.Place.PARK));
        MunicipalBusFleetLife.fare(t,1);
    }
    assertEquals(0,MunicipalBusFleetLife.boarding(t,s,TownLife.Place.STAZIONE,TownLife.Place.PARK));
    assertEquals(54,MunicipalBusFleetLife.cash(t));assertEquals(12,MunicipalBusFleetLife.view(t.copy(),s).buses().getFirst().passengers());
 }
}
