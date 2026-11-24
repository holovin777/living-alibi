package com.holovin.npclife;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Player presence is separate from the world's authoritative NPC and investigator data.
 *  An NPC never receives the player's name unless the player explicitly discloses it. */
final class PlayerLife {
    private PlayerLife() { }

    /** Player's private state (not an NPC identity). */
    static final class Player {
        TownLife.Place place=TownLife.Place.PARK;
        TownLife.Place destination=TownLife.Place.PARK;
        int travelRemaining;
        String privateName; // Never passed to NPCs automatically.
        int hunger = 0;
        int thirst = 0;
        int fatigue = 0;
        int health = 100;
        boolean alive = true;
        final Map<String,Contact> contacts=new LinkedHashMap<>();
        Player copy() {
            Player p=new Player();
            p.place=place;p.destination=destination;p.travelRemaining=travelRemaining;
            p.privateName=privateName;
            p.hunger=hunger;
            p.thirst=thirst;
            p.fatigue=fatigue;
            p.health=health;
            p.alive=alive;
            for(var e:contacts.entrySet())p.contacts.put(e.getKey(),e.getValue().copy());
            return p;
        }
    }

    /** This is what one particular resident remembers about the visitor. */
    static final class Contact {
        final String residentId;
        String knownName; // Null means the visitor has not introduced themselves to this resident.
        int conversations;
        int lastDay;
        int lastMinute;
        String lastHeard;
        Contact(String residentId) {this.residentId=residentId;}
        Contact copy() {
            Contact c=new Contact(residentId);
            c.knownName=knownName;c.conversations=conversations;
            c.lastDay=lastDay;c.lastMinute=lastMinute;c.lastHeard=lastHeard;
            return c;
        }
    }

    private static final List<TownLife.Place> PUBLIC_PLACES=List.of(
            TownLife.Place.PARK, TownLife.Place.CAFE, TownLife.Place.MARKET,
            TownLife.Place.BAKERY, TownLife.Place.LIBRARY, TownLife.Place.OFFICE,
            TownLife.Place.CLINIC, TownLife.Place.SCHOOL, TownLife.Place.WORKSHOP,
            TownLife.Place.NORTH_RESIDENCES, TownLife.Place.SOUTH_RESIDENCES,
            TownLife.Place.RIVER, TownLife.Place.WOODS);

    static List<TownLife.Place> publicPlaces() {return PUBLIC_PLACES;}
    static TownLife.Place destination(String input) {
        String normalized=input.strip().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if(normalized.equals("NORTH"))normalized="NORTH_RESIDENCES";
        if(normalized.equals("SOUTH"))normalized="SOUTH_RESIDENCES";
        try {
            var place=TownLife.Place.valueOf(normalized);
            if(PUBLIC_PLACES.contains(place))return place;
        } catch(IllegalArgumentException ignored) { }
        throw new IllegalArgumentException("Not a public destination: "+input+". Use locations to list public places.");
    }
    static int startTravel(Player player,TownLife.Place place) {
        requireAlive(player);
        if(player.travelRemaining>0)throw new IllegalArgumentException("Already traveling to "+player.destination.label+".");
        if(!PUBLIC_PLACES.contains(place))throw new IllegalArgumentException("Private residences are not open to visitors.");
        if(player.place==place)return 0;
        player.destination=place;
        player.travelRemaining=(player.place==TownLife.Place.PARK||place==TownLife.Place.PARK)?10:8;
        return player.travelRemaining;
    }
    static void tick(Player player) {
        if(player.travelRemaining<=0)return;
        player.travelRemaining--;
        if(player.travelRemaining==0)player.place=player.destination;
    }

    /** Needs follow the authoritative world clock, including waits and offline catch-up.
     *  A public visitor does not automatically consume NPC supplies or reveal their identity. */
    static void tick(Player player,int minute) {
        if(!player.alive)return;
        tick(player);
        if(minute%30==0)player.hunger=Math.min(100,player.hunger+1);
        if(minute%20==0)player.thirst=Math.min(100,player.thirst+1);
        if(minute%30==0)player.fatigue=Math.min(100,player.fatigue+1);
    }

    /** Rest is a short public break, not sleep or ownership of a private home. */
    static int rest(Player player,int minutes) {
        requireAlive(player);
        if(minutes<15 || minutes>240 || minutes%15!=0)
            throw new IllegalArgumentException("Rest for 15–240 game minutes in multiples of 15.");
        if(player.travelRemaining>0)
            throw new IllegalArgumentException("Finish traveling before you rest.");
        int restored=Math.min(player.fatigue,minutes/6);
        player.fatigue-=restored;
        return restored;
    }

    /** A sleeping spot is the *public* part of the current area, never a private home. */
    static int exposure(TownLife.Place place) {
        return switch(place) {
            case RIVER -> 3;
            case WOODS, PARK, NORTH_RESIDENCES, SOUTH_RESIDENCES -> 2;
            case CLINIC, LIBRARY, CAFE -> 0; // Covered public approaches, not private interiors.
            default -> 1;
        };
    }

    record SleepResult(int minutes,int fatigueRecovered,int healthChange,boolean survived) { }

