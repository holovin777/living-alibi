package com.holovin.npclife;

import java.util.Comparator;
import java.util.Locale;

/** Voluntary assistance outside shops. No new people, invented cash or
 * compulsory interaction; asking is a temporary routine, not an identity. */
final class StreetAidLife {
    private StreetAidLife(){}
    static final int GIFT=2;
    private static final String PREFIX="street-aid:";
    private static String key(TownLife.Citizen c,String f){return PREFIX+c.id+":"+f;}
    private static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    private static void set(TownLife.Town t,String k,int n){t.industryStocks.put(k,n);}
    static TownLife.Place destination(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(!c.alive||c.generationIndex<=0||c.age<18||c.employed||c.health<40||
                c.hunger>=85||c.thirst>=85||c.fatigue>=85||
                RebirthLife.linkedAdult(t,c)||EmploymentLife.sick(c,day)||CityExpansionLife.outer(c.home))return null;
        boolean habit=get(t,key(c,"successful-days"))>=3;
        boolean need=c.money<12||c.money<25&&(c.food==0||c.water==0||c.unpaidBills>0);
        if(!need&&!(habit&&c.money<45))return null;
        int start=Math.floorMod(c.personalSeed,2)==0?600:960;
        if(minute<start||minute>=start+120)return null;
        return Math.floorMod(c.personalSeed,3)==0?TownLife.Place.BAKERY:TownLife.Place.MARKET;
    }
    static boolean asking(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        return c.travelRemaining==0&&"ASKING_FOR_HELP".equals(c.activity)&&
                c.place==destination(t,c,day,minute)&&c.currentSpace().equals(TownLife.publicSpace(c.place));
    }
    private static void recordGift(TownLife.Town t,TownLife.Citizen c,int day,int amount){
        if(get(t,key(c,"last-received"))!=day){
            set(t,key(c,"last-received"),day);
            set(t,key(c,"successful-days"),Math.addExact(get(t,key(c,"successful-days")),1));
        }
        set(t,key(c,"total"),Math.addExact(get(t,key(c,"total")),amount));
    }
    static String dialogue(TownLife.Town t,TownLife.Citizen c,String message,int day,int minute){
        String m=message.strip().toLowerCase(Locale.ROOT).replaceAll("[.!?]+$","");
        boolean gift=m.equals("give 2 eur")||m.equals("offri 2 eur")||m.equals("ti do 2 euro");
        boolean decline=m.equals("decline request")||m.equals("rifiuta gentilmente");
        boolean reason=m.equals("why are you asking for money")||m.equals("perché chiedi soldi");
        if(!gift&&!decline&&!reason)return null;
        if(decline)return "No problem. Have a good day.";
        if(!asking(t,c,day,minute))return "I am not asking for money right now.";
        if(reason)return c.food==0||c.water==0?"I need money for groceries.":
                get(t,key(c,"successful-days"))>=3?"I have been getting by with a little help.":
                "I am looking for work as well.";
        String once=PREFIX+"player:"+t.player.incarnation+":"+c.id+":day";
        if(get(t,once)==day)return "You have already helped me today.";
        if(RebirthLife.active(t)){
            var self=RebirthLife.child(t);
            if(!RebirthLife.linkedAdult(t,self)||self.money<GIFT)return "You do not have 2 EUR to spare.";
            int wallet=Math.addExact(c.money,GIFT);
            self.money-=GIFT;c.money=wallet;
        }else{
            if(t.player.money<GIFT)return "You do not have 2 EUR to spare.";
            int wallet=Math.addExact(c.money,GIFT);
            t.player.money-=GIFT;c.money=wallet;
        }
        set(t,once,day);recordGift(t,c,day,GIFT);
        SocialMemoryLife.remember(t,c.id,SocialMemoryLife.Kind.SUPPORT,day);
        return "Thank you. That helps.";
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute%30!=0)return;
        var recipients=t.people.values().stream().filter(c->asking(t,c,s.day,s.minute))
                .sorted(Comparator.comparingInt((TownLife.Citizen c)->c.money).thenComparing(c->c.id)).toList();
        if(recipients.isEmpty())return;
        for(var donor:t.people.values()){
            if(!donor.alive||donor.generationIndex<=0||donor.age<18||donor.money<60||
                    donor.travelRemaining>0||RebirthLife.linkedAdult(t,donor)||
                    ConversationAvailability.urgent(donor)||donor.sociability<55||
                    "WORKING".equals(donor.activity)||get(t,key(donor,"donated"))==s.day)continue;
            if(!donor.currentSpace().equals(TownLife.publicSpace(donor.place)))continue;
            if(Math.floorMod(donor.personalSeed+s.day*31L+s.minute,100)>=donor.sociability/3)continue;
            for(var c:recipients){
                if(c==donor||c.place!=donor.place||!asking(t,c,s.day,s.minute))continue;
                int wallet=Math.addExact(c.money,GIFT);
                donor.money-=GIFT;c.money=wallet;
                set(t,key(donor,"donated"),s.day);recordGift(t,c,s.day,GIFT);
                break;
            }
        }
    }
}
