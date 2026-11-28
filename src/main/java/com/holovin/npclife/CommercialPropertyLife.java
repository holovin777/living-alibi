package com.holovin.npclife;
import java.util.*;

/** Finite commercial plots, paid construction and real seven-day office leases. */
final class CommercialPropertyLife {
    private CommercialPropertyLife(){}
    static final int STAGES=3, LABOR=12, STEEL=4, MATERIAL=2, LAND=90;
    static final List<String> PLOTS=List.of("via-verdi-office","via-roma-office","via-pavia-office");
    private static int get(TownLife.Town t,String p,String field){return t.industryStocks.getOrDefault("commercial:"+p+":"+field,0);}
    private static void set(TownLife.Town t,String p,String field,int n){t.industryStocks.put("commercial:"+p+":"+field,n);}
    static TownLife.Citizen heir(TownLife.Town t,TownLife.Citizen deceased){
        if(deceased==null||deceased.generationIndex<=0)return null;
        return t.people.values().stream().filter(c->c.alive&&c!=deceased&&c.generationIndex>0&&
            (DemographyLife.get(t,c,"parent-a")==deceased.generationIndex||DemographyLife.get(t,c,"parent-b")==deceased.generationIndex))
            .sorted(Comparator.comparingInt((TownLife.Citizen c)->c.age).reversed().thenComparing(c->c.id)).findFirst().orElse(null);
    }
    static boolean buy(TownLife.Town t,WorldState s,String p,TownLife.Citizen c){
        if(!PLOTS.contains(p)||get(t,p,"owner")!=0||!NpcEnterpriseLife.eligible(t,c,s)
            ||PLOTS.stream().anyMatch(x->get(t,x,"owner")==c.generationIndex)
            ||c.money<NpcEnterpriseLife.PERSONAL_RESERVE+LAND+STAGES*(LABOR+STEEL+MATERIAL))return false;
        c.money-=LAND;t.employerFunds.merge("city-maintenance",LAND,Math::addExact);
        set(t,p,"owner",c.generationIndex);return true;
    }
    static boolean build(TownLife.Town t,WorldState s,String p){
        if(!PLOTS.contains(p)||get(t,p,"stage")>=STAGES||get(t,p,"build-day")==s.day)return false;
        var owner=DemographyLife.byIndex(t,get(t,p,"owner"));
        if(owner==null||!owner.alive||owner.age<18||RelationshipBoundaryLife.controlled(t,owner)
            ||owner.money<NpcEnterpriseLife.PERSONAL_RESERVE+LABOR+STEEL+MATERIAL)return false;
        var worker=t.people.values().stream().filter(c->c!=owner&&"Engineer".equals(c.occupation)
            &&WorkplaceHierarchy.onDuty(c,s.day)&&!RelationshipBoundaryLife.controlled(t,c)
            &&!NpcJusticeLife.confined(t,c,s)&&!TownLife.inPlayerConversation(t,c)&&c.fatigue<80)
            .min(Comparator.comparing(c->c.id)).orElse(null);
        if(worker==null||get(t,"town","crew-day")==s.day||IndustryLife.stock(t,IndustryLife.FACTORY_STEEL)<1
            ||FactoryNetworkLife.stock(t,"building-material")<1)return false;
        owner.money-=LABOR+STEEL+MATERIAL;worker.money=Math.addExact(worker.money,LABOR);
        worker.fatigue=Math.min(100,worker.fatigue+5);
        t.employerFunds.merge("machinist",STEEL,Math::addExact);
        t.employerFunds.merge(FactoryNetworkLife.supplier("building-material"),MATERIAL,Math::addExact);
        t.industryStocks.merge(IndustryLife.FACTORY_STEEL,-1,Integer::sum);
        FactoryNetworkLife.change(t,"building-material",-1);
        set(t,p,"build-day",s.day);set(t,"town","crew-day",s.day);
        set(t,p,"stage",get(t,p,"stage")+1);
        return true;
    }
    static void estate(TownLife.Town t,String p){
        int index=get(t,p,"owner");if(index<=0)return;
        var c=DemographyLife.byIndex(t,index);if(c!=null&&c.alive)return;
        var heir=heir(t,c);set(t,p,"owner",heir==null?0:heir.generationIndex);
    }
    /** Called only when a real IT lease fee has already been deducted. */
    static void rent(TownLife.Town t,TownLife.Citizen tenant,int day){
        for(String p:PLOTS){
            var owner=DemographyLife.byIndex(t,get(t,p,"owner"));
            if(get(t,p,"stage")!=STAGES||owner==null||!owner.alive||owner==tenant)continue;
            int prior=get(t,p,"tenant");
            if(prior!=tenant.generationIndex&&get(t,p,"until")>=day)continue;
            owner.money=Math.addExact(owner.money,ITBusinessLife.RENT);
            set(t,p,"tenant",tenant.generationIndex);set(t,p,"until",day+6);return;
        }
        ITBusinessLife.credit(t,MunicipalLife.CITY,ITBusinessLife.RENT);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        if(s.minute==390){for(String p:PLOTS)estate(t,p);return;}
        if(s.minute==660&&s.day>=7&&s.day-get(t,"town","last-sale")>=7&&BusinessCreditLife.clerk(t,s)){
            var investors=t.people.values().stream().filter(c->NpcEnterpriseLife.eligible(t,c,s))
                .sorted(Comparator.comparingInt((TownLife.Citizen c)->c.money).reversed().thenComparing(c->c.id)).toList();
            for(String p:PLOTS)for(var c:investors)if(buy(t,s,p,c)){set(t,"town","last-sale",s.day);return;}
        }
        if(s.minute==840)for(String p:PLOTS)if(build(t,s,p))break;
    }
    record Plot(String id,String owner,int stage,int stages,int landPrice,int weeklyRent,String tenant,int leaseUntil){}
    static List<Plot> view(TownLife.Town t,int day){
        if(!StaffingLife.enabled(t))return List.of();
        return PLOTS.stream().map(p->{
            var c=DemographyLife.byIndex(t,get(t,p,"owner"));
            var tenant=DemographyLife.byIndex(t,get(t,p,"tenant"));
            boolean leased=tenant!=null&&tenant.alive&&get(t,p,"until")>=day;
            return new Plot(p,c==null||!c.alive?"":c.name,get(t,p,"stage"),STAGES,LAND,ITBusinessLife.RENT,
                leased?tenant.name:"",leased?get(t,p,"until"):0);
        }).toList();
    }
}
