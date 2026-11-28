package com.holovin.npclife;
import java.util.*;

/** Independent industrial expansion with paid construction and inventory. */
final class IndustrialExpansionLife {
    private IndustrialExpansionLife(){}
    record Plant(String id,String role,String output,boolean heat){}
    static final List<Plant> PLANTS=List.of(
        new Plant("cement-2","Cement expansion operator","cement",true),
        new Plant("materials-2","Materials expansion operator","building-material",false));
    static final int LAND=120,CAPITAL=80,SHORTAGE_DAYS=3,WAGE=4,BATCH=5,WHOLESALE=3;
    private static String key(Plant p,String field){return "industrial-expansion:"+p.id()+":"+field;}
    static int get(TownLife.Town t,Plant p,String field){return t.industryStocks.getOrDefault(key(p,field),0);}
    static void set(TownLife.Town t,Plant p,String field,int n){t.industryStocks.put(key(p,field),n);}
    static String account(Plant p){return "industrial-expansion:"+p.id();}
    static Plant role(String name){return PLANTS.stream().filter(p->p.role().equals(name)).findFirst().orElse(null);}
    static TownLife.Citizen owner(TownLife.Town t,Plant p){return DemographyLife.byIndex(t,get(t,p,"owner"));}
    static int owned(TownLife.Town t,TownLife.Citizen c){return (int)PLANTS.stream().filter(p->owner(t,p)==c).count();}
    static boolean open(TownLife.Town t,Plant p){return get(t,p,"stage")==3;}
    static boolean available(TownLife.Town t,String role){
        var p=role(role);return p==null||open(t,p)&&t.employerFunds.getOrDefault(account(p),0)>=WAGE;
    }
    static boolean demand(TownLife.Town t,Plant p){
        boolean construction=CommercialPropertyLife.view(t,1).stream().anyMatch(v->!v.owner().isEmpty()&&v.stage()<3)
            ||ResidentialDevelopmentLife.view(t).stream().anyMatch(v->!v.owner().isEmpty()&&v.stage()<3);
        return construction&&FactoryNetworkLife.stock(t,p.output())<2;
    }
    static void observe(TownLife.Town t,WorldState s,Plant p){
        if(get(t,p,"observed-day")>=s.day)return;
        int previous=get(t,p,"observed-day");
        set(t,p,"observed-day",s.day);
        set(t,p,"shortage",demand(t,p)?Math.min(365,(previous==s.day-1?get(t,p,"shortage"):0)+1):0);
    }
    static boolean invest(TownLife.Town t,WorldState s,Plant p,TownLife.Citizen c){
        if(get(t,p,"owner")!=0||get(t,p,"stage")!=0||get(t,p,"shortage")<SHORTAGE_DAYS
            ||t.employerFunds.containsKey(account(p))||!NpcEnterpriseLife.eligible(t,c,s)
            ||NpcEnterpriseLife.count(t,c)>=NpcEnterpriseLife.MAX_FIRMS
            ||c.money<NpcEnterpriseLife.PERSONAL_RESERVE+LAND+ConstructionCrewLife.total()+CAPITAL)return false;
        c.money-=LAND+CAPITAL;t.employerFunds.merge("city-maintenance",LAND,Math::addExact);
        t.employerFunds.put(account(p),CAPITAL);set(t,p,"owner",c.generationIndex);return true;
    }
    static boolean build(TownLife.Town t,WorldState s,Plant p){
        var c=owner(t,p);int stage=get(t,p,"stage");
        if(c==null||!c.alive||c.age<18||RelationshipBoundaryLife.controlled(t,c)||stage>=3
            ||FactoryNetworkLife.stock(t,"steel")<1||FactoryNetworkLife.stock(t,"building-material")<1)return false;
        if(!ConstructionCrewLife.complete(t,s,c,stage))return false;
        set(t,p,"stage",stage+1);return true;
    }
    static boolean produce(TownLife.Town t,WorldState s,Plant p){
        if(!open(t,p)||get(t,p,"production-day")>=s.day||get(t,p,"stock")>5
            ||!GridDistributionLife.power(t,TownLife.Place.FACTORY)||FireRecoveryLife.closed(t,TownLife.Place.FACTORY))return false;
        var w=ConstructionCrewLife.worker(t,s,p.role(),null);
        String a=account(p);
        // A separate paid import batch, not the incumbent's already purchased inventory.
        boolean heat=p.heat()&&GasNetworkLife.active(t);
        int inputs=4+FuelLife.PRICE+(p.heat()?0:2),other=inputs+WAGE+(heat?GasNetworkLife.PRICE:0);
        if(w==null||FuelLife.stock(t)<1||!SupplyChainLife.attending(t,TownLife.Place.LOGISTICS_DEPOT,"Warehouse worker")
            ||!SupplyChainLife.attending(t,TownLife.Place.QUARRY,"Transport driver")||!p.heat()&&FactoryNetworkLife.stock(t,"cement")<1
            ||t.employerFunds.getOrDefault(a,0)<other||!EnergyLife.canBuy(t,a,other)
            ||heat&&!GasNetworkLife.canBuy(t,a,inputs+WAGE+(EnergyLife.enabled(t)?EnergyLife.PRICE:0)))return false;
        if(!EnergyLife.buy(t,a))return false;
        if(heat&&!GasNetworkLife.buy(t,a))throw new IllegalStateException("Validated gas purchase failed");
        SupplyChainLife.pay(t,a,FactoryNetworkLife.SUPPLIER,4);
        SupplyChainLife.pay(t,a,FuelLife.ACCOUNT,FuelLife.PRICE);
        SupplyChainLife.set(t,FuelLife.STOCK,FuelLife.stock(t)-1);
        if(!p.heat()){
            SupplyChainLife.pay(t,a,FactoryNetworkLife.supplier("cement"),2);
            FactoryNetworkLife.change(t,"cement",-1);
        }
        t.employerFunds.merge(a,-WAGE,Integer::sum);w.money=Math.addExact(w.money,WAGE);
        w.fatigue=Math.min(100,w.fatigue+4);
        set(t,p,"stock",get(t,p,"stock")+BATCH);set(t,p,"production-day",s.day);return true;
    }
    static boolean sell(TownLife.Town t,WorldState s,Plant p){
        if(!open(t,p)||get(t,p,"sale-day")>=s.day||!demand(t,p))return false;
        String distributor=FactoryNetworkLife.supplier(p.output());
        int n=Math.min(get(t,p,"stock"),Math.min(BATCH,12-FactoryNetworkLife.stock(t,p.output())));
        n=Math.min(n,Math.max(0,t.employerFunds.getOrDefault(distributor,0)-BusinessCreditLife.WORKING_RESERVE)/WHOLESALE);
        if(n<=0||!SupplyChainLife.pay(t,distributor,account(p),n*WHOLESALE))return false;
        set(t,p,"stock",get(t,p,"stock")-n);FactoryNetworkLife.change(t,p.output(),n);
        set(t,p,"sale-day",s.day);set(t,p,"sold",Math.addExact(get(t,p,"sold"),n));return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        if(s.minute==390)for(var p:PLANTS){
            var c=owner(t,p);
            if(c!=null&&!c.alive){var h=CommercialPropertyLife.heir(t,c);set(t,p,"owner",h==null?0:h.generationIndex);}
        }
        if(s.minute==660&&s.day%7==0&&BusinessCreditLife.clerk(t,s)){
            var candidates=t.people.values().stream().filter(c->NpcEnterpriseLife.eligible(t,c,s))
                .sorted(Comparator.comparingInt((TownLife.Citizen c)->c.money).reversed().thenComparing(c->c.id)).toList();
            outer:for(var p:PLANTS)for(var c:candidates)if(invest(t,s,p,c))break outer;
        }
        if(s.minute==780)for(var p:PLANTS){produce(t,s,p);sell(t,s,p);}
        if(s.minute==840)for(var p:PLANTS)if(build(t,s,p))break;
        if(s.minute==900)for(var p:PLANTS)observe(t,s,p);
    }
    record View(String id,String owner,String output,int stage,int shortageDays,int stock,int sold,int cash,int workers){}
    static List<View> view(TownLife.Town t){
        if(!StaffingLife.enabled(t))return List.of();
        return PLANTS.stream().map(p->{var c=owner(t,p);
            return new View(p.id(),c==null||!c.alive?"":c.name,p.output(),get(t,p,"stage"),get(t,p,"shortage"),
                get(t,p,"stock"),get(t,p,"sold"),t.employerFunds.getOrDefault(account(p),0),
                (int)t.people.values().stream().filter(w->w.alive&&w.employed&&p.role().equals(w.occupation)).count());
        }).toList();
    }
}
