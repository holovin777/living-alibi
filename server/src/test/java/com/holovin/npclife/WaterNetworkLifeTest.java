package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WaterNetworkLifeTest {
 @Test void treatmentUsesRiverAndPowerAndSewageReturnsOnlyAfterProcessing(){
  var s=new WorldState();s.day=8;s.minute=700;var t=TownLife.create(s);
  t.industryStocks.put(StaffingLife.ENABLED,1);EnergyLife.ensure(t);WaterNetworkLife.ensure(t);
  EnergyLife.set(t,"battery",10);EnergyLife.set(t,"public-power",1);
  t.employerFunds.put(WaterNetworkLife.ACCOUNT,100);
  var water=new TownLife.Citizen("water-tech","Operator",WaterNetworkLife.WATER_ROLE,TownLife.Place.RIVER);
  var sewer=new TownLife.Citizen("sewer-tech","Operator",WaterNetworkLife.SEWER_ROLE,TownLife.Place.FACTORY);
  water.generationIndex=901;sewer.generationIndex=902;water.activity="WORKING";sewer.activity="WORKING";
  t.people.put(water.id,water);t.people.put(sewer.id,sewer);
  var river=t.naturalResources.get("river-water");river.stock=60;
  WaterNetworkLife.generate(t,s);assertEquals(44,river.stock);
  assertEquals(16,WaterNetworkLife.get(t,"treated"));assertEquals(9,EnergyLife.get(t,"battery"));
  t.retailStock.put("water",0);t.employerFunds.put(FoodSupplyLife.SHOP,100);
  int shop=FoodSupplyLife.stock(t,"water");assertEquals(8,WaterNetworkLife.sell(t));
  assertEquals(shop+8,FoodSupplyLife.stock(t,"water"));
  WaterNetworkLife.discharge(t);WaterNetworkLife.discharge(t);
  WaterNetworkLife.process(t,s);assertEquals(46,river.stock);
  assertEquals(0,WaterNetworkLife.get(t,"sewage"));assertEquals(2,WaterNetworkLife.get(t,"processed"));
  EnergyLife.set(t,"battery",0);WaterNetworkLife.generate(t,s);
  assertEquals(8,WaterNetworkLife.get(t,"treated"));assertEquals(46,river.stock);
  assertEquals(WaterNetworkLife.view(t),WaterNetworkLife.view(t.copy()));
  WaterNetworkLife.set(t,"sewage",32);WaterNetworkLife.discharge(t);
  assertEquals(32,WaterNetworkLife.get(t,"sewage"));assertEquals(1,WaterNetworkLife.get(t,"overflow"));
 }
}
