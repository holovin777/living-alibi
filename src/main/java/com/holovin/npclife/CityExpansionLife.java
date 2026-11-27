package com.holovin.npclife;

import java.util.List;
import java.util.EnumMap;
import java.util.Arrays;
import static com.holovin.npclife.TownLife.Place.*;

/** Stable coordinate registry. New blocks preserve old map coordinates and saved place IDs. */
final class CityExpansionLife {
    private CityExpansionLife(){}
    static final int WIDTH=21, HEIGHT=21;
    static final List<TownLife.Place> SOUTH=List.of(BORGO_SUD,PIAZZA_FONTANA,GIARDINI,STAZIONE,VIA_TIGLI);
    private static final TownLife.Place[][] CORE={
        {NORTH_RESIDENCES,CLINIC,SCHOOL,OFFICE,FACTORY},
        {MARKET,CENTRO,PARK,LIBRARY,WORKSHOP},
        {SOUTH_RESIDENCES,PORTICI,RIVER,WOODS,QUARRY},
        {CAFE,BAKERY,FARM,LOGISTICS_DEPOT,FUEL_STATION},
        {BORGO_SUD,PIAZZA_FONTANA,GIARDINI,STAZIONE,VIA_TIGLI}
    };
    record Cell(int x,int y){}
    private static final List<TownLife.Place> OUTER=Arrays.stream(TownLife.Place.values()).filter(CityExpansionLife::outer).toList();
    private static final EnumMap<TownLife.Place,Cell> CELLS=build();
    private static EnumMap<TownLife.Place,Cell> build(){
        var m=new EnumMap<TownLife.Place,Cell>(TownLife.Place.class);
        for(int y=0;y<CORE.length;y++)for(int x=0;x<CORE[y].length;x++)m.put(CORE[y][x],new Cell(x,y));
        for(var p:OUTER){
            String[] parts=p.name().split("_");
            m.put(p,new Cell(Integer.parseInt(parts[1]),Integer.parseInt(parts[2])));
        }
        return m;
    }
    static boolean outer(TownLife.Place p){return p.name().startsWith("OUTER_");}
    static List<TownLife.Place> outerPlaces(){return OUTER;}
    static Cell cell(TownLife.Place p){return CELLS.get(p);}
    static boolean motorDistrict(TownLife.Place p){return outer(p)||List.of(OFFICE,LIBRARY,WORKSHOP,FACTORY,QUARRY,FUEL_STATION,LOGISTICS_DEPOT).contains(p);}
    static boolean outerMotorRoad(TownLife.Place a,TownLife.Place b){return (outer(a)||outer(b))&&motorDistrict(a)&&motorDistrict(b)&&adjacent(a,b);}
    static boolean serviceHub(TownLife.Place p){var c=cell(p);return outer(p)&&c.x()%5==0&&c.y()%5==0;}
    static TownLife.Place newHome(int index){
        // Keep new households in reachable residential blocks while outer services mature.
        var residential=OUTER.stream().filter(p->p.label.startsWith("Borgata")&&cell(p).x()<=8&&cell(p).y()<=8).toList();
        return residential.get(Math.floorMod(index,residential.size()));
    }
    static boolean adjacent(TownLife.Place a,TownLife.Place b){return distance(a,b)==1;}
    static int distance(TownLife.Place a,TownLife.Place b){
        var x=cell(a);var y=cell(b);
        return x==null||y==null?-1:Math.abs(x.x()-y.x())+Math.abs(x.y()-y.y());
    }
    static TownLife.Place leisure(TownLife.Citizen c,int day){
        if((day+c.generationIndex)%4==0){
            var nearby=OUTER.stream().filter(p->distance(c.home,p)>=0&&distance(c.home,p)<=4).toList();
            if(!nearby.isEmpty())return nearby.get(Math.floorMod(c.generationIndex*13+day*7,nearby.size()));
        }
        return SOUTH.get(Math.floorMod(c.generationIndex+day,SOUTH.size()));
    }
    static int travelTime(TownLife.Place a,TownLife.Place b){
        if(a==b)return 0;
        if(outer(a)||outer(b)){int steps=distance(a,b);return steps>0?8*steps:24;}
        if(SOUTH.contains(a)||SOUTH.contains(b)){
            if(adjacent(a,b))return 8;
            return a==PARK||b==PARK?18:14;
        }
        return a==PARK||b==PARK?10:8;
    }
}
