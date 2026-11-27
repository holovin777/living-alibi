package com.holovin.npclife;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** Persistent waiting requests and up to four parallel resident appointments. The washing machine
 * is owned by the resident (existing interior-laundry state), not by the service firm.
 * Visits use the existing district travel model, not exact interior navigation. */
final class ResidentServiceLife {
    private ResidentServiceLife(){}
    static final int RESERVE=100, START=1170, END=1290, MAX_PARALLEL=4;
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
    static int slot(TownLife.Town t,int i,String f){return ServiceCompanyLife.get(t,"dispatch:"+i+":"+f);}
    static void slot(TownLife.Town t,int i,String f,int value){ServiceCompanyLife.set(t,"dispatch:"+i+":"+f,value);}
    static TownLife.Citizen customer(TownLife.Town t,int i){
        return NpcVehicleCrimeLife.person(t,slot(t,i,"customer"));
    }
    static int active(TownLife.Town t){
        int n=0;for(int i=0;i<MAX_PARALLEL;i++)if(slot(t,i,"day")>0)n++;return n;
    }
    static boolean pending(TownLife.Town t){return active(t)>0;}
    static int queued(TownLife.Town t){
        return (int)t.people.values().stream().filter(c->eligible(t,c)&&get(t,c,"queued-day")>0).count();
    }
    static boolean availableTech(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return (HomeServiceLife.mechanic(t,c)||PlayerServiceBusiness.employee(t,c)&&PlayerServiceBusiness.operating(t,s.day))&&!EmploymentLife.sick(c,s.day)
            &&!NpcJusticeLife.confined(t,c,s)&&c.fatigue<85;
    }
    static TownLife.Place destination(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(s.minute<START||s.minute>=END||c.generationIndex<=0)return null;
        for(int i=0;i<MAX_PARALLEL;i++){
            if(slot(t,i,"day")!=s.day||slot(t,i,"mechanic")!=c.generationIndex)continue;
            if(!availableTech(t,c,s))return null;
            var customer=customer(t,i);
            return eligible(t,customer)?customer.home:null;
        }
        return null;
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
    static boolean needsService(TownLife.Town t,TownLife.Citizen c){
        return eligible(t,c)&&InteriorLife.installed(t,c,"laundry")&&condition(t,c)<=40;
    }
    static void clear(TownLife.Town t,int i){
        for(String f:List.of("customer","mechanic","day","minutes","last-minute","price","contractor"))slot(t,i,f,0);
    }
    static void assign(TownLife.Town t,int i,TownLife.Citizen c,TownLife.Citizen tech,int day){
        clear(t,i);slot(t,i,"customer",c.generationIndex);slot(t,i,"mechanic",tech.generationIndex);slot(t,i,"day",day);
        slot(t,i,"price",fee(t,tech));slot(t,i,"contractor",PlayerServiceBusiness.employee(t,tech)?1:2);
    }
    /** Preserve a previously scheduled appointment when upgrading an old save. */
    static void migrate(TownLife.Town t){
        int day=ServiceCompanyLife.get(t,"resident-day");
        if(day<=0)return;
        var c=NpcVehicleCrimeLife.person(t,ServiceCompanyLife.get(t,"resident-customer"));
        var tech=NpcVehicleCrimeLife.person(t,ServiceCompanyLife.get(t,"resident-mechanic"));
        if(eligible(t,c)){
            set(t,c,"queued-day",day);
            if(tech!=null&&active(t)==0){
                assign(t,0,c,tech,day);
                slot(t,0,"minutes",ServiceCompanyLife.get(t,"resident-minutes"));
                slot(t,0,"last-minute",ServiceCompanyLife.get(t,"resident-last-minute"));
            }
        }
        for(String f:List.of("resident-customer","resident-mechanic","resident-day","resident-minutes","resident-last-minute"))
            ServiceCompanyLife.set(t,f,0);
    }
    static int fee(TownLife.Town t,TownLife.Citizen tech){return PlayerServiceBusiness.employee(t,tech)?PlayerServiceBusiness.price(t):HomeServiceLife.REPAIR_PRICE;}
    static int minimumFee(TownLife.Town t,int day){return PlayerServiceBusiness.operating(t,day)&&!PlayerServiceBusiness.workers(t).isEmpty()?Math.min(HomeServiceLife.REPAIR_PRICE,PlayerServiceBusiness.price(t)):HomeServiceLife.REPAIR_PRICE;}
    static void prepare(TownLife.Town t,WorldState s){
        migrate(t);
        if(!StaffingLife.enabled(t)||s.minute!=360||ServiceCompanyLife.get(t,"resident-prepare-day")>=s.day)return;
        ServiceCompanyLife.set(t,"resident-prepare-day",s.day);
        var people=t.people.values().stream().filter(c->eligible(t,c))
                .sorted(Comparator.comparingInt(c->c.generationIndex)).toList();
        if((s.day-1)%7==0)people.stream().filter(c->!InteriorLife.installed(t,c,"laundry")
                &&c.money>=HomeGoodsLife.product("washer").price()+RESERVE)
                .findFirst().ifPresent(c->buy(t,c));
        for(var c:people){
            if(InteriorLife.installed(t,c,"laundry"))
                set(t,c,"wear",Math.min(100,get(t,c,"wear")+1+Math.floorMod(c.generationIndex,3)));
            if(!needsService(t,c))set(t,c,"queued-day",0);
            else if(c.money>=RESERVE+minimumFee(t,s.day)&&get(t,c,"queued-day")==0)
                set(t,c,"queued-day",s.day);
        }
        if((s.day-1)%7>=5)return;
        // Old requests first; fully broken appliances win ties within the same day.
        var queue=people.stream().filter(c->get(t,c,"queued-day")>0&&needsService(t,c)
                &&c.money>=RESERVE+minimumFee(t,s.day))
            .sorted(Comparator.<TownLife.Citizen>comparingInt(c->get(t,c,"queued-day"))
                .thenComparingInt(c->condition(t,c)).thenComparingInt(c->c.generationIndex)).toList();
        var busy=new HashSet<Integer>();
        var scheduled=new HashSet<Integer>();
        for(int i=0;i<MAX_PARALLEL;i++)if(slot(t,i,"day")>0){
            busy.add(slot(t,i,"mechanic"));scheduled.add(slot(t,i,"customer"));
        }
        for(var c:queue){
            if(scheduled.contains(c.generationIndex)||busy.contains(c.generationIndex))continue;
            int free=-1;
            for(int i=0;i<MAX_PARALLEL;i++)if(slot(t,i,"day")==0){free=i;break;}
            if(free<0)break;
            var tech=people.stream().filter(p->p!=c&&!p.homeUnit.equals(c.homeUnit)
                    &&!busy.contains(p.generationIndex)&&!scheduled.contains(p.generationIndex)
                    &&availableTech(t,p,s)&&c.money>=RESERVE+fee(t,p))
                .min(Comparator.<TownLife.Citizen>comparingInt(p->fee(t,p))
                    .thenComparingInt(p->p.home==c.home?0:1)
                    .thenComparingInt(p->p.generationIndex)).orElse(null);
            if(tech==null)continue;
            assign(t,free,c,tech,s.day);busy.add(tech.generationIndex);scheduled.add(c.generationIndex);
        }
    }
    static void tick(TownLife.Town t,WorldState s){
        for(int i=0;i<MAX_PARALLEL;i++){
            int day=slot(t,i,"day");
            if(day<=0)continue;
            if(s.day>day||s.day==day&&s.minute>=END){
                ServiceCompanyLife.count(t,"missed",1);clear(t,i);continue;
            }
            if(s.day!=day||s.minute<START)continue;
            var c=customer(t,i);
            var tech=NpcVehicleCrimeLife.person(t,slot(t,i,"mechanic"));
            int quoted=slot(t,i,"price");if(quoted==0)quoted=HomeServiceLife.REPAIR_PRICE;
            boolean own=PlayerServiceBusiness.employee(t,tech);
            int contractor=slot(t,i,"contractor");
            boolean ready=(contractor==0||contractor==(own?1:2))&&needsService(t,c)&&tech!=null&&destination(t,tech,s)==c.home
                &&HomeRoutineLife.atHome(c)&&tech.presentAt(c.home)&&"WORKING".equals(tech.activity)
                &&c.money>=RESERVE+quoted&&(own?PlayerServiceBusiness.available(t):ServiceCompanyLife.available(t));
            if(!ready){slot(t,i,"minutes",0);continue;}
            if(slot(t,i,"last-minute")==s.minute)continue;
            slot(t,i,"last-minute",s.minute);
            slot(t,i,"minutes",slot(t,i,"minutes")+1);
            if(slot(t,i,"minutes")<30)continue;
            c.money-=quoted;
            if(own)PlayerServiceBusiness.complete(t,tech,quoted);
            else {ServiceCompanyLife.complete(t,tech);ServiceCompanyLife.count(t,"resident-completed",1);}
            set(t,c,"wear",0);set(t,c,"queued-day",0);clear(t,i);
        }
    }
}
