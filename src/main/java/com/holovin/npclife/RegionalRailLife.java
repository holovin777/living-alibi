package com.holovin.npclife;
import java.util.*;

/** Regional rail services terminate off-map. Local passengers make return
 * excursions; paid freight is released only after the train returns. */
final class RegionalRailLife {
    private RegionalRailLife(){}
    static final String ACCOUNT="regional-rail", DRIVER="Train driver", FREIGHT="Rail freight operator";
    static final int FARE=8, SEATS=16, CARGO=4, CAPITAL=40, PURCHASE=80, HANDLING=4, WAGE=5;
    record Line(String id,String destination,int minutes,int departure,String input,String buyer){}
    static final List<Line> LINES=List.of(
        new Line("pavia","Pavia",35,540,"limestone","factory-network:cement-works"),
        new Line("milano","Milano",45,780,"components","factory-network:electronics-factory"));
    private static String key(Line l,String field){return "regional-rail:"+l.id()+":"+field;}
    static int get(TownLife.Town t,Line l,String field){return t.industryStocks.getOrDefault(key(l,field),0);}
    static void set(TownLife.Town t,Line l,String field,int n){t.industryStocks.put(key(l,field),n);}
    static int now(WorldState s){return (s.day-1)*1440+s.minute;}
    static boolean active(TownLife.Town t){return t.industryStocks.getOrDefault("regional-rail:enabled",0)>0;}
    static int cash(TownLife.Town t){return t.employerFunds.getOrDefault(ACCOUNT,0);}
    static boolean role(String occupation){return DRIVER.equals(occupation)||FREIGHT.equals(occupation);}
    static boolean enable(TownLife.Town t){
        if(active(t)||MunicipalLife.balance(t,MunicipalLife.CITY)<MunicipalLife.RESERVE+PURCHASE+CAPITAL)return false;
        SupplyChainLife.pay(t,MunicipalLife.CITY,"regional-rolling-stock",PURCHASE);
        SupplyChainLife.pay(t,MunicipalLife.CITY,ACCOUNT,CAPITAL);
        t.industryStocks.put("regional-rail:enabled",1);return true;
    }
    static boolean away(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return t.industryStocks.getOrDefault("regional-rail:away:"+c.id,0)>now(s);
    }
    static boolean candidate(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c.alive&&c.age>=18&&c.generationIndex>0&&c.money>=FARE+50&&c.curiosity>=60
            &&!RelationshipBoundaryLife.controlled(t,c)&&!NpcJusticeLife.confined(t,c,s)&&!EmploymentLife.sick(c,s.day)
            &&!TownLife.inPlayerConversation(t,c)&&!away(t,c,s)&&!"WORKING".equals(c.activity)
            &&c.presentAt(TownLife.Place.STAZIONE);
    }
    static boolean depart(TownLife.Town t,WorldState s,Line l){
        if(!active(t)||get(t,l,"day")>=s.day||get(t,l,"cargo")>0||cash(t)<WAGE+2
            ||LINES.stream().anyMatch(x->get(t,x,"arrival")>now(s)))return false;
        var driver=ConstructionCrewLife.worker(t,s,DRIVER,null);
        var agent=ConstructionCrewLife.worker(t,s,"Station attendant",null);
        if(driver==null||agent==null||!GridDistributionLife.power(t,TownLife.Place.STAZIONE)
            ||!EnergyLife.canBuy(t,ACCOUNT,WAGE+2)||!EnergyLife.buy(t,ACCOUNT))return false;
        t.employerFunds.merge(ACCOUNT,-WAGE-2,Integer::sum);
        driver.money=Math.addExact(driver.money,WAGE);agent.money=Math.addExact(agent.money,2);
        driver.fatigue=Math.min(100,driver.fatigue+5);
        set(t,l,"day",s.day);set(t,l,"departed",now(s));set(t,l,"arrival",now(s)+2*l.minutes()+30);
        set(t,l,"trips",Math.addExact(get(t,l,"trips"),1));set(t,l,"passengers",0);
        var passengers=t.people.values().stream().filter(c->candidate(t,c,s)).sorted(Comparator.comparing(c->c.id)).limit(SEATS).toList();
        for(var c:passengers){
            c.money-=FARE;t.employerFunds.merge(ACCOUNT,FARE,Math::addExact);
            t.industryStocks.put("regional-rail:away:"+c.id,get(t,l,"arrival"));
            c.activity="REGIONAL_TRIP";set(t,l,"passengers",get(t,l,"passengers")+1);
        }
        set(t,l,"tickets",Math.addExact(get(t,l,"tickets"),passengers.size()));
        var handler=ConstructionCrewLife.worker(t,s,FREIGHT,null);
        int quantity=Math.min(CARGO,Math.max(0,4-FactoryNetworkLife.stock(t,l.input())));
        int cost=quantity*2;
        if(handler!=null&&quantity>0&&t.employerFunds.getOrDefault(l.buyer(),0)>=cost+HANDLING+BusinessCreditLife.WORKING_RESERVE){
            SupplyChainLife.pay(t,l.buyer(),FactoryNetworkLife.SUPPLIER,cost);
            SupplyChainLife.pay(t,l.buyer(),ACCOUNT,HANDLING);
            t.employerFunds.merge(ACCOUNT,-2,Integer::sum);handler.money=Math.addExact(handler.money,2);
            set(t,l,"cargo",quantity);
        }
        return true;
    }
    static void arrive(TownLife.Town t,WorldState s,Line l){
        if(get(t,l,"arrival")==0||get(t,l,"arrival")>now(s))return;
        if(get(t,l,"passengers")>0)set(t,l,"passengers",0);
        int cargo=get(t,l,"cargo");
        if(cargo>0){
            // Do not discard a manifest if another delivery filled the store.
            int unload=Math.min(cargo,Math.max(0,12-FactoryNetworkLife.stock(t,l.input())));
            if(unload>0){
                FactoryNetworkLife.change(t,l.input(),unload);
                set(t,l,"cargo",cargo-unload);set(t,l,"delivered",Math.addExact(get(t,l,"delivered"),unload));
            }
        }
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        for(var l:LINES)arrive(t,s,l);
        if(s.minute==480&&t.industryStocks.getOrDefault("regional-rail:review",0)<s.day){
            t.industryStocks.put("regional-rail:review",s.day);
            if(!active(t)&&ConstructionCrewLife.worker(t,s,DRIVER,null)!=null)enable(t);
            if(active(t)&&cash(t)<12&&MunicipalLife.balance(t,MunicipalLife.CITY)>=MunicipalLife.RESERVE+12)
                SupplyChainLife.pay(t,MunicipalLife.CITY,ACCOUNT,12);
        }
        for(var l:LINES)if(s.minute==l.departure())depart(t,s,l);
    }
    record Service(String id,String destination,int departureMinute,int tripMinutes,int fare,boolean enRoute,int passengers,int seats,int cargo,String cargoItem,int delivered,int trips,int tickets){}
    record View(boolean active,int cash,List<Service> services){}
    static View view(TownLife.Town t,WorldState s){
        return new View(active(t),cash(t),LINES.stream().map(l->new Service(l.id(),l.destination(),l.departure(),l.minutes(),FARE,
            get(t,l,"arrival")>now(s),get(t,l,"passengers"),SEATS,get(t,l,"cargo"),l.input(),get(t,l,"delivered"),get(t,l,"trips"),get(t,l,"tickets"))).toList());
    }
}
