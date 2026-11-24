package com.holovin.npclife;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A small material-conserving industry chain, driven by actual NPC attendance.
 *
 * FINITE ORE SEAM -> QUARRY INVENTORY -> FACTORY ORE -> REFINED METAL
 * -> FABRICATED PARTS -> SHARED WORKSHOP MATERIALS.
 *
 * All units are simplified fictional game units. No natural seam replenishment,
 * automatic component creation, remote mining or free daily stock refill.
 */
final class IndustryLife {
    private IndustryLife() { }

    static final int EXTRACT_MINUTE=540;     // 09:00
    static final int TRANSPORT_MINUTE=660;   // 11:00
    static final int REFINE_MINUTE=720;      // 12:00
    static final int FABRICATE_MINUTE=840;   // 14:00
    static final int DELIVER_MINUTE=900;     // 15:00

    private static final String EARTH_IRON="seam:iron-ore";
    private static final String EARTH_COPPER="seam:copper-ore";
    private static final String QUARRY_IRON="quarry:iron-ore";
    private static final String QUARRY_COPPER="quarry:copper-ore";
    private static final String FACTORY_IRON="factory:iron-ore";
    private static final String FACTORY_COPPER_ORE="factory:copper-ore";
    private static final String FACTORY_STEEL="factory:steel";
    private static final String FACTORY_COPPER="factory:copper";
    private static final String FACTORY_PARTS="factory:spare-parts";

    static void ensure(TownLife.Town town) {
        // An existing zero is meaningful: never reset exhausted seams or inventories.
        town.industryStocks.putIfAbsent(EARTH_IRON,80);
        town.industryStocks.putIfAbsent(EARTH_COPPER,40);
        town.industryStocks.putIfAbsent(QUARRY_IRON,0);
        town.industryStocks.putIfAbsent(QUARRY_COPPER,0);
        town.industryStocks.putIfAbsent(FACTORY_IRON,0);
        town.industryStocks.putIfAbsent(FACTORY_COPPER_ORE,0);
        town.industryStocks.putIfAbsent(FACTORY_STEEL,0);
        town.industryStocks.putIfAbsent(FACTORY_COPPER,0);
        town.industryStocks.putIfAbsent(FACTORY_PARTS,0);
    }

    record Report(int ironRemaining,int copperRemaining,int oreAtQuarry,
                  int oreAtFactory,int factorySteel,int factoryCopper,
                  int factoryParts,int workshopSteel,int workshopCopper,
                  int workshopParts,int minersAtWork,int factoryWorkersAtWork) { }

    static Report report(TownLife.Town town) {
        return new Report(stock(town,EARTH_IRON),stock(town,EARTH_COPPER),
                stock(town,QUARRY_IRON)+stock(town,QUARRY_COPPER),
                stock(town,FACTORY_IRON)+stock(town,FACTORY_COPPER_ORE),
                stock(town,FACTORY_STEEL),stock(town,FACTORY_COPPER),
                stock(town,FACTORY_PARTS),
                town.technicalMaterials.getOrDefault("steel",0),
                town.technicalMaterials.getOrDefault("copper",0),
                town.technicalMaterials.getOrDefault("spare-parts",0),
                workers(town,TownLife.Place.QUARRY,"Miner"),
                workers(town,TownLife.Place.FACTORY,"Factory machinist"));
    }

    static int stock(TownLife.Town town,String id) {
        return town.industryStocks.getOrDefault(id,0);
    }

    /** Transfer real units from one finite source to another, with a bounded batch. */
    private static int move(TownLife.Town town,String from,String to,int limit) {
        int moved=Math.min(Math.max(0,limit),stock(town,from));
        if(moved==0)return 0;
        town.industryStocks.put(from,stock(town,from)-moved);
        town.industryStocks.put(to,Math.addExact(stock(town,to),moved));
        return moved;
    }

    private static int deliver(TownLife.Town town,String factoryId,String product,int limit) {
        int quantity=Math.min(limit,stock(town,factoryId));
        if(quantity<=0)return 0;
        town.industryStocks.put(factoryId,stock(town,factoryId)-quantity);
        town.technicalMaterials.put(product,
                Math.addExact(town.technicalMaterials.getOrDefault(product,0),quantity));
        return quantity;
    }

    private static TownLife.Citizen attending(TownLife.Town town,
                                              TownLife.Place site,String job) {
        return town.people.values().stream()
                .filter(c->c.generationIndex>0 && c.alive && c.employed &&
                        c.occupation.equals(job) && c.workplace==site &&
                        c.presentAt(site) && c.activity.equals("WORKING"))
                .findFirst().orElse(null);
    }

