package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FoodSupplyLifeTest {
 @Test void bakeryAndResidentShareRealInventory(){
  var t=TownLife.create(new WorldState());t.retailStock.put("bread",0);t.retailStock.put("flour",3);t.retailStock.put("water",3);
  assertEquals(6,FoodSupplyLife.bake(t,8));assertEquals(0,FoodSupplyLife.stock(t,"flour"));assertEquals(2,FoodSupplyLife.stock(t,"water"));
  var c=t.get("elena");c.food=0;c.money=20;int cash=t.employerFunds.getOrDefault("shop-assistant",0);
  assertEquals(3,FoodSupplyLife.buy(t,c,"bread",3));assertEquals(3,c.food);assertEquals(11,c.money);
  assertEquals(3,FoodSupplyLife.stock(t,"bread"));assertEquals(cash+9,t.employerFunds.get("shop-assistant"));
 }
 @Test void pizzaAndImportsArePaidAndBounded(){
  var t=TownLife.create(new WorldState());t.retailStock.put("pizza",0);t.retailStock.put("flour",2);t.retailStock.put("vegetables",1);t.employerFunds.put("pizzeria",20);
  assertEquals(1,FoodSupplyLife.prepare(t,"pizza",2));assertEquals(0,FoodSupplyLife.stock(t,"vegetables"));assertEquals(17,t.employerFunds.get("pizzeria"));
  assertEquals(0,FoodSupplyLife.prepare(t,"pizza",2));t.retailStock.put("water",0);t.employerFunds.put("shop-assistant",3);
  assertEquals(3,FoodSupplyLife.importGoods(t,"water",10));assertEquals(0,t.employerFunds.get("shop-assistant"));assertEquals(3,t.employerFunds.get(FoodSupplyLife.SUPPLIER));
 }
}
