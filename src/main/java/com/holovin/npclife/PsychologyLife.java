package com.holovin.npclife;

import java.util.HashMap;
import java.util.HashSet;

/** Persistent fictional wellbeing metrics, not diagnoses or clinical predictions. */
final class PsychologyLife {
    private PsychologyLife(){}
    static String key(TownLife.Citizen c,String field){return "psychology:"+c.id+":"+field;}
    static int get(TownLife.Town t,TownLife.Citizen c,String field,int fallback){return t.industryStocks.getOrDefault(key(c,field),fallback);}
    static void set(TownLife.Town t,TownLife.Citizen c,String field,int value){t.industryStocks.put(key(c,field),Math.max(0,Math.min(100,value)));}
    static void adjust(TownLife.Town t,TownLife.Citizen c,String field,int fallback,int delta){set(t,c,field,get(t,c,field,fallback)+delta);}
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute!=1260||t.industryStocks.getOrDefault("psychology:last-day",0)>=s.day)return;
        t.industryStocks.put("psychology:last-day",s.day);
        long now=(s.day-1L)*1440+s.minute;
        var contact=new HashSet<String>();var support=new HashSet<String>();var tension=new HashMap<String,Integer>();
        for(var r:t.peerRelationships.values()){
            if(r.lastInteractionTick<0||r.lastInteractionTick>now||now-r.lastInteractionTick>=1440)continue;
            contact.add(r.firstId);contact.add(r.secondId);
            if(r.trust>=60&&r.affinity>=60&&r.tension<30){support.add(r.firstId);support.add(r.secondId);}
            tension.merge(r.firstId,r.tension,Math::max);tension.merge(r.secondId,r.tension,Math::max);
        }
        for(var c:t.people.values()){
            if(!c.alive||c.generationIndex<=0)continue;
            boolean close=support.contains(c.id),social=contact.contains(c.id);
            boolean grief=FuneralLife.grieving(t,c,s.day),strain=tension.getOrDefault(c.id,0)>=60;
            boolean hardship=c.unpaidBills>0||c.hunger>=80||c.thirst>=80;
            int stress=(hardship?3:0)+(strain?5:0)+(grief?4:0)+(c.fatigue>=75?3:0)-(close?3:0)-2;
            adjust(t,c,"stress",20,stress);
            adjust(t,c,"loneliness",20,close?-8:social?-3:1+c.sociability/35);
            int burnout=get(t,c,"burnout",0);
            set(t,c,"burnout",burnout+(c.employed&&c.fatigue>=75?3:-2));
            adjust(t,c,"confidence",50,close?1:strain?-2:0);
            adjust(t,c,"satisfaction",60,(close?3:0)+(c.health>=75?1:-2)-(grief?4:0)-(strain?2:0)-(get(t,c,"stress",20)>=70?2:0)-(get(t,c,"loneliness",20)>=70?2:0));
        }
    }
    record View(int stress,int loneliness,int burnout,int confidence,int satisfaction){}
    static View view(TownLife.Town t,TownLife.Citizen c){return c==null?null:new View(get(t,c,"stress",20),get(t,c,"loneliness",20),get(t,c,"burnout",0),get(t,c,"confidence",50),get(t,c,"satisfaction",60));}
}
