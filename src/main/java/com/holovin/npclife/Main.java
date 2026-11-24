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
            System.out.println("Living Alibi v1.15 — Anonymous Visitor");
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
                    if(lower.equals("me")||lower.equals("whereami")) {
                        System.out.println("You | "+PlayerLife.position(town.player)+
                                " | private name: "+(town.player.privateName==null?"not set":town.player.privateName));
                        System.out.println(PlayerLife.condition(town.player));
                        System.out.println("Your name is NOT automatically known by any resident. Investigations are optional.");
                        continue;
                    }
                    if(lower.equals("needs")) {
                        System.out.println(PlayerLife.condition(town.player));
                        continue;
                    }
                    if(lower.equals("sources")) {
                        var available=NatureLife.available(town);
                        System.out.println("Natural resources at "+PlayerLife.position(town.player)+":");
                        if(available.isEmpty())System.out.println("  No accessible resources here.");
                        for(var source:available.values())
                            System.out.println("  "+source.id+" | "+source.label+" | "+
                                    source.stock+"/"+source.capacity+" available");
                        continue;
                    }
                    if(lower.startsWith("drink ")) {
                        var candidate=town.copy();
                        String result=NatureLife.drink(candidate,arg(input),state.day,state.minute);
                        advanceAndSave(db,state,candidate,5);
                        town.copyFrom(candidate);
                        System.out.println(result+" "+PlayerLife.condition(town.player));
                        continue;
                    }
                    if(lower.startsWith("forage ")) {
                        var candidate=town.copy();
                        String result=NatureLife.forage(candidate,arg(input));
                        advanceAndSave(db,state,candidate,15);
                        town.copyFrom(candidate);
                        System.out.println(result+" "+PlayerLife.condition(town.player));
                        continue;
                    }
                    if(lower.equals("rest")||lower.startsWith("rest ")) {
                        int minutes=lower.equals("rest")?60:Integer.parseInt(arg(input));
                        var candidate=town.copy();
                        int recovered=PlayerLife.rest(candidate.player,minutes);
                        advanceAndSave(db,state,candidate,minutes);
                        town.copyFrom(candidate);
                        System.out.println("You rested for "+minutes+" game minutes (up to "+
                                recovered+" fatigue recovered). "+PlayerLife.condition(town.player));
                        continue;
                    }
                    if(lower.equals("explore")) {
                        System.out.println("Free exploration: locations | look | go <place> | speak <id> <text> | introduce <id> [alias] | needs | sources | rest [minutes]");
                        System.out.println("You can simply wander, wait, and meet people. No cases or missions are required.");
                        continue;
                    }
                    if(lower.equals("locations")) {
                        System.out.println("Public places (no access to private apartments):");
                        for(var place:PlayerLife.publicPlaces())
                            System.out.println("  "+place.name().toLowerCase(Locale.ROOT).replace('_','-')+" | "+place.label);
                        continue;
                    }
                    if(lower.equals("look")) {
                        System.out.println("Location: "+PlayerLife.position(town.player));
                        var nearby=PlayerLife.visible(town);
                        if(nearby.isEmpty())System.out.println("No residents are visible here now. You can wait or visit another place.");
                        else for(var person:nearby)
                            System.out.println("  "+person.id+" | "+person.name+" | "+person.activity);
                        continue;
                    }
                    if(lower.startsWith("go ")) {
                        var destination=PlayerLife.destination(arg(input));
                        var candidate=town.copy();
                        int duration=PlayerLife.startTravel(candidate.player,destination);
                        if(duration==0)System.out.println("You are already at "+destination.label+".");
                        else {
                            advanceAndSave(db,state,candidate,duration);
                            town.copyFrom(candidate);
                            System.out.println("You walked to "+destination.label+" ("+duration+" game minutes).");
                        }
                        continue;
                    }
                    if(lower.startsWith("identity ")) {
                        var candidate=town.copy();
                        PlayerLife.setPrivateName(candidate.player,arg(input));
                        db.save(state,List.of(),List.of(),candidate,List.of(),List.of(),List.of());
                        town.copyFrom(candidate);
                        System.out.println("Private identity saved. No resident was told your name.");
                        continue;
                    }
                    if(lower.startsWith("introduce ")) {
                        String[] parts=arg(input).split("\\s+",2);
                        if(parts.length==0 || parts[0].isBlank())
                            throw new IllegalArgumentException("Use introduce <resident-id> [name or alias].");
                        String alias=parts.length>1?parts[1]:town.player.privateName;
                        if(alias==null)throw new IllegalArgumentException("Use identity <name> privately first, or supply introduce <id> <name>.");
                        var candidate=town.copy();
                        var contact=PlayerLife.introduce(candidate,parts[0],alias,state.day,state.minute);
                        advanceAndSave(db,state,candidate,2);
                        town.copyFrom(candidate);
                        System.out.println(candidate.get(contact.residentId).name+" now remembers you as "+contact.knownName+". Others were not informed.");
                        continue;
                    }
                    if(lower.startsWith("speak ")) {
                        String[] parts=arg(input).split("\\s+",2);
                        if(parts.length!=2)throw new IllegalArgumentException("Use speak <resident-id> <message>.");
                        var candidate=town.copy();
                        String response=PlayerLife.speak(candidate,parts[0],parts[1],state.day,state.minute);
                        String residentName=candidate.get(parts[0]).name;
                        advanceAndSave(db,state,candidate,3);
                        town.copyFrom(candidate);
                        System.out.println(residentName+": "+response+" (offline, rule-based)");
                        continue;
                    }
                    if(lower.equals("contacts")) {
                        if(town.player.contacts.isEmpty())System.out.println("You have not spoken to anyone yet.");
                        for(var c:town.player.contacts.values())
                            System.out.println("  "+c.residentId+" | knows you as: "+
                                    (c.knownName==null?"unnamed visitor":c.knownName)+
                                    " | conversations: "+c.conversations);
                        continue;
                    }
                    if(lower.startsWith("contact ")) {
                        var resident=town.get(arg(input));
                        var c=town.player.contacts.get(resident.id);
                        if(c==null)System.out.println(resident.name+" has no recorded conversation or introduction with you.");
                        else {
                            System.out.println(resident.name+" remembers: "+
                                    (c.knownName==null?"an unnamed visitor":c.knownName)+
                                    " | conversations: "+c.conversations+
                                    " | last contact: Day "+c.lastDay+" "+clock(c.lastMinute));
                            if(c.lastHeard!=null)System.out.println("Last thing they heard: "+c.lastHeard);
                        }
                        continue;
                    }
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
                    if(lower.startsWith("inventory ")) {
                        var resident=town.get(arg(input));
                        var belongings=PropertyLife.relevantItems(town,resident.id);
                        System.out.println("Property connected to "+resident.name+": "+belongings.size()+" item(s) (developer view)");
                        for(var item:belongings)
                            System.out.println("  "+item.id+" | "+item.type+" | legal owner: "+item.ownerId+
                                    " | held by: "+(item.holderId==null?"stored":item.holderId)+
                                    " | "+item.currentSpace(town)+" | "+(item.stolen?"STOLEN":"normal"));
                        continue;
                    }
                    if(lower.startsWith("item ")) {
                        String id=arg(input).toLowerCase(Locale.ROOT);
                        var item=town.items.get(id);
                        if(item==null)throw new IllegalArgumentException("Unknown item ID: "+id);
                        System.out.println(id+" | "+item.type+" | legal owner: "+item.ownerId+
                                " | holder: "+(item.holderId==null?"stored":item.holderId)+
                                " | location: "+item.currentSpace(town)+" | stolen: "+item.stolen+
                                " | value EUR "+item.value+" | first recorded day "+item.createdDay);
                        System.out.println("Developer-only authoritative inventory, not detective evidence.");
                        continue;
                    }
                    if(lower.equals("property-events") || lower.startsWith("property-events ")) {
                        var changes=db.propertyChanges(optionalLimit(input,20));
                        if(changes.isEmpty())System.out.println("No property changes recorded yet.");
                        for(var change:changes)
                            System.out.println("Day "+change.day()+" "+clock(change.minute())+
                                    " | "+change.action()+" | "+change.itemId()+
                                    " | "+change.fromId()+" -> "+change.toId()+" | "+change.details());
                        System.out.println("Developer-only property history; do not use during a fair investigation.");
                        continue;
                    }
                    if(lower.startsWith("career ")) {
                        var person=town.get(arg(input));
                        if(person.generationIndex<=0) {
                            System.out.println(person.name+" uses the legacy life model; employment changes apply to procedural residents.");
                        } else {
                            String status=person.employed?"EMPLOYED":"UNEMPLOYED ("+person.unemploymentDays+" day(s))";
                            System.out.println(person.name+" | "+status+" | Last/current role: "+person.occupation+
                                    " | Workplace: "+person.workplace.label+" | New jobs: "+person.jobChanges);
                            System.out.println("Health: "+(EmploymentLife.sick(person,state.day)?
                                    "on sick leave through day "+person.sickUntilDay:"not on sick leave")+
                                    " | Activity: "+person.activity);
                        }
                        System.out.println("Developer-only employment and health state.");
                        continue;
                    }
                    if(lower.equals("life-events") || lower.startsWith("life-events ")) {
                        String[] parts=input.split("\\s+");
                        String resident=null;int limit=20;
                        if(parts.length>=2) {
                            if(parts[1].matches("[0-9]+"))limit=Integer.parseInt(parts[1]);
                            else {resident=town.get(parts[1].toLowerCase(Locale.ROOT)).id;
                                if(parts.length==3)limit=Integer.parseInt(parts[2]);}
                        }
                        if(parts.length>3 || limit<1 || limit>100)
                            throw new IllegalArgumentException("Use life-events [count] or life-events <id> [count], limit 1–100.");
                        var entries=db.residentLifeEvents(resident,limit);
                        if(entries.isEmpty())System.out.println("No new life events recorded yet.");
                        for(var e:entries)System.out.println("Day "+e.day()+" "+clock(e.minute())+
                                " | "+e.residentId()+" | "+e.kind()+" | "+e.detail());
                        continue;
                    }
                    if(lower.startsWith("finances ")) {
                        var person=town.get(arg(input));
                        long owes=town.loans.values().stream().filter(l->l.borrowerId.equals(person.id))
                                .mapToLong(l->l.outstanding).sum();
                        long lent=town.loans.values().stream().filter(l->l.lenderId.equals(person.id))
                                .mapToLong(l->l.outstanding).sum();
                        System.out.println(person.name+" | Cash EUR "+person.money+
                                " | Unpaid bills EUR "+person.unpaidBills+
                                " | Personal loans owed EUR "+owes+" | Receivable EUR "+lent);
                        System.out.println("Developer-only current finances; detective evidence requires an incident-time background.");
                        continue;
                    }
                    if(lower.equals("debts")) {
                        System.out.println("Loan contracts: "+town.loans.size()+" (developer view)");
                        for(var loan:town.loans.values())System.out.println(loan.id+" | "+loan.borrowerId+
                                " owes "+loan.lenderId+" | EUR "+loan.outstanding+"/"+
                                loan.original+" | due day "+loan.dueDay+" | "+loan.status);
                        continue;
                    }
                    if(lower.equals("ledger")||lower.startsWith("ledger ")) {
                        String[] parts=input.split("\\s+");
                        String resident=null;int limit=20;
                        if(parts.length>=2) {
                            if(parts[1].matches("[0-9]+"))limit=Integer.parseInt(parts[1]);
                            else {resident=town.get(parts[1].toLowerCase(Locale.ROOT)).id;
                                if(parts.length==3)limit=Integer.parseInt(parts[2]);}
                        }
                        if(parts.length>3 || limit<1 || limit>100)
                            throw new IllegalArgumentException("Use ledger [count] or ledger <id> [count], limit 1–100.");
                        var entries=db.economyLedger(resident,limit);
                        if(entries.isEmpty())System.out.println("No economy transactions recorded yet.");
                        for(var e:entries)System.out.println("Day "+e.day()+" "+clock(e.minute())+
                                " | "+e.kind()+" | "+e.fromId()+" -> "+e.toId()+
                                " | EUR "+e.amount()+" | "+(e.loanId()==null?"-":e.loanId()));
                        continue;
                    }
                    if(lower.equals("cases")) {
                        System.out.println("Reported cases: "+town.cases.size());
                        if(town.cases.isEmpty())System.out.println("No reports yet. Simulate a week to observe the town.");
                        for(var c:town.cases.values())System.out.println("  "+c.publicSummary(town));
                        continue;
                    }
                    if(lower.startsWith("case ")) {
                        var c=InvestigationLife.caseById(town,arg(input));
                        System.out.println(c.publicSummary(town));
                        System.out.println("Scene inspected: "+c.inspected+" | Interviews: "+c.interviews.size());
                        if(c.inspected)System.out.println("Scene clue: "+InvestigationLife.sceneClue(town,c));
                        for(var entry:c.interviews.entrySet())System.out.println("  "+town.get(entry.getKey()).name+": "+entry.getValue());
                        for(var entry:c.alibiChecks.entrySet())System.out.println("  Alibi check ("+
                                town.get(entry.getKey()).name+"): "+entry.getValue());
                        System.out.println("Evidence searches: "+c.searches.size()+"/3");
                        for(var entry:c.searches.entrySet())System.out.println("  Search ("+
                                town.get(entry.getKey()).name+"): "+entry.getValue());
                        if(c.physicalItemId!=null) {
                            var property=town.items.get(c.physicalItemId);
                            if(c.returned)
                                System.out.println("Physical evidence: RETURNED TO OWNER");
                            else if(property!=null && ("evidence:"+c.id).equals(property.storedSpace))
                                System.out.println("Physical evidence: RECOVERED / HELD IN EVIDENCE");
                            else System.out.println("Physical evidence: NOT RECOVERED");
                        }
                        continue;
                    }
                    if(lower.startsWith("inspect ")) {
                        var candidate=town.copy();
                        String result=InvestigationLife.investigate(candidate,arg(input));
                        db.save(state,List.of(),List.of(),candidate,List.of(),List.of(),List.of());
                        town.copyFrom(candidate);System.out.println(result);continue;
                    }
                    if(lower.startsWith("interview ")) {
                        String[] parts=arg(input).split("\\s+");
                        if(parts.length!=2)throw new IllegalArgumentException("Use interview <resident-id> <case-id>.");
                        var candidate=town.copy();
                        String result=InvestigationLife.interview(candidate,parts[1],parts[0]);
                        db.save(state,List.of(),List.of(),candidate,List.of(),List.of(),List.of());
                        town.copyFrom(candidate);System.out.println(result);continue;
                    }
                    if(lower.startsWith("check-alibi ")) {
                        String[] parts=arg(input).split("\\s+");
                        if(parts.length!=2)throw new IllegalArgumentException("Use check-alibi <case-id> <resident-id>.");
                        var candidate=town.copy();
                        String result=InvestigationLife.checkAlibi(candidate,parts[0],parts[1]);
                        db.save(state,List.of(),List.of(),candidate,List.of(),List.of(),List.of());
                        town.copyFrom(candidate);System.out.println(result);continue;
                    }
                    if(lower.startsWith("background ")) {
                        String[] parts=arg(input).split("\\s+");
                        if(parts.length!=2)throw new IllegalArgumentException(
                                "Use background <case-id> <resident-id>.");
                        System.out.println(InvestigationLife.background(town,parts[0],parts[1]));
                        continue;
                    }
                    if(lower.startsWith("search ")) {
                        String[] parts=arg(input).split("\\s+");
                        if(parts.length!=2)throw new IllegalArgumentException("Use search <case-id> <resident-id>.");
                        var candidate=town.copy();
                        String result=InvestigationLife.search(candidate,state,parts[0],parts[1]);
                        db.save(state,List.of(),List.of(),candidate,List.of(),List.of(),List.of());
                        town.copyFrom(candidate);System.out.println(result);continue;
                    }
                    if(lower.startsWith("return-item ")) {
                        var candidate=town.copy();
                        String result=InvestigationLife.returnItem(candidate,state,arg(input));
                        db.save(state,List.of(),List.of(),candidate,List.of(),List.of(),List.of());
                        town.copyFrom(candidate);System.out.println(result);continue;
                    }
                    if(lower.startsWith("accuse ")) {
                        String[] parts=arg(input).split("\\s+");
                        if(parts.length!=2)throw new IllegalArgumentException("Use accuse <case-id> <resident-id>.");
                        var candidate=town.copy();
                        String result=InvestigationLife.accuse(candidate,parts[0],parts[1]);
                        db.save(state,List.of(),List.of(),candidate,List.of(),List.of(),List.of());
                        town.copyFrom(candidate);System.out.println(result);continue;
                    }
                    if(lower.startsWith("secrets ")) {
                        String[] parts=arg(input).toLowerCase(Locale.ROOT).split("\\s+");
                        if(parts.length<1 || parts.length>2)
                            throw new IllegalArgumentException("Use secrets <resident-id> [count].");
                        var owner=town.get(parts[0]);
                        int limit=parts.length==1?20:Integer.parseInt(parts[1]);
                        if(limit<1||limit>100)throw new IllegalArgumentException("Count must be 1–100.");
                        var items=db.secrets(owner.id,limit);
                        System.out.println("Developer-only private secrets for "+owner.name+": "+items.size());
                        for(var s:items)System.out.println("Day "+s.day()+" "+clock(s.minute())+
                                " | "+s.eventKey()+" | "+s.reason());
                        continue;
                    }
                    if(lower.equals("deceptions")||lower.startsWith("deceptions ")) {
                        var items=db.deceptions(optionalLimit(input,20));
                        if(items.isEmpty())System.out.println("No concealments or deliberate lies recorded yet.");
                        for(var d:items)System.out.println("Day "+d.day()+" "+clock(d.minute())+
                                " | "+d.kind()+" | "+d.fromId()+" -> "+d.toId()+
                                " | "+d.statement());
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
                    if(lower.startsWith("beliefs ")) {
                        String[] parts=arg(input).toLowerCase(Locale.ROOT).split("\\s+");
                        if(parts.length<1 || parts.length>2)
                            throw new IllegalArgumentException("Use beliefs <resident-id> [count].");
                        String resident=town.get(parts[0]).id;
                        int limit=parts.length==1?20:Integer.parseInt(parts[1]);
                        if(limit<1 || limit>100)throw new IllegalArgumentException("Count must be 1–100.");
                        var beliefs=db.beliefs(resident,limit);
                        if(beliefs.isEmpty())System.out.println("No tracked claims for "+resident+" yet.");
                        for(var b:beliefs)System.out.println("Day "+b.eventDay()+" "+clock(b.eventMinute())+
                                " | "+b.origin()+" | confidence "+b.confidence()+"/100 | via "+b.learnedFrom()+
                                " | hops "+b.hops()+" | "+b.statement());
                        continue;
                    }
                    if(lower.equals("rumors")||lower.startsWith("rumors ")) {
                        var transfers=db.informationTransfers(optionalLimit(input,20));
                        if(transfers.isEmpty())System.out.println("No reported information exchanges yet.");
                        for(var t:transfers)System.out.println("Day "+t.day()+" "+clock(t.minute())+
                                " | "+t.fromId()+" -> "+t.toId()+" | confidence "+t.confidence()+
                                "/100 | hops "+t.hops()+" | "+t.statement());
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
                        PlayerLife.nearby(town,"marco"); // No remote disclosure to Marco.
                        String fact=arg(input);
                        if(fact.isBlank()||fact.length()>280){System.out.println("Save a fact of 1–280 characters.");continue;}
                        if(fact.regionMatches(true,0,"My name is ",0,11)) {
                            String disclosed=fact.substring(11).strip();
                            if(disclosed.endsWith("."))disclosed=disclosed.substring(0,disclosed.length()-1);
                            var candidate=town.copy();
                            PlayerLife.introduce(candidate,"marco",disclosed,state.day,state.minute);
                            db.save(state,List.of(),List.of(),candidate,List.of(),List.of(),List.of());
                            town.copyFrom(candidate);
                        }
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
                        PlayerLife.nearby(town,"marco"); // Existing AI chat also requires face-to-face presence.
                        String message=arg(input);
                        if(message.isBlank()||message.length()>1000){System.out.println("Enter 1–1000 characters after talk.");continue;}
                        if(state.activity.equals("SLEEPING")) {System.out.println("Marco is sleeping. Try again when he is awake.");continue;}
                        String answer;
                        if(OpenAiDialogue.available()) {
                            try {answer=OpenAiDialogue.talk(state,db.memories(18),db.milestones(30),db.facts(50),
                                    db.observations("marco",30),message);}
                            catch(Exception ex) {System.out.println("OpenAI unavailable: "+safeError(ex));answer=demoReply(state);}
                        } else answer=demoReply(state);
                        PlayerLife.speak(town,"marco",message,state.day,state.minute);
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
        if(c.generationIndex>0) {
            System.out.println("Employment: "+(c.employed?"EMPLOYED":"UNEMPLOYED for "+c.unemploymentDays+" day(s)")+
                    " | Sick leave end day: "+(c.sickUntilDay==0?"none":c.sickUntilDay)+" | Job changes: "+c.jobChanges);
            System.out.println("Procedurally generated citizen (no API calls).");
        }
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
        System.out.println(" explore | me | needs | locations | look | go <place> — visitor presence and condition");
        System.out.println(" identity <private-name> | introduce <resident-id> [alias] — voluntary identity");
        System.out.println(" speak <resident-id> <message> | contacts | contact <resident-id>");
        System.out.println(" rest [minutes]        — pause for 15–240 game minutes (default: 60)");
        System.out.println(" sources               — inspect finite resources at your location");
        System.out.println(" drink <source-id>     — drink local water (5 min; unsafe water can harm health)");
        System.out.println(" forage <source-id>    — gather local wild food (15 min)");
        System.out.println(" town | population [count] | population generate 12 | resident resident-0001");
        System.out.println(" home <id>             — inspect a resident's private home unit (developer view)");
        System.out.println(" where <id> | known <id> [count] | town-events [count]");
        System.out.println(" relationships [count] | relationship <id> <id> | social-events [count]");
        System.out.println(" beliefs <id> [count] | rumors [count] — attributed claims (developer view)");
        System.out.println(" secrets <id> [count] | deceptions [count] — confidential developer diagnostics");
        System.out.println(" cases | case <id> | inspect <id> | interview <resident-id> <case-id>");
        System.out.println(" check-alibi <case-id> <resident-id> | background <case-id> <resident-id>");
        System.out.println(" search <case-id> <resident-id> | return-item <case-id> — evidence custody");
        System.out.println(" accuse <case-id> <resident-id>");
        System.out.println(" finances <id> | debts | ledger [count] | ledger <id> [count] — developer finance diagnostics");
        System.out.println(" career <id> | life-events [count] | life-events <id> [count] — developer life diagnostics");
        System.out.println(" inventory <id> | item <item-id> | property-events [count] — developer property diagnostics");
        System.out.println(" wait 60               — advance 60 game minutes");
        System.out.println(" simulate 7            — simulate seven game days (up to 365)");
        System.out.println(" speed 60              — game minutes per real second in live mode (1,10,60,1440)");
        System.out.println(" live 30               — run live simulation for 30 real seconds");
        System.out.println(" offline on|off        — catch up with elapsed real time on the next launch");
        System.out.println(" talk Hello, Marco!    — face-to-face AI chat with Marco (optional paid call)");
        System.out.println(" remember My name is ... — explicitly tell nearby Marco a durable fact");
        System.out.println(" reflect               — one paid AI call to reconsider Marco's long-term priority");
        System.out.println(" help | exit");
    }
}
