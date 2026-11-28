package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RegionalRailLifeTest {
 @Test void finiteCapitalAndManifestDelivery(){
    var s=new WorldState();s.day=10;s.minute=540;var t=TownLife.create(s);
    t.industryStocks.put(StaffingLife.ENABLED,1);FactoryNetworkLife.ensure(t);
    t.employerFunds.put(MunicipalLife.CITY,40);
    assertFalse(RegionalRailLife.enable(t));
    t.employerFunds.put(MunicipalLife.CITY,200);
    assertTrue(RegionalRailLife.enable(t));assertFalse(RegionalRailLife.enable(t));
    assertEquals(80,t.employerFunds.get(MunicipalLife.CITY));assertEquals(40,RegionalRailLife.cash(t));
    var line=RegionalRailLife.LINES.getFirst();
    RegionalRailLife.set(t,line,"cargo",4);RegionalRailLife.set(t,line,"arrival",RegionalRailLife.now(s)+10);
    int initial=FactoryNetworkLife.stock(t,line.input());
    RegionalRailLife.arrive(t,s,line);assertEquals(initial,FactoryNetworkLife.stock(t,line.input()));
    s.minute+=10;RegionalRailLife.arrive(t,s,line);
    assertEquals(initial+4,FactoryNetworkLife.stock(t,line.input()));
    RegionalRailLife.arrive(t,s,line);assertEquals(initial+4,FactoryNetworkLife.stock(t,line.input()));
    assertEquals(4,RegionalRailLife.view(t.copy(),s).services().getFirst().delivered());
 }
}
