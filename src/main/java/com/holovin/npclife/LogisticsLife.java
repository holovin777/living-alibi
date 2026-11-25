package com.holovin.npclife;

/** An owner-operated carrier. Its ledger persists with the freight manifest. */
final class LogisticsLife {
    private LogisticsLife() {}
    static final int REGISTRATION=20, TRUCK_PRICE=220, RELOCATION=8;
    static final String OWNER="logistics:owner", CASH="logistics:cash", TRUCK="logistics:truck-place";
    static final String DRIVER="logistics:driver", AUTO="logistics:auto", LAST_DAY="logistics:last-dispatch-day", COMPLETED="logistics:completed";
    static final String OCCUPATION="Company freight driver";
    static final int SIGNING=10, DRIVER_WAGE=18, WEAR=3, SERVICE=15, RESTORE=30;
    static final String CONDITION="logistics:condition", REVENUE="logistics:revenue", FUEL_COST="logistics:fuel-cost", RELOCATION_COST="logistics:relocation-cost", WAGE_COST="logistics:wage-cost", HIRING_COST="logistics:hiring-cost", REPAIR_COST="logistics:repair-cost", CAPITAL_COST="logistics:capital-cost", INVESTED="logistics:invested", WITHDRAWN="logistics:withdrawn", DELIVERIES="logistics:deliveries";
    static void ensure(TownLife.Town t) {
        for(String key:new String[]{OWNER,CASH,TRUCK,DRIVER,AUTO,LAST_DAY,COMPLETED,REVENUE,FUEL_COST,RELOCATION_COST,WAGE_COST,HIRING_COST,REPAIR_COST,CAPITAL_COST,INVESTED,WITHDRAWN,DELIVERIES})t.industryStocks.putIfAbsent(key,0);
        // Additive migration: existing trucks start with full condition, not zero.
        t.industryStocks.putIfAbsent(CONDITION,100);
        SecondFleetLife.ensure(t);
    }
    static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int value){t.industryStocks.put(key,value);}
    static void record(TownLife.Town t,String key,int amount){set(t,key,Math.addExact(get(t,key),amount));}
    static void wear(TownLife.Town t){set(t,CONDITION,Math.max(0,get(t,CONDITION)-WEAR));}
    static boolean yours(TownLife.Town t){return get(t,OWNER)>0 && get(t,OWNER)==t.player.incarnation;}
    static boolean hasTruck(TownLife.Town t){return get(t,TRUCK)>0;}
    static TownLife.Place truckPlace(TownLife.Town t){return hasTruck(t)?TownLife.Place.values()[get(t,TRUCK)-1]:null;}
    static void park(TownLife.Town t,TownLife.Place place){set(t,TRUCK,place.ordinal()+1);}
    static void requireOwner(TownLife.Town t) {
        TransportLife.ready(t.player);
        if(!yours(t))throw new IllegalArgumentException("Register or acquire the logistics company first.");
    }
    static void office(TownLife.Town t) {
        TransportLife.ready(t.player);
        if(t.player.place!=TownLife.Place.WORKSHOP)throw new IllegalArgumentException("Visit the workshop office.");
        if(FreightLife.active(t))throw new IllegalArgumentException("Complete the active freight delivery first.");
    }
    static void spend(TownLife.Town t,int amount) {
        if(amount<0 || get(t,CASH)<amount)throw new IllegalArgumentException("Insufficient company funds.");
        set(t,CASH,get(t,CASH)-amount);
    }
    static void receive(TownLife.Town t,int amount){set(t,CASH,Math.addExact(get(t,CASH),amount));}
    static TownLife.Citizen driver(TownLife.Town t) {
        int index=get(t,DRIVER);
        if(index<=0)return null;
        return t.people.values().stream().filter(c->c.generationIndex==index && c.alive && c.employed && OCCUPATION.equals(c.occupation)).findFirst().orElse(null);
    }
    static boolean isDriver(TownLife.Town t,TownLife.Citizen c){return c==driver(t)||c==SecondFleetLife.driver(t);}
    static TownLife.Place activeDriverDestination(TownLife.Town t,TownLife.Citizen c) {
        if(c==driver(t)&&FreightLife.automated(t))return FreightLife.truckPlace(t);
        if(c==SecondFleetLife.driver(t)&&SecondFleetLife.active(t))return SecondFleetLife.place(t);
        return null;
    }
    static java.util.List<TownLife.Citizen> candidates(TownLife.Town t,int day) {
        if(!yours(t)||t.player.place!=TownLife.Place.WORKSHOP||t.player.travelRemaining>0)return java.util.List.of();
        return t.people.values().stream().filter(c->c.generationIndex>0 && c.presentAt(TownLife.Place.WORKSHOP)
                && !EmploymentLife.sick(c,day) && !BusinessLife.isEmployee(t,c) && !isDriver(t,c)).toList();
    }
    static String hire(TownLife.Town t,String residentId,int day) {
        office(t);requireOwner(t);
        if(!hasTruck(t)||driver(t)!=null)throw new IllegalArgumentException("Buy a truck and ensure its driver position is vacant.");
        var c=candidates(t,day).stream().filter(x->x.id.equals(residentId)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Choose an available resident at the workshop."));
        if(get(t,CASH)<SIGNING+DRIVER_WAGE+12)throw new IllegalArgumentException("Reserve at least 40 EUR for hiring and dispatch.");
        spend(t,SIGNING);c.money=Math.addExact(c.money,SIGNING);record(t,HIRING_COST,SIGNING);
        c.occupation=OCCUPATION;c.workplace=TownLife.Place.WORKSHOP;c.employed=true;
        c.unemploymentDays=0;c.lastHiredDay=day;c.jobChanges++;
        set(t,DRIVER,c.generationIndex);set(t,AUTO,0);
        return c.name+" hired for freight deliveries with a 10 EUR signing payment.";
    }
    static void tick(TownLife.Town t,WorldState clock,java.util.List<TownLife.Event> events,
                     java.util.List<TownLife.Observation> observations) {
        var c=driver(t);
        if(FreightLife.automated(t)) {
            if(c==null||EmploymentLife.sick(c,clock.day)) {
                FreightLife.recover(t);
                return;
            }
            if(!c.presentAt(FreightLife.truckPlace(t)) || clock.minute%15!=0)return;
            if(FreightLife.next(t)!=null) {
                FreightLife.advanceTruck(t,FreightLife.next(t));
            } else {
                String message=FreightLife.finish(t,clock.day,c);
                set(t,COMPLETED,Math.addExact(get(t,COMPLETED),1));
                TownLife.publish(t,clock,c.id,c.place,message,events,observations);
            }
            return;
        }
        if(c==null||!yours(t)||!t.player.alive||get(t,AUTO)==0||FreightLife.active(t)
                ||clock.minute<540||clock.minute>=780||(clock.day-1)%7>=5||clock.minute%15!=0
                ||get(t,LAST_DAY)>=clock.day||!c.presentAt(TownLife.Place.WORKSHOP)
                ||EmploymentLife.sick(c,clock.day)||!"WORKING".equals(c.activity))return;
        // Prefer a route at the current truck position, then other funded routes.
        var choices=new java.util.ArrayList<>(FreightLife.ROUTES);
        choices.sort(java.util.Comparator.comparingInt(r->r.places().getFirst()==truckPlace(t)?0:1));
        for(var route:choices) {
            try {
                String message=FreightLife.dispatch(t,route,true,true);
                set(t,LAST_DAY,clock.day);
                TownLife.publish(t,clock,c.id,c.place,message,events,observations);
                break;
            } catch(IllegalArgumentException unavailable) {
                // All dispatch validation precedes stock/cash mutation.
            }
        }
    }
    static String manage(TownLife.Town t,String action) {
        office(t);
        var p=t.player;
        if("register".equals(action)) {
            if(yours(t))throw new IllegalArgumentException("You already own this company.");
            int previous=get(t,OWNER);
            if(previous>0&&!t.pastLives.containsKey(previous))throw new IllegalArgumentException("The previous owner is still alive.");
            if(p.money<REGISTRATION)throw new IllegalArgumentException("Need 20 EUR to register.");
            p.money-=REGISTRATION;
            t.employerFunds.merge("city-maintenance",REGISTRATION,Math::addExact);
            set(t,OWNER,p.incarnation);
            return "Registered or acquired the carrier. Invest in its separate account.";
        }
        requireOwner(t);
        return switch(action) {
            case "invest-25","invest-100" -> {
                int amount=action.equals("invest-25")?25:100;
                if(p.money<amount)throw new IllegalArgumentException("Insufficient personal savings.");
                p.money-=amount;receive(t,amount);record(t,INVESTED,amount);
                yield "Invested "+amount+" EUR.";
            }
            case "withdraw-25" -> {
                spend(t,25);p.money=Math.addExact(p.money,25);record(t,WITHDRAWN,25);
                yield "Withdrew 25 EUR from company reserves.";
            }
            case "auto-on","auto-off" -> {
                if(driver(t)==null)throw new IllegalArgumentException("Hire a driver first.");
                set(t,AUTO,action.equals("auto-on")?1:0);
                yield "Automatic contracts "+(get(t,AUTO)>0?"enabled.":"disabled.");
            }
            case "service-truck" -> {
                if(!hasTruck(t)||get(t,CONDITION)>=100)throw new IllegalArgumentException("No truck needing service.");
                int relocation=truckPlace(t)==TownLife.Place.WORKSHOP?0:RELOCATION;
                int fuel=relocation==0?0:1;
                if(FuelLife.stock(t)<fuel || t.technicalMaterials.getOrDefault("spare-parts",0)<1)
                    throw new IllegalArgumentException("Service needs one spare part and fuel for transport if required.");
                spend(t,SERVICE+relocation+fuel*FuelLife.PRICE);
                t.technicalMaterials.merge("spare-parts",-1,Integer::sum);
                t.employerFunds.merge("technician",SERVICE,Math::addExact);
                record(t,REPAIR_COST,SERVICE);
                if(relocation>0) {
                    t.employerFunds.merge(FreightLife.JOB,relocation,Math::addExact);
                    t.employerFunds.merge(FuelLife.ACCOUNT,fuel*FuelLife.PRICE,Math::addExact);
                    t.industryStocks.merge(FuelLife.STOCK,-fuel,Integer::sum);
                    record(t,RELOCATION_COST,relocation);record(t,FUEL_COST,fuel*FuelLife.PRICE);
                    park(t,TownLife.Place.WORKSHOP);
                }
                set(t,CONDITION,Math.min(100,get(t,CONDITION)+RESTORE));
                yield "Serviced company truck: one spare part, 15 EUR plus any transport costs.";
            }
            case "buy-truck" -> {
                if(hasTruck(t))throw new IllegalArgumentException("The company already has a truck.");
                spend(t,TRUCK_PRICE);
                t.employerFunds.merge("technician",TRUCK_PRICE,Math::addExact);
                park(t,TownLife.Place.WORKSHOP);set(t,CONDITION,100);record(t,CAPITAL_COST,TRUCK_PRICE);
                yield "Bought the company truck for 220 EUR.";
            }
            default -> throw new IllegalArgumentException("Unknown company action.");
        };
    }
    record Financials(int revenue,int fuel,int relocation,int wages,int hiring,int maintenance,int truckPurchases,int invested,int withdrawn,int operatingNet,int deliveries) {}
    static Financials financials(TownLife.Town t) {
        int revenue=get(t,REVENUE),fuel=get(t,FUEL_COST),relocation=get(t,RELOCATION_COST),wages=get(t,WAGE_COST),hiring=get(t,HIRING_COST),repair=get(t,REPAIR_COST);
        return new Financials(revenue,fuel,relocation,wages,hiring,repair,get(t,CAPITAL_COST),get(t,INVESTED),get(t,WITHDRAWN),revenue-fuel-relocation-wages-hiring-repair,get(t,DELIVERIES));
    }
    record Candidate(String id,String name) {}
    record Report(boolean registered,boolean yours,int cash,boolean truck,String truckPlace,int contractFee,int relocationFee,String driverName,boolean automatic,int completed,java.util.List<Candidate> candidates,int condition,Financials financials,SecondFleetLife.Report secondTruck) {}
    static Report report(TownLife.Town t,int day) {
        var place=truckPlace(t);
        var worker=driver(t);
        return new Report(get(t,OWNER)>0,yours(t),get(t,CASH),hasTruck(t),place==null?null:place.name(),FreightLife.CONTRACT_FEE,RELOCATION,worker==null?null:worker.name,get(t,AUTO)>0,get(t,COMPLETED),candidates(t,day).stream().map(c->new Candidate(c.id,c.name)).toList(),hasTruck(t)?get(t,CONDITION):0,financials(t),SecondFleetLife.report(t));
    }
}
