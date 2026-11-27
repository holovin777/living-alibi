package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WasteLifeTest {
 @Test void collectionAndRecoveryConsumeFiniteInputsAndSellBiogas(){
  var s=new WorldState();s.day=8;s.minute=710;var t=TownLife.create(s);
  t.industryStocks.put(StaffingLife.ENABLED,1);FactoryNetworkLife.ensure(t);GasNetworkLife.ensure(t);WasteLife.ensure(t);
  t.employerFunds.put(WasteLife.ACCOUNT,100);t.employerFunds.put(GasNetworkLife.ACCOUNT,30);
  var collector=new TownLife.Citizen("waste-driver","Collector",WasteLife.COLLECTOR,TownLife.Place.LOGISTICS_DEPOT);
  var operator=new TownLife.Citizen("waste-op","Recycler",WasteLife.OPERATOR,TownLife.Place.FACTORY);
  collector.generationIndex=980;operator.generationIndex=981;collector.activity="WORKING";operator.activity="WORKING";
  t.people.put(collector.id,collector);t.people.put(operator.id,operator);
  WasteLife.discard(t,"organic",4);WasteLife.discard(t,"paper",4);
  int fuel=FuelLife.stock(t);
  WasteLife.collect(t,s);assertEquals(fuel-1,FuelLife.stock(t));
  assertEquals(0,WasteLife.get(t,"bin:organic"));assertEquals(4,WasteLife.get(t,"sorted:organic"));
  WasteLife.process(t,s);assertEquals(0,WasteLife.get(t,"sorted:organic"));
  assertEquals(2,WasteLife.get(t,"recovered:biogas"));assertEquals(2,WasteLife.get(t,"recovered:paper"));
  WasteLife.sell(t);assertEquals(1,GasNetworkLife.stock(t));assertEquals(1,WasteLife.get(t,"recovered:biogas"));
  assertEquals(1,FactoryNetworkLife.stock(t,"paper"));assertEquals(29,GasNetworkLife.cash(t));
  WasteLife.discard(t,"glass",30);assertEquals(24,WasteLife.get(t,"bin:glass"));assertEquals(6,WasteLife.get(t,"litter"));
  assertEquals(WasteLife.view(t),WasteLife.view(t.copy()));
 }
}
