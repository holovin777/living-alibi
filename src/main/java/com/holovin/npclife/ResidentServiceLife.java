package com.holovin.npclife;

import java.util.Comparator;

/** One bounded resident service appointment at a time. The washing machine
 * is owned by the resident (existing interior-laundry state), not by the service firm.
 * Visits use the existing district travel model, not exact interior navigation. */
final class ResidentServiceLife {
    private ResidentServiceLife(){}
    static final int RESERVE=100, START=1170, END=1290;
    static String key(TownLife.Citizen c,String f){return "service-company:resident:"+c.id+":"+f;}
    static int get(TownLife.Town t,TownLife.Citizen c,String f){return t.industryStocks.getOrDefault(key(c,f),0);}
    static void set(TownLife.Town t,TownLife.Citizen c,String f,int n){t.industryStocks.put(key(c,f),n);}
    static boolean eligible(TownLife.Town t,TownLife.Citizen c){
        return c!=null&&c.alive&&c.age>=18&&c.generationIndex>0&&!RebirthLife.linkedAdult(t,c)
                &&c.home!=null&&c.homeUnit!=null;
    }
    static int condition(TownLife.Town t,TownLife.Citizen c){
        return Math.max(0,100-get(t,c,"wear"));
    }
    static TownLife.Citizen customer(TownLife.Town t){
        return NpcVehicleCrimeLife.person(t,ServiceCompanyLife.get(t,"resident-customer"));
    }
    static boolean pending(TownLife.Town t){return ServiceCompanyLife.get(t,"resident-day")>0;}
    static TownLife.Place destination(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(!pending(t)||ServiceCompanyLife.get(t,"resident-day")!=s.day||s.minute<START||s.minute>=END
                ||ServiceCompanyLife.get(t,"resident-mechanic")!=c.generationIndex
                ||!HomeServiceLife.mechanic(t,c)||EmploymentLife.sick(c,s.day)
                ||NpcJusticeLife.confined(t,c,s)||c.fatigue>=85)return null;
        var customer=customer(t);
        return eligible(t,customer)?customer.home:null;
    }
    static boolean buy(TownLife.Town t,TownLife.Citizen c){
        int price=HomeGoodsLife.product("washer").price();
        if(!eligible(t,c)||InteriorLife.installed(t,c,"laundry")||c.money<price+RESERVE
                ||SupplyChainLife.stock(t,"washer")<1)return false;
        SupplyChainLife.take(t,"washer");
        c.money-=price;t.employerFunds.merge("shop-assistant",price,Math::addExact);
        EducationLife.set(t,c,"interior-laundry",1);set(t,c,"wear",0);
        return true;
    }
    static void prepare(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t)||s.minute!=360||ServiceCompanyLife.get(t,"resident-prepare-day")>=s.day)return;
        ServiceCompanyLife.set(t,"resident-prepare-day",s.day);
        var people=t.people.values().stream().filter(c->eligible(t,c))
                .sorted(Comparator.comparingInt(c->c.generationIndex)).toList();
        if((s.day-1)%7==0)people.stream().filter(c->!InteriorLife.installed(t,c,"laundry")
                &&c.money>=HomeGoodsLife.product("washer").price()+RESERVE)
                .findFirst().ifPresent(c->buy(t,c));
        for(var c:people)if(InteriorLife.installed(t,c,"laundry"))
            set(t,c,"wear",Math.min(100,get(t,c,"wear")+1+Math.floorMod(c.generationIndex,3)));
        if(pending(t)||(s.day-1)%7>=5)return;
        for(var c:people){
            if(!InteriorLife.installed(t,c,"laundry")||condition(t,c)>40||c.money<RESERVE+HomeServiceLife.REPAIR_PRICE)continue;
            var tech=people.stream().filter(p->p!=c&&!p.homeUnit.equals(c.homeUnit)
                    &&HomeServiceLife.mechanic(t,p)&&!EmploymentLife.sick(p,s.day)
                    &&!NpcJusticeLife.confined(t,p,s))
                    .min(Comparator.comparingInt(p->p.generationIndex)).orElse(null);
            if(tech==null)continue;
            ServiceCompanyLife.set(t,"resident-customer",c.generationIndex);
            ServiceCompanyLife.set(t,"resident-mechanic",tech.generationIndex);
            ServiceCompanyLife.set(t,"resident-day",s.day);
            ServiceCompanyLife.set(t,"resident-minutes",0);
            return;
        }
    }
    static void clear(TownLife.Town t){
        for(String f:java.util.List.of("resident-customer","resident-mechanic","resident-day","resident-minutes","resident-last-minute"))
            ServiceCompanyLife.set(t,f,0);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!pending(t))return;
        int day=ServiceCompanyLife.get(t,"resident-day");
        if(s.day>day||s.day==day&&s.minute>=END){
            ServiceCompanyLife.count(t,"missed",1);clear(t);return;
        }
        if(s.day!=day||s.minute<START)return;
        var c=customer(t);
        var tech=NpcVehicleCrimeLife.person(t,ServiceCompanyLife.get(t,"resident-mechanic"));
        boolean ready=eligible(t,c)&&tech!=null&&destination(t,tech,s)==c.home
                &&HomeRoutineLife.atHome(c)&&tech.presentAt(c.home)&&"WORKING".equals(tech.activity)
                &&InteriorLife.installed(t,c,"laundry")&&c.money>=RESERVE+HomeServiceLife.REPAIR_PRICE
                &&ServiceCompanyLife.available(t);
        if(!ready){ServiceCompanyLife.set(t,"resident-minutes",0);return;}
        if(ServiceCompanyLife.get(t,"resident-last-minute")==s.minute)return;
        ServiceCompanyLife.set(t,"resident-last-minute",s.minute);
        ServiceCompanyLife.count(t,"resident-minutes",1);
        if(ServiceCompanyLife.get(t,"resident-minutes")<30)return;
        c.money-=HomeServiceLife.REPAIR_PRICE;
        ServiceCompanyLife.complete(t,tech);
        set(t,c,"wear",0);ServiceCompanyLife.count(t,"resident-completed",1);clear(t);
    }
}
