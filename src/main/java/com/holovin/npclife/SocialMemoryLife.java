package com.holovin.npclife;

/** Bounded, private autobiographical summaries of interactions with this visitor.
 * Existing persisted world counters hold one last date per kind, not transcripts.
 * The incarnation key prevents a new life inheriting someone else's reputation.
 */
final class SocialMemoryLife {
    private SocialMemoryLife() {}
    enum Kind { SUPPORT, HURT, BROKEN_TRUST, APOLOGY, UNRESOLVED, RECEIVED_AID, AUTO_BREAD, AUTO_WATER }
    private static String key(TownLife.Town town,String resident,Kind kind) {
        return "social-memory:"+town.player.incarnation+":"+resident+":"+kind.name();
    }
    static int last(TownLife.Town town,String resident,Kind kind) {
        return town.industryStocks.getOrDefault(key(town,resident,kind),0);
    }
    static void remember(TownLife.Town town,String resident,Kind kind,int day) {
        town.industryStocks.merge(key(town,resident,kind),day,Math::max);
        LifetimeMemoryLife.remember(town,resident,kind,day);
        if(kind==Kind.HURT||kind==Kind.BROKEN_TRUST)town.industryStocks.put(key(town,resident,Kind.UNRESOLVED),1);
        if(kind==Kind.APOLOGY)town.industryStocks.put(key(town,resident,Kind.UNRESOLVED),0);
    }
    static boolean unresolved(TownLife.Town town,String resident) {
        return last(town,resident,Kind.UNRESOLVED)>0;
    }
    /** An ordinary slight can lose its emotional force; keep the event itself. */
    static void relaxHurt(TownLife.Town town,String resident,int day,boolean serious){
        int hurt=last(town,resident,Kind.HURT);
        if(!serious&&unresolved(town,resident)&&hurt>0&&day-hurt>=90)
            town.industryStocks.put(key(town,resident,Kind.UNRESOLVED),0);
    }
    static String recollection(TownLife.Town town,String resident) {
        if(unresolved(town,resident))return "I remember how you treated me. Trust takes time.";
        if(last(town,resident,Kind.APOLOGY)>0)return "I remember your apology. We can move forward.";
        if(last(town,resident,Kind.SUPPORT)>0)return "I remember when you were there for me. Thank you.";
        if(last(town,resident,Kind.HURT)>0)return "I remember we had a disagreement, but it was a while ago.";
        return "We haven't shared anything like that yet.";
    }
}
