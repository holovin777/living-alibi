package com.holovin.npclife;

import java.util.List;

/**
 * First small owner-operated venture. Money, machines, raw stock and wages
 * move between existing world accounts: a business is not a money printer.
 *
 * The studio is in the public Workshop district. Employees have normal
 * homes, independent needs and actual work schedules. Ownership belongs
 * to a player incarnation, never automatically to the next visitor.
 */
final class BusinessLife {
    private BusinessLife() { }

    static final int REGISTRATION=15;
    static final int MACHINE_PRICE=25;
    static final int SIGNING_BONUS=10;
    static final int HOURLY_WAGE=11;
    static final int STEEL_PRICE=4;
    static final int PART_PRICE=15;
    static final int FIRST_HOUR=720; // 12:00
    static final int LAST_HOUR=840;  // 14:00
    static final int FABRICATE_MINUTE=795; // 13:15
    static final int SELL_MINUTE=825; // 13:45, while the worker is still present
    static final int MACHINE_WEAR_PER_BATCH=15;
    static final int MIN_WORKABLE_MACHINE_CONDITION=20;
    static final int SERVICE_PARTS=2;
    static final int SERVICE_PRICE=10;
    static final int SERVICE_MINUTES=45;
    static final int SERVICE_RESTORE=60;

    static final class Shop {
        boolean registered;
        int ownerIncarnation;
        int cash;
        boolean machine;
        int machineCondition;
        String employeeId;
        int parts;
        int lastPayDay;
        int lastPayMinute=-1;
        int paidHours;
        int lastProductionDay;
        int lastSaleDay;

        Shop copy() {
            Shop x=new Shop();
            x.registered=registered;
            x.ownerIncarnation=ownerIncarnation;
            x.cash=cash;
            x.machine=machine;
            x.machineCondition=machineCondition;
            x.employeeId=employeeId;
            x.parts=parts;
            x.lastPayDay=lastPayDay;
            x.lastPayMinute=lastPayMinute;
            x.paidHours=paidHours;
            x.lastProductionDay=lastProductionDay;
            x.lastSaleDay=lastSaleDay;
            return x;
        }
    }

    static void ensure(TownLife.Town town) {
        town.employerFunds.putIfAbsent("equipment-supplier",0);
    }

    record Status(boolean registered,boolean yours,int ownerIncarnation,
                  boolean machine,int machineCondition,int cash,String employeeId,
                  String employeeName,int spareParts,int paidHoursToday,
                  int lastProductionDay,int lastSaleDay) { }

    static Status status(TownLife.Town town,int day) {
        Shop b=town.business;
        var worker=town.people.get(b.employeeId);
        boolean employed=worker!=null && worker.employed &&
                "Independent fabricator".equals(worker.occupation);
        return new Status(b.registered,b.registered &&
                b.ownerIncarnation==town.player.incarnation,
                b.ownerIncarnation,b.machine,b.machine?b.machineCondition:0,b.cash,
                employed?worker.id:null,employed?worker.name:null,
                b.parts,b.lastPayDay==day?b.paidHours:0,
                b.lastProductionDay,b.lastSaleDay);
    }

    /** Hiring a co-present neighbor prevents invisible global recruitment. */
    static List<TownLife.Citizen> candidates(TownLife.Town town,int day) {
        if(!town.player.alive || town.player.place!=TownLife.Place.WORKSHOP ||
                town.player.travelRemaining>0)return List.of();
        return town.people.values().stream().filter(c->
                c.generationIndex>0 && c.alive && c.presentAt(TownLife.Place.WORKSHOP) &&
                !EmploymentLife.sick(c,day) &&
                !(c.id.equals(town.business.employeeId) && c.employed &&
                        "Independent fabricator".equals(c.occupation))).toList();
    }

    private static void requireWorkshop(TownLife.Town town) {
        var p=town.player;
        PlayerLife.requireAlive(p);
        if(p.travelRemaining>0 || p.place!=TownLife.Place.WORKSHOP)
            throw new IllegalArgumentException("Go to the Workshop before managing this company.");
    }

    private static void requireOwner(TownLife.Town town) {
        requireWorkshop(town);
        if(!town.business.registered ||
                town.business.ownerIncarnation!=town.player.incarnation)
            throw new IllegalArgumentException("This visitor does not own the workshop company.");
    }

