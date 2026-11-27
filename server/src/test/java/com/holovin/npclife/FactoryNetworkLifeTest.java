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
 @Test void successfulSalesCanFundOneWeeklyBonusWithoutCreatingCash(){
  var s=new WorldState();s.day=8;
  var t=TownLife.create(s);t.industryStocks.put(StaffingLife.ENABLED,1);
  FactoryNetworkLife.ensure(t);
  var plant=FactoryNetworkLife.PLANTS.stream().filter(p->p.id().equals("printing-house")).findFirst().orElseThrow();
  var worker=new TownLife.Citizen("bonus-worker","Ada",plant.occupation(),plant.site());
  worker.home=TownLife.Place.NORTH_RESIDENCES;worker.activity="WORKING";
  worker.carefulness=100;worker.fatigue=0;worker.hunger=0;worker.thirst=0;
  t.people.put(worker.id,worker);
  EducationLife.set(t,worker,EducationLife.subject(worker.occupation),100);
  for(int m=480;m<=720;m+=60){s.minute=m;WorkplacePerformance.tick(t,s);}
  var account=FactoryNetworkLife.account(plant);
  t.employerFunds.put(account,35);
  assertFalse(WorkplaceRewards.afterSale(t,s,plant));
  t.employerFunds.put(account,100);
  int wallet=worker.money;
  var book=PersonalGoodsLife.product("book");
  t.industryStocks.put(PersonalGoodsLife.stockKey(book),0);
  t.employerFunds.put("shop-assistant",50);
  SupplyChainLife.set(t,FuelLife.STOCK,2);
  FactoryNetworkLife.change(t,"book",1);
  assertTrue(FactoryNetworkLife.deliver(t,plant));
  int afterSale=t.employerFunds.get(account);
  assertTrue(WorkplaceRewards.afterSale(t,s,plant));
  assertEquals(afterSale-WorkplaceRewards.BONUS,t.employerFunds.get(account).intValue());
  assertEquals(wallet+WorkplaceRewards.BONUS,worker.money);
  assertFalse(WorkplaceRewards.afterSale(t,s,plant));
  assertFalse(WorkplaceRewards.afterSale(t.copy(),s,plant));
  s.day=15;worker.sickUntilDay=15;
  assertFalse(WorkplaceRewards.afterSale(t,s,plant));
 }
 @Test void wornMachinesStopProductionAndPaidRepairConsumesRealParts(){
  var s=new WorldState();s.day=8;
  var t=TownLife.create(s);t.industryStocks.put(StaffingLife.ENABLED,1);
  FactoryNetworkLife.ensure(t);
  var p=FactoryNetworkLife.PLANTS.stream().filter(x->x.id().equals("paper-mill")).findFirst().orElseThrow();
  FactoryNetworkLife.change(t,"logs",3);
  assertTrue(FactoryNetworkLife.produce(t,p));
  assertEquals(93,FactoryMaintenance.condition(t,p));
  t.industryStocks.put(FactoryMaintenance.key(p,"condition"),20);
  int logs=FactoryNetworkLife.stock(t,"logs");
  assertFalse(FactoryNetworkLife.produce(t,p));
  assertEquals(logs,FactoryNetworkLife.stock(t,"logs"));
  var tech=new TownLife.Citizen("repair-tech","Ada","Factory machinist",TownLife.Place.FACTORY);
  tech.home=TownLife.Place.NORTH_RESIDENCES;tech.activity="WORKING";
  t.people.put(tech.id,tech);
  SupplyChainLife.set(t,IndustryLife.FACTORY_PARTS,0);
  assertFalse(FactoryMaintenance.service(t,s,p));
  SupplyChainLife.set(t,IndustryLife.FACTORY_PARTS,1);
  tech.sickUntilDay=8;assertFalse(FactoryMaintenance.service(t,s,p));tech.sickUntilDay=0;
  int cash=t.employerFunds.get(FactoryNetworkLife.account(p)),supplier=t.employerFunds.get("machinist"),wallet=tech.money;
  t.employerFunds.put(FactoryNetworkLife.account(p),0);
  assertFalse(FactoryMaintenance.service(t,s,p));
  t.employerFunds.put(FactoryNetworkLife.account(p),cash);
  assertTrue(FactoryMaintenance.service(t,s,p));
  assertEquals(60,FactoryMaintenance.condition(t,p));
  assertEquals(0,SupplyChainLife.get(t,IndustryLife.FACTORY_PARTS));
  assertEquals(cash-9,t.employerFunds.get(FactoryNetworkLife.account(p)).intValue());
  assertEquals(supplier+5,t.employerFunds.get("machinist").intValue());
  assertEquals(wallet+4,tech.money);
  assertFalse(FactoryMaintenance.service(t,s,p));
  assertEquals(60,FactoryMaintenance.condition(t.copy(),p));
  assertTrue(FactoryNetworkLife.produce(t,p));
 }
}
