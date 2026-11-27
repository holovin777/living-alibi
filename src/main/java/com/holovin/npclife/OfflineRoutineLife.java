package com.holovin.npclife;

/** Bounded local-save catch-up. Linked residents keep their real identities,
 * so existing restrictions against autonomous marriage, childbirth, recruitment
 * and household relocation remain in force during this simulation.
 */
final class OfflineRoutineLife {
    private OfflineRoutineLife(){}
    static final int MAX_MINUTES=180;
    static int minutes(long saved,long now){
        if(saved<=0||now<=saved)return 0;
        return (int)Math.min(MAX_MINUTES,(now-saved)/60);
    }
    static void prepare(TownLife.Town t){
        var c=RebirthLife.child(t);
        if(RebirthLife.linkedAdult(t,c)||RebirthLife.linkedChild(t,c))
            RebirthLife.releaseManualRoutine(t);
    }
    record View(int minutesAdvanced,boolean capped,boolean lifeEnded){}
}
