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
        int incarnation = 1; // A fresh visitor receives the next generation.
        int bread;
        int bottledWater;
        long roomUntil;
        boolean ownsHouse;
        boolean ownsBicycle;
        boolean cycling;
        TownLife.Place bicycleParked;
        TownLife.Place carPlace;
        int carFuel,carCondition;
        int storedBread,storedWater;
        final Map<String,Integer> bagItems=new LinkedHashMap<>();
        final Map<String,Integer> pantryItems=new LinkedHashMap<>();
        int hygiene=100;
        int money=50; // Modest personal cash on first entry; no preset profession.
        String jobId; // Null until the player voluntarily applies.
        int engineeringSkill;
        int investigationSkill;
        int lastWorkDay; // 0 means no paid shift yet.
        int lastTechnicalDay; // Requires physical completion before a technical salary.
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
            p.incarnation=incarnation;
            p.money=money;
            p.bread=bread;p.bottledWater=bottledWater;p.roomUntil=roomUntil;p.ownsHouse=ownsHouse;
            p.ownsBicycle=ownsBicycle;p.cycling=cycling;p.bicycleParked=bicycleParked;
            p.carPlace=carPlace;p.carFuel=carFuel;p.carCondition=carCondition;
            p.storedBread=storedBread;p.storedWater=storedWater;p.hygiene=hygiene;
            p.bagItems.putAll(bagItems);p.pantryItems.putAll(pantryItems);
            p.jobId=jobId;
            p.engineeringSkill=engineeringSkill;
            p.investigationSkill=investigationSkill;
            p.lastWorkDay=lastWorkDay;
            p.lastTechnicalDay=lastTechnicalDay;
            for(var e:contacts.entrySet())p.contacts.put(e.getKey(),e.getValue().copy());
            return p;
        }
    }

    /** Private player history, never broadcast into NPC knowledge. */
    record LifeRecord(int incarnation,int day,int minute,TownLife.Place place,
                      String privateName) { }

    /** Start a different person without rewinding the world or inheriting old contacts. */
    static LifeRecord beginNewLife(TownLife.Town town,int day,int minute) {
        Player previous=town.player;
        if(previous.alive)
            throw new IllegalArgumentException("The current visitor is still alive.");
        if(previous.incarnation==Integer.MAX_VALUE)
            throw new IllegalStateException("Maximum visitor incarnation reached.");
        if(town.pastLives.containsKey(previous.incarnation))
            throw new IllegalStateException("This visitor's death was already recorded.");
        LifeRecord record=new LifeRecord(previous.incarnation,day,minute,
                previous.place,previous.privateName);
        town.pastLives.put(record.incarnation(),record);
        FreightLife.recover(town);
        SecondFleetLife.recover(town);
        GardenLife.clear(town);
        HenLife.clear(town);
        OrchardLife.clear(town);
        BankLife.clear(town);
        RentalLife.estate(town);
        ConstructionLife.estate(town);
        MarketStallLife.clear(town);
        Player next=new Player();
        next.incarnation=previous.incarnation+1;
        town.player=next;
        return record;
    }

    /** This is what one particular resident remembers about the visitor. */
    static final class Contact {
        final String residentId;
        String knownName; // Null means the visitor has not introduced themselves to this resident.
        int conversations;
        int trust=50;
        int affinity=0;
        int lastPositiveDay;
        int lastDay;
        int lastMinute;
        String lastHeard;
        Contact(String residentId) {this.residentId=residentId;}
        Contact copy() {
            Contact c=new Contact(residentId);
            c.knownName=knownName;c.conversations=conversations;
            c.trust=trust;c.affinity=affinity;c.lastPositiveDay=lastPositiveDay;
            c.lastDay=lastDay;c.lastMinute=lastMinute;c.lastHeard=lastHeard;
            return c;
        }
    }

    private static final List<TownLife.Place> PUBLIC_PLACES=List.of(
            TownLife.Place.PARK, TownLife.Place.CAFE, TownLife.Place.MARKET,
            TownLife.Place.BAKERY, TownLife.Place.LIBRARY, TownLife.Place.OFFICE,
            TownLife.Place.CLINIC, TownLife.Place.SCHOOL, TownLife.Place.WORKSHOP,
            TownLife.Place.NORTH_RESIDENCES, TownLife.Place.SOUTH_RESIDENCES,
            TownLife.Place.RIVER, TownLife.Place.WOODS,
            TownLife.Place.QUARRY, TownLife.Place.FACTORY, TownLife.Place.CENTRO, TownLife.Place.PORTICI, TownLife.Place.FARM, TownLife.Place.FUEL_STATION, TownLife.Place.LOGISTICS_DEPOT);

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
    /** The authoritative travel estimate used by both console and Godot. */
    static int travelMinutes(Player player,TownLife.Place place) {
        requireAlive(player);
        if(!PUBLIC_PLACES.contains(place))
            throw new IllegalArgumentException("Private residences are not open to visitors.");
        if(player.travelRemaining>0)
            throw new IllegalArgumentException("Already traveling to "+player.destination.label+".");
        if(player.place==place)return 0;
        int walking=player.place==TownLife.Place.PARK||place==TownLife.Place.PARK?10:8;
        return player.ownsBicycle&&player.cycling&&player.bicycleParked==null?(walking+1)/2:walking;
    }
    static int startTravel(Player player,TownLife.Place place) {
        int minutes=travelMinutes(player,place);
        if(minutes==0)return 0;
        player.destination=place;
        player.travelRemaining=minutes;
        return minutes;
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
        if(minute%60==0)player.hygiene=Math.max(0,player.hygiene-1);
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
        return sleepWithExposure(player,hours,exposure(player.place));
    }
    static SleepResult sleepSheltered(Player player,int hours) {
        return sleepWithExposure(player,hours,0);
    }
    private static SleepResult sleepWithExposure(Player player,int hours,int exposure) {
        requireAlive(player);
        if(hours<1||hours>12)
            throw new IllegalArgumentException("Sleep between 1 and 12 game hours.");
        if(player.travelRemaining>0)
            throw new IllegalArgumentException("Finish traveling before sleeping.");

        int beforeHealth=player.health;
        int recovered=Math.min(player.fatigue,hours*11);
        player.fatigue-=recovered;
        int exposureLoss=exposure*hours/2;
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

    /** Weather-aware sleep on the authoritative game clock.
     *  The older two-arg helper remains for isolated baseline rule tests. */
    static SleepResult sleep(Player player,int hours,int day,int minute) {
        if(day<1 || minute<0 || minute>=1440)
            throw new IllegalArgumentException("Invalid game date.");
        SleepResult basic=sleep(player,hours);
        int before=player.health;
        int penalty=WeatherLife.sleepPenalty(player.place,day,minute,hours);
        player.health=Math.max(0,player.health-penalty);
        if(player.health==0)player.alive=false;
        return new SleepResult(basic.minutes(),basic.fatigueRecovered(),
                basic.healthChange()+player.health-before,player.alive);
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
    static String relationship(Contact c) {
        if(c==null)return "STRANGER";
        if(c.affinity<=-20 || c.trust<30)return "WARY";
        if(c.affinity>=30 && c.trust>=70)return "FRIENDLY";
        if(c.affinity>=12 && c.trust>=55)return "FAMILIAR";
        return "ACQUAINTANCE";
    }

    private static void adjust(Contact c,int trust,int affinity) {
        c.trust=Math.max(0,Math.min(100,c.trust+trust));
        c.affinity=Math.max(-100,Math.min(100,c.affinity+affinity));
    }

    private static String introductionKey(TownLife.Town t,String id){
        return "introduction:"+t.player.incarnation+":"+id;
    }
    static boolean knowsResident(TownLife.Town t,String id){
        return t.industryStocks.getOrDefault(introductionKey(t,id),0)>0;
    }
    static String displayName(TownLife.Town t,TownLife.Citizen c){
        return knowsResident(t,c.id)?c.name:"Unknown resident";
    }
    static void learnResident(TownLife.Town t,String id){
        t.industryStocks.put(introductionKey(t,id),1);
    }

    // Accept common typed variants without interpreting arbitrary text as a financial action.
    static String conversationalIntent(String input){
        String text=input.strip();
        String key=text.toLowerCase(java.util.Locale.ROOT).replace('’','\'').replaceAll("[.!?]+$","").strip();
        if(key.startsWith("mi chiamo "))return "My name is "+text.substring(10);
        if(key.startsWith("my name is "))return "My name is "+text.substring(11);
        return switch(key){
            case "hi","hey","hello","ciao","salve","buongiorno","buonasera","good morning","good evening" -> "Hello";
            case "what is your name","what's your name","come ti chiami","qual è il tuo nome" -> "What is your name?";
            case "how are you","how's it going","come stai","come va","tutto bene" -> "How are you?";
            case "what do you do","what's your job","where do you work","che lavoro fai","cosa fai di lavoro","dove lavori" -> "What do you do?";
            case "where do you live","dove abiti","dove vivi" -> "Where do you live?";
            case "sorry","i'm sorry","mi dispiace","scusa","scusami" -> "I'm sorry.";
            case "thank you","thanks","grazie","grazie mille" -> "Thank you";
            case "bye","goodbye","see you later","arrivederci","a presto","ci vediamo" -> "Goodbye";
            case "are you busy","what are you doing","cosa stai facendo","sei occupato","sei occupata" -> "What are you doing?";
            default -> text;
        };
    }

    static String speak(TownLife.Town town,String id,String message,int day,int minute) {
        TownLife.Citizen resident=nearby(town,id);
        String original=message.strip();
        if(original.isEmpty()||original.length()>1000)
            throw new IllegalArgumentException("Speak 1–1000 characters.");
        String spoken=conversationalIntent(original);
        if(resident.activity.equals("SLEEPING")||resident.activity.equals("TRAVELING"))
            throw new IllegalArgumentException(displayName(town,resident)+" is not available for a conversation.");
        Contact contact=town.player.contacts.computeIfAbsent(resident.id,Contact::new);
        boolean askingName=spoken.equalsIgnoreCase("What is your name?")||spoken.equalsIgnoreCase("Come ti chiami?");
        boolean introduction=spoken.regionMatches(true,0,"My name is ",0,11);
        boolean changedAlias=false;
        if(introduction) {
            String alias=spoken.substring(11).strip();
            if(alias.endsWith("."))alias=alias.substring(0,alias.length()-1);
            String disclosed=validateName(alias);
            changedAlias=contact.knownName!=null && !contact.knownName.equalsIgnoreCase(disclosed);
            contact.knownName=disclosed;
        }
        boolean permission=spoken.equalsIgnoreCase("You can tell people my name.");
        if(permission && contact.knownName==null)
            throw new IllegalArgumentException("Introduce yourself before sharing your name.");
        boolean rude=spoken.equalsIgnoreCase("Leave me alone!");
        boolean apology=spoken.equalsIgnoreCase("I'm sorry.");
        boolean askingMood=spoken.equalsIgnoreCase("How are you?") || spoken.equalsIgnoreCase("Come stai?");
        boolean positive=introduction || spoken.equalsIgnoreCase("Hello") || apology || askingMood
                || spoken.equalsIgnoreCase("What do you do?");
        boolean support=spoken.equalsIgnoreCase("I'm here for you.") || spoken.equalsIgnoreCase("Ti sono vicino.");
        boolean recall=spoken.equalsIgnoreCase("What do you remember about me?") || spoken.equalsIgnoreCase("Cosa ricordi di me?");
        if(spoken.equalsIgnoreCase("Ask for a 10 EUR loan")||spoken.equalsIgnoreCase("Repay personal loan")) {
            if(RebirthLife.active(town))throw new IllegalArgumentException("Personal loans are available to visitors only.");
            if(spoken.equalsIgnoreCase("Ask for a 10 EUR loan"))return PersonalLoanLife.borrow(town,resident,contact,day);
            return PersonalLoanLife.repay(town,resident,contact,day);
        }
        boolean giveBread=spoken.equalsIgnoreCase("Offer bread");
        boolean giveWater=spoken.equalsIgnoreCase("Offer water");
        boolean askBread=spoken.equalsIgnoreCase("Ask for bread");
        boolean askWater=spoken.equalsIgnoreCase("Ask for water");
        if(giveBread||giveWater||askBread||askWater){
            String item=giveWater||askWater?"water":"bread";
            if(giveBread||giveWater){
                boolean alreadySupported=SocialMemoryLife.last(town,id,SocialMemoryLife.Kind.SUPPORT)==day;
                if(!MutualAidLife.give(town,resident,item,day))return "I cannot accept that right now.";
                if(!alreadySupported)adjust(contact,5,7);
                return "Thank you for sharing your supplies.";
            }
            return MutualAidLife.receive(town,resident,item,day)?"Here, take this. You need it.":"Sorry, I cannot spare supplies right now.";
        }
        if(changedAlias){
            adjust(contact,-12,-4);
            SocialMemoryLife.remember(town,id,SocialMemoryLife.Kind.BROKEN_TRUST,day);
        } else if(rude){
            adjust(contact,-8,-12);
            SocialMemoryLife.remember(town,id,SocialMemoryLife.Kind.HURT,day);
        } else if(support){
            var mood=EmotionLife.mood(town,resident,day,minute);
            boolean difficult=switch(mood){case TIRED,STRESSED,IRRITATED,SAD,LONELY -> true;default -> false;};
            if(difficult && SocialMemoryLife.last(town,id,SocialMemoryLife.Kind.SUPPORT)!=day){
                adjust(contact,4,6);
                SocialMemoryLife.remember(town,id,SocialMemoryLife.Kind.SUPPORT,day);
            }
        } else if(positive && contact.lastPositiveDay!=day) {
            adjust(contact,apology?5:3,apology?5:(resident.sociability>=60?5:3));
            contact.lastPositiveDay=day;
        }
        if(apology && SocialMemoryLife.unresolved(town,id))
            SocialMemoryLife.remember(town,id,SocialMemoryLife.Kind.APOLOGY,day);
        contact.conversations++;
        contact.lastDay=day;contact.lastMinute=minute;contact.lastHeard=original;
        String greeting=contact.knownName==null?"stranger":contact.knownName;
        if(spoken.equalsIgnoreCase("Where do you live?")||spoken.equalsIgnoreCase("Dove abiti?"))return AddressLife.ask(town,resident,contact);
        if(askingName){
            if(knowsResident(town,id))return "My name is "+resident.name+".";
            var mood=EmotionLife.mood(town,resident,day,minute);
            if(contact.trust<45||mood==EmotionLife.Mood.IRRITATED||mood==EmotionLife.Mood.STRESSED)return "I would rather not say.";
            if(contact.knownName==null&&(resident.sociability<70||contact.conversations<2))return "Why do you ask? Please introduce yourself first.";
            if(resident.sociability<40&&contact.trust<60)return "Maybe another time.";
            learnResident(town,id);
            return "My name is "+resident.name+".";
        }
        if(permission) {
            KnowledgeLife.shareableIntroduction(town,resident,contact.knownName,day,minute);
            return "All right. I may mention that we met.";
        }
        if(recall)return SocialMemoryLife.recollection(town,id);
        if(support)return SocialMemoryLife.last(town,id,SocialMemoryLife.Kind.SUPPORT)==day?"Thank you for being here.":"Thank you for the kind words.";
        if(askingMood)return EmotionLife.reply(EmotionLife.mood(town,resident,day,minute),contact.trust>=55);
        if(spoken.equalsIgnoreCase("Any news about visitors?"))
            return KnowledgeLife.visitorNews(town,resident,day,minute);
        if(changedAlias)return "I remember a different name. I'll remember "+greeting+" for now.";
        if(rude)return "All right. I'll give you some space.";
        if(apology)return contact.trust<30?"I need a little time.":"Thank you for saying that.";
        if(introduction)return "Nice to meet you, "+greeting+".";
        if(relationship(contact).equals("WARY"))return "I'd rather keep this brief, "+greeting+".";
        if(spoken.equalsIgnoreCase("Hello") && relationship(contact).equals("FRIENDLY"))
            return "It's good to see you, "+greeting+"! How have you been?";
        if(spoken.equalsIgnoreCase("What do you do?"))
            return resident.employed?"I work as "+resident.occupation+".":"I'm currently looking for work.";
        if(resident.sociability<40)return "Hello. I'm having a quiet day.";
        if(spoken.contains("?"))return "I'm not sure, "+greeting+". I can only tell you what I know.";
        if(!spoken.equalsIgnoreCase("Hello"))return "I am not sure what you mean. Could you ask me something else?";
        if(contact.conversations==1)return "Hello, "+greeting+". Nice to meet someone around here.";
        return "Good to see you again, "+greeting+".";
    }
}
