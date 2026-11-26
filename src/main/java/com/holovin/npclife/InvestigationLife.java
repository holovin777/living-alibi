package com.holovin.npclife;

import java.util.ArrayList;
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
        /** Search outcomes are retained to prevent unlimited repeated searches. */
        final Map<String,String> searches=new LinkedHashMap<>();
        /** Empty for pre-v1.9 cases, whose historic scene evidence cannot be rewritten. */
        String fabricColor;
        /** Only post-v1.13 incidents link to a real, movable world item. */
        String physicalItemId;
        /** Private engine reason. Legacy cases do not acquire fabricated motives. */
        String motiveCode;
        int motiveStrength;
        /** Contemporary circumstances for every resident, not an accusation. */
        final Map<String,MotiveLife.Indicators> incidentProfiles=new LinkedHashMap<>();
        boolean inspected, solved, returned;

        CaseFile(String id,int day,int minute,TownLife.Place place,
                 String victimId,String offenderId,String item,int amount) {
            this.id=id;this.day=day;this.minute=minute;this.place=place;
            this.victimId=victimId;this.offenderId=offenderId;
            this.item=item;this.amount=amount;
        }
        CaseFile copy() {
            var c=new CaseFile(id,day,minute,place,victimId,offenderId,item,amount);
            c.inspected=inspected;c.solved=solved;c.returned=returned;c.witnesses.addAll(witnesses);
            c.interviews.putAll(interviews);c.incidentSpaces.putAll(incidentSpaces);
            c.alibiChecks.putAll(alibiChecks);c.searches.putAll(searches);c.fabricColor=fabricColor;
            c.physicalItemId=physicalItemId;
            c.motiveCode=motiveCode;c.motiveStrength=motiveStrength;
            c.incidentProfiles.putAll(incidentProfiles);
            return c;
        }
        String publicSummary(TownLife.Town town) {
            return id+" | Day "+day+" "+clock(minute)+" | "+place.label+" | "+
                   town.get(victimId).name+" reported a missing "+item+" | "+
                   (solved?"SOLVED":"OPEN");
        }
    }

    /** Check real opportunities at irregular incident times, not on a fixed weekday. */
    static void tick(TownLife.Town town,WorldState clock,List<TownLife.Event> truth,
                     List<TownLife.Observation> observations) {
        // A bounded number of decision windows, not an expensive per-minute N^2 search.
        if(clock.minute<660 || clock.minute>1170 || clock.minute%13!=0 ||
                town.people.size()<3)return;
        // Avoid floods; no more than one incident within the preceding three days.
        for(var incident:town.cases.values()) {
            long age=((long)clock.day-incident.day)*1440L+clock.minute-incident.minute;
            if(age>=0 && age<3L*1440L)return;
        }
        TownLife.Citizen chosen=null, target=null;
        MotiveLife.Assessment winning=null;
        TownLife.Place scene=null;
        int best=-1;
        long now=((long)clock.day-1)*1440L+clock.minute;
        for(var place:List.of(TownLife.Place.PARK,TownLife.Place.CAFE,
                              TownLife.Place.MARKET,TownLife.Place.LIBRARY)) {
            var people=new ArrayList<TownLife.Citizen>();
            for(var person:town.people.values())
                if(!DemographyLife.child(person)&&person.presentAt(place) && person.currentSpace().equals(TownLife.publicSpace(place)) &&
                        !person.activity.equals("SLEEPING") && !person.activity.equals("TRAVELING"))
                    people.add(person);
            if(people.size()<3)continue;
            for(var offender:people) {
                // One impulsive character cannot repeatedly generate every crime.
                boolean recentlyOffended=false;
                for(var c:town.cases.values())
                    if(c.offenderId.equals(offender.id) && clock.day-c.day<10)
                        {recentlyOffended=true;break;}
                if(recentlyOffended)continue;
                for(var victim:people) {
                    if(victim==offender || victim.money<30 || PropertyLife.carriedWallet(town,victim)==null ||
                            recentlyVictimized(town,clock,victim.id,14))continue;
                    var assessment=MafiaLife.assess(town,offender,MotiveLife.assess(town,clock,offender,victim));
                    if(assessment.strength()<59)continue;
                    int jitter=(int)Math.floorMod(mix(now^offender.id.hashCode()^
                            Long.rotateLeft(victim.id.hashCode(),17)),13);
                    int rank=assessment.strength()+jitter;
                    if(rank>best) {
                        best=rank;chosen=offender;target=victim;
                        winning=assessment;scene=place;
                    }
                }
            }
        }
        if(chosen==null)return;
        // A strong temptation increases risk but never guarantees a crime.
        long entropy=mix(now^chosen.id.hashCode()^
                Long.rotateLeft(target.id.hashCode(),31)^chosen.personalSeed);
        int chance=Math.min(95,Math.max(8,winning.strength()-38)); // rare per-window chance; motivation changes probability
        if(Math.floorMod(entropy,1000)>=chance)return;
        recordIncident(town,clock,scene,chosen,target,winning,truth,observations);
    }

    /** Trauma/caution cooldown applies to future victim selection, not old case data. */
    static boolean recentlyVictimized(TownLife.Town town,WorldState clock,String victimId,int days) {
        long now=((long)clock.day-1)*1440L+clock.minute;
        for(var c:town.cases.values()) {
            long then=((long)c.day-1)*1440L+c.minute;
            if(c.victimId.equals(victimId) && then<=now && now-then<(long)days*1440L)
                return true;
        }
        return false;
    }

    private static long mix(long n) {
        n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;
        n=(n^(n>>>27))*0x94d049bb133111ebL;
        return n^(n>>>31);
    }

    /** Shared by simulation and deterministic regression fixtures; always checks scene truth. */
    static CaseFile recordIncident(TownLife.Town town,WorldState clock,TownLife.Place place,
                                   TownLife.Citizen offender,TownLife.Citizen victim,
                                   MotiveLife.Assessment motive,List<TownLife.Event> truth,
                                   List<TownLife.Observation> observations) {
        PropertyLife.ensureStarterItems(town,clock);
        if(offender==victim || DemographyLife.child(offender)||DemographyLife.child(victim)|| !offender.presentAt(place) || !victim.presentAt(place) ||
                !offender.currentSpace().equals(TownLife.publicSpace(place)) ||
                !victim.currentSpace().equals(TownLife.publicSpace(place)) || victim.money<15 ||
                PropertyLife.carriedWallet(town,victim)==null)
            throw new IllegalArgumentException("An incident needs two co-present people and a solvent victim.");
        for(var prior:town.cases.values())
            if(prior.day==clock.day && prior.minute==clock.minute)
                throw new IllegalArgumentException("An incident already exists for this instant.");
        int amount=15;
        String id="case-%04d".formatted(town.cases.size()+1);
        var c=new CaseFile(id,clock.day,clock.minute,place,victim.id,offender.id,"wallet",amount);
        c.fabricColor=coatColor(offender,clock.day);
        c.motiveCode=motive.reason();c.motiveStrength=motive.strength();
        for(var p:town.people.values()) {
            if(!p.alive)continue;
            // A travelling resident retains a background, but has no confirmed scene.
            if(p.travelRemaining==0)c.incidentSpaces.put(p.id,p.currentSpace());
            c.incidentProfiles.put(p.id,MotiveLife.indicators(town,clock,p,victim));
            if(p.presentAt(place) && p.currentSpace().equals(TownLife.publicSpace(place)))
                c.witnesses.add(p.id);
        }
        // Freeze motives BEFORE changing balances: post-theft wealth would rewrite context.
        // This wallet remains legally owned by the victim but is now carried by the offender.
        c.physicalItemId=PropertyLife.stealWallet(town,clock,offender,victim);
        victim.money-=amount;offender.money+=amount;
        if(victim.id.equals("marco"))clock.money-=amount;
        if(offender.id.equals("marco"))clock.money+=amount;
        town.cases.put(c.id,c);
        MafiaLife.noteCase(town,c);
        // Public report never includes offender, motive, or protected profiles.
        TownLife.publish(town,clock,offender.id,place,
                "A wallet belonging to "+victim.name+" disappeared at "+place.label+".",
                truth,observations);
        return c;
    }

    /** Backward-compatible test helper for manually staged, known-location incidents. */
    static CaseFile recordIncident(TownLife.Town town,WorldState clock,TownLife.Place place,
                                   TownLife.Citizen offender,TownLife.Citizen victim,
                                   List<TownLife.Event> truth,List<TownLife.Observation> observations) {
        return recordIncident(town,clock,place,offender,victim,
                MotiveLife.assess(town,clock,offender,victim),truth,observations);
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

    /** A detective-accessible lead, gated by an actual interview; not a guilt score. */
    static String background(TownLife.Town town,String caseId,String residentId) {
        var c=caseById(town,caseId);
        var person=town.get(residentId);
        if(!c.inspected)return "Inspect the scene before researching a resident's background.";
        if(!c.interviews.containsKey(person.id))
            return "Interview "+person.name+" before reviewing their circumstances.";
        var data=c.incidentProfiles.get(person.id);
        if(data==null)
            return "No contemporaneous background snapshot is available for this older case. "+
                   "Do not infer past circumstances from today's information.";
        return person.name+" | Incident-time leads | "+data.publicSummary();
    }

    /** Player-facing, evidence-gated suspect search. No global inventory is exposed. */
    static String search(TownLife.Town town,WorldState clock,String caseId,String residentId) {
        var c=caseById(town,caseId);
        var target=town.get(residentId);
        if(c.physicalItemId==null)
            return "This historical case has no traceable physical item. Do not invent past evidence.";
        var item=town.items.get(c.physicalItemId);
        if(item==null)return "The physical item record is unavailable.";
        if(("evidence:"+c.id).equals(item.storedSpace))
            return "The item is already held in the evidence room.";
        if(!item.stolen)return "The reported item is no longer missing.";
        if(!c.inspected || c.interviews.size()<2 || !c.interviews.containsKey(target.id))
            return "Inspect the scene and interview at least two people, including this resident, before searching.";
        if(c.searches.containsKey(target.id))return "Previous search: "+c.searches.get(target.id);
        if(c.searches.size()>=3)return "No more searches are authorized in this investigation (limit: 3).";
        boolean namedByWitness=false;
        for(var entry:c.interviews.entrySet()) {
            if(entry.getKey().equals(target.id) || !c.witnesses.contains(entry.getKey()))continue;
            if(entry.getValue().contains(target.name)) {namedByWitness=true;break;}
        }
        boolean matchesFabric=c.witnesses.contains(target.id) &&
                c.fabricColor!=null && c.fabricColor.equals(coatColor(target,c.day));
        if(!namedByWitness && !matchesFabric)
            return "Insufficient grounds: need a named witness lead or matching scene clothing for this person.";
        String result;
        if(PropertyLife.seizeForCase(town,clock,c,target.id)) {
            result="MATCH: a wallet bearing the recorded item identifier was found and secured in the evidence room. " +
                    "This links custody to the suspect, but case resolution remains separate.";
        } else {
            result="NO MATCH: no wallet matching this case's recorded item was found during the search. " +
                    "This does not prove innocence.";
        }
        c.searches.put(target.id,result);
        return result;
    }

    /** Release the physical item only after a solved case and an actual seizure. */
    static String returnItem(TownLife.Town town,WorldState clock,String caseId) {
        var c=caseById(town,caseId);
        if(c.physicalItemId==null)
            return "This old case has no documented physical item to return.";
        if(c.returned)return "This case's item has already been returned to its legal owner.";
        if(!c.solved)return "The case must be solved before evidence can be released to its owner.";
        if(PropertyLife.returnFromEvidence(town,clock,c)) {
            c.returned=true;
            return "The recovered wallet was returned to "+town.get(c.victimId).name+". Chain of custody recorded.";
        }
        return "The item is not available in this case's evidence room. Recover it first, or it has already been returned.";
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
