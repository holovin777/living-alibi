package com.holovin.npclife;

import java.util.*;

/** Shared domestic staging for guests and the linked player's household.
 * Coordinates match GeoInterior._home(); Java remains the source of activities.
 */
final class GeoHome {
    record Slot(double x,double z,double heading,String pose) {}
    private static final Slot[] KITCHEN={
        new Slot(2.2,-5.15,0,"SERVE"),new Slot(4.4,-5.15,0,"SERVE")};
    private static final Slot[] TABLE={
        new Slot(-4.35,4.5,-Math.PI/2,"DESK"),
        new Slot(-1.65,4.5,Math.PI/2,"DESK"),
        new Slot(-3.0,3.45,Math.PI,"DESK"),
        new Slot(-3.0,5.55,0,"DESK")};
    private static final Slot[] PLAY={
        new Slot(-5.0,1.3,Math.PI,"STAND"),new Slot(-3.2,1.3,0,"STAND")};
    private static final Slot[] CLEAN={
        new Slot(-5.0,-2.4,0,"SERVE"),new Slot(-3.2,-2.4,0,"SERVE")};
    private static final Slot[] REST={
        new Slot(-4.0,6.0,Math.PI,"STAND"),new Slot(-2.0,6.0,Math.PI,"STAND")};
    private static final Slot[] HALL={
        new Slot(-0.7,-4.0,0,"STAND"),new Slot(0.7,-4.0,0,"STAND"),
        new Slot(-0.7,-2.0,0,"STAND"),new Slot(0.7,-2.0,0,"STAND"),
        new Slot(-0.7,0.0,0,"STAND"),new Slot(0.7,0.0,0,"STAND"),
        new Slot(-0.7,2.0,0,"STAND"),new Slot(0.7,2.0,0,"STAND")};
    private static Slot[] preferred(String activity){
        return switch(activity){
            case "COOKING","DRINKING" -> KITCHEN;
            case "FAMILY_DINNER","HOMEWORK","READING" -> TABLE;
            case "PLAYING" -> PLAY;
            case "CLEANING" -> CLEAN;
            case "RESTING","WATCHING_TV" -> REST;
            default -> HALL;
        };
    }
    static Slot allocate(String activity,Set<Slot> used){
        for(var slot:preferred(activity))if(used.add(slot))return slot;
        for(var slot:HALL)if(used.add(slot))return slot;
        throw new IllegalArgumentException("Household visual capacity exceeded");
    }
    static List<GeoProjection.Actor> actors(TownLife.Town t,WorldState s,GeoProjection.Point center,
            List<TownLife.Citizen> members,java.util.function.Predicate<TownLife.Citizen> talkable){
        var used=new HashSet<Slot>();
        var result=new ArrayList<GeoProjection.Actor>();
        for(var c:members.stream().limit(8).toList()){
            String activity=HomeRoutineLife.activity(t,c,s.day,s.minute);
            var slot=allocate(activity,used);
            result.add(new GeoProjection.Actor(c.id,center.x()+slot.x(),center.z()+slot.z(),activity,
                talkable.test(c),slot.pose(),slot.heading(),"",GameSession.outfitVisual(c),GameSession.appearanceStage(c)));
        }
        return List.copyOf(result);
    }
}
