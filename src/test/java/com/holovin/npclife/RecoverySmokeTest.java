package com.holovin.npclife;

import java.util.ArrayList;

/** Evidence-gated searches, real custody, release, and victim protections. */
public final class RecoverySmokeTest {
    private RecoverySmokeTest() { }
    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError("Failed: "+label);
    }
    public static void main(String[] args) {
        var clock=new WorldState();clock.day=14;clock.minute=1050;
        var town=TownLife.create(clock);PopulationGenerator.add(town,6);
        PropertyLife.ensureStarterItems(town,clock);
        for(var p:town.people.values()) {
            p.place=TownLife.Place.PARK;p.destination=p.place;p.travelRemaining=0;
            p.activity="FREE_TIME";p.money=90;
        }
        var offender=town.get("marco");var victim=town.get("elena");
        var c=InvestigationLife.recordIncident(town,clock,TownLife.Place.PARK,offender,victim,
                new ArrayList<>(),new ArrayList<>());
        String id=c.physicalItemId;
        check(id!=null,"case links actual item");
        check(InvestigationLife.search(town,clock,c.id,offender.id).contains("Inspect the scene"),
                "no search without preliminary evidence");
        check(c.searches.isEmpty(),"denied search does not consume investigation budget");
        InvestigationLife.investigate(town,c.id);
        InvestigationLife.interview(town,c.id,"elena");
        InvestigationLife.interview(town,c.id,"marco");
        check(InvestigationLife.search(town,clock,c.id,offender.id).startsWith("MATCH:"),
                "identifiable wallet can be found when custody and evidence match");
        check(c.searches.size()==1,"search recorded exactly once");
        check(InvestigationLife.search(town,clock,c.id,offender.id).contains("already held"),
                "an already seized item cannot be seized again");
        var wallet=town.items.get(id);
        check(wallet.ownerId.equals(victim.id),"legal title stays with victim");
        check(wallet.holderId==null && wallet.storedSpace.equals("evidence:"+c.id) && wallet.stolen,
                "physical evidence held apart from citizen inventory");
        check(InvestigationLife.returnItem(town,clock,c.id).contains("must be solved"),
                "no premature release while the case remains open");
        check(InvestigationLife.accuse(town,c.id,offender.id).startsWith("Case solved:"),
                "a correctly established suspect can close the case");
        check(InvestigationLife.returnItem(town,clock,c.id).contains("returned to"),
                "seized wallet returned to its legal owner");
        check(c.returned,"case retains historically completed return");
        check(wallet.ownerId.equals(victim.id) && victim.id.equals(wallet.holderId) && !wallet.stolen,
                "owner and holder reunited, stolen flag cleared");
        check(PropertyLife.carriedWallet(town,victim)!=null,"victim physically carries returned wallet");
        int before=town.pendingProperty.size();
        check(!InvestigationLife.returnItem(town,clock,c.id).contains("returned to Elena"),
                "return command is idempotent");
        check(town.pendingProperty.size()==before,"duplicate release creates no audit entry");
        check(town.pendingProperty.stream().filter(x->x.action()==PropertyLife.Action.SEIZED).count()==1,
                "exactly one seizure in chain of custody");
        check(town.pendingProperty.stream().filter(x->x.action()==PropertyLife.Action.RETURNED).count()==1,
                "exactly one return in chain of custody");
        clock.day=27;clock.minute=1049;
        check(InvestigationLife.recentlyVictimized(town,clock,"elena",14),
                "repeat victim protected during cooldown");
        clock.day=28;clock.minute=1050;
        check(!InvestigationLife.recentlyVictimized(town,clock,"elena",14),
                "victim cooldown eventually expires");
        check(!InvestigationLife.recentlyVictimized(town,clock,"resident-0001",14),
                "a different resident is not penalized");
        var old=new InvestigationLife.CaseFile("case-legacy",1,1050,TownLife.Place.PARK,
                "elena","marco","wallet",15);
        town.cases.put(old.id,old);
        check(InvestigationLife.search(town,clock,old.id,"marco").contains("historical"),
                "legacy incidents do not gain imaginary physical evidence");
        System.out.println("RecoverySmokeTest: ALL CHECKS PASSED");
    }
}
