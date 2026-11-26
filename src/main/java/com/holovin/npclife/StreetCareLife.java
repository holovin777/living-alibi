package com.holovin.npclife;

import java.util.List;

/** Public upkeep uses the existing persistent city ledger, not a second economy. */
final class StreetCareLife {
    private StreetCareLife(){}
    static final String CLEANER="Street cleaner", GARDENER="Gardener";
    static final int WAGE=8;
    static final String ACCOUNT="street-care-payroll";
    static final int CORE_RESERVE=500, WEEKLY_GRANT=80;
    private static int get(TownLife.Town t,String key,int fallback){
        return t.industryStocks.getOrDefault("street-care:"+key,fallback);
    }
    private static void put(TownLife.Town t,String key,int value){
        t.industryStocks.put("street-care:"+key,value);
    }
    static boolean crew(TownLife.Citizen c){
        return CLEANER.equals(c.occupation)||GARDENER.equals(c.occupation);
    }
    static TownLife.Place station(TownLife.Citizen c){
        return GARDENER.equals(c.occupation)?TownLife.Place.PARK:TownLife.Place.CENTRO;
    }
    static boolean funded(TownLife.Town t){
        return t.employerFunds.getOrDefault(ACCOUNT,0)>=WAGE;
    }
    static void tick(TownLife.Town t,WorldState clock){
        if(clock.minute!=360||get(t,"last-day",0)>=clock.day)return;
        put(t,"last-day",clock.day);
        if((clock.day-1)%7==0){
            int amount=Math.min(WEEKLY_GRANT,Math.max(0,t.employerFunds.getOrDefault(MunicipalLife.CITY,0)-CORE_RESERVE));
            if(amount>0){
                t.employerFunds.merge(MunicipalLife.CITY,-amount,Integer::sum);
                t.employerFunds.merge(ACCOUNT,amount,Math::addExact);
            }
        }
        put(t,"litter",Math.min(100,get(t,"litter",24)+12));
        put(t,"garden",Math.min(100,get(t,"garden",24)+8));
    }
    static boolean payHourly(TownLife.Town t,TownLife.Citizen c,WorldState clock,
                             List<TownLife.Event> events,List<TownLife.Observation> observations){
        if(!crew(c))return false;
        if(!c.employed||!c.presentAt(station(c))||!"WORKING".equals(c.activity)
                ||EmploymentLife.sick(c,clock.day)||clock.minute%60!=0||!funded(t))return true;
        String dayKey=c.id+":paid-day", minuteKey=c.id+":paid-minute";
        if(get(t,dayKey,0)==clock.day&&get(t,minuteKey,-1)==clock.minute)return true;
        int wallet=Math.addExact(c.money,WAGE);
        t.employerFunds.merge(ACCOUNT,-WAGE,Integer::sum);
        c.money=wallet;
        put(t,dayKey,clock.day);put(t,minuteKey,clock.minute);
        String field=GARDENER.equals(c.occupation)?"garden":"litter";
        put(t,field,Math.max(0,get(t,field,24)-10));
        TownLife.publish(t,clock,c.id,c.place,c.name+" completed municipal upkeep.",events,observations);
        return true;
    }
    record View(int litter,int gardenNeglect){}
    static View view(TownLife.Town t){
        return new View(get(t,"litter",24),get(t,"garden",24));
    }
}
