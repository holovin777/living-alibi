package com.holovin.npclife;

import java.util.Locale;

/** Stable fictional world identifiers; no real vehicle registry or real plates. */
final class VehicleRegistrationLife {
    private VehicleRegistrationLife(){}
    static String number(String prefix,int id){
        return "LA "+prefix+Integer.toUnsignedString(id,36).toUpperCase(Locale.ROOT);
    }
    static String personal(TownLife.Town t,String kind){
        return number(switch(kind){case "scooter" -> "S";case "motorcycle" -> "M";default -> "P";},t.player.incarnation);
    }
    static String resident(int generation){return number("R",generation);}
    static String reserve(String id){
        return switch(id){
            case "municipal-reserve-bus" -> "LA BUS1";
            case "freight-reserve-truck" -> "LA TRK1";
            case "municipal-scooter" -> "LA SCO1";
            case "municipal-motorcycle" -> "LA MOT1";
            default -> "";
        };
    }
}
