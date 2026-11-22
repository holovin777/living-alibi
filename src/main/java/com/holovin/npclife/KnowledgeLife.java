package com.holovin.npclife;

import java.util.LinkedHashMap;
import java.util.Map;

/** Epistemic layer: what a resident observed is not what another resident heard. */
final class KnowledgeLife {
    private KnowledgeLife() { }

    /** Event identity is derived only from the authoritative event, never from the rumor. */
    record Belief(String residentId, String eventKey, int eventDay, int eventMinute,
                  String actorId, String sceneId, String statement,
                  String origin, String learnedFrom, int learnedDay, int learnedMinute,
                  int hops, int confidence) { }

    record Transfer(int day, int minute, String fromId, String toId,
                    String eventKey, int confidence, int hops, String statement) { }

    /** Avoid filling every resident's memory with hourly wages, drinking and commuting. */
    static boolean notable(String statement) {
        String s=statement.toLowerCase(java.util.Locale.ROOT);
        return s.contains("crossed paths") || s.contains("argument") ||
               s.contains("tense exchange") || s.contains("reconciled") ||
               s.contains("died") || s.contains("disappeared") ||
               s.contains("stolen") || s.contains("stole ") ||
               s.contains("opened his own bakery");
    }

    static String eventKey(TownLife.Event event) {
        String raw=event.day()+"|"+event.minute()+"|"+event.actorId()+"|"+
                   event.spaceId()+"|"+event.text();
        long value=0xcbf29ce484222325L;
        for(int i=0;i<raw.length();i++) {
            value^=raw.charAt(i);
            value*=0x100000001b3L;
        }
        return "ev-"+event.day()+"-"+event.minute()+"-"+Long.toUnsignedString(value,16);
    }

    static void witnessed(TownLife.Town town,TownLife.Event event,String residentId) {
        if(!notable(event.text()))return;
        var person=town.people.get(residentId);
        if(person==null || !person.alive)return;
        String key=eventKey(event);
        var own=town.beliefs.computeIfAbsent(residentId,ignored->new LinkedHashMap<>());
        // Direct observation always outranks hearing somebody's account.
        own.put(key,new Belief(residentId,key,event.day(),event.minute(),event.actorId(),
                event.spaceId(),event.text(),"DIRECT",residentId,
                event.day(),event.minute(),0,100));
        DeceptionLife.onWitness(town,event,residentId);
    }

    /** Only an actual conversation between co-present, awake, living residents can relay a claim. */
    static void exchange(TownLife.Town town,WorldState clock,String firstId,String secondId) {
        TownLife.Citizen first=town.get(firstId),second=town.get(secondId);
        if(first.id.equals(second.id) || !first.alive || !second.alive ||
                first.travelRemaining>0 || second.travelRemaining>0 ||
                first.activity.equals("SLEEPING") || second.activity.equals("SLEEPING") ||
                !first.currentSpace().equals(second.currentSpace()))return;
        if(!share(town,clock,first,second))share(town,clock,second,first);
    }

    private static boolean share(TownLife.Town town,WorldState clock,
                                 TownLife.Citizen speaker,TownLife.Citizen listener) {
        Map<String,Belief> source=town.beliefs.get(speaker.id);
        if(source==null || source.isEmpty())return false;
        Map<String,Belief> receiver=town.beliefs.computeIfAbsent(listener.id,
                ignored->new LinkedHashMap<>());
        long current=(long)(clock.day-1)*1440+clock.minute;
        Belief chosen=null;
        // Choose the freshest useful claim, never rewrite it as a first-hand observation.
        for(Belief claim:source.values()) {
            long age=current-((long)(claim.eventDay()-1)*1440+claim.eventMinute());
            if(age<0 || age>14L*1440 || claim.hops()>=3 ||
                    receiver.containsKey(claim.eventKey()))continue;
            if(chosen==null || claim.eventDay()>chosen.eventDay() ||
                    (claim.eventDay()==chosen.eventDay() && claim.eventMinute()>chosen.eventMinute()))
                chosen=claim;
        }
        if(chosen==null)return false;
        var relation=town.existingRelationship(speaker.id,listener.id);
        int trust=relation==null?45:relation.trust;
        DeceptionLife.Mode mode=DeceptionLife.mode(town,speaker,listener,chosen.eventKey(),trust);
        if(mode==DeceptionLife.Mode.WITHHELD) {
            DeceptionLife.log(town,clock,speaker.id,listener.id,chosen.eventKey(),mode,"[withheld]");
            return false;
        }
        String claimed=mode==DeceptionLife.Mode.LIE
                ?DeceptionLife.falseStatement(chosen.statement()):chosen.statement();
        DeceptionLife.log(town,clock,speaker.id,listener.id,chosen.eventKey(),mode,claimed);
        int confidence=Math.max(20,Math.min(90,chosen.confidence()-12-(100-trust)/8));
        int hops=chosen.hops()+1;
        receiver.put(chosen.eventKey(),new Belief(listener.id,chosen.eventKey(),
                chosen.eventDay(),chosen.eventMinute(),chosen.actorId(),chosen.sceneId(),
                claimed,"HEARD",speaker.id,clock.day,clock.minute,hops,confidence));
        town.pendingTransfers.add(new Transfer(clock.day,clock.minute,speaker.id,
                listener.id,chosen.eventKey(),confidence,hops,claimed));
        return true;
    }
}
