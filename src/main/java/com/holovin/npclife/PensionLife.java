package com.holovin.npclife;

import java.util.Comparator;
import java.util.List;

/** NPC pension entitlements and payments. Existing city-funded pensions
 * are retained as a backstop, but real pension contributions are used first.
 * Unpaid claims survive a save; deceased residents' future claims stop. */
final class PensionLife {
    private PensionLife(){}
    private static final String PREFIX="pension:", LAST=PREFIX+"last-week",
            PAID=PREFIX+"fund-paid", CITY_PAID=PREFIX+"city-paid";
    private static String due(TownLife.Citizen c){return PREFIX+"due:"+c.id;}
    private static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    private static void set(TownLife.Town t,String key,int n){t.industryStocks.put(key,n);}
    private static boolean retired(TownLife.Citizen c){
        return c.generationIndex>0&&c.alive&&"Retired".equals(c.occupation);
    }
    static int outstanding(TownLife.Town t,TownLife.Citizen c){return get(t,due(c));}
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute!=360)return;
        for(var c:t.people.values())if(!c.alive&&outstanding(t,c)>0)set(t,due(c),0);
        if((s.day-1)%7!=0||get(t,LAST)>=s.day)return;
        set(t,LAST,s.day);
        List<TownLife.Citizen> people=t.people.values().stream().filter(PensionLife::retired)
                .sorted(Comparator.comparing(c->c.id)).toList();
        if(people.isEmpty())return;
        for(var c:people)set(t,due(c),Math.addExact(outstanding(t,c),DemographyLife.WEEKLY_PENSION));
        int fund=t.employerFunds.getOrDefault(EmploymentContractLife.PENSION_ACCOUNT,0);
        int city=MunicipalLife.balance(t,MunicipalLife.CITY);
        int cityAvailable=Math.max(0,city-MunicipalLife.RESERVE);
        long totalDue=people.stream().mapToLong(c->outstanding(t,c)).sum();
        int budget=(int)Math.min(Integer.MAX_VALUE,Math.min(totalDue,(long)fund+cityAvailable));
        int remaining=budget;
        // Proportional allocation, rotating the remainder each week for fairness.
        int offset=Math.floorMod((s.day-1)/7,people.size());
        int[] shares=new int[people.size()];
        for(int i=0;i<people.size();i++){
            shares[i]=(int)((long)budget*outstanding(t,people.get(i))/totalDue);
            remaining-=shares[i];
        }
        for(int n=0;n<people.size()&&remaining>0;n++){
            int i=(offset+n)%people.size();
            if(shares[i]<outstanding(t,people.get(i))){shares[i]++;remaining--;}
        }
        int paid=budget-remaining;
        int fromFund=Math.min(fund,paid),fromCity=paid-fromFund;
        int nextFundPaid=Math.addExact(get(t,PAID),fromFund);
        int nextCityPaid=Math.addExact(get(t,CITY_PAID),fromCity);
        int nextTotal=Math.addExact(DemographyLife.get(t,DemographyLife.PENSIONS),paid);
        for(int i=0;i<people.size();i++)Math.addExact(people.get(i).money,shares[i]);
        for(int i=0;i<people.size();i++){
            var c=people.get(i);c.money+=shares[i];set(t,due(c),outstanding(t,c)-shares[i]);
        }
        t.employerFunds.put(EmploymentContractLife.PENSION_ACCOUNT,fund-fromFund);
        t.employerFunds.put(MunicipalLife.CITY,city-fromCity);
        set(t,PAID,nextFundPaid);set(t,CITY_PAID,nextCityPaid);
        DemographyLife.set(t,DemographyLife.PENSIONS,nextTotal);
    }
    record Report(int fund,int retirees,int weeklyPension,long arrears,int fundPaid,int municipalPaid){}
    static Report report(TownLife.Town t){
        var people=t.people.values().stream().filter(PensionLife::retired).toList();
        return new Report(t.employerFunds.getOrDefault(EmploymentContractLife.PENSION_ACCOUNT,0),
                people.size(),DemographyLife.WEEKLY_PENSION,people.stream().mapToLong(c->outstanding(t,c)).sum(),
                get(t,PAID),get(t,CITY_PAID));
    }
}
