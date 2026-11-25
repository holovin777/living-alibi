package com.holovin.npclife;

/** Abstract turn-based, non-contact sports practice. No damage or real fighting instructions. */
final class KumiteLife {
    private KumiteLife(){}
    static final int ROUNDS=5, FEE=5;
    static int get(TownLife.Town t,TownLife.Citizen c,String key){return DojoLife.get(t,c,"match-"+key);}
    static void set(TownLife.Town t,TownLife.Citizen c,String key,int value){DojoLife.set(t,c,"match-"+key,value);}
    static TownLife.Citizen partner(TownLife.Town t,TownLife.Citizen c){
        return DemographyLife.byIndex(t,get(t,c,"partner"));
    }
    static boolean present(TownLife.Town t,TownLife.Citizen p,int day,int minute){
        return p!=null&&DojoCommunityLife.view(t,day,minute).present().stream().anyMatch(m->m.id().equals(p.id));
    }
    static TownLife.Citizen candidate(TownLife.Town t,int day,int minute){
        return DojoCommunityLife.view(t,day,minute).present().stream()
            .filter(m->m.role().equals("student")).map(m->t.people.get(m.id()))
            .filter(p->p!=null&&p.fatigue<80&&p.health>=65).findFirst().orElse(null);
    }
    static boolean active(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        int round=get(t,c,"round");
        return get(t,c,"day")==day&&round>=1&&round<=ROUNDS&&RebirthLife.awakePresent(c)&&
            c.place==TownLife.Place.SCHOOL&&present(t,partner(t,c),day,minute)&&
            DojoCommunityLife.view(t,day,minute).instructorPresent();
    }
    static String cue(TownLife.Town t,TownLife.Citizen c){
        var p=partner(t,c);
        return switch(Math.floorMod((p==null?0:p.personalSeed)+get(t,c,"round")+get(t,c,"day"),3)){
            case 0 -> "close";case 1 -> "far";default -> "opening";
        };
    }
    record View(boolean active,boolean canStart,String partnerId,String partnerName,int round,int score,int partnerScore,String cue,boolean completed){}
    static View view(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(!RebirthLife.linkedAdult(t,c))return null;
        boolean active=active(t,c,day,minute);
        var p=active?partner(t,c):candidate(t,day,minute);
        boolean start=!active&&DojoLife.view(t,c,day,minute).canTrain()&&p!=null&&
            DojoCommunityLife.view(t,day,minute).instructorPresent();
        return new View(active,start,p==null?null:p.id,p==null?null:p.name,get(t,c,"round"),
            get(t,c,"score"),get(t,c,"opponent"),active?cue(t,c):null,get(t,c,"round")==ROUNDS+1);
    }
    static RebirthLife.HomeAction act(TownLife.Town t,int day,int minute,String action){
        var c=RebirthLife.requireAdult(t);
        if(action.equals("start")){
            var v=view(t,c,day,minute);
            if(!v.canStart())throw new IllegalArgumentException("A supervised partner session is unavailable.");
            var p=candidate(t,day,minute);
            int school=Math.addExact(t.employerFunds.getOrDefault("school",0),FEE);
            c.money-=FEE;t.employerFunds.put("school",school);
            set(t,c,"partner",p.generationIndex);set(t,c,"day",day);set(t,c,"round",1);
            set(t,c,"score",0);set(t,c,"opponent",0);
            SportLife.set(t,c,"last-day",day);
            RebirthLife.holdPublicPlace(t,c);
            return new RebirthLife.HomeAction(1,"Started supervised non-contact kumite.");
        }
        if(action.equals("stop")){
            if(get(t,c,"round")<1||get(t,c,"round")>ROUNDS)throw new IllegalArgumentException("No unfinished practice.");
            set(t,c,"round",0);
            return new RebirthLife.HomeAction(0,"Ended practice without completing the round.");
        }
        if(!active(t,c,day,minute))throw new IllegalArgumentException("The supervised session is no longer available.");
        if(!java.util.List.of("guard","step","touch").contains(action))throw new IllegalArgumentException("Unknown practice choice.");
        String expected=switch(cue(t,c)){case "close" -> "guard";case "far" -> "step";default -> "touch";};
        String counter=action.equals(expected)?"score":"opponent";
        set(t,c,counter,get(t,c,counter)+1);
        int round=get(t,c,"round")+1;set(t,c,"round",round);
        c.fatigue=Math.min(100,c.fatigue+2);c.thirst=Math.min(100,c.thirst+1);
        if(round>ROUNDS){
            DojoLife.set(t,c,"kumite",Math.min(100,DojoLife.get(t,c,"kumite")+1));
            DojoLife.set(t,c,"sessions",Math.addExact(DojoLife.get(t,c,"sessions"),1));
            EducationLife.learn(t,c,"fitness",1);
            SportLife.set(t,c,"karate",Math.min(100,SportLife.get(t,c,"karate")+2));
            return new RebirthLife.HomeAction(1,"Completed five controlled practice exchanges.");
        }
        return new RebirthLife.HomeAction(1,action.equals(expected)?"Controlled response scored a point.":"Partner scored a point. Reset safely.");
    }
}
