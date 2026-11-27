package com.holovin.npclife;

import java.util.List;
import java.util.Comparator;

/** Supervised practice with a real resident. Ranks are career milestones,
 * not statutory professional titles or automatic changes of occupation. */
final class ApprenticeshipLife {
    private ApprenticeshipLife(){}
    static final int DURATION=30;
    static int get(TownLife.Town t,String f){return VocationalLife.get(t,"practice:"+f);}
    static void set(TownLife.Town t,String f,int n){VocationalLife.set(t,"practice:"+f,n);}
    static boolean qualified(TownLife.Town t){
        return VocationalLife.certified(t,"electrical")||VocationalLife.certified(t,"mechanical");
    }
    static boolean present(TownLife.Town t){
        return !RebirthLife.active(t)&&t.player.alive&&t.player.travelRemaining==0
            &&t.player.place==TownLife.Place.WORKSHOP&&t.player.fatigue<85;
    }
    static boolean mentor(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.generationIndex>0&&c.workplace==TownLife.Place.WORKSHOP
            &&List.of("Mechanic","Electrician","Engineer").contains(c.occupation)
            &&WorkplaceHierarchy.onDuty(c,s.day)&&WorkplaceHierarchy.tenure(c,s.day)>=14
            &&EducationLife.get(t,c,"technical")>=45&&c.fatigue<80
            &&!RebirthLife.linkedAdult(t,c)&&!NpcJusticeLife.confined(t,c,s)
            &&!TownLife.inPlayerConversation(t,c);
    }
    static List<TownLife.Citizen> mentors(TownLife.Town t,WorldState s){
        return t.people.values().stream().filter(c->mentor(t,c,s))
            .sorted(Comparator.<TownLife.Citizen>comparingInt(c->EducationLife.get(t,c,"technical")).reversed()
                .thenComparing(c->c.id)).toList();
    }
    static TownLife.Citizen current(TownLife.Town t){
        return NpcVehicleCrimeLife.person(t,get(t,"mentor"));
    }
    static boolean startReady(TownLife.Town t,WorldState s){
        return qualified(t)&&present(t)&&s.minute>=480&&s.minute+DURATION+5<=960
            &&(s.day-1)%7<5&&get(t,"last-day")<s.day&&get(t,"mentor")==0;
    }
    static void clear(TownLife.Town t){
        for(String f:List.of("mentor","day","start","fault"))set(t,f,0);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(get(t,"mentor")>0&&(get(t,"day")!=s.day||s.minute>=960||!present(t)||!mentor(t,current(t),s)))clear(t);
    }
    static boolean answerReady(TownLife.Town t,WorldState s){
        return qualified(t)&&present(t)&&get(t,"day")==s.day&&get(t,"mentor")>0
            &&s.minute>=get(t,"start")+DURATION&&s.minute+5<=960&&mentor(t,current(t),s);
    }
    record Step(int minutes,String message){}
    static Step act(TownLife.Town t,WorldState s,String action){
        if(action.startsWith("start:")){
            if(!startReady(t,s))throw new IllegalArgumentException("Training requires a certificate, a free weekday and an available mentor.");
            var c=t.people.get(action.substring(6));
            if(!mentor(t,c,s))throw new IllegalArgumentException("That mentor is unavailable.");
            set(t,"mentor",c.generationIndex);set(t,"day",s.day);set(t,"start",s.minute);
            set(t,"fault",1+Math.floorMod(s.day+c.generationIndex,3));set(t,"last-day",s.day);
            return new Step(DURATION,"Supervised practice started with "+c.name+".");
        }
        if(action.equals("cancel")){clear(t);return new Step(1,"Training session cancelled.");}
        if(!action.startsWith("answer:")||!answerReady(t,s))
            throw new IllegalArgumentException("Finish the supervised session with your mentor first.");
        var c=current(t);
        if(!action.substring(7).equals(PlayerRepairLife.part(get(t,"fault")))){
            clear(t);return new Step(5,"Incorrect diagnosis. Try another practice session tomorrow.");
        }
        set(t,"sessions",Math.addExact(get(t,"sessions"),1));
        t.player.engineeringSkill=Math.addExact(t.player.engineeringSkill,1);
        c.fatigue=Math.min(100,c.fatigue+2);
        t.player.fatigue=Math.min(100,t.player.fatigue+3);
        clear(t);return new Step(5,"Mentor approved the diagnosis. Engineering skill +1.");
    }
    static int repairs(TownLife.Town t){
        return PlayerServiceBusiness.yours(t)?PlayerServiceBusiness.get(t,"manual-jobs"):0;
    }
    static String rank(TownLife.Town t){
        if(!qualified(t))return "Student";
        int sessions=get(t,"sessions"),jobs=repairs(t);
        if(sessions<3)return "Trainee";
        if(jobs<5)return "Apprentice";
        if(sessions<5||jobs<15||!VocationalLife.certified(t,"electrical")||!VocationalLife.certified(t,"mechanical"))return "Technician";
        if(PlayerServiceBusiness.yours(t)){
            int staff=PlayerServiceBusiness.workers(t).size();
            int total=PlayerServiceBusiness.get(t,"jobs");
            if(jobs>=25&&total>=40&&staff>=3)return "Technical director";
            if(jobs>=20&&total>=25&&staff>=2)return "Team leader";
        }
        return "Senior technician";
    }
    record Mentor(String id,String name,int skill){}
    record View(String rank,int sessions,int repairs,String mentor,boolean active,boolean canAnswer,String reading,List<Mentor> mentors){}
    static View view(TownLife.Town t,WorldState s){
        var c=current(t);boolean answer=answerReady(t,s);
        return new View(rank(t),get(t,"sessions"),repairs(t),c==null?"":c.name,get(t,"mentor")>0,
            answer,answer?PlayerRepairLife.reading(get(t,"fault")):"",
            startReady(t,s)?mentors(t,s).stream().map(x->new Mentor(x.id,x.name,EducationLife.get(t,x,"technical"))).toList():List.of());
    }
}
