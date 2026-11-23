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
        /** Snapshot captured at the moment of the incident, never reconstructed later. */
        final Map<String,String> incidentSpaces=new LinkedHashMap<>();
        /** Detective notes, not authoritative evidence. May be updated after new interviews. */
        final Map<String,String> alibiChecks=new LinkedHashMap<>();
        /** Empty for pre-v1.9 cases, whose historic scene evidence cannot be rewritten. */
        String fabricColor;
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
            c.interviews.putAll(interviews);c.incidentSpaces.putAll(incidentSpaces);
            c.alibiChecks.putAll(alibiChecks);c.fabricColor=fabricColor;
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
            c.fabricColor=coatColor(offender,clock.day);
            for(var p:present)c.witnesses.add(p.id);
            // Freeze contemporary location evidence before anybody travels away.
            for(var p:town.people.values()) {
                if(p.alive && p.travelRemaining==0)
                    c.incidentSpaces.put(p.id,p.currentSpace());
            }
            town.cases.put(id,c);
            // The event text does not identify the offender, even to co-present witnesses.
            TownLife.publish(town,clock,offender.id,place,
                    "A wallet belonging to "+victim.name+" disappeared at "+place.label+".",
                    truth,observations);
            return;
        }
    }

    private static final String[] COAT_COLORS={"blue", "brown", "olive", "charcoal"};

    /** Clothing is a reproducible daily trait, not a unique identifier or proof. */
    static String coatColor(TownLife.Citizen p,int day) {
        int index=Math.floorMod(p.id.hashCode() ^ Long.hashCode(p.personalSeed) ^ (day*97),COAT_COLORS.length);
        return COAT_COLORS[index];
    }

    static String sceneClue(TownLife.Town town,CaseFile c) {
        if(c.fabricColor==null) {
            // Pre-v1.9 evidence did not retain reliable chain of custody. Do not
            // reconstruct a clue from the secret offender ID and spoil old cases.
            return "A torn receipt was reported in v1.8, but its original marking cannot "+
                    "be verified. No new physical evidence can be inferred retrospectively.";
        }
        return "A "+c.fabricColor+" fabric thread and a torn receipt with no legible name. "+
                "The fabric color is not unique and neither item proves who took the wallet.";
    }

    static String investigate(TownLife.Town town,String caseId) {
        var c=caseById(town,caseId);
        c.inspected=true;
        return "Scene examined: "+c.place.label+". "+sceneClue(town,c);
    }

    /** Interviews are self-reports and uncertain eyewitness recollections, never engine truth. */
    static String interview(TownLife.Town town,String caseId,String residentId) {
        var c=caseById(town,caseId);
        var p=town.get(residentId);
        if(!p.alive)return p.name+" is deceased and cannot be interviewed.";
        if(c.interviews.containsKey(p.id))return "Recorded earlier: "+c.interviews.get(p.id);
        String statement;
        if(p.id.equals(c.victimId))
            statement="My wallet went missing at "+c.place.label+". I did not see anyone take it.";
        else if(c.witnesses.contains(p.id)) {
            // The culprit does not give a uniquely evasive answer: evasiveness alone
            // must not be a perfect, deterministic offender detector.
            String color=c.fabricColor==null?"dark":c.fabricColor;
            if(p.id.equals(c.offenderId) || p.carefulness<60) {
                statement="I was there. I recall people nearby, but I didn't see a theft.";
            } else {
                // Observers can misidentify someone near the victim's belongings.
                // Such a statement is a lead, NOT authoritative identification.
                var bystanders=new ArrayList<String>();
                for(var id:c.witnesses)if(!id.equals(c.victimId) && !id.equals(p.id) &&
                        !id.equals(c.offenderId))bystanders.add(id);
                int roll=Math.floorMod((p.id+":"+c.id).hashCode(),5);
                String personId=roll==0 && !bystanders.isEmpty()
                        ?bystanders.get(Math.floorMod(p.id.hashCode(),bystanders.size()))
                        :c.offenderId;
                statement="I recall a "+color+" coat and noticed "+town.get(personId).name+
                        " near the belongings. I didn't see anyone take the wallet.";
            }
        } else if(!c.incidentSpaces.isEmpty() && c.incidentSpaces.containsKey(p.id)) {
            // A resident's location claim can be corroborated independently only
            // if another interviewed person occupied the same PUBLIC space.
            String space=c.incidentSpaces.get(p.id);
            if(space.startsWith("public:")) {
                String label=TownLife.Place.valueOf(space.substring(7)).label;
                statement="I remember being at "+label+" around that time, not at "+c.place.label+".";
            } else {
                statement="I was at home then, as far as I remember. I was not at the scene.";
            }
        } else {
            // Legacy v1.8 cases lack position snapshots. Do not invent their history.
            String report="A wallet belonging to "+town.get(c.victimId).name+
                    " disappeared at "+c.place.label+".";
            String key=KnowledgeLife.eventKey(new TownLife.Event(c.day,c.minute,c.offenderId,
                    c.place,TownLife.publicSpace(c.place),report));
            var own=town.beliefs.get(p.id);
            var belief=own==null?null:own.get(key);
            if(belief!=null && belief.origin().equals("HEARD"))
                statement="I heard from "+town.get(belief.learnedFrom()).name+
                        " that a wallet was missing. I wasn't there myself.";
            else statement="I don't have a reliable account of where I was when that happened.";
        }
        // A concrete clothing description makes fabric evidence useful, but not unique.
        if(c.fabricColor!=null && c.witnesses.contains(p.id))
            statement += " I wore a "+coatColor(p,c.day)+" jacket that evening.";
        c.interviews.put(p.id,statement);
        return p.name+": \""+statement+"\"";
    }

    /** A checked alibi requires separate interviews and an independent, co-present witness.
     *  Snapshots establish the engine's reference truth; they are never printed directly.
     */
    static String checkAlibi(TownLife.Town town,String caseId,String residentId) {
        var c=caseById(town,caseId);
        var target=town.get(residentId);
        if(!c.interviews.containsKey(target.id))
            return "Interview "+target.name+" before checking their alibi.";
        if(c.incidentSpaces.isEmpty())
            return "No incident-time location snapshot exists for this pre-v1.9 case. "+
                   "An independent alibi cannot be reconstructed retrospectively.";
        String space=c.incidentSpaces.get(target.id);
        if(space==null || !space.startsWith("public:") || space.equals(TownLife.publicSpace(c.place))) {
            String result="UNVERIFIED: No independent elsewhere alibi has been established for "+
                          target.name+". This is not proof of guilt.";
            c.alibiChecks.put(target.id,result);
            return result;
        }
        String confirmedBy=null;
        for(var e:c.incidentSpaces.entrySet()) {
            if(e.getKey().equals(target.id) || !space.equals(e.getValue()))continue;
            if(c.interviews.containsKey(e.getKey())) {confirmedBy=e.getKey();break;}
        }
        String result;
        if(confirmedBy==null) {
            result="UNVERIFIED: "+target.name+" reports being elsewhere, but no interviewed independent "+
                   "person corroborates it. Interview more residents.";
        } else {
            String label=TownLife.Place.valueOf(space.substring(7)).label;
            result="CORROBORATED (follow-up): "+town.get(confirmedBy).name+
                   " confirms seeing "+target.name+" at "+label+
                   " around the incident time. This supports an alibi but is not judicial proof.";
        }
        c.alibiChecks.put(target.id,result);
        return result;
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
