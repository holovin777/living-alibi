package com.holovin.npclife;

import java.util.List;

/**
 * Finite industrial supply chain, with staffed work, conserved cash and
 * bounded freight. Quantities are deliberately abstract gameplay units.
 *
 * A truck manifests at 11:00 and unloads 30 minutes later, only when a
 * factory worker can receive the shipment. Repeating either minute after
 * restart cannot produce another batch. Salvage is a separate finite stock.
 */
final class IndustryLife {
    private IndustryLife() { }

    static final int SURVEY_MINUTE=525;      // 08:45
    static final int EXTRACT_MINUTE=540;     // 09:00
    static final int TRANSPORT_MINUTE=660;   // 11:00
    static final int ARRIVE_MINUTE=690;      // 11:30
    static final int REFINE_MINUTE=720;      // 12:00
    static final int RECYCLE_MINUTE=780;     // 13:00
    static final int FABRICATE_MINUTE=840;   // 14:00
    static final int DELIVER_MINUTE=900;     // 15:00

    private static final String EARTH_IRON="seam:iron-ore";
    private static final String EARTH_COPPER="seam:copper-ore";
    private static final String WEST_IRON="seam:west-iron-ore";
    private static final String WEST_COPPER="seam:west-copper-ore";
    private static final String WEST_KNOWN="survey:west-discovered";
    private static final String QUARRY_IRON="quarry:iron-ore";
    private static final String QUARRY_COPPER="quarry:copper-ore";
    private static final String TRUCK_IRON="truck:iron-ore";
    private static final String TRUCK_COPPER="truck:copper-ore";
    private static final String FACTORY_IRON="factory:iron-ore";
    private static final String FACTORY_COPPER_ORE="factory:copper-ore";
    private static final String FACTORY_STEEL="factory:steel";
    private static final String FACTORY_COPPER="factory:copper";
    private static final String FACTORY_PARTS="factory:spare-parts";
    private static final String SALVAGE="factory:scrap-metal";

    static final int TRUCK_CAPACITY=3;
    static final int SURVEY_COST=30;
    static final int HOURLY_WAGE=11;

    static void ensure(TownLife.Town town) {
        // An existing zero means exhausted. Hidden deposits physically exist
        // from world creation; discovery unlocks them, not magically refills them.
        town.industryStocks.putIfAbsent(EARTH_IRON,80);
        town.industryStocks.putIfAbsent(EARTH_COPPER,40);
        town.industryStocks.putIfAbsent(WEST_IRON,30);
        town.industryStocks.putIfAbsent(WEST_COPPER,15);
        town.industryStocks.putIfAbsent(WEST_KNOWN,0);
        town.industryStocks.putIfAbsent(QUARRY_IRON,0);
        town.industryStocks.putIfAbsent(QUARRY_COPPER,0);
        town.industryStocks.putIfAbsent(TRUCK_IRON,0);
        town.industryStocks.putIfAbsent(TRUCK_COPPER,0);
        town.industryStocks.putIfAbsent(FACTORY_IRON,0);
        town.industryStocks.putIfAbsent(FACTORY_COPPER_ORE,0);
        town.industryStocks.putIfAbsent(FACTORY_STEEL,0);
        town.industryStocks.putIfAbsent(FACTORY_COPPER,0);
        town.industryStocks.putIfAbsent(FACTORY_PARTS,0);
        // Pre-existing discarded machinery from the town's old industries;
        // it is not daily replenished and recovery is deliberately lossy.
        town.industryStocks.putIfAbsent(SALVAGE,10);
    }

    record Report(int ironRemaining,int copperRemaining,int oreAtQuarry,
                  int oreAtFactory,int factorySteel,int factoryCopper,
                  int factoryParts,int workshopSteel,int workshopCopper,
                  int workshopParts,int minersAtWork,int factoryWorkersAtWork,
                  int oreOnTruck,int scrapRemaining,boolean westDepositDiscovered,
                  int quarryPayroll,int factoryPayroll,int cityBudget) { }

