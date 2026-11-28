package com.holovin.npclife;

import java.util.*;

/** Fictional business credit, financed from the existing town bank's free cash. */
final class BusinessCreditLife {
    private BusinessCreditLife(){}
    static final int MAX_LOAN=100, MAX_BALANCE=150, BANK_RESERVE=100, WORKING_RESERVE=40, DAILY_INSTALLMENT=10;
    private static String key(String account,String field){return "business-credit:"+account+":"+field;}
    static int get(TownLife.Town t,String account,String field){return t.industryStocks.getOrDefault(key(account,field),0);}
    private static void set(TownLife.Town t,String account,String field,int value){t.industryStocks.put(key(account,field),value);}
    static boolean clerk(TownLife.Town t,WorldState s){
        return t.people.values().stream().anyMatch(c->"Bank clerk".equals(c.occupation)&&c.workplace==TownLife.Place.OFFICE
                &&WorkplaceHierarchy.onDuty(c,s.day)&&!RebirthLife.linkedAdult(t,c)&&!NpcJusticeLife.confined(t,c,s)&&!TownLife.inPlayerConversation(t,c));
    }
    static int offer(TownLife.Town t,String a){return offer(t,a,0);}
    static int offer(TownLife.Town t,String a,int day){
        if(!StaffingLife.enabled(t)||!BusinessSolvencyLife.accounts(t).contains(a)||get(t,a,"balance")>0
            ||BusinessSolvencyLife.distressDays(t,a)<2)return 0;
        int need=BusinessSolvencyLife.outstanding(t,a)+(day>0?TradeCreditLife.overdue(t,a,day):0)+WORKING_RESERVE-t.employerFunds.getOrDefault(a,0);
        int available=Math.max(0,BankLife.free(t)-BANK_RESERVE);
        if(need<=0||need>MAX_LOAN||available<need)return 0;
        return need;
    }
    static boolean lend(TownLife.Town t,WorldState s,String a){
        int amount=offer(t,a,s.day);
        if(amount<=0||!clerk(t,s)||!SupplyChainLife.pay(t,BankLife.ACCOUNT,a,amount))return false;
        set(t,a,"balance",amount);set(t,a,"last-week",(s.day-1)/7);set(t,a,"next-payment",s.day+1);
        set(t,a,"missed",0);set(t,a,"borrowed",Math.addExact(get(t,a,"borrowed"),amount));
        // Wage claims precede discretionary spending.
        BusinessSolvencyLife.settle(t,a);
        TradeCreditLife.settle(t,a,s.day);
        return true;
    }
    static void review(TownLife.Town t,WorldState s,String a){
        if(get(t,a,"review-day")>=s.day)return;
        set(t,a,"review-day",s.day);
        int balance=get(t,a,"balance");if(balance<=0)return;
        int week=(s.day-1)/7,previous=get(t,a,"last-week");
        for(int w=previous;w<week&&balance<MAX_BALANCE;w++){
            int interest=Math.min(MAX_BALANCE-balance,Math.max(1,(balance+24)/25));
            balance+=interest;set(t,a,"interest",Math.addExact(get(t,a,"interest"),interest));
        }
        set(t,a,"last-week",week);set(t,a,"balance",balance);
        if(s.day<get(t,a,"next-payment"))return;
        BusinessSolvencyLife.settle(t,a);
        int due=Math.min(DAILY_INSTALLMENT,balance);
        int spare=Math.max(0,t.employerFunds.getOrDefault(a,0)-WORKING_RESERVE);
        int paid=BusinessSolvencyLife.outstanding(t,a)==0?Math.min(due,spare):0;
        if(paid>0&&SupplyChainLife.pay(t,a,BankLife.ACCOUNT,paid)){
            set(t,a,"balance",balance-paid);set(t,a,"repaid",Math.addExact(get(t,a,"repaid"),paid));
        }
        set(t,a,"missed",paid==due?0:Math.addExact(get(t,a,"missed"),1));
        set(t,a,"next-payment",s.day+1);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        if(s.minute==540)for(String a:BusinessSolvencyLife.accounts(t))review(t,s,a);
        if(s.minute!=600||get(t,"bank","last-loan-day")>=s.day||!clerk(t,s))return;
        var candidates=BusinessSolvencyLife.accounts(t).stream().filter(a->offer(t,a,s.day)>0)
            .sorted(Comparator.<String>comparingInt(a->BusinessSolvencyLife.distressDays(t,a)).reversed().thenComparing(a->a)).toList();
        for(String a:candidates)if(lend(t,s,a)){set(t,"bank","last-loan-day",s.day);break;}
    }
    record Loan(String account,int balance,int missedDays,int borrowed,int repaid,int interest){}
    static List<Loan> view(TownLife.Town t){
        if(!StaffingLife.enabled(t))return List.of();
        return BusinessSolvencyLife.accounts(t).stream().filter(a->get(t,a,"borrowed")>0)
            .map(a->new Loan(a,get(t,a,"balance"),get(t,a,"missed"),get(t,a,"borrowed"),get(t,a,"repaid"),get(t,a,"interest"))).toList();
    }
}
