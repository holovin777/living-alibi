package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StaffingLifeTest {
 @Test void largeWorldAssignsRealResidentsWithoutAddingOrReplacingIdentities(){
  var s=new WorldState();var t=TownLife.create(s);
  assertFalse(StaffingLife.establish(t,s,12,1));
  while(DemographyLife.living(t)<224)PopulationGenerator.add(t,Math.min(20,224-(int)DemographyLife.living(t)));
  int size=t.people.size();
  assertTrue(StaffingLife.establish(t,s,224,1));assertEquals(size,t.people.size());
  for(String name:java.util.List.of("Doctor","Pharmacist","Electrician","Plumber","Heating technician","Electronics seller"))
    assertTrue(StaffingLife.count(t,StaffingLife.role(name))>=StaffingLife.role(name).target(),name);
  assertFalse(StaffingLife.establish(t,s,224,0));
  assertTrue(StaffingLife.view(t.copy()).active());
 }
}
