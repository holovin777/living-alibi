package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CultureLifeTest {
    @Test void authorRetainsIdentityAndBuyerFundsTheSale(){
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,2);
        var author=people.get(0);var buyer=people.get(1);
        for(var c:people){
            c.age=30;c.health=100;c.fatigue=0;c.hunger=10;c.thirst=10;
            c.home=TownLife.Place.NORTH_RESIDENCES;c.place=TownLife.Place.LIBRARY;c.travelRemaining=0;
            c.activity="FREE_TIME";c.money=100;c.sickUntilDay=0;
        }
        var art=CultureLife.Art.PAINTING;
        CultureLife.set(t,author,CultureLife.field(art,"skill"),50);
        s.day=1;s.minute=1080;
        CultureLife.create(t,author,s,art);
        int serial=CultureLife.get(t,author,CultureLife.field(art,"pending"));
        assertTrue(serial>0);
        assertThrows(IllegalArgumentException.class,()->CultureLife.create(t,author,s,art));
        CultureLife.set(t,author,CultureLife.field(art,"listed"),serial);
        s.day=7;s.minute=1000;
        CultureLife.fair(t,s);
        assertEquals(115,author.money);assertEquals(85,buyer.money);
        assertEquals(author.generationIndex,t.industryStocks.get("culture:work:"+serial+":author"));
        assertEquals(buyer.generationIndex,t.industryStocks.get("culture:work:"+serial+":holder"));
        CultureLife.fair(t,s);assertEquals(115,author.money);
        assertEquals(1,CultureLife.view(t.copy(),author).sales());
    }
}
