package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TransportDevelopmentLifeTest {
 @Test void completedStopExtendsRoutesAndRoadWearAddsDelay(){
    var s=new WorldState();var t=TownLife.create(s);
    t.industryStocks.put(StaffingLife.ENABLED,1);
    var p=TownLife.Place.NORTH_RESIDENCES;var park=TownLife.Place.PARK;
    assertFalse(BusLife.route(t,p,park));
    TransportDevelopmentLife.set(t,p,"stage",1);assertFalse(BusLife.route(t,p,park));
    TransportDevelopmentLife.set(t,p,"stage",2);assertTrue(BusLife.route(t,p,park));
    int base=BusLife.duration(t,p,park,600);
    TransportDevelopmentLife.set(t,p,"condition",35);
    assertEquals(base+6,BusLife.duration(t,p,park,600));
    assertTrue(BusLife.route(t.copy(),p,park));
 }
}
