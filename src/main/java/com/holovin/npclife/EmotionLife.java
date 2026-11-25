package com.holovin.npclife;

/** Observable, situational mood derived from already persisted world state.
 * Never exposes private testimony or an identity of the other participant.
 */
final class EmotionLife {
    private EmotionLife() {}
    enum Mood { CALM, HAPPY, ENTHUSIASTIC, TIRED, STRESSED, IRRITATED, SAD, LONELY }
    static Mood mood(TownLife.Town town,TownLife.Citizen c,int day,int minute) {
        if(c.health<45 || "SICK_LEAVE".equals(c.activity))return Mood.SAD;
        if(c.fatigue>=85)return Mood.TIRED;
        if(c.hunger>=85 || c.thirst>=85)return Mood.IRRITATED;
        long now=(long)(day-1)*1440L+minute;
        int recentTension=0;boolean friendly=false;
        long lastContact=-1;
        for(var r:town.peerRelationships.values()) {
            if(!r.firstId.equals(c.id)&&!r.secondId.equals(c.id))continue;
            long last=r.lastInteractionTick;
            if(last<0||last>now)continue;
            lastContact=Math.max(lastContact,last);
            if(now-last<=360) {
                recentTension=Math.max(recentTension,r.tension);
                if(r.affinity>=65&&r.trust>=58&&r.tension<25)friendly=true;
            }
        }
        if(recentTension>=60)return Mood.IRRITATED;
        if(recentTension>=35 || c.fatigue>=65)return Mood.STRESSED;
        if(friendly)return Mood.HAPPY;
        if(c.sociability>=70 && now>=2880 && (lastContact<0||now-lastContact>=2880))return Mood.LONELY;
        if("WORKING".equals(c.activity)&&c.curiosity>=70&&c.fatigue<40&&c.hunger<55&&c.thirst<55)return Mood.ENTHUSIASTIC;
        return Mood.CALM;
    }
    static String reply(Mood mood,boolean trustsVisitor) {
        if(!trustsVisitor && mood!=Mood.CALM && mood!=Mood.HAPPY && mood!=Mood.ENTHUSIASTIC)
            return "I'd rather not talk about it right now.";
        return switch(mood) {
            case TIRED -> "I'm exhausted. I need some rest.";
            case IRRITATED -> "I'm a little irritated today.";
            case STRESSED -> "It's been a stressful day.";
            case SAD -> "I'm not feeling very well today.";
            case LONELY -> "I haven't had much company lately.";
            case HAPPY -> "I'm having a good day, thanks!";
            case ENTHUSIASTIC -> "I'm really enjoying my work today!";
            case CALM -> "I'm doing all right, thanks for asking.";
        };
    }
}
