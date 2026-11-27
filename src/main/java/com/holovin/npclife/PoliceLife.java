package com.holovin.npclife;

import java.util.Comparator;
import java.util.List;

/** Local public safety staff. Assignments use public case scenes, never offender truth. */
final class PoliceLife {
    static final String OFFICER="Police officer", DETECTIVE="Detective";
    static final String SEEDED="police:staffed-v1";
    private static final List<TownLife.Place> PATROL=List.of(TownLife.Place.CENTRO,
            TownLife.Place.PARK,TownLife.Place.MARKET,TownLife.Place.PORTICI,
            TownLife.Place.OFFICE,TownLife.Place.SCHOOL);
    private PoliceLife(){}
    static boolean officer(TownLife.Citizen c){return c!=null&&c.employed&&OFFICER.equals(c.occupation);}
    static boolean detective(TownLife.Citizen c){return c!=null&&c.employed&&DETECTIVE.equals(c.occupation);}
    static boolean staff(TownLife.Citizen c){return officer(c)||detective(c);}

    /** One-time staffing of ordinary adult residents; never replace identities or resurrect anyone. */
    static boolean establish(TownLife.Town t,WorldState clock,int target){
        if(target<24||t.industryStocks.getOrDefault(SEEDED,0)>0)return false;
        var candidates=t.people.values().stream().filter(c->c.alive&&c.generationIndex>0&&c.age>=23&&c.age<60
                &&!staff(c)&&!RebirthLife.linkedAdult(t,c)&&!BusinessLife.isEmployee(t,c)&&!ITStaffLife.isEmployee(t,c)
                &&!MarketStallLife.isEmployee(t,c)&&!LogisticsLife.isDriver(t,c)
                &&(!c.employed||List.of("Office clerk","Librarian","Barista","Teacher","Market vendor","Baker","Nurse").contains(c.occupation)))
                .sorted(Comparator.comparingInt((TownLife.Citizen c)->!c.employed?0:"Office clerk".equals(c.occupation)?1:2)
                        .thenComparingInt(c->c.generationIndex)).toList();
        int next=0;
        for(String role:List.of(OFFICER,DETECTIVE)){
            long current=t.people.values().stream().filter(c->c.alive&&c.employed&&role.equals(c.occupation)).count();
            int desired=role.equals(OFFICER)?2:1;
            for(int n=(int)current;n<desired&&next<candidates.size();n++){
                var c=candidates.get(next++);
                c.occupation=role;c.employed=true;c.unemploymentDays=0;
                c.workplace=TownLife.Place.OFFICE;c.lastHiredDay=clock.day;c.jobChanges++;
                EducationLife.ensureResident(t,c);
                EducationLife.learn(t,c,"academic",Math.max(0,50-EducationLife.get(t,c,"academic")));
                if(role.equals(DETECTIVE))EducationLife.set(t,c,"diploma-investigation",1);
                // Existing location and travel are preserved; the normal scheduler starts the new shift.
            }
        }
        t.industryStocks.put(SEEDED,1);
        return true;
    }

    static int shiftStart(TownLife.Citizen c){
        return officer(c)?(Math.floorMod(c.generationIndex,2)==0?420:720):480;
    }
    static boolean scheduled(TownLife.Citizen c,int day,int minute){
        int start=shiftStart(c);
        return staff(c)&&minute>=start&&minute<start+480&&(officer(c)||(day-1)%7<5);
    }
    static TownLife.Place assignment(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(officer(c)){
            int block=Math.max(0,minute-shiftStart(c))/75;
            return PATROL.get(Math.floorMod(block+c.generationIndex+day,PATROL.size()));
        }
        if(detective(c)){
            // Visit a public scene of a recent open case, then return to the office.
            if(minute>=570&&minute<840){
                var vehicleScene=NpcVehicleCrimeLife.scene(t);
                if(vehicleScene!=null&&NpcVehicleCrimeLife.get(t,"examined")==0)return vehicleScene;
                var propertyScene=NpcPropertyCrimeLife.scene(t);
                if(propertyScene!=null&&NpcPropertyCrimeLife.get(t,"examined")==0)return propertyScene;
                var latest=t.cases.values().stream().filter(f->!f.solved&&day-f.day<=7&&day>=f.day
                        &&PlayerLife.publicPlaces().contains(f.place))
                        .max(Comparator.comparingInt((InvestigationLife.CaseFile f)->f.day)
                                .thenComparingInt(f->f.minute));
                if(latest.isPresent())return latest.get().place;
            }
        }
        return TownLife.Place.OFFICE;
    }

    static String assistance(TownLife.Town t,TownLife.Citizen c,String intent,int day,int minute){
        if(!staff(c))return "You can look for a uniformed police officer around town.";
        if(!scheduled(c,day,minute))return "I'm off duty right now. Look for an officer on patrol.";
        if("@police-help".equals(intent))
            return "Tell me what happened and where. A report is not proof of a crime.";
        if("@police-work".equals(intent))
            return detective(c)?"I interview witnesses and check evidence. I don't assume someone is guilty.":"I'm on patrol, checking public places and helping residents.";
        if("@police-cases".equals(intent)){
            long open=t.cases.values().stream().filter(f->!f.solved&&f.place==c.place).count();
            return open>0?"There is an open investigation in this district. I cannot discuss private evidence."
                    :"I have no open investigation in this district to discuss.";
        }
        return null;
    }
}
