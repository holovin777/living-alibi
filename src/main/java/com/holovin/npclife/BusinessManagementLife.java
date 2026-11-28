package com.holovin.npclife;

import java.util.*;

/** Owner controls for existing staffed operating accounts; directors retain their occupations. */
final class BusinessManagementLife {
    private BusinessManagementLife(){}
    private static String key(String a,String f){return "business-management:"+a+":"+f;}
    static int get(TownLife.Town t,String a,String f){return t.industryStocks.getOrDefault(key(a,f),0);}
    private static void set(TownLife.Town t,String a,String f,int n){t.industryStocks.put(key(a,f),n);}
    static int wage(TownLife.Town t,String a){
        int base=BusinessSolvencyLife.wage(a);
        return BusinessOwnershipLife.yours(t,a)?base+get(t,a,"raise"):base;
    }
    static boolean member(TownLife.Citizen c,String a){
        return c.alive&&c.employed&&c.generationIndex>0&&a.equals(BusinessSolvencyLife.account(c.occupation));
    }
    static List<TownLife.Citizen> members(TownLife.Town t,String a){
        return t.people.values().stream().filter(c->member(c,a)).sorted(Comparator.comparing(c->c.id)).toList();
    }
    static TownLife.Citizen director(TownLife.Town t,String a){
        int index=get(t,a,"director");
        return index<=0||(!BusinessOwnershipLife.yours(t,a)&&!NpcEnterpriseLife.owned(t,a))?null:members(t,a).stream().filter(c->c.generationIndex==index).findFirst().orElse(null);
    }
    static int target(TownLife.Town t,StaffingLife.Role r){
        String a=BusinessSolvencyLife.account(r.name());
        if(a==null||!BusinessOwnershipLife.yours(t,a))return r.target();
        int configured=get(t,a,"target");
        return configured==0?r.target():configured-1;
    }
    static boolean accepts(TownLife.Town t,String role){
        var r=StaffingLife.role(role);if(r==null)return true;
        String a=BusinessSolvencyLife.account(role);
        return a==null||!BusinessOwnershipLife.yours(t,a)||StaffingLife.count(t,r)<target(t,r);
    }
    private static StaffingLife.Role role(String a){
        return StaffingLife.ROLES.stream().filter(r->a.equals(BusinessSolvencyLife.account(r.name()))).findFirst().orElse(null);
    }
    private static boolean office(TownLife.Town t,WorldState s,String a){
        return BusinessOwnershipLife.yours(t,a)&&t.player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)
            &&t.player.travelRemaining==0&&!t.player.cycling&&t.player.place==TownLife.Place.OFFICE&&s.minute>=540&&s.minute+15<=1020;
    }
    private static TownLife.Citizen candidate(TownLife.Town t,String a,WorldState s){
        return members(t,a).stream().filter(c->c.age>=25&&!EmploymentLife.sick(c,s.day)&&!RebirthLife.linkedAdult(t,c)
            &&!NpcJusticeLife.confined(t,c,s))
            .max(Comparator.comparingInt((TownLife.Citizen c)->c.carefulness+c.sociability+WorkplaceHierarchy.tenure(c,s.day)).thenComparing(c->c.id)).orElse(null);
    }
    static void appointNpc(TownLife.Town t,WorldState s,String a){
        if(!NpcEnterpriseLife.owned(t,a)||director(t,a)!=null)return;
        var c=candidate(t,a,s);
        if(c!=null&&!RelationshipBoundaryLife.controlled(t,c)){
            EnterpriseGovernanceLife.clearManagement(t,a);set(t,a,"director",c.generationIndex);
        }
    }
    record Option(String id,String label){}
    static List<Option> options(TownLife.Town t,WorldState s,String a){
        var r=role(a);if(r==null||!office(t,s,a))return List.of();
        var options=new ArrayList<Option>();var c=candidate(t,a,s);var director=director(t,a);
        if(c!=null&&director!=c)options.add(new Option("director","Appoint director / Nomina direttore: "+c.name));
        if(director!=null)options.add(new Option("remove-director","Remove director / Revoca direttore"));
        if(get(t,a,"raise")<4&&t.employerFunds.getOrDefault(a,0)>=Math.max(1,members(t,a).size())*(wage(t,a)+1)*4)
            options.add(new Option("raise","Raise hourly wage / Aumenta paga · €"+(wage(t,a)+1)));
        if(get(t,a,"raise")>0)options.add(new Option("lower","Reduce hourly wage / Riduci paga · €"+(wage(t,a)-1)));
        if(target(t,r)<r.target()+3)options.add(new Option("more","Increase staffing target / Aumenta organico"));
        if(target(t,r)>0)options.add(new Option("less","Reduce staffing target / Riduci organico"));
        var dismiss=members(t,a).stream().filter(x->x!=director&&!RebirthLife.linkedAdult(t,x)).reduce((x,y)->y).orElse(null);
        if(dismiss!=null)options.add(new Option("dismiss","Dismiss / Licenzia "+dismiss.name));
        return List.copyOf(options);
    }
    static String action(TownLife.Town t,WorldState s,String a,String action){
        if(options(t,s,a).stream().noneMatch(o->o.id().equals(action)))throw new IllegalArgumentException("Management action unavailable.");
        var r=role(a);
        switch(action){
            case "director" -> {var c=candidate(t,a,s);EnterpriseGovernanceLife.clearManagement(t,a);set(t,a,"director",c.generationIndex);return "Appointed "+c.name+" as working director of "+a+".";}
            case "remove-director" -> {set(t,a,"director",0);EnterpriseGovernanceLife.clearManagement(t,a);}
            case "raise" -> set(t,a,"raise",get(t,a,"raise")+1);
            case "lower" -> set(t,a,"raise",get(t,a,"raise")-1);
            case "more" -> set(t,a,"target",target(t,r)+2);
            case "less" -> set(t,a,"target",target(t,r));
            case "dismiss" -> {
                var director=director(t,a);
                var c=members(t,a).stream().filter(x->x!=director&&!RebirthLife.linkedAdult(t,x)).reduce((x,y)->y).orElseThrow();
                EmploymentLife.loseJob(t,s,c);
                return "Employment ended for "+c.name+". Existing wage arrears remain payable.";
            }
            default -> throw new IllegalArgumentException("Unknown management action.");
        }
        return "Updated management of "+a+".";
    }
    static void clear(TownLife.Town t,String a){for(String f:List.of("director","raise","target"))set(t,a,f,0);EnterpriseGovernanceLife.clearManagement(t,a);}
    record View(String account,String director,int employees,int target,int hourlyWage,EnterpriseGovernanceLife.View governance,List<ProcurementLife.Order> procurement,TradeCreditLife.View tradeCredit){}
    static View view(TownLife.Town t,String a){
        var c=director(t,a);var r=role(a);
        return new View(a,c==null?"":c.name,members(t,a).size(),r==null?0:target(t,r),wage(t,a),EnterpriseGovernanceLife.view(t,a),ProcurementLife.view(t,a),TradeCreditLife.view(t,a));
    }
}
