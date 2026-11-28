package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FireServiceLifeTest {
    @Test void visualFadesWithSuppressionAndClears(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)throws Exception{
        var graph=dir.resolve("fire.tsv");
        java.nio.file.Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nE\t1\t2\nM\t1\t0\t0\nM\t2\t100\t0\nD\t1\t2\nD\t2\t1\n");
        var map=new GeoProjection(graph);
        map.anchors.put(TownLife.Place.FACTORY,"2");
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        t.employerFunds.put("city-maintenance",100);t.industryStocks.put(FuelLife.STOCK,20);
        WaterNetworkLife.set(t,"enabled",1);WaterNetworkLife.set(t,"treated",8);
        assertNull(map.fireVisual(t,s));
        FireServiceLife.start(t,s,TownLife.Place.FACTORY);
        var first=map.fireVisual(t,s);assertEquals(106,first.x(),.001);
        s.minute+=10;assertTrue(map.fireVisual(t,s).intensity()>first.intensity());
        FireServiceLife.report(t);assertTrue(FireServiceLife.dispatch(t,s));
        s.minute+=FireServiceLife.responseMinutes(TownLife.Place.FACTORY)+12;
        assertTrue(map.fireVisual(t,s).intensity()<.3);
        s.minute+=3;FireServiceLife.tick(t,s);assertNull(map.fireVisual(t,s));
    }
    @Test void dispatchConsumesFiniteWaterAndFuelThenCompletes(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        t.player.place=TownLife.Place.FACTORY;t.player.destination=t.player.place;
        t.employerFunds.put("city-maintenance",100);t.industryStocks.put(FuelLife.STOCK,20);
        WaterNetworkLife.set(t,"enabled",1);WaterNetworkLife.set(t,"treated",0);
        assertTrue(FireServiceLife.start(t,s,t.player.place));
        FireServiceLife.playerReport(t);
        assertFalse(FireServiceLife.dispatch(t,s));
        WaterNetworkLife.set(t,"treated",8);
        int units=FireServiceLife.fuel(t.player.place);
        assertTrue(FireServiceLife.dispatch(t,s));
        assertEquals(4,WaterNetworkLife.get(t,"treated"));assertEquals(20-units,FuelLife.stock(t));
        assertEquals(100-units*FuelLife.PRICE-6,t.employerFunds.get("city-maintenance").intValue());
        assertFalse(FireServiceLife.dispatch(t,s));assertEquals("EN_ROUTE",FireServiceLife.view(t,s).status());
        s.minute+=FireServiceLife.responseMinutes(t.player.place);
        assertEquals("FIGHTING",FireServiceLife.view(t.copy(),s).status());
        s.minute+=15;FireServiceLife.tick(t,s);
        assertEquals("NONE",FireServiceLife.view(t,s).status());assertEquals(1,FireServiceLife.view(t,s).extinguished());
    }
}
