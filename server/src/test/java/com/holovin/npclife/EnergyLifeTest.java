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
  assertTrue(EnergyLife.publicPower(t));assertEquals(14,EnergyLife.get(t,"generated"));
  assertEquals(10,EnergyLife.get(t,"battery"));assertEquals(fuel,FuelLife.stock(t));
  EnergyLife.tick(t,s);assertEquals(10,EnergyLife.get(t,"battery"));
  var p=FactoryNetworkLife.PLANTS.get(0);FactoryNetworkLife.change(t,"logs",2);
  int cash=t.employerFunds.get(FactoryNetworkLife.account(p));
  assertTrue(FactoryNetworkLife.produce(t,p,s.day));
  assertEquals(9,EnergyLife.get(t,"battery"));assertEquals(cash-6,t.employerFunds.get(FactoryNetworkLife.account(p)).intValue());
  EnergyLife.set(t,"battery",0);FactoryNetworkLife.change(t,"logs",2);
  assertFalse(FactoryNetworkLife.produce(t,p,s.day));
  assertEquals(2,FactoryNetworkLife.stock(t,"logs"));
  s.day=9;t.people.get("energy-0").sickUntilDay=9;t.people.get("energy-1").sickUntilDay=9;
  EnergyLife.tick(t,s);assertEquals(10,EnergyLife.get(t,"generated"));
  assertEquals(fuel-1,FuelLife.stock(t));assertTrue(EnergyLife.publicPower(t));
  assertEquals(EnergyLife.view(t),EnergyLife.view(t.copy()));
  s.day=10;EnergyLife.set(t,"battery",0);
  for(int i=0;i<3;i++)t.people.get("energy-"+i).sickUntilDay=10;
  EnergyLife.tick(t,s);assertFalse(EnergyLife.publicPower(t));assertEquals(1,EnergyLife.get(t,"outage-days"));
  t.player.ownsHouse=true;assertFalse(HomeApplianceLife.power(t));assertFalse(UtilityLife.status(t).lightsWorking());
 }
}
