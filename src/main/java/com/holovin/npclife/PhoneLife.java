package com.holovin.npclife;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Bounded, deterministic visitor SMS inbox. No remote transfers or invented jobs. */
final class PhoneLife {
    private PhoneLife() {}
    record Message(String residentId,String name,int day,int minute,String text,String reply) {}
    record Meeting(String residentId,String name,int day,int minute,String place,String status) {}
    record Report(List<Message> messages,boolean available,List<Meeting> meetings) {}
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
    static final int MEETING_MINUTE=1080, MEETING_END=1110, DEPART_MINUTE=1050;
    static boolean meetingWindow(TownLife.Town t,TownLife.Citizen c,WorldState clock){
        return available(t)&&(get(t,c.id,"meeting-state")==1||get(t,c.id,"meeting-state")==2)&&get(t,c.id,"meeting-day")==clock.day&&clock.minute>=DEPART_MINUTE&&clock.minute<MEETING_END;
    }
    static void tick(TownLife.Town t,WorldState clock){
        if(available(t))for(var contact:t.player.contacts.values()){
            String id=contact.residentId;
            if(get(t,id,"meeting-state")!=1)continue;
            var c=t.people.get(id);
            int day=get(t,id,"meeting-day");
            if(c==null||!c.alive){put(t,id,"meeting-state",4);continue;}
            if(clock.day>day||(clock.day==day&&clock.minute>=MEETING_END)){
                put(t,id,"meeting-state",3);continue;
            }
            if(clock.day==day&&clock.minute>=MEETING_MINUTE&&c.presentAt(TownLife.Place.CAFE)&&
                    t.player.place==TownLife.Place.CAFE&&t.player.travelRemaining==0&&c.travelRemaining==0){
                put(t,id,"meeting-state",2);
            }
        }
        if(!available(t)||clock.minute<540||clock.minute>1140||clock.minute%60!=0||
                get(t,"inbox","last-day")==clock.day)return;
        for(var contact:t.player.contacts.values()){
            var c=t.people.get(contact.residentId);
            if(c==null||!c.alive||c.age<18||c.sociability<45||contact.conversations<2||
                    contact.knownName==null||!PlayerLife.knowsResident(t,c.id)||contact.trust<60||contact.affinity<12||
                    Set.of("SLEEPING","TRAVELING","SICK_LEAVE").contains(c.activity))continue;
            int last=get(t,c.id,"day");
            if(get(t,c.id,"meeting-state")==1||last>0&&clock.day-last<3)continue;
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
        if(!available(t))return new Report(List.of(),false,List.of());
        var messages=new ArrayList<Message>();
        var meetings=new ArrayList<Meeting>();
        for(var contact:t.player.contacts.values()){
            var c=t.people.get(contact.residentId);
            if(c==null)continue;
            int meetingState=get(t,c.id,"meeting-state");
            if(meetingState>0)meetings.add(new Meeting(c.id,c.name,get(t,c.id,"meeting-day"),MEETING_MINUTE,"CAFE",switch(meetingState){
                case 1 -> "SCHEDULED"; case 2 -> "MET"; case 3 -> "MISSED"; default -> "CANCELLED";
            }));
            int day=get(t,c.id,"day");
            if(day==0)continue;
            String text=switch(get(t,c.id,"kind")){
                case 2 -> "Coffee tomorrow at 18:00? Let me know.";
                case 3 -> "How are you doing? Remember to take care of yourself.";
                default -> "Hi! Hope your day is going well.";
            };
            String reply=switch(get(t,c.id,"reply")){
                case 1 -> get(t,c.id,"kind")==2?"Yes, coffee tomorrow at 18:00.":"Thanks! See you around.";
                case 2 -> "Sorry, not today.";
                default -> "";
            };
            messages.add(new Message(c.id,c.name,day,get(t,c.id,"minute"),text,reply));
        }
        messages.sort(Comparator.comparingInt(Message::day).thenComparingInt(Message::minute).reversed());
        meetings.sort(Comparator.comparingInt(Meeting::day).reversed());
        return new Report(List.copyOf(messages),true,List.copyOf(meetings));
    }
    static String reply(TownLife.Town t,String id,int choice,int day){
        if(!available(t)||t.player.travelRemaining>0)throw new IllegalArgumentException("Phone messaging is unavailable.");
        var c=t.people.get(id);
        if(c==null||!c.alive||!t.player.contacts.containsKey(id)||get(t,id,"day")==0||
                get(t,id,"reply")!=0||(choice!=1&&choice!=2))
            throw new IllegalArgumentException("No unanswered message from this contact.");
        if(choice==1&&get(t,id,"kind")==2){
            if(t.player.contacts.values().stream().anyMatch(contact->get(t,contact.residentId,"meeting-state")==1))
                throw new IllegalArgumentException("You already have a planned meeting.");
            if(day!=get(t,id,"day"))throw new IllegalArgumentException("The invitation has expired. Reply not today.");
            put(t,id,"meeting-day",Math.addExact(day,1));
            put(t,id,"meeting-state",1);
        }
        put(t,id,"reply",choice);
        return "Message sent.";
    }
}
