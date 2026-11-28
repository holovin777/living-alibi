package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class IndustrialExpansionLifeTest {
 @Test void investmentRequiresShortageAndTransfersRealCapital(){
    var s=new WorldState();s.day=14;s.minute=660;var t=TownLife.create(s);
    t.industryStocks.put(StaffingLife.ENABLED,1);FactoryNetworkLife.ensure(t);
    var c=PopulationGenerator.add(t,1).getFirst();
    c.age=40;c.carefulness=90;c.curiosity=90;c.money=1000;c.sickUntilDay=0;
    var p=IndustrialExpansionLife.PLANTS.getFirst();
    assertFalse(IndustrialExpansionLife.invest(t,s,p,c));
    IndustrialExpansionLife.set(t,p,"shortage",3);
    int city=t.employerFunds.getOrDefault("city-maintenance",0);
    assertTrue(IndustrialExpansionLife.invest(t,s,p,c));
    assertEquals(800,c.money);assertEquals(city+120,t.employerFunds.get("city-maintenance"));
    assertEquals(80,t.employerFunds.get(IndustrialExpansionLife.account(p)));
    assertFalse(IndustrialExpansionLife.open(t,p));
    assertFalse(IndustrialExpansionLife.available(t,p.role()));
    assertFalse(IndustrialExpansionLife.produce(t,s,p));
    assertEquals(c.name,IndustrialExpansionLife.view(t.copy()).getFirst().owner());
    assertFalse(IndustrialExpansionLife.invest(t,s,p,c));
 }
}
