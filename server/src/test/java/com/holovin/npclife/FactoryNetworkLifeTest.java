package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FactoryNetworkLifeTest {
 @Test void timberPaperAndBooksShareStockAndCash(){
  var t=TownLife.create(new WorldState());t.industryStocks.put(StaffingLife.ENABLED,1);
  assertTrue(FactoryNetworkLife.ensure(t));assertFalse(FactoryNetworkLife.ensure(t));
  var mill=FactoryNetworkLife.PLANTS.stream().filter(p->p.id().equals("paper-mill")).findFirst().orElseThrow();
  var print=FactoryNetworkLife.PLANTS.stream().filter(p->p.id().equals("printing-house")).findFirst().orElseThrow();
  assertFalse(FactoryNetworkLife.produce(t,mill));SupplyChainLife.set(t,FuelLife.STOCK,3);
  assertTrue(FactoryNetworkLife.harvest(t));assertEquals(78,SupplyChainLife.get(t,"factory-network:forest"));
  assertTrue(FactoryNetworkLife.produce(t,mill));assertEquals(1,FactoryNetworkLife.stock(t,"logs"));
  assertTrue(FactoryNetworkLife.produce(t,print));assertEquals(1,FactoryNetworkLife.stock(t,"paper"));
  var book=PersonalGoodsLife.product("book");t.industryStocks.put(PersonalGoodsLife.stockKey(book),0);
  t.employerFunds.put("shop-assistant",50);assertTrue(FactoryNetworkLife.deliver(t,print));
  assertEquals(1,PersonalGoodsLife.stock(t.copy(),book));assertEquals(1,FactoryNetworkLife.stock(t,"book"));
  assertEquals(44,t.employerFunds.get("shop-assistant"));assertEquals(1,FuelLife.stock(t));
 }
}
