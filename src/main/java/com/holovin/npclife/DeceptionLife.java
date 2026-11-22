package com.holovin.npclife;

import java.util.LinkedHashMap;
import java.util.Locale;

/** Private motives and deliberate false testimony; never edits authoritative world events. */
final class DeceptionLife {
    private DeceptionLife() { }

    record Secret(String keeperId, String eventKey, int day, int minute, String reason) { }
    record Decision(int day, int minute, String fromId, String toId, String eventKey,
                    String kind, String statement) { }
    enum Mode { TRUTH, WITHHELD, LIE }

    /** Certain sensitive first-hand experiences become private to a participant. */
    static void onWitness(TownLife.Town town, TownLife.Event event, String witnessId) {
        var witness = town.people.get(witnessId);
        if(witness==null || !witness.alive) return;
        String statement=event.text().toLowerCase(Locale.ROOT);
        boolean sensitive=statement.contains("argument") || statement.contains("tense exchange") ||
                statement.contains("died") || statement.contains("stole ") || statement.contains("stolen") ||
                statement.contains("crossed paths");
        boolean involved=witness.id.equals(event.actorId()) ||
                statement.contains(witness.name.toLowerCase(Locale.ROOT));
        if(!sensitive || !involved) return;
        String eventKey=KnowledgeLife.eventKey(event);
        // Reproducible across runs. No model calls, wall-clock randomness or arbitrary plot scripts.
        int index=Math.floorMod((eventKey+":"+witnessId).hashCode(),7);
        if(index!=0) return;
        mark(town,witnessId,eventKey,event.day(),event.minute(),"PERSONAL_INCIDENT");
    }

    static void mark(TownLife.Town town,String keeperId,String eventKey,int day,int minute,String reason) {
        town.secrets.computeIfAbsent(keeperId,ignored->new LinkedHashMap<>())
                .putIfAbsent(eventKey,new Secret(keeperId,eventKey,day,minute,reason));
    }

    static Mode mode(TownLife.Town town,TownLife.Citizen speaker,
                     TownLife.Citizen listener,String eventKey,int trust) {
        var own=town.secrets.get(speaker.id);
        if(own==null || !own.containsKey(eventKey))return Mode.TRUTH;
        if(trust>=75)return Mode.TRUTH;
        if(speaker.carefulness>=65 && trust<=50)return Mode.LIE;
        return Mode.WITHHELD;
    }

    /** Explicit negation of a true proposition; the listener is not told it is a lie. */
    static String falseStatement(String original) {
        if(original.contains(" crossed paths at "))
            return original.replace(" crossed paths at "," did not cross paths at ");
        return "It is not true that " + original;
    }

    static void log(TownLife.Town town,WorldState clock,String from,String to,
                    String eventKey,Mode mode,String statement) {
        if(mode==Mode.TRUTH)return;
        town.pendingDecisions.add(new Decision(clock.day,clock.minute,from,to,
                eventKey,mode.name(),statement));
    }
}
