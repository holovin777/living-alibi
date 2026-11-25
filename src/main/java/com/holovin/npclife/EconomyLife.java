package com.holovin.npclife;

import java.util.ArrayList;
import java.util.List;

/** Persistent household expenses and voluntary zero-interest peer loans.
 *  The accounting ledger is authoritative; dialogue about money remains private.
 */
final class EconomyLife {
    private EconomyLife() { }

    enum Kind { BILL, MEDICAL_BILL, BILL_PAYMENT, LOAN, REPAYMENT, OVERDUE }
    enum Status { OPEN, OVERDUE, PAID }

    static final class Loan {
        final String id, lenderId, borrowerId;
        final int original, day, dueDay;
        int outstanding;
        Status status;

        Loan(String id,String lenderId,String borrowerId,int original,int day,int dueDay) {
            if(original<=0 || dueDay<=day || lenderId.equals(borrowerId))
                throw new IllegalArgumentException("Invalid loan contract.");
            this.id=id;this.lenderId=lenderId;this.borrowerId=borrowerId;
            this.original=original;this.outstanding=original;this.day=day;
            this.dueDay=dueDay;this.status=Status.OPEN;
        }
        Loan copy() {
            var l=new Loan(id,lenderId,borrowerId,original,day,dueDay);
            l.outstanding=outstanding;l.status=status;return l;
        }
    }

    record Entry(int day,int minute,Kind kind,String fromId,String toId,
                 String loanId,int amount,String detail) { }

    /** Only generated residents have the new housing expenses. Marco's existing
     *  independent economy must not be charged twice during a migration.
     */
    static void tick(TownLife.Town town,WorldState clock,List<TownLife.Event> truth,
                     List<TownLife.Observation> observations) {
        int minute=clock.minute;
        if(minute==480) {
            for(var person:town.people.values()) {
                if(!person.alive || person.generationIndex<=0)continue;
                if(Math.floorMod(clock.day+person.generationIndex*3,7)==0&&!RentalLife.housed(town,person,clock.day)) {
                    int rent=155 + Math.floorMod((int)person.personalSeed,5)*9;
                    addBill(town,clock,person,rent,"housing and utilities",truth,observations);
                }
                if(Math.floorMod(clock.day+person.generationIndex*11,21)==0) {
                    int repair=45+Math.floorMod((int)(person.personalSeed>>>8),6)*19;
                    addBill(town,clock,person,repair,"household repair",truth,observations);
                }
                // Allow overdue bills to be settled after income arrives. Avoid draining
                // the last EUR 20 required for food and water.
                payBills(town,clock,person,truth,observations);
            }
            for(var loan:new ArrayList<>(town.loans.values())) {
                if(loan.status==Status.PAID)continue;
                var borrower=town.get(loan.borrowerId);
                if(borrower.alive) {
                    int available=Math.max(0,borrower.money-90);
                    if(available>0)repay(town,clock,loan,Math.min(loan.outstanding,available));
                }
                if(loan.outstanding>0 && clock.day>loan.dueDay && loan.status!=Status.OVERDUE) {
                    loan.status=Status.OVERDUE;
                    town.pendingEconomy.add(new Entry(clock.day,minute,Kind.OVERDUE,
                            loan.borrowerId,loan.lenderId,loan.id,loan.outstanding,"Payment overdue"));
                    var relation=town.existingRelationship(loan.lenderId,loan.borrowerId);
                    if(relation!=null) {
                        relation.tension=Math.min(100,relation.tension+12);
                        relation.trust=Math.max(0,relation.trust-9);
                    }
                }
            }
        }
        // Financial conversation only when both residents are co-present in a
        // public place. Check at four times per day, never every simulation tick.
        if(minute==720 || minute==960 || minute==1050 || minute==1110) {
            for(var borrower:town.people.values()) {
                if(!borrower.alive || borrower.generationIndex<=0 ||
                        (borrower.money>=50 && borrower.unpaidBills==0) || hasOpenLoan(town,borrower.id))continue;
                TownLife.Citizen lender=null;
                int score=-1;
                for(var candidate:town.people.values()) {
                    if(!candidate.alive || candidate.id.equals(borrower.id) || candidate.money<210 ||
                            candidate.travelRemaining>0 || borrower.travelRemaining>0 ||
                            candidate.place!=borrower.place ||
                            !borrower.currentSpace().equals(candidate.currentSpace()) ||
                            borrower.currentSpace().startsWith("unit:"))continue;
                    var relationship=town.existingRelationship(candidate.id,borrower.id);
                    if(relationship==null || relationship.trust<46 || relationship.tension>40)continue;
                    int value=relationship.trust*2 + candidate.money/30 - relationship.tension;
                    if(value>score) {lender=candidate;score=value;}
                }
                if(lender!=null) {
                    int amount=Math.min(70,Math.max(25,borrower.unpaidBills+60-borrower.money));
                    issueLoan(town,clock,lender,borrower,amount,truth,observations);
                }
            }
        }
    }

