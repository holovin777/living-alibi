package com.holovin.npclife;

import java.util.List;
import static com.holovin.npclife.TownLife.Place.*;

/** A used car parked in a public motor district; no teleportation or free fuel. */
final class MotorLife {
    private MotorLife() { }
    static final int PRICE=180,CAPACITY=12;
    static List<TownLife.Place> neighbors(TownLife.Place place) {
        return switch(place) {
            case OFFICE -> List.of(FACTORY,LIBRARY);
            case LIBRARY -> List.of(OFFICE,WORKSHOP);
            case WORKSHOP -> List.of(LIBRARY,FACTORY,QUARRY);
            case FACTORY -> List.of(OFFICE,WORKSHOP);
            case QUARRY -> List.of(WORKSHOP,FUEL_STATION);
            case FUEL_STATION -> List.of(QUARRY);
            default -> List.of();
        };
    }
    static void workshop(PlayerLife.Player p,int minute) {
        TransportLife.ready(p);
        if(p.place!=WORKSHOP || minute<480 || minute>=1080)
            throw new IllegalArgumentException("Visit the workshop 08:00–18:00.");
    }
    static void present(PlayerLife.Player p) {
        TransportLife.ready(p);
        if(p.carPlace==null || p.carPlace!=p.place)
            throw new IllegalArgumentException("Return to your parked car.");
    }
    static void pay(TownLife.Town t,int price) {
        if(t.player.money<price)throw new IllegalArgumentException("Not enough money.");
        t.player.money-=price;
        t.employerFunds.merge("technician",price,Math::addExact);
    }
    static String buy(TownLife.Town t,int minute) {
        var p=t.player;workshop(p,minute);
        if(p.carPlace!=null)throw new IllegalArgumentException("You already own a car.");
        pay(t,PRICE);p.carPlace=WORKSHOP;p.carFuel=6;p.carCondition=80;
        return "Bought a used compact car for 180 EUR.";
    }
    static String refuel(TownLife.Town t,int minute) {
        return FuelLife.refuel(t,minute);
    }
    static String repair(TownLife.Town t,int minute) {
        var p=t.player;workshop(p,minute);present(p);
        if(p.carCondition==100)throw new IllegalArgumentException("Car already in good condition.");
        pay(t,15);p.carCondition=Math.min(100,p.carCondition+30);
        return "Serviced the car (+up to 30 condition).";
    }
    static int drive(PlayerLife.Player p,TownLife.Place destination) {
        present(p);
        if(!neighbors(p.place).contains(destination))
            throw new IllegalArgumentException("Choose an adjacent motor district.");
        // A portable bicycle must be left at a rack before driving.
        if(p.ownsBicycle && p.bicycleParked==null)
            throw new IllegalArgumentException("Park your bicycle before driving.");
        if(p.carFuel<1)throw new IllegalArgumentException("No fuel.");
        if(p.carCondition<3)throw new IllegalArgumentException("Car needs service.");
        p.carFuel--;p.carCondition-=3;p.cycling=false;
        p.destination=destination;p.travelRemaining=3;p.carPlace=destination;
        return 3;
    }
}
