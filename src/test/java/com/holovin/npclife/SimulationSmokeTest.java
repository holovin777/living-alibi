package com.holovin.npclife;

/** No third-party test framework required; run with java -ea. */
public final class SimulationSmokeTest {
    public static void main(String[] args) {
        WorldState s = new WorldState();
        var e=LifeSimulation.advance(s,600);
        check(s.day==1 && s.minute==1050,"clock advanced 600 minutes");
        check(s.money>=0,"money is nonnegative");
        check(s.food>=0 && s.water>=0,"inventory is nonnegative");
        check(!e.isEmpty(),"life events are produced");
        WorldState later=s.copy();
        LifeSimulation.advance(later,1440*7);
        check(later.day==8,"calendar advances through a week");
        check(later.hunger>=0 && later.hunger<=100,"hunger bounded");
        check(later.stress>=0 && later.stress<=100,"stress bounded");
        WorldState sick=new WorldState();
        sick.day=6;sick.minute=450;sick.health=1;sick.thirst=100;sick.hunger=100;
        sick.food=0;sick.water=0;sick.money=0;
        var death=LifeSimulation.advance(sick,120);
        check(!sick.alive && sick.health==0,"death when health reaches zero");
        check(death.stream().anyMatch(x->x.text().contains("died")),"death is logged");
        int oldDay=sick.day;
        LifeSimulation.advance(sick,1440);
        check(sick.day==oldDay+1 && !sick.alive,"clock continues, deceased NPC does not revive");
        WorldState old=new WorldState();
        old.day=365;old.minute=1439;old.age=36;
        LifeSimulation.advance(old,1);
        check(old.age==37,"birthday updates the age");
        WorldState share=new WorldState();
        share.goal="IMPROVE_HEALTH";share.minute=1000;share.loneliness=0;
        LifeSimulation.advance(share,10);
        check(share.activity.equals("WALKING_FOR_HEALTH"),"strategic goal affects behavior");
        System.out.println("SimulationSmokeTest: ALL CHECKS PASSED");
    }
    private static void check(boolean ok,String msg) {
        if(!ok)throw new AssertionError("Failed: "+msg);
    }
}
