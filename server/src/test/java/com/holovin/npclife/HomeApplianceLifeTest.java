package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.holovin.npclife.TownLife.Place.*;
class HomeApplianceLifeTest {
    @Test void refrigerationAndBillsPreserveFoodAndMoney(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);
        t.player.ownsHouse=true;t.player.place=SOUTH_RESIDENCES;t.player.money=40;
        HomeGoodsLife.set(t,"fridge",1);FoodLife.change(t.player,"milk",false,2);
        HomeApplianceLife.transfer(t,s,"milk",true);
        assertEquals(1,FoodLife.quantity(t.player,"milk",false));assertEquals(1,HomeApplianceLife.quantity(t.copy(),"milk"));
        s.day=2;s.minute=0;HomeApplianceLife.tick(t,s);HomeApplianceLife.tick(t,s);
        assertEquals(1,HomeApplianceLife.get(t,"bill"));
        HomeApplianceLife.set(t,"bill",12);assertFalse(HomeApplianceLife.power(t));
        assertThrows(IllegalArgumentException.class,()->HomeApplianceLife.transfer(t,s,"milk",true));
        HomeApplianceLife.pay(t,s);assertEquals(28,t.player.money);assertTrue(HomeApplianceLife.power(t));
        HomeApplianceLife.transfer(t,s,"milk",false);assertEquals(2,FoodLife.quantity(t.player,"milk",false));
    }
    @Test void washerAndNewsConsumeFiniteResources(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);
        t.player.ownsHouse=true;t.player.place=SOUTH_RESIDENCES;
        HomeGoodsLife.set(t,"washer",1);HomeGoodsLife.set(t,"television",1);
        HomeApplianceLife.set(t,"laundry-dirt",24);t.player.bottledWater=2;
        HomeApplianceLife.wash(t,s);assertEquals(100,HomeApplianceLife.cleanliness(t));assertEquals(1,t.player.bottledWater);
        ArchiveLife.record(t,"press-edition:1",1,500,NewspaperLife.CATEGORY,"Local bulletin");
        assertEquals("Local bulletin",HomeApplianceLife.watch(t,s));
        assertEquals(2,HomeApplianceLife.get(t,"bill"));
        assertThrows(IllegalArgumentException.class,()->HomeApplianceLife.watch(t,s));
    }
}
