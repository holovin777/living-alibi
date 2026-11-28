package com.holovin.npclife;

import java.util.List;
import static com.holovin.npclife.TownLife.Place.*;

/** Timetabled abstract municipal shuttle. Java owns time, fares and endpoints. */
final class BusLife {
    private BusLife(){}
    static final int FARE=2;
    static boolean stop(TownLife.Place p){return p==PARK||p==STAZIONE||p==WORKSHOP||CityExpansionLife.serviceHub(p);}
    static boolean route(TownLife.Place a,TownLife.Place b){
        if(a==b||!stop(a)||!stop(b))return false;
        int distance=CityExpansionLife.distance(a,b);
        return distance>0&&distance<=7;
    }
    static boolean stop(TownLife.Town t,TownLife.Place p){return stop(p)||TransportDevelopmentLife.opened(t,p);}
    static boolean route(TownLife.Town t,TownLife.Place a,TownLife.Place b){
        int distance=CityExpansionLife.distance(a,b);return a!=b&&stop(t,a)&&stop(t,b)&&distance>0&&distance<=7;
    }
    static int duration(TownLife.Town t,TownLife.Place a,TownLife.Place b,int minute){return duration(a,b,minute)+TransportDevelopmentLife.delay(t,a,b);}
    static int duration(TownLife.Town t,WorldState s,TownLife.Place a,TownLife.Place b){
        int id=MunicipalBusFleetLife.boarding(t,s,a,b);return id>0?MunicipalBusFleetLife.remaining(t,s,id):duration(t,a,b,s.minute);
    }
    static int waitMinutes(int minute){return Math.floorMod(-minute,15);}
    static boolean open(int minute){return minute>=360&&minute+waitMinutes(minute)<1320;}
    static int duration(TownLife.Place a,TownLife.Place b,int minute){return waitMinutes(minute)+Math.max(4,CityExpansionLife.distance(a,b)*2);}
    record Route(String id,String label,int minutes){}
    record View(boolean atStop,boolean available,int fare,int waitMinutes,List<Route> routes,boolean serviceOpen,int nextDepartureMinute,int localBusTravellers){}
    static View view(TownLife.Town t,WorldState s){
        var p=t.player;
        boolean at=stop(t,p.place);
        boolean available=at&&open(s.minute)&&p.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&p.travelRemaining==0
                &&!p.cycling&&(!p.ownsBicycle||p.bicycleParked!=null)&&p.money>=FARE&&!IntercomLife.insideVisit(t,s.day,s.minute);
        int travellers=at?(int)t.people.values().stream().filter(c->c.alive&&c.place==p.place&&NpcTransportLife.mode(t,c)==NpcTransportLife.BUS).count():0;
        return new View(at,available,FARE,waitMinutes(s.minute),at?PlayerLife.publicPlaces().stream().filter(d->route(t,p.place,d))
                .map(d->new Route(d.name(),d.label,duration(t,s,p.place,d))).toList():List.of(),
                open(s.minute),open(s.minute)?s.minute+waitMinutes(s.minute):-1,travellers);
    }
    static int ride(TownLife.Town t,WorldState s,TownLife.Place destination){
        if(!view(t,s).available()||!route(t,t.player.place,destination))throw new IllegalArgumentException("Bus service unavailable for this journey.");
        int id=MunicipalBusFleetLife.boarding(t,s,t.player.place,destination);
        int minutes=duration(t,s,t.player.place,destination);
        t.player.money-=FARE;
        MunicipalBusFleetLife.fare(t,id);
        t.player.cycling=false;t.player.destination=destination;t.player.travelRemaining=minutes;
        return minutes;
    }
}