    static Report report(TownLife.Town town) {
        return new Report(stock(town,EARTH_IRON)+
                (discovered(town)?stock(town,WEST_IRON):0),
                stock(town,EARTH_COPPER)+
                (discovered(town)?stock(town,WEST_COPPER):0),
                stock(town,QUARRY_IRON)+stock(town,QUARRY_COPPER),
                stock(town,FACTORY_IRON)+stock(town,FACTORY_COPPER_ORE),
                stock(town,FACTORY_STEEL),stock(town,FACTORY_COPPER),
                stock(town,FACTORY_PARTS),
                town.technicalMaterials.getOrDefault("steel",0),
                town.technicalMaterials.getOrDefault("copper",0),
                town.technicalMaterials.getOrDefault("spare-parts",0),
                workers(town,TownLife.Place.QUARRY,"Miner"),
                workers(town,TownLife.Place.FACTORY,"Factory machinist"),
                stock(town,TRUCK_IRON)+stock(town,TRUCK_COPPER),
                stock(town,SALVAGE),discovered(town),
                town.employerFunds.getOrDefault("miner",0),
                town.employerFunds.getOrDefault("machinist",0),
                town.employerFunds.getOrDefault("city-maintenance",0));
    }

    static int stock(TownLife.Town town,String id) {
        return town.industryStocks.getOrDefault(id,0);
    }

    private static boolean discovered(TownLife.Town town) {
        return stock(town,WEST_KNOWN)>0;
    }

    /** Actual finite money transfer. The sum of payer and payee stays fixed. */
    private static boolean transfer(TownLife.Town town,String from,String to,int amount) {
        if(amount<=0)return false;
        int available=town.employerFunds.getOrDefault(from,0);
        if(available<amount)return false;
        town.employerFunds.put(from,available-amount);
        town.employerFunds.put(to,Math.addExact(town.employerFunds.getOrDefault(to,0),amount));
        return true;
    }

    /** Counts units at each physical stage; a zero-valued source stays zero. */
    private static int move(TownLife.Town town,String from,String to,int limit) {
        int moved=Math.min(Math.max(0,limit),stock(town,from));
        if(moved==0)return 0;
        town.industryStocks.put(from,stock(town,from)-moved);
        town.industryStocks.put(to,Math.addExact(stock(town,to),moved));
        return moved;
    }

    private static int deliver(TownLife.Town town,String factoryId,String product,int amount) {
        if(amount<=0)return 0;
        town.industryStocks.put(factoryId,stock(town,factoryId)-amount);
        town.technicalMaterials.put(product,
                Math.addExact(town.technicalMaterials.getOrDefault(product,0),amount));
        return amount;
    }

    private static TownLife.Citizen attending(TownLife.Town town,
                                              TownLife.Place site,String job) {
        return town.people.values().stream()
                .filter(c->c.generationIndex>0 && c.alive && c.employed &&
                        c.occupation.equals(job) && c.workplace==site &&
                        c.presentAt(site) && c.activity.equals("WORKING"))
                .findFirst().orElse(null);
    }

    private static boolean funded(TownLife.Town town,String role) {
        return town.employerFunds.getOrDefault(role,0)>=HOURLY_WAGE;
    }

    private static int workers(TownLife.Town town,TownLife.Place site,String job) {
        return (int)town.people.values().stream()
                .filter(c->c.generationIndex>0 && c.alive && c.employed &&
                        c.occupation.equals(job) && c.workplace==site &&
                        c.presentAt(site) && c.activity.equals("WORKING"))
                .count();
    }

    static String companyFor(String occupation) {
        return switch(occupation) {
            case "Miner", "Transport driver" -> "miner";
            case "Factory machinist" -> "machinist";
            default -> null;
        };
    }

    /** NPC hourly pay and visitor wages draw from the SAME enterprise purse. */
    static boolean payHourly(TownLife.Town town,TownLife.Citizen worker,
                             WorldState clock,List<TownLife.Event> events,
                             List<TownLife.Observation> observations) {
        String employer=companyFor(worker.occupation);
        if(employer==null)return false;
        int balance=town.employerFunds.getOrDefault(employer,0);
        if(balance<HOURLY_WAGE)return true; // Industrial wage unpaid; no magic cash.
        town.employerFunds.put(employer,balance-HOURLY_WAGE);
        worker.money=Math.addExact(worker.money,HOURLY_WAGE);
        TownLife.publish(town,clock,worker.id,worker.place,
                worker.name+" received EUR "+HOURLY_WAGE+
                " hourly pay from "+employer+" enterprise funds.",
                events,observations);
        return true;
    }

    private static String seam(TownLife.Town town,boolean iron) {
        String original=iron?EARTH_IRON:EARTH_COPPER;
        if(stock(town,original)>0)return original;
        return discovered(town)?(iron?WEST_IRON:WEST_COPPER):original;
    }

