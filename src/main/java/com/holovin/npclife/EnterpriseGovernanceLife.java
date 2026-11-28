package com.holovin.npclife;

import java.util.*;

/** Autonomous working managers, real applicant appointments and bounded cash reports.
 * All state uses the existing persisted stock map; no additional wallets or employees. */
final class EnterpriseGovernanceLife {
    private EnterpriseGovernanceLife(){}
    private static final int HISTORY=7;
    private static String key(String a,String f){return "enterprise-governance:"+a+":"+f;}
    private static int get(TownLife.Town t,String a,String f){return t.industryStocks.getOrDefault(key(a,f),0);}
    private static void set(TownLife.Town t,String a,String f,int value){t.industryStocks.put(key(a,f),value);}
    private static int cash(TownLife.Town t,String a){return t.employerFunds.getOrDefault(a,0);}
    private static boolean available(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.age>=25&&c.age<67&&!EmploymentLife.sick(c,s.day)
            &&!RelationshipBoundaryLife.controlled(t,c)&&!NpcJusticeLife.confined(t,c,s);
    }
    private static TownLife.Citizen byIndex(TownLife.Town t,int index){
        return index<=0?null:t.people.values().stream().filter(c->c.generationIndex==index).findFirst().orElse(null);
    }
    private static List<TownLife.Citizen> roster(TownLife.Town t,String a,WorldState s){
        return BusinessManagementLife.members(t,a).stream().filter(c->available(t,c,s))
            .sorted(Comparator.comparingInt((TownLife.Citizen c)->c.carefulness+c.sociability+
                Math.min(90,WorkplaceHierarchy.tenure(c,s.day))).reversed().thenComparing(c->c.id)).toList();
    }
    static boolean delegated(TownLife.Town t,String a){
        return a!=null&&BusinessManagementLife.director(t,a)!=null;
    }
    private static TownLife.Citizen director(TownLife.Town t,String a,WorldState s,List<TownLife.Citizen> members){
        if(BusinessOwnershipLife.yours(t,a)||NpcEnterpriseLife.owned(t,a))return BusinessManagementLife.director(t,a);
        return members.isEmpty()?null:members.getFirst();
    }
    private static TownLife.Citizen select(List<TownLife.Citizen> members,Set<Integer> used,boolean social,TownLife.Citizen fallback){
        var c=members.stream().filter(p->!used.contains(p.generationIndex))
            .max(Comparator.comparingInt((TownLife.Citizen p)->social?p.sociability:p.carefulness)
                .thenComparing(p->p.id)).orElse(fallback);
        if(c!=null)used.add(c.generationIndex);
        return c;
    }
    private static boolean working(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return available(t,c,s)&&WorkplaceHierarchy.onDuty(c,s.day)&&c.fatigue<85&&!TownLife.inPlayerConversation(t,c);
    }
    private static StaffingLife.Role role(String a){
        return StaffingLife.ROLES.stream().filter(r->a.equals(BusinessSolvencyLife.account(r.name()))).findFirst().orElse(null);
    }
    private static boolean qualified(TownLife.Town t,TownLife.Citizen c,StaffingLife.Role r,WorldState s){
        return c!=null&&!c.employed&&StaffingLife.eligible(t,c,s)
            &&EducationLife.get(t,c,EducationLife.subject(r.name()))>=r.skill()
            &&(r.diploma()==null||EducationLife.get(t,c,"diploma-"+r.diploma())>0);
    }
    private static boolean affordable(TownLife.Town t,String a){
        return BusinessSolvencyLife.outstanding(t,a)==0&&!BusinessSolvencyLife.suspended(t,a)
            &&(long)cash(t,a)>=8L*BusinessManagementLife.wage(t,a)*(BusinessManagementLife.members(t,a).size()+1L);
    }
    static void recordPayroll(TownLife.Town t,String a,int paid){
        if(paid>0&&get(t,a,"opening-day")>0)set(t,a,"payroll",Math.addExact(get(t,a,"payroll"),paid));
    }
    private static void opening(TownLife.Town t,String a,WorldState s){
        int previous=get(t,a,"opening-day");
        if(previous==s.day)return;
        if(previous>0&&previous==s.day-1){
            String slot="report:"+Math.floorMod(previous,HISTORY)+":";
            set(t,a,slot+"day",previous);
            set(t,a,slot+"opening",get(t,a,"opening"));
            set(t,a,slot+"closing",cash(t,a));
            set(t,a,slot+"payroll",get(t,a,"payroll"));
            set(t,a,slot+"arrears",BusinessSolvencyLife.outstanding(t,a));
            set(t,a,slot+"bank",BusinessCreditLife.get(t,a,"balance"));
            set(t,a,slot+"staff",BusinessManagementLife.members(t,a).size());
        }
        set(t,a,"opening-day",s.day);
        set(t,a,"opening",cash(t,a));set(t,a,"payroll",0);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t))return;
        // Cutoff before morning payroll settlement: each report covers 06:30 to 06:30.
        if(s.minute==390){for(String a:BusinessSolvencyLife.accounts(t))opening(t,a,s);return;}
        if(s.minute<480||s.minute>1080||s.minute%60!=0)return;
        for(String a:BusinessSolvencyLife.accounts(t)){
            if(BusinessSolvencyLife.suspended(t,a))continue;
            var r=role(a);if(r==null)continue;
            var members=roster(t,a,s);
            var director=director(t,a,s,members);
            if(!available(t,director,s))continue;
            Set<Integer> used=new HashSet<>();used.add(director.generationIndex);
            var lead=select(members,used,false,director);
            var accountant=select(members,used,false,director);
            var hr=select(members,used,true,director);
            for(var entry:Map.of("director",director,"lead",lead,"accountant",accountant,"hr",hr).entrySet())
                set(t,a,entry.getKey(),entry.getValue().generationIndex);
            if(working(t,director,s)&&get(t,a,"decision-day")!=s.day){
                set(t,a,"decision-day",s.day);
                // 1: preserve liquidity, 2: authorize recruitment, 3: maintain staffing.
                set(t,a,"decision",!affordable(t,a)?1:StaffingLife.count(t,r)<BusinessManagementLife.target(t,r)?2:3);
            }
            if(working(t,lead,s)&&get(t,a,"lead-day")!=s.day){
                set(t,a,"lead-day",s.day);
                var novice=members.stream().filter(p->p!=lead&&working(t,p,s)
                    &&WorkplaceHierarchy.tenure(p,s.day)<14&&WorkplaceHierarchy.skill(t,p)<WorkplaceHierarchy.skill(t,lead))
                    .min(Comparator.comparingInt(p->WorkplaceHierarchy.skill(t,p))).orElse(null);
                if(novice!=null)EducationLife.learn(t,novice,EducationLife.subject(novice.occupation),1);
            }
            if(working(t,accountant,s)&&get(t,a,"audit-day")!=s.day){
                set(t,a,"audit-day",s.day);
                set(t,a,"audit-cash",cash(t,a));
                set(t,a,"audit-arrears",BusinessSolvencyLife.outstanding(t,a));
            }
            if(!working(t,hr,s)||get(t,a,"hr-day")==s.day)continue;
            set(t,a,"hr-day",s.day);
            int appointment=get(t,a,"appointment");
            if(appointment>0&&appointment<=s.day){
                var applicant=byIndex(t,get(t,a,"candidate"));
                boolean hire=qualified(t,applicant,r,s)&&affordable(t,a)
                    &&StaffingLife.count(t,r)<BusinessManagementLife.target(t,r)
                    &&applicant.carefulness+applicant.sociability>=60;
                if(hire)StaffingLife.assign(t,s,applicant,r,false);
                set(t,a,"interview-day",s.day);set(t,a,"result",hire?2:3);
                set(t,a,"appointment",0);
                continue;
            }
            if(appointment>0||get(t,a,"decision-day")!=s.day||get(t,a,"decision")!=2
                ||!affordable(t,a)||StaffingLife.count(t,r)>=BusinessManagementLife.target(t,r))continue;
            var applicant=t.people.values().stream().filter(c->qualified(t,c,r,s))
                .sorted(Comparator.comparingInt((TownLife.Citizen c)->EducationLife.get(t,c,EducationLife.subject(r.name())))
                    .reversed().thenComparing(c->c.id)).findFirst().orElse(null);
            if(applicant!=null){
                set(t,a,"candidate",applicant.generationIndex);set(t,a,"appointment",s.day+1);
                set(t,a,"result",1);
            }
        }
    }
    private static String name(TownLife.Town t,int index){
        var c=byIndex(t,index);return c==null?"":c.name;
    }
    record Report(int day,int openingCash,int closingCash,int netCashChange,int payrollPaid,int wageDebt,int bankDebt,int staff){}
    record View(String director,String departmentHead,String accountant,String hr,int decisionDay,String decision,
                int auditDay,int auditCash,int auditArrears,String candidate,int appointmentDay,int interviewDay,String interviewResult,List<Report> reports){}
    static View view(TownLife.Town t,String a){
        List<Report> history=new ArrayList<>();
        for(int i=0;i<HISTORY;i++){
            String slot="report:"+i+":";int day=get(t,a,slot+"day");
            if(day<=0)continue;
            int start=get(t,a,slot+"opening"),end=get(t,a,slot+"closing");
            history.add(new Report(day,start,end,end-start,get(t,a,slot+"payroll"),get(t,a,slot+"arrears"),get(t,a,slot+"bank"),get(t,a,slot+"staff")));
        }
        history.sort(Comparator.comparingInt(Report::day).reversed());
        String decision=switch(get(t,a,"decision")){case 1->"FREEZE_HIRING";case 2->"RECRUIT";case 3->"MAINTAIN";default->"PENDING";};
        String result=switch(get(t,a,"result")){case 1->"SCHEDULED";case 2->"HIRED";case 3->"REJECTED";default->"NONE";};
        return new View(name(t,get(t,a,"director")),name(t,get(t,a,"lead")),name(t,get(t,a,"accountant")),name(t,get(t,a,"hr")),
            get(t,a,"decision-day"),decision,get(t,a,"audit-day"),get(t,a,"audit-cash"),get(t,a,"audit-arrears"),
            name(t,get(t,a,"candidate")),get(t,a,"appointment"),get(t,a,"interview-day"),result,List.copyOf(history));
    }
    static void clearManagement(TownLife.Town t,String a){
        for(String field:List.of("director","lead","accountant","hr","decision-day","decision","lead-day","audit-day","audit-cash","audit-arrears","hr-day","candidate","appointment","interview-day","result"))
            t.industryStocks.remove(key(a,field));
    }
}
