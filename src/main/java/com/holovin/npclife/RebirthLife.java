package com.holovin.npclife;
import java.util.List;
import java.util.ArrayList;

/** Persistent family selection and linked newborn preview; adult controls remain locked. */
final class RebirthLife {
    private RebirthLife(){}
    static final String PHASE="rebirth:phase", CHILD="rebirth:child";
    static final int ADULT=0, CHOICE=1, CHILDHOOD=2;
    static void ensure(TownLife.Town t){
        t.industryStocks.putIfAbsent(PHASE,ADULT);t.industryStocks.putIfAbsent(CHILD,0);
    }
    static int phase(TownLife.Town t){return t.industryStocks.getOrDefault(PHASE,ADULT);}
    static boolean active(TownLife.Town t){return phase(t)!=ADULT;}
    static TownLife.Citizen child(TownLife.Town t){return DemographyLife.byIndex(t,t.industryStocks.getOrDefault(CHILD,0));}
    static void prepare(TownLife.Town t,int day,int minute){
        if(active(t))return;
        PlayerLife.beginNewLife(t,day,minute);
        t.player.alive=false;t.player.health=0;t.player.money=0;
        t.industryStocks.put(PHASE,CHOICE);t.industryStocks.put(CHILD,0);
    }
    record Family(int first,int second,String firstName,String secondName,String firstJob,String secondJob,String district){}
    static List<Family> families(TownLife.Town t,int day){
        var result=new ArrayList<Family>();
        for(var a:t.people.values())if(DemographyLife.parentAge(a)){
            var b=DemographyLife.byIndex(t,DemographyLife.get(t,a,"partner"));
            if(b!=null&&a.generationIndex<b.generationIndex&&DemographyLife.canBirth(t,a,b,day))
                result.add(new Family(a.generationIndex,b.generationIndex,a.name,b.name,a.occupation,b.occupation,a.home.label));
        }
        return List.copyOf(result);
    }
    static String choose(TownLife.Town t,int day,int first,int second){
        if(phase(t)!=CHOICE)throw new IllegalArgumentException("No family selection is pending.");
        if(families(t,day).stream().noneMatch(f->f.first()==first&&f.second()==second))
            throw new IllegalArgumentException("This family is not currently eligible.");
        var a=DemographyLife.byIndex(t,first);var b=DemographyLife.byIndex(t,second);
        var newborn=DemographyLife.birth(t,a,b,day);
        if(newborn==null)throw new IllegalArgumentException("This family is no longer available.");
        t.industryStocks.put(CHILD,newborn.generationIndex);t.industryStocks.put(PHASE,CHILDHOOD);
        synchronize(t);
        return "Born as "+newborn.name+". Childhood preview: follow the family and advance one day at a time.";
    }
    static void synchronize(TownLife.Town t){
        if(phase(t)!=CHILDHOOD)return;
        var c=child(t);
        if(c==null||!c.alive){
            t.industryStocks.put(CHILD,0);t.industryStocks.put(PHASE,CHOICE);
            t.player.health=0;
            return;
        }
        // The child is the only living identity; the old visitor shell cannot
        // earn, shop, travel independently, vote or consume duplicate supplies.
        t.player.place=c.place;t.player.destination=c.place;
        t.player.health=c.health;t.player.hunger=c.hunger;t.player.thirst=c.thirst;t.player.fatigue=c.fatigue;
    }
    // Bound each request: early childhood is faster, adolescence is slower.
    static int maxWaitDays(TownLife.Town t){
        if(phase(t)==CHOICE)return 1;
        var c=child(t);
        if(phase(t)!=CHILDHOOD||c==null||!c.alive||c.age>=18)return 0;
        return c.age<6?7:c.age<13?3:1;
    }
    static boolean canContinueWaiting(TownLife.Town t,int initialPhase,int initialMaxDays){
        return phase(t)==initialPhase&&maxWaitDays(t)>0&&maxWaitDays(t)==initialMaxDays;
    }
    static String adultFallback(TownLife.Town t){
        if(phase(t)!=CHOICE)throw new IllegalArgumentException("Only available before choosing a family.");
        var p=new PlayerLife.Player();p.incarnation=t.player.incarnation;t.player=p;
        t.industryStocks.put(PHASE,ADULT);t.industryStocks.put(CHILD,0);
        return "Started an adult life in the park.";
    }
    record Report(String phase,List<Family> families,String childName,int age,String parentA,String parentB,String district,boolean preview,int maxWaitDays){}
    static Report report(TownLife.Town t,int day){
        int phase=phase(t);var c=child(t);
        var a=c==null?null:DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-a"));
        var b=c==null?null:DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-b"));
        return new Report(phase==CHOICE?"CHOICE":phase==CHILDHOOD?"CHILDHOOD":"ADULT",phase==CHOICE?families(t,day):List.of(),
            c==null?null:c.name,c==null?0:c.age,a==null?null:a.name,b==null?null:b.name,c==null?null:c.home.label,phase==CHILDHOOD,maxWaitDays(t));
    }
}
