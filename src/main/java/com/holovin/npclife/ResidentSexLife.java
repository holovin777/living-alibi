package com.holovin.npclife;

import java.util.Set;

/** Biological sex used only by the abstract natural-birth model.
 * Older saves lack this field; infer only from the original generated name
 * catalogue, never from appearance, occupation or personality. */
final class ResidentSexLife {
    private ResidentSexLife(){}
    static final int MALE=1,FEMALE=2;
    private static final Set<String> FEMALE_NAMES=Set.of("Amelia","Giulia","Sofia","Chiara","Aurora","Beatrice","Marta","Valentina","Elisa","Irene","Sara","Noemi","Alice","Caterina","Greta","Anna","Mia","Emma");
    private static final Set<String> MALE_NAMES=Set.of("Luca","Pietro","Davide","Matteo","Nico","Alessandro","Andrea","Filippo","Riccardo","Samuele","Leonardo","Federico","Gabriele","Tommaso","Edoardo","Lorenzo","Leo","Marco");
    static int sex(TownLife.Town t,TownLife.Citizen c){
        if(c==null)return 0;
        int stored=DemographyLife.get(t,c,"sex");
        if(stored==MALE||stored==FEMALE)return stored;
        if(DemographyLife.get(t,c,"born")>0)return c.generationIndex%2==0?FEMALE:MALE;
        String first=c.name.split(" ",2)[0];
        if(FEMALE_NAMES.contains(first))return FEMALE;
        if(MALE_NAMES.contains(first))return MALE;
        return 0;
    }
    static boolean biologicalPair(TownLife.Town t,TownLife.Citizen a,TownLife.Citizen b){
        int x=sex(t,a),y=sex(t,b);return x!=0&&y!=0&&x!=y;
    }
}
