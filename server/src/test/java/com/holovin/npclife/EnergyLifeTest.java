package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EnergyLifeTest {
 @Test void staffedRenewablesAndFuelBackupServeFiniteDemand(){
  var s=new WorldState();s.day=8;s.minute=690;var t=TownLife.create(s);
  t.industryStocks.put(StaffingLife.ENABLED,1);FactoryNetworkLife.ensure(t);
  EnergyLife.ensure(t);
  t.employerFunds.put(EnergyLife.ACCOUNT,100);
  for(int i=0;i<EnergyLife.STATIONS.size();i++){
   var station=EnergyLife.STATIONS.get(i);
   var c=new TownLife.Citizen("energy-"+i,"Operator",station.role(),station.site());
   c.age=35;c.generationIndex=800+i;c.activity="WORKING";c.fatigue=0;
   c.home=TownLife.Place.NORTH_RESIDENCES;t.people.put(c.id,c);
  }
  int fuel=FuelLife.stock(t);
  EnergyLife.tick(t,s);
  assertTrue(EnergyLife.publicPower(t));assertEquals(24,EnergyLife.get(t,"generated")); // thermal backup covers industrial demand
  assertEquals(20,EnergyLife.get(t,"battery"));assertEquals(fuel-1,FuelLife.stock(t));
  EnergyLife.tick(t,s);assertEquals(20,EnergyLife.get(t,"battery"));
  var p=FactoryNetworkLife.PLANTS.get(0);FactoryNetworkLife.change(t,"logs",2);
  int cash=t.employerFunds.get(FactoryNetworkLife.account(p));
  assertTrue(FactoryNetworkLife.produce(t,p,s.day));
  assertEquals(19,EnergyLife.get(t,"battery"));assertEquals(cash-6,t.employerFunds.get(FactoryNetworkLife.account(p)).intValue());
  EnergyLife.set(t,"battery",0);FactoryNetworkLife.change(t,"logs",2);
  assertFalse(FactoryNetworkLife.produce(t,p,s.day));
  assertEquals(2,FactoryNetworkLife.stock(t,"logs"));
  s.day=9;t.people.get("energy-0").sickUntilDay=9;t.people.get("energy-1").sickUntilDay=9;
  EnergyLife.tick(t,s);assertEquals(10,EnergyLife.get(t,"generated"));
  assertEquals(fuel-2,FuelLife.stock(t));assertTrue(EnergyLife.publicPower(t));
  assertEquals(EnergyLife.view(t),EnergyLife.view(t.copy()));
  s.day=10;EnergyLife.set(t,"battery",0);
  for(int i=0;i<3;i++)t.people.get("energy-"+i).sickUntilDay=10;
  EnergyLife.tick(t,s);assertFalse(EnergyLife.publicPower(t));assertEquals(1,EnergyLife.get(t,"outage-days"));
  t.player.ownsHouse=true;assertFalse(HomeApplianceLife.power(t));assertFalse(UtilityLife.status(t).lightsWorking());
 }
 @Test void isolatedFeederFailureAndPaidRepairDoNotCutOtherDistricts(){
  var s=new WorldState();s.day=8;s.minute=690;var t=TownLife.create(s);
  t.industryStocks.put(StaffingLife.ENABLED,1);EnergyLife.ensure(t);
  EnergyLife.set(t,"public-power",1);EnergyLife.set(t,"battery",10);
  var south=GridDistributionLife.FEEDERS.get(2);
  EnergyLife.set(t,GridDistributionLife.key(south,"condition"),20);
  t.player.ownsHouse=true;
  assertFalse(HomeApplianceLife.power(t));
  assertTrue(GridDistributionLife.power(t,TownLife.Place.NORTH_RESIDENCES));
  assertTrue(GridDistributionLife.power(t,TownLife.Place.PARK));
  assertTrue(GridDistributionLife.power(t,TownLife.Place.FACTORY));
  var tech=new TownLife.Citizen("grid-south","Technician","South grid technician",TownLife.Place.SOUTH_RESIDENCES);
  tech.age=35;tech.generationIndex=850;tech.activity="WORKING";tech.fatigue=0;tech.money=20;
  tech.home=TownLife.Place.NORTH_RESIDENCES;t.people.put(tech.id,tech);
  t.employerFunds.put(EnergyLife.ACCOUNT,50);t.technicalMaterials.put("spare-parts",0);
  GridDistributionLife.tick(t,s);assertFalse(HomeApplianceLife.power(t));
  assertEquals(1,EnergyLife.get(t,GridDistributionLife.key(south,"outage-days")));
  s.day=9;t.technicalMaterials.put("spare-parts",1);
  GridDistributionLife.tick(t,s);GridDistributionLife.tick(t,s);
  assertTrue(HomeApplianceLife.power(t));assertEquals(63,GridDistributionLife.condition(t,south));
  assertEquals(41,EnergyLife.cash(t));assertEquals(24,tech.money);
  assertEquals(0,t.technicalMaterials.get("spare-parts").intValue());
  assertEquals(GridDistributionLife.view(t),GridDistributionLife.view(t.copy()));
  var industry=GridDistributionLife.FEEDERS.get(3);
  EnergyLife.set(t,GridDistributionLife.key(industry,"condition"),10);
  assertFalse(GridDistributionLife.power(t,TownLife.Place.FACTORY));
  assertTrue(HomeApplianceLife.power(t));
 }
}
