package com.holovin.npclife;

/** Local-game introductory montage. Real-time progress is persisted per resident.
 * This intentionally advances developmental age, not eighteen calendar years
 * for the entire city. Online servers must use a separate authoritative policy.
 */
final class ChildhoodPrologue {
    private ChildhoodPrologue(){}
    static final int TOTAL_SECONDS=1800, STEP_SECONDS=10;
    private static String key(TownLife.Citizen c){return "childhood:"+c.id+":prologue-seconds";}
    static boolean active(TownLife.Town t,TownLife.Citizen c){
        return RebirthLife.randomBirth(t)&&RebirthLife.linkedChild(t,c);
    }
    static int elapsed(TownLife.Town t,TownLife.Citizen c){
        return Math.min(TOTAL_SECONDS,Math.max(c.age*100,t.industryStocks.getOrDefault(key(c),0)));
    }
    record View(int elapsedSeconds,int totalSeconds,boolean atHome){}
    static View view(TownLife.Town t){
        var c=RebirthLife.child(t);
        return active(t,c)?new View(elapsed(t,c),TOTAL_SECONDS,EarlyChildhoodLife.atHome(c)):null;
    }
    static String step(TownLife.Town t){
        var c=RebirthLife.child(t);
        if(!active(t,c))throw new IllegalArgumentException("No active childhood prologue.");
        int next=Math.min(TOTAL_SECONDS,elapsed(t,c)+STEP_SECONDS);
        t.industryStocks.put(key(c),next);
        int target=next/100;
        if(target>=18&&c.travelRemaining>0)return "Finishing your journey.";
        c.age=Math.max(c.age,target);
        if(c.age>=18){
            // Unlock adult systems without fabricating school attendance or wages.
            EducationLife.graduate(t,c);
            c.occupation="Job seeker";c.employed=false;c.unemploymentDays=0;
            boolean home=EarlyChildhoodLife.atHome(c);
            RebirthLife.continueAdult(t);
            if(home)RebirthLife.goHome(t);
            else if(PlayerLife.publicPlaces().contains(c.place))RebirthLife.holdPublicPlace(t,c);
            RebirthLife.synchronize(t);
            return "You are now eighteen. Your family home remains available.";
        }
        return "Childhood continues.";
    }
}
