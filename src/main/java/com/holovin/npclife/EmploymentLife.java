package com.holovin.npclife;

import java.util.List;

/** Reproducible life disruptions for procedural residents; no AI calls or wall-clock RNG.
 *  Historical events are append-only and private medical costs stay off public witness logs.
 */
final class EmploymentLife {
    private EmploymentLife() { }

    enum Kind { JOB_LOSS, HIRED, SICK_LEAVE, RECOVERED, MEDICAL_EXPENSE }
    record Event(int day,int minute,String residentId,Kind kind,String detail) { }

    private static final String[] JOBS={"Teacher","Mechanic","Librarian","Barista",
            "Office clerk","Nurse","Baker","Market vendor"};
    private static final TownLife.Place[] WORK={TownLife.Place.SCHOOL,TownLife.Place.WORKSHOP,
            TownLife.Place.LIBRARY,TownLife.Place.CAFE,TownLife.Place.OFFICE,
            TownLife.Place.CLINIC,TownLife.Place.BAKERY,TownLife.Place.MARKET};

    static boolean sick(TownLife.Citizen person,int day) {
        return person.sickUntilDay>=day;
    }

    /** Mix a personal seed and simulated calendar day, independently for each decision. */
    private static int roll(TownLife.Citizen person,int day,long salt,int bound) {
        long x=person.personalSeed ^ (long)day*0x9e3779b97f4a7c15L ^ salt;
        x^=x>>>33;x*=0xff51afd7ed558ccdL;x^=x>>>33;
        x*=0xc4ceb9fe1a85ec53L;x^=x>>>33;
        return (int)Math.floorMod(x,bound);
    }

    static void tick(TownLife.Town town,WorldState clock,List<TownLife.Event> truth,
                     List<TownLife.Observation> observations) {
        // Decisions happen once at 06:30, so simulation speed does not affect them.
        if(clock.minute!=390)return;
        for(var p:town.people.values()) {
            if(!p.alive || p.generationIndex<=0)continue;
            if(p.sickUntilDay>0 && p.sickUntilDay<clock.day) {
                p.sickUntilDay=0;
                log(town,clock,p,Kind.RECOVERED,"Returned to usual health after a brief illness.");
            }
            // A company that cannot pay its employees must stop pretending
            // they still earn wages. Former industrial workers can search the
            // wider local job market instead of starving in an unpaid post.
            String industryAccount=IndustryLife.companyFor(p.occupation);
            boolean workshopEmployee=BusinessLife.isEmployee(town,p);
            boolean insolventWorkshop=workshopEmployee &&
                    town.business.cash<BusinessLife.HOURLY_WAGE;
            if(p.employed && ((industryAccount!=null &&
                    town.employerFunds.getOrDefault(industryAccount,0)<IndustryLife.HOURLY_WAGE) ||
                    insolventWorkshop)) {
                loseJob(town,clock,p);
            }
            if(!p.employed) {
                p.unemploymentDays++;
                if((industryAccount!=null || workshopEmployee) &&
                        p.unemploymentDays>=2) {
                    // Simplified emergency placement after a factory closure:
                    // the old industrial career and physical production end.
                    hire(town,clock,p,roll(p,clock.day,0xC2L,JOBS.length));
                } else {
                    // Other job seekers retain the slower independent search.
                    int threshold=p.unpaidBills>0 || p.money<80?225:145;
                    if(p.unemploymentDays>=5 && roll(p,clock.day,0xB1L,1000)<threshold)
                        hire(town,clock,p,roll(p,clock.day,0xC2L,JOBS.length));
                }
            } else if((p.lastHiredDay==0 || clock.day-p.lastHiredDay>=14) &&
                    roll(p,clock.day,0xA3L,1000)<17) {
                loseJob(town,clock,p);
            }
            if(p.sickUntilDay==0 && roll(p,clock.day,0xD4L,1000)<21) {
                int duration=2+roll(p,clock.day,0xE5L,2);
                int cost=75+roll(p,clock.day,0xF6L,7)*15;
                startIllness(town,clock,p,duration,cost,truth,observations);
            }
        }
    }

    static void loseJob(TownLife.Town town,WorldState clock,TownLife.Citizen p) {
        if(!p.alive || p.generationIndex<=0 || !p.employed)
            throw new IllegalArgumentException("Only employed procedural residents can lose a job.");
        p.employed=false;p.unemploymentDays=0;
        log(town,clock,p,Kind.JOB_LOSS,"Employment ended: "+p.occupation+".");
    }

    static void hire(TownLife.Town town,WorldState clock,TownLife.Citizen p,int roleIndex) {
        if(!p.alive || p.generationIndex<=0 || p.employed || roleIndex<0 || roleIndex>=JOBS.length)
            throw new IllegalArgumentException("Invalid job offer.");
        String previous=p.occupation;
        p.employed=true;p.unemploymentDays=0;p.lastHiredDay=clock.day;
        p.occupation=JOBS[roleIndex];p.workplace=WORK[roleIndex];
        p.jobChanges++;
        log(town,clock,p,Kind.HIRED,"Hired as "+p.occupation+" at "+p.workplace.label+
                " (previous occupation: "+previous+").");
    }

    static void startIllness(TownLife.Town town,WorldState clock,TownLife.Citizen p,
                             int days,int medicalCost,List<TownLife.Event> truth,
                             List<TownLife.Observation> observations) {
        if(!p.alive || p.generationIndex<=0 || days<1 || days>7 || medicalCost<1 ||
                sick(p,clock.day))throw new IllegalArgumentException("Invalid illness episode.");
        p.sickUntilDay=clock.day+days-1;
        p.fatigue=Math.min(100,p.fatigue+12);
        p.health=Math.max(1,p.health-2);
        log(town,clock,p,Kind.SICK_LEAVE,"Brief illness; resting at home through day "+p.sickUntilDay+".");
        EconomyLife.addBill(town,clock,p,medicalCost,"medical costs",truth,observations);
        log(town,clock,p,Kind.MEDICAL_EXPENSE,"Medical expenses EUR "+medicalCost+" (paid or in arrears).");
    }

    private static void log(TownLife.Town town,WorldState clock,TownLife.Citizen p,
                            Kind kind,String description) {
        town.pendingLifeEvents.add(new Event(clock.day,clock.minute,p.id,kind,description));
        // Do not publish confidential health or employment records to every bystander.
        // Detective-visible access to these records will require evidence acquisition later.
    }
}
