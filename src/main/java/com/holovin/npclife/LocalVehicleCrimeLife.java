package com.holovin.npclife;

import java.util.List;

/** Abstract, server-authoritative theft in the schematic town. Legal property is never overwritten. */
final class LocalVehicleCrimeLife {
    private LocalVehicleCrimeLife(){}
    static final int CAR=1,BUS=2,TRUCK=3;
    static boolean active(TownLife.Town t){return VehicleTheftLife.active(t)&&VehicleTheftLife.get(t,"local-type")>0;}
    static int type(TownLife.Town t){return VehicleTheftLife.get(t,"local-type");}
    static TownLife.Place parked(TownLife.Town t){
        int code=VehicleTheftLife.get(t,"local-parked");
        return active(t)&&code>0&&code<=TownLife.Place.values().length?TownLife.Place.values()[code-1]:null;
    }
    static TownLife.Citizen owner(TownLife.Town t){
        if(MotorLife.neighbors(t.player.place).isEmpty()||t.player.carPlace==t.player.place)return null;
        return t.people.values().stream().filter(c->NpcTransportLife.enabled(t)?
                NpcTransportLife.owns(t,c)&&!VehicleTheftLife.missing(t,c)&&NpcTransportLife.mode(t,c)!=NpcTransportLife.CAR&&NpcTransportLife.parked(t,c)==t.player.place:
                VehicleTheftLife.owner(c)&&c.travelRemaining==0&&c.place==t.player.place)
                .sorted(java.util.Comparator.comparingInt(c->c.generationIndex)).findFirst().orElse(null);
    }
    static boolean ready(TownLife.Town t){
        var p=t.player;
        return p.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&p.travelRemaining==0&&
                (!p.ownsBicycle||p.bicycleParked!=null);
    }
    static boolean bus(TownLife.Town t,WorldState s){
        return BusLife.stop(t.player.place)&&BusLife.open(s.minute)&&BusLife.waitMinutes(s.minute)<=2;
    }
    static boolean truck(TownLife.Town t){
        return FreightLife.active(t)&&!FreightLife.automated(t)&&FreightLife.stock(t,FreightLife.COMPANY_RUN)==0&&
                !FreightLife.JOB.equals(t.player.jobId)&&FreightLife.truckPlace(t)==t.player.place;
    }
    static List<TownLife.Place> routes(TownLife.Town t){
        if(!active(t)||parked(t)!=t.player.place)return List.of();
        if(type(t)==BUS)return PlayerLife.publicPlaces().stream().filter(d->BusLife.route(t.player.place,d)).toList();
        if(type(t)==TRUCK)return PlayerLife.publicPlaces().stream().filter(d->FreightLife.ROUTES.stream().anyMatch(r->{
            int a=r.places().indexOf(t.player.place),b=r.places().indexOf(d);return a>=0&&b>=0&&Math.abs(a-b)==1;
        })||MotorLife.neighbors(t.player.place).contains(d)).toList();
        return MotorLife.neighbors(t.player.place);
    }
    record View(boolean active,String type,String parked,boolean carAvailable,boolean busAvailable,boolean truckAvailable,int fuel,int condition,List<String> destinations){}
    static View view(TownLife.Town t,WorldState s){
        boolean free=ready(t)&&!VehicleTheftLife.active(t)&&!IntercomLife.insideVisit(t,s.day,s.minute);
        var place=parked(t);
        return new View(active(t),switch(type(t)){case CAR->"CAR";case BUS->"BUS";case TRUCK->"TRUCK";default->"";},
                place==null?null:place.name(),free&&owner(t)!=null,free&&bus(t,s),free&&truck(t),
                active(t)?VehicleTheftLife.get(t,"fuel"):0,active(t)?VehicleTheftLife.get(t,"condition"):0,
                ready(t)?routes(t).stream().map(Enum::name).toList():List.of());
    }
    static void take(TownLife.Town t,WorldState s,String kind,List<TownLife.Event> events,List<TownLife.Observation> observations){
        var report=view(t,s);
        int type=switch(kind){case "car"->CAR;case "bus"->BUS;case "truck"->TRUCK;default->throw new IllegalArgumentException("Unknown vehicle.");};
        if(!(type==CAR?report.carAvailable():type==BUS?report.busAvailable():report.truckAvailable()))
            throw new IllegalArgumentException("That vehicle is not available.");
        // For a freight theft return reserved goods and payments before removing the truck from its job.
        if(type==TRUCK)FreightLife.recover(t);
        VehicleTheftLife.take(t,s,type==CAR?owner(t):null,events,observations);
        if(VehicleTheftLife.active(t)){
            VehicleTheftLife.set(t,"local-type",type);
            VehicleTheftLife.set(t,"local-parked",t.player.place.ordinal()+1);
        }
    }
    static int drive(TownLife.Town t,TownLife.Place destination){
        JusticeLife.requireFree(t);TransportLife.ready(t.player);
        if(!ready(t)||!routes(t).contains(destination))throw new IllegalArgumentException("No stolen vehicle route from here.");
        int fuel=VehicleTheftLife.get(t,"fuel"),condition=VehicleTheftLife.get(t,"condition");
        if(fuel<1||condition<3)throw new IllegalArgumentException("Stolen vehicle needs fuel or service.");
        int minutes=type(t)==BUS?Math.max(4,CityExpansionLife.distance(t.player.place,destination)*2):3;
        VehicleTheftLife.set(t,"fuel",fuel-1);VehicleTheftLife.set(t,"condition",condition-3);
        VehicleTheftLife.set(t,"local-parked",destination.ordinal()+1);
        t.player.destination=destination;t.player.travelRemaining=minutes;t.player.cycling=false;
        return minutes;
    }
    static String abandon(TownLife.Town t){
        if(!active(t)||parked(t)!=t.player.place)throw new IllegalArgumentException("Return to the stolen vehicle.");
        VehicleTheftLife.recover(t);
        return "Stolen vehicle recovered. Police report remains active.";
    }
}
