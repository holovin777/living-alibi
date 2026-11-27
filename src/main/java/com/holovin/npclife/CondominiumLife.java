package com.holovin.npclife;

/** Shared maintenance reserve: allocate part of an existing housing charge,
 * never add a second utilities bill. All values are fictional game amounts. */
final class CondominiumLife {
    private CondominiumLife(){}
    static final int MONTH=30, CONTRIBUTION=12, LIVING_RESERVE=20;
    private static final String PREFIX="condominium:", RESERVE=PREFIX+"reserve", ARREARS=PREFIX+"arrears";
    private static String monthKey(TownLife.Citizen c){return PREFIX+"unit:"+c.homeUnit+":period";}
    private static String debtKey(TownLife.Citizen c){return PREFIX+"payer:"+c.id+":due";}
    private static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    private static void set(TownLife.Town t,String k,int value){t.industryStocks.put(k,value);}
    private static int period(int day){return (day-1)/MONTH+1;}

    /** Called only when a regular housing bill is due; one share per household per period. */
    static int allocate(TownLife.Town t,WorldState clock,TownLife.Citizen c,int housingCharge){
        if(!c.alive||c.generationIndex<=0||c.age<18||AddressLife.building(c)!=1||
                RentalLife.housed(t,c,clock.day)||housingCharge<=CONTRIBUTION||
                get(t,monthKey(c))>=period(clock.day))return 0;
        set(t,monthKey(c),period(clock.day));
        set(t,debtKey(c),Math.addExact(get(t,debtKey(c)),CONTRIBUTION));
        set(t,ARREARS,Math.addExact(get(t,ARREARS),CONTRIBUTION));
        t.pendingEconomy.add(new EconomyLife.Entry(clock.day,clock.minute,EconomyLife.Kind.BILL,
                c.id,"magnolia-condominium",null,CONTRIBUTION,"Common building maintenance share"));
        return CONTRIBUTION;
    }

    /** Transfer only actual available cash, after ordinary bills and above food reserve. */
    static int collect(TownLife.Town t,WorldState clock,TownLife.Citizen c){
        if(!c.alive||c.age<18)return 0;
        int due=get(t,debtKey(c));
        int amount=Math.min(due,Math.max(0,c.money-LIVING_RESERVE));
        if(amount==0)return 0;
        int balance=Math.addExact(get(t,RESERVE),amount);
        c.money-=amount;
        if(c.id.equals("marco"))clock.money=c.money;
        set(t,debtKey(c),due-amount);
        set(t,ARREARS,get(t,ARREARS)-amount);
        set(t,RESERVE,balance);
        t.pendingEconomy.add(new EconomyLife.Entry(clock.day,clock.minute,EconomyLife.Kind.BILL_PAYMENT,
                c.id,"magnolia-condominium",null,amount,"Paid common building maintenance share"));
        return amount;
    }
    record Report(int reserve,int arrears,int contribution,int periodDays){}
    static Report report(TownLife.Town t){
        return new Report(get(t,RESERVE),get(t,ARREARS),CONTRIBUTION,MONTH);
    }
}
