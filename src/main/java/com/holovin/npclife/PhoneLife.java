package com.holovin.npclife;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Bounded, deterministic visitor SMS inbox. No remote transfers or invented jobs. */
final class PhoneLife {
    private PhoneLife() {}
    record Message(String residentId,String name,int day,int minute,String text,String reply) {}
    record Meeting(String residentId,String name,int day,int minute,String place,String status,boolean guestPresent,boolean mealServed) {}
    record Call(String residentId,String name,int day,int minute,String status,String text,boolean invitation) {}
    record DialContact(String residentId,String name,boolean connected) {}
    record OutgoingCall(String residentId,String name,int day,int minute,String status,String text) {}
    record Report(List<Message> messages,boolean available,List<Meeting> meetings,List<Call> calls,
                  List<DialContact> dialContacts,List<OutgoingCall> outgoingCalls) {
        Report(List<Message> messages,boolean available,List<Meeting> meetings){this(messages,available,meetings,List.of(),List.of(),List.of());}
        Report(List<Message> messages,boolean available,List<Meeting> meetings,List<Call> calls){this(messages,available,meetings,calls,List.of(),List.of());}
    }
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
        return t.player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t);
    }
    static boolean shareNumber(TownLife.Town t,TownLife.Citizen c){
        var contact=t.player.contacts.get(c.id);
        if(!available(t)||c.age<18||contact==null||contact.knownName==null||
                !PlayerLife.knowsResident(t,c.id)||contact.trust<60||contact.affinity<12)return false;
        put(t,c.id,"number-shared",1);return true;
    }
    private static final int CALL_WINDOW=5;
    private static final int OUTGOING_WINDOW=10;
    static boolean homeInvitation(TownLife.Town t,String id,int day,int minute){
        return get(t,id,"meeting-kind")==2&&get(t,id,"meeting-day")==day
                &&(get(t,id,"meeting-state")==1||get(t,id,"meeting-state")==2)
                &&minute>=MEETING_MINUTE&&minute<MEETING_END;
    }
    static boolean homeVisitWindow(TownLife.Town t,TownLife.Citizen c,WorldState clock){
        return available(t)&&get(t,c.id,"meeting-kind")==2&&get(t,c.id,"meeting-day")==clock.day
                &&(get(t,c.id,"meeting-state")==1||get(t,c.id,"meeting-state")==2)
                &&clock.minute>=DEPART_MINUTE&&clock.minute<1170;
    }
    static boolean playerHomeWindow(TownLife.Town t,TownLife.Citizen c,WorldState clock){
        return available(t)&&t.player.ownsHouse&&get(t,c.id,"meeting-kind")==3
                &&get(t,c.id,"meeting-day")==clock.day
                &&(get(t,c.id,"meeting-state")==1||get(t,c.id,"meeting-state")==2)
                &&clock.minute>=DEPART_MINUTE&&clock.minute<1170;
    }
    static boolean guestPresent(TownLife.Town t,String id,int day,int minute){
        var c=t.people.get(id);
        return available(t)&&t.player.ownsHouse&&t.player.place==HousingLife.HOUSE_DISTRICT
                &&t.player.travelRemaining==0&&c!=null&&c.presentAt(HousingLife.HOUSE_DISTRICT)
                &&c.currentSpace().equals(TownLife.publicSpace(HousingLife.HOUSE_DISTRICT))
                &&get(t,id,"meeting-kind")==3&&get(t,id,"meeting-day")==day
                &&get(t,id,"meeting-state")==2&&minute>=MEETING_MINUTE&&minute<1170
                &&!Set.of("WORKING","SLEEPING","SICK_LEAVE","RESTING_WHILE_SICK").contains(c.activity);
    }
    static String cookTogether(TownLife.Town t,String id,int day,int minute){
        if(!guestPresent(t,id,day,minute)||minute+25>=1170)
            throw new IllegalArgumentException("A guest and enough visit time are required.");
        if(get(t,id,"shared-meal-day")==day)throw new IllegalArgumentException("You already cooked together today.");
        var recipe=FoodLife.recipe("pasta-verdure");
        for(var ingredient:recipe.ingredients().entrySet())
            if(FoodLife.quantity(t.player,ingredient.getKey(),true)<ingredient.getValue()*2)
                throw new IllegalArgumentException("Two portions of pasta, vegetables and water are required.");
        for(var ingredient:recipe.ingredients().entrySet())
            FoodLife.change(t.player,ingredient.getKey(),true,-ingredient.getValue()*2);
        var c=t.people.get(id);
        t.player.hunger=Math.max(0,t.player.hunger-recipe.hunger());
        t.player.thirst=Math.max(0,t.player.thirst-recipe.thirst());
        c.hunger=Math.max(0,c.hunger-recipe.hunger());
        c.thirst=Math.max(0,c.thirst-recipe.thirst());
        var contact=t.player.contacts.get(id);
        contact.trust=Math.min(100,contact.trust+2);contact.affinity=Math.min(100,contact.affinity+3);
        contact.lastDay=day;contact.lastMinute=minute;
        put(t,id,"shared-meal-day",day);
        ConversationMemory.remember(contact,day,minute,"und","Cooked pasta together at your home","You cooked and ate together.");
        ConversationMemory.mark(contact,ConversationMemory.Milestone.SHARED_MEAL,contact.dialogue.getLast());
        return "You cooked and ate together.";
    }
    static void visitArrived(TownLife.Town t,String id,int day,int minute){
        if(homeInvitation(t,id,day,minute))put(t,id,"meeting-state",2);
    }
    private static boolean canHost(TownLife.Town t,TownLife.Citizen c,PlayerLife.Contact contact){
        return contact!=null&&contact.trust>=80&&contact.affinity>=35&&AddressLife.sharedBuilding(t,c.id)==1
                &&AddressLife.building(c)==1&&!SocialMemoryLife.unresolved(t,c.id);
    }
    private static long outgoingStart(TownLife.Town t,String id){
        return (get(t,id,"out-day")-1L)*1440+get(t,id,"out-minute");
    }
    private static boolean canAnswer(TownLife.Citizen c,int minute){
        return c.alive&&c.age>=18&&minute>=480&&minute<1260&&c.travelRemaining==0&&
                Set.of("FREE_TIME","RELAXING","SOCIALIZING").contains(c.activity);
    }
    static String outgoing(TownLife.Town t,String id,String action,int day,int minute){
        var c=t.people.get(id);
        var contact=t.player.contacts.get(id);
        if(!available(t)||t.player.travelRemaining>0||t.player.cycling||c==null||!c.alive||
                contact==null||get(t,id,"number-shared")!=1)
            throw new IllegalArgumentException("Call unavailable.");
        long now=(day-1L)*1440+minute;
        if("dial".equals(action)){
            if(t.player.contacts.keySet().stream().anyMatch(x->ringing(t,x)||get(t,x,"out-state")==2))
                throw new IllegalArgumentException("Finish the current call first.");
            if(get(t,id,"out-day")>0&&now-outgoingStart(t,id)<30)
                return "Please wait before calling again.";
            put(t,id,"out-day",day);put(t,id,"out-minute",minute);
            if(!canAnswer(c,minute)){
                put(t,id,"out-state",1);return "No answer.";
            }
            if(contact.trust<50||contact.affinity<5||SocialMemoryLife.unresolved(t,id)){
                put(t,id,"out-state",3);return "Scusa, preferisco non parlare adesso.";
            }
            put(t,id,"out-state",2);
            contact.lastDay=day;contact.lastMinute=minute;
            return "Ciao! Dimmi pure.";
        }
        if(get(t,id,"out-state")!=2||now-outgoingStart(t,id)>=OUTGOING_WINDOW)
            throw new IllegalArgumentException("Call has ended.");
        if("hangup".equals(action)){
            put(t,id,"out-state",4);return "Call ended.";
        }
        boolean home="ask-home-visit".equals(action);
        boolean hosting="invite-my-home".equals(action);
        if(hosting&&(!t.player.ownsHouse||c.home==HousingLife.HOUSE_DISTRICT||contact.trust<75||contact.affinity<30||SocialMemoryLife.unresolved(t,id))){
            put(t,id,"out-state",9);return "Mi dispiace, non posso venire a casa tua.";
        }
        if(!home&&!hosting&&!"invite-coffee".equals(action))throw new IllegalArgumentException("Choose a call action.");
        if(home&&!canHost(t,c,contact)){
            put(t,id,"out-state",7);return "Mi dispiace, preferisco non ricevere visite a casa.";
        }
        if(!canAnswer(c,minute)||contact.trust<65||contact.affinity<20||
                t.player.contacts.values().stream().anyMatch(x->get(t,x.residentId,"meeting-state")==1)){
            put(t,id,"out-state",hosting?9:home?7:5);return hosting?"Mi dispiace, non posso venire a casa tua.":home?"Mi dispiace, preferisco non ricevere visite a casa.":"Mi dispiace, non posso prendere un caffè domani.";
        }
        put(t,id,"meeting-day",Math.addExact(day,1));put(t,id,"meeting-state",1);
        put(t,id,"meeting-kind",hosting?3:home?2:1);
        put(t,id,"out-state",hosting?10:home?8:6);
        contact.lastDay=day;contact.lastMinute=minute;
        return hosting?"Volentieri! Vengo a casa tua domani alle 18.":home?"Volentieri! Vieni a trovarmi domani alle 18.":"Volentieri! Domani alle 18 al bar.";
    }
    private static String outgoingStatus(int state){
        return switch(state){case 1 -> "NO_ANSWER";case 2 -> "CONNECTED";case 3 -> "DECLINED";
            case 4 -> "ENDED";case 5,7,9 -> "INVITATION_DECLINED";default -> "ACCEPTED";};
    }
    private static String outgoingText(int state){
        return switch(state){case 2 -> "Ciao! Dimmi pure.";case 3 -> "Scusa, preferisco non parlare adesso.";
            case 5 -> "Mi dispiace, non posso prendere un caffè domani.";
            case 6 -> "Volentieri! Domani alle 18 al bar.";case 7 -> "Mi dispiace, preferisco non ricevere visite a casa.";case 8 -> "Volentieri! Vieni a trovarmi domani alle 18.";case 9 -> "Mi dispiace, non posso venire a casa tua.";case 10 -> "Volentieri! Vengo a casa tua domani alle 18.";default -> "";};
    }
    private static boolean ringing(TownLife.Town t,String id){return get(t,id,"call-state")==1;}
    private static long callStart(TownLife.Town t,String id){
        return (get(t,id,"call-day")-1L)*1440+get(t,id,"call-minute");
    }
    private static void tickCalls(TownLife.Town t,WorldState clock){
        if(!available(t))return;
        long now=(clock.day-1L)*1440+clock.minute;
        boolean active=false;
        for(var contact:t.player.contacts.values()){
            String id=contact.residentId;
            var c=t.people.get(id);
            if(get(t,id,"out-state")==2){
                if(c==null||!c.alive||now-outgoingStart(t,id)>=OUTGOING_WINDOW)put(t,id,"out-state",4);
                else active=true;
            }
            if(!ringing(t,id))continue;
            if(c==null||!c.alive||now-callStart(t,id)>=CALL_WINDOW)put(t,id,"call-state",4);
            else active=true;
        }
        if(active||clock.minute<600||clock.minute>1140||clock.minute%60!=0||
                get(t,"inbox","last-call-day")==clock.day)return;
        for(var contact:t.player.contacts.values()){
            String id=contact.residentId;var c=t.people.get(id);
            if(c==null||!c.alive||c.age<18||get(t,id,"number-shared")!=1||
                    contact.trust<65||contact.affinity<20||c.sociability<45||
                    !Set.of("FREE_TIME","RELAXING","SOCIALIZING").contains(c.activity))continue;
            int last=get(t,id,"call-day");
            if(last>0&&clock.day-last<7)continue;
            // Prefer a catch-up after a few days; good friends can offer coffee.
            if(clock.day-contact.lastDay<3&&contact.affinity<35)continue;
            boolean invitation=contact.affinity>=35&&get(t,id,"meeting-state")!=1&&
                    t.player.contacts.values().stream().noneMatch(x->get(t,x.residentId,"meeting-state")==1);
            put(t,id,"call-day",clock.day);put(t,id,"call-minute",clock.minute);
            put(t,id,"call-state",1);put(t,id,"call-kind",invitation?(canHost(t,c,contact)&&Math.floorMod(id.hashCode()+clock.day,3)==0?3:2):1);
            put(t,"inbox","last-call-day",clock.day);return;
        }
    }
    private static String callText(int kind){
        return kind==3?"Ciao! Ti va di venire a casa mia domani alle 18?":kind==2?"Ciao! Ti va un caffè domani alle 18?":"Ciao! Come stai? È da un po' che non ci sentiamo.";
    }
    static String answerCall(TownLife.Town t,String id,String action,int day,int minute){
        var c=t.people.get(id);
        if(!available(t)||c==null||!c.alive||get(t,id,"number-shared")!=1)
            throw new IllegalArgumentException("Call unavailable.");
        int state=get(t,id,"call-state");
        if("answer".equals(action)||"decline".equals(action)){
            long now=(day-1L)*1440+minute;
            if(state!=1||now-callStart(t,id)>=CALL_WINDOW)throw new IllegalArgumentException("Call has ended.");
            put(t,id,"call-state","answer".equals(action)?2:3);
            if("decline".equals(action))return "Call declined.";
            var contact=t.player.contacts.get(id);
            contact.lastDay=day;contact.lastMinute=minute;
            return callText(get(t,id,"call-kind"));
        }
        if(!"accept".equals(action)||state!=2||get(t,id,"call-kind")<2||get(t,id,"call-day")!=day)
            throw new IllegalArgumentException("No current coffee invitation.");
        if(t.player.contacts.values().stream().anyMatch(x->get(t,x.residentId,"meeting-state")==1))
            throw new IllegalArgumentException("You already have a planned meeting.");
        put(t,id,"meeting-day",Math.addExact(day,1));put(t,id,"meeting-state",1);
        boolean home=get(t,id,"call-kind")==3;
        if(home&&!canHost(t,c,t.player.contacts.get(id)))throw new IllegalArgumentException("Home invitation is no longer available.");
        put(t,id,"meeting-kind",home?2:1);
        put(t,id,"call-state",5);
        return home?"A domani alle 18 a casa mia!":"A domani alle 18 al bar!";
    }
    static final int MEETING_MINUTE=1080, MEETING_END=1110, DEPART_MINUTE=1050;
    static boolean meetingWindow(TownLife.Town t,TownLife.Citizen c,WorldState clock){
        return available(t)&&get(t,c.id,"meeting-kind")<2&&(get(t,c.id,"meeting-state")==1||get(t,c.id,"meeting-state")==2)&&get(t,c.id,"meeting-day")==clock.day&&clock.minute>=DEPART_MINUTE&&clock.minute<MEETING_END;
    }
    static void tick(TownLife.Town t,WorldState clock){
        tickCalls(t,clock);
        if(available(t))for(var contact:t.player.contacts.values()){
            String id=contact.residentId;
            if(get(t,id,"meeting-state")!=1)continue;
            var c=t.people.get(id);
            int day=get(t,id,"meeting-day");
            if(c==null||!c.alive){put(t,id,"meeting-state",4);continue;}
            if(clock.day>day||(clock.day==day&&clock.minute>=MEETING_END)){
                put(t,id,"meeting-state",3);continue;
            }
            if(get(t,id,"meeting-kind")==3&&clock.day==day&&clock.minute>=MEETING_MINUTE&&t.player.ownsHouse&&c.presentAt(HousingLife.HOUSE_DISTRICT)&&c.currentSpace().equals(TownLife.publicSpace(HousingLife.HOUSE_DISTRICT))&&t.player.place==HousingLife.HOUSE_DISTRICT&&t.player.travelRemaining==0&&!Set.of("WORKING","SLEEPING","SICK_LEAVE","RESTING_WHILE_SICK").contains(c.activity))put(t,id,"meeting-state",2);
            if(get(t,id,"meeting-kind")<2&&clock.day==day&&clock.minute>=MEETING_MINUTE&&c.presentAt(TownLife.Place.CAFE)&&
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
    static Report report(TownLife.Town t){return report(t,0,0);}
    static Report report(TownLife.Town t,int currentDay,int currentMinute){
        if(!available(t))return new Report(List.of(),false,List.of());
        var messages=new ArrayList<Message>();
        var meetings=new ArrayList<Meeting>();
        var calls=new ArrayList<Call>();
        var dialContacts=new ArrayList<DialContact>();
        var outgoingCalls=new ArrayList<OutgoingCall>();
        for(var contact:t.player.contacts.values()){
            var c=t.people.get(contact.residentId);
            if(c==null)continue;
            int outState=get(t,c.id,"out-state");
            if(c.alive&&get(t,c.id,"number-shared")==1)dialContacts.add(new DialContact(c.id,c.name,outState==2));
            if(outState>0)outgoingCalls.add(new OutgoingCall(c.id,c.name,get(t,c.id,"out-day"),get(t,c.id,"out-minute"),outgoingStatus(outState),outgoingText(outState)));
            int callState=get(t,c.id,"call-state");
            if(callState>0)calls.add(new Call(c.id,c.name,get(t,c.id,"call-day"),get(t,c.id,"call-minute"),switch(callState){case 1 -> "RINGING";case 2 -> "ANSWERED";case 3 -> "DECLINED";case 4 -> "MISSED";default -> "ACCEPTED";},callState==2||callState==5?callText(get(t,c.id,"call-kind")):"",callState==2&&get(t,c.id,"call-kind")>=2));
            int meetingState=get(t,c.id,"meeting-state");
            if(meetingState>0)meetings.add(new Meeting(c.id,c.name,get(t,c.id,"meeting-day"),MEETING_MINUTE,get(t,c.id,"meeting-kind")==3?"PLAYER_HOME":get(t,c.id,"meeting-kind")==2?"HOME":"CAFE",switch(meetingState){
                case 1 -> "SCHEDULED"; case 2 -> "MET"; case 3 -> "MISSED"; default -> "CANCELLED";
            },guestPresent(t,c.id,currentDay,currentMinute),currentDay>0&&get(t,c.id,"shared-meal-day")==currentDay));
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
        calls.sort(Comparator.comparingInt(Call::day).thenComparingInt(Call::minute).reversed());
        outgoingCalls.sort(Comparator.comparingInt(OutgoingCall::day).thenComparingInt(OutgoingCall::minute).reversed());
        return new Report(List.copyOf(messages),true,List.copyOf(meetings),List.copyOf(calls),List.copyOf(dialContacts),List.copyOf(outgoingCalls));
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
            put(t,id,"meeting-kind",1);
        }
        put(t,id,"reply",choice);
        return "Message sent.";
    }
}
