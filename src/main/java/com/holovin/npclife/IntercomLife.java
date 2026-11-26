package com.holovin.npclife;

import java.util.Set;

/** A door call is local to a known building. A visit never grants unrestricted private access. */
final class IntercomLife {
    private IntercomLife(){}
    private static String key(TownLife.Town t,String id,String field){
        return "intercom:"+t.player.incarnation+":"+id+":"+field;
    }
    private static int get(TownLife.Town t,String id,String field){
        return t.industryStocks.getOrDefault(key(t,id,field),0);
    }
    private static void put(TownLife.Town t,String id,String field,int value){
        t.industryStocks.put(key(t,id,field),value);
    }
    private static int now(int day,int minute){
        return Math.addExact(Math.multiplyExact(day-1,1440),minute);
    }
    private static TownLife.Citizen validate(TownLife.Town t,String id,int building){
        if(!PhoneLife.available(t)||t.player.travelRemaining>0||t.player.cycling)
            throw new IllegalArgumentException("Door calls require an adult visitor on foot.");
        var c=t.people.get(id);
        if(c==null||!PlayerLife.knowsResident(t,id)||building==0||
                AddressLife.sharedBuilding(t,id)!=building||
                !AddressLife.district(building).equals(t.player.place.name()))
            throw new IllegalArgumentException("You do not know this address or are not at its district.");
        return c;
    }
    private static boolean available(TownLife.Citizen c,int minute){
        return c.alive&&c.age>=18&&c.travelRemaining==0&&c.place==c.home&&
                c.currentSpace().equals(c.homeUnit)&&minute>=480&&minute<1260&&
                !Set.of("SLEEPING","WORKING","SICK_LEAVE","RESTING_WHILE_SICK","TRAVELING").contains(c.activity);
    }
    static String ring(TownLife.Town t,String id,int building,int day,int minute){
        var c=validate(t,id,building);
        int time=now(day,minute);
        if(get(t,id,"last-ring")>0&&time-get(t,id,"last-ring")<10)
            return "Please wait before ringing again.";
        put(t,id,"last-ring",time);
        put(t,id,"until",0);
        if(AddressLife.building(c)!=building||!available(c,minute))return "No answer at the intercom.";
        var contact=t.player.contacts.get(id);
        if(contact==null||contact.trust<75||contact.affinity<25)
            return "Hello. Sorry, I cannot receive visitors right now.";
        put(t,id,"until",Math.addExact(time,10));
        return "Hello! We can chat at the entrance for a few minutes.";
    }
    record ApartmentVisit(String residentId,String name,int unit,int level,String style,String status,int until,String activity,int cleanliness,HouseholdLife.View household) {}
    private static int unit(TownLife.Town t,TownLife.Citizen c){
        if(c.id.equals("marco"))return 1;
        String key="intercom:unit:"+c.homeUnit;
        int assigned=t.industryStocks.getOrDefault(key,0);
        if(assigned>0)return assigned;
        for(int n=2;n<=6;n++){
            final int candidate=n;
            if(t.industryStocks.entrySet().stream().noneMatch(e->e.getKey().startsWith("intercom:unit:")&&e.getValue()==candidate)){
                t.industryStocks.put(key,n);return n;
            }
        }
        return 0;
    }
    static String invite(TownLife.Town t,String id,int building,int day,int minute){
        var c=validate(t,id,building);
        int time=now(day,minute);
        var contact=t.player.contacts.get(id);
        if(building!=1||get(t,id,"until")<=time||AddressLife.building(c)!=1||!available(c,minute)||
                contact==null||contact.trust<80||contact.affinity<35)
            return "Sorry, I cannot invite you inside right now.";
        if(apartment(t,day,minute)!=null)return "You already have an apartment invitation.";
        int number=unit(t,c);
        if(number==0)return "This apartment is not accessible yet.";
        put(t,id,"unit",number);
        put(t,id,"inside-until",Math.addExact(time,PhoneLife.homeInvitation(t,id,day,minute)?60:30));
        put(t,id,"inside",0);
        put(t,id,"until",0);
        return "Come in! Apartment "+number+".";
    }
    static ApartmentVisit apartment(TownLife.Town t,int day,int minute){
        if(!PhoneLife.available(t)||t.player.place!=TownLife.Place.NORTH_RESIDENCES||t.player.travelRemaining>0||t.player.cycling)return null;
        for(var contact:t.player.contacts.values()){
            String id=contact.residentId;
            var c=t.people.get(id);
            if(c==null||get(t,id,"inside-until")<=now(day,minute)||!available(c,minute)||AddressLife.building(c)!=1||
                    contact.trust<80||contact.affinity<35)continue;
            int level=c.money>=250?2:c.money>=90?1:0;
            String style=c.carefulness>=65?"tiglio":c.sociability>=65?"rosa":"olmo";
            return new ApartmentVisit(id,c.name,get(t,id,"unit"),level,style,get(t,id,"inside")>0?"VISITING":"INVITED",get(t,id,"inside-until"),HomeRoutineLife.activity(t,c,day,minute),HomeRoutineLife.cleanliness(t,c),HouseholdLife.view(t,c,day,minute,get(t,id,"inside")>0));
        }
        return null;
    }
    static TownLife.Citizen visitingResident(TownLife.Town t,String id,int day,int minute){
        var visit=apartment(t,day,minute);
        if(visit==null||!"VISITING".equals(visit.status()))return null;
        var host=t.people.get(visit.residentId());
        var member=t.people.get(id);
        return HouseholdLife.availableMember(host,member)?member:null;
    }
    static boolean insideVisit(TownLife.Town t,int day,int minute){
        var visit=apartment(t,day,minute);
        return visit!=null&&"VISITING".equals(visit.status());
    }
    static String introduceFamily(TownLife.Town t,String id,int day,int minute,String language){
        var visit=apartment(t,day,minute);
        if(visit==null||!"VISITING".equals(visit.status())||!visit.residentId().equals(id))
            return "it".equals(language)?"Possiamo parlarne quando sarai a casa mia.":"We can talk about that when you visit my home.";
        return HouseholdLife.introduce(t,t.get(id),day,minute,language);
    }
    static String enter(TownLife.Town t,String id,int building,int day,int minute){
        validate(t,id,building);
        var invitation=apartment(t,day,minute);
        if(invitation==null||!invitation.residentId().equals(id))throw new IllegalArgumentException("No valid apartment invitation.");
        put(t,id,"inside",1);
        PhoneLife.visitArrived(t,id,day,minute);
        return "Welcome inside.";
    }
    record SocialResult(int minutes,String message) {}
    static SocialResult socialize(TownLife.Town t,String id,int building,String action,int day,int minute){
        var host=validate(t,id,building);
        var invitation=apartment(t,day,minute);
        if(invitation==null||!invitation.residentId().equals(id)||!"VISITING".equals(invitation.status()))
            throw new IllegalArgumentException("You need an active apartment visit.");
        int duration=switch(action){
            case "talk" -> 5;
            case "coffee" -> 5;
            case "snack" -> 8;
            case "dinner" -> 15;
            case "rest" -> 10;
            default -> throw new IllegalArgumentException("Unknown guest activity.");
        };
        if(now(day,minute)+duration>=invitation.until())
            throw new IllegalArgumentException("There is not enough time left in this visit.");
        if(get(t,id,"social-"+action)==day)
            throw new IllegalArgumentException("You already did this together today.");
        var contact=t.player.contacts.get(id);
        String message;
        switch(action){
            case "talk" -> {
                contact.conversations++;
                contact.lastDay=day;contact.lastMinute=minute;contact.lastHeard="Conversation at home";
                message="You talked together at home.";
            }
            case "coffee" -> {
                if(host.water<1)throw new IllegalArgumentException("Sorry, I have no water for coffee.");
                host.water--;
                t.player.thirst=Math.max(0,t.player.thirst-16);
                message="You shared a coffee.";
            }
            case "snack" -> {
                if(host.food<1)throw new IllegalArgumentException("Sorry, I have no food to share.");
                host.food--;
                t.player.hunger=Math.max(0,t.player.hunger-25);
                message="You shared a snack.";
            }
            case "dinner" -> {
                if(minute<1080||minute>=1260)throw new IllegalArgumentException("Dinner is served between 18:00 and 21:00.");
                if(host.food<2||host.water<2)throw new IllegalArgumentException("Sorry, I do not have enough food for dinner.");
                host.food-=2;host.water-=2;
                host.hunger=Math.max(0,host.hunger-35);host.thirst=Math.max(0,host.thirst-20);
                t.player.hunger=Math.max(0,t.player.hunger-35);t.player.thirst=Math.max(0,t.player.thirst-20);
                message="You shared dinner.";
            }
            default -> {
                t.player.fatigue=Math.max(0,t.player.fatigue-12);
                message="You relaxed together on the sofa.";
            }
        }
        put(t,id,"social-"+action,day);
        contact.lastDay=day;contact.lastMinute=minute;
        if("dinner".equals(action)){
            ConversationMemory.remember(contact,day,minute,"und","Shared dinner at home",message);
            ConversationMemory.mark(contact,ConversationMemory.Milestone.SHARED_MEAL,contact.dialogue.getLast());
        }
        contact.trust=Math.min(100,contact.trust+1);
        contact.affinity=Math.min(100,contact.affinity+2);
        return new SocialResult(duration,message);
    }
    static String leave(TownLife.Town t,String id,int building,int day,int minute){
        validate(t,id,building);
        put(t,id,"inside-until",0);
        put(t,id,"inside",0);
        return "You left the apartment.";
    }
    static String visit(TownLife.Town t,String id,int building,int day,int minute){
        var c=validate(t,id,building);
        int time=now(day,minute);
        if(get(t,id,"until")<=time||AddressLife.building(c)!=building||!available(c,minute))
            throw new IllegalArgumentException("There is no current invitation at this door.");
        put(t,id,"until",0);
        var contact=t.player.contacts.get(id);
        if(contact==null||contact.trust<75||contact.affinity<25)
            throw new IllegalArgumentException("The resident is no longer available for a visit.");
        if(get(t,id,"visit-day")!=day){
            contact.trust=Math.min(100,contact.trust+2);
            contact.affinity=Math.min(100,contact.affinity+3);
            put(t,id,"visit-day",day);
        }
        contact.conversations++;
        contact.lastDay=day;
        contact.lastMinute=minute;
        contact.lastHeard="Doorstep visit";
        return "You chatted together at the entrance.";
    }
}
