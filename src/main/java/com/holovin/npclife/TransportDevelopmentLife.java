package com.holovin.npclife;
import java.util.*;
import static com.holovin.npclife.TownLife.Place.*;

/** Municipal bus-stop construction and road maintenance on existing corridors. */
final class TransportDevelopmentLife {
    private TransportDevelopmentLife(){}
    static final int STAGES=2, COST=16, REPAIR=10;
    static final List<TownLife.Place> DISTRICTS=List.of(NORTH_RESIDENCES,SOUTH_RESIDENCES,FACTORY);
    private static String key(TownLife.Place p,String field){return "transport-development:"+p.name()+":"+field;}
    static int get(TownLife.Town t,TownLife.Place p,String f){return t.industryStocks.getOrDefault(key(p,f),0);}
    static void set(TownLife.Town t,TownLife.Place p,String f,int n){t.industryStocks.put(key(p,f),n);}
    static boolean opened(TownLife.Town t,TownLife.Place p){return DISTRICTS.contains(p)&&get(t,p,"stage")==STAGES;}
    static int condition(TownLife.Town t,TownLife.Place p){return t.industryStocks.getOrDefault(key(p,"condition"),100);}
    static int demand(TownLife.Town t,TownLife.Place p){
        int residents=(int)t.people.values().stream().filter(c->c.alive&&c.home==p).count();
        int employees=(int)t.people.values().stream().filter(c->c.alive&&c.employed&&c.workplace==p&&c.home!=p).count();
        return residents+employees;
    }
    static int delay(TownLife.Town t,TownLife.Place a,TownLife.Place b){
        if(!StaffingLife.enabled(t))return 0;
        int worst=Math.min(condition(t,a),condition(t,b));
        return worst<40?6:worst<65?3:0;
    }
    static boolean approve(TownLife.Town t,WorldState s,TownLife.Place p){
        if(!DISTRICTS.contains(p)||get(t,p,"approved")!=0||demand(t,p)<12
            ||MunicipalLife.balance(t,MunicipalLife.CITY)<MunicipalLife.RESERVE+STAGES*COST
            ||DISTRICTS.stream().anyMatch(x->get(t,x,"approved")>0&&!opened(t,x)))return false;
        set(t,p,"approved",s.day);return true;
    }
    static boolean work(TownLife.Town t,WorldState s,TownLife.Place p,boolean repair){
        if(!DISTRICTS.contains(p)||t.industryStocks.getOrDefault("construction-crew:last-day",0)>=s.day)return false;
        if(repair?condition(t,p)>=65:get(t,p,"approved")==0||opened(t,p))return false;
        int cost=repair?REPAIR:COST;
        if(MunicipalLife.balance(t,MunicipalLife.CITY)<MunicipalLife.RESERVE+cost
            ||FactoryNetworkLife.stock(t,"building-material")<1||FuelLife.stock(t)<1)return false;
        var builder=ConstructionCrewLife.worker(t,s,ConstructionCrewLife.BUILDER,null);
        var driver=ConstructionCrewLife.worker(t,s,ConstructionCrewLife.DRIVER,null);
        if(builder==null||driver==null)return false;
        int pay=cost-6;
        t.employerFunds.merge(MunicipalLife.CITY,-cost,Integer::sum);
        builder.money=Math.addExact(builder.money,pay/2);driver.money=Math.addExact(driver.money,pay-pay/2);
        t.employerFunds.merge(FactoryNetworkLife.supplier("building-material"),4,Math::addExact);
        t.employerFunds.merge(FuelLife.ACCOUNT,2,Math::addExact);
        FactoryNetworkLife.change(t,"building-material",-1);
        SupplyChainLife.set(t,FuelLife.STOCK,FuelLife.stock(t)-1);
        t.industryStocks.put("construction-crew:last-day",s.day);
        set(t,p,"spent",Math.addExact(get(t,p,"spent"),cost));
        if(repair){set(t,p,"condition",Math.min(100,condition(t,p)+35));set(t,p,"repairs",get(t,p,"repairs")+1);}
        else{
            set(t,p,"stage",get(t,p,"stage")+1);
            if(opened(t,p))ArchiveLife.record(t,"bus-stop:"+p.name(),s.day,s.minute,"Municipality","New bus stop opened at "+p.label+".");
        }
        return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        if(s.minute==660&&t.industryStocks.getOrDefault("transport-development:review",0)<s.day){
            t.industryStocks.put("transport-development:review",s.day);
            DISTRICTS.stream().sorted(Comparator.comparingInt((TownLife.Place p)->demand(t,p)).reversed()).anyMatch(p->approve(t,s,p));
        }
        if(s.minute==840){
            for(var p:DISTRICTS)if(work(t,s,p,true))return;
            for(var p:DISTRICTS)if(work(t,s,p,false))return;
        }
        if(s.minute==1200&&t.industryStocks.getOrDefault("transport-development:wear",0)<s.day){
            t.industryStocks.put("transport-development:wear",s.day);
            for(var p:DISTRICTS)if(demand(t,p)>0)set(t,p,"condition",Math.max(0,condition(t,p)-Math.min(3,1+demand(t,p)/20)));
        }
    }
    record View(String district,int demand,int condition,int approvedDay,int stage,boolean busStop,int repairs,int spent){}
    static List<View> view(TownLife.Town t){
        if(!StaffingLife.enabled(t))return List.of();
        return DISTRICTS.stream().map(p->new View(p.name(),demand(t,p),condition(t,p),get(t,p,"approved"),get(t,p,"stage"),opened(t,p),get(t,p,"repairs"),get(t,p,"spent"))).toList();
    }
}
