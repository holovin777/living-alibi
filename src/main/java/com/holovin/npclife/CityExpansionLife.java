package com.holovin.npclife;

import java.util.List;
import static com.holovin.npclife.TownLife.Place.*;

/** Southern extension. Old travel times and enum ordering stay compatible with saved games. */
final class CityExpansionLife {
    private CityExpansionLife(){}
    static final List<TownLife.Place> SOUTH=List.of(BORGO_SUD,PIAZZA_FONTANA,GIARDINI,STAZIONE,VIA_TIGLI);
    static TownLife.Place leisure(TownLife.Citizen c,int day){
        return SOUTH.get(Math.floorMod(c.generationIndex+day,SOUTH.size()));
    }
    static int travelTime(TownLife.Place a,TownLife.Place b){
        if(a==b)return 0;
        if(SOUTH.contains(a)||SOUTH.contains(b)){
            if(RebirthLife.adjacent(a,b))return 8;
            return a==PARK||b==PARK?18:14;
        }
        return a==PARK||b==PARK?10:8;
    }
}
