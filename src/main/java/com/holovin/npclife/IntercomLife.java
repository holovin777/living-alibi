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
    record ApartmentVisit(String residentId,String name,int unit,int level,String style,String status,int until) {}
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
        put(t,id,"inside-until",Math.addExact(time,30));
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
            return new ApartmentVisit(id,c.name,get(t,id,"unit"),level,style,get(t,id,"inside")>0?"VISITING":"INVITED",get(t,id,"inside-until"));
        }
        return null;
    }
    static String enter(TownLife.Town t,String id,int building,int day,int minute){
        validate(t,id,building);
        var invitation=apartment(t,day,minute);
        if(invitation==null||!invitation.residentId().equals(id))throw new IllegalArgumentException("No valid apartment invitation.");
        put(t,id,"inside",1);
        return "Welcome inside.";
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
