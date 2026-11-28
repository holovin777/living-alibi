package com.holovin.npclife;
import java.util.*;

/** New households occupy paid-for homes only after a staffed utility connection. */
final class ResidentialDevelopmentLife {
    private ResidentialDevelopmentLife(){}
    static final int LAND=100, CONNECTION=12;
    record Lot(String id,TownLife.Place district){}
    static final List<Lot> LOTS=List.of(new Lot("casa-verde",TownLife.Place.NORTH_RESIDENCES),
        new Lot("casa-olivo",TownLife.Place.SOUTH_RESIDENCES),new Lot("casa-sole",TownLife.Place.NORTH_RESIDENCES));
    private static int get(TownLife.Town t,Lot p,String k){return t.industryStocks.getOrDefault("residential-development:"+p.id()+":"+k,0);}
    private static void set(TownLife.Town t,Lot p,String k,int n){t.industryStocks.put("residential-development:"+p.id()+":"+k,n);}
    static String unit(Lot p){return "unit:development-"+p.id();}
    static TownLife.Citizen owner(TownLife.Town t,Lot p){return DemographyLife.byIndex(t,get(t,p,"owner"));}
    static List<TownLife.Citizen> household(TownLife.Town t,TownLife.Citizen c){
        return t.people.values().stream().filter(x->x.alive&&c.homeUnit.equals(x.homeUnit)).toList();
    }
    static boolean buy(TownLife.Town t,WorldState s,Lot p,TownLife.Citizen c){
        if(!LOTS.contains(p)||get(t,p,"owner")!=0||get(t,p,"stage")!=0||!NpcEnterpriseLife.eligible(t,c,s)
            ||!TownLife.privateUnit(c.id).equals(c.homeUnit)||c.unpaidBills>0
            ||household(t,c).stream().noneMatch(x->x!=c)
            ||household(t,c).stream().anyMatch(x->RelationshipBoundaryLife.controlled(t,x))
            ||LOTS.stream().anyMatch(x->get(t,x,"owner")==c.generationIndex)
            ||c.money<NpcEnterpriseLife.PERSONAL_RESERVE+LAND+ConstructionCrewLife.total()+CONNECTION)return false;
        c.money-=LAND;t.employerFunds.merge("city-maintenance",LAND,Math::addExact);
        set(t,p,"owner",c.generationIndex);return true;
    }
    static boolean build(TownLife.Town t,WorldState s,Lot p){
        var c=owner(t,p);int stage=get(t,p,"stage");
        if(c==null||!c.alive||c.age<18||RelationshipBoundaryLife.controlled(t,c)||stage>=3
            ||get(t,p,"build-day")==s.day||IndustryLife.stock(t,IndustryLife.FACTORY_STEEL)<1
            ||FactoryNetworkLife.stock(t,"building-material")<1)return false;
        if(!ConstructionCrewLife.complete(t,s,c,stage))return false;
        set(t,p,"stage",stage+1);set(t,p,"build-day",s.day);return true;
    }
    static boolean connect(TownLife.Town t,WorldState s,Lot p){
        var c=owner(t,p);
        if(c==null||!c.alive||c.age<18||RelationshipBoundaryLife.controlled(t,c)||get(t,p,"stage")!=3
            ||get(t,p,"connected")!=0||!EnergyLife.enabled(t)||!WaterNetworkLife.active(t)
            ||!GridDistributionLife.power(t,p.district())||WaterNetworkLife.get(t,"treated")<=0
            ||WaterNetworkLife.get(t,"sewage")>=WaterNetworkLife.SEWER_CAPACITY
            ||t.technicalMaterials.getOrDefault("spare-parts",0)<2||c.money<NpcEnterpriseLife.PERSONAL_RESERVE+CONNECTION)return false;
        var electric=ConstructionCrewLife.worker(t,s,"Electrician",c);
        var plumber=ConstructionCrewLife.worker(t,s,"Plumber",c);
        if(electric==null||plumber==null)return false;
        c.money-=CONNECTION;
        electric.money=Math.addExact(electric.money,4);plumber.money=Math.addExact(plumber.money,4);
        t.employerFunds.merge(EnergyLife.ACCOUNT,2,Math::addExact);
        t.employerFunds.merge(WaterNetworkLife.ACCOUNT,2,Math::addExact);
        t.technicalMaterials.merge("spare-parts",-2,Integer::sum);
        set(t,p,"connected",1);return true;
    }
    static boolean move(TownLife.Town t,Lot p){
        var c=owner(t,p);
        if(c==null||!c.alive||c.age<18||get(t,p,"connected")==0||get(t,p,"moved")!=0
            ||!GridDistributionLife.power(t,p.district())||WaterNetworkLife.get(t,"treated")<=0
            ||!TownLife.privateUnit(c.id).equals(c.homeUnit))return false;
        var family=household(t,c);
        if(family.stream().anyMatch(x->RelationshipBoundaryLife.controlled(t,x)||!HomeRoutineLife.atHome(x))
            ||t.people.values().stream().anyMatch(x->x.alive&&unit(p).equals(x.homeUnit)))return false;
        for(var x:family)RentalLife.moveHome(t,x,p.district(),unit(p));
        set(t,p,"moved",1);return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        if(s.minute==390){
            for(var p:LOTS){
                var c=owner(t,p);
                if(c!=null&&!c.alive){
                    var heir=CommercialPropertyLife.heir(t,c);
                    set(t,p,"owner",heir==null?0:heir.generationIndex);
                }
            }
        }
        if(s.minute==660&&s.day>=7&&s.day%7==0&&BusinessCreditLife.clerk(t,s)){
            var candidates=t.people.values().stream().filter(c->NpcEnterpriseLife.eligible(t,c,s))
                .sorted(Comparator.comparingInt((TownLife.Citizen c)->c.money).reversed().thenComparing(c->c.id)).toList();
            outer:for(var p:LOTS)for(var c:candidates)if(buy(t,s,p,c))break outer;
        }
        if(s.minute==840){
            for(var p:LOTS)if(connect(t,s,p))break;
            for(var p:LOTS)if(build(t,s,p))break;
        }
        if(s.minute==1320)for(var p:LOTS)move(t,p);
    }
    record View(String id,String district,String owner,int stage,boolean connected,int residents,int landPrice,int connectionCost){}
    static List<View> view(TownLife.Town t){
        if(!StaffingLife.enabled(t))return List.of();
        return LOTS.stream().map(p->{
            var c=owner(t,p);
            return new View(p.id(),p.district().name(),c==null||!c.alive?"":c.name,get(t,p,"stage"),get(t,p,"connected")!=0,
                (int)t.people.values().stream().filter(x->x.alive&&unit(p).equals(x.homeUnit)).count(),LAND,CONNECTION);
        }).toList();
    }
}
