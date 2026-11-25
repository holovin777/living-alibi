package com.holovin.npclife;

/** Earmarked family savings, owned by the contributing partner until spent.
 * Existing personal wallets, child support and housing bills remain authoritative.
 */
final class FamilyBudgetLife {
    private FamilyBudgetLife() {}
    static final int STEP=10, RESERVE=50;
    private static String key(TownLife.Citizen c){return "family-budget:"+c.id+":savings";}
    static int savings(TownLife.Town t,TownLife.Citizen c){return t.industryStocks.getOrDefault(key(c),0);}
    private static void set(TownLife.Town t,TownLife.Citizen c,int n){t.industryStocks.put(key(c),n);}
    record View(int personalSavings,int partnerSavings,int total,int unpaidBills,int children,
                boolean canDeposit,boolean canWithdraw,boolean canPayBills){}
    static View view(TownLife.Town t,TownLife.Citizen c,TownLife.Citizen p){
        if(p==null)return null;
        int own=savings(t,c),other=savings(t,p);
        int children=(int)t.people.values().stream().filter(x->x.alive&&DemographyLife.child(x)&&
                (DemographyLife.get(t,x,"parent-a")==c.generationIndex||DemographyLife.get(t,x,"parent-b")==c.generationIndex||
                 DemographyLife.get(t,x,"parent-a")==p.generationIndex||DemographyLife.get(t,x,"parent-b")==p.generationIndex)).count();
        boolean meeting=RebirthLife.meeting(c,p)&&RebirthLife.together(c,p)&&RebirthLife.atHome(c)&&RebirthLife.atHome(p);
        return new View(own,other,Math.addExact(own,other),Math.addExact(c.unpaidBills,p.unpaidBills),children,
                meeting&&c.money>=RESERVE+STEP,meeting&&own>=STEP,meeting&&own>0&&c.unpaidBills>0);
    }
    static String action(TownLife.Town t,String action,WorldState clock){
        var c=RebirthLife.requireAdult(t);var p=RebirthLife.partner(t,c);
        var v=view(t,c,p);
        if(v==null)throw new IllegalArgumentException("A living partner is required.");
        switch(action){
            case "deposit" -> {
                if(!v.canDeposit())throw new IllegalArgumentException("Meet your partner at a shared home and retain EUR 50.");
                int next=Math.addExact(savings(t,c),STEP);
                c.money-=STEP;set(t,c,next);
                return "Set aside 10 EUR for the family.";
            }
            case "withdraw" -> {
                if(!v.canWithdraw())throw new IllegalArgumentException("Only your own family savings may be withdrawn.");
                c.money=Math.addExact(c.money,STEP);set(t,c,savings(t,c)-STEP);
                return "Returned 10 EUR from your family savings to your wallet.";
            }
            case "bills" -> {
                if(!v.canPayBills())throw new IllegalArgumentException("No payable personal bills or available family savings.");
                int amount=Math.min(STEP,Math.min(savings(t,c),c.unpaidBills));
                set(t,c,savings(t,c)-amount);c.unpaidBills-=amount;
                t.pendingEconomy.add(new EconomyLife.Entry(clock.day,clock.minute,EconomyLife.Kind.BILL_PAYMENT,
                        c.id,"payables",null,amount,"Payment from family savings"));
                return "Paid household bills from family savings.";
            }
            default -> throw new IllegalArgumentException("Unknown family budget action.");
        }
    }
    static void releaseEstate(TownLife.Town t,TownLife.Citizen c){
        int saved=savings(t,c);
        if(saved>0){c.money=Math.addExact(c.money,saved);set(t,c,0);}
    }
}
