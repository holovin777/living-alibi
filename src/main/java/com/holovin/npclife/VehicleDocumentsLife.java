package com.holovin.npclife;

import java.util.List;

/** Fictional insurance and inspection certificates for personal vehicle slots. */
final class VehicleDocumentsLife {
    private VehicleDocumentsLife(){}
    static final String INSURER="motor-insurance";
    static final int INSURANCE_DAYS=30, INSPECTION_DAYS=60, INSPECTION_FEE=10;
    static final List<String> KINDS=List.of("car","scooter","motorcycle");
    static String key(TownLife.Town t,String kind,String field){
        if(!KINDS.contains(kind))throw new IllegalArgumentException("Unknown vehicle type.");
        return "vehicle-docs:"+t.player.incarnation+":"+kind+":"+field;
    }
    static int get(TownLife.Town t,String kind,String field){return t.industryStocks.getOrDefault(key(t,kind,field),0);}
    static void set(TownLife.Town t,String kind,String field,int value){t.industryStocks.put(key(t,kind,field),value);}
    static void clear(TownLife.Town t,String kind){set(t,kind,"insurance",0);set(t,kind,"inspection",0);}
    static TownLife.Place parked(TownLife.Town t,String kind){return kind.equals("car")?t.player.carPlace:LightMotorLife.parked(t,kind);}
    static int condition(TownLife.Town t,String kind){return kind.equals("car")?t.player.carCondition:LightMotorLife.get(t,kind,"condition");}
    static int premium(String kind){return switch(kind){case "car" -> 20;case "motorcycle" -> 15;default -> 10;};}
    static boolean insured(TownLife.Town t,WorldState s,String kind){return get(t,kind,"insurance")>s.day;}
    static boolean inspected(TownLife.Town t,WorldState s,String kind){return get(t,kind,"inspection")>s.day;}
    record Document(String kind,String plate,int insuranceDays,int inspectionDays){}
    static List<Document> view(TownLife.Town t,WorldState s){
        return KINDS.stream().filter(k->parked(t,k)!=null).map(k->new Document(k,VehicleRegistrationLife.personal(t,k),
            Math.max(0,get(t,k,"insurance")-s.day),Math.max(0,get(t,k,"inspection")-s.day))).toList();
    }
    record Option(String id,String label){}
    static List<Option> options(TownLife.Town t,WorldState s){
        if(!t.player.alive||RebirthLife.active(t)||JusticeLife.confined(t)||t.player.travelRemaining>0||t.player.cycling||s.minute<480||s.minute+15>1020)return List.of();
        var result=new java.util.ArrayList<Option>();
        for(String kind:KINDS){
            if(parked(t,kind)==null)continue;
            if(t.player.place==TownLife.Place.OFFICE&&get(t,kind,"insurance")-s.day<=7&&t.player.money>=premium(kind))
                result.add(new Option("vehicle-docs:insurance:"+kind,"Insure / Assicura "+kind+" · €"+premium(kind)));
            if(t.player.place==TownLife.Place.WORKSHOP&&parked(t,kind)==t.player.place&&condition(t,kind)>=60
                    &&get(t,kind,"inspection")-s.day<=7&&t.player.money>=INSPECTION_FEE)
                result.add(new Option("vehicle-docs:inspection:"+kind,"Inspect / Revisione "+kind+" · €"+INSPECTION_FEE));
        }
        return List.copyOf(result);
    }
    static String action(TownLife.Town t,WorldState s,String action,String kind){
        if(options(t,s).stream().noneMatch(o->o.id().equals("vehicle-docs:"+action+":"+kind)))throw new IllegalArgumentException("Vehicle document action unavailable.");
        boolean insurance=action.equals("insurance");
        int price=insurance?premium(kind):INSPECTION_FEE;
        t.player.money-=price;t.employerFunds.merge(insurance?INSURER:"technician",price,Math::addExact);
        set(t,kind,action,Math.addExact(Math.max(s.day,get(t,kind,action)),insurance?INSURANCE_DAYS:INSPECTION_DAYS));
        return (insurance?"Insurance / Assicurazione":"Inspection / Revisione")+" "+kind+" renewed.";
    }
}
