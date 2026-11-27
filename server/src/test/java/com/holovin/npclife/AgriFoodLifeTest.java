package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AgriFoodLifeTest {
 @Test void grainFeedMilkAndFoodUseFiniteInputs(){
  var s=new WorldState();s.day=8;s.minute=725;var t=TownLife.create(s);
  t.industryStocks.put(StaffingLife.ENABLED,1);AgriFoodLife.ensure(t);
  t.employerFunds.put(AgriFoodLife.ACCOUNT,100);
  String[] jobs={AgriFoodLife.FIELD,AgriFoodLife.HERD,AgriFoodLife.MILL,AgriFoodLife.DAIRY,AgriFoodLife.FOOD};
  for(int i=0;i<jobs.length;i++){
   var c=new TownLife.Citizen("agri-"+i,"Worker",jobs[i],i<2?TownLife.Place.FARM:TownLife.Place.FACTORY);
   c.generationIndex=990+i;c.activity="WORKING";t.people.put(c.id,c);
  }
  AgriFoodLife.harvest(t,s);assertEquals(6,AgriFoodLife.get(t,"wheat"));
  AgriFoodLife.mill(t,s);assertEquals(4,AgriFoodLife.get(t,"wheat"));assertEquals(1,AgriFoodLife.get(t,"feed"));
  AgriFoodLife.herd(t,s);assertEquals(4,AgriFoodLife.get(t,"milk"));assertEquals(0,AgriFoodLife.get(t,"feed"));
  AgriFoodLife.dairy(t,s);assertEquals(2,AgriFoodLife.get(t,"milk"));assertEquals(1,AgriFoodLife.get(t,"cheese"));
  AgriFoodLife.food(t,s);assertEquals(2,AgriFoodLife.get(t,"pasta"));assertEquals(1,AgriFoodLife.get(t,"oil"));
  assertEquals(80,AgriFoodLife.cash(t));assertEquals(AgriFoodLife.view(t),AgriFoodLife.view(t.copy()));
  AgriFoodLife.set(t,"feed",0);AgriFoodLife.herd(t,s);assertEquals(1,AgriFoodLife.get(t,"unfed-days"));
  assertEquals(2,AgriFoodLife.get(t,"milk"));
 }
}
