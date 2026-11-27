package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ConvictionLifeTest {
    @Test void childExposureDoesNotAssignFaithAndPlayerChoicePersists(){
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,3);
        var a=people.get(0);var b=people.get(1);var child=people.get(2);
        child.age=0;
        ConvictionLife.upbringing(t,child,a,b);
        assertEquals(ConvictionLife.Belief.UNDECIDED,ConvictionLife.belief(t,child));
        child.age=18;child.activity="FREE_TIME";child.travelRemaining=0;
        t.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
        t.industryStocks.put(RebirthLife.CHILD,child.generationIndex);
        DemographyLife.set(t,child,"played-incarnation",t.player.incarnation);
        ConvictionLife.choose(t,child,ConvictionLife.Belief.BUDDHIST);
        s.minute=360;
        for(int day=1;day<=400;day++){s.day=day;ConvictionLife.tick(t,s);}
        assertEquals(ConvictionLife.Belief.BUDDHIST,ConvictionLife.belief(t,child));
        var copy=t.copy();
        assertEquals(ConvictionLife.Belief.BUDDHIST,ConvictionLife.belief(copy,copy.get(child.id)));
        ConvictionLife.choose(t,child,ConvictionLife.Belief.ATHEIST);
        assertEquals(ConvictionLife.Belief.ATHEIST,ConvictionLife.belief(t,child));
    }
}
