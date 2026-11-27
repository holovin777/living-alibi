package com.holovin.npclife;

import java.util.*;

/** Significant personal experiences, distinct from fallible hearsay.
 * Reuses the durable belief store. A life key is never an exposed private name.
 * Limit repeated events to one episode per kind, witness and 30-day window.
 */
final class LifetimeMemoryLife {
    private LifetimeMemoryLife(){}
    static final String PREFIX="life-memory:";
    static String actor(TownLife.Town t){
        var c=RebirthLife.child(t);
        return c==null?"visitor-"+t.player.incarnation:"life-resident:"+c.id;
    }
    static boolean relevant(SocialMemoryLife.Kind kind){
        return switch(kind){case SUPPORT,HURT,BROKEN_TRUST,APOLOGY,RECEIVED_AID->true;default->false;};
    }
    static void remember(TownLife.Town t,String id,SocialMemoryLife.Kind kind,int day){
        if(!relevant(kind)||day<1)return;
        var resident=t.people.get(id);
        if(resident==null||!resident.alive)return;
        String actor=actor(t);
        String key=PREFIX+actor+":"+id+":"+kind.name()+":"+(day-1)/30;
        var own=t.beliefs.computeIfAbsent(id,k->new LinkedHashMap<>());
        if(own.containsKey(key))return;
        var contact=t.player.contacts.get(id);
        String alias=contact!=null&&contact.knownName!=null&&!contact.knownName.isBlank()?contact.knownName:"someone I know";
        // Only an already disclosed alias is included, never a private name.
        String statement=switch(kind){
            case SUPPORT -> alias+" helped me.";
            case HURT -> alias+" treated me unkindly.";
            case BROKEN_TRUST -> alias+" broke my trust.";
            case APOLOGY -> alias+" apologized to me.";
            case RECEIVED_AID -> "I helped "+alias+".";
            default -> throw new IllegalArgumentException("Not a significant memory.");
        };
        own.put(key,new KnowledgeLife.Belief(id,key,day,0,actor,resident.currentSpace(),statement,"DIRECT",id,day,0,0,100));
        if((kind==SocialMemoryLife.Kind.HURT||kind==SocialMemoryLife.Kind.BROKEN_TRUST)
                &&resident.carefulness>=65)
            DeceptionLife.mark(t,id,key,day,0,"PERSONAL_INCIDENT");
    }
    static boolean episode(KnowledgeLife.Belief b){return b.eventKey().startsWith(PREFIX);}
    static boolean recognizable(TownLife.Town t,TownLife.Citizen c){
        var contact=t.player.contacts.get(c.id);
        if(contact!=null&&contact.conversations>0)return true;
        var self=RebirthLife.child(t);
        if(self==null)return false;
        var relation=t.existingRelationship(self.id,c.id);
        return relation!=null&&relation.conversations>0||ParentingLife.parents(t,self).contains(c);
    }
    static KnowledgeLife.Belief latest(TownLife.Town t,TownLife.Citizen c,boolean direct){
        var own=t.beliefs.get(c.id);
        if(own==null)return null;
        return own.values().stream().filter(b->episode(b)&&b.actorId().equals(actor(t))&&direct==b.origin().equals("DIRECT"))
                .max(Comparator.comparingInt(KnowledgeLife.Belief::eventDay).thenComparing(KnowledgeLife.Belief::eventKey)).orElse(null);
    }
    static String recollect(TownLife.Town t,TownLife.Citizen c,String language){
        boolean it="it".equals(language);
        var direct=latest(t,c,true);
        if(direct!=null){
            String kind=direct.eventKey().split(":")[direct.eventKey().split(":").length-2];
            String event=switch(kind){
                case "SUPPORT" -> it?"mi hai aiutato":"you helped me";
                case "HURT" -> it?"mi hai trattato male":"you treated me unkindly";
                case "BROKEN_TRUST" -> it?"hai tradito la mia fiducia":"you broke my trust";
                case "APOLOGY" -> it?"mi hai chiesto scusa":"you apologized to me";
                default -> it?"ti ho aiutato":"I helped you";
            };
            return it?"Ricordo il giorno "+direct.eventDay()+": "+event+".":"I remember day "+direct.eventDay()+": "+event+".";
        }
        var heard=latest(t,c,false);
        if(heard!=null&&recognizable(t,c)){
            return (it?"Mi hanno raccontato qualcosa, ma non ero presente. Non so se sia vero. ":"I heard something, but I wasn't there. I don't know if it is true. ")+(it?"":"The account was: "+heard.statement());
        }
        return null;
    }
    static boolean question(String message){
        return Set.of("what do you remember about me","cosa ricordi di me","do you remember me","ti ricordi di me").contains(NaturalDialogue.normalize(message));
    }
}
