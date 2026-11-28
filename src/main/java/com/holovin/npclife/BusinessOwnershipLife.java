package com.holovin.npclife;

import java.util.*;

/** Acquisition of suspended public business accounts, without cancelling existing liabilities. */
final class BusinessOwnershipLife {
    private BusinessOwnershipLife(){}
    static final int SALE_DAYS=5, PRICE=20, CAPITAL_STEP=20, DIVIDEND_RESERVE=80;
    private static String key(String a){return "business-owner:"+a;}
    static int owner(TownLife.Town t,String a){return t.industryStocks.getOrDefault(key(a),0);}
    static boolean yours(TownLife.Town t,String a){return owner(t,a)>0&&owner(t,a)==t.player.incarnation&&t.player.alive;}
    static boolean listed(TownLife.Town t,String a){
        return StaffingLife.enabled(t)&&BusinessSolvencyLife.accounts(t).contains(a)&&owner(t,a)==0
            &&BusinessSolvencyLife.suspended(t,a)&&BusinessSolvencyLife.distressDays(t,a)>=SALE_DAYS;
    }
    static int capital(TownLife.Town t,String a){
        return Math.max(0,Math.addExact(BusinessSolvencyLife.outstanding(t,a),BusinessCreditLife.WORKING_RESERVE)-t.employerFunds.getOrDefault(a,0));
    }
    static int total(TownLife.Town t,String a){return Math.addExact(PRICE,capital(t,a));}
    private static boolean office(TownLife.Town t,WorldState s){
        return StaffingLife.enabled(t)&&t.player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)
            &&t.player.travelRemaining==0&&!t.player.cycling&&t.player.place==TownLife.Place.OFFICE
            &&s.minute>=540&&s.minute+15<=1020;
    }
    static boolean canBuy(TownLife.Town t,WorldState s,String a){return office(t,s)&&listed(t,a)&&t.player.money>=total(t,a);}
    static String buy(TownLife.Town t,WorldState s,String a){
        if(!canBuy(t,s,a))throw new IllegalArgumentException("Business acquisition unavailable or insufficient capital.");
        int funding=capital(t,a);
        t.player.money-=PRICE+funding;
        t.employerFunds.merge("city-maintenance",PRICE,Math::addExact);
        t.employerFunds.merge(a,funding,Math::addExact);
        t.industryStocks.put(key(a),t.player.incarnation);
        BusinessSolvencyLife.settle(t,a);
        return "Acquired / Acquistata "+a+". EUR "+funding+" invested. Existing bank debt remains with the firm.";
    }
    static boolean canInvest(TownLife.Town t,WorldState s,String a){
        return office(t,s)&&yours(t,a)&&BusinessSolvencyLife.accounts(t).contains(a)&&t.player.money>=CAPITAL_STEP;
    }
    static String invest(TownLife.Town t,WorldState s,String a){
        if(!canInvest(t,s,a))throw new IllegalArgumentException("Investment unavailable.");
        t.player.money-=CAPITAL_STEP;t.employerFunds.merge(a,CAPITAL_STEP,Math::addExact);
        BusinessSolvencyLife.settle(t,a);
        return "Invested EUR "+CAPITAL_STEP+" in "+a+".";
    }
    static boolean canDividend(TownLife.Town t,WorldState s,String a){
        return office(t,s)&&yours(t,a)&&!BusinessSolvencyLife.suspended(t,a)
            &&BusinessSolvencyLife.outstanding(t,a)==0&&BusinessCreditLife.get(t,a,"balance")==0
            &&t.employerFunds.getOrDefault(a,0)>=DIVIDEND_RESERVE+CAPITAL_STEP;
    }
    static String dividend(TownLife.Town t,WorldState s,String a){
        if(!canDividend(t,s,a))throw new IllegalArgumentException("Only debt-free firms with sufficient reserves can distribute cash.");
        t.employerFunds.merge(a,-CAPITAL_STEP,Integer::sum);t.player.money=Math.addExact(t.player.money,CAPITAL_STEP);
        return "Received EUR "+CAPITAL_STEP+" owner distribution from "+a+".";
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t)||s.minute!=390)return;
        for(String a:BusinessSolvencyLife.accounts(t))
            if(owner(t,a)>0&&(!t.player.alive||owner(t,a)!=t.player.incarnation))t.industryStocks.put(key(a),0);
    }
    record Firm(String account,boolean listed,boolean owned,int price,int capital,int cash,int wageDebt,int bankDebt){}
    static List<Firm> view(TownLife.Town t){
        if(!StaffingLife.enabled(t))return List.of();
        return BusinessSolvencyLife.accounts(t).stream().filter(a->listed(t,a)||yours(t,a))
            .map(a->new Firm(a,listed(t,a),yours(t,a),PRICE,capital(t,a),t.employerFunds.getOrDefault(a,0),
                BusinessSolvencyLife.outstanding(t,a),BusinessCreditLife.get(t,a,"balance"))).toList();
    }
}
