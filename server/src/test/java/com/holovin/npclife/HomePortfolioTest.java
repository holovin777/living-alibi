package com.holovin.npclife;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HomePortfolioTest {
    @TempDir Path folder;
    @Test void additionalHomesRemainOwnedWhenCashRunsOut()throws Exception{
        var s=new WorldState();var t=TownLife.create(s);
        var c=PopulationGenerator.add(t,1).getFirst();
        c.age=35;c.money=2000;c.place=TownLife.Place.OFFICE;c.travelRemaining=0;c.activity="FREE_TIME";
        t.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
        t.industryStocks.put(RebirthLife.CHILD,c.generationIndex);
        DemographyLife.set(t,c,"played-incarnation",t.player.incarnation);
        HomeMarketLife.act(t,"buy-tiglio",600);
        HomeMarketLife.act(t,"buy-rosa",600);
        HomeMarketLife.act(t,"buy-olmo",600);
        assertEquals(3,HomeMarketLife.portfolio(t,c).size());
        assertEquals("unit:home-market-tiglio",c.homeUnit);
        c.money=0;
        var second=HomeMarketLife.LISTINGS.get(1);
        for(int day=1;day<=70;day++)HomeMarketLife.tick(t,day,360);
        assertEquals(3,HomeMarketLife.portfolio(t,c).size());
        assertEquals(10*HomeMarketLife.weeklyUpkeep(second),HomeMarketLife.arrears(t,second));
        assertEquals(0,c.money);
        try(var db=new WorldDatabase(folder.resolve("portfolio.db"))){
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
            var restored=db.loadTown(db.load());var owner=restored.get(c.id);
            assertEquals(3,HomeMarketLife.portfolio(restored,owner).size());
            assertEquals(HomeMarketLife.arrears(t,second),HomeMarketLife.arrears(restored,second));
            owner.money=500;
            HomeMarketLife.tick(restored,71,360);
            assertEquals(0,HomeMarketLife.arrears(restored,second));
            assertEquals(3,HomeMarketLife.portfolio(restored,owner).size());
        }
    }
}
