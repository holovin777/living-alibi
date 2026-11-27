package com.holovin.npclife;

import java.util.List;

/** A saved application, an appointment on a subsequent day, and a documented decision.
 * Competition is a simplified estimate from unemployed residents of the related occupation. */
final class InterviewLife {
    private InterviewLife(){}
    static final int DURATION=30;
    private static String key(TownLife.Town t,String job,String field){
        return "job-centre:"+t.player.incarnation+":application:"+job+":"+field;
    }
    private static int get(TownLife.Town t,String job,String field){return t.industryStocks.getOrDefault(key(t,job,field),0);}
    private static void set(TownLife.Town t,String job,String field,int n){t.industryStocks.put(key(t,job,field),n);}
    private static CareerLife.Offer offer(String id){
        return CareerLife.offers().stream().filter(v->v.id().equals(id)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Unknown vacancy."));
    }
    private static boolean open(WorldState s,int minutes){return (s.day-1)%7<5&&s.minute>=540&&s.minute+minutes<=1020;}
    private static int nextWeekday(int day){
        do{day++;}while((day-1)%7>=5);
        return day;
    }
    private static boolean present(TownLife.Town t,CareerLife.Offer v){
        return EmploymentCenterLife.visitor(t)&&t.player.place==v.workplace()&&!v.id().equals(t.player.jobId);
    }
    private static String occupation(String job){
        return switch(job){
            case "shop-assistant" -> "Market vendor";
            case "technician" -> "Mechanic";
            case "engineer" -> "Engineer";
            case "investigator" -> "Detective";
            case "miner" -> "Miner";
            case "machinist" -> "Factory machinist";
            case "farm-worker" -> "Farm worker";
            case "fuel-attendant" -> "Fuel station attendant";
            case "journalist" -> NewspaperLife.OCCUPATION;
            default -> "Transport driver";
        };
    }
    static int competition(TownLife.Town t,CareerLife.Offer v){
        String role=occupation(v.id());
        return (int)t.people.values().stream().filter(c->c.alive&&!c.employed&&DemographyLife.workingAge(c)&&role.equals(c.occupation)).limit(3).count();
    }
    static String submit(TownLife.Town t,WorldState s,String id){
        var v=offer(id);
        if(!present(t,v)||!open(s,15))throw new IllegalArgumentException("Submit your application at the workplace on weekdays 09:00–17:00.");
        if(get(t,id,"appointment")>=s.day)throw new IllegalArgumentException("An interview is already scheduled.");
        if(get(t,id,"retry")>s.day)throw new IllegalArgumentException("Wait until the application cooldown ends.");
        if(CareerLife.skill(t.player,v.subject())<v.requiredSkill())throw new IllegalArgumentException("You need the required skills before applying.");
        if(t.employerFunds.getOrDefault(id,0)<v.wage())throw new IllegalArgumentException("Employer cannot fund a shift right now.");
        set(t,id,"appointment",nextWeekday(s.day));
        set(t,id,"result",0);
        return "Application submitted. Return on the scheduled interview day.";
    }
    static String attend(TownLife.Town t,WorldState s,String id){
        var v=offer(id);
        if(!present(t,v)||!open(s,DURATION)||get(t,id,"appointment")!=s.day)
            throw new IllegalArgumentException("Attend at the workplace on your appointment day, 09:00–17:00.");
        var p=t.player;
        if(FreightLife.active(t)&&!FreightLife.automated(t))throw new IllegalArgumentException("Finish the freight delivery before changing jobs.");
        int skill=CareerLife.skill(p,v.subject());
        int score=50+Math.min(20,Math.max(0,skill-v.requiredSkill())*4)+p.hygiene/5-p.fatigue/5;
        int required=45+competition(t,v)*5;
        boolean funds=t.employerFunds.getOrDefault(id,0)>=v.wage();
        boolean accepted=funds&&skill>=v.requiredSkill()&&score>=required;
        String result=accepted?CareerLife.apply(t,id,s.day):!funds?
                "Interview unsuccessful: employer no longer has sufficient payroll.":
                "Interview unsuccessful: improve your preparation and qualifications before applying again.";
        set(t,id,"appointment",0);
        set(t,id,"result",accepted?1:2);
        if(!accepted)set(t,id,"retry",s.day+3);
        return result;
    }
    record Application(String id,String title,String workplace,int appointmentDay,int retryDay,
                       int result,int competition,boolean canSubmit,boolean canAttend){}
    static List<Application> report(TownLife.Town t,WorldState s){
        if(RebirthLife.active(t))return List.of();
        return CareerLife.offers().stream().map(v->{
            String id=v.id();int appointment=get(t,id,"appointment"),retry=get(t,id,"retry");
            return new Application(id,v.title(),v.workplace().name(),appointment,retry,get(t,id,"result"),
                    competition(t,v),present(t,v)&&open(s,15)&&appointment<s.day&&retry<=s.day&&
                    CareerLife.skill(t.player,v.subject())>=v.requiredSkill()&&t.employerFunds.getOrDefault(id,0)>=v.wage(),
                    present(t,v)&&open(s,DURATION)&&appointment==s.day);
        }).toList();
    }
}
