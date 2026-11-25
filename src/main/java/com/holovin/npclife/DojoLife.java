package com.holovin.npclife;

import java.util.List;

/** Fictional Shotokan club syllabus; rank thresholds are game-specific. */
final class DojoLife {
    private DojoLife(){}
    static final List<String> BELTS=List.of("White","Yellow","Orange","Green","Blue","Brown","Black");
    static final List<String> MODES=List.of("kihon","kata","kumite");
    static final int EXAM_FEE=15;
    static int get(TownLife.Town t,TownLife.Citizen c,String key){return EducationLife.get(t,c,"dojo-"+key);}
    static void set(TownLife.Town t,TownLife.Citizen c,String key,int value){EducationLife.set(t,c,"dojo-"+key,value);}
    record View(String belt,String nextBelt,int kihon,int kata,int kumite,int required,int sessions,boolean canTrain,boolean canExam){}
    static boolean open(int day,int minute){
        int weekday=Math.floorMod(day-1,7);
        return (weekday==0||weekday==2||weekday==4)&&minute>=1080&&minute+60<=1260;
    }
    static View view(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(!RebirthLife.linkedAdult(t,c))return null;
        int rank=Math.min(BELTS.size()-1,get(t,c,"rank"));
        int required=3*(rank+1);
        int k=get(t,c,"kihon"),a=get(t,c,"kata"),u=get(t,c,"kumite");
        var session=SportLife.SESSIONS.stream().filter(s->s.id().equals("karate")).findFirst().orElseThrow();
        boolean ready=open(day,minute)&&SportLife.available(t,c,session,day,minute);
        boolean exam=ready&&rank<BELTS.size()-1&&k>=required&&a>=required&&u>=required&&c.money>=EXAM_FEE;
        return new View(BELTS.get(rank),rank+1<BELTS.size()?BELTS.get(rank+1):null,k,a,u,required,get(t,c,"sessions"),ready,exam);
    }
    static RebirthLife.HomeAction act(TownLife.Town t,int day,int minute,String action){
        var c=RebirthLife.requireAdult(t);var v=view(t,c,day,minute);
        if("exam".equals(action)){
            if(!v.canExam())throw new IllegalArgumentException("The belt examination requirements are not met.");
            int funds=Math.addExact(t.employerFunds.getOrDefault("school",0),EXAM_FEE);
            c.money-=EXAM_FEE;t.employerFunds.put("school",funds);
            set(t,c,"rank",get(t,c,"rank")+1);
            SportLife.set(t,c,"last-day",day);
            c.fatigue=Math.min(100,c.fatigue+10);
            RebirthLife.holdPublicPlace(t,c);
            return new RebirthLife.HomeAction(60,"Passed the dojo belt examination.");
        }
        if(!MODES.contains(action)||!v.canTrain())throw new IllegalArgumentException("The dojo class is unavailable.");
        var result=SportLife.train(t,day,minute,"karate");
        DojoCommunityLife.found(t,day);
        set(t,c,action,Math.min(100,get(t,c,action)+1));
        set(t,c,"sessions",Math.addExact(get(t,c,"sessions"),1));
        return new RebirthLife.HomeAction(result.minutes(),"Completed a Shotokan class.");
    }
}
