package com.holovin.npclife;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A small, deterministic, headless town. Truth and witness knowledge are separate. */
final class TownLife {
    private TownLife() { }

    enum Place {
        MARCO_HOME("Marco's home"), ELENA_HOME("Elena's home"), BAKERY("Bakery"),
        MARKET("Market"), PARK("Park"), CAFE("Cafe");
        final String label;
        Place(String label) { this.label=label; }
    }

    static final class Citizen {
        final String id;
        final String name;
        final String occupation;
        Place place;
        Place destination;
        int travelRemaining;
        String activity;
        int hunger;
        int thirst;
        int fatigue;
        int health;
        int money;
        int food;
        int water;
        int age;
        int sociability;
        boolean alive;

        Citizen(String id, String name, String occupation, Place place) {
            this.id=id;this.name=name;this.occupation=occupation;
            this.place=place;this.destination=place;this.activity="RELAXING";
            this.hunger=20;this.thirst=20;this.fatigue=20;this.health=100;
            this.money=55;this.food=3;this.water=3;this.age=32;this.alive=true;this.sociability=62;
        }
        Citizen copy() {
            Citizen c=new Citizen(id,name,occupation,place);
            c.destination=destination;c.travelRemaining=travelRemaining;c.activity=activity;
            c.hunger=hunger;c.thirst=thirst;c.fatigue=fatigue;c.health=health;
            c.money=money;c.food=food;c.water=water;c.age=age;c.alive=alive;c.sociability=sociability;
            return c;
        }
        boolean presentAt(Place p) {return alive && travelRemaining==0 && place==p;}
        String locationText() {
            if(!alive)return place.label+" (deceased)";
            if(travelRemaining>0)return "in transit to "+destination.label+" ("+travelRemaining+" min remaining)";
            return place.label;
        }
    }

    static final class Town {
        final Map<String,Citizen> people=new LinkedHashMap<>();
        boolean together;
        long meetingSinceTick=-1;
        SocialLife.Relationship relationship=new SocialLife.Relationship("marco","elena");
        Citizen get(String id) {
            Citizen p=people.get(id.toLowerCase());
            if(p==null)throw new IllegalArgumentException("Unknown resident: "+id+". Try marco or elena.");
            return p;
        }
        Town copy() {
            Town t=new Town();t.together=together;t.meetingSinceTick=meetingSinceTick;
            t.relationship=relationship.copy();
            for(var e:people.entrySet())t.people.put(e.getKey(),e.getValue().copy());
            return t;
        }
        void copyFrom(Town other) {
            people.clear();for(var e:other.people.entrySet())people.put(e.getKey(),e.getValue().copy());
            together=other.together;meetingSinceTick=other.meetingSinceTick;
            relationship=other.relationship.copy();
        }
    }

    record Event(int day,int minute,String actorId,Place place,String text) { }
    record Observation(String witnessId,int day,int minute,Place place,String text) { }
    static Town create(WorldState marco) {
        Town t=new Town();
        Place initial=desiredMarco(marco,null);
        t.people.put("marco",new Citizen("marco","Marco Bianchi","Baker",initial));
        t.people.put("elena",new Citizen("elena","Elena Rossi","Shopkeeper",desiredElena(marco.day,marco.minute)));
        t.get("elena").activity="WORKING";
        t.together=initial==t.get("elena").place;
        if(t.together)t.meetingSinceTick=(long)(marco.day-1)*1440L+marco.minute;
        return t;
    }

    static void tick(Town town,WorldState marco,List<Event> truth,List<Observation> observations) {
        tick(town,marco,truth,observations,new ArrayList<>());
    }
    static void tick(Town town,WorldState marco,List<Event> truth,List<Observation> observations,
                     List<SocialLife.Interaction> interactions) {
        Citizen m=town.get("marco"), e=town.get("elena");
        m.alive=marco.alive;
        m.age=marco.age;
        m.hunger=marco.hunger;m.thirst=marco.thirst;m.fatigue=marco.fatigue;
        m.health=marco.health;m.money=marco.money;m.activity=marco.activity;
        m.food=marco.food;m.water=marco.water;
        if(m.alive)move(town,m,desiredMarco(marco,m.place),marco,truth,observations);
        elenaNeeds(e,marco,truth,observations,town);
        if(e.alive)move(town,e,desiredElena(marco.day,marco.minute,e),marco,truth,observations);
        boolean nowTogether=m.alive&&e.alive&&m.travelRemaining==0&&e.travelRemaining==0&&m.place==e.place;
        if(nowTogether&&!town.together) {
            town.meetingSinceTick=(long)(marco.day-1)*1440L+marco.minute;
            SocialLife.onEncounter(town);
            publish(town,marco,"marco",m.place,"Marco Bianchi and Elena Rossi crossed paths at "+m.place.label+".",truth,observations);
        }
        if(!nowTogether)town.meetingSinceTick=-1;
        town.together=nowTogether;
        SocialLife.tick(town,marco,truth,observations,interactions);
    }

