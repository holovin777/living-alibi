package com.holovin.npclife;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/** English-language console observer. The simulation does not depend on OpenAI availability. */
public final class Main {
    private static final Path SAVE = Path.of("data", "world.db");
    private Main() { }

    public static void main(String[] args) throws Exception {
        try (WorldDatabase db=new WorldDatabase(SAVE); Scanner scanner=new Scanner(System.in)) {
            WorldState state=db.load();
            TownLife.Town town=db.loadTown(state);
            System.out.println("Living Alibi v1.5 — Social Network");
            System.out.println("OpenAI: " + (OpenAiDialogue.available() ? "available (paid, only on talk/reflect)" : "offline demonstration (no API charges)"));
            System.out.println("Save: " + SAVE.toAbsolutePath());
            if(state.offlineProgress && state.lastSavedEpoch>0) {
                long elapsed=Math.max(0,(Instant.now().getEpochSecond()-state.lastSavedEpoch)/60);
                if(elapsed>0) {
                    long simulated=Math.min(elapsed,525_600L); // protective 1-year limit per relaunch
                    System.out.println("Replaying "+simulated+" offline minute(s) without API calls...");
                    advanceAndSave(db,state,town,simulated);
                    if(elapsed>simulated)System.out.println("Offline interval exceeded one year; extra time was not simulated.");
                }
            }
            help();
            while(true) {
                System.out.print("\n[Day "+state.day+" "+clock(state.minute)+"] > ");
                if(!scanner.hasNextLine())break;
                String input=scanner.nextLine().trim();
                if(input.isBlank())continue;
                String lower=input.toLowerCase(Locale.ROOT);
                try {
                    if(lower.equals("exit")||lower.equals("quit")) break;
                    if(lower.equals("help")) {help();continue;}
                    if(lower.equals("status")||lower.equals("observe")) {status(state);continue;}
                    if(lower.equals("personality")) {personality(state);continue;}
                    if(lower.equals("town")) {townStatus(town,state);continue;}
                    if(lower.equals("population") || lower.startsWith("population ")) {
                        String[] parts=input.split("\\s+");
                        if(parts.length==1 || (parts.length==2 && parts[1].matches("[0-9]+"))) {
                            int limit=parts.length==1?20:Integer.parseInt(parts[1]);
                            if(limit<1||limit>100)throw new IllegalArgumentException("List 1–100 residents.");
                            populationStatus(town,limit);continue;
                        }
                        if(parts.length==3 && (parts[1].equalsIgnoreCase("add")||parts[1].equalsIgnoreCase("generate"))) {
                            int count=Integer.parseInt(parts[2]);
                            var candidate=town.copy();
                            var added=PopulationGenerator.add(candidate,count);
                            db.save(state,List.of(),List.of(),candidate,List.of(),List.of(),List.of());
                            town.copyFrom(candidate);
                            System.out.println("Generated "+added.size()+" residents. Use population to browse their IDs.");
                            for(var c:added)System.out.println("  "+c.id+" | "+c.name+" | "+c.occupation);
                            continue;
                        }
                        throw new IllegalArgumentException("Use population, population 20, or population generate 12.");
                    }
                    if(lower.startsWith("resident ")) {
                        var citizen=town.get(arg(input));
                        residentStatus(citizen);continue;
                    }
                    if(lower.startsWith("home ")) {
                        var citizen=town.get(arg(input));
                        System.out.println(citizen.name+" lives in "+
                                TownLife.sceneLabel(citizen.home,citizen.homeUnit));
                        continue;
                    }
                    if(lower.equals("relationships") || lower.startsWith("relationships ")) {
                        int limit=optionalLimit(input,20);
                        relationshipOverview(town,limit);continue;
                    }
                    if(lower.startsWith("relationship ")) {
                        String[] ids=arg(input).toLowerCase(Locale.ROOT).split("\\s+");
                        if(ids.length!=2)throw new IllegalArgumentException("Use relationship <first-id> <second-id>.");
                        var first=town.get(ids[0]);var second=town.get(ids[1]);
                        var relation=town.existingRelationship(first.id,second.id);
                        if(relation==null)System.out.println("No encounter recorded for this pair yet.");
                        else relationshipStatus(town,relation);
                        continue;
                    }
                    if(lower.equals("social-events") || lower.startsWith("social-events ")) {
                        var entries=db.socialEvents(optionalLimit(input,20));
                        if(entries.isEmpty())System.out.println("No social conversations recorded yet.");
                        for(var e:entries)System.out.println("Day "+e.day()+" "+clock(e.minute())+
                                " | "+e.kind()+" | "+e.place().label+" | "+e.description());
                        continue;
                    }
                    if(lower.startsWith("where ")) {
                        var citizen=town.get(arg(input));
                        System.out.println(citizen.name+" | "+citizen.locationText()+" | "+
                                citizen.activity+" | "+(citizen.alive?"alive":"deceased"));
                        continue;
                    }
                    if(lower.equals("town-events")||lower.startsWith("town-events ")) {
                        var entries=db.townEvents(optionalLimit(input,20));
                        if(entries.isEmpty())System.out.println("No recorded town events yet.");
                        for(var e:entries)System.out.println("Day "+e.day()+" "+clock(e.minute())+
                                " | "+TownLife.sceneLabel(e.place(),e.spaceId())+" | "+e.text());
                        continue;
                    }
                    if(lower.startsWith("known ")) {
                        String[] parts=arg(input).toLowerCase(Locale.ROOT).split("\\s+");
                        if(parts.length<1 || parts.length>2)
                            throw new IllegalArgumentException("Use known marco [count] or known elena [count].");
                        String witness=parts[0];town.get(witness);
                        int limit=parts.length==1?20:Integer.parseInt(parts[1]);
                        if(limit<1||limit>100)throw new IllegalArgumentException("Count must be 1–100.");
                        var entries=db.observations(witness,limit);
                        if(entries.isEmpty())System.out.println("No direct observations recorded for "+witness+".");
                        for(var e:entries)System.out.println("Day "+e.day()+" "+clock(e.minute())+
                                " | "+TownLife.sceneLabel(e.place(),e.spaceId())+" | "+e.text());
                        continue;
                    }
                    if(lower.equals("memories")||lower.startsWith("memories ")) {
                        int limit=optionalLimit(input,12);var entries=db.memories(limit);
                        if(entries.isEmpty())System.out.println("No conversations recorded yet.");
                        for(var m:entries)System.out.println("Day "+m.day()+" "+clock(m.minute())+
                                " — "+(m.speaker().equals("marco")?"Marco":"Player")+": "+m.text());
                        continue;
                    }
                    if(lower.equals("events")||lower.startsWith("events ")) {
                        int limit=optionalLimit(input,20);var entries=db.events(limit);
                        if(entries.isEmpty())System.out.println("No life events recorded yet.");
                        for(var e:entries)System.out.println("Day "+e.day()+" "+clock(e.minute())+" — "+e.text());
                        continue;
                    }
                    if(lower.equals("facts")) {
                        var facts=db.facts(50);
                        if(facts.isEmpty())System.out.println("No long-term facts saved. Try remember My name is ...");
                        else for(String f:facts)System.out.println("- "+f);
                        continue;
                    }
                    if(lower.startsWith("remember ")) {
                        if(!state.alive){System.out.println("Marco is deceased; memories are read-only.");continue;}
                        String fact=arg(input);
                        if(fact.isBlank()||fact.length()>280){System.out.println("Save a fact of 1–280 characters.");continue;}
                        db.rememberFact(state,fact);
                        System.out.println("Fact saved to Marco's long-term memory.");
                        continue;
                    }
                    if(lower.startsWith("wait ")) {
                        long minutes=Long.parseLong(arg(input));
                        if(minutes<1||minutes>525_600L){System.out.println("Allowed range: 1 to 525600 minutes.");continue;}
                        advanceAndSave(db,state,town,minutes);
                        System.out.println("Advanced "+minutes+" game minutes. "+(state.alive?"Marco is "+LifeSimulation.readableActivity(state.activity)+".":"Marco is deceased."));
                        continue;
                    }
                    if(lower.startsWith("simulate ")) {
                        long days=Long.parseLong(arg(input));
                        if(days<1||days>365){System.out.println("Allowed range: 1 to 365 days.");continue;}
                        advanceAndSave(db,state,town,days*1440L);
                        System.out.println("Simulated "+days+" day(s). Type status or events 30 to inspect what happened.");
                        continue;
                    }
                    if(lower.startsWith("speed ")) {
                        int v=Integer.parseInt(arg(input));
                        if(v!=1&&v!=10&&v!=60&&v!=1440) {System.out.println("Choose: speed 1, 10, 60 or 1440");continue;}
                        state.speed=v;
                        db.save(state,List.of(),List.of());
                        System.out.println("Live rate: "+v+" game minute(s) per real second.");
                        continue;
                    }
                    if(lower.startsWith("live ")) {
                        int seconds=Integer.parseInt(arg(input));
                        if(seconds<1||seconds>600) {System.out.println("Allowed range: 1 to 600 real seconds.");continue;}
                        System.out.println("Observing a live simulation for "+seconds+" second(s). Ctrl+C exits the process.");
                        for(int i=0;i<seconds;i++) {
                            Thread.sleep(1000);
                            advanceAndSave(db,state,town,state.speed);
                            if((i+1)%10==0||!state.alive)
                                System.out.println("Day "+state.day+" "+clock(state.minute)+" — "+LifeSimulation.readableActivity(state.activity));
                        }
                        continue;
                    }
                    if(lower.startsWith("offline ")) {
                        String mode=arg(input).toLowerCase(Locale.ROOT);
                        if(!mode.equals("on")&&!mode.equals("off")) {System.out.println("Use offline on or offline off.");continue;}
                        state.offlineProgress=mode.equals("on");
                        db.save(state,List.of(),List.of());
                        System.out.println("Offline catch-up: "+(state.offlineProgress?"ON (replay at next launch, max one year)":"OFF (world pauses while closed)"));
                        continue;
                    }
                    if(lower.equals("reflect")) {
                        if(!state.alive) {System.out.println("Marco is deceased. His history remains available.");continue;}
                        if(!OpenAiDialogue.available()) {System.out.println("Set OPENAI_API_KEY to enable paid reflection. No request was made.");continue;}
                        try {
                            String priority=OpenAiDialogue.reflect(state,db.events(20));
                            state.goal=priority;
                            db.save(state,List.of(new LifeSimulation.LifeEvent(state.day,state.minute,
                                    "Marco reflected on his priorities and chose "+priority+".")),List.of());
                            System.out.println("Marco's new priority: "+priority);
                        } catch(Exception ex) {System.out.println("Reflection failed: "+safeError(ex));}
                        continue;
                    }
                    if(lower.startsWith("talk ")) {
                        if(!state.alive){System.out.println("Marco has passed away. You can still read his memories.");continue;}
                        String message=arg(input);
                        if(message.isBlank()||message.length()>1000){System.out.println("Enter 1–1000 characters after talk.");continue;}
                        if(state.activity.equals("SLEEPING")) {System.out.println("Marco is sleeping. Try again when he is awake.");continue;}
                        String answer;
                        if(OpenAiDialogue.available()) {
                            try {answer=OpenAiDialogue.talk(state,db.memories(18),db.milestones(30),db.facts(50),
                                    db.observations("marco",30),message);}
                            catch(Exception ex) {System.out.println("OpenAI unavailable: "+safeError(ex));answer=demoReply(state);}
                        } else answer=demoReply(state);
                        System.out.println("Marco: "+answer);
                        int day=state.day,minute=state.minute;
                        state.friendship=Math.min(100,state.friendship+1);
                        state.loneliness=Math.max(0,state.loneliness-8);
                        List<TownLife.Event> townEvents=new ArrayList<>();
                        List<TownLife.Observation> observations=new ArrayList<>();
                        List<SocialLife.Interaction> socialEvents=new ArrayList<>();
                        List<LifeSimulation.LifeEvent> events=LifeSimulation.advance(state,5,
                                current -> TownLife.tick(town,current,townEvents,observations,socialEvents));
                        db.save(state,events,List.of(new WorldDatabase.Memory("player",message,day,minute),
                                new WorldDatabase.Memory("marco",answer,day,minute)),town,townEvents,observations,socialEvents);
                        continue;
                    }
                    System.out.println("Unknown command. Type help.");
                } catch(NumberFormatException ex){System.out.println("Enter a valid integer.");}
                catch(IllegalArgumentException ex){System.out.println(ex.getMessage());}
                catch(SQLException ex){System.out.println("Database error: "+safeError(ex));}
            }
            db.save(state,List.of(),List.of(),town,List.of(),List.of());
            System.out.println("World saved. Goodbye.");
        }
    }

