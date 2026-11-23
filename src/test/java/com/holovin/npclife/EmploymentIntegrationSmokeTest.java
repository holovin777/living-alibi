package com.holovin.npclife;

import java.util.ArrayList;

/** Regression: job and illness events emerge naturally without fake historical changes. */
public final class EmploymentIntegrationSmokeTest {
    private EmploymentIntegrationSmokeTest() { }
    public static void main(String[] args) {
        var clock=new WorldState();
        var town=TownLife.create(clock);
        PopulationGenerator.add(town,12);
        var truth=new ArrayList<TownLife.Event>();
        var witnessed=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(clock,35L*1440L,s->TownLife.tick(town,s,truth,witnessed));
        long lost=town.pendingLifeEvents.stream().filter(e->e.kind()==EmploymentLife.Kind.JOB_LOSS).count();
        long hired=town.pendingLifeEvents.stream().filter(e->e.kind()==EmploymentLife.Kind.HIRED).count();
        long illness=town.pendingLifeEvents.stream().filter(e->e.kind()==EmploymentLife.Kind.SICK_LEAVE).count();
        long expenses=town.pendingLifeEvents.stream().filter(e->e.kind()==EmploymentLife.Kind.MEDICAL_EXPENSE).count();
        check(lost>0,"job losses arise from the simulated calendar");
        check(hired>0,"job searches can lead to new employment");
        check(illness>0 && illness==expenses,"short illnesses have matching medical expenses");
        for(var c:town.people.values()) {
            check(c.money>=0,"cash remains nonnegative: "+c.id);
            check(c.unpaidBills>=0,"bill balances remain nonnegative: "+c.id);
        }
        check(town.get("marco").employed && town.get("elena").employed,
                "original special-case residents retain their legacy work behavior");
        check(town.pendingLifeEvents.stream().noneMatch(e->e.residentId().equals("marco") ||
                e.residentId().equals("elena")),"legacy residents have no fabricated career history");
        System.out.println("EmploymentIntegrationSmokeTest: ALL CHECKS PASSED ("+
                lost+" job losses, "+hired+" hires, "+illness+" illnesses in 35 days)");
    }
    private static void check(boolean condition,String message) {
        if(!condition)throw new AssertionError("Failed: "+message);
    }
}
