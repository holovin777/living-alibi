package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SupplyChainLifeTest {
 @Test void appliancesConsumeMetalFuelAndCash(){
  var s=new WorldState();s.minute=600;var t=TownLife.create(s);
  var r=SupplyChainLife.RECIPES.stream().filter(x->x.id().equals("fridge")).findFirst().orElseThrow();
  assertFalse(SupplyChainLife.assemble(t,r));
  SupplyChainLife.set(t,IndustryLife.FACTORY_STEEL,2);SupplyChainLife.set(t,IndustryLife.FACTORY_COPPER,1);
  assertTrue(SupplyChainLife.assemble(t,r));assertFalse(SupplyChainLife.assemble(t,r));
  SupplyChainLife.set(t,FuelLife.STOCK,1);t.employerFunds.put("shop-assistant",100);
  assertTrue(SupplyChainLife.deliver(t,r));assertEquals(58,t.employerFunds.get("shop-assistant"));
  assertEquals(0,FuelLife.stock(t));assertEquals(3,SupplyChainLife.stock(t,"fridge"));
  t.player.place=TownLife.Place.PORTICI;t.player.ownsHouse=true;t.player.money=100;
  HomeGoodsLife.buy(t,s,"fridge");assertEquals(2,SupplyChainLife.stock(t.copy(),"fridge"));
 }
}
