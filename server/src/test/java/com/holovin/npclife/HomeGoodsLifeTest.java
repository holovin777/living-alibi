package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.holovin.npclife.TownLife.Place.*;
class HomeGoodsLifeTest {
    @Test void goodsRequireCorrectShopAndOwnedHome(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);t.player.money=200;t.player.place=PORTICI;
        assertThrows(IllegalArgumentException.class,()->HomeGoodsLife.buy(t,s,"television"));
        t.player.ownsHouse=true;
        assertThrows(IllegalArgumentException.class,()->HomeGoodsLife.buy(t,s,"shelf"));
        HomeGoodsLife.buy(t,s,"television");assertEquals(135,t.player.money);
        assertTrue(HomeGoodsLife.installed(t.copy(),"television"));
        assertThrows(IllegalArgumentException.class,()->HomeGoodsLife.buy(t,s,"television"));
        assertThrows(IllegalArgumentException.class,()->PersonalGoodsLife.buy(t,s,"book"));
        PersonalGoodsLife.buy(t,s,"clothing");assertEquals(120,t.player.money);
        assertThrows(IllegalArgumentException.class,()->HomeGoodsLife.wear(t,s,true));
        t.player.place=SOUTH_RESIDENCES;HomeGoodsLife.wear(t,s,true);
        assertTrue(HomeGoodsLife.view(t,s).changedOutfit());
        HomeGoodsLife.wear(t,s,false);assertFalse(HomeGoodsLife.view(t,s).changedOutfit());
    }
}
