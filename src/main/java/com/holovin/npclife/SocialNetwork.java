package com.holovin.npclife;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pairwise social life for all residents beyond the legacy Marco/Elena prototype. */
final class SocialNetwork {
    private SocialNetwork() { }
    static String key(String first,String second) {
        return first.compareTo(second)<=0?first+"|"+second:second+"|"+first;
    }
    private static int clamp(int n){return Math.max(0,Math.min(100,n));}
    private static long seed(String key) {
        long v=0xcbf29ce484222325L;
        for(int i=0;i<key.length();i++){v^=key.charAt(i);v*=0x100000001b3L;}
        return v==0?1:v;
    }

    static void tick(TownLife.Town town,WorldState clock,
                     List<TownLife.Event> truth,List<TownLife.Observation> witnesses,
                     List<SocialLife.Interaction> social) {
        // Group by exact scene, not Place: neighboring apartments are different scenes.
        Map<String,List<TownLife.Citizen>> byScene=new LinkedHashMap<>();
        for(var citizen:town.people.values()) {
            if(!citizen.alive || citizen.travelRemaining>0 ||
                    citizen.activity.equals("SLEEPING") || citizen.activity.equals("TRAVELING"))continue;
            byScene.computeIfAbsent(citizen.currentSpace(),ignored->new ArrayList<>()).add(citizen);
        }
        long now=(long)(clock.day-1)*1440L+clock.minute;
        for(var group:byScene.values()) {
            for(int i=0;i<group.size();i++)for(int j=i+1;j<group.size();j++) {
                var first=group.get(i);var second=group.get(j);
                if((first.id.equals("marco")&&second.id.equals("elena")) ||
                   (first.id.equals("elena")&&second.id.equals("marco")))continue;
                String k=key(first.id,second.id);
                var relation=town.peerRelationships.get(k);
                if(relation==null) {
                    String a=first.id.compareTo(second.id)<=0?first.id:second.id;
                    String b=a.equals(first.id)?second.id:first.id;
                    relation=new SocialLife.Relationship(a,b);
                    relation.rng=seed(k);
                    town.peerRelationships.put(k,relation);
                }
                if(relation.lastSeenTick!=now-1) {
                    relation.encounters++;
                    relation.meetingSinceTick=now;
                }
                relation.lastSeenTick=now;
                // One conversation per encounter and at most one per pair per game day.
                if(now-relation.meetingSinceTick<10 || clock.minute%15!=0 ||
                        relation.lastInteractionTick>=relation.meetingSinceTick ||
                        (relation.lastInteractionTick>=0 && relation.lastInteractionTick/1440==now/1440))continue;
                if(relation.lastAttemptDay==clock.day)continue;
                relation.lastAttemptDay=clock.day;
                // A single approach opportunity per pair per game day. This keeps a
                // small town from turning into hundreds of conversations per day.
                int chance=clamp(7+(first.sociability+second.sociability)/10-
                        (first.fatigue+second.fatigue)/15-relation.tension/5);
                if(relation.nextInt(100)>=chance)continue;
                int pressure=(first.hunger+second.hunger+first.thirst+second.thirst)/4+
                        relation.tension/2;
                boolean argue=pressure>85 && relation.nextInt(100)<35;
                boolean reconcile=!argue && relation.tension>35 && relation.nextInt(100)<45;
                SocialLife.Kind kind;
                String description;
                String names=first.name+" and "+second.name;
                if(argue) {
                    kind=SocialLife.Kind.TENSE_EXCHANGE;
                    relation.affinity=clamp(relation.affinity-6);
                    relation.trust=clamp(relation.trust-4);
                    relation.tension=clamp(relation.tension+17);
                    relation.arguments++;
                    description=names+" had an argument at "+first.place.label+".";
                } else if(reconcile) {
                    kind=SocialLife.Kind.RECONCILIATION;
                    relation.affinity=clamp(relation.affinity+3);
                    relation.trust=clamp(relation.trust+4);
                    relation.tension=clamp(relation.tension-18);
                    description=names+" reconciled at "+first.place.label+".";
                } else if(relation.conversations<2) {
                    kind=SocialLife.Kind.SMALL_TALK;
                    relation.affinity=clamp(relation.affinity+2);
                    relation.trust=clamp(relation.trust+1);
                    relation.tension=clamp(relation.tension-1);
                    description=names+" exchanged a few words at "+first.place.label+".";
                } else {
                    kind=SocialLife.Kind.FRIENDLY_CHAT;
                    relation.affinity=clamp(relation.affinity+2);
                    relation.trust=clamp(relation.trust+2);
                    relation.tension=clamp(relation.tension-2);
                    String[] topics={"their neighborhood","work","recent town life","their plans"};
                    description=names+" chatted about "+topics[relation.nextInt(topics.length)]+
                            " at "+first.place.label+".";
                }
                relation.lastInteractionTick=now;
                relation.conversations++;
                social.add(new SocialLife.Interaction(clock.day,clock.minute,
                        relation.firstId,relation.secondId,first.place,kind,description));
                TownLife.publish(town,clock,first.id,first.place,description,truth,witnesses);
                // Social contact also benefits Marco without any AI calls.
                if(first.id.equals("marco") || second.id.equals("marco")) {
                    clock.loneliness=clamp(clock.loneliness-5);
                    clock.mood=clamp(clock.mood+1);
                }
            }
        }
    }
}