    private static int clamp(int n) {return Math.max(0,Math.min(100,n));}
    private static void elenaNeeds(Citizen e,WorldState clock,List<Event> events,List<Observation> observations,Town town) {
        if(!e.alive)return;
        int minute=clock.minute;
        if(minute==0 && clock.day>1 && (clock.day-1)%365==0) {
            e.age++;
            publish(town,clock,"elena",e.place,"Elena Rossi turned "+e.age+".",events,observations);
        }
        if(minute%12==0)e.hunger=clamp(e.hunger+1);
        if(minute%10==0)e.thirst=clamp(e.thirst+1);
        boolean sleeping=minute<390||minute>=1320;
        e.activity=sleeping?"SLEEPING":(isWorkTime(clock.day,minute)?"WORKING":"FREE_TIME");
        if(minute%15==0)e.fatigue=clamp(e.fatigue+(sleeping?-3:1));
        if(e.travelRemaining>0)return;
        if(e.hunger>=65&&e.food>0&&(e.place==Place.ELENA_HOME||e.place==Place.MARKET)) {
            e.food--;e.hunger=clamp(e.hunger-48);
            publish(town,clock,"elena",e.place,"Elena Rossi ate a meal.",events,observations);
        }
        if(e.thirst>=65&&e.water>0&&(e.place==Place.ELENA_HOME||e.place==Place.MARKET)) {
            e.water--;e.thirst=clamp(e.thirst-50);
            publish(town,clock,"elena",e.place,"Elena Rossi drank water.",events,observations);
        }
        if(e.place==Place.MARKET && minute>=420 && minute<1200) {
            if(e.food<2 && e.money>=9) {
                e.food+=3;e.money-=9;
                publish(town,clock,"elena",e.place,"Elena Rossi purchased groceries.",events,observations);
            }
            if(e.water<2 && e.money>=3) {
                e.water+=3;e.money-=3;
                publish(town,clock,"elena",e.place,"Elena Rossi purchased bottled water.",events,observations);
            }
            if(isWorkTime(clock.day,minute)&&minute%60==0) {
                e.money+=12;
                publish(town,clock,"elena",e.place,"Elena Rossi earned her hourly wage at the market.",events,observations);
            }
        }
        if(minute%60==0 && (e.hunger>93||e.thirst>93)) e.health=clamp(e.health-2);
        // Elena can recover after a shortage instead of remaining permanently at low health.
        if(minute%180==0 && e.hunger<65 && e.thirst<60 && e.fatigue<85)
            e.health=clamp(e.health+1);
        if(e.health==0) {
            e.alive=false;e.activity="DECEASED";
            publish(town,clock,"elena",e.place,"Elena Rossi died from declining health.",events,observations);
        }
    }
    private static boolean isWorkTime(int day,int minute) {
        return (day-1)%7<5 && minute>=420 && minute<960;
    }
    private static Place desiredElena(int day,int minute) {
        // Initial placement, before the citizen has a local inventory snapshot.
        if(minute<390||minute>=1320)return Place.ELENA_HOME;
        if(isWorkTime(day,minute))return Place.MARKET;
        if(minute>=960 && minute<1200)return (day%3==0)?Place.CAFE:Place.PARK;
        return Place.ELENA_HOME;
    }
    private static Place desiredElena(int day,int minute,Citizen e) {
        if(minute<390 || minute>=1320)return Place.ELENA_HOME;
        if(isWorkTime(day,minute))return Place.MARKET;
        // Shopping is a separate need, not an effect of having a weekday job.
        // On weekends she still visits the market when food/water runs low.
        if(minute>=420 && minute<1200 &&
                ((e.food<2 && e.money>=9) || (e.water<2 && e.money>=3)))
            return Place.MARKET;
        if(minute>=960 && minute<1200)return (day%3==0)?Place.CAFE:Place.PARK;
        return Place.ELENA_HOME;
    }
    private static Place desiredMarco(WorldState s,Place current) {
        if(!s.alive)return current==null?Place.MARCO_HOME:current;
        return switch(s.activity) {
            case "WORKING" -> Place.BAKERY;
            case "SOCIALIZING" -> Place.CAFE;
            case "EXPLORING_TOWN", "WALKING_FOR_HEALTH" -> Place.PARK;
            case "SLEEPING", "RESTING_WHILE_SICK", "RELAXING_AT_HOME", "WASHING" -> Place.MARCO_HOME;
            // Short-lived food, water and shopping actions are not yet spatially simulated.
            default -> current==null?Place.MARCO_HOME:current;
        };
    }
    private static int travelTime(Place from,Place to) {
        // Coarse walking times; no route geometry yet.
        return from==to?0:(from==Place.PARK||to==Place.PARK?10:8);
    }
    private static void move(Town town,Citizen c,Place desired,WorldState clock,List<Event> events,List<Observation> observations) {
        if(c.travelRemaining>0) {
            c.travelRemaining--;
            if(c.travelRemaining==0) {
                c.place=c.destination;
                publish(town,clock,c.id,c.place,c.name+" arrived at "+c.place.label+".",events,observations);
            }
            return;
        }
        if(c.place!=desired) {
            c.destination=desired;
            c.travelRemaining=travelTime(c.place,desired);
            c.activity="TRAVELING";
        }
    }
    static void publish(Town t,WorldState clock,String actor,Place where,String text,
                                List<Event> truth,List<Observation> observations) {
        truth.add(new Event(clock.day,clock.minute,actor,where,text));
        for(Citizen citizen:t.people.values()) {
            if(citizen.presentAt(where))
                observations.add(new Observation(citizen.id,clock.day,clock.minute,where,text));
        }
    }
}
