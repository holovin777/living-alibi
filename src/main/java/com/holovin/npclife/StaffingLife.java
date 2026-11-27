package com.holovin.npclife;
import java.util.*;
import static com.holovin.npclife.TownLife.Place.*;

/** Staffing targets, not guaranteed attendance. New immigrants arrive with professional training. */
final class StaffingLife {
    private StaffingLife(){}
    static final int DEFAULT_POPULATION=240, MIN_POPULATION=96;
    static final String ENABLED="staffing:enabled";
    record Role(String name,TownLife.Place workplace,int target,String fund,String diploma,int skill){}
    static Role job(String name,TownLife.Place place,int target){return new Role(name,place,target,null,null,20);}
    static final List<Role> ROLES=List.of(
        job("Farm machinery technician",FARM,1),job("Irrigation operator",FARM,1),job("Veterinarian",FARM,1),
        job("Grain farmer",FARM,2),job("Dairy farmer",FARM,2),job("Mill operator",FACTORY,1),job("Dairy processor",FACTORY,1),job("Food processing worker",FACTORY,2),
        job("Glass worker",FACTORY,2),job("Chemical plant operator",FACTORY,2),job("Plastics worker",FACTORY,2),job("Packaging worker",FACTORY,2),job("Cement worker",FACTORY,2),job("Building materials worker",FACTORY,2),
        job("Waste collector",LOGISTICS_DEPOT,2),job("Recycling operator",FACTORY,2),
        job("Gas network operator",FUEL_STATION,1),
        job("Water treatment operator",RIVER,1),job("Wastewater operator",FACTORY,1),
        job("Centre grid technician",OFFICE,1),job("North grid technician",NORTH_RESIDENCES,1),job("South grid technician",SOUTH_RESIDENCES,1),job("Industrial grid technician",FACTORY,1),
        job("Solar plant operator",FARM,1),job("Hydropower operator",RIVER,1),job("Thermal plant operator",FACTORY,1),
        job("Forestry worker",WOODS,2),job("Sawmill worker",FACTORY,2),job("Paper mill worker",FACTORY,1),
        job("Textile worker",FACTORY,1),job("Printer",FACTORY,1),job("Garment worker",FACTORY,1),
        job("Carpenter",WORKSHOP,2),job("Electronics assembler",FACTORY,2),job("Device assembler",FACTORY,1),job("Watch assembler",FACTORY,1),
        new Role("Doctor",CLINIC,3,null,"medicine",65),job("Nurse",CLINIC,5),job("Pharmacist",CLINIC,2),
        job("Teacher",SCHOOL,6),job("Police officer",OFFICE,5),new Role("Detective",OFFICE,2,null,"investigation",45),
        job("Mechanic",WORKSHOP,5),job("Electrician",WORKSHOP,3),job("Plumber",WORKSHOP,3),job("Heating technician",WORKSHOP,3),
        new Role("Engineer",WORKSHOP,3,null,"engineering",45),new Role("Programmer",OFFICE,3,null,"programming",45),
        job("Office clerk",OFFICE,5),job("Bank clerk",OFFICE,3),new Role("Postal worker",OFFICE,2,PostalLife.ACCOUNT,null,20),
        new Role("Journalist",LIBRARY,2,NewspaperLife.JOB,null,35),job("Librarian",LIBRARY,3),job("Barber",CENTRO,2),
        new Role("Pizzaiolo",CENTRO,2,"pizzeria",null,20),new Role("Gelataio",CENTRO,2,"gelateria",null,20),
        job("Market vendor",MARKET,5),new Role("Security guard",MARKET,2,SecurityLife.JOB,null,20),job("Book seller",MARKET,2),
        job("Clothing seller",PORTICI,2),job("Electronics seller",PORTICI,2),job("Furniture seller",WORKSHOP,2),
        job("Barista",CAFE,4),job("Baker",BAKERY,4),new Role("Miner",QUARRY,3,"miner",null,20),
        new Role("Factory machinist",FACTORY,3,"machinist",null,20),new Role("Transport driver",QUARRY,2,"miner",null,20),
        job("Farm worker",FARM,4),job("Fuel station attendant",FUEL_STATION,2),job("Warehouse worker",LOGISTICS_DEPOT,3),job("Station attendant",STAZIONE,2));
    static boolean enabled(TownLife.Town t){return t.industryStocks.getOrDefault(ENABLED,0)>0;}
    static Role role(String name){return ROLES.stream().filter(r->r.name().equals(name)).findFirst().orElse(null);}
    static boolean staffed(TownLife.Citizen c,Role r){return c.alive&&c.employed&&c.age>=18&&c.age<67&&r.name().equals(c.occupation)&&r.workplace()==c.workplace;}
    static long count(TownLife.Town t,Role r){return t.people.values().stream().filter(c->staffed(c,r)).count();}
    static boolean funded(TownLife.Town t,Role r){return r.fund()==null||t.employerFunds.getOrDefault(r.fund(),0)>=(r.name().equals("Journalist")?NewspaperLife.REPORTER_PAY:r.name().equals("Postal worker")?PostalLife.WAGE:r.name().equals("Security guard")?5:IndustryLife.HOURLY_WAGE);}
    static boolean eligible(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c.alive&&c.generationIndex>0&&c.age>=25&&c.age<65&&!RebirthLife.linkedAdult(t,c)
            &&!EmploymentLife.sick(c,s.day)&&!NpcJusticeLife.confined(t,c,s)
            &&!PlayerServiceBusiness.employee(t,c)&&!BusinessLife.isEmployee(t,c)&&!ITStaffLife.isEmployee(t,c)&&!MarketStallLife.isEmployee(t,c)&&!LogisticsLife.isDriver(t,c);
    }
    static void assign(TownLife.Town t,WorldState s,TownLife.Citizen c,Role r,boolean immigrant){
        c.occupation=r.name();c.workplace=r.workplace();c.employed=true;c.unemploymentDays=0;c.lastHiredDay=s.day;c.jobChanges++;
        if(immigrant){
            EducationLife.ensureResident(t,c);String subject=EducationLife.subject(r.name());
            EducationLife.learn(t,c,subject,Math.max(0,r.skill()-EducationLife.get(t,c,subject)));
            if(r.diploma()!=null)EducationLife.set(t,c,"diploma-"+r.diploma(),1);
            if(r.name().equals("Doctor"))EducationLife.learn(t,c,"social",Math.max(0,35-EducationLife.get(t,c,"social")));
        }
        EmploymentContractLife.signCitizen(t,c,s.day);SocialProtectionLife.hired(t,c);
    }
    static boolean establish(TownLife.Town t,WorldState s,int target,int firstNew){
        if(target<MIN_POPULATION)return false;
        boolean changed=!enabled(t);t.industryStocks.put(ENABLED,1);
        if(firstNew<=0)return changed;
        var recruits=t.people.values().stream().filter(c->c.generationIndex>=firstNew&&eligible(t,c,s)).toList();
        Set<Integer> used=new HashSet<>();
        for(var r:ROLES){
            long missing=r.target()-count(t,r);if(!funded(t,r))continue;
            for(int i=0;i<missing;i++){
                var c=recruits.stream().filter(x->!used.contains(x.generationIndex)&&(!x.employed||role(x.occupation)==null||count(t,role(x.occupation))>role(x.occupation).target())).findFirst().orElse(null);
                if(c==null)break;
                used.add(c.generationIndex);assign(t,s,c,r,true);changed=true;
            }
        }
        return changed;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!enabled(t)||s.minute!=390)return;
        int budget=4;
        for(var r:ROLES){
            if(budget==0)return;
            if(!funded(t,r)||count(t,r)>=r.target())continue;
            var c=t.people.values().stream().filter(x->!x.employed&&eligible(t,x,s)
                &&EducationLife.get(t,x,EducationLife.subject(r.name()))>=r.skill()
                &&(r.diploma()==null||EducationLife.get(t,x,"diploma-"+r.diploma())>0)
                &&(!r.name().equals("Doctor")||EducationLife.get(t,x,"social")>=35)).findFirst().orElse(null);
            if(c!=null){assign(t,s,c,r,false);budget--;}
        }
    }
    record Coverage(String role,String workplace,int target,long employed,long onDuty){}
    record View(boolean active,long population,List<Coverage> roles){}
    static View view(TownLife.Town t){
        return new View(enabled(t),DemographyLife.living(t),enabled(t)?ROLES.stream().map(r->new Coverage(r.name(),r.workplace().name(),r.target(),count(t,r),
            t.people.values().stream().filter(c->staffed(c,r)&&c.presentAt(r.workplace())&&"WORKING".equals(c.activity)).count())).toList():List.of());
    }
}
