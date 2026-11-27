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
        if(NpcJusticeLife.confined(town,c,clock)){
            NpcJusticeLife.custody(c);
            return;
        }
        // A resident who agreed to speak stays here for this conversation
        // minute, rather than starting their autonomous commute mid-sentence.
        // GameSession removes this transient flag before persisting the world.
        if(TownLife.inPlayerConversation(town,c)) {
            if(!PoliceLife.staff(c)&&!(c.place==TownLife.Place.SCHOOL
                    &&("STUDYING".equals(c.activity)||"Teacher".equals(c.occupation)&&"WORKING".equals(c.activity))))
                c.activity="SOCIALIZING";
            return;
        }
        if(RebirthLife.linkedAdult(town,c)&&minute%60==0){
            EducationLife.set(town,c,"dirt",Math.min(100,EducationLife.get(town,c,"dirt")+2));
        }
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
        boolean itEmployee=ITStaffLife.isEmployee(town,c);
        boolean stallEmployee=MarketStallLife.isEmployee(town,c);
        boolean carrier=LogisticsLife.isDriver(town,c);
        TownLife.Place freightDestination=LogisticsLife.activeDriverDestination(town,c);
        boolean freightRun=carrier && freightDestination!=null && !sick;
        boolean journalist=NewspaperLife.reporter(c);
        boolean foodWorker=RetailLife.foodRole(c.occupation);
        boolean publicSafety=PoliceLife.staff(c);
        boolean working=!child&&!retired&&c.employed && !sick && (publicSafety?PoliceLife.scheduled(c,clock.day,minute):(foodWorker?(clock.day-1)%7!=0:workday)) &&
                (publicSafety?true:itEmployee?ITStaffLife.scheduled(town,c,clock.day,minute):journalist?(minute>=480&&minute<=960):carrier?(minute>=510 && minute<840):shopEmployee?(minute>=660 && minute<840):stallEmployee?MarketStallLife.scheduled(town,clock.day,minute):foodWorker?(minute>=660&&minute<1140):
                (minute>=420+c.scheduleOffset && minute<960+c.scheduleOffset));
        boolean marketOpen=minute>=420 && minute<1200;
        boolean needFood=c.food<2 && (c.money>=9||MarketStallLife.offer(town,c,clock.day,minute)!=null);
        boolean needWater=c.water<2;
        // A repaired public fountain provides an affordable alternative to
        // market purchases, but only while finite filtered water is available.
        boolean publicWater=needWater && UtilityLife.status(town).fountainWorking()
                && town.cleanWater>0;
        boolean condominiumRepair=CondominiumLife.assigned(town,c,clock);
        boolean condominiumCleaning=CondominiumLife.cleaningAssigned(town,c,clock);
        boolean postalRound=PostalLife.assigned(town,c,clock);
        TownLife.Place desired;
        TownLife.Place manual=child?RebirthLife.childOuting(town,c,clock.day,minute):RebirthLife.manualDestination(town,c);
        if(RebirthLife.outsideHome(town,c)&&(sleeping||sick)){
            RebirthLife.resumeRoutine(town);manual=null;
        }
        if(sick)desired=!sleeping&&manual==TownLife.Place.CLINIC?TownLife.Place.CLINIC:c.home;
        else if(manual!=null)desired=manual;
        else if(condominiumRepair||condominiumCleaning||postalRound)desired=TownLife.Place.NORTH_RESIDENCES;
        else if(freightRun)desired=freightDestination;
        else if(!publicSafety&&!child&&!carrier&&!RebirthLife.linkedAdult(town,c)&&PublicSafetyLife.alarmed(town,c,clock.day,minute))desired=c.home;
        else if(sleeping)desired=c.home;
        else if(child)desired=c.age>=6&&workday&&minute>=480&&minute<780?TownLife.Place.SCHOOL:c.home;
        else if(itEmployee && working)desired=TownLife.Place.OFFICE;
        else if(carrier && working)desired=c.workplace;
        else if(shopEmployee && working)desired=TownLife.Place.WORKSHOP;
        else if(stallEmployee && working)desired=TownLife.Place.MARKET;
        else if(marketOpen && needFood)desired=CityExpansionLife.outer(c.home)?c.home:TownLife.Place.MARKET;
        else if(marketOpen && publicWater&&!CityExpansionLife.outer(c.home))desired=TownLife.Place.PARK;
        else if(marketOpen && needWater && c.money>=3)desired=CityExpansionLife.outer(c.home)?c.home:TownLife.Place.MARKET;
        else if(publicSafety&&working)desired=PoliceLife.assignment(town,c,clock.day,minute);
        else if(journalist&&working)desired=NewspaperLife.assignment(clock.day,minute);
        else if(working)desired=c.workplace;
        else if(PhoneLife.playerHomeWindow(town,c,clock))desired=HousingLife.HOUSE_DISTRICT;
        else if(PhoneLife.homeVisitWindow(town,c,clock))desired=c.home;
        else if(PhoneLife.meetingWindow(town,c,clock))desired=TownLife.Place.CAFE;
        else if(DojoCommunityLife.scheduled(town,c,clock.day,minute))desired=TownLife.Place.SCHOOL;
        else if(minute>=960 && minute<1200) {
            // The same resident favors a location consistently for a given day.
            // Unlit parks lose visitors at dusk; they choose a covered cafe.
            int preference=Math.floorMod((int)(c.personalSeed^(clock.day*31L)),4);
            if(c.generationIndex>48 && (clock.day+c.generationIndex)%2==0)desired=CityExpansionLife.leisure(c,clock.day);
            else if(preference==0)desired=TownLife.Place.CAFE;
            else if(preference==1)desired=TownLife.Place.LIBRARY;
            else if(minute>=1080 && !UtilityLife.status(town).lightsWorking())
                desired=TownLife.Place.CAFE;
            else if(preference==2)desired=TownLife.Place.CENTRO;
            else desired=TownLife.Place.PARK;
        } else desired=c.home;
        NpcTransportLife.prepare(town,c,clock,events,observations);
        if(c.travelRemaining>0 || c.place!=desired) {
            NpcTransportLife.depart(town,c,desired,clock);
            TownLife.move(town,c,desired,clock,events,observations);
            if(c.travelRemaining>0){c.activity="TRAVELING";return;}
        }
        // During a short illness, essential supplies may be delivered to the private unit.
        if((sick||child||RebirthLife.stayingHome(town,c)) && c.place==c.home && marketOpen) {
            if(c.food<2 && c.money>=12) {
                c.money-=12;c.food+=3;
                if(child||RebirthLife.stayingHome(town,c))town.employerFunds.merge("shop-assistant",12,Math::addExact);
                TownLife.publish(town,clock,c.id,c.place,c.name+" received groceries at home.",events,observations);
            }
            if(c.water<2 && c.money>=6) {
                c.money-=6;c.water+=3;
                if(child||RebirthLife.stayingHome(town,c))town.employerFunds.merge("shop-assistant",6,Math::addExact);
                TownLife.publish(town,clock,c.id,c.place,c.name+" received drinking water at home.",events,observations);
            }
        }
        if(CityExpansionLife.outer(c.home)&&c.place==c.home&&marketOpen&&!child){
            if(c.food<2&&c.money>=9){c.money-=9;c.food+=3;town.employerFunds.merge("shop-assistant",9,Math::addExact);}
            if(c.water<2&&c.money>=3){c.money-=3;c.water+=3;town.employerFunds.merge("shop-assistant",3,Math::addExact);}
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
        if(!child&&!working&&!sick&&!RebirthLife.linkedAdult(town,c)&&c.place==TownLife.Place.CENTRO&&minute%60==0)
            RetailLife.foodCustomer(town,c,clock,events,observations);
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
        else if(child)c.activity=c.place==TownLife.Place.SCHOOL?"STUDYING":c.place==c.home?"AT_HOME":"FREE_TIME";
        else if(RebirthLife.outsideHome(town,c))c.activity="OUTSIDE_HOME";
        else if(RebirthLife.stayingHome(town,c)&&c.place==c.home)c.activity="AT_HOME";
        else if(retired)c.activity="RETIRED";
        else if(condominiumCleaning&&c.place==TownLife.Place.NORTH_RESIDENCES)c.activity="WORKING";
        else if(!c.employed)c.activity=c.place==TownLife.Place.MARKET?"SHOPPING":"JOB_SEARCH";
        else if((condominiumRepair||postalRound)&&c.place==TownLife.Place.NORTH_RESIDENCES)c.activity="WORKING";
        else if(freightRun)c.activity="WORKING";
        else if(journalist&&working&&c.place==NewspaperLife.assignment(clock.day,minute))c.activity="WORKING";
        else if(working && (c.place==c.workplace||publicSafety&&c.place==PoliceLife.assignment(town,c,clock.day,minute))){
            c.activity="WORKING";
            if(minute%60==0){
                if(foodWorker)RetailLife.foodShift(town,c,clock,events,observations);
                else if(!ITStaffLife.OCCUPATION.equals(c.occupation) && !TechnicalWorkLife.linkedEngineer(town,c) && !SoftwareLife.linkedProgrammer(town,c) && !journalist && !stallEmployee && !MarketStallLife.OCCUPATION.equals(c.occupation) && !LogisticsLife.OCCUPATION.equals(c.occupation) && !BusinessLife.payHourly(town,c,clock,events,observations) &&
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