    static String prospect(TownLife.Town town) {
        if(discovered(town))
            throw new IllegalArgumentException("The western mineral seam has already been surveyed.");
        if(!transfer(town,"miner","survey-office",SURVEY_COST))
            throw new IllegalArgumentException("Quarry cannot afford the 30 EUR survey.");
        town.industryStocks.put(WEST_KNOWN,1);
        return "Western ore seam discovered: 30 finite iron and 15 copper units.";
    }

    /** Geological survey is tied to player presence and employer funding. */
    static String prospectPlayer(TownLife.Town town,int minute) {
        var p=town.player;
        PlayerLife.requireAlive(p);
        if(p.place!=TownLife.Place.QUARRY || p.travelRemaining>0 ||
                !"miner".equals(p.jobId))
            throw new IllegalArgumentException("Only a hired miner at the quarry can prospect.");
        if(minute<480 || minute+120>1080)
            throw new IllegalArgumentException("Surveys take two hours during 08:00–18:00.");
        return prospect(town);
    }

    /** A player's paid industrial shift must create physical output first. */
    static String playerShift(TownLife.Town town,String jobId) {
        if("miner".equals(jobId)) {
            int iron=move(town,seam(town,true),QUARRY_IRON,1);
            int copper=move(town,seam(town,false),QUARRY_COPPER,1);
            if(iron+copper==0)
                throw new IllegalArgumentException("Quarry seams are exhausted. Survey more geology.");
            return "Extracted "+iron+" iron and "+copper+" copper ore units.";
        }
        if("machinist".equals(jobId)) {
            int steel=move(town,FACTORY_IRON,FACTORY_STEEL,1);
            int copper=move(town,FACTORY_COPPER_ORE,FACTORY_COPPER,1);
            if(steel+copper==0 && stock(town,SALVAGE)>=2) {
                town.industryStocks.put(SALVAGE,stock(town,SALVAGE)-2);
                town.industryStocks.put(FACTORY_STEEL,stock(town,FACTORY_STEEL)+1);
                return "Recycled 2 finite scrap units into 1 steel unit.";
            }
            if(steel+copper==0)
                throw new IllegalArgumentException("No raw ore or recyclable scrap at the factory.");
            return "Refined "+steel+" steel and "+copper+" copper units.";
        }
        return "";
    }