    /** Original registration, or a separately purchased takeover after owner death. */
    static String open(TownLife.Town town) {
        requireWorkshop(town);
        Shop b=town.business;
        if(b.registered && b.ownerIncarnation==town.player.incarnation)
            throw new IllegalArgumentException("You already own this fabrication business.");
        if(b.registered && !town.pastLives.containsKey(b.ownerIncarnation))
            throw new IllegalArgumentException("The current business owner is still alive.");
        if(town.player.money<REGISTRATION)
            throw new IllegalArgumentException("Need 15 EUR for business registration.");
        town.player.money-=REGISTRATION;
        town.employerFunds.put("city-maintenance",
                Math.addExact(town.employerFunds.getOrDefault("city-maintenance",0),REGISTRATION));
        boolean takeover=b.registered;
        b.registered=true;
        b.ownerIncarnation=town.player.incarnation;
        return takeover
                ? "Acquired the existing workshop company without resetting staff, machine or debts."
                : "Registered a small fabrication business. Invest cash to buy a machine and hire a worker.";
    }

    static String invest(TownLife.Town town,int amount) {
        requireOwner(town);
        if(amount<1 || amount>town.player.money)
            throw new IllegalArgumentException("Investment must be positive and within your personal savings.");
        town.player.money-=amount;
        town.business.cash=Math.addExact(town.business.cash,amount);
        return "Transferred "+amount+" EUR from your wallet to the company's separate account.";
    }

    static String withdraw(TownLife.Town town,int amount) {
        requireOwner(town);
        if(amount<1 || amount>town.business.cash)
            throw new IllegalArgumentException("The company cannot withdraw money it does not have.");
        town.business.cash-=amount;
        town.player.money=Math.addExact(town.player.money,amount);
        return "Paid "+amount+" EUR from company reserves to the owner.";
    }

    static String machine(TownLife.Town town) {
        requireOwner(town);
        var b=town.business;
        if(b.machine)throw new IllegalArgumentException("This workshop already owns its machine.");
        if(b.cash<MACHINE_PRICE)
            throw new IllegalArgumentException("The company needs 25 EUR for production equipment.");
        b.cash-=MACHINE_PRICE;
        town.employerFunds.put("equipment-supplier",
                Math.addExact(town.employerFunds.get("equipment-supplier"),MACHINE_PRICE));
        b.machine=true;
        b.machineCondition=100;
        return "Company bought a new fabrication machine at condition 100/100 for 25 EUR.";
    }

    /** Pay for real stocked spare parts; servicing is optional but breakdowns are not. */
    static String service(TownLife.Town town) {
        requireOwner(town);
        Shop b=town.business;
        if(!b.machine)
            throw new IllegalArgumentException("Purchase fabrication equipment before servicing it.");
        if(b.machineCondition>=100)
            throw new IllegalArgumentException("Equipment is already in perfect condition.");
        if(b.cash<SERVICE_PRICE)
            throw new IllegalArgumentException("Business needs 10 EUR to buy machine service parts.");
        if(town.technicalMaterials.getOrDefault("spare-parts",0)<SERVICE_PARTS)
            throw new IllegalArgumentException("City warehouse lacks the two required spare parts.");
        b.cash-=SERVICE_PRICE;
        town.employerFunds.put("city-maintenance",
                Math.addExact(town.employerFunds.getOrDefault("city-maintenance",0),SERVICE_PRICE));
        town.technicalMaterials.put("spare-parts",
                town.technicalMaterials.get("spare-parts")-SERVICE_PARTS);
        int old=b.machineCondition;
        b.machineCondition=Math.min(100,b.machineCondition+SERVICE_RESTORE);
        return "Serviced machine from "+old+" to "+b.machineCondition+
                "/100 using two finite parts and 10 EUR of company cash.";
    }

    static String hire(TownLife.Town town,String residentId,int day) {
        requireOwner(town);
        var b=town.business;
        if(!b.machine)
            throw new IllegalArgumentException("Buy a workshop machine before offering an industrial job.");
        if(b.cash<SIGNING_BONUS+2*HOURLY_WAGE+STEEL_PRICE)
            throw new IllegalArgumentException("Reserve at least 36 EUR for bonus, wages and raw steel.");
        if(b.employeeId!=null) {
            var previous=town.people.get(b.employeeId);
            if(previous!=null && previous.employed &&
                    "Independent fabricator".equals(previous.occupation))
                throw new IllegalArgumentException("The business already has an employed worker.");
        }
        if(residentId==null)
            throw new IllegalArgumentException("Choose a real nearby resident from the candidates.");
        var candidate=candidates(town,day).stream()
                .filter(c->c.id.equals(residentId)).findFirst().orElse(null);
        if(candidate==null)
            throw new IllegalArgumentException("That person is not available at the Workshop.");
        // A specific person accepts a paid contract rather than appearing
        // from thin air. The previous employer does not control their choice.
        b.cash-=SIGNING_BONUS;
        candidate.money=Math.addExact(candidate.money,SIGNING_BONUS);
        candidate.occupation="Independent fabricator";
        candidate.workplace=TownLife.Place.WORKSHOP;
        candidate.employed=true;
        candidate.unemploymentDays=0;
        candidate.lastHiredDay=day;
        candidate.jobChanges++;
        b.employeeId=candidate.id;
        return candidate.name+" signed a workshop contract and received a 10 EUR advance.";
    }