    /** Sleeping is possible in every public scene without owning a bed or dwelling.
     *  Exposure is a fictional deterministic model; weather is not implemented yet. */
    static SleepResult sleep(Player player,int hours) {
        requireAlive(player);
        if(hours<1||hours>12)
            throw new IllegalArgumentException("Sleep between 1 and 12 game hours.");
        if(player.travelRemaining>0)
            throw new IllegalArgumentException("Finish traveling before sleeping.");

        int beforeHealth=player.health;
        int recovered=Math.min(player.fatigue,hours*11);
        player.fatigue-=recovered;
        int exposureLoss=exposure(player.place)*hours/2;
        int deprivation=0;
        if(player.thirst>=85)deprivation+=hours/2;
        if(player.hunger>=90)deprivation+=hours/3;
        if(exposureLoss+deprivation>0) {
            player.health=Math.max(0,player.health-exposureLoss-deprivation);
        } else if(player.hunger<70 && player.thirst<70) {
            player.health=Math.min(100,player.health+hours/4);
        }
        if(player.health==0)player.alive=false;
        return new SleepResult(hours*60,recovered,player.health-beforeHealth,player.alive);
    }

    static void requireAlive(Player player) {
        if(!player.alive)
            throw new IllegalArgumentException("The visitor is deceased. The town continues without them.");
    }

    static String condition(Player player) {
        return "Hunger "+player.hunger+"/100 | Thirst "+player.thirst+
                "/100 | Fatigue "+player.fatigue+"/100 | Health "+player.health+
                "/100 | "+(player.alive?"alive":"deceased");
    }
    static String position(Player player) {
        if(player.travelRemaining>0)return "Traveling to "+player.destination.label+
                " ("+player.travelRemaining+" minute(s) remaining)";
        return player.place.label;
    }
    static List<TownLife.Citizen> visible(TownLife.Town town) {
        List<TownLife.Citizen> people=new ArrayList<>();
        if(!town.player.alive)return people;
        if(town.player.travelRemaining>0)return people;
        String scene=TownLife.publicSpace(town.player.place);
        for(var citizen:town.people.values()) {
            if(citizen.alive && citizen.travelRemaining==0 &&
                    citizen.currentSpace().equals(scene))people.add(citizen);
        }
        return people;
    }
    static TownLife.Citizen nearby(TownLife.Town town,String residentId) {
        requireAlive(town.player);
        TownLife.Citizen person=town.get(residentId);
        if(!visible(town).contains(person))
            throw new IllegalArgumentException(person.name+" is not in your current public space. Use look to see nearby people.");
        return person;
    }
    static String validateName(String name) {
        String n=name.strip();
        if(n.isEmpty()||n.length()>60||n.contains("\n")||n.contains("\r")||n.contains("|"))
            throw new IllegalArgumentException("Choose a name or alias of 1–60 printable characters.");
        for(int i=0;i<n.length();i++)if(Character.isISOControl(n.charAt(i)))
            throw new IllegalArgumentException("Names cannot contain control characters.");
        return n;
    }
    static void setPrivateName(Player p,String name) {p.privateName=validateName(name);}
    static Contact introduce(TownLife.Town town,String id,String alias,int day,int minute) {
        var resident=nearby(town,id);
        if(resident.activity.equals("SLEEPING")||resident.activity.equals("TRAVELING"))
            throw new IllegalArgumentException(resident.name+" cannot respond to an introduction right now.");
        String disclosed=validateName(alias);
        Contact contact=town.player.contacts.computeIfAbsent(resident.id,Contact::new);
        contact.knownName=disclosed;
        contact.lastDay=day;contact.lastMinute=minute;
        contact.lastHeard="My name is "+disclosed+".";
        return contact;
    }
    static String speak(TownLife.Town town,String id,String message,int day,int minute) {
        TownLife.Citizen resident=nearby(town,id);
        String spoken=message.strip();
        if(spoken.isEmpty()||spoken.length()>1000)
            throw new IllegalArgumentException("Speak 1–1000 characters.");
        if(resident.activity.equals("SLEEPING")||resident.activity.equals("TRAVELING"))
            throw new IllegalArgumentException(resident.name+" is not available for a conversation.");
        Contact contact=town.player.contacts.computeIfAbsent(resident.id,Contact::new);
        // A deliberate natural-language introduction counts; normal conversation does NOT.
        if(spoken.regionMatches(true,0,"My name is ",0,11)) {
            String alias=spoken.substring(11).strip();
            if(alias.endsWith("."))alias=alias.substring(0,alias.length()-1);
            contact.knownName=validateName(alias);
        }
        contact.conversations++;
        contact.lastDay=day;contact.lastMinute=minute;contact.lastHeard=spoken;
        String greeting=contact.knownName==null?"stranger":contact.knownName;
        if(spoken.regionMatches(true,0,"My name is ",0,11))
            return "Nice to meet you, "+greeting+".";
        if(resident.sociability<40)return "Hello. I'm having a quiet day.";
        if(spoken.contains("?"))return "I'm not sure, "+greeting+". I can only tell you what I know.";
        if(contact.conversations==1)return "Hello, "+greeting+". Nice to meet someone around here.";
        return "Good to see you again, "+greeting+".";
    }
}
