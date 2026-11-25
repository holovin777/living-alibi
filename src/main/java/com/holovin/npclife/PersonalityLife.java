package com.holovin.npclife;

/** Modest behavioural preferences derived from existing persisted traits.
 * No random global state, save migration, wage changes or personality-based
 * override of health, qualifications, consent or player-directed choices.
 */
final class PersonalityLife {
    private PersonalityLife() {}
    private static int centered(int trait) { return Math.max(-50, Math.min(50, trait-50)); }

    /** Four public leisure places; keep variation across days. */
    static int leisurePreference(TownLife.Citizen c,int day) {
        int[] weights={25,25,25,25}; // cafe, library, centre, park
        int social=centered(c.sociability)/3;
        int curiosity=centered(c.curiosity)/3;
        int care=centered(c.carefulness)/5;
        weights[0]+=social;
        weights[1]+=curiosity+care;
        weights[2]+=social/2;
        weights[3]-=social/2;
        int total=0;
        for(int i=0;i<weights.length;i++){weights[i]=Math.max(5,weights[i]);total+=weights[i];}
        long mixed=c.personalSeed ^ (day*0x9E3779B97F4A7C15L);
        mixed=(mixed^(mixed>>>30))*0xBF58476D1CE4E5B9L;
        mixed=(mixed^(mixed>>>27))*0x94D049BB133111EBL;
        int draw=Math.floorMod(mixed^(mixed>>>31),total);
        for(int i=0;i<weights.length;i++){draw-=weights[i];if(draw<0)return i;}
        return 3;
    }

    /** Shared interests gently accelerate bonding, not automatic friendship. */
    static int affinityBonus(TownLife.Citizen a,TownLife.Citizen b) {
        return Math.abs(a.curiosity-b.curiosity)<=15 && Math.abs(a.sociability-b.sociability)<=20?1:0;
    }

    /** Self-regulation modifies conflict pressure but does not predict guilt. */
    static int conflictBias(TownLife.Citizen a,TownLife.Citizen b) {
        return -(centered(a.carefulness)+centered(b.carefulness))/10;
    }

    static int careerBias(TownLife.Citizen c,String subject) {
        return switch(subject) {
            case "social" -> centered(c.sociability)/4;
            case "academic" -> centered(c.curiosity)/4;
            case "technical" -> centered(c.carefulness)/5+centered(c.curiosity)/10;
            case "commerce" -> centered(c.sociability)/8+centered(c.carefulness)/8;
            default -> 0;
        };
    }
}
