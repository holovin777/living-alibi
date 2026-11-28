package com.holovin.npclife;

import java.util.*;

/** Fire cover for existing business accounts, using shared insurer and agent capacity. */
final class BusinessInsuranceLife {
    private BusinessInsuranceLife(){}
    static final int PREMIUM=24, TERM=30, COMMISSION=2, MIN_FUNDS=80, DEDUCTIBLE=10, LIMIT=120;
    record Business(String account,TownLife.Place district){}
    static List<Business> businesses(TownLife.Town t){
        var all=new ArrayList<Business>();
        all.add(new Business("shop-assistant",TownLife.Place.MARKET));
        all.add(new Business("machinist",TownLife.Place.FACTORY));
        if(StaffingLife.enabled(t))for(var p:FactoryNetworkLife.PLANTS)
            if(p.site()==TownLife.Place.FACTORY)all.add(new Business(FactoryNetworkLife.account(p),p.site()));
        return all.stream().filter(b->t.employerFunds.containsKey(b.account())).toList();
    }
    private static String key(Business b,String field){return "business-insurance:"+b.account()+":"+field;}
    static int get(TownLife.Town t,Business b,String field){return t.industryStocks.getOrDefault(key(b,field),0);}
    private static void set(TownLife.Town t,Business b,String field,int n){t.industryStocks.put(key(b,field),n);}
    static boolean active(TownLife.Town t,WorldState s,Business b){return get(t,b,"until")>s.day;}
    static boolean sell(TownLife.Town t,WorldState s,Business b,TownLife.Citizen agent){
        if(!HouseholdInsuranceLife.onDuty(t,agent,s)||t.employerFunds.getOrDefault(b.account(),0)<MIN_FUNDS
                ||get(t,b,"until")-s.day>7||FireRecoveryLife.closed(t,b.district()))return false;
        t.employerFunds.merge(b.account(),-PREMIUM,Integer::sum);
        t.employerFunds.merge(HomeInsuranceLife.ACCOUNT,PREMIUM-COMMISSION,Math::addExact);
        agent.money=Math.addExact(agent.money,COMMISSION);
        set(t,b,"until",Math.addExact(Math.max(s.day,get(t,b,"until")),TERM));
        set(t,b,"premiums",Math.addExact(get(t,b,"premiums"),PREMIUM));
        return true;
    }
    static void incidentStarted(TownLife.Town t,WorldState s,TownLife.Place district){
        if(district!=TownLife.Place.MARKET&&district!=TownLife.Place.FACTORY)return;
        for(var b:businesses(t))if(b.district()==district&&active(t,s,b)){
            set(t,b,"incident",FireServiceLife.get(t,"started"));set(t,b,"loss",0);
        }
    }
    static void damage(TownLife.Town t,TownLife.Place district,int loss){
        int id=FireServiceLife.get(t,"started");
        if(id<=0||loss<=0||(district!=TownLife.Place.MARKET&&district!=TownLife.Place.FACTORY))return;
        for(var b:businesses(t))if(b.district()==district&&get(t,b,"incident")==id)
            set(t,b,"loss",Math.min(100,get(t,b,"loss")+loss));
    }
    static void incidentEnded(TownLife.Town t){
        int id=FireServiceLife.get(t,"started");if(id<=0)return;
        for(var b:businesses(t))if(get(t,b,"incident")==id){
            int claim=Math.min(LIMIT,Math.max(0,2*get(t,b,"loss")-DEDUCTIBLE));
            set(t,b,"pending",Math.addExact(get(t,b,"pending"),claim));set(t,b,"incident",0);set(t,b,"loss",0);
        }
    }
    static boolean payClaim(TownLife.Town t,WorldState s,Business b,TownLife.Citizen agent){
        if(!HouseholdInsuranceLife.onDuty(t,agent,s))return false;
        int amount=Math.min(get(t,b,"pending"),Math.max(0,t.employerFunds.getOrDefault(HomeInsuranceLife.ACCOUNT,0)));
        if(amount<=0||!SupplyChainLife.pay(t,HomeInsuranceLife.ACCOUNT,b.account(),amount))return false;
        set(t,b,"pending",get(t,b,"pending")-amount);set(t,b,"paid",Math.addExact(get(t,b,"paid"),amount));
        return true;
    }
    record View(int businesses,int insured,int pending,int paid){}
    static View view(TownLife.Town t,WorldState s){
        var all=businesses(t);
        return new View(all.size(),(int)all.stream().filter(b->active(t,s,b)).count(),
                all.stream().mapToInt(b->get(t,b,"pending")).sum(),all.stream().mapToInt(b->get(t,b,"paid")).sum());
    }
}
