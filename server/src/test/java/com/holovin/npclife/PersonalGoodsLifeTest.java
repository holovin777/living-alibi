package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.holovin.npclife.TownLife.Place.*;
class PersonalGoodsLifeTest {
    @Test void purchaseLoanGiftAndResaleKeepUniqueTitle() throws Exception {
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);t.player.place=MARKET;t.player.money=50;
        var c=t.get("elena");c.place=MARKET;c.travelRemaining=0;c.activity="FREE_TIME";
        var contact=new PlayerLife.Contact(c.id);contact.knownName="Visitor";contact.trust=95;t.player.contacts.put(c.id,contact);PlayerLife.learnResident(t,c.id);
        PersonalGoodsLife.buy(t,s,"book");assertEquals(42,t.player.money);
        var item=t.items.get("personal-good-1");assertEquals(PersonalGoodsLife.visitor(t),item.ownerId);
        PersonalGoodsLife.exchange(t,s,"lend",item.id,c.id);assertEquals(c.id,item.holderId);assertEquals(PersonalGoodsLife.visitor(t),item.ownerId);
        assertThrows(IllegalArgumentException.class,()->PersonalGoodsLife.sell(t,s,item.id));
        PersonalGoodsLife.exchange(t,s,"return",item.id,c.id);
        PersonalGoodsLife.sell(t,s,item.id);assertEquals(46,t.player.money);
        PersonalGoodsLife.buy(t,s,"book");assertEquals(PersonalGoodsLife.visitor(t),item.ownerId);
        PersonalGoodsLife.exchange(t,s,"gift",item.id,c.id);assertEquals(c.id,item.ownerId);
        assertThrows(IllegalArgumentException.class,()->PersonalGoodsLife.sell(t,s,item.id));
        assertEquals(c.id,t.copy().items.get(item.id).ownerId);
        assertTrue(PersonalGoodsLife.view(t,s).items().isEmpty());
    }
}