    private static int workers(TownLife.Town town,TownLife.Place site,String job) {
        return (int)town.people.values().stream()
                .filter(c->c.generationIndex>0 && c.alive && c.employed &&
                        c.occupation.equals(job) && c.workplace==site &&
                        c.presentAt(site) && c.activity.equals("WORKING"))
                .count();
    }

    /**
     * Runs at precisely one world-clock minute per stage, in the normal NPC
     * update loop. Persisted day stamps prevent replay after save/reload.
     */
    static void tick(TownLife.Town town,WorldState clock,
                     java.util.List<TownLife.Event> events,
                     java.util.List<TownLife.Observation> observations) {
        int day=clock.day,minute=clock.minute;
        if(minute==EXTRACT_MINUTE && day>town.lastIndustryExtractDay) {
            town.lastIndustryExtractDay=day;
            var miner=attending(town,TownLife.Place.QUARRY,"Miner");
            if(miner!=null) {
                int iron=move(town,EARTH_IRON,QUARRY_IRON,2);
                int copper=move(town,EARTH_COPPER,QUARRY_COPPER,1);
                if(iron+copper>0)
                    TownLife.publish(town,clock,miner.id,TownLife.Place.QUARRY,
                            miner.name+" extracted "+iron+" iron ore and "+copper+
                            " copper ore from finite geological seams.",events,observations);
            }
        }
        if(minute==TRANSPORT_MINUTE && day>town.lastIndustryTransportDay) {
            town.lastIndustryTransportDay=day;
            var miner=attending(town,TownLife.Place.QUARRY,"Miner");
            var machinist=attending(town,TownLife.Place.FACTORY,"Factory machinist");
            if(miner!=null && machinist!=null) {
                int iron=move(town,QUARRY_IRON,FACTORY_IRON,2);
                int copper=move(town,QUARRY_COPPER,FACTORY_COPPER_ORE,1);
                if(iron+copper>0)
                    TownLife.publish(town,clock,machinist.id,TownLife.Place.FACTORY,
                            machinist.name+" received "+iron+" iron ore and "+copper+
                            " copper ore from the quarry.",events,observations);
            }
        }
        if(minute==REFINE_MINUTE && day>town.lastIndustryRefineDay) {
            town.lastIndustryRefineDay=day;
            var machinist=attending(town,TownLife.Place.FACTORY,"Factory machinist");
            if(machinist!=null) {
                int steel=move(town,FACTORY_IRON,FACTORY_STEEL,2);
                int copper=move(town,FACTORY_COPPER_ORE,FACTORY_COPPER,1);
                if(steel+copper>0)
                    TownLife.publish(town,clock,machinist.id,TownLife.Place.FACTORY,
                            machinist.name+" refined "+steel+" steel and "+copper+
                            " copper from mined ore.",events,observations);
            }
        }
        if(minute==FABRICATE_MINUTE && day>town.lastIndustryFabricateDay) {
            town.lastIndustryFabricateDay=day;
            var machinist=attending(town,TownLife.Place.FACTORY,"Factory machinist");
            if(machinist!=null && stock(town,FACTORY_STEEL)>=1) {
                town.industryStocks.put(FACTORY_STEEL,stock(town,FACTORY_STEEL)-1);
                town.industryStocks.put(FACTORY_PARTS,
                        Math.addExact(stock(town,FACTORY_PARTS),2));
                TownLife.publish(town,clock,machinist.id,TownLife.Place.FACTORY,
                        machinist.name+" fabricated 2 spare parts using 1 unit of steel.",
                        events,observations);
            }
        }
        if(minute==DELIVER_MINUTE && day>town.lastIndustryDeliverDay) {
            town.lastIndustryDeliverDay=day;
            var machinist=attending(town,TownLife.Place.FACTORY,"Factory machinist");
            if(machinist!=null) {
                int parts=deliver(town,FACTORY_PARTS,"spare-parts",2);
                int steel=deliver(town,FACTORY_STEEL,"steel",1);
                int copper=deliver(town,FACTORY_COPPER,"copper",1);
                if(parts+steel+copper>0)
                    TownLife.publish(town,clock,machinist.id,TownLife.Place.FACTORY,
                            machinist.name+" dispatched "+parts+" spare parts, "+steel+
                            " steel and "+copper+" copper to city maintenance.",
                            events,observations);
            }
        }
    }
}
