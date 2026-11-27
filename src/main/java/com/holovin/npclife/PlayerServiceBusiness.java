package com.holovin.npclife;

import java.util.List;

/** A separate visitor-owned repair contractor paid for completed orders. */
final class PlayerServiceBusiness {
    private PlayerServiceBusiness(){}
    static final String ROLE="Service technician";
    static final int REGISTRATION=15, RENT=10, TOOLS=20, TRANSFER=20, SIGNING=5;
    static int get(TownLife.Town t,String f){return ServiceCompanyLife.get(t,"player-business:"+f);}
    static void set(TownLife.Town t,String f,int n){ServiceCompanyLife.set(t,"player-business:"+f,n);}
    static void add(TownLife.Town t,String f,int n){set(t,f,Math.addExact(get(t,f),n));}
    static boolean yours(TownLife.Town t){return get(t,"owner")>0&&get(t,"owner")==t.player.incarnation&&t.player.alive&&!RebirthLife.active(t);}
    static boolean office(TownLife.Town t,int minute){
        return t.player.alive&&!RebirthLife.active(t)&&t.player.travelRemaining==0
            &&t.player.place==TownLife.Place.WORKSHOP&&minute>=480&&minute<1080;
    }
    static boolean operating(TownLife.Town t,int day){return yours(t)&&get(t,"rent-until")>=day&&get(t,"tools")==1;}
    static boolean employee(TownLife.Town t,TownLife.Citizen c){
        return c!=null&&c.generationIndex>0&&get(t,"employee")==c.generationIndex&&c.alive&&c.employed&&ROLE.equals(c.occupation);
    }
    static TownLife.Citizen worker(TownLife.Town t){
        var c=NpcVehicleCrimeLife.person(t,get(t,"employee"));return employee(t,c)?c:null;
    }
    static List<TownLife.Citizen> candidates(TownLife.Town t,int day){
        return t.people.values().stream().filter(c->c.alive&&c.generationIndex>0&&DemographyLife.workingAge(c)
            &&!c.employed&&!RebirthLife.linkedAdult(t,c)&&c.presentAt(TownLife.Place.WORKSHOP)
            &&!EmploymentLife.sick(c,day)&&EducationLife.get(t,c,"technical")>=35).toList();
    }
    static void spend(TownLife.Town t,String account,int n){
        if(get(t,"cash")<n)throw new IllegalArgumentException("Insufficient company funds.");
        add(t,"cash",-n);t.employerFunds.merge(account,n,Math::addExact);
    }
    static String act(TownLife.Town t,WorldState s,String action){
        if(!office(t,s.minute))throw new IllegalArgumentException("Visit the workshop office during business hours.");
        if(action.equals("register")){
            if(get(t,"owner")>0||t.player.money<REGISTRATION)throw new IllegalArgumentException("Registration unavailable.");
            t.player.money-=REGISTRATION;t.employerFunds.merge("city-maintenance",REGISTRATION,Math::addExact);
            set(t,"owner",t.player.incarnation);return "Repair company registered.";
        }
        if(!yours(t))throw new IllegalArgumentException("You do not own this company.");
        switch(action){
            case "invest" -> {
                if(t.player.money<TRANSFER)throw new IllegalArgumentException("Insufficient personal funds.");
                t.player.money-=TRANSFER;add(t,"cash",TRANSFER);return "Invested EUR 20.";
            }
            case "withdraw" -> {
                if(get(t,"cash")<TRANSFER)throw new IllegalArgumentException("Insufficient company funds.");
                add(t,"cash",-TRANSFER);t.player.money=Math.addExact(t.player.money,TRANSFER);return "Withdrew EUR 20.";
            }
            case "rent" -> {
                if(get(t,"rent-until")>=s.day)throw new IllegalArgumentException("Lease still active.");
                spend(t,"city-maintenance",RENT);set(t,"rent-until",s.day+6);return "Workshop rented for seven days.";
            }
            case "tools" -> {
                if(get(t,"tools")==1)throw new IllegalArgumentException("Tools already purchased.");
                spend(t,"equipment-supplier",TOOLS);set(t,"tools",1);return "Purchased repair tools.";
            }
            default -> {
                if(!action.startsWith("hire:")||!operating(t,s.day)||worker(t)!=null||get(t,"cash")<SIGNING)
                    throw new IllegalArgumentException("Hiring unavailable.");
                var c=candidates(t,s.day).stream().filter(x->x.id.equals(action.substring(5))).findFirst()
                    .orElseThrow(()->new IllegalArgumentException("Choose an available qualified job seeker."));
                add(t,"cash",-SIGNING);c.money=Math.addExact(c.money,SIGNING);
                c.occupation=ROLE;c.workplace=TownLife.Place.WORKSHOP;c.employed=true;c.unemploymentDays=0;
                c.lastHiredDay=s.day;c.jobChanges++;set(t,"employee",c.generationIndex);
                EmploymentContractLife.signCitizen(t,c,s.day);
                return "Hired "+c.name+" on a per-job contract.";
            }
        }
    }
    static boolean available(TownLife.Town t){return t.technicalMaterials.getOrDefault("spare-parts",0)>0;}
    static void complete(TownLife.Town t,TownLife.Citizen c){
        int gross=HomeServiceLife.REPAIR_PRICE,tax=1;
        t.technicalMaterials.merge("spare-parts",-1,Math::addExact);
        t.employerFunds.merge("city-maintenance",HomeServiceLife.PART_PRICE+tax,Math::addExact);
        c.money=Math.addExact(c.money,ServiceCompanyLife.LABOR);
        add(t,"cash",gross-tax-HomeServiceLife.PART_PRICE-ServiceCompanyLife.LABOR);
        add(t,"revenue",gross);add(t,"tax",tax);add(t,"jobs",1);
    }
    static void tick(TownLife.Town t,WorldState s){
        var c=worker(t);
        if(c!=null&&!operating(t,s.day)){
            EmploymentLife.loseJob(t,s,c);set(t,"employee",0);
        }
    }
    record Candidate(String id,String name){}
    record View(boolean registered,boolean yours,int cash,int rentUntil,boolean tools,String employee,int jobs,int revenue,int tax,
                boolean canRegister,boolean canInvest,boolean canWithdraw,boolean canRent,boolean canTools,List<Candidate> candidates){}
    static View view(TownLife.Town t,WorldState s){
        boolean own=yours(t),at=office(t,s.minute);var c=worker(t);
        return new View(get(t,"owner")>0,own,own?get(t,"cash"):0,own?get(t,"rent-until"):0,own&&get(t,"tools")==1,
            own&&c!=null?c.name:"",own?get(t,"jobs"):0,own?get(t,"revenue"):0,own?get(t,"tax"):0,
            at&&get(t,"owner")==0&&t.player.money>=REGISTRATION,own&&at&&t.player.money>=TRANSFER,
            own&&at&&get(t,"cash")>=TRANSFER,own&&at&&get(t,"rent-until")<s.day&&get(t,"cash")>=RENT,
            own&&at&&get(t,"tools")==0&&get(t,"cash")>=TOOLS,
            own&&at&&operating(t,s.day)&&c==null&&get(t,"cash")>=SIGNING?candidates(t,s.day).stream().map(x->new Candidate(x.id,x.name)).toList():List.of());
    }
}
