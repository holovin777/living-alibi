package com.holovin.npclife;

import java.util.List;

/** Independent, scarce-resource assistance between co-present generated residents. */
final class PeerAidLife {
    private PeerAidLife() {}
    private static String key(TownLife.Citizen c) {return "peer-aid:"+c.id+":last-gift";}
    static boolean family(TownLife.Town t,TownLife.Citizen a,TownLife.Citizen b) {
        int x=a.generationIndex,y=b.generationIndex;
        return x>0&&y>0&&(DemographyLife.get(t,a,"partner")==y||
            DemographyLife.get(t,b,"partner")==x||
            DemographyLife.get(t,a,"parent-a")==y||DemographyLife.get(t,a,"parent-b")==y||
            DemographyLife.get(t,b,"parent-a")==x||DemographyLife.get(t,b,"parent-b")==x);
    }
    private static boolean present(TownLife.Citizen c) {
        return c.alive&&c.generationIndex>0&&c.travelRemaining==0&&
                !"SLEEPING".equals(c.activity)&&!"TRAVELING".equals(c.activity);
    }
    static boolean share(TownLife.Town t,TownLife.Citizen donor,TownLife.Citizen recipient,boolean water,int day) {
        if(donor==recipient||!present(donor)||!present(recipient)||donor.age<18||
                "SICK_LEAVE".equals(donor.activity)||!donor.currentSpace().equals(recipient.currentSpace()))return false;
        if(RebirthLife.linkedAdult(t,donor)||RebirthLife.linkedAdult(t,recipient))return false;
        if(t.industryStocks.getOrDefault(key(donor),0)>=day)return false;
        var relation=t.peerRelationships.get(SocialNetwork.key(donor.id,recipient.id));
        boolean related=family(t,donor,recipient);
        if(!related&&(donor.sociability<55||relation==null||relation.trust<60||relation.affinity<65||relation.tension>35))return false;
        int stock=water?donor.water:donor.food;
        int donorNeed=water?donor.thirst:donor.hunger;
        int recipientNeed=water?recipient.thirst:recipient.hunger;
        int recipientStock=water?recipient.water:recipient.food;
        if(stock<3||donorNeed>=65||recipientNeed<80||recipientStock!=0)return false;
        if(water){donor.water--;recipient.water++;}else{donor.food--;recipient.food++;}
        t.industryStocks.put(key(donor),day);
        t.industryStocks.put("peer-aid:"+donor.id+":"+recipient.id+":last-help",day);
        if(relation!=null){
            relation.trust=Math.min(100,relation.trust+2);
            relation.affinity=Math.min(100,relation.affinity+2);
        }
        return true;
    }
    static void tick(TownLife.Town t,WorldState clock,List<TownLife.Event> truth,List<TownLife.Observation> observations) {
        if(clock.minute%15!=0)return;
        var residents=t.people.values().stream().filter(PeerAidLife::present).toList();
        for(var recipient:residents){
            if((recipient.thirst<80||recipient.water>0)&&(recipient.hunger<80||recipient.food>0))continue;
            for(var donor:residents){
                boolean water=recipient.thirst>=80&&recipient.water==0;
                if(!share(t,donor,recipient,water,clock.day))continue;
                TownLife.publish(t,clock,donor.id,donor.place,donor.name+" shared "+(water?"water":"food")+" with "+recipient.name+".",truth,observations);
                break;
            }
        }
    }
}
