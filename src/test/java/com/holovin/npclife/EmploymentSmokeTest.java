package com.holovin.npclife;

import java.util.ArrayList;

/** A job loss stops wages; hiring restores work; illness causes private expenses. */
public final class EmploymentSmokeTest {
    private EmploymentSmokeTest() { }
    public static void main(String[] args) {
        var clock=new WorldState();clock.day=2;clock.minute=390;
        var town=TownLife.create(clock);
        PopulationGenerator.add(town,1);
        var person=town.get("resident-0001");
        person.money=200;person.food=4;person.water=4;
        EmploymentLife.loseJob(town,clock,person);
        check(!person.employed,"job lost");
        check(town.pendingLifeEvents.size()==1,"loss logged once");
        var truth=new ArrayList<TownLife.Event>();
        var witnessed=new ArrayList<TownLife.Observation>();
        clock.minute=480;
        person.place=person.workplace;person.destination=person.workplace;
        PopulationLife.tick(town,clock,truth,witnessed);
        check(person.money==200,"no wage paid while unemployed");
        person.place=person.home;person.destination=person.home;person.travelRemaining=0;
        clock.minute=481;
        PopulationLife.tick(town,clock,truth,witnessed);
        check(person.activity.equals("JOB_SEARCH"),"unemployed resident searches instead of working");
        clock.minute=390;
        EmploymentLife.hire(town,clock,person,2); // librarian, Library
        check(person.employed && person.workplace==TownLife.Place.LIBRARY,"new career has a real workplace");
        check(person.jobChanges==1 && person.lastHiredDay==2,"new job tracked");
        person.place=person.workplace;person.destination=person.workplace;
        clock.minute=540;
        PopulationLife.tick(town,clock,truth,witnessed);
        check(person.money==211,"wage resumes after hiring");
        clock.minute=390;
        EmploymentLife.startIllness(town,clock,person,3,250,truth,witnessed);
        check(EmploymentLife.sick(person,2),"sick leave covers first day");
        check(person.sickUntilDay==4,"sick leave has fixed end day");
        check(person.unpaidBills>0,"medical expense produces arrears when unaffordable");
        check(town.pendingEconomy.stream().anyMatch(e->
                e.kind()==EconomyLife.Kind.MEDICAL_BILL && e.toId().equals("clinic")),
                "medical bill is clearly distinguished from rent in the ledger");
        int before=person.money;
        person.place=person.home;person.destination=person.home;person.travelRemaining=0;
        clock.minute=480;
        PopulationLife.tick(town,clock,truth,witnessed);
        check(person.money<=before,"no wage paid during illness");
        check(person.activity.equals("SICK_LEAVE"),"daytime sick leave is not work");
        clock.day=5;clock.minute=390;
        EmploymentLife.tick(town,clock,truth,witnessed);
        check(!EmploymentLife.sick(person,5),"illness ends automatically");
        check(town.pendingLifeEvents.stream().anyMatch(e->e.kind()==EmploymentLife.Kind.RECOVERED),
                "recovery is recorded");
        check(truth.stream().noneMatch(e->e.text().contains("medical costs")),
                "private medical bills do not appear as public town events");
        System.out.println("EmploymentSmokeTest: ALL CHECKS PASSED");
    }
    private static void check(boolean condition,String message) {
        if(!condition)throw new AssertionError("Failed: "+message);
    }
}
