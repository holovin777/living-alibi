package com.holovin.npclife;

import java.util.ArrayList;

/** Crime-scene evidence is ambiguous; alibis require independent, same-scene interviews. */
public final class AlibiSmokeTest {
    private AlibiSmokeTest() { }
    public static void main(String[] args) {
        var clock=new WorldState();clock.day=7;clock.minute=1050;
        var town=TownLife.create(clock);
        PopulationGenerator.add(town,4);
        for(var p:town.people.values()) {
            if(p.id.equals("marco")||p.id.equals("elena")||p.id.equals("resident-0001"))
                p.place=TownLife.Place.PARK;
            else if(p.id.equals("resident-0002")||p.id.equals("resident-0003"))
                p.place=TownLife.Place.LIBRARY;
            else p.place=p.home;
            p.destination=p.place;p.travelRemaining=0;p.activity="FREE_TIME";p.money=80;
        }
        InvestigationLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
        var c=town.cases.get("case-0001");
        check(c!=null,"case generated");
        check(c.witnesses.size()==3,"only three scene witnesses");
        check(c.incidentSpaces.size()==6,"all contemporaneous locations recorded");
        String clue=InvestigationLife.investigate(town,c.id);
        check(clue.contains("receipt") && clue.contains("fabric"),"ambiguous physical clues");
        check(!clue.contains("initials"),"no culprit-initials leak in new cases");
        check(!InvestigationLife.sceneClue(town,c).contains("initials"),"case summary hides initials");
        check(InvestigationLife.checkAlibi(town,c.id,"resident-0002").contains("Interview"),
                "must first obtain the person's own testimony");
        check(InvestigationLife.interview(town,c.id,"resident-0002").contains("Library"),
                "person can state an alternate public location");
        check(InvestigationLife.checkAlibi(town,c.id,"resident-0002").contains("UNVERIFIED"),
                "self-reported location alone is not corroborated");
        InvestigationLife.interview(town,c.id,"resident-0003");
        String result=InvestigationLife.checkAlibi(town,c.id,"resident-0002");
        check(result.contains("CORROBORATED") && result.contains("Library"),
                "second interviewed, co-located person corroborates alibi");
        InvestigationLife.interview(town,c.id,"resident-0004");
        check(InvestigationLife.checkAlibi(town,c.id,"resident-0004").contains("UNVERIFIED"),
                "private-home alibis cannot be corroborated by nearby households");
        var copy=town.copy();
        copy.cases.get(c.id).alibiChecks.clear();
        check(town.cases.get(c.id).alibiChecks.size()==2,"copy isolation of alibi notes");
        var legacy=new InvestigationLife.CaseFile("legacy",7,1050,
                TownLife.Place.PARK,"marco","elena","wallet",15);
        town.cases.put(legacy.id,legacy);
        check(!InvestigationLife.investigate(town,"legacy").contains("M.R."),
                "do not infer legacy physical clues from offender identity");
        InvestigationLife.interview(town,"legacy","resident-0002");
        check(InvestigationLife.checkAlibi(town,"legacy","resident-0002").contains("pre-v1.9"),
                "old cases have no made-up historical positions");
        System.out.println("AlibiSmokeTest: ALL CHECKS PASSED");
    }
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError("Failed: "+label);}
}
