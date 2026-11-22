package com.holovin.npclife;

import java.util.List;

/** Rule-based, local, deterministic social interactions; no language-model calls. */
final class SocialLife {
    private SocialLife() { }

    enum Kind { FRIENDLY_CHAT, TENSE_EXCHANGE, RECONCILIATION, SMALL_TALK }

    record Interaction(int day, int minute, String firstId, String secondId,
                       TownLife.Place place, Kind kind, String description) { }

    /** A relationship is directed neither way for now; future residents will use one per pair. */
    static final class Relationship {
        final String firstId;
        final String secondId;
        int affinity=50;      // 0: hostile, 100: close
        int trust=45;         // 0: distrustful, 100: trusting
        int tension=8;        // 0: calm, 100: tense
        int encounters;
        int conversations;
        int arguments;
        long lastInteractionTick=-1;
        long rng=0x6A09E667F3BCC909L;

        Relationship(String firstId,String secondId) {
            this.firstId=firstId;this.secondId=secondId;
        }
        Relationship copy() {
            Relationship r=new Relationship(firstId,secondId);
            r.affinity=affinity;r.trust=trust;r.tension=tension;
            r.encounters=encounters;r.conversations=conversations;r.arguments=arguments;
            r.lastInteractionTick=lastInteractionTick;r.rng=rng;
            return r;
        }
        String label() {
            if(encounters==0)return "Strangers";
            if(tension>=58 || affinity<27)return "Strained";
            if(affinity>=82 && trust>=75 && encounters>=5)return "Close friends";
            if(affinity>=65 && trust>=58 && encounters>=3)return "Friends";
            return "Acquaintances";
        }
        int nextInt(int bound) {
            // Persisted state allows exactly the same sequence after restarting the app.
            rng ^= rng << 13;
            rng ^= rng >>> 7;
            rng ^= rng << 17;
            return (int)Long.remainderUnsigned(rng,bound);
        }
    }

    static void onEncounter(TownLife.Town town) {
        town.relationship.encounters++;
    }

    static void tick(TownLife.Town town,WorldState marco,List<TownLife.Event> events,
                     List<TownLife.Observation> observations,List<Interaction> interactions) {
        if(!town.together || town.meetingSinceTick<0)return;
        TownLife.Citizen m=town.get("marco"),e=town.get("elena");
        if(!m.alive||!e.alive||!m.presentAt(e.place))return;
        if(m.activity.equals("SLEEPING")||e.activity.equals("SLEEPING"))return;
        long now=(long)(marco.day-1)*1440L+marco.minute;
        if(now-town.meetingSinceTick<10)return;
        if(town.relationship.lastInteractionTick>=town.meetingSinceTick)return; // one per encounter
        // Only retry every 15 minutes. Long conversations do not flood the journal.
        if(now%15!=0)return;
        Relationship r=town.relationship;
        int sociability=(marco.personalitySociable + e.sociability)/2;
        int willingness=clamp(sociability+20 - r.tension/2 - (marco.fatigue+e.fatigue)/12);
        if(r.nextInt(100)>=willingness)return;

        int pressure=(marco.stress + marco.hunger/2 + e.hunger/2 + e.fatigue/2)/3;
        boolean argue=(pressure+r.tension/2)>72 && r.nextInt(100)<55;
        boolean reconcile=!argue && r.tension>=32 && r.nextInt(100)<50;
        Kind kind;
        String description;
        if(argue) {
            kind=Kind.TENSE_EXCHANGE;
            description="Marco Bianchi and Elena Rossi had a tense exchange at "+m.place.label+".";
            r.affinity=clamp(r.affinity-5);
            r.trust=clamp(r.trust-4);
            r.tension=clamp(r.tension+18);
            r.arguments++;
            marco.stress=clamp(marco.stress+4);
            marco.mood=clamp(marco.mood-5);
        } else if(reconcile) {
            kind=Kind.RECONCILIATION;
            description="Marco Bianchi and Elena Rossi cleared up a disagreement at "+m.place.label+".";
            r.affinity=clamp(r.affinity+3);
            r.trust=clamp(r.trust+5);
            r.tension=clamp(r.tension-22);
            marco.stress=clamp(marco.stress-3);
        } else if(r.encounters<3) {
            kind=Kind.SMALL_TALK;
            description="Marco Bianchi and Elena Rossi made small talk at "+m.place.label+".";
            r.affinity=clamp(r.affinity+2);
            r.trust=clamp(r.trust+1);
            r.tension=clamp(r.tension-2);
        } else {
            kind=Kind.FRIENDLY_CHAT;
            String[] subjects={"their working day","the bakery and the market","their plans for the evening",
                    "life in the neighborhood","their favorite places in town"};
            String subject=subjects[r.nextInt(subjects.length)];
            description="Marco Bianchi and Elena Rossi talked about "+subject+" at "+m.place.label+".";
            r.affinity=clamp(r.affinity+3);
            r.trust=clamp(r.trust+2);
            r.tension=clamp(r.tension-3);
        }
        r.conversations++;
        r.lastInteractionTick=now;
        if(kind!=Kind.TENSE_EXCHANGE) {
            marco.loneliness=clamp(marco.loneliness-6);
            marco.mood=clamp(marco.mood+2);
        }
        interactions.add(new Interaction(marco.day,marco.minute,m.id,e.id,m.place,kind,description));
        TownLife.publish(town,marco,m.id,m.place,description,events,observations);
    }

    private static int clamp(int n) {return Math.max(0,Math.min(100,n));}
}
