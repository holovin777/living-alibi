package com.holovin.npclife;

import java.util.List;

/** Owner-operated consultancy with personal and company balances kept separate. */
final class ITBusinessLife {
    private ITBusinessLife(){}
    static final int REGISTRATION=50,TRANSFER=20,RENT=20;
    record Offer(String id,String title,int gross,int tax,String prompt,boolean available){}
    record Status(boolean registered,int cash,int rentUntil,int contracts,int taxPaid,
                  boolean canRegister,boolean canInvest,boolean canWithdraw,boolean canRent,String guide,List<Offer> offers,ITStaffLife.Staff staff){}
    static int get(TownLife.Town t,TownLife.Citizen c,String key){return EducationLife.get(t,c,"it-"+key);}
    static void set(TownLife.Town t,TownLife.Citizen c,String key,int value){EducationLife.set(t,c,"it-"+key,value);}
    static boolean registered(TownLife.Town t,TownLife.Citizen c){return get(t,c,"registered")>0;}
    static boolean ready(TownLife.Town t,TownLife.Citizen c,int day,int minute,int duration){
        return DeveloperCareerLife.ready(t,c,day,minute,duration);
    }
    static int retailTotal(TownLife.Town t){return t.retailStock.getOrDefault("bread",0)+t.retailStock.getOrDefault("water",0);}
    static int utilityTotal(TownLife.Town t){return UtilityLife.status(t).cleanWater();}
    static Status report(TownLife.Town t,int day,int minute){
        var c=RebirthLife.child(t);
        if(!SoftwareLife.linkedProgrammer(t,c))return null;
        boolean own=registered(t,c),at=ready(t,c,day,minute,5);
        int cash=get(t,c,"cash"),rent=get(t,c,"rent-until");
        boolean deliver=own&&rent>=day&&ready(t,c,day,minute,60);
        int bread=t.retailStock.getOrDefault("bread",0),water=t.retailStock.getOrDefault("water",0);
        var offers=own?List.of(
            new Offer("retail","Bottega Alimentare support",40,8,
                "Catalog reconciliation: bread="+bread+", water="+water+". Submit total stock of these two items.",
                deliver&&SoftwareLife.deployed(t,"retail-api")&&!DevOpsLife.unavailable(t)
                    &&eligibleWeek(t,c,"retail",day)&&t.employerFunds.getOrDefault("shop-assistant",0)>=40),
            new Offer("utility","Municipal water monitoring",50,10,
                "Monitoring snapshot: clean water="+utilityTotal(t)+" units. Confirm the current stored water total.",
                deliver&&SoftwareLife.deployed(t,"water-control")&&eligibleWeek(t,c,"utility",day)
                    &&t.employerFunds.getOrDefault("city-maintenance",0)>=50)):List.<Offer>of();
        return new Status(own,cash,rent,get(t,c,"contracts"),get(t,c,"tax-paid"),
            !own&&at&&DeveloperCareerLife.rank(t,c)>=2&&c.money>=REGISTRATION,
            own&&at&&c.money>=TRANSFER,own&&at&&cash>=TRANSFER,own&&at&&cash>=RENT&&rent<day,
            "Senior Developer can register for EUR 50. Keep business funds separate. Rent a shared office for seven days, then deliver each available client report at most once per seven days. A fictional 20% revenue tax is paid to the town; this is not real tax law.",offers,ITStaffLife.report(t,c,day,minute,offers));
    }
    static boolean eligibleWeek(TownLife.Town t,TownLife.Citizen c,String id,int day){
        int last=get(t,c,"last-"+id);return last==0||day-last>=7;
    }
    static void credit(TownLife.Town t,String account,int amount){
        t.employerFunds.merge(account,amount,Math::addExact);
    }
    static RebirthLife.HomeAction action(TownLife.Town t,int day,int minute,String action){
        var c=RebirthLife.requireAdult(t);var status=report(t,day,minute);
        if(status==null)throw new IllegalArgumentException("Join the programming team first.");
        if(action.startsWith("hire:")||action.equals("dismiss")||action.startsWith("delegate:"))return ITStaffLife.action(t,c,day,minute,action,status);
        int duration=5;String message;
        switch(action){
            case "register" -> {
                if(!status.canRegister())throw new IllegalArgumentException("Registration requires Senior Developer, EUR 50 and office hours.");
                c.money-=REGISTRATION;credit(t,MunicipalLife.CITY,REGISTRATION);
                set(t,c,"registered",1);message="Registered your IT consultancy.";
            }
            case "invest" -> {
                if(!status.canInvest())throw new IllegalArgumentException("Insufficient personal cash or company access.");
                set(t,c,"cash",Math.addExact(status.cash(),TRANSFER));c.money-=TRANSFER;message="Invested EUR 20 in company.";
            }
            case "withdraw" -> {
                if(!status.canWithdraw())throw new IllegalArgumentException("Insufficient company cash or company access.");
                c.money=Math.addExact(c.money,TRANSFER);set(t,c,"cash",status.cash()-TRANSFER);message="Withdrew EUR 20 to your wallet.";
            }
            case "rent" -> {
                if(!status.canRent())throw new IllegalArgumentException("Office rental requires EUR 20 in company cash and an expired lease.");
                set(t,c,"cash",status.cash()-RENT);credit(t,"city-maintenance",RENT);
                set(t,c,"rent-until",Math.addExact(day,6));message="Shared office rented for seven days.";
            }
            default -> {
                String[] parts=action.split(":",3);
                if(parts.length!=3||!parts[0].equals("deliver"))throw new IllegalArgumentException("Unknown company operation.");
                var offer=status.offers().stream().filter(o->o.id().equals(parts[1])&&o.available()).findFirst()
                    .orElseThrow(()->new IllegalArgumentException("Contract unavailable: check lease, service, hours, client funds and weekly limit."));
                int value;
                try{if(parts[2].length()>10)throw new NumberFormatException();value=Integer.parseInt(parts[2].strip());}
                catch(NumberFormatException ex){throw new IllegalArgumentException("Enter a whole number.");}
                int expected=offer.id().equals("retail")?retailTotal(t):utilityTotal(t);
                if(value!=expected)throw new IllegalArgumentException("Report does not match live data.");
                String client=offer.id().equals("retail")?"shop-assistant":"city-maintenance";
                set(t,c,"cash",Math.addExact(status.cash(),offer.gross()-offer.tax()));
                t.employerFunds.merge(client,-offer.gross(),Integer::sum);
                credit(t,MunicipalLife.CITY,offer.tax());
                set(t,c,"tax-paid",Math.addExact(status.taxPaid(),offer.tax()));
                set(t,c,"last-"+offer.id(),day);set(t,c,"contracts",Math.addExact(status.contracts(),1));
                duration=60;message="Client paid EUR "+offer.gross()+"; EUR "+offer.tax()+" tax; net company revenue EUR "+(offer.gross()-offer.tax())+".";
            }
        }
        RebirthLife.travel(t,TownLife.Place.OFFICE,day);
        return new RebirthLife.HomeAction(duration,message);
    }
    /** Sole proprietor liquidates remaining cash into the existing inheritance flow once. */
    static void liquidate(TownLife.Town t,TownLife.Citizen c){
        if(c.alive||!registered(t,c))return;
        ITStaffLife.release(t,c);
        c.money=Math.addExact(c.money,get(t,c,"cash"));set(t,c,"cash",0);
        set(t,c,"registered",0);set(t,c,"rent-until",0);
    }
}
