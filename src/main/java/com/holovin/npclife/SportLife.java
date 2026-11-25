package com.holovin.npclife;

import java.util.List;

/** Voluntary adult training, using existing resident fitness and real needs.
 * Skill levels are fictional progression, not real sporting qualifications. */
final class SportLife {
    private SportLife(){}
    record Session(String id,TownLife.Place place,int minutes,int price,int fatigue,int hunger,int thirst,int fitness,String skill){}
    static final List<Session> SESSIONS=List.of(
        new Session("run",TownLife.Place.PARK,30,0,12,5,8,2,"endurance"),
        new Session("yoga",TownLife.Place.PARK,30,0,3,2,3,1,"flexibility"),
        new Session("karate",TownLife.Place.SCHOOL,60,5,18,7,10,2,"karate"),
        new Session("gym",TownLife.Place.SCHOOL,45,4,14,6,8,2,"strength"),
        new Session("cycling",TownLife.Place.RIVER,45,3,12,5,8,2,"endurance"));
    static int get(TownLife.Town t,TownLife.Citizen c,String k){return EducationLife.get(t,c,"sport-"+k);}
    static void set(TownLife.Town t,TownLife.Citizen c,String k,int n){EducationLife.set(t,c,"sport-"+k,n);}
    record Option(String id,String district,int minutes,int price,boolean available){}
    record View(int fitness,int endurance,int flexibility,int strength,int karate,int sessions,List<Option> options){}
    static boolean available(TownLife.Town t,TownLife.Citizen c,Session s,int day,int minute){
        if(!RebirthLife.linkedAdult(t,c)||!RebirthLife.awakePresent(c)||RebirthLife.atHome(c)||
                c.place!=s.place()||EmploymentLife.sick(c,day)||c.health<65||c.fatigue>65||
                c.hunger>65||c.thirst>60||c.money<s.price()||get(t,c,"last-day")>=day)return false;
        if(minute<420||minute+s.minutes()>1260)return false;
        if(s.place()==TownLife.Place.SCHOOL)return minute>=1020;
        var weather=WeatherLife.at(day,minute);
        return !weather.snowing()&&(!s.id().equals("cycling")||!weather.raining());
    }
    static View view(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(!RebirthLife.linkedAdult(t,c))return null;
        return new View(EducationLife.get(t,c,"fitness"),get(t,c,"endurance"),get(t,c,"flexibility"),
                get(t,c,"strength"),get(t,c,"karate"),get(t,c,"sessions"),
                SESSIONS.stream().map(s->new Option(s.id(),s.place().label,s.minutes(),s.price(),available(t,c,s,day,minute))).toList());
    }
    static RebirthLife.HomeAction train(TownLife.Town t,int day,int minute,String id){
        var c=RebirthLife.requireAdult(t);
        var s=SESSIONS.stream().filter(x->x.id().equals(id)&&available(t,c,x,day,minute)).findFirst()
            .orElseThrow(()->new IllegalArgumentException("This training session is unavailable."));
        if(s.price()>0){
            String account=s.place()==TownLife.Place.SCHOOL?"school":"shop-assistant";
            int funds=Math.addExact(t.employerFunds.getOrDefault(account,0),s.price());
            c.money-=s.price();t.employerFunds.put(account,funds);
        }
        c.fatigue=Math.min(100,c.fatigue+s.fatigue());
        c.hunger=Math.min(100,c.hunger+s.hunger());c.thirst=Math.min(100,c.thirst+s.thirst());
        EducationLife.learn(t,c,"fitness",s.fitness());
        set(t,c,s.skill(),Math.min(100,get(t,c,s.skill())+2));
        set(t,c,"sessions",Math.addExact(get(t,c,"sessions"),1));set(t,c,"last-day",day);
        RebirthLife.holdPublicPlace(t,c);
        return new RebirthLife.HomeAction(s.minutes(),"Completed a training session.");
    }
}
