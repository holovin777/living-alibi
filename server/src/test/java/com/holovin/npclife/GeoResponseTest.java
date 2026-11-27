package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
class GeoResponseTest {
    @TempDir Path folder;
    @Test void directedDispatchAndCollision()throws Exception{
        var graph=folder.resolve("roads.tsv");
        Files.writeString(graph,"N\t1\t0\t0\nN\t2\t100\t0\nN\t3\t100\t100\nE\t1\t2\nE\t2\t3\nM\t1\t0\t0\nM\t2\t100\t0\nM\t3\t100\t100\nD\t1\t2\nD\t2\t3\n");
        var map=new GeoProjection(graph);
        assertEquals("2",GeoResponse.next(map,"1","3"));
        assertNull(GeoResponse.next(map,"3","1"));
        var response=new GeoResponse("test","1",0);
        response.advance(map,"3",150);
        assertEquals(new GeoProjection.Point(100,50),response.position(map));
        assertTrue(GeoResponse.contact(new GeoProjection.Point(0,0),new GeoProjection.Point(8,0),new GeoProjection.Point(4,1)));
        assertFalse(GeoResponse.contact(new GeoProjection.Point(0,0),new GeoProjection.Point(8,0),new GeoProjection.Point(4,4)));
        var s=new WorldState();var town=TownLife.create(s);
        var events=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
        VehicleTheftLife.take(town,s,events,observations);
        JusticeLife.dispatchDetain(town,s,events,observations);
        assertTrue(JusticeLife.confined(town));assertFalse(VehicleTheftLife.active(town));
    }
}
