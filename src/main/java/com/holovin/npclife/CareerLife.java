package com.holovin.npclife;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Optional jobs and vocational study for the player. No mandatory quests,
 * free detective badge, or wages created from an unlimited money faucet.
 */
final class CareerLife {
    private CareerLife() { }

    record Offer(String id,String title,TownLife.Place workplace,
                 String subject,int requiredSkill,int wage,int startingPayroll) { }

    static final int LESSON_FEE=10;
    static final int LESSON_MINUTES=60;
    static final int SHIFT_MINUTES=120;

    private static final List<Offer> OFFERS=List.of(
            new Offer(SecurityLife.JOB,"Security guard",TownLife.Place.MARKET,"none",0,24,240),
            new Offer("shop-assistant","Market assistant",TownLife.Place.MARKET,
                    "none",0,24,480),
            new Offer("technician","Workshop technician",TownLife.Place.WORKSHOP,
                    "engineering",3,38,760),
            new Offer("engineer","Junior engineer",TownLife.Place.OFFICE,
                    "engineering",8,62,1240),
            new Offer("investigator","Private investigator",TownLife.Place.OFFICE,
                    "investigation",8,54,1080),
            new Offer("miner","Quarry miner",TownLife.Place.QUARRY,
                    "none",0,30,360),
            new Offer("machinist","Factory machinist",TownLife.Place.FACTORY,
                    "none",0,34,560),
            new Offer("farm-worker","Farm worker",TownLife.Place.FARM,"none",0,26,300),
            new Offer("fuel-attendant","Fuel station attendant",TownLife.Place.FUEL_STATION,"none",0,25,300),
            new Offer(NewspaperLife.JOB,"Local journalist",TownLife.Place.LIBRARY,"investigation",2,20,1600),
            new Offer(FreightLife.JOB,"Freight driver",TownLife.Place.QUARRY,"none",0,FreightLife.WAGE,320)
    );

    static List<Offer> offers() {return OFFERS;}

    static void ensureEmployers(TownLife.Town town) {
        for(var offer:OFFERS)
            town.employerFunds.putIfAbsent(offer.id,offer.startingPayroll);
        town.employerFunds.putIfAbsent(PostalLife.ACCOUNT,360);
        town.employerFunds.putIfAbsent(SecurityLife.INSURER,0);
        town.employerFunds.putIfAbsent(EmploymentContractLife.PENSION_ACCOUNT,0);
        town.employerFunds.putIfAbsent(SocialProtectionLife.ACCOUNT,0);
        town.employerFunds.putIfAbsent("barber",900);
        town.employerFunds.putIfAbsent("pizzeria",1200);
        town.employerFunds.putIfAbsent("gelateria",1200);
        town.employerFunds.putIfAbsent("school",200);
        town.employerFunds.putIfAbsent("library",200);
        // Enterprise workers share real company funds with player industrial wages.
        town.employerFunds.putIfAbsent("city-maintenance",500);
        town.employerFunds.putIfAbsent(ServiceCompanyLife.ACCOUNT,0);
        town.employerFunds.putIfAbsent(EnergyLife.ACCOUNT,0);
        town.employerFunds.putIfAbsent(WaterNetworkLife.ACCOUNT,0);
        town.employerFunds.putIfAbsent("survey-office",0);
        town.employerFunds.putIfAbsent("guesthouse",0);
        town.employerFunds.putIfAbsent("farm-contractors",0);
        town.employerFunds.putIfAbsent(FuelLife.SUPPLIER,0);
        town.employerFunds.putIfAbsent(StallSupplyLife.CARRIER,0);
        town.employerFunds.putIfAbsent(BankLife.ACCOUNT,500);
        town.employerFunds.putIfAbsent(ConstructionLife.CONTRACTOR,0);
        town.employerFunds.putIfAbsent(EducationLife.SCHOOL_SERVICE,0);
        for(String account:List.of(MunicipalLife.CLINIC,MunicipalLife.CARE,MunicipalLife.MAINTENANCE))town.employerFunds.putIfAbsent(account,0);
    }

    private static Offer offer(String id) {
        if(id==null)throw new IllegalArgumentException("Choose an occupation from jobs.");
        String normalized=id.strip().toLowerCase(Locale.ROOT);
        for(var offer:OFFERS)
            if(offer.id.equals(normalized))return offer;
        throw new IllegalArgumentException("Unknown job: "+id+". Use jobs to see vacancies.");
    }

    static Map<String,Integer> payrolls(TownLife.Town town) {
        return new LinkedHashMap<>(town.employerFunds);
    }

    private static void requirePresent(PlayerLife.Player player,TownLife.Place where) {
        PlayerLife.requireAlive(player);
        if(player.travelRemaining>0)
            throw new IllegalArgumentException("Finish traveling before interacting.");
        if(player.place!=where)
            throw new IllegalArgumentException("Travel to "+where.label+" first.");
    }

    static int skill(PlayerLife.Player player,String subject) {
        return switch(subject) {
            case "engineering" -> player.engineeringSkill;
            case "investigation" -> player.investigationSkill;
            default -> 0;
        };
    }

