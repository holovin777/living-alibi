package com.holovin.npclife;

import java.util.List;

/** Conserved bank cash, two deposit accounts and a small bounded credit line. */
final class BankLife {
    private BankLife() {}
    static final String ACCOUNT="town-bank", SAVINGS="bank:savings", BUSINESS="bank:business",
            DEBT="bank:debt", LAST_WEEK="bank:last-week", INTEREST="bank:interest-paid";
    static final int STEP=10, LOAN=50, MAX_DEBT=100;
    static final List<String> KEYS=List.of(SAVINGS,BUSINESS,DEBT,LAST_WEEK,INTEREST);
    static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    static void set(TownLife.Town t,String k,int n){t.industryStocks.put(k,n);}
    static int cash(TownLife.Town t){return t.employerFunds.getOrDefault(ACCOUNT,0);}
    static void cash(TownLife.Town t,int n){t.employerFunds.put(ACCOUNT,n);}
    static int deposits(TownLife.Town t){return Math.addExact(get(t,SAVINGS),get(t,BUSINESS));}
    static int free(TownLife.Town t){return Math.max(0,cash(t)-deposits(t));}
    static void ensure(TownLife.Town t){for(String k:KEYS)t.industryStocks.putIfAbsent(k,0);}
    static boolean eligible(TownLife.Town t){return t.player.jobId!=null||MarketStallLife.get(t,MarketStallLife.LICENSE)>0;}
    static String act(TownLife.Town t,String action,int day,int minute){
        var p=t.player;TransportLife.ready(p);
        if(p.place!=TownLife.Place.OFFICE||minute<540||minute>=1020)
            throw new IllegalArgumentException("Visit the bank counter at Offices, 09:00–17:00.");
        if(action.equals("bank-borrow")){
            if(!eligible(t)||get(t,DEBT)>0||free(t)<LOAN)
                throw new IllegalArgumentException("Need a job or registered stall, no outstanding loan and available bank funds.");
            int money=Math.addExact(p.money,LOAN);
            cash(t,cash(t)-LOAN);p.money=money;set(t,DEBT,LOAN);
            set(t,LAST_WEEK,(day-1)/7);
            return "Received a 50 EUR loan; interest is charged at weekly boundaries.";
        }
        if(action.equals("bank-repay")){
            int n=Math.min(STEP,get(t,DEBT));
            if(n==0||p.money<n)throw new IllegalArgumentException("Need an outstanding balance and enough cash.");
            int balance=Math.addExact(cash(t),n);
            p.money-=n;cash(t,balance);set(t,DEBT,get(t,DEBT)-n);
            return "Repaid "+n+" EUR of your loan.";
        }
        String key=action.endsWith("-personal")?SAVINGS:action.endsWith("-business")?BUSINESS:null;
        if(key==null)throw new IllegalArgumentException("Unknown bank action.");
        if(key.equals(BUSINESS)&&MarketStallLife.get(t,MarketStallLife.LICENSE)==0)
            throw new IllegalArgumentException("Register a private stall first.");
        if(action.equals("bank-deposit-personal")||action.equals("bank-deposit-business")){
            if(p.money<STEP)throw new IllegalArgumentException("Need 10 EUR cash.");
            int next=Math.addExact(get(t,key),STEP),balance=Math.addExact(cash(t),STEP);
            p.money-=STEP;cash(t,balance);set(t,key,next);
            return "Deposited 10 EUR.";
        }
        if(action.equals("bank-withdraw-personal")||action.equals("bank-withdraw-business")){
            int n=Math.min(STEP,get(t,key));
            if(n==0||cash(t)<n)throw new IllegalArgumentException("No available deposit balance.");
            int money=Math.addExact(p.money,n);
            p.money=money;cash(t,cash(t)-n);set(t,key,get(t,key)-n);
            return "Withdrew "+n+" EUR.";
        }
        throw new IllegalArgumentException("Unknown bank action.");
    }
    static void tick(TownLife.Town t,int day,int minute){
        if(minute!=360||!t.player.alive)return;
        int week=(day-1)/7;
        if(week<=get(t,LAST_WEEK))return;
        set(t,LAST_WEEK,week);
        int debt=get(t,DEBT);
        if(debt>0)set(t,DEBT,Math.min(MAX_DEBT,debt+Math.max(1,(debt+24)/25)));
        // Deposit interest is a claim on existing bank capital, not newly minted cash.
        int interest=Math.min(free(t),get(t,SAVINGS)/100);
        if(interest>0){
            set(t,SAVINGS,Math.addExact(get(t,SAVINGS),interest));
            set(t,INTEREST,Math.addExact(get(t,INTEREST),interest));
        }
    }
    static void clear(TownLife.Town t){
        int amount=deposits(t),city=Math.addExact(t.employerFunds.getOrDefault("city-maintenance",0),amount);
        cash(t,cash(t)-amount);t.employerFunds.put("city-maintenance",city);
        // Unpaid debt is a loss for the bank; a new visitor does not inherit it.
        for(String k:KEYS)set(t,k,0);
    }
    record Report(int personal,int business,int debt,int bankCash,int lendable,int interestEarned,boolean eligible,boolean businessAvailable) {}
    static Report report(TownLife.Town t){
        return new Report(get(t,SAVINGS),get(t,BUSINESS),get(t,DEBT),cash(t),free(t),get(t,INTEREST),eligible(t),
                MarketStallLife.get(t,MarketStallLife.LICENSE)>0);
    }
}
