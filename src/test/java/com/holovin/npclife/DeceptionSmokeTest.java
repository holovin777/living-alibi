package com.holovin.npclife;

import java.util.ArrayList;

/** Hidden private information and false testimony must not rewrite objective history. */
public final class DeceptionSmokeTest {
    private DeceptionSmokeTest() { }
    public static void main(String[] args) {
        var state=new WorldState();state.day=3;state.minute=1050;
        var town=TownLife.create(state);
        var marco=town.get("marco");var elena=town.get("elena");
        put(marco,TownLife.Place.PARK);put(elena,TownLife.Place.MARKET);
        var truth=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
        String actual="Marco Bianchi and Elena Rossi crossed paths at Park.";
        TownLife.publish(town,state,"marco",TownLife.Place.PARK,actual,truth,observations);
        String key=KnowledgeLife.eventKey(truth.getFirst());
        DeceptionLife.mark(town,"marco",key,state.day,state.minute,"PERSONAL_INCIDENT");
        marco.carefulness=90;
        put(elena,TownLife.Place.PARK);
        KnowledgeLife.exchange(town,state,"marco","elena");
        var heard=town.beliefs.get("elena").get(key);
        check(heard!=null && heard.origin().equals("HEARD"),"lie is attributed hearsay");
        check(!heard.statement().equals(actual) && heard.statement().contains("did not"),"false claim differs from truth");
        check(truth.size()==1 && truth.getFirst().text().equals(actual),"world truth unchanged");
        check(town.pendingDecisions.size()==1 && town.pendingDecisions.getFirst().kind().equals("LIE"),
                "deception recorded only for developer");
        check(town.pendingTransfers.size()==1 && town.pendingTransfers.getFirst().statement().equals(heard.statement()),
                "listener hears deceptive wording");
        KnowledgeLife.witnessed(town,truth.getFirst(),"elena");
        var direct=town.beliefs.get("elena").get(key);
        check(direct.origin().equals("DIRECT") && direct.statement().equals(actual) && direct.confidence()==100,
                "later direct observation overrides false hearsay");
        var copy=town.copy();
        check(copy.secrets.get("marco").containsKey(key) && copy.pendingDecisions.size()==1,
                "secrets and pending decisions copied for atomic state updates");
        // A less careful keeper prefers silence to deception.
        var second=new TownLife.Event(state.day,state.minute+1,"marco",TownLife.Place.PARK,
                TownLife.publicSpace(TownLife.Place.PARK),"Marco Bianchi had an argument at Park.");
        String key2=KnowledgeLife.eventKey(second);
        KnowledgeLife.witnessed(town,second,"marco");
        DeceptionLife.mark(town,"marco",key2,state.day,state.minute+1,"PERSONAL_INCIDENT");
        marco.carefulness=55;
        state.minute++;
        KnowledgeLife.exchange(town,state,"marco","elena");
        check(town.pendingDecisions.stream().anyMatch(d->d.kind().equals("WITHHELD")),
                "secret can be withheld instead of lied about");
        check(!town.beliefs.get("elena").containsKey(key2),"withheld event not leaked");
        System.out.println("DeceptionSmokeTest: ALL CHECKS PASSED");
    }
    private static void put(TownLife.Citizen c,TownLife.Place p) {
        c.place=p;c.destination=p;c.travelRemaining=0;c.activity="FREE_TIME";
    }
    private static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
}
