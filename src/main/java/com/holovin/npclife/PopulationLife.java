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
            EducationLife.attendance(town,c,clock);
            OpinionLife.read(town,c,clock.day,clock.minute);
        }
    }

    private static void lifeTick(TownLife.Town town,TownLife.Citizen c,WorldState clock,
                                 List<TownLife.Event> events,List<TownLife.Observation> observations) {
        int minute=clock.minute;
        boolean child=DemographyLife.child(c),retired="Retired".equals(c.occupation);
        // Keep each resident's need cycles independent of everyone else's.
        int hungerPhase=Math.floorMod(c.generationIndex*7,12);
        int thirstPhase=Math.floorMod(c.generationIndex*3,10);
        if((minute+hungerPhase)%12==0)c.hunger=clamp(c.hunger+1);
        if((minute+thirstPhase)%10==0)c.thirst=clamp(c.thirst+1);
        boolean sleeping=minute<390||minute>=1320;
        if(minute%15==0)c.fatigue=clamp(c.fatigue+(sleeping?-3:1));
        boolean workday=(clock.day-1)%7<5;
        boolean sick=EmploymentLife.sick(c,clock.day);
        // Independent workshops use short real two-hour shifts, not fictitious
        // wages for eight hours to fabricate one small batch of parts.
        boolean shopEmployee=BusinessLife.isEmployee(town,c);
        boolean stallEmployee=MarketStallLife.isEmployee(town,c);
        boolean carrier=LogisticsLife.isDriver(town,c);
        TownLife.Place freightDestination=LogisticsLife.activeDriverDestination(town,c);
        boolean freightRun=carrier && freightDestination!=null && !sick;
        boolean journalist=NewspaperLife.reporter(c);
        boolean working=!child&&!retired&&c.employed && !sick && workday &&
                (journalist?(minute>=480&&minute<=960):carrier?(minute>=510 && minute<840):shopEmployee?(minute>=660 && minute<840):stallEmployee?MarketStallLife.scheduled(town,clock.day,minute):
                (minute>=420+c.scheduleOffset && minute<960+c.scheduleOffset));
        boolean marketOpen=minute>=420 && minute<1200;
        boolean needFood=c.food<2 && (c.money>=9||MarketStallLife.offer(town,c,clock.day,minute)!=null);
        boolean needWater=c.water<2;
        // A repaired public fountain provides an affordable alternative to
        // market purchases, but only while finite filtered water is available.
        boolean publicWater=needWater && UtilityLife.status(town).fountainWorking()
                && town.cleanWater>0;
        TownLife.Place desired;
        TownLife.Place manual=RebirthLife.manualDestination(town,c);
        if(sick)desired=c.home;
        else if(manual!=null)desired=manual;
        else if(freightRun)desired=freightDestination;
        else if(sleeping)desired=c.home;
        else if(child)desired=c.age>=6&&workday&&minute>=480&&minute<780?TownLife.Place.SCHOOL:c.home;
        else if(carrier && working)desired=c.workplace;
        else if(shopEmployee && working)desired=TownLife.Place.WORKSHOP;
        else if(stallEmployee && working)desired=TownLife.Place.MARKET;
        else if(marketOpen && needFood)desired=TownLife.Place.MARKET;
        else if(marketOpen && publicWater)desired=TownLife.Place.PARK;
        else if(marketOpen && needWater && c.money>=3)desired=TownLife.Place.MARKET;
        else if(journalist&&working)desired=NewspaperLife.assignment(clock.day,minute);
        else if(working)desired=c.workplace;
        else if(minute>=960 && minute<1200) {
            // The same resident favors a location consistently for a given day.
            // Unlit parks lose visitors at dusk; they choose a covered cafe.
            int preference=Math.floorMod((int)(c.personalSeed^(clock.day*31L)),4);
            if(preference==0)desired=TownLife.Place.CAFE;
            else if(preference==1)desired=TownLife.Place.LIBRARY;
            else if(minute>=1080 && !UtilityLife.status(town).lightsWorking())
                desired=TownLife.Place.CAFE;
            else if(preference==2)desired=TownLife.Place.CENTRO;
            else desired=TownLife.Place.PARK;
        } else desired=c.home;
        if(c.travelRemaining>0 || c.place!=desired) {
            TownLife.move(town,c,desired,clock,events,observations);
            if(c.travelRemaining>0){c.activity="TRAVELING";return;}
        }
        // During a short illness, essential supplies may be delivered to the private unit.
        if((sick||child) && c.place==c.home && marketOpen) {
            if(c.food<2 && c.money>=12) {
                c.money-=12;c.food+=3;
                if(child)town.employerFunds.merge("shop-assistant",12,Math::addExact);
                TownLife.publish(town,clock,c.id,c.place,c.name+" received groceries at home.",events,observations);
            }
            if(c.water<2 && c.money>=6) {
                c.money-=6;c.water+=3;
                if(child)town.employerFunds.merge("shop-assistant",6,Math::addExact);
                TownLife.publish(town,clock,c.id,c.place,c.name+" received drinking water at home.",events,observations);
            }
        }
        if(c.place==TownLife.Place.MARKET && marketOpen) {
            if(stallEmployee && working)MarketStallLife.pay(town,c,clock,events,observations);
            String stallSale=MarketStallLife.purchase(town,c,clock.day,minute);
            if(stallSale!=null)TownLife.publish(town,clock,c.id,c.place,stallSale,events,observations);
            else if(c.food<2 && c.money>=9) {
                c.money-=9;c.food+=3;
                TownLife.publish(town,clock,c.id,c.place,c.name+" purchased groceries.",events,observations);
            }
            if(c.water<2 && c.money>=3) {
                c.money-=3;c.water+=3;
                TownLife.publish(town,clock,c.id,c.place,c.name+" purchased water.",events,observations);
            }
        }
        if(!sleeping && c.hunger>=65 && c.food>0) {
            c.food--;c.hunger=clamp(c.hunger-50);
            TownLife.publish(town,clock,c.id,c.place,c.name+" ate a meal.",events,observations);
        }
        if(!sleeping && c.thirst>=65 && c.water>0) {
            c.water--;c.thirst=clamp(c.thirst-52);
            TownLife.publish(town,clock,c.id,c.place,c.name+" drank water.",events,observations);
        }
        if(sleeping)c.activity="SLEEPING";
        else if(sick)c.activity="SICK_LEAVE";
        else if(child)c.activity=c.place==TownLife.Place.SCHOOL?"STUDYING":"AT_HOME";
        else if(retired)c.activity="RETIRED";
        else if(!c.employed)c.activity=c.place==TownLife.Place.MARKET?"SHOPPING":"JOB_SEARCH";
        else if(freightRun)c.activity="WORKING";
        else if(journalist&&working&&c.place==NewspaperLife.assignment(clock.day,minute))c.activity="WORKING";
        else if(working && c.place==c.workplace){
            c.activity="WORKING";
            if(minute%60==0){
                if(!journalist && !stallEmployee && !MarketStallLife.OCCUPATION.equals(c.occupation) && !LogisticsLife.OCCUPATION.equals(c.occupation) && !BusinessLife.payHourly(town,c,clock,events,observations) &&
                        !IndustryLife.payHourly(town,c,clock,events,observations)) {
                    // Existing unrelated occupations retain their original
                    // placeholder wage model until their companies are modeled.
                    c.money+=11;
                    TownLife.publish(town,clock,c.id,c.place,
                            c.name+" earned EUR 11 for one hour of work.",events,observations);
                }
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
