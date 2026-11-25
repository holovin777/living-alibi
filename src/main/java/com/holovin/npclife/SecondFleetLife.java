package com.holovin.npclife;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import static com.holovin.npclife.LogisticsLife.*;

/** Second, independently staffed company truck; shares real goods and cash with the first. */
final class SecondFleetLife {
    private SecondFleetLife() {}
    static final String PLACE="fleet2:place", HEALTH="fleet2:condition", WORKER="fleet2:driver", ENABLED="fleet2:auto",
            DAY="fleet2:dispatch-day", DONE="fleet2:done", STAGE="fleet2:stage", KIND="fleet2:kind",
            FIRST="fleet2:first", SECOND="fleet2:second", INVOICE="fleet2:invoice";
    static void ensure(TownLife.Town t) {
        for(String key:new String[]{PLACE,HEALTH,WORKER,ENABLED,DAY,DONE,STAGE,KIND,FIRST,SECOND,INVOICE})t.industryStocks.putIfAbsent(key,0);
    }
    static boolean owns(TownLife.Town t){return get(t,PLACE)>0;}
    static boolean active(TownLife.Town t){return get(t,STAGE)>0;}
    static TownLife.Place place(TownLife.Town t){return owns(t)?TownLife.Place.values()[get(t,PLACE)-1]:null;}
    static void park(TownLife.Town t,TownLife.Place p){set(t,PLACE,p.ordinal()+1);}
    static TownLife.Citizen driver(TownLife.Town t) {
        int index=get(t,WORKER);
        if(index<=0)return null;
        return t.people.values().stream().filter(c->c.generationIndex==index&&c.alive&&c.employed&&OCCUPATION.equals(c.occupation)).findFirst().orElse(null);
    }
    static FreightLife.Route route(TownLife.Town t){return FreightLife.ROUTES.get(get(t,KIND));}
    static int reservedCrop(TownLife.Town t,String crop) {
        if(!active(t)||get(t,KIND)!=2)return 0;
        return switch(crop){case "vegetables"->get(t,FIRST);case "rice"->get(t,SECOND);default->0;};
    }
    static void office(TownLife.Town t) {
        requireOwner(t);
        if(t.player.place!=TownLife.Place.WORKSHOP)throw new IllegalArgumentException("Visit the workshop office.");
    }
    static String manage(TownLife.Town t,String action,String resident,int day) {
        office(t);
        switch(action) {
            case "buy-truck-2" -> {
                if(!hasTruck(t)||owns(t))throw new IllegalArgumentException("Buy truck one first; fleet limit is two.");
                spend(t,TRUCK_PRICE);t.employerFunds.merge("technician",TRUCK_PRICE,Math::addExact);
                park(t,TownLife.Place.WORKSHOP);set(t,HEALTH,100);record(t,CAPITAL_COST,TRUCK_PRICE);
                return "Purchased second company truck.";
            }
            case "hire-driver-2" -> {
                if(!owns(t)||driver(t)!=null||active(t))throw new IllegalArgumentException("No second driver vacancy.");
                var c=LogisticsLife.candidates(t,day).stream().filter(x->x.id.equals(resident)).findFirst()
                        .orElseThrow(()->new IllegalArgumentException("Choose another available resident."));
                if(get(t,CASH)<40)throw new IllegalArgumentException("Reserve 40 EUR before hiring.");
                spend(t,SIGNING);record(t,HIRING_COST,SIGNING);c.money=Math.addExact(c.money,SIGNING);
                c.occupation=OCCUPATION;c.workplace=TownLife.Place.WORKSHOP;c.employed=true;
                c.unemploymentDays=0;c.lastHiredDay=day;c.jobChanges++;
                set(t,WORKER,c.generationIndex);set(t,ENABLED,0);
                return "Hired "+c.name+" for truck two.";
            }
            case "auto-on-2","auto-off-2" -> {
                if(driver(t)==null)throw new IllegalArgumentException("Hire the second driver first.");
                set(t,ENABLED,action.equals("auto-on-2")?1:0);
                return "Second truck automatic dispatch "+(get(t,ENABLED)>0?"enabled.":"disabled after current delivery.");
            }
            case "service-truck-2" -> {
                if(!owns(t)||active(t)||get(t,HEALTH)>=100)throw new IllegalArgumentException("Truck two is busy or does not need service.");
                int relocation=place(t)==TownLife.Place.WORKSHOP?0:RELOCATION;
                int fuel=relocation==0?0:1;
                if(FuelLife.stock(t)<fuel||t.technicalMaterials.getOrDefault("spare-parts",0)<1)
                    throw new IllegalArgumentException("Need one spare part and any transport fuel.");
                spend(t,SERVICE+relocation+fuel*FuelLife.PRICE);
                t.technicalMaterials.merge("spare-parts",-1,Integer::sum);
                t.employerFunds.merge("technician",SERVICE,Math::addExact);record(t,REPAIR_COST,SERVICE);
                if(relocation>0) {
                    t.employerFunds.merge(FreightLife.JOB,relocation,Math::addExact);
                    t.employerFunds.merge(FuelLife.ACCOUNT,fuel*FuelLife.PRICE,Math::addExact);
                    t.industryStocks.merge(FuelLife.STOCK,-fuel,Integer::sum);
                    record(t,RELOCATION_COST,relocation);record(t,FUEL_COST,fuel*FuelLife.PRICE);
                    park(t,TownLife.Place.WORKSHOP);
                }
                set(t,HEALTH,Math.min(100,get(t,HEALTH)+RESTORE));
                return "Serviced second truck.";
            }
            default -> throw new IllegalArgumentException("Unknown fleet action.");
        }
    }
    static void dispatch(TownLife.Town t,FreightLife.Route r) {
        if(!owns(t)||active(t)||driver(t)==null)throw new IllegalArgumentException("Truck two unavailable.");
        int first=Math.min(2,FreightLife.stock(t,r.first())),second=Math.min(1,FreightLife.stock(t,r.second()));
        if(r.kind()==2) {
            first=Math.min(first,RetailLife.product("vegetables").capacity()-t.retailStock.getOrDefault("vegetables",0)-FreightLife.reservedCrop(t,"vegetables"));
            second=Math.min(second,RetailLife.product("rice").capacity()-t.retailStock.getOrDefault("rice",0)-FreightLife.reservedCrop(t,"rice"));
        }
        first=Math.max(0,first);second=Math.max(0,second);
        int bill=first*r.firstPrice()+second*r.secondPrice();
        int relocation=place(t)==r.places().getFirst()?0:RELOCATION;
        int fuel=r.places().size()-1+(relocation>0?1:0), fuelCost=fuel*FuelLife.PRICE;
        if(first+second==0||FuelLife.stock(t)<fuel||get(t,HEALTH)<fuel*WEAR
                ||t.employerFunds.getOrDefault(r.buyer(),0)<bill+FreightLife.CONTRACT_FEE)
            throw new IllegalArgumentException("Cargo, fuel, condition or buyer budget unavailable.");
        spend(t,fuelCost+relocation+DRIVER_WAGE);
        record(t,FUEL_COST,fuelCost);record(t,RELOCATION_COST,relocation);
        t.employerFunds.merge(FuelLife.ACCOUNT,fuelCost,Math::addExact);
        t.industryStocks.merge(FuelLife.STOCK,-fuel,Integer::sum);
        if(relocation>0) {
            t.employerFunds.merge(FreightLife.JOB,relocation,Math::addExact);
            set(t,HEALTH,get(t,HEALTH)-WEAR);
        }
        t.employerFunds.merge(r.buyer(),-bill-FreightLife.CONTRACT_FEE,Integer::sum);
        t.industryStocks.merge(r.first(),-first,Integer::sum);
        t.industryStocks.merge(r.second(),-second,Integer::sum);
        park(t,r.places().getFirst());set(t,KIND,r.kind());set(t,FIRST,first);set(t,SECOND,second);
        set(t,INVOICE,bill);set(t,STAGE,1);
    }
    static void clear(TownLife.Town t){for(String key:new String[]{STAGE,KIND,FIRST,SECOND,INVOICE})set(t,key,0);}
    static void recover(TownLife.Town t) {
        if(!active(t))return;
        var r=route(t);
        t.industryStocks.merge(r.first(),get(t,FIRST),Math::addExact);
        t.industryStocks.merge(r.second(),get(t,SECOND),Math::addExact);
        t.employerFunds.merge(r.buyer(),get(t,INVOICE)+FreightLife.CONTRACT_FEE,Math::addExact);
        receive(t,DRIVER_WAGE);clear(t);
    }
    static void finish(TownLife.Town t,TownLife.Citizen c) {
        var r=route(t);int first=get(t,FIRST),second=get(t,SECOND);
        switch(r.kind()) {
            case 0 -> {
                t.industryStocks.merge(IndustryLife.FACTORY_IRON,first,Math::addExact);
                t.industryStocks.merge(IndustryLife.FACTORY_COPPER_ORE,second,Math::addExact);
            }
            case 1 -> {
                t.technicalMaterials.merge("spare-parts",first,Math::addExact);
                t.technicalMaterials.merge("steel",second,Math::addExact);
            }
            case 2 -> {
                t.retailStock.merge("vegetables",first,Math::addExact);
                t.retailStock.merge("rice",second,Math::addExact);
            }
            default -> throw new IllegalStateException("Unknown route.");
        }
        t.employerFunds.merge(r.source(),get(t,INVOICE),Math::addExact);
        receive(t,FreightLife.CONTRACT_FEE);record(t,REVENUE,FreightLife.CONTRACT_FEE);
        record(t,WAGE_COST,DRIVER_WAGE);record(t,DELIVERIES,1);record(t,DONE,1);
        c.money=Math.addExact(c.money,DRIVER_WAGE);c.fatigue=Math.min(100,c.fatigue+8);
        clear(t);
    }
    static void tick(TownLife.Town t,WorldState clock,List<TownLife.Event> events,List<TownLife.Observation> observations) {
        var c=driver(t);
        if(active(t)) {
            if(c==null||EmploymentLife.sick(c,clock.day)||!yours(t)||!t.player.alive) {recover(t);return;}
            if(clock.minute%15!=0||!c.presentAt(place(t)))return;
            var r=route(t);
            if(get(t,STAGE)<r.places().size()) {
                park(t,r.places().get(get(t,STAGE)));record(t,STAGE,1);
                set(t,HEALTH,get(t,HEALTH)-WEAR);
            } else {
                finish(t,c);
                TownLife.publish(t,clock,c.id,c.place,c.name+" completed a second-truck freight delivery.",events,observations);
            }
            return;
        }
        if(c==null||!yours(t)||!t.player.alive||get(t,ENABLED)==0||get(t,DAY)>=clock.day
                ||clock.minute<540||clock.minute>=780||clock.minute%15!=0||(clock.day-1)%7>=5
                ||EmploymentLife.sick(c,clock.day)||!c.presentAt(TownLife.Place.WORKSHOP)||!"WORKING".equals(c.activity))return;
        var choices=new ArrayList<>(FreightLife.ROUTES);
        choices.sort(Comparator.comparingInt(r->r.places().getFirst()==place(t)?0:1));
        for(var r:choices) {
            try {
                dispatch(t,r);set(t,DAY,clock.day);
                TownLife.publish(t,clock,c.id,c.place,c.name+" accepted a second-truck "+r.name()+" delivery.",events,observations);
                break;
            } catch(IllegalArgumentException unavailable) {}
        }
    }
    record Report(boolean purchased,String place,int condition,String driverName,boolean automatic,boolean active,String cargo,int units,int completed) {}
    static Report report(TownLife.Town t) {
        var c=driver(t);var p=place(t);
        return new Report(owns(t),p==null?null:p.name(),get(t,HEALTH),c==null?null:c.name,get(t,ENABLED)>0,
                active(t),active(t)?route(t).name():null,get(t,FIRST)+get(t,SECOND),get(t,DONE));
    }
}
