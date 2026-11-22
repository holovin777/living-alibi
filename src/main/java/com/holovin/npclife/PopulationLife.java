package com.holovin.npclife;

import java.util.List;

/** Lightweight independent physiology, jobs and travel for generated residents. */
final class PopulationLife {
    private PopulationLife() { }
    private static int clamp(int n){return Math.max(0,Math.min(100,n));}

    static void tick(TownLife.Town town,WorldState clock,List<TownLife.Event> events,
                     List<TownLife.Observation> observations) {
        for(var c:town.people.values()) {
            if(c.generationIndex<=0 || !c.alive)continue;
            lifeTick(town,c,clock,events,observations);
        }
    }

    private static void lifeTick(TownLife.Town town,TownLife.Citizen c,WorldState clock,
                                 List<TownLife.Event> events,List<TownLife.Observation> observations) {
        int minute=clock.minute;
        if(minute==0 && clock.day>1 && (clock.day-1)%365==0)c.age++;
        // Keep each resident's need cycles independent of everyone else's.
        int hungerPhase=Math.floorMod(c.generationIndex*7,12);
        int thirstPhase=Math.floorMod(c.generationIndex*3,10);
        if((minute+hungerPhase)%12==0)c.hunger=clamp(c.hunger+1);
        if((minute+thirstPhase)%10==0)c.thirst=clamp(c.thirst+1);
        boolean sleeping=minute<390||minute>=1320;
        if(minute%15==0)c.fatigue=clamp(c.fatigue+(sleeping?-3:1));
        boolean workday=(clock.day-1)%7<5;
        boolean working=workday && minute>=420+c.scheduleOffset && minute<960+c.scheduleOffset;
        boolean marketOpen=minute>=420 && minute<1200;
        boolean needFood=c.food<2 && c.money>=9;
        boolean needWater=c.water<2 && c.money>=3;
        TownLife.Place desired;
        if(sleeping)desired=c.home;
        else if(marketOpen && (needFood||needWater)) desired=TownLife.Place.MARKET;
        else if(working)desired=c.workplace;
        else if(minute>=960 && minute<1200) {
            // The same resident favors a location consistently for a given day.
            int preference=Math.floorMod((int)(c.personalSeed^(clock.day*31L)),4);
            desired=preference==0?TownLife.Place.CAFE:(preference==1?TownLife.Place.LIBRARY:TownLife.Place.PARK);
        } else desired=c.home;
        if(c.travelRemaining>0 || c.place!=desired) {
            TownLife.move(town,c,desired,clock,events,observations);
            if(c.travelRemaining>0){c.activity="TRAVELING";return;}
        }
        if(c.place==TownLife.Place.MARKET && marketOpen) {
            if(c.food<2 && c.money>=9) {
                c.money-=9;c.food+=3;
                TownLife.publish(town,clock,c.id,c.place,c.name+" purchased groceries.",events,observations);
            }
            if(c.water<2 && c.money>=3) {
                c.money-=3;c.water+=3;
                TownLife.publish(town,clock,c.id,c.place,c.name+" purchased water.",events,observations);
            }
        }
        if(c.hunger>=65 && c.food>0) {
            c.food--;c.hunger=clamp(c.hunger-50);
            TownLife.publish(town,clock,c.id,c.place,c.name+" ate a meal.",events,observations);
        }
        if(c.thirst>=65 && c.water>0) {
            c.water--;c.thirst=clamp(c.thirst-52);
            TownLife.publish(town,clock,c.id,c.place,c.name+" drank water.",events,observations);
        }
        if(sleeping)c.activity="SLEEPING";
        else if(working && c.place==c.workplace){
            c.activity="WORKING";
            if(minute%60==0){
                c.money+=11;
                TownLife.publish(town,clock,c.id,c.place,c.name+" earned a day's hourly wage.",events,observations);
            }
        } else c.activity=c.place==TownLife.Place.MARKET?"SHOPPING":"FREE_TIME";
        if(minute%60==0 && (c.hunger>93||c.thirst>93))c.health=clamp(c.health-2);
        if(minute%180==0 && c.hunger<65 && c.thirst<60 && c.fatigue<85)c.health=clamp(c.health+1);
        if(c.health==0){
            c.alive=false;c.activity="DECEASED";
            TownLife.publish(town,clock,c.id,c.place,c.name+" died from declining health.",events,observations);
        }
    }
}
