package com.holovin.npclife;

import java.util.*;

/** Read-only speech opportunities. A client delivers an offer only when the
 * actual rendered speaker is within hearing range. No conversation is forced.
 */
final class SpontaneousSpeechLife {
    private SpontaneousSpeechLife(){}
    record Offer(String id,String residentId,String kind,String text,String textIt,int priority){}
    static Offer offer(TownLife.Town t,TownLife.Citizen c,String kind,String en,String it,int priority,long slot){
        return new Offer(t.player.incarnation+":"+c.id+":"+kind+":"+slot,c.id,kind,en,it,priority);
    }
    static List<Offer> view(TownLife.Town t,int day,int minute){
        if(JusticeLife.confined(t))return List.of();
        var self=RebirthLife.child(t);
        if(RebirthLife.active(t)&&(self==null||!self.alive||self.travelRemaining>0||"SLEEPING".equals(self.activity)))return List.of();
        var visible=RebirthLife.active(t)?RebirthLife.visible(t):PlayerLife.visible(t);
        var parents=ParentingLife.parents(t,self);
        var notices=self==null?List.<ParentingLife.Notice>of():ParentingLife.view(t,self);
        long tick=(day-1L)*1440+minute;
        var result=new ArrayList<Offer>();
        for(var c:visible){
            if(!RebirthLife.awakePresent(c)||c.age<3)continue;
            boolean parent=parents.contains(c);
            if(parent&&!ConversationAvailability.urgent(c)){
                var notice=notices.stream().filter(n->n.parentId().equals(c.id)).findFirst().orElse(null);
                if(notice!=null){
                    result.add(offer(t,c,"FAMILY_REQUEST",notice.message(),notice.messageIt(),100,day));continue;
                }
                if(HouseholdLife.dinnerNow(t,c,day,minute)){
                    result.add(offer(t,c,"DINNER","Come to the table, dinner is ready!","Vieni a tavola, è pronta la cena!",90,day));continue;
                }
                if("SEARCHING_FOR_CHILD".equals(c.activity)){
                    result.add(offer(t,c,"SEARCH","Where have you been? We were worried!","Dove sei stato? Eravamo preoccupati!",110,day));continue;
                }
            }
            if(ConversationAvailability.urgent(c)||ConversationAvailability.duty(c,day,minute))continue;
            if(parent&&minute>=420&&minute<1320){
                result.add(offer(t,c,"FAMILY_GREETING","How are you doing?","Come stai?",70,tick/180));continue;
            }
            if(minute<420||minute>=1320||c.sociability<35)continue;
            var contact=t.player.contacts.get(c.id);
            var relation=self==null?null:t.existingRelationship(self.id,c.id);
            boolean known=contact!=null&&contact.conversations>0||relation!=null&&relation.conversations>0;
            boolean friend=relation!=null&&relation.affinity>=65&&relation.trust>=58||contact!=null&&contact.conversations>=3&&contact.trust>=65;
            long slot=tick/60;
            // Stable opportunities in a one-hour window. More sociable people
            // initiate more often, without consuming the world's RNG on reads.
            if(Math.floorMod(c.personalSeed^slot,100)>=c.sociability/3)continue;
            if(friend&&Math.floorMod(c.personalSeed+slot,3)==0)
                result.add(offer(t,c,"INVITE","Would you like to go for a walk?","Ti va di fare una passeggiata?",50,slot));
            else if(known)
                result.add(offer(t,c,"GREETING","Hi! How's it going?","Ciao! Come va?",40,slot));
            else if(!HomeRoutineLife.atHome(c)&&Math.floorMod(c.personalSeed+slot,5)==0)
                result.add(offer(t,c,"DIRECTIONS","Excuse me, do you know the way to the town center?","Scusa, sai come si arriva in centro?",25,slot));
            else result.add(offer(t,c,"HELLO","Good morning!","Buongiorno!",20,slot));
        }
        result.sort(Comparator.comparingInt(Offer::priority).reversed().thenComparing(Offer::residentId));
        return List.copyOf(result);
    }
}
