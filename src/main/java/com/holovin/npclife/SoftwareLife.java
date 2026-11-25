package com.holovin.npclife;

import java.util.List;

/** Finite, town-owned software contracts. Deployment changes throughput, never resource creation. */
final class SoftwareLife {
    private SoftwareLife(){}
    record Project(String id,String title,String effect,String account,int wage){}
    static final List<Project> PROJECTS=List.of(
            new Project("water-control","Water control dashboard","Working pump: up to 6 instead of 4 river-water units per day","city-maintenance",45),
            new Project("factory-planning","Factory production planning","Refining: up to 3 instead of 2 iron units per shift","machinist",60));
    static String key(String id){return "software:phase:"+id;}
    static void ensure(TownLife.Town t){for(var p:PROJECTS)t.industryStocks.putIfAbsent(key(p.id()),0);}
    static int phase(TownLife.Town t,String id){return t.industryStocks.getOrDefault(key(id),0);}
    static boolean deployed(TownLife.Town t,String id){return phase(t,id)==3;}
    static boolean linkedProgrammer(TownLife.Town t,TownLife.Citizen c){
        return RebirthLife.linkedAdult(t,c)&&c.employed&&"Programmer".equals(c.occupation)
                &&EducationLife.get(t,c,"diploma-programming")>0&&EducationLife.get(t,c,"technical")>=45;
    }
    record ProjectView(String id,String title,String effect,int wage,String phase,String nextAction,int minutes,boolean deployed,boolean canAdvance){}
    static List<ProjectView> projects(TownLife.Town t,int day,int minute){
        var c=RebirthLife.child(t);
        if(!linkedProgrammer(t,c))return List.of();
        boolean at=RebirthLife.awakePresent(c)&&!RebirthLife.atHome(c)&&c.place==TownLife.Place.OFFICE
                &&!EmploymentLife.sick(c,day)&&c.fatigue<85&&minute>=480;
        return PROJECTS.stream().map(p->{
            int phase=phase(t,p.id()),duration=phase==0||phase==1?60:phase==2?30:0;
            return new ProjectView(p.id(),p.title(),p.effect(),p.wage(),List.of("OPEN","DEVELOPED","TESTED","DEPLOYED").get(phase),
                    List.of("Develop","Test","Deploy","Complete").get(phase),duration,phase==3,
                    at&&phase<3&&minute+duration<=1080&&t.employerFunds.getOrDefault(p.account(),0)>=p.wage());
        }).toList();
    }
    static RebirthLife.HomeAction advance(TownLife.Town t,int day,int minute,String id){
        if(phase(t,id)==0)throw new IllegalArgumentException("Submit a tested Java solution for this ticket first.");
        return progress(t,day,minute,id);
    }
    static RebirthLife.HomeAction submit(TownLife.Town t,int day,int minute,String id,String code){
        var c=RebirthLife.requireAdult(t);
        if(EducationLife.get(t,c,"java-onboarded")==0)throw new IllegalArgumentException("Complete the company onboarding first.");
        if(phase(t,id)!=0)throw new IllegalArgumentException("Ticket is not awaiting code.");
        JavaTicketLife.verify(id,code);
        return progress(t,day,minute,id);
    }
    private static RebirthLife.HomeAction progress(TownLife.Town t,int day,int minute,String id){
        var c=RebirthLife.requireAdult(t);
        var view=projects(t,day,minute).stream().filter(v->v.id().equals(id)&&v.canAdvance()).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Software work unavailable: check qualification, location, hours and client funds."));
        var p=PROJECTS.stream().filter(v->v.id().equals(id)).findFirst().orElseThrow();
        int next=phase(t,id)+1;
        int wallet=next==3?Math.addExact(c.money,p.wage()):c.money;
        RebirthLife.travel(t,TownLife.Place.OFFICE,day);
        t.industryStocks.put(key(id),next);
        c.fatigue=Math.min(100,c.fatigue+4);
        EducationLife.learn(t,c,"technical",1);
        if(next==3){
            t.employerFunds.merge(p.account(),-p.wage(),Integer::sum);c.money=wallet;
            return new RebirthLife.HomeAction(view.minutes(),"Deployed "+p.title()+"; earned EUR "+p.wage()+".");
        }
        return new RebirthLife.HomeAction(view.minutes(),view.nextAction()+" completed for "+p.title()+".");
    }
}
