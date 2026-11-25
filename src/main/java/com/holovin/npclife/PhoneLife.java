package com.holovin.npclife;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Bounded, deterministic visitor SMS inbox. No remote transfers or invented jobs. */
final class PhoneLife {
    private PhoneLife() {}
    record Message(String residentId,String name,int day,int minute,String text,String reply) {}
    record Report(List<Message> messages,boolean available) {}
    private static String key(TownLife.Town t,String id,String field){
        return "phone:"+t.player.incarnation+":"+id+":"+field;
    }
    private static int get(TownLife.Town t,String id,String field){
        return t.industryStocks.getOrDefault(key(t,id,field),0);
    }
    private static void put(TownLife.Town t,String id,String field,int value){
        t.industryStocks.put(key(t,id,field),value);
    }
    static boolean available(TownLife.Town t){
        return t.player.alive&&!RebirthLife.active(t);
    }
    static void tick(TownLife.Town t,WorldState clock){
        if(!available(t)||clock.minute<540||clock.minute>1140||clock.minute%60!=0||
                get(t,"inbox","last-day")==clock.day)return;
        for(var contact:t.player.contacts.values()){
            var c=t.people.get(contact.residentId);
            if(c==null||!c.alive||c.age<18||c.sociability<45||contact.conversations<2||
                    contact.knownName==null||contact.trust<60||contact.affinity<12||
                    Set.of("SLEEPING","TRAVELING","SICK_LEAVE").contains(c.activity))continue;
            int last=get(t,c.id,"day");
            if(last>0&&clock.day-last<3)continue;
            int kind=contact.affinity>=35?2:1;
            if(t.player.hunger>=65||t.player.thirst>=65)kind=3;
            put(t,c.id,"day",clock.day);
            put(t,c.id,"minute",clock.minute);
            put(t,c.id,"kind",kind);
            put(t,c.id,"reply",0);
            put(t,"inbox","last-day",clock.day);
            return;
        }
    }
    static Report report(TownLife.Town t){
        if(!available(t))return new Report(List.of(),false);
        var messages=new ArrayList<Message>();
        for(var contact:t.player.contacts.values()){
            var c=t.people.get(contact.residentId);
            if(c==null)continue;
            int day=get(t,c.id,"day");
            if(day==0)continue;
            String text=switch(get(t,c.id,"kind")){
                case 2 -> "Want to meet for coffee sometime? Let me know.";
                case 3 -> "How are you doing? Remember to take care of yourself.";
                default -> "Hi! Hope your day is going well.";
            };
            String reply=switch(get(t,c.id,"reply")){
                case 1 -> "Thanks! See you around.";
                case 2 -> "Sorry, not today.";
                default -> "";
            };
            messages.add(new Message(c.id,c.name,day,get(t,c.id,"minute"),text,reply));
        }
        messages.sort(Comparator.comparingInt(Message::day).thenComparingInt(Message::minute).reversed());
        return new Report(List.copyOf(messages),true);
    }
    static String reply(TownLife.Town t,String id,int choice){
        if(!available(t)||t.player.travelRemaining>0)throw new IllegalArgumentException("Phone messaging is unavailable.");
        var c=t.people.get(id);
        if(c==null||!c.alive||!t.player.contacts.containsKey(id)||get(t,id,"day")==0||
                get(t,id,"reply")!=0||(choice!=1&&choice!=2))
            throw new IllegalArgumentException("No unanswered message from this contact.");
        put(t,id,"reply",choice);
        return "Message sent.";
    }
}
