package com.holovin.npclife;

import java.util.ArrayList;

/** Simulated payroll, recurring bills and autonomous lending over an actual town calendar. */
public final class EconomyIntegrationSmokeTest {
    private EconomyIntegrationSmokeTest() { }

    public static void main(String[] args) {
        var state=new WorldState();
        state.day=6;state.minute=450; // first weekend: residents have not earned wages yet
        var town=TownLife.create(state);
        PopulationGenerator.add(town,12);
        for(int i=1;i<=12;i++) {
            var person=town.get("resident-%04d".formatted(i));
            person.money=i<=3?500:15;
        }
        // Known, trusted acquaintances make a loan possible, but two people
        // must still physically meet before an actual transfer can occur.
        for(var a:town.people.values())for(var b:town.people.values()) {
            if(a.id.compareTo(b.id)>=0 || a.generationIndex<=0 || b.generationIndex<=0)continue;
            var relation=new SocialLife.Relationship(a.id,b.id);
            relation.trust=80;relation.tension=5;
            town.peerRelationships.put(SocialNetwork.key(a.id,b.id),relation);
        }
        var events=new ArrayList<TownLife.Event>();
        var observed=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(state,14L*1440L,s->TownLife.tick(town,s,events,observed));
        long billed=town.pendingEconomy.stream().filter(e->e.kind()==EconomyLife.Kind.BILL).count();
        long borrowed=town.pendingEconomy.stream().filter(e->e.kind()==EconomyLife.Kind.LOAN).count();
        check(billed>=12,"recurring household bills run on the simulation clock");
        check(borrowed>0,"low-cash residents can autonomously borrow from trusted contacts");
        check(town.loans.size()==borrowed,"loans have unique persistent identities");
        for(var p:town.people.values()) {
            check(p.money>=0,"cash never becomes negative: "+p.id);
            check(p.unpaidBills>=0,"unpaid bills never become negative: "+p.id);
        }
        for(var loan:town.loans.values())
            check(loan.outstanding>=0 && loan.outstanding<=loan.original,"loan principal bounds");
        System.out.println("EconomyIntegrationSmokeTest: ALL CHECKS PASSED ("+
                billed+" bills, "+borrowed+" autonomous loans in 14 days)");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError("Failed: "+message);}
}
