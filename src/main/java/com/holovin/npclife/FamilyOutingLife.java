package com.holovin.npclife;

import java.util.*;

/** A bounded autonomous family outing, persisted in existing world stock keys. */
final class FamilyOutingLife {
    private FamilyOutingLife(){}
    static final int START=900, END=1050, RETURN_END=1140;
    private static String key(TownLife.Citizen c,String field){return "family-outing:"+c.id+":"+field;}
    private static int get(TownLife.Town t,TownLife.Citizen c,String field){return t.industryStocks.getOrDefault(key(c,field),0);}
    private static void put(TownLife.Town t,TownLife.Citizen c,String field,int n){t.industryStocks.put(key(c,field),n);}
    private static boolean well(TownLife.Citizen c){
        return c.alive&&c.health>=60&&c.hunger<70&&c.thirst<70&&c.fatigue<80;
    }
    private static boolean controlled(TownLife.Town t,TownLife.Citizen c){
        return RebirthLife.active(t)&&RebirthLife.child(t)==c;
    }
    static void plan(TownLife.Town t,WorldState clock){
        if(clock.minute!=START||(clock.day-1)%7<5)return;
        var weather=WeatherLife.at(clock.day,clock.minute);
        if(weather.raining()||weather.snowing()||weather.temperatureC()<5||weather.temperatureC()>34)return;
        for(var child:t.people.values()){
            if(child.age<3||child.age>12||!DemographyLife.child(child)||controlled(t,child)||
                !well(child)||!HomeRoutineLife.atHome(child)||get(t,child,"day")==clock.day)continue;
            var guardian=DemographyLife.guardian(t,child);
            if(guardian==null||guardian.age<18||guardian.generationIndex<=0||controlled(t,guardian)||
                !well(guardian)||!HomeRoutineLife.atHome(guardian)||!child.homeUnit.equals(guardian.homeUnit)||
                EmploymentLife.sick(guardian,clock.day)||EmploymentLife.sick(child,clock.day)||
                Set.of("WORKING","SLEEPING","SICK_LEAVE").contains(guardian.activity)||
                PhoneLife.meetingWindow(t,guardian,clock)||RebirthLife.manualDestination(t,guardian)!=null)continue;
            put(t,child,"day",clock.day);put(t,child,"guardian",guardian.generationIndex);
            put(t,guardian,"day",clock.day);put(t,guardian,"guardian",guardian.generationIndex);
        }
    }
    private static boolean planned(TownLife.Town t,TownLife.Citizen c,int day){
        return get(t,c,"day")==day&&get(t,c,"guardian")>0;
    }
    static TownLife.Place destination(TownLife.Town t,TownLife.Citizen c,WorldState clock){
        if(!planned(t,c,clock.day)||controlled(t,c)||clock.minute<START||clock.minute>=RETURN_END)return null;
        var guardian=DemographyLife.byIndex(t,get(t,c,"guardian"));
        if(clock.minute>=END||guardian==null||!well(guardian)||!well(c)||EmploymentLife.sick(guardian,clock.day)||EmploymentLife.sick(c,clock.day))
            return c.home;
        return TownLife.Place.PARK;
    }
    static String role(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(!planned(t,c,day)||minute<START||minute>=END||!well(c)||!c.presentAt(TownLife.Place.PARK)||
            !c.currentSpace().equals(TownLife.publicSpace(c.place))||controlled(t,c))return "";
        var guardian=DemographyLife.byIndex(t,get(t,c,"guardian"));
        if(guardian==null||!well(guardian)||!guardian.presentAt(TownLife.Place.PARK)||!guardian.currentSpace().equals(TownLife.publicSpace(guardian.place)))return "";
        if(c==guardian){
            return t.people.values().stream().anyMatch(child->child!=guardian&&planned(t,child,day)&&
                get(t,child,"guardian")==guardian.generationIndex&&well(child)&&child.presentAt(TownLife.Place.PARK)&&child.currentSpace().equals(TownLife.publicSpace(child.place)))?"GUARDIAN":"";
        }
        return "PLAYING";
    }
}