    static boolean isEmployee(TownLife.Town town,TownLife.Citizen c) {
        return town.business.registered && town.business.employeeId!=null &&
                town.business.employeeId.equals(c.id) && c.employed &&
                c.alive && "Independent fabricator".equals(c.occupation);
    }

    static boolean workingHours(int minute) {
        return minute>=FIRST_HOUR && minute<LAST_HOUR;
    }

    /** Called only on actual hourly NPC work ticks; never as a daily bonus. */
    static boolean payHourly(TownLife.Town town,TownLife.Citizen person,
                             WorldState clock,List<TownLife.Event> events,
                             List<TownLife.Observation> observations) {
        if(!"Independent fabricator".equals(person.occupation))return false;
        var b=town.business;
        if(!workingHours(clock.minute) || clock.minute%60!=0 ||
                !isEmployee(town,person) || b.cash<HOURLY_WAGE)return true;
        // A repeated tick at the exact saved clock minute must not pay
        // the same hourly wage again (even after a SQLite restart).
        if(b.lastPayDay==clock.day && (b.lastPayMinute>=clock.minute ||
                b.paidHours>=2))return true;
        b.cash-=HOURLY_WAGE;
        person.money=Math.addExact(person.money,HOURLY_WAGE);
        if(b.lastPayDay!=clock.day) {
            b.lastPayDay=clock.day;
            b.lastPayMinute=-1;
            b.paidHours=0;
        }
        b.lastPayMinute=clock.minute;
        b.paidHours++;
        TownLife.publish(town,clock,person.id,person.place,
                person.name+" received EUR 11 from the independent workshop's account.",
                events,observations);
        return true;
    }

    /** Real output: paid labor + owned machine + nearby paid raw stock. */
    static void tick(TownLife.Town town,WorldState clock,
                     List<TownLife.Event> events,
                     List<TownLife.Observation> observations) {
        var b=town.business;
        if(!b.registered)return;
        int minute=clock.minute,day=clock.day;
        if(minute==FABRICATE_MINUTE && b.lastProductionDay<day) {
            b.lastProductionDay=day;
            var worker=town.people.get(b.employeeId);
            if(b.machine && b.machineCondition>=MIN_WORKABLE_MACHINE_CONDITION &&
                    worker!=null && isEmployee(town,worker) &&
                    worker.presentAt(TownLife.Place.WORKSHOP) &&
                    worker.activity.equals("WORKING") &&
                    b.lastPayDay==day && b.paidHours>=2 &&
                    town.technicalMaterials.getOrDefault("steel",0)>=1 &&
                    b.cash>=STEEL_PRICE) {
                // The shop buys one real steel unit already physically in
                // the municipal Workshop, then produces two spare parts.
                b.cash-=STEEL_PRICE;
                town.employerFunds.put("city-maintenance",
                        Math.addExact(town.employerFunds.get("city-maintenance"),STEEL_PRICE));
                town.technicalMaterials.put("steel",
                        town.technicalMaterials.get("steel")-1);
                b.parts=Math.addExact(b.parts,2);
                b.machineCondition=Math.max(0,b.machineCondition-MACHINE_WEAR_PER_BATCH);
                TownLife.publish(town,clock,worker.id,TownLife.Place.WORKSHOP,
                        worker.name+" fabricated 2 spare parts from one purchased steel unit. "+
                        "Machine condition "+b.machineCondition+"/100.",
                        events,observations);
            }
        }
        if(minute==SELL_MINUTE && b.lastSaleDay<day) {
            b.lastSaleDay=day;
            int sale=Math.min(2,b.parts);
            int price=sale*PART_PRICE;
            var worker=town.people.get(b.employeeId);
            if(sale>0 && worker!=null && isEmployee(town,worker) &&
                    worker.presentAt(TownLife.Place.WORKSHOP) &&
                    worker.activity.equals("WORKING") &&
                    town.employerFunds.getOrDefault("city-maintenance",0)>=price) {
                b.parts-=sale;
                b.cash=Math.addExact(b.cash,price);
                town.employerFunds.put("city-maintenance",
                        town.employerFunds.get("city-maintenance")-price);
                town.technicalMaterials.put("spare-parts",
                        Math.addExact(town.technicalMaterials.getOrDefault("spare-parts",0),sale));
                TownLife.publish(town,clock,worker.id,TownLife.Place.WORKSHOP,
                        "Local workshop sold "+sale+" physical spare parts to city maintenance.",
                        events,observations);
            }
        }
    }
}
