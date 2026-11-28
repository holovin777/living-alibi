package com.holovin.npclife;

/** District-level evacuation, using the ordinary resident travel scheduler. */
final class FireEvacuationLife {
    private FireEvacuationLife(){}
    static final TownLife.Place SHELTER=TownLife.Place.PARK;
    private static String key(TownLife.Citizen c){return "fire-evacuation:"+c.generationIndex;}
    static boolean affected(TownLife.Town t,TownLife.Citizen c){
        int incident=FireServiceLife.get(t,"started");
        return incident>0&&t.industryStocks.getOrDefault(key(c),0)==incident;
    }
    static TownLife.Place destination(TownLife.Town t,TownLife.Citizen c,TownLife.Place planned){
        var site=FireServiceLife.site(t);
        if(!c.alive||c.generationIndex<=0||FireServiceLife.assigned(t,c)
                ||RebirthLife.linkedAdult(t,c)||RebirthLife.linkedChild(t,c))return null;
        // Closed housing remains unavailable after the fire is out.
        if((c.home==TownLife.Place.NORTH_RESIDENCES||c.home==TownLife.Place.SOUTH_RESIDENCES)&&FireRecoveryLife.closed(t,c.home))return SHELTER;
        if(site==null){return FireRecoveryLife.closed(t,planned)?SHELTER:null;}
        // Residents react once they are in, headed into, or normally scheduled in the affected district.
        if(!affected(t,c)&&c.place!=site&&planned!=site&&(c.travelRemaining<=0||c.destination!=site))return null;
        if(!affected(t,c)){
            t.industryStocks.put(key(c),FireServiceLife.get(t,"started"));
            FireServiceLife.add(t,"evacuated-total",1);
        }
        return SHELTER;
    }
    static int affectedCount(TownLife.Town t){
        return (int)t.people.values().stream().filter(c->c.alive&&affected(t,c)).count();
    }
    static int shelteredCount(TownLife.Town t){
        return (int)t.people.values().stream().filter(c->c.alive&&affected(t,c)&&c.presentAt(SHELTER)).count();
    }
}