    private static void advanceAndSave(WorldDatabase db,WorldState s,TownLife.Town town,long minutes)
            throws SQLException {
        // Compute off-state, then commit the whole world and both event logs together.
        WorldState candidate=s.copy();
        TownLife.Town townCandidate=town.copy();
        List<TownLife.Event> townEvents=new ArrayList<>();
        List<TownLife.Observation> observations=new ArrayList<>();
        List<SocialLife.Interaction> socialEvents=new ArrayList<>();
        List<LifeSimulation.LifeEvent> events=LifeSimulation.advance(candidate,minutes,
                current -> TownLife.tick(townCandidate,current,townEvents,observations,socialEvents));
        db.save(candidate,events,List.of(),townCandidate,townEvents,observations,socialEvents);
        copy(candidate,s);
        town.copyFrom(townCandidate);
    }
    private static void copy(WorldState from,WorldState to) {
        // Loaded state is the only mutable reference in the console; explicit copy avoids surprising aliases.
        to.day=from.day;to.minute=from.minute;to.age=from.age;to.friendship=from.friendship;
        to.hunger=from.hunger;to.thirst=from.thirst;to.fatigue=from.fatigue;to.health=from.health;
        to.money=from.money;to.food=from.food;to.water=from.water;to.workMinutes=from.workMinutes;
        to.activity=from.activity;to.stress=from.stress;to.loneliness=from.loneliness;
        to.mood=from.mood;to.hygiene=from.hygiene;to.illness=from.illness;to.debt=from.debt;
        to.fitness=from.fitness;to.personalitySociable=from.personalitySociable;
        to.personalityCareful=from.personalityCareful;to.personalityCurious=from.personalityCurious;
        to.daysWorked=from.daysWorked;to.missedRent=from.missedRent;to.daysSick=from.daysSick;
        to.alive=from.alive;to.ownsBakery=from.ownsBakery;to.goal=from.goal;to.rng=from.rng;
        to.lastSavedEpoch=from.lastSavedEpoch;to.offlineProgress=from.offlineProgress;
        to.aiAutonomy=from.aiAutonomy;to.speed=from.speed;
    }
    private static int optionalLimit(String input,int defaultValue) {
        int space=input.indexOf(' ');
        if(space<0)return defaultValue;
        int n=Integer.parseInt(input.substring(space+1).trim());
        if(n<1||n>100) throw new IllegalArgumentException("Choose a result count between 1 and 100.");
        return n;
    }
    private static String arg(String input){return input.substring(input.indexOf(' ')+1).trim();}
    private static String safeError(Exception ex) {return ex.getClass().getSimpleName()+" (details withheld; no secrets printed)";}
    static String clock(int minute){return "%02d:%02d".formatted(minute/60,minute%60);}
    private static String demoReply(WorldState s) {
        if(s.friendship==0)return "Hello! I'm Marco, a baker. Nice to meet you. (offline demo)";
        if(s.hunger>65)return "Good to see you again. I'm feeling hungry today. (offline demo)";
        if(s.illness>40)return "I haven't been feeling well lately. (offline demo)";
        return "I remember that we've talked before. How have you been? (offline demo)";
    }
    private static void status(WorldState s) {
        System.out.println("Marco Bianchi | age "+s.age+" | "+(s.alive?"alive":"deceased")+" | Day "+s.day+" "+clock(s.minute));
        System.out.println("Activity: "+LifeSimulation.readableActivity(s.activity)+" | Goal: "+s.goal);
        System.out.println("Hunger "+s.hunger+" | Thirst "+s.thirst+" | Fatigue "+s.fatigue+" | Illness "+s.illness+" (higher = worse)");
        System.out.println("Health "+s.health+" | Fitness "+s.fitness+" | Hygiene "+s.hygiene+" | Mood "+s.mood);
        System.out.println("Stress "+s.stress+" | Loneliness "+s.loneliness+" (higher = worse)");
        System.out.println("Money EUR "+s.money+" | Debt EUR "+s.debt+" | Food "+s.food+" | Water "+s.water);
        System.out.println("Bakery owner: "+s.ownsBakery+" | Friendship with player: "+s.friendship+"/100");
        System.out.println("Offline catch-up: "+(s.offlineProgress?"ON":"OFF")+" | Live speed: "+s.speed+" game min/real sec");
    }
    private static void townStatus(TownLife.Town town,WorldState s) {
        System.out.println("Town status | Day "+s.day+" "+clock(s.minute));
        int shown=0;
        for(TownLife.Citizen citizen:town.people.values()) {
            if(shown++>=12)break;
            System.out.println(citizen.name+" ("+citizen.occupation+") | "+
                    citizen.locationText()+" | "+(citizen.alive?citizen.activity:"deceased"));
            if(citizen.id.equals("elena"))
                System.out.println("  Hunger "+citizen.hunger+" | Thirst "+citizen.thirst+
                        " | Fatigue "+citizen.fatigue+" | Health "+citizen.health+
                        " | Money EUR "+citizen.money);
        }
        if(town.people.size()>12)System.out.println("... and "+(town.people.size()-12)+" more residents (use population).");
        System.out.println("Locations include homes, workplaces, market, cafe and park.");
        System.out.println("Developer view: authoritative town-events and private known logs are for testing.");
    }
    private static void populationStatus(TownLife.Town town,int limit) {
        long alive=town.people.values().stream().filter(p->p.alive).count();
        System.out.println("Population: "+town.people.size()+" total | "+alive+" alive | "+
                (town.people.size()-alive)+" deceased");
        int shown=0;
        for(var p:town.people.values()) {
            if(shown++>=limit)break;
            System.out.println("  "+p.id+" | "+p.name+" | "+p.occupation+
                    " | age "+p.age+" | "+p.locationText());
        }
        if(town.people.size()>limit)System.out.println("Showing "+limit+" of "+town.people.size()+". Use population 50.");
    }
    private static void residentStatus(TownLife.Citizen c) {
        System.out.println(c.name+" | "+c.id+" | "+c.occupation+" | age "+c.age+
                " | "+(c.alive?"alive":"deceased"));
        System.out.println("Home: "+TownLife.sceneLabel(c.home,c.homeUnit)+" | Work: "+c.workplace.label+
                " | Location: "+c.locationText()+" | Activity: "+c.activity);
        System.out.println("Hunger "+c.hunger+" | Thirst "+c.thirst+" | Fatigue "+c.fatigue+
                " | Health "+c.health+" | Money EUR "+c.money+
                " | Food "+c.food+" | Water "+c.water);
        System.out.println("Sociability "+c.sociability+" | Carefulness "+c.carefulness+
                " | Curiosity "+c.curiosity+" | Goal: "+c.goal);
        if(c.generationIndex>0)System.out.println("Procedurally generated citizen (no API calls).");
    }
    private static void relationshipOverview(TownLife.Town town,int limit) {
        List<SocialLife.Relationship> all=new ArrayList<>();
        all.add(town.relationship);
        all.addAll(town.peerRelationships.values());
        all.sort((a,b)->Integer.compare(b.conversations,a.conversations));
        System.out.println("Recorded relationship pairs: "+all.size());
        int shown=0;
        for(var r:all) {
            if(shown++>=limit)break;
            System.out.println("  "+r.firstId+" ↔ "+r.secondId+" | "+r.label()+
                    " | conversations "+r.conversations+" | trust "+r.trust+"/100");
        }
    }
    private static void relationshipStatus(TownLife.Town town,SocialLife.Relationship r) {
        var first=town.get(r.firstId);var second=town.get(r.secondId);
        boolean deceased=!first.alive || !second.alive;
        System.out.println(first.name+" ↔ "+second.name+" | "+r.label()+
                (deceased?" (historical; participant deceased)":""));
        System.out.println("Affinity "+r.affinity+"/100 | Trust "+r.trust+"/100 | Tension "+r.tension+"/100");
        System.out.println("Encounters "+r.encounters+" | Conversations "+r.conversations+
                " | Arguments "+r.arguments);
        System.out.println("This relationship is simulated using local rules (no AI calls).");
    }
    private static void personality(WorldState s) {
        System.out.println("Marco's personality (stable traits, 0–100):");
        System.out.println("Sociability: "+s.personalitySociable+" | Carefulness: "+s.personalityCareful+
                " | Curiosity: "+s.personalityCurious);
        System.out.println("Current priority: "+s.goal+". Traits influence a limited set of behavior rules.");
    }
    private static void help() {
        System.out.println("Commands (English only):");
        System.out.println(" status | observe | personality | events [count] | memories [count] | facts");
        System.out.println(" town | population [count] | population generate 12 | resident resident-0001");
        System.out.println(" home <id>             — inspect a resident's private home unit (developer view)");
        System.out.println(" where <id> | known <id> [count] | town-events [count]");
        System.out.println(" relationships [count] | relationship <id> <id> | social-events [count]");
        System.out.println(" wait 60               — advance 60 game minutes");
        System.out.println(" simulate 7            — simulate seven game days (up to 365)");
        System.out.println(" speed 60              — game minutes per real second in live mode (1,10,60,1440)");
        System.out.println(" live 30               — run live simulation for 30 real seconds");
        System.out.println(" offline on|off        — catch up with elapsed real time on the next launch");
        System.out.println(" talk Hello, Marco!    — AI reply when key is configured, otherwise demo reply");
        System.out.println(" remember My name is ... — explicitly save a durable fact Marco can recall");
        System.out.println(" reflect               — one paid AI call to reconsider Marco's long-term priority");
        System.out.println(" help | exit");
    }
}
