package com.holovin.npclife;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** First playable investigation. World truth is not detective-visible evidence. */
final class InvestigationLife {
    private InvestigationLife() { }

    static final class CaseFile {
        final String id;
        final int day, minute;
        final TownLife.Place place;
        final String victimId, offenderId, item;
        final int amount;
        final Set<String> witnesses=new LinkedHashSet<>();
        final Map<String,String> interviews=new LinkedHashMap<>();
        boolean inspected, solved;

        CaseFile(String id,int day,int minute,TownLife.Place place,
                 String victimId,String offenderId,String item,int amount) {
            this.id=id;this.day=day;this.minute=minute;this.place=place;
            this.victimId=victimId;this.offenderId=offenderId;
            this.item=item;this.amount=amount;
        }
        CaseFile copy() {
            var c=new CaseFile(id,day,minute,place,victimId,offenderId,item,amount);
            c.inspected=inspected;c.solved=solved;c.witnesses.addAll(witnesses);
            c.interviews.putAll(interviews);
            return c;
        }
        String publicSummary(TownLife.Town town) {
            return id+" | Day "+day+" "+clock(minute)+" | "+place.label+" | "+
                   town.get(victimId).name+" reported a missing "+item+" | "+
                   (solved?"SOLVED":"OPEN");
        }
    }

    static void tick(TownLife.Town town,WorldState clock,List<TownLife.Event> truth,
                     List<TownLife.Observation> observations) {
        // Incident opportunities are assessed on one evening per week for the console MVP.
        // Participants and outcomes depend on the current living population and its positions.
        if(clock.day%7!=0 || clock.minute!=1050 || town.people.size()<3)return;
        // Cap at one new incident per week. The case IDs persist across restarts.
        for(var c:town.cases.values())if(c.day==clock.day)return;
        for(var place:List.of(TownLife.Place.PARK,TownLife.Place.CAFE,TownLife.Place.MARKET)) {
            var present=new ArrayList<TownLife.Citizen>();
            for(var p:town.people.values()) {
                if(p.presentAt(place) && p.currentSpace().equals(TownLife.publicSpace(place)) &&
                        !p.activity.equals("SLEEPING") && p.money>=0)present.add(p);
            }
            if(present.size()<3)continue;
            // No fixed suspect or victim: choose from real co-present people and persistent traits.
            present.sort(Comparator.comparingInt(p -> p.carefulness*2+p.money/30 +
                    Math.floorMod((p.personalSeed ^ (clock.day*17L)),19)));
            TownLife.Citizen offender=present.getFirst();
            TownLife.Citizen victim=null;
            for(var p:present) {
                if(p.id.equals(offender.id) || p.money<25)continue;
                if(victim==null || p.money>victim.money)victim=p;
            }
            if(victim==null)continue;
            int amount=15;
            victim.money-=amount;offender.money+=amount;
            // Marco's authoritative physiology ledger is separate from his spatial citizen snapshot.
            if(victim.id.equals("marco"))clock.money-=amount;
            if(offender.id.equals("marco"))clock.money+=amount;
            String id="case-%04d".formatted(town.cases.size()+1);
            var c=new CaseFile(id,clock.day,clock.minute,place,victim.id,offender.id,"wallet",amount);
            for(var p:present)c.witnesses.add(p.id);
            town.cases.put(id,c);
            // The event text does not identify the offender, even to co-present witnesses.
            TownLife.publish(town,clock,offender.id,place,
                    "A wallet belonging to "+victim.name+" disappeared at "+place.label+".",
                    truth,observations);
            return;
        }
    }

    static String investigate(TownLife.Town town,String caseId) {
        var c=caseById(town,caseId);
        c.inspected=true;
        var offender=town.get(c.offenderId);
        String initials=initials(offender.name);
        return "Scene examined: "+c.place.label+". A torn receipt was found bearing initials '"+
                initials+"'. This is a lead, not proof of ownership or guilt.";
    }

    static String interview(TownLife.Town town,String caseId,String residentId) {
        var c=caseById(town,caseId);
        var p=town.get(residentId);
        if(!p.alive)return p.name+" is deceased and cannot be interviewed.";
        if(c.interviews.containsKey(p.id))return "Recorded earlier: "+c.interviews.get(p.id);
        String statement;
        if(p.id.equals(c.victimId))
            statement="My wallet went missing at "+c.place.label+". I did not see anyone take it.";
        else if(p.id.equals(c.offenderId))
            statement="I was nearby that evening. I'd rather not discuss the missing wallet.";
        else if(c.witnesses.contains(p.id)) {
            if(p.carefulness>=60)statement="I noticed "+town.get(c.offenderId).name+
                    " near the belongings that evening, but I didn't see a theft.";
            else statement="I was there, but I only heard that a wallet was missing.";
        } else {
            // A non-witness can know about the incident only through an attributed belief.
            String report="A wallet belonging to "+town.get(c.victimId).name+
                    " disappeared at "+c.place.label+".";
            String key=KnowledgeLife.eventKey(new TownLife.Event(c.day,c.minute,c.offenderId,
                    c.place,TownLife.publicSpace(c.place),report));
            var own=town.beliefs.get(p.id);
            var belief=own==null?null:own.get(key);
            if(belief!=null && belief.origin().equals("HEARD"))
                statement="I heard from "+town.get(belief.learnedFrom()).name+
                        " that a wallet was missing. I wasn't there myself.";
            else statement="I was not at "+c.place.label+" when that happened.";
        }
        // This is attributed testimony, not a change to the world's authoritative history.
        c.interviews.put(p.id,statement);
        return p.name+": \""+statement+"\"";
    }

    static String accuse(TownLife.Town town,String caseId,String residentId) {
        var c=caseById(town,caseId);
        var accused=town.get(residentId);
        if(c.solved)return "Case already closed.";
        if(!c.inspected || c.interviews.size()<2)
            return "Insufficient investigation: inspect the scene and interview at least two people first.";
        if(accused.id.equals(c.offenderId)) {
            c.solved=true;
            return "Case solved: the simulation confirmed your accusation against "+accused.name+
                    ". (Prototype resolution; legal evidence standards are not simulated.)";
        }
        return "Accusation against "+accused.name+" was not confirmed. Case remains open.";
    }

    static CaseFile caseById(TownLife.Town town,String id) {
        var c=town.cases.get(id.toLowerCase(Locale.ROOT));
        if(c==null)throw new IllegalArgumentException("Unknown case: "+id+". Use cases to list IDs.");
        return c;
    }
    static String initials(String fullName) {
        var parts=fullName.trim().split("\\s+");
        return (""+parts[0].charAt(0)+"."+parts[parts.length-1].charAt(0)+".").toUpperCase(Locale.ROOT);
    }
    private static String clock(int minute) {return "%02d:%02d".formatted(minute/60,minute%60);}
}
