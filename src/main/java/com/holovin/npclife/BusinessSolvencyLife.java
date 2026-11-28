package com.holovin.npclife;

import java.util.*;

/** Simplified liquidity suspension and conserved wage liabilities in staffed worlds. */
final class BusinessSolvencyLife {
    private BusinessSolvencyLife(){}
    static final int GRACE_DAYS=3;
    static String account(String role){
        if("Market vendor".equals(role))return "shop-assistant";
        if("Factory machinist".equals(role))return "machinist";
        return FactoryNetworkLife.employer(role);
    }
    static int wage(String account){return "machinist".equals(account)?IndustryLife.HOURLY_WAGE:4;}
    static boolean managed(TownLife.Town t,String role){return StaffingLife.enabled(t)&&account(role)!=null;}
    private static String key(String a,String f){return "business-solvency:"+a+":"+f;}
    private static int get(TownLife.Town t,String a,String f){return t.industryStocks.getOrDefault(key(a,f),0);}
    private static void set(TownLife.Town t,String a,String f,int n){t.industryStocks.put(key(a,f),n);}
    static int debt(TownLife.Town t,String a,TownLife.Citizen c){return get(t,a,"debt:"+c.id);}
    static int distressDays(TownLife.Town t,String a){return get(t,a,"days");}
    static boolean suspended(TownLife.Town t,String a){return StaffingLife.enabled(t)&&a!=null&&get(t,a,"suspended")>0;}
    static List<String> accounts(TownLife.Town t){
        var a=new TreeSet<String>();a.add("shop-assistant");a.add("machinist");
        for(var p:FactoryNetworkLife.PLANTS)a.add(FactoryNetworkLife.account(p));
        return a.stream().filter(t.employerFunds::containsKey).toList();
    }
    static int outstanding(TownLife.Town t,String a){
        String prefix=key(a,"debt:");
        return t.industryStocks.entrySet().stream().filter(e->e.getKey().startsWith(prefix)).mapToInt(Map.Entry::getValue).sum();
    }
    static boolean payHour(TownLife.Town t,TownLife.Citizen c,WorldState s){
        String a=account(c.occupation);
        if(!managed(t,c.occupation))return false;
        if(suspended(t,a))return true;
        String marker="hour:"+c.id;
        if(get(t,a,marker)==FireServiceLife.now(s))return true;
        set(t,a,marker,FireServiceLife.now(s));
        int due=BusinessManagementLife.wage(t,a),paid=Math.min(due,Math.max(0,t.employerFunds.getOrDefault(a,0)));
        t.employerFunds.merge(a,-paid,Integer::sum);
        c.money=Math.addExact(c.money,paid);
        EnterpriseGovernanceLife.recordPayroll(t,a,paid);
        if(paid<due)set(t,a,"debt:"+c.id,Math.addExact(debt(t,a,c),due-paid));
        return true;
    }
    static void settle(TownLife.Town t,String a){
        // Arrears remain with their original worker, including after job loss.
        for(var c:t.people.values()){
            if(!c.alive)continue;
            int due=debt(t,a,c),paid=Math.min(due,Math.max(0,t.employerFunds.getOrDefault(a,0)));
            if(paid<=0)continue;
            t.employerFunds.merge(a,-paid,Integer::sum);c.money=Math.addExact(c.money,paid);
            EnterpriseGovernanceLife.recordPayroll(t,a,paid);
            set(t,a,"debt:"+c.id,due-paid);
        }
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t)||s.minute!=390)return;
        for(String a:accounts(t)){
            if(get(t,a,"day")>=s.day)continue;
            set(t,a,"day",s.day);settle(t,a);
            boolean distressed=outstanding(t,a)>0||t.employerFunds.getOrDefault(a,0)<BusinessManagementLife.wage(t,a);
            if(!distressed){set(t,a,"days",0);set(t,a,"suspended",0);continue;}
            int days=Math.addExact(get(t,a,"days"),1);set(t,a,"days",days);
            if(days>=GRACE_DAYS){
                set(t,a,"suspended",1);
                for(var c:t.people.values())if(c.alive&&c.employed&&c.generationIndex>0&&a.equals(account(c.occupation)))
                    EmploymentLife.loseJob(t,s,c);
            }
        }
    }
    record Firm(String account,int cash,int arrears,int distressDays,boolean suspended){}
    static List<Firm> view(TownLife.Town t){
        if(!StaffingLife.enabled(t))return List.of();
        return accounts(t).stream().map(a->new Firm(a,t.employerFunds.getOrDefault(a,0),outstanding(t,a),get(t,a,"days"),suspended(t,a))).toList();
    }
}
