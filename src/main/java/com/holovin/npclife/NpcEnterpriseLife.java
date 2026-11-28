package com.holovin.npclife;
import java.util.*;

/** Resident ownership is separate from visitor incarnations. All investment is paid. */
final class NpcEnterpriseLife {
    private NpcEnterpriseLife(){}
    static final int PERSONAL_RESERVE=180, MAX_FIRMS=3, STARTUP_CAPITAL=60;
    private static int get(TownLife.Town t,String f){return t.industryStocks.getOrDefault("npc-enterprise:"+f,0);}
    private static void set(TownLife.Town t,String f,int n){t.industryStocks.put("npc-enterprise:"+f,n);}
    static TownLife.Citizen owner(TownLife.Town t,String a){
        var c=DemographyLife.byIndex(t,get(t,"owner:"+a));
        return c!=null&&c.alive?c:null;
    }
    static boolean owned(TownLife.Town t,String a){return owner(t,a)!=null;}
    static int count(TownLife.Town t,TownLife.Citizen c){
        return (int)BusinessSolvencyLife.accounts(t).stream().filter(a->owner(t,a)==c).count()
            +(ITBusinessLife.registered(t,c)?1:0)+IndustrialExpansionLife.owned(t,c);
    }
    static boolean eligible(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.alive&&c.generationIndex>0&&c.age>=25&&c.age<65
            &&c.carefulness>=60&&c.curiosity>=55&&!RelationshipBoundaryLife.controlled(t,c)
            &&!NpcJusticeLife.confined(t,c,s)&&!EmploymentLife.sick(c,s.day)&&!TownLife.inPlayerConversation(t,c);
    }
    static boolean acquire(TownLife.Town t,WorldState s,String a,TownLife.Citizen c){
        if(!eligible(t,c,s)||!BusinessOwnershipLife.listed(t,a)||count(t,c)>=MAX_FIRMS
            ||BusinessSolvencyLife.distressDays(t,a)<BusinessOwnershipLife.SALE_DAYS+2)return false;
        int capital=BusinessOwnershipLife.capital(t,a),price=BusinessOwnershipLife.PRICE;
        if((long)c.money<PERSONAL_RESERVE+(long)price+capital)return false;
        c.money-=price+capital;
        t.employerFunds.merge("city-maintenance",price,Math::addExact);
        t.employerFunds.merge(a,capital,Math::addExact);
        BusinessManagementLife.clear(t,a);
        set(t,"owner:"+a,c.generationIndex);
        BusinessSolvencyLife.settle(t,a);TradeCreditLife.settle(t,a,s.day);
        return true;
    }
    static boolean founder(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return eligible(t,c,s)&&count(t,c)<MAX_FIRMS&&!ITBusinessLife.registered(t,c)
            &&"Programmer".equals(c.occupation)&&EducationLife.get(t,c,"technical")>=65
            &&WorkplaceHierarchy.tenure(c,s.day)>=30
            &&c.money>=PERSONAL_RESERVE+ITBusinessLife.REGISTRATION+ITBusinessLife.RENT+STARTUP_CAPITAL;
    }
    static boolean startIT(TownLife.Town t,WorldState s,TownLife.Citizen c){
        if(!founder(t,c,s)||(!SoftwareLife.deployed(t,"retail-api")&&!SoftwareLife.deployed(t,"water-control")))return false;
        c.money-=ITBusinessLife.REGISTRATION+ITBusinessLife.RENT+STARTUP_CAPITAL;
        ITBusinessLife.credit(t,MunicipalLife.CITY,ITBusinessLife.REGISTRATION);
        CommercialPropertyLife.rent(t,c,s.day);
        ITBusinessLife.set(t,c,"registered",1);ITBusinessLife.set(t,c,"cash",STARTUP_CAPITAL);
        ITBusinessLife.set(t,c,"rent-until",s.day+6);ITBusinessLife.set(t,c,"managed",1);
        ITBusinessLife.set(t,c,"npc-founded",s.day);
        return true;
    }
    private static void hireIT(TownLife.Town t,WorldState s,TownLife.Citizen c){
        if(!eligible(t,c,s)||!ITBusinessLife.registered(t,c)||ITBusinessLife.get(t,c,"npc-founded")<=0
            ||ITBusinessLife.get(t,c,"npc-founded")>=s.day||ITStaffLife.employee(t,c)!=null)return;
        int cash=ITBusinessLife.get(t,c,"cash");
        int rent=ITBusinessLife.get(t,c,"rent-until")<s.day?ITBusinessLife.RENT:0;
        if(cash<ITStaffLife.BONUS+2*ITStaffLife.WAGE+rent)return;
        var w=t.people.values().stream().filter(x->ITStaffLife.eligible(t,c,x,s.day)&&!NpcJusticeLife.confined(t,x,s))
            .sorted(Comparator.comparingInt((TownLife.Citizen x)->EducationLife.get(t,x,"technical")).reversed()
                .thenComparing(x->x.id)).findFirst().orElse(null);
        if(w==null)return;
        if(rent>0){CommercialPropertyLife.rent(t,c,s.day);ITBusinessLife.set(t,c,"rent-until",s.day+6);}
        ITBusinessLife.set(t,c,"cash",cash-rent-ITStaffLife.BONUS);
        w.money=Math.addExact(w.money,ITStaffLife.BONUS);w.employed=true;w.unemploymentDays=0;
        w.lastHiredDay=s.day;w.occupation=ITStaffLife.OCCUPATION;w.workplace=TownLife.Place.OFFICE;w.jobChanges++;
        ITBusinessLife.set(t,c,"employee",w.generationIndex);ITBusinessLife.set(t,w,"employer",c.generationIndex);
        ITBusinessLife.set(t,c,"managed",1);EmploymentContractLife.signCitizen(t,w,s.day);SocialProtectionLife.hired(t,w);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        if(s.minute==390){
            for(String a:BusinessSolvencyLife.accounts(t))if(get(t,"owner:"+a)>0&&owner(t,a)==null){
                var heir=CommercialPropertyLife.heir(t,DemographyLife.byIndex(t,get(t,"owner:"+a)));
                set(t,"owner:"+a,heir==null?0:heir.generationIndex);BusinessManagementLife.clear(t,a);
            }
            return;
        }
        if(s.minute!=660||get(t,"review-day")>=s.day)return;
        set(t,"review-day",s.day);
        for(String a:BusinessSolvencyLife.accounts(t)){
            var c=owner(t,a);if(!eligible(t,c,s))continue;
            BusinessManagementLife.appointNpc(t,s,a);
            if(s.day-get(t,"capital-day:"+a)<7)continue;
            int cash=t.employerFunds.getOrDefault(a,0);
            if(BusinessSolvencyLife.suspended(t,a)&&c.money>=PERSONAL_RESERVE+20){
                c.money-=20;t.employerFunds.merge(a,20,Math::addExact);
                BusinessSolvencyLife.settle(t,a);TradeCreditLife.settle(t,a,s.day);set(t,"capital-day:"+a,s.day);
            }else if(!BusinessSolvencyLife.suspended(t,a)&&cash>=BusinessOwnershipLife.DIVIDEND_RESERVE+20
                &&BusinessSolvencyLife.outstanding(t,a)==0&&TradeCreditLife.payable(t,a)==0
                &&BusinessCreditLife.get(t,a,"balance")==0){
                t.employerFunds.merge(a,-20,Integer::sum);c.money=Math.addExact(c.money,20);set(t,"capital-day:"+a,s.day);
            }
        }
        if(!BusinessCreditLife.clerk(t,s))return;
        for(var c:t.people.values())hireIT(t,s,c);
        if(s.day-get(t,"last-acquisition")>=7){
            var buyers=t.people.values().stream().filter(c->eligible(t,c,s)&&count(t,c)<MAX_FIRMS)
                .sorted(Comparator.comparingInt((TownLife.Citizen c)->c.money).reversed().thenComparing(c->c.id)).toList();
            boolean bought=false;
            for(String a:BusinessSolvencyLife.accounts(t)){
                if(!BusinessOwnershipLife.listed(t,a))continue;
                for(var c:buyers)if(acquire(t,s,a,c)){set(t,"last-acquisition",s.day);bought=true;break;}
                if(bought)break;
            }
        }
        long startups=t.people.values().stream().filter(c->c.alive&&ITBusinessLife.registered(t,c)
            &&ITBusinessLife.get(t,c,"npc-founded")>0).count();
        if(startups<3&&s.day-get(t,"last-startup")>=7){
            var c=t.people.values().stream().filter(x->founder(t,x,s))
                .max(Comparator.comparingInt((TownLife.Citizen x)->x.money).thenComparing(x->x.id)).orElse(null);
            if(c!=null&&startIT(t,s,c))set(t,"last-startup",s.day);
        }
    }
}
