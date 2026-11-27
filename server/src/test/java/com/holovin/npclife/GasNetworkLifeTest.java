package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GasNetworkLifeTest {
 @Test void gasImportsRequirePaidStaffAndSupplyRealIndustrialHeat(){
  var s=new WorldState();s.day=8;s.minute=680;var t=TownLife.create(s);
  t.industryStocks.put(StaffingLife.ENABLED,1);FactoryNetworkLife.ensure(t);GasNetworkLife.ensure(t);
  t.employerFunds.put(GasNetworkLife.ACCOUNT,50);
  assertEquals(0,GasNetworkLife.deliver(t,s));
  var c=new TownLife.Citizen("gas-op","Operator",GasNetworkLife.ROLE,TownLife.Place.FUEL_STATION);
  c.generationIndex=970;c.activity="WORKING";c.money=20;t.people.put(c.id,c);
  int supplier=t.employerFunds.get(GasNetworkLife.SUPPLIER);
  assertEquals(12,GasNetworkLife.deliver(t,s));
  assertEquals(12,GasNetworkLife.stock(t));assertEquals(34,GasNetworkLife.cash(t));
  assertEquals(supplier+12,t.employerFunds.get(GasNetworkLife.SUPPLIER).intValue());assertEquals(24,c.money);
  var p=FactoryNetworkLife.PLANTS.stream().filter(x->x.id().equals("paper-mill")).findFirst().orElseThrow();
  FactoryNetworkLife.change(t,"logs",2);
  assertTrue(FactoryNetworkLife.produce(t,p,s.day));assertEquals(11,GasNetworkLife.stock(t));
  assertTrue(GasNetworkLife.homeUse(t));assertEquals(10,GasNetworkLife.stock(t));
  GasNetworkLife.set(t,"stock",0);
  assertFalse(GasNetworkLife.homeUse(t));assertFalse(FactoryNetworkLife.produce(t,p,s.day));
  assertEquals(1,FactoryNetworkLife.stock(t,"logs"));
  assertEquals(GasNetworkLife.view(t),GasNetworkLife.view(t.copy()));
 }
}
