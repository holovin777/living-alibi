package com.holovin.npclife;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeoWorkTest {
    @Test void onlyExposedInputPrefixesAndBoundedValuesAreAccepted(){
        var view=new GeoWork.View(null,null,List.of(),List.of(new GeoProjection.Choice("rebirth-java-onboard","Start")),
            List.of(new GeoWork.Input("rebirth-java-submit:water-control:","Ticket","Rule","return 0;",180)));
        assertTrue(GeoWork.accepts(view,"rebirth-java-onboard"));
        assertTrue(GeoWork.accepts(view,"rebirth-java-submit:water-control:return Math.min(requested, stock);"));
        assertFalse(GeoWork.accepts(view,"rebirth-java-submit:factory-planning:return 0;"));
        assertFalse(GeoWork.accepts(view,"rebirth-java-submit:water-control: "));
        assertFalse(GeoWork.accepts(view,"rebirth-java-submit:water-control:"+"x".repeat(181)));
        var t=TownLife.create(new WorldState());
        assertTrue(GeoWork.view(t,new WorldState()).actions().isEmpty());
        assertTrue(GeoWork.view(t,new WorldState()).inputs().isEmpty());
    }
}
