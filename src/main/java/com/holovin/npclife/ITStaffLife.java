package com.holovin.npclife;

import java.util.List;

/** A resident is recruited for paid support assignments, never a fabricated NPC or free payroll. */
final class ITStaffLife {
    private ITStaffLife(){}
    static final String OCCUPATION="IT support assistant";
    static final int BONUS=10,WAGE=12;
    record Candidate(String id,String name,int age,int technical,boolean diploma){}
    record Employee(String id,String name,int technical,String activity,boolean sick){}
    record Staff(Employee employee,List<Candidate> candidates,boolean canDismiss,int delegated,List<String> delegatable,boolean managed,int managementProfit,int managementFailures){}
    static int get(TownLife.Town t,TownLife.Citizen c,String k){return ITBusinessLife.get(t,c,k);}
    static void set(TownLife.Town t,TownLife.Citizen c,String k,int v){ITBusinessLife.set(t,c,k,v);}
    static TownLife.Citizen employee(TownLife.Town t,TownLife.Citizen owner){
        var w=DemographyLife.byIndex(t,get(t,owner,"employee"));
        return w!=null&&w.alive&&w.employed&&OCCUPATION.equals(w.occupation)
            &&get(t,w,"employer")==owner.generationIndex?w:null;
    }
    static boolean isEmployee(TownLife.Town t,TownLife.Citizen w){
        if(!w.alive||!w.employed||!OCCUPATION.equals(w.occupation))return false;
        var owner=DemographyLife.byIndex(t,get(t,w,"employer"));
        return owner!=null&&owner.alive&&ITBusinessLife.registered(t,owner)&&get(t,owner,"employee")==w.generationIndex;
    }
    static boolean scheduled(TownLife.Town t,TownLife.Citizen w,int day,int minute){
        if(!isEmployee(t,w))return false;
        var owner=DemographyLife.byIndex(t,get(t,w,"employer"));
        return get(t,owner,"rent-until")>=day&&(day-1)%7<5&&minute>=480&&minute<1080;
    }
    static boolean eligible(TownLife.Town t,TownLife.Citizen owner,TownLife.Citizen w,int day){
        return w!=owner&&w.generationIndex>0&&w.alive&&DemographyLife.workingAge(w)&&!w.employed
            &&!RelationshipBoundaryLife.controlled(t,w)&&!EmploymentLife.sick(w,day)&&EducationLife.get(t,w,"technical")>=20;
    }
    static Staff report(TownLife.Town t,TownLife.Citizen owner,int day,int minute,List<ITBusinessLife.Offer> offers){
        boolean own=ITBusinessLife.registered(t,owner),ready=own&&ITBusinessLife.ready(t,owner,day,minute,5);
        var w=employee(t,owner);int cash=get(t,owner,"cash");
        var cv=ready&&w==null&&cash>=BONUS+WAGE&&get(t,owner,"rent-until")>=day?
            t.people.values().stream().filter(p->eligible(t,owner,p,day)).limit(12)
            .map(p->new Candidate(p.id,p.name,p.age,EducationLife.get(t,p,"technical"),EducationLife.get(t,p,"diploma-programming")>0)).toList():List.<Candidate>of();
        boolean canDelegate=ready&&w!=null&&cash>=WAGE&&scheduled(t,w,day,minute)&&minute+60<=1080
            &&!EmploymentLife.sick(w,day)&&w.presentAt(TownLife.Place.OFFICE)&&"WORKING".equals(w.activity)
            &&get(t,owner,"last-assignment")<day;
        return new Staff(w==null?null:new Employee(w.id,w.name,EducationLife.get(t,w,"technical"),w.activity,EmploymentLife.sick(w,day)),
            cv,ready&&w!=null,get(t,owner,"delegated"),canDelegate?offers.stream().filter(ITBusinessLife.Offer::available).map(ITBusinessLife.Offer::id).toList():List.of(),get(t,owner,"managed")>0,get(t,owner,"management-profit")-get(t,owner,"management-loss"),get(t,owner,"management-failures"));
    }
    static void release(TownLife.Town t,TownLife.Citizen owner){
        var w=employee(t,owner);
        if(w!=null){w.employed=false;w.unemploymentDays=0;w.occupation="Job seeker";set(t,w,"employer",0);}
        set(t,owner,"employee",0);set(t,owner,"managed",0);
    }
    static void tick(TownLife.Town t,int day,int minute){
        if(minute==390){
            for(var owner:t.people.values())if(get(t,owner,"employee")>0){
                if(!owner.alive||!ITBusinessLife.registered(t,owner)||employee(t,owner)==null){release(t,owner);continue;}
                if(get(t,owner,"rent-until")<day&&get(t,owner,"managed")>0&&get(t,owner,"cash")>=ITBusinessLife.RENT+WAGE){
                    set(t,owner,"cash",get(t,owner,"cash")-ITBusinessLife.RENT);
                    ITBusinessLife.credit(t,MunicipalLife.CITY,ITBusinessLife.RENT);
                    set(t,owner,"rent-until",day+6);
                }
                // Managed firms pause when the lease cannot be renewed; ownership is retained.
                if(get(t,owner,"rent-until")<day&&get(t,owner,"managed")==0)release(t,owner);
            }
        }
        if(minute==900)for(var owner:t.people.values())managedShift(t,owner,day,minute);
    }
    /** One real paid assignment per day; no owner travel or personal spending. */
    static void managedShift(TownLife.Town t,TownLife.Citizen owner,int day,int minute){
        if(!owner.alive||!ITBusinessLife.registered(t,owner)||get(t,owner,"managed")==0||
                get(t,owner,"last-assignment")>=day||get(t,owner,"cash")<WAGE)return;
        var w=employee(t,owner);
        if(w==null||!scheduled(t,w,day,minute)||EmploymentLife.sick(w,day)||
                !w.presentAt(TownLife.Place.OFFICE)||!"WORKING".equals(w.activity)||w.fatigue>=85)return;
        String id=null,client=null;int gross=0,tax=0;
        if(SoftwareLife.deployed(t,"retail-api")&&!DevOpsLife.unavailable(t)&&ITBusinessLife.eligibleWeek(t,owner,"retail",day)
                &&t.employerFunds.getOrDefault("shop-assistant",0)>=40){id="retail";client="shop-assistant";gross=40;tax=8;}
        else if(SoftwareLife.deployed(t,"water-control")&&ITBusinessLife.eligibleWeek(t,owner,"utility",day)
                &&MunicipalLife.balance(t,MunicipalLife.CITY)>=50){id="utility";client=MunicipalLife.CITY;gross=50;tax=10;}
        if(id==null)return;
        set(t,owner,"last-assignment",day);
        set(t,owner,"cash",get(t,owner,"cash")-WAGE);w.money=Math.addExact(w.money,WAGE);
        w.fatigue=Math.min(100,w.fatigue+4);
        int skill=EducationLife.get(t,w,"technical");
        // Modest deterministic quality risk: wages remain due even for rejected work.
        int quality=Math.floorMod(w.personalSeed^((long)day*0x9E3779B97F4A7C15L),100);
        if(quality>=Math.min(98,65+skill/3)){
            set(t,owner,"management-failures",Math.addExact(get(t,owner,"management-failures"),1));
            set(t,owner,"management-loss",Math.addExact(get(t,owner,"management-loss"),WAGE));
            return;
        }
        t.employerFunds.merge(client,-gross,Integer::sum);ITBusinessLife.credit(t,MunicipalLife.CITY,tax);
        set(t,owner,"cash",Math.addExact(get(t,owner,"cash"),gross-tax));
        set(t,owner,"tax-paid",Math.addExact(get(t,owner,"tax-paid"),tax));
        set(t,owner,"last-"+id,day);set(t,owner,"contracts",Math.addExact(get(t,owner,"contracts"),1));
        set(t,owner,"delegated",Math.addExact(get(t,owner,"delegated"),1));
        set(t,owner,"management-profit",Math.addExact(get(t,owner,"management-profit"),gross-tax-WAGE));
        EducationLife.learn(t,w,"technical",1);
    }
    static RebirthLife.HomeAction action(TownLife.Town t,TownLife.Citizen owner,int day,int minute,String action,ITBusinessLife.Status status){
        var staff=status.staff();
        if(action.equals("manage-on")||action.equals("manage-off")){
            if(!staff.canDismiss())throw new IllegalArgumentException("An available support employee and office access are required.");
            set(t,owner,"managed",action.equals("manage-on")?1:0);
            return new RebirthLife.HomeAction(5,action.equals("manage-on")?"Authorized ongoing management.":"Returned to manual management.");
        }
        if(action.startsWith("hire:")){
            String id=action.substring(5);
            if(staff.candidates().stream().noneMatch(p->p.id().equals(id)))throw new IllegalArgumentException("Applicant unavailable or insufficient company funds.");
            var w=t.get(id);int wallet=Math.addExact(w.money,BONUS);
            set(t,owner,"cash",status.cash()-BONUS);w.money=wallet;w.employed=true;w.unemploymentDays=0;w.lastHiredDay=day;
            w.occupation=OCCUPATION;w.workplace=TownLife.Place.OFFICE;w.jobChanges++;
            set(t,owner,"employee",w.generationIndex);set(t,w,"employer",owner.generationIndex);
            RebirthLife.travel(t,TownLife.Place.OFFICE,day);
            return new RebirthLife.HomeAction(5,"Hired "+w.name+" for paid support assignments.");
        }
        if(action.equals("dismiss")){
            if(!staff.canDismiss())throw new IllegalArgumentException("No available support contract.");
            release(t,owner);RebirthLife.travel(t,TownLife.Place.OFFICE,day);
            return new RebirthLife.HomeAction(5,"Ended support contract.");
        }
        String id=action.substring("delegate:".length());
        if(!staff.delegatable().contains(id))throw new IllegalArgumentException("Check employee attendance, illness, lease, cash and available contracts.");
        var w=employee(t,owner);int wallet=Math.addExact(w.money,WAGE);
        int answer=id.equals("retail")?ITBusinessLife.retailTotal(t):ITBusinessLife.utilityTotal(t);
        var result=ITBusinessLife.action(t,day,minute,"deliver:"+id+":"+answer);
        set(t,owner,"cash",get(t,owner,"cash")-WAGE);w.money=wallet;
        set(t,owner,"last-assignment",day);set(t,owner,"delegated",Math.addExact(get(t,owner,"delegated"),1));
        EducationLife.learn(t,w,"technical",1);w.fatigue=Math.min(100,w.fatigue+4);
        return new RebirthLife.HomeAction(result.minutes(),w.name+" delivered the report; EUR "+WAGE+" paid from company cash. "+result.message());
    }
}
