package com.holovin.npclife;

import java.util.*;

/** One fictional residential policy per occupied home unit; finite agent capacity and insurer cash. */
final class HouseholdInsuranceLife {
    private HouseholdInsuranceLife(){}
    static final String ROLE="Insurance agent", ACCOUNT=HomeInsuranceLife.ACCOUNT;
    static final int COMMISSION=1, MIN_SAVINGS=60;
    record Household(String key,TownLife.Place district,TownLife.Citizen payer){}
    private static String key(Household h,String field){return "household-insurance:"+h.key()+":"+field;}
    static int get(TownLife.Town t,Household h,String field){return t.industryStocks.getOrDefault(key(h,field),0);}
    private static void set(TownLife.Town t,Household h,String field,int n){t.industryStocks.put(key(h,field),n);}
    static List<Household> households(TownLife.Town t){
        var grouped=new TreeMap<String,Household>();
        for(var c:t.people.values()){
            if(!c.alive||c.generationIndex<=0||c.age<18||RebirthLife.linkedAdult(t,c)
                ||(c.home!=TownLife.Place.NORTH_RESIDENCES&&c.home!=TownLife.Place.SOUTH_RESIDENCES))continue;
            String unit=c.home.name()+":"+(c.homeUnit==null?c.id:c.homeUnit);
            var old=grouped.get(unit);
            if(old==null||c.money>old.payer().money||c.money==old.payer().money&&c.id.compareTo(old.payer().id)<0)
                grouped.put(unit,new Household(unit,c.home,c));
        }
        return List.copyOf(grouped.values());
    }
    static boolean agent(TownLife.Citizen c){return c.alive&&c.employed&&ROLE.equals(c.occupation)&&c.workplace==TownLife.Place.OFFICE;}
    static boolean onDuty(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return agent(c)&&WorkplaceHierarchy.onDuty(c,s.day)&&!RebirthLife.linkedAdult(t,c)
            &&!NpcJusticeLife.confined(t,c,s)&&!TownLife.inPlayerConversation(t,c);
    }
    static boolean active(TownLife.Town t,Household h,WorldState s){return get(t,h,"until")>s.day;}
    static boolean sell(TownLife.Town t,WorldState s,Household h,TownLife.Citizen agent){
        var payer=h.payer();
        if(!onDuty(t,agent,s)||payer.money<MIN_SAVINGS||payer.carefulness<55
                ||get(t,h,"until")-s.day>7||FireRecoveryLife.closed(t,h.district()))return false;
        payer.money-=HomeInsuranceLife.PREMIUM;
        t.employerFunds.merge(ACCOUNT,HomeInsuranceLife.PREMIUM-COMMISSION,Math::addExact);
        agent.money=Math.addExact(agent.money,COMMISSION);
        set(t,h,"until",Math.addExact(Math.max(s.day,get(t,h,"until")),HomeInsuranceLife.TERM));
        set(t,h,"premiums",Math.addExact(get(t,h,"premiums"),HomeInsuranceLife.PREMIUM));
        return true;
    }
    static void incidentStarted(TownLife.Town t,WorldState s,TownLife.Place p){
        if(p!=TownLife.Place.NORTH_RESIDENCES&&p!=TownLife.Place.SOUTH_RESIDENCES)return;
        for(var h:households(t))if(h.district()==p&&active(t,h,s)){
            set(t,h,"incident",FireServiceLife.get(t,"started"));set(t,h,"loss",0);
        }
    }
    static void damage(TownLife.Town t,TownLife.Place p,int loss){
        int incident=FireServiceLife.get(t,"started");
        if(incident<=0||loss<=0||(p!=TownLife.Place.NORTH_RESIDENCES&&p!=TownLife.Place.SOUTH_RESIDENCES))return;
        for(var h:households(t))if(h.district()==p&&get(t,h,"incident")==incident)
            set(t,h,"loss",Math.min(100,get(t,h,"loss")+loss));
    }
    static void incidentEnded(TownLife.Town t){
        int incident=FireServiceLife.get(t,"started");if(incident<=0)return;
        for(var h:households(t))if(get(t,h,"incident")==incident){
            int claim=Math.min(HomeInsuranceLife.MAX_CLAIM,Math.max(0,get(t,h,"loss")-HomeInsuranceLife.DEDUCTIBLE));
            set(t,h,"pending",Math.addExact(get(t,h,"pending"),claim));
            set(t,h,"incident",0);set(t,h,"loss",0);
        }
    }
    static boolean payClaim(TownLife.Town t,WorldState s,Household h,TownLife.Citizen agent){
        if(!onDuty(t,agent,s))return false;
        int amount=Math.min(get(t,h,"pending"),Math.max(0,t.employerFunds.getOrDefault(ACCOUNT,0)));
        if(amount<=0)return false;
        t.employerFunds.merge(ACCOUNT,-amount,Integer::sum);
        h.payer().money=Math.addExact(h.payer().money,amount);
        set(t,h,"pending",get(t,h,"pending")-amount);
        set(t,h,"paid",Math.addExact(get(t,h,"paid"),amount));
        return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t)||s.minute<540||s.minute>=960||s.minute%15!=0)return;
        int now=FireServiceLife.now(s);
        if(t.industryStocks.getOrDefault("household-insurance:last-tick",0)==now)return;
        t.industryStocks.put("household-insurance:last-tick",now);
        var agents=t.people.values().stream().filter(c->onDuty(t,c,s)).sorted(Comparator.comparing(c->c.id)).toList();
        if(agents.isEmpty())return;
        var homes=households(t);
        for(var agent:agents){
            // Existing claims take priority; otherwise issue one new or renewed policy.
            boolean handled=false;
            for(var h:homes)if(payClaim(t,s,h,agent)){handled=true;break;}
            if(!handled)for(var h:homes)if(sell(t,s,h,agent))break;
        }
    }
    record View(int households,int insured,int agents,int onDuty,int pending,int paid,int funds){}
    static View view(TownLife.Town t,WorldState s){
        var homes=households(t);
        return new View(homes.size(),(int)homes.stream().filter(h->active(t,h,s)).count(),
            (int)t.people.values().stream().filter(HouseholdInsuranceLife::agent).count(),
            (int)t.people.values().stream().filter(c->onDuty(t,c,s)).count(),
            homes.stream().mapToInt(h->get(t,h,"pending")).sum(),homes.stream().mapToInt(h->get(t,h,"paid")).sum(),
            t.employerFunds.getOrDefault(ACCOUNT,0));
    }
}