    static void addBill(TownLife.Town town,WorldState clock,TownLife.Citizen p,int amount,
                        String reason,List<TownLife.Event> truth,List<TownLife.Observation> observations) {
        if(amount<=0)throw new IllegalArgumentException("Bills must be positive.");
        p.unpaidBills=Math.addExact(p.unpaidBills,amount);
        boolean medical=reason.equals("medical costs");
        town.pendingEconomy.add(new Entry(clock.day,clock.minute,
                medical?Kind.MEDICAL_BILL:Kind.BILL,p.id,medical?"clinic":"housing",null,
                amount,reason));
        // Do not publish a person's personal financial statement to nearby strangers.
        payBills(town,clock,p,truth,observations);
    }

    static int payBills(TownLife.Town town,WorldState clock,TownLife.Citizen p,
                        List<TownLife.Event> truth,List<TownLife.Observation> observations) {
        int amount=Math.min(p.unpaidBills,Math.max(0,p.money-20));
        if(amount<=0)return 0;
        p.money-=amount;p.unpaidBills-=amount;
        if(p.id.equals("marco"))clock.money=p.money;
        // Arrears are currently pooled; a payment is deliberately not attributed to
        // an individual rent/medical invoice when that allocation is unknown.
        town.pendingEconomy.add(new Entry(clock.day,clock.minute,Kind.BILL_PAYMENT,
                p.id,"payables",null,amount,"Payment on pooled outstanding bills"));
        return amount;
    }

    static boolean hasOpenLoan(TownLife.Town town,String borrowerId) {
        return town.loans.values().stream().anyMatch(l->l.borrowerId.equals(borrowerId) && l.outstanding>0);
    }

    /** Validates consent proxies (trust), real co-location and funds. */
    static Loan issueLoan(TownLife.Town town,WorldState clock,TownLife.Citizen lender,
                          TownLife.Citizen borrower,int amount,List<TownLife.Event> truth,
                          List<TownLife.Observation> observations) {
        if(amount<10 || amount>70 || !lender.alive || !borrower.alive ||
                lender.id.equals(borrower.id) || hasOpenLoan(town,borrower.id) ||
                lender.money-amount<100 || borrower.travelRemaining>0 || lender.travelRemaining>0 ||
                !lender.currentSpace().equals(borrower.currentSpace()) ||
                lender.currentSpace().startsWith("unit:"))
            throw new IllegalArgumentException("The proposed loan cannot be issued.");
        var relation=town.existingRelationship(lender.id,borrower.id);
        if(relation==null || relation.trust<46 || relation.tension>40)
            throw new IllegalArgumentException("These residents are not willing to lend to one another.");
        var loan=new Loan("loan-%04d".formatted(town.loans.size()+1),
                lender.id,borrower.id,amount,clock.day,clock.day+7);
        lender.money-=amount;borrower.money+=amount;
        if(lender.id.equals("marco"))clock.money=lender.money;
        if(borrower.id.equals("marco"))clock.money=borrower.money;
        town.loans.put(loan.id,loan);
        town.pendingEconomy.add(new Entry(clock.day,clock.minute,Kind.LOAN,
                lender.id,borrower.id,loan.id,amount,"Zero-interest personal loan"));
        // To avoid perfect knowledge of private loans, bystanders only hear that
        // a financial conversation occurred, not its amount or contractual terms.
        TownLife.publish(town,clock,borrower.id,borrower.place,
                lender.name+" and "+borrower.name+" discussed a financial matter.",truth,observations);
        return loan;
    }

    /** Money conservation: repayment moves cash from borrower to lender; no money is created. */
    static int repay(TownLife.Town town,WorldState clock,Loan loan,int requested) {
        if(requested<=0 || loan.status==Status.PAID)return 0;
        var borrower=town.get(loan.borrowerId);var lender=town.get(loan.lenderId);
        int amount=Math.min(requested,Math.min(loan.outstanding,Math.max(0,borrower.money)));
        if(amount<=0)return 0;
        borrower.money-=amount;lender.money=Math.addExact(lender.money,amount);
        if(borrower.id.equals("marco"))clock.money=borrower.money;
        if(lender.id.equals("marco"))clock.money=lender.money;
        loan.outstanding-=amount;
        if(loan.outstanding==0)loan.status=Status.PAID;
        town.pendingEconomy.add(new Entry(clock.day,clock.minute,Kind.REPAYMENT,
                loan.borrowerId,loan.lenderId,loan.id,amount,"Principal repayment"));
        return amount;
    }
}
