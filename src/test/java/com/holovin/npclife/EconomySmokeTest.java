package com.holovin.npclife;

import java.util.ArrayList;

/** Money conservation, privacy, overdue consequences and repayment invariants. */
public final class EconomySmokeTest {
    private EconomySmokeTest() { }

    public static void main(String[] args) {
        var clock=new WorldState();
        var town=TownLife.create(clock);
        PopulationGenerator.add(town,2);
        var lender=town.get("resident-0001");
        var borrower=town.get("resident-0002");
        lender.place=TownLife.Place.PARK;lender.destination=lender.place;
        borrower.place=TownLife.Place.PARK;borrower.destination=borrower.place;
        lender.money=500;borrower.money=15;
        var relationship=new SocialLife.Relationship(lender.id,borrower.id);
        relationship.trust=80;relationship.tension=8;
        town.peerRelationships.put(SocialNetwork.key(lender.id,borrower.id),relationship);
        var events=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();
        int before=lender.money+borrower.money;
        var loan=EconomyLife.issueLoan(town,clock,lender,borrower,60,events,observations);
        check(lender.money+borrower.money==before,"loan does not create money");
        check(loan.original==60&&loan.outstanding==60&&loan.dueDay==8,"contract terms");
        check(events.size()==1 && !events.getFirst().text().contains("60"),"private terms not witnessed");
        check(town.copy().loans.get(loan.id).outstanding==60,"loan survives transactional snapshot");
        boolean rejected=false;
        try {EconomyLife.issueLoan(town,clock,lender,borrower,15,events,observations);}
        catch(IllegalArgumentException ex) {rejected=true;}
        check(rejected,"cannot stack multiple unpaid loans");
        EconomyLife.addBill(town,clock,borrower,140,"test household repairs",events,observations);
        check(borrower.unpaidBills>0,"unpaid household charge accrues when cash is insufficient");
        borrower.money=20;
        clock.day=10;clock.minute=480;
        EconomyLife.tick(town,clock,events,observations);
        check(loan.status==EconomyLife.Status.OVERDUE,"loan becomes overdue");
        check(relationship.trust<80&&relationship.tension>8,"late payment harms relationship");
        long late=town.pendingEconomy.stream().filter(e->e.kind()==EconomyLife.Kind.OVERDUE).count();
        EconomyLife.tick(town,clock,events,observations);
        check(town.pendingEconomy.stream().filter(e->e.kind()==EconomyLife.Kind.OVERDUE).count()==late,
                "late penalty applied only once");
        borrower.money=180;
        int sum=lender.money+borrower.money;
        check(EconomyLife.repay(town,clock,loan,60)==60,"repayment transfers remaining principal");
        check(lender.money+borrower.money==sum,"repayment conserves total cash");
        check(loan.outstanding==0&&loan.status==EconomyLife.Status.PAID,"debt closed");
        check(EconomyLife.repay(town,clock,loan,60)==0,"paid contract cannot pay twice");
        System.out.println("EconomySmokeTest: ALL CHECKS PASSED");
    }

    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError("Failed: "+message);
    }
}
