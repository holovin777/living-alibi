package com.holovin.npclife;

import java.util.Comparator;

/** One simulated local operator with finite revenue, power and maintenance.
 * Virtual SIM numbers never correspond to real phone numbers. */
final class TelecomLife {
    private TelecomLife(){}
    static final String ACCOUNT="telecom-operator",NETWORK="Network engineer",SYSTEMS="System administrator";
    static final int FEE=8,TERM=30,WAGE=4,PART_COST=5;
    static boolean role(String job){return NETWORK.equals(job)||SYSTEMS.equals(job);}
    static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault("telecom:"+k,0);}
    static void set(TownLife.Town t,String k,int value){t.industryStocks.put("telecom:"+k,value);}
    static String key(TownLife.Citizen c,String f){return "sim:"+c.id+":"+f;}
    static int condition(TownLife.Town t){return t.industryStocks.getOrDefault("telecom:condition",100);}
    static boolean enabled(TownLife.Town t){return StaffingLife.enabled(t);}
    static int cash(TownLife.Town t){return t.employerFunds.getOrDefault(ACCOUNT,0);}
    static void ensure(TownLife.Town t){
        if(get(t,"initialized")>0)return;
        set(t,"initialized",1);set(t,"condition",100);
        SupplyChainLife.pay(t,MunicipalLife.CITY,ACCOUNT,Math.min(40, MunicipalLife.balance(t,MunicipalLife.CITY)));
    }
    static boolean network(TownLife.Town t){
        return !enabled(t)||GridDistributionLife.power(t,TownLife.Place.OFFICE)&&condition(t)>25;
    }
    static boolean coverage(TownLife.Town t,TownLife.Citizen c){
        return network(t)&&(!enabled(t)||GridDistributionLife.power(t,c.place));
    }
    static boolean connected(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return !enabled(t)||coverage(t,c)&&get(t,key(c,"until"))>=s.day;
    }
    static boolean canSubscribe(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return enabled(t)&&RebirthLife.linkedAdult(t,c)&&RebirthLife.awakePresent(c)&&!NpcJusticeLife.confined(t,c,s)
            &&c.money>=FEE&&get(t,key(c,"until"))<s.day;
    }
    static void subscribe(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(!canSubscribe(t,c,s))throw new IllegalArgumentException("Subscription unavailable.");
        ensure(t);
        c.money-=FEE;t.employerFunds.merge(ACCOUNT,FEE,Math::addExact);
        set(t,key(c,"until"),Math.addExact(s.day,TERM-1));set(t,key(c,"number"),c.generationIndex);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!enabled(t))return;
        if(s.minute==360&&get(t,"billing-day")<s.day){
            ensure(t);set(t,"billing-day",s.day);
            for(var c:t.people.values()){
                if(!c.alive||c.age<18||c.generationIndex<=0||RelationshipBoundaryLife.controlled(t,c)||c.money<FEE+50
                    ||get(t,key(c,"until"))>=s.day||Math.floorMod(c.personalSeed,3)!=0)continue;
                c.money-=FEE;t.employerFunds.merge(ACCOUNT,FEE,Math::addExact);
                set(t,key(c,"until"),Math.addExact(s.day,TERM-1));set(t,key(c,"number"),c.generationIndex);
            }
        }
        if(s.minute!=750||get(t,"service-day")>=s.day)return;
        ensure(t);set(t,"service-day",s.day);
        var workers=t.people.values().stream().filter(c->role(c.occupation)&&c.alive&&c.employed
            &&c.presentAt(TownLife.Place.OFFICE)&&"WORKING".equals(c.activity)&&!EmploymentLife.sick(c,s.day)
            &&!NpcJusticeLife.confined(t,c,s)).sorted(Comparator.comparing(c->c.id)).toList();
        for(var w:workers)if(cash(t)>=WAGE){
            t.employerFunds.merge(ACCOUNT,-WAGE,Math::addExact);w.money=Math.addExact(w.money,WAGE);
            if(NETWORK.equals(w.occupation)&&condition(t)<=60&&cash(t)>=PART_COST&&t.technicalMaterials.getOrDefault("spare-parts",0)>0){
                SupplyChainLife.pay(t,ACCOUNT,MunicipalLife.CITY,PART_COST);
                t.technicalMaterials.merge("spare-parts",-1,Integer::sum);
                set(t,"condition",Math.min(100,condition(t)+40));
            }
        }
        if(GridDistributionLife.power(t,TownLife.Place.OFFICE))set(t,"condition",Math.max(0,condition(t)-1));
        if(!network(t))set(t,"outage-days",Math.addExact(get(t,"outage-days"),1));
    }
    record View(String number,int paidUntil,String status,int condition,boolean canSubscribe,int outageDays){}
    static View view(TownLife.Town t,TownLife.Citizen c,WorldState s){
        int number=get(t,key(c,"number"));
        return new View(number>0?"SIM-"+number:"",get(t,key(c,"until")),
            !enabled(t)?"LEGACY":!coverage(t,c)?"OUTAGE":connected(t,c,s)?"CONNECTED":"NO ACTIVE SIM",
            condition(t),canSubscribe(t,c,s),get(t,"outage-days"));
    }
}