    /**
     * All steps occur after NPC life movement/attendance. Time stamps survive
     * SQLite restart and no transition may run twice on a given game day.
     */
    static void tick(TownLife.Town town,WorldState clock,
                     List<TownLife.Event> events,List<TownLife.Observation> observations) {
        int day=clock.day,minute=clock.minute;
        if(minute==SURVEY_MINUTE && stock(town,EARTH_IRON)==0 &&
                stock(town,EARTH_COPPER)==0 && !discovered(town) &&
                attending(town,TownLife.Place.QUARRY,"Miner")!=null &&
                funded(town,"miner") &&
                town.employerFunds.getOrDefault("miner",0)>=SURVEY_COST+HOURLY_WAGE) {
            prospect(town);
            var miner=attending(town,TownLife.Place.QUARRY,"Miner");
            TownLife.publish(town,clock,miner.id,TownLife.Place.QUARRY,
                    miner.name+" discovered a finite western ore deposit after a paid survey.",
                    events,observations);
        }
        if(minute==EXTRACT_MINUTE && day>town.lastIndustryExtractDay) {
            town.lastIndustryExtractDay=day;
            var miner=attending(town,TownLife.Place.QUARRY,"Miner");
            if(miner!=null && funded(town,"miner")) {
                int iron=move(town,seam(town,true),QUARRY_IRON,2);
                int copper=move(town,seam(town,false),QUARRY_COPPER,1);
                if(iron+copper>0)
                    TownLife.publish(town,clock,miner.id,TownLife.Place.QUARRY,
                            miner.name+" extracted "+iron+" iron ore and "+copper+
                            " copper ore from finite geological seams.",events,observations);
            }
        }
        if(minute==TRANSPORT_MINUTE && day>town.lastIndustryTransportDay) {
            town.lastIndustryTransportDay=day;
            var miner=attending(town,TownLife.Place.QUARRY,"Miner");
            var driver=attending(town,TownLife.Place.QUARRY,"Transport driver");
            int freight= Math.min(2,stock(town,QUARRY_IRON))+
                         Math.min(1,stock(town,QUARRY_COPPER));
            int space=TRUCK_CAPACITY-stock(town,TRUCK_IRON)-stock(town,TRUCK_COPPER);
            if(miner!=null && driver!=null && funded(town,"miner") && space>=freight &&
                    freight>0 && town.employerFunds.getOrDefault("machinist",0)>=3*freight &&
                    transfer(town,"machinist","miner",3*freight)) {
                int iron=move(town,QUARRY_IRON,TRUCK_IRON,2);
                int copper=move(town,QUARRY_COPPER,TRUCK_COPPER,1);
                TownLife.publish(town,clock,driver.id,TownLife.Place.QUARRY,
                        driver.name+" dispatched a limited 3-unit truck with "+
                        iron+" iron and "+copper+" copper ore; factory paid quarry EUR "+
                        (3*freight)+".",events,observations);
            }
        }
        if(minute==ARRIVE_MINUTE && day>town.lastIndustryArrivalDay) {
            town.lastIndustryArrivalDay=day;
            var machinist=attending(town,TownLife.Place.FACTORY,"Factory machinist");
            if(machinist!=null && funded(town,"machinist")) {
                int iron=move(town,TRUCK_IRON,FACTORY_IRON,TRUCK_CAPACITY);
                int copper=move(town,TRUCK_COPPER,FACTORY_COPPER_ORE,TRUCK_CAPACITY);
                if(iron+copper>0)
                    TownLife.publish(town,clock,machinist.id,TownLife.Place.FACTORY,
                            machinist.name+" received a truck delivery of "+iron+
                            " iron and "+copper+" copper ore after 30 minutes in transit.",
                            events,observations);
            }
        }
        if(minute==REFINE_MINUTE && day>town.lastIndustryRefineDay) {
            town.lastIndustryRefineDay=day;
            var machinist=attending(town,TownLife.Place.FACTORY,"Factory machinist");
            if(machinist!=null && funded(town,"machinist")) {
                int steel=move(town,FACTORY_IRON,FACTORY_STEEL,2);
                int copper=move(town,FACTORY_COPPER_ORE,FACTORY_COPPER,1);
                if(steel+copper>0)
                    TownLife.publish(town,clock,machinist.id,TownLife.Place.FACTORY,
                            machinist.name+" refined "+steel+" steel and "+copper+
                            " copper from mined ore.",events,observations);
            }
        }
        if(minute==RECYCLE_MINUTE && day>town.lastIndustryRecycleDay) {
            town.lastIndustryRecycleDay=day;
            var machinist=attending(town,TownLife.Place.FACTORY,"Factory machinist");
            if(machinist!=null && funded(town,"machinist") && stock(town,SALVAGE)>=2) {
                town.industryStocks.put(SALVAGE,stock(town,SALVAGE)-2);
                town.industryStocks.put(FACTORY_STEEL,
                        Math.addExact(stock(town,FACTORY_STEEL),1));
                TownLife.publish(town,clock,machinist.id,TownLife.Place.FACTORY,
                        machinist.name+" recovered 1 steel unit from 2 units of finite industrial scrap.",
                        events,observations);
            }
        }
        if(minute==FABRICATE_MINUTE && day>town.lastIndustryFabricateDay) {
            town.lastIndustryFabricateDay=day;
            var machinist=attending(town,TownLife.Place.FACTORY,"Factory machinist");
            if(machinist!=null && funded(town,"machinist") && stock(town,FACTORY_STEEL)>=1) {
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
            if(machinist!=null && funded(town,"machinist")) {
                int parts=Math.min(2,stock(town,FACTORY_PARTS));
                int steel=Math.min(1,stock(town,FACTORY_STEEL));
                int copper=Math.min(1,stock(town,FACTORY_COPPER));
                int bill=5*parts+4*steel+5*copper;
                if(bill>0 && transfer(town,"city-maintenance","machinist",bill)) {
                    deliver(town,FACTORY_PARTS,"spare-parts",parts);
                    deliver(town,FACTORY_STEEL,"steel",steel);
                    deliver(town,FACTORY_COPPER,"copper",copper);
                    TownLife.publish(town,clock,machinist.id,TownLife.Place.FACTORY,
                            machinist.name+" dispatched "+parts+" spare parts, "+steel+
                            " steel and "+copper+" copper to city maintenance for EUR "+
                            bill+" of its finite city budget.",events,observations);
                }
            }
        }
    }
}
