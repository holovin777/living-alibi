package com.holovin.npclife;

/** Renewable but bounded crops; every shipment is paid out of existing retail funds.
 *  Stored in the existing persisted physical stock ledger, including daily markers. */
final class FarmLife {
    private FarmLife() { }
    static final String ACCOUNT="farm-worker";
    static final String[] CROPS={"vegetables","rice"};
    static String field(String crop){return "farm:field:"+crop;}
    static String barn(String crop){return "farm:barn:"+crop;}
    static int stock(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    static void set(TownLife.Town t,String key,int n){t.industryStocks.put(key,n);}
    static void ensure(TownLife.Town t) {
        for(String crop:CROPS) {
            t.industryStocks.putIfAbsent(field(crop),12);
            t.industryStocks.putIfAbsent(barn(crop),0);
        }
        for(String stage:new String[]{"grow","harvest","delivery"})
            t.industryStocks.putIfAbsent("farm:day:"+stage,0);
    }
    static boolean local(String crop){return "vegetables".equals(crop)||"rice".equals(crop);}
    static boolean once(TownLife.Town t,String stage,int day) {
        String key="farm:day:"+stage;
        if(stock(t,key)>=day)return false;
        set(t,key,day);return true;
    }
    static int harvest(TownLife.Town t,int perCrop) {
        int total=0;
        for(String crop:CROPS) {
            int n=Math.min(perCrop,Math.min(stock(t,field(crop)),24-stock(t,barn(crop))-FreightLife.reservedCrop(t,crop)-(crop.equals("vegetables")?StallSupplyLife.reserved(t):0)));
            if(n<=0)continue;
            set(t,field(crop),stock(t,field(crop))-n);
            set(t,barn(crop),stock(t,barn(crop))+n);
            total+=n;
        }
        return total;
    }
    static String playerShift(TownLife.Town t) {
        int n=harvest(t,4);
        if(n==0)throw new IllegalArgumentException("No ripe crops or barn storage available.");
        return "Harvested "+n+" crop units into the farm barn.";
    }
    static int deliver(TownLife.Town t) {
        int moved=0;
        for(String crop:CROPS) {
            var product=RetailLife.product(crop);
            int cost=product.price()-1;
            int budget=t.employerFunds.getOrDefault("shop-assistant",0);
            int n=Math.min(6-moved,Math.min(stock(t,barn(crop)),
                    Math.min(product.capacity()-t.retailStock.getOrDefault(crop,0)-FreightLife.reservedCrop(t,crop),budget/cost)));
            if(n<=0)continue;
            set(t,barn(crop),stock(t,barn(crop))-n);
            t.retailStock.merge(crop,n,Integer::sum);
            t.employerFunds.put("shop-assistant",budget-n*cost);
            t.employerFunds.merge(ACCOUNT,n*cost,Math::addExact);
            moved+=n;
        }
        return moved;
    }
    static void tick(TownLife.Town t,int day,int minute) {
        if(minute==360 && once(t,"grow",day)) {
            var weather=WeatherLife.at(day,minute);
            int veg=switch(weather.season()) {
                case SPRING,SUMMER -> 4;
                case AUTUMN -> 2;
                case WINTER -> 1;
            };
            int rice=switch(weather.season()) {
                case SPRING -> 2;
                case SUMMER -> 4;
                case AUTUMN -> 2;
                case WINTER -> 0;
            };
            veg=FarmModernizationLife.cropGrowth(t,veg,false);
            rice=FarmModernizationLife.cropGrowth(t,rice,true);
            set(t,field("vegetables"),Math.min(24,stock(t,field("vegetables"))+veg));
            set(t,field("rice"),Math.min(24,stock(t,field("rice"))+rice));
        }
        // Abstract contracted harvest service, not a fabricated named NPC.
        if(minute==480 && once(t,"harvest",day) && t.employerFunds.getOrDefault(ACCOUNT,0)>=4) {
            int n=harvest(t,3);
            if(n>0) {
                t.employerFunds.merge(ACCOUNT,-4,Integer::sum);
                t.employerFunds.merge("farm-contractors",4,Math::addExact);
            }
        }
        if(minute==660 && once(t,"delivery",day))deliver(t);
    }
    record Report(int vegetablesInField,int riceInField,int vegetablesInBarn,int riceInBarn,
                  int payroll,int marketVegetables,int marketRice) { }
    static Report report(TownLife.Town t) {
        return new Report(stock(t,field("vegetables")),stock(t,field("rice")),
                stock(t,barn("vegetables")),stock(t,barn("rice")),
                t.employerFunds.getOrDefault(ACCOUNT,0),
                t.retailStock.getOrDefault("vegetables",0),t.retailStock.getOrDefault("rice",0));
    }
}
