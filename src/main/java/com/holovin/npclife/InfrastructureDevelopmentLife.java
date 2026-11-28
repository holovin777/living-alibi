package com.holovin.npclife;
import java.util.*;

/** Municipal capital projects, financed from actual tax/municipal cash.
 * These are capacity upgrades to the existing network, not a second utility. */
final class InfrastructureDevelopmentLife {
    private InfrastructureDevelopmentLife(){}
    static final int STAGES=3, PRICE=18;
    record Project(String id,TownLife.Place site,String role){}
    static final Project SOLAR=new Project("solar-extension",TownLife.Place.FARM,"Solar plant operator"),
        NORTH=new Project("north-substation",TownLife.Place.NORTH_RESIDENCES,"North grid technician"),
        SOUTH=new Project("south-substation",TownLife.Place.SOUTH_RESIDENCES,"South grid technician"),
        WATER=new Project("water-extension",TownLife.Place.RIVER,WaterNetworkLife.WATER_ROLE),
        SEWER=new Project("sewer-extension",TownLife.Place.FACTORY,WaterNetworkLife.SEWER_ROLE);
    static final List<Project> PROJECTS=List.of(SOLAR,NORTH,SOUTH,WATER,SEWER);
    private static String key(Project p,String field){return "infrastructure:"+p.id()+":"+field;}
    static int get(TownLife.Town t,Project p,String field){return t.industryStocks.getOrDefault(key(p,field),0);}
    static void set(TownLife.Town t,Project p,String field,int n){t.industryStocks.put(key(p,field),n);}
    static boolean complete(TownLife.Town t,Project p){return get(t,p,"stage")==STAGES;}
    static int homes(TownLife.Town t,TownLife.Place place){
        return (int)ResidentialDevelopmentLife.view(t).stream().filter(v->v.connected()&&v.district().equals(place.name())).count();
    }
    static int priority(TownLife.Town t){return EnergyLife.PRIORITY+homes(t,TownLife.Place.NORTH_RESIDENCES)+homes(t,TownLife.Place.SOUTH_RESIDENCES);}
    static int score(TownLife.Town t,Project p){
        if(complete(t,p))return 0;
        if(p==SOLAR)return EnergyLife.enabled(t)&&(!EnergyLife.publicPower(t)||EnergyLife.get(t,"battery")<=2)?4:0;
        if(p==NORTH||p==SOUTH){
            var feeder=GridDistributionLife.feeder(p.site());
            return EnergyLife.enabled(t)&&(!GridDistributionLife.operational(t,feeder)||homes(t,p.site())>0&&GridDistributionLife.condition(t,feeder)<=65)?5:0;
        }
        if(p==WATER)return WaterNetworkLife.active(t)&&WaterNetworkLife.get(t,"treated")<=2?4:0;
        return WaterNetworkLife.active(t)&&WaterNetworkLife.get(t,"sewage")>=WaterNetworkLife.sewerCapacity(t)*3/4?6:0;
    }
    static boolean authorized(TownLife.Town t,Project p){return get(t,p,"approved")>0;}
    static void observe(TownLife.Town t,WorldState s){
        for(var p:PROJECTS){
            if(get(t,p,"observed")>=s.day)continue;
            int previous=get(t,p,"observed");
            set(t,p,"observed",s.day);
            set(t,p,"pressure",score(t,p)>0?Math.min(365,(previous==s.day-1?get(t,p,"pressure"):0)+1):0);
        }
    }
    static boolean approve(TownLife.Town t,WorldState s,Project p){
        int threshold=ElectionLife.policy(t)==ElectionLife.INFRASTRUCTURE?2:4;
        if(!PROJECTS.contains(p)||authorized(t,p)||complete(t,p)||get(t,p,"pressure")<threshold
            ||PROJECTS.stream().anyMatch(x->authorized(t,x)&&!complete(t,x))
            ||MunicipalLife.balance(t,MunicipalLife.CITY)<MunicipalLife.RESERVE+STAGES*PRICE)return false;
        set(t,p,"approved",s.day);
        ArchiveLife.record(t,"infrastructure-approval:"+p.id(),s.day,s.minute,"Municipality","Approved "+p.id()+" capital project.");
        return true;
    }
    static boolean work(TownLife.Town t,WorldState s,Project p){
        if(!authorized(t,p)||complete(t,p)||t.industryStocks.getOrDefault("construction-crew:last-day",0)>=s.day
            ||MunicipalLife.balance(t,MunicipalLife.CITY)<MunicipalLife.RESERVE+PRICE
            ||FactoryNetworkLife.stock(t,"steel")<1||FactoryNetworkLife.stock(t,"building-material")<1||FuelLife.stock(t)<1)return false;
        var tech=ConstructionCrewLife.worker(t,s,p.role(),null);
        var builder=ConstructionCrewLife.worker(t,s,ConstructionCrewLife.BUILDER,null);
        var driver=ConstructionCrewLife.worker(t,s,ConstructionCrewLife.DRIVER,null);
        if(tech==null||builder==null||driver==null)return false;
        // EUR 4 technician, 4 builder, 2 driver, 4 steel, 2 materials, 2 fuel.
        t.employerFunds.merge(MunicipalLife.CITY,-PRICE,Integer::sum);
        tech.money=Math.addExact(tech.money,4);builder.money=Math.addExact(builder.money,4);driver.money=Math.addExact(driver.money,2);
        tech.fatigue=Math.min(100,tech.fatigue+3);builder.fatigue=Math.min(100,builder.fatigue+4);driver.fatigue=Math.min(100,driver.fatigue+2);
        t.employerFunds.merge("machinist",4,Math::addExact);
        t.employerFunds.merge(FactoryNetworkLife.supplier("building-material"),2,Math::addExact);
        t.employerFunds.merge(FuelLife.ACCOUNT,2,Math::addExact);
        FactoryNetworkLife.change(t,"steel",-1);FactoryNetworkLife.change(t,"building-material",-1);
        SupplyChainLife.set(t,FuelLife.STOCK,FuelLife.stock(t)-1);
        t.industryStocks.put("construction-crew:last-day",s.day);
        set(t,p,"stage",get(t,p,"stage")+1);
        set(t,p,"spent",Math.addExact(get(t,p,"spent"),PRICE));
        if(complete(t,p)){
            if(p==NORTH||p==SOUTH)EnergyLife.set(t,GridDistributionLife.key(GridDistributionLife.feeder(p.site()),"condition"),100);
            ArchiveLife.record(t,"infrastructure-complete:"+p.id(),s.day,s.minute,"Municipality","Completed "+p.id()+" capital project.");
        }
        return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        if(s.minute==660&&t.industryStocks.getOrDefault("infrastructure:review",0)<s.day){
            t.industryStocks.put("infrastructure:review",s.day);
            PROJECTS.stream().sorted(Comparator.comparingInt((Project p)->score(t,p)).reversed())
                .filter(p->score(t,p)>0).anyMatch(p->approve(t,s,p));
        }
        if(s.minute==840)for(var p:PROJECTS)if(work(t,s,p))break;
        if(s.minute==1200)observe(t,s);
    }
    record View(String id,String district,int pressureDays,int approvedDay,int stage,int spent,int totalCost){}
    static List<View> view(TownLife.Town t){
        if(!StaffingLife.enabled(t))return List.of();
        return PROJECTS.stream().map(p->new View(p.id(),p.site().name(),get(t,p,"pressure"),get(t,p,"approved"),
            get(t,p,"stage"),get(t,p,"spent"),STAGES*PRICE)).toList();
    }
}
