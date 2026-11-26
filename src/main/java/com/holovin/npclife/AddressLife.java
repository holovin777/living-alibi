package com.holovin.npclife;

import java.util.ArrayList;
import java.util.List;

/** Fictional building-level addresses. Private units and whereabouts are not exposed. */
final class AddressLife {
    private AddressLife() {}
    record Entry(String residentId,String name,String address,String district) {}
    private static String key(TownLife.Town t,String id){
        return "address:"+t.player.incarnation+":"+id;
    }
    static int building(TownLife.Citizen c){
        if(c.home==TownLife.Place.MARCO_HOME)return 1;
        if(c.home==TownLife.Place.ELENA_HOME||c.home==TownLife.Place.SOUTH_RESIDENCES)return 3;
        if(c.home==TownLife.Place.NORTH_RESIDENCES)
            return Math.floorMod(c.homeUnit.hashCode(),2)==0?1:2;
        return 0;
    }
    static String label(int code){
        return switch(code){
            case 1 -> "Via delle Magnolie 12";
            case 2 -> "Via delle Magnolie 14";
            case 3 -> "Via del Glicine 8";
            default -> "";
        };
    }
    static String district(int code){
        return code==3?"SOUTH_RESIDENCES":code==1||code==2?"NORTH_RESIDENCES":"";
    }
    static String ask(TownLife.Town t,TownLife.Citizen c,PlayerLife.Contact contact){
        if(!PlayerLife.knowsResident(t,c.id)||contact.knownName==null||contact.trust<70||
                contact.affinity<20||c.age<18||!c.alive)
            return "I prefer to keep my address private.";
        int code=building(c);
        if(code==0)return "I do not have a listed address.";
        t.industryStocks.put(key(t,c.id),code);
        return "I live at "+label(code)+".";
    }
    static int sharedBuilding(TownLife.Town t,String residentId){
        return t.industryStocks.getOrDefault(key(t,residentId),0);
    }
    static List<Entry> contacts(TownLife.Town t){
        var result=new ArrayList<Entry>();
        for(var contact:t.player.contacts.values()){
            var c=t.people.get(contact.residentId);
            if(c==null||!PlayerLife.knowsResident(t,c.id))continue;
            int code=t.industryStocks.getOrDefault(key(t,c.id),0);
            result.add(new Entry(c.id,c.name,label(code),district(code)));
        }
        return List.copyOf(result);
    }
}
