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
    static String gitKey(String id){return "software:git:"+id;}
    static void ensure(TownLife.Town t){
        for(var p:PROJECTS){
            t.industryStocks.putIfAbsent(key(p.id()),0);
            t.industryStocks.putIfAbsent(gitKey(p.id()),0);
        }
    }
    // Old saves with developed/tested code can resume without repeating development.
    static int gitStage(TownLife.Town t,String id){
        int stage=t.industryStocks.getOrDefault(gitKey(id),0);
        return stage==0&&phase(t,id)>0?(phase(t,id)==1?1:phase(t,id)==2?3:4):stage;
    }
    static String branch(String id){return "feature/"+(id.equals("water-control")?"BD-101":"BD-102")+"-"+id;}
    static int phase(TownLife.Town t,String id){return t.industryStocks.getOrDefault(key(id),0);}
    static boolean deployed(TownLife.Town t,String id){return phase(t,id)==3;}
    static boolean linkedProgrammer(TownLife.Town t,TownLife.Citizen c){
        return RebirthLife.linkedAdult(t,c)&&c.employed&&"Programmer".equals(c.occupation)
                &&EducationLife.get(t,c,"diploma-programming")>0&&EducationLife.get(t,c,"technical")>=45;
    }
    record ProjectView(String id,String title,String effect,int wage,String phase,String nextAction,int minutes,boolean deployed,boolean canAdvance,String branch,String command,String explanation){}
    static List<ProjectView> projects(TownLife.Town t,int day,int minute){
        var c=RebirthLife.child(t);
        if(!linkedProgrammer(t,c))return List.of();
        boolean at=RebirthLife.awakePresent(c)&&!RebirthLife.atHome(c)&&c.place==TownLife.Place.OFFICE
                &&!EmploymentLife.sick(c,day)&&c.fatigue<85&&minute>=480;
        return PROJECTS.stream().map(p->{
            int phase=phase(t,p.id()),stage=gitStage(t,p.id());
            String next,command,explanation;int duration;
            if(phase==3){next="Complete";command="";duration=0;explanation="Release deployed to the city.";}
            else if(phase==0&&stage==0){next="Create branch";command="branch";duration=5;explanation="git switch -c "+branch(p.id())+" — isolate your work from main.";}
            else if(phase==0){next="Develop";command="code";duration=60;explanation="Edit the method and pass the acceptance tests before committing.";}
            else if(phase==1&&stage==1){next="Commit";command="commit";duration=5;explanation="git add . && git commit — record a tested change in your branch.";}
            else if(phase==1&&stage==2){next="Open pull request";command="pr";duration=10;explanation="git push -u origin "+branch(p.id())+" — ask the team to review a change into main.";}
            else if(phase==1){next="CI and code review";command="review";duration=60;explanation="The simulated pipeline checks acceptance tests; reviewer checks the business rule and boundary cases.";}
            else if(stage<4){next="Merge pull request";command="merge";duration=5;explanation="Merge the approved pull request into main. Deployment is a separate step.";}
            else{next="Deploy";command="deploy";duration=30;explanation="Release the approved version to the client; payment follows deployment.";}
            return new ProjectView(p.id(),p.title(),p.effect(),p.wage(),List.of("OPEN","DEVELOPED","TESTED","DEPLOYED").get(phase),
                    next,duration,phase==3,at&&phase<3&&EducationLife.get(t,c,"java-onboarded")>0
                    &&minute+duration<=1080&&t.employerFunds.getOrDefault(p.account(),0)>=p.wage(),branch(p.id()),command,explanation);
        }).toList();
    }
    static RebirthLife.HomeAction advance(TownLife.Town t,int day,int minute,String id){
        var view=ready(t,day,minute,id);
        if(!List.of("review","deploy").contains(view.command()))
            throw new IllegalArgumentException("Complete the required Git step: "+view.nextAction()+".");
        return progress(t,day,minute,id);
    }
    static RebirthLife.HomeAction git(TownLife.Town t,int day,int minute,String id,String command){
        var c=RebirthLife.requireAdult(t);var view=ready(t,day,minute,id);
        if(!view.command().equals(command)||!List.of("branch","commit","pr","merge").contains(command))
            throw new IllegalArgumentException("Git step out of order: "+view.nextAction()+".");
        int stage=switch(command){case "branch"->1;case "commit"->2;case "pr"->3;case "merge"->4;default->throw new IllegalArgumentException("Unknown Git step.");};
        RebirthLife.travel(t,TownLife.Place.OFFICE,day);
        t.industryStocks.put(gitKey(id),stage);
        return new RebirthLife.HomeAction(view.minutes(),view.nextAction()+" completed on "+view.branch()+".");
    }
    private static ProjectView ready(TownLife.Town t,int day,int minute,String id){
        return projects(t,day,minute).stream().filter(v->v.id().equals(id)&&v.canAdvance()).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Software work unavailable: check qualification, onboarding, location, hours and client funds."));
    }
    static RebirthLife.HomeAction submit(TownLife.Town t,int day,int minute,String id,String code){
        var c=RebirthLife.requireAdult(t);
        if(EducationLife.get(t,c,"java-onboarded")==0)throw new IllegalArgumentException("Complete the company onboarding first.");
        if(!ready(t,day,minute,id).command().equals("code"))throw new IllegalArgumentException("Create a branch before submitting code.");
        JavaTicketLife.verify(id,code);
        return progress(t,day,minute,id);
    }
    private static RebirthLife.HomeAction progress(TownLife.Town t,int day,int minute,String id){
        var c=RebirthLife.requireAdult(t);
        var view=ready(t,day,minute,id);
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
