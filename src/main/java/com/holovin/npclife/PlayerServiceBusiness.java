package com.holovin.npclife;

import java.util.List;

/** A separate visitor-owned repair contractor paid for completed orders. */
final class PlayerServiceBusiness {
    private PlayerServiceBusiness(){}
    static final String ROLE="Service technician";
    static final int REGISTRATION=15, RENT=10, TOOLS=20, TRANSFER=20, SIGNING=5, MAX_STAFF=3, MIN_PRICE=14, MAX_PRICE=20;
    static int get(TownLife.Town t,String f){return ServiceCompanyLife.get(t,"player-business:"+f);}
    static void set(TownLife.Town t,String f,int n){ServiceCompanyLife.set(t,"player-business:"+f,n);}
    static void add(TownLife.Town t,String f,int n){set(t,f,Math.addExact(get(t,f),n));}
    static boolean yours(TownLife.Town t){return get(t,"owner")>0&&get(t,"owner")==t.player.incarnation&&t.player.alive&&!RebirthLife.active(t);}
    static boolean office(TownLife.Town t,int minute){
        return t.player.alive&&!RebirthLife.active(t)&&t.player.travelRemaining==0
            &&t.player.place==TownLife.Place.WORKSHOP&&minute>=480&&minute<1080;
    }
    static boolean operating(TownLife.Town t,int day){return yours(t)&&get(t,"rent-until")>=day&&get(t,"tools")==1;}
    static String staffKey(int i){return i==0?"employee":"employee-"+i;}
    static boolean employee(TownLife.Town t,TownLife.Citizen c){
        if(c==null||c.generationIndex<=0||!c.alive||!c.employed||!ROLE.equals(c.occupation))return false;
        for(int i=0;i<MAX_STAFF;i++)if(get(t,staffKey(i))==c.generationIndex)return true;
        return false;
    }
    static List<TownLife.Citizen> workers(TownLife.Town t){
        return t.people.values().stream().filter(c->employee(t,c)).toList();
    }
    static int freeStaff(TownLife.Town t){
        for(int i=0;i<MAX_STAFF;i++){
            var c=NpcVehicleCrimeLife.person(t,get(t,staffKey(i)));
            if(!employee(t,c))return i;
        }
        return -1;
    }
    static int price(TownLife.Town t){
        int n=get(t,"price");return n==0?HomeServiceLife.REPAIR_PRICE:Math.max(MIN_PRICE,Math.min(MAX_PRICE,n));
    }
    static boolean canRent(TownLife.Town t,int day){return get(t,"rent-until")<=day+2&&get(t,"cash")>=RENT;}
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
                if(!canRent(t,s.day))throw new IllegalArgumentException("Lease renewal unavailable.");
                spend(t,"city-maintenance",RENT);set(t,"rent-until",Math.max(s.day-1,get(t,"rent-until"))+7);return "Workshop leased for seven more days.";
            }
            case "tools" -> {
                if(get(t,"tools")==1)throw new IllegalArgumentException("Tools already purchased.");
                spend(t,"equipment-supplier",TOOLS);set(t,"tools",1);return "Purchased repair tools.";
            }
            default -> {
                if(action.startsWith("price:")){
                    int n;
                    try{n=Integer.parseInt(action.substring(6));}catch(NumberFormatException ex){throw new IllegalArgumentException("Invalid price.");}
                    if(n<MIN_PRICE||n>MAX_PRICE)throw new IllegalArgumentException("Price must be 14–20 EUR.");
                    set(t,"price",n);return "Repair price updated for future orders.";
                }
                int free=freeStaff(t);
                if(!action.startsWith("hire:")||!operating(t,s.day)||free<0||get(t,"cash")<SIGNING)
                    throw new IllegalArgumentException("Hiring unavailable.");
                var c=candidates(t,s.day).stream().filter(x->x.id.equals(action.substring(5))).findFirst()
                    .orElseThrow(()->new IllegalArgumentException("Choose an available qualified job seeker."));
                add(t,"cash",-SIGNING);c.money=Math.addExact(c.money,SIGNING);
                c.occupation=ROLE;c.workplace=TownLife.Place.WORKSHOP;c.employed=true;c.unemploymentDays=0;
                c.lastHiredDay=s.day;c.jobChanges++;set(t,staffKey(free),c.generationIndex);
                EmploymentContractLife.signCitizen(t,c,s.day);
                return "Hired "+c.name+" on a per-job contract.";
            }
        }
    }
    static boolean available(TownLife.Town t){return t.technicalMaterials.getOrDefault("spare-parts",0)>0;}
    static void complete(TownLife.Town t,TownLife.Citizen c,int gross){
        if(gross<MIN_PRICE||gross>MAX_PRICE)throw new IllegalArgumentException("Invalid quoted fee.");
        int tax=1;
        t.technicalMaterials.merge("spare-parts",-1,Math::addExact);
        t.employerFunds.merge("city-maintenance",HomeServiceLife.PART_PRICE+tax,Math::addExact);
        c.money=Math.addExact(c.money,ServiceCompanyLife.LABOR);
        add(t,"cash",gross-tax-HomeServiceLife.PART_PRICE-ServiceCompanyLife.LABOR);
        add(t,"revenue",gross);add(t,"tax",tax);add(t,"jobs",1);
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!operating(t,s.day))for(var c:workers(t)){
            EmploymentLife.loseJob(t,s,c);
        }
        for(int i=0;i<MAX_STAFF;i++)if(!employee(t,NpcVehicleCrimeLife.person(t,get(t,staffKey(i)))))set(t,staffKey(i),0);
    }
    record Candidate(String id,String name){}
    record View(boolean registered,boolean yours,int cash,int rentUntil,boolean tools,String employee,int jobs,int revenue,int tax,
                boolean canRegister,boolean canInvest,boolean canWithdraw,boolean canRent,boolean canTools,List<Candidate> candidates,int price,int staffCount,List<String> employees){}
    static View view(TownLife.Town t,WorldState s){
        boolean own=yours(t),at=office(t,s.minute);var staff=workers(t);
        return new View(get(t,"owner")>0,own,own?get(t,"cash"):0,own?get(t,"rent-until"):0,own&&get(t,"tools")==1,
            own?String.join(", ",staff.stream().map(c->c.name).toList()):"",own?get(t,"jobs"):0,own?get(t,"revenue"):0,own?get(t,"tax"):0,
            at&&get(t,"owner")==0&&t.player.money>=REGISTRATION,own&&at&&t.player.money>=TRANSFER,
            own&&at&&get(t,"cash")>=TRANSFER,own&&at&&canRent(t,s.day),
            own&&at&&get(t,"tools")==0&&get(t,"cash")>=TOOLS,
            own&&at&&operating(t,s.day)&&freeStaff(t)>=0&&get(t,"cash")>=SIGNING?candidates(t,s.day).stream().map(x->new Candidate(x.id,x.name)).toList():List.of(),own?price(t):0,own?staff.size():0,own?staff.stream().map(x->x.name).toList():List.of());
    }
}
