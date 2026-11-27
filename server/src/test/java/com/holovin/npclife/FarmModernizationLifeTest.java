package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FarmModernizationLifeTest {
 @Test void machineryFuelWaterAndVeterinaryCareUseFiniteResources(){
  var s=new WorldState();s.day=8;s.minute=725;var t=TownLife.create(s);
  t.industryStocks.put(StaffingLife.ENABLED,1);AgriFoodLife.ensure(t);FarmModernizationLife.ensure(t);
  t.employerFunds.put(AgriFoodLife.ACCOUNT,100);
  String[] jobs={AgriFoodLife.FIELD,FarmModernizationLife.MECHANIC,FarmModernizationLife.IRRIGATION,FarmModernizationLife.VET};
  for(int i=0;i<jobs.length;i++){
   var c=new TownLife.Citizen("farm-modern-"+i,"Worker",jobs[i],TownLife.Place.FARM);
   c.generationIndex=1200+i;c.activity="WORKING";t.people.put(c.id,c);
  }
  int fuel=FuelLife.stock(t);AgriFoodLife.harvest(t,s);
  assertEquals(fuel-1,FuelLife.stock(t));assertEquals(96,FarmModernizationLife.get(t,"tractor"));
  assertEquals(6,AgriFoodLife.get(t,"wheat"));
  FarmModernizationLife.set(t,"water",0);assertEquals(0,FarmModernizationLife.cropGrowth(t,4,true));
  var river=t.naturalResources.get("river-water");river.stock=10;
  FarmModernizationLife.irrigate(t,s);assertEquals(4,river.stock);assertEquals(6,FarmModernizationLife.water(t));
  FarmModernizationLife.set(t,"tractor",20);t.technicalMaterials.put("spare-parts",1);
  FarmModernizationLife.maintain(t,s);assertEquals(65,FarmModernizationLife.get(t,"tractor"));
  assertEquals(0,t.technicalMaterials.get("spare-parts"));
  FarmModernizationLife.set(t,"herd-health",60);
  assertEquals(2,FarmModernizationLife.milk(t,4));
  FarmModernizationLife.veterinary(t,s);assertEquals(85,FarmModernizationLife.get(t,"herd-health"));
  assertEquals(1,FarmModernizationLife.get(t,"vet-visits"));
  assertEquals(FarmModernizationLife.view(t),FarmModernizationLife.view(t.copy()));
 }
}