    /** All offers are public. Applying requires visiting the actual workplace. */
    static String apply(TownLife.Town town,String job) {
        var p=town.player;
        var vacancy=offer(job);
        if(FreightLife.active(town)&&!FreightLife.automated(town))throw new IllegalArgumentException("Finish the freight delivery before changing jobs.");
        requirePresent(p,vacancy.workplace);
        if(vacancy.id.equals(p.jobId))
            throw new IllegalArgumentException("You already hold this job.");
        if(skill(p,vacancy.subject)<vacancy.requiredSkill)
            throw new IllegalArgumentException("Study "+vacancy.subject+" first: "+
                    vacancy.requiredSkill+" skill required.");
        if(town.employerFunds.getOrDefault(vacancy.id,0)<vacancy.wage)
            throw new IllegalArgumentException("Employer cannot fund another shift right now.");
        EmploymentContractLife.clearVisitor(town);
        p.jobId=vacancy.id;
        return "Hired as "+vacancy.title+". You may work a paid two-hour shift each day.";
    }

    static String apply(TownLife.Town town,String job,int day){
        String result=apply(town,job);
        EmploymentContractLife.signVisitor(town,day);
        return result+" Signed a 30-day contract with a 7-day probation.";
    }

    /** Paid instruction requires a physical school or library and available cash. */
    static String study(TownLife.Town town,String subject,int minute) {
        var p=town.player;
        String normalized=subject==null?"":subject.strip().toLowerCase(Locale.ROOT);
        TownLife.Place where=switch(normalized) {
            case "engineering" -> TownLife.Place.SCHOOL;
            case "investigation" -> TownLife.Place.LIBRARY;
            default -> throw new IllegalArgumentException(
                    "Choose study engineering or study investigation.");
        };
        requirePresent(p,where);
        if(minute<480 || minute+LESSON_MINUTES>1080)
            throw new IllegalArgumentException("Lessons take place 08:00–18:00.");
        if(p.money<LESSON_FEE)
            throw new IllegalArgumentException("Need "+LESSON_FEE+" EUR for a lesson.");
        p.money-=LESSON_FEE;
        String tuitionAccount=normalized.equals("engineering")?"school":"library";
        town.employerFunds.put(tuitionAccount,
                Math.addExact(town.employerFunds.get(tuitionAccount),LESSON_FEE));
        if(normalized.equals("engineering"))p.engineeringSkill++;
        else p.investigationSkill++;
        return "Completed "+normalized+" lesson (+1 skill, -"+
                LESSON_FEE+" EUR).";
    }

    /** A finite employer fund pays once per day, not the game's money printer. */
    static String work(TownLife.Town town,int day,int minute) {
        var p=town.player;
        PlayerLife.requireAlive(p);
        if(p.jobId==null)
            throw new IllegalArgumentException("Find and apply for a job first.");
        var vacancy=offer(p.jobId);
        if(FreightLife.JOB.equals(p.jobId))throw new IllegalArgumentException("Load and deliver a truck to earn your freight wage.");
        requirePresent(p,vacancy.workplace);
        if(minute<480 || minute+SHIFT_MINUTES>1080)
            throw new IllegalArgumentException("Two-hour shifts start between 08:00 and 16:00.");
        if(p.lastWorkDay==day)
            throw new IllegalArgumentException("Already completed a shift today.");
        if((vacancy.id.equals("technician") || vacancy.id.equals("engineer")) &&
                p.lastTechnicalDay!=day)
            throw new IllegalArgumentException(
                    "Inspect and complete an actual city work order before this technical shift.");
        if(NewspaperLife.JOB.equals(vacancy.id)&&!NewspaperLife.filed(town,day))throw new IllegalArgumentException("Interview a resident and file a public news dispatch before working.");
        int funds=town.employerFunds.getOrDefault(vacancy.id,0);
        if(funds<vacancy.wage)
            throw new IllegalArgumentException("Employer has insufficient payroll funds.");
        // Industrial roles must produce real physical goods during the shift.
        // A copied town is committed only after this and the rest of the action succeed.
        String industrialOutput=vacancy.id.equals("farm-worker")?FarmLife.playerShift(town):IndustryLife.playerShift(town,vacancy.id);
        // One conserved transfer: employer cash -> player cash.
        town.employerFunds.put(vacancy.id,funds-vacancy.wage);
        int net=EmploymentContractLife.recordPay(town,day,vacancy.wage);
        p.lastWorkDay=day;
        if(SecurityLife.JOB.equals(p.jobId))SecurityLife.visitorShift(town,day,minute);
        p.fatigue=Math.min(100,p.fatigue+8);
        return "Worked two hours as "+vacancy.title+
                ". Earned "+net+" EUR net from "+vacancy.wage+" EUR gross."+
                (industrialOutput.isEmpty()?"":" "+industrialOutput);
    }

    static String status(PlayerLife.Player p) {
        String job=p.jobId==null?"unemployed":offer(p.jobId).title;
        return "Cash "+p.money+" EUR | Job: "+job+" | Engineering "+
                p.engineeringSkill+" | Investigation "+p.investigationSkill;
    }
}
