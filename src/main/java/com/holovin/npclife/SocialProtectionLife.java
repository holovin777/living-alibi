package com.holovin.npclife;

import java.util.Comparator;

/** Fictional short-term means-tested income support, never an unlimited wage.
 * Public reports contain aggregates only; personal eligibility stays private. */
final class SocialProtectionLife {
    private SocialProtectionLife(){}
    static final String ACCOUNT="social-protection";
    static final int WEEKLY_GRANT=30, WAIT_DAYS=2, BENEFIT_DAYS=14,
            UNEMPLOYMENT=8, SICKNESS=10, CASH_LIMIT=100;
    private static final String PREFIX="social-protection:", LAST=PREFIX+"last-day",
            GRANTS=PREFIX+"grants", JOB_PAID=PREFIX+"unemployment-paid", SICK_PAID=PREFIX+"sickness-paid";
    private static String loss(TownLife.Citizen c){return PREFIX+"loss:"+c.id;}
    private static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    private static void set(TownLife.Town t,String k,int n){t.industryStocks.put(k,n);}
    static void jobLost(TownLife.Town t,TownLife.Citizen c,int day){set(t,loss(c),day);}
    static void hired(TownLife.Town t,TownLife.Citizen c){set(t,loss(c),0);}
    static void fund(TownLife.Town t){
        int available=Math.max(0,MunicipalLife.balance(t,MunicipalLife.CITY)-MunicipalLife.RESERVE);
        int grant=Math.min(WEEKLY_GRANT,available);
        if(grant>0)MunicipalLife.grant(t,ACCOUNT,grant,GRANTS);
    }
    private static boolean eligible(TownLife.Citizen c){
        return c.alive&&c.generationIndex>0&&c.age>=18&&c.age<67&&
                DemographyLife.workingAge(c)&&c.money<CASH_LIMIT;
    }
    private static boolean unemployed(TownLife.Town t,TownLife.Citizen c,int day){
        int lost=get(t,loss(c));
        return eligible(c)&&!c.employed&&lost>0&&day-lost>=WAIT_DAYS&&day-lost<WAIT_DAYS+BENEFIT_DAYS;
    }
    private static boolean sick(TownLife.Citizen c,int day){
        return eligible(c)&&c.employed&&EmploymentLife.sick(c,day);
    }
    private static void pay(TownLife.Town t,TownLife.Citizen c,int rate,String metric){
        int balance=t.employerFunds.getOrDefault(ACCOUNT,0);
        int amount=Math.min(balance,Math.min(rate,CASH_LIMIT-c.money));
        if(amount<=0)return;
        int wallet=Math.addExact(c.money,amount),total=Math.addExact(get(t,metric),amount);
        c.money=wallet;t.employerFunds.put(ACCOUNT,balance-amount);set(t,metric,total);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute!=390||get(t,LAST)>=s.day)return;
        set(t,LAST,s.day);
        var candidates=t.people.values().stream().filter(c->sick(c,s.day)||unemployed(t,c,s.day))
                .sorted(Comparator.comparing((TownLife.Citizen c)->!sick(c,s.day))
                        .thenComparingInt(c->c.money).thenComparing(c->c.id)).toList();
        for(var c:candidates){
            if(sick(c,s.day))pay(t,c,SICKNESS,SICK_PAID);
            else if(unemployed(t,c,s.day))pay(t,c,UNEMPLOYMENT,JOB_PAID);
        }
    }
    record Report(int fund,int municipalGrants,int unemploymentPaid,int sicknessPaid,
                  int eligibleUnemployed,int eligibleSick){}
    static Report report(TownLife.Town t,int day){
        return new Report(t.employerFunds.getOrDefault(ACCOUNT,0),get(t,GRANTS),get(t,JOB_PAID),get(t,SICK_PAID),
                (int)t.people.values().stream().filter(c->unemployed(t,c,day)).count(),
                (int)t.people.values().stream().filter(c->sick(c,day)).count());
    }
}
