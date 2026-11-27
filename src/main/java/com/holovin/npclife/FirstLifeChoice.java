package com.holovin.npclife;

/** First-life preference and permanent account reservation, independent of online presence.
 * Account identifiers must come from a future authenticated server, never a client action.
 */
final class FirstLifeChoice {
    private FirstLifeChoice(){}
    private static final String PREFERENCE="identity:first-sex";
    static void require(TownLife.Town t){t.industryStocks.putIfAbsent(PREFERENCE,3);}
    static boolean pending(TownLife.Town t){return t.player.incarnation==1&&t.industryStocks.getOrDefault(PREFERENCE,0)==3;}
    static void choose(TownLife.Town t,int sex){
        if(!pending(t)||RebirthLife.phase(t)!=RebirthLife.CHOICE||(sex!=1&&sex!=2))
            throw new IllegalArgumentException("First character selection is unavailable.");
        t.industryStocks.put(PREFERENCE,sex);
    }
    // Newborn names already alternate female/male by generation index in DemographyLife.
    // This does not infer sex from personality or change an existing resident.
    static int birthSex(TownLife.Citizen c){return Math.floorMod(c.generationIndex,2)==1?1:2;}
    static boolean matches(TownLife.Town t,TownLife.Citizen c){
        int sex=t.industryStocks.getOrDefault(PREFERENCE,0);
        return t.player.incarnation!=1||sex==0||sex==birthSex(c);
    }
    private static String ownerKey(TownLife.Citizen c){return "identity:account-owner:"+c.id;}
    static boolean accountOwned(TownLife.Town t,TownLife.Citizen c){return t.industryStocks.getOrDefault(ownerKey(c),0)>0;}
    static void reserveForAccount(TownLife.Town t,TownLife.Citizen c,int accountId){
        if(c==null||!c.alive||c.generationIndex<=0||accountId<=0||DemographyLife.get(t,c,"played-incarnation")>0)
            throw new IllegalArgumentException("Resident is unavailable.");
        int previous=t.industryStocks.getOrDefault(ownerKey(c),0);
        if(previous!=0&&previous!=accountId)throw new IllegalArgumentException("Resident belongs to another account.");
        t.industryStocks.put(ownerKey(c),accountId);
    }
}
