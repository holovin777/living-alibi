package com.holovin.npclife;
import java.util.*;

/** Geographic controls for existing home agriculture and property investments. */
final class GeoProperty {
    record View(GardenLife.Report garden,HenLife.Report hens,OrchardLife.Report orchard,
                FarmLife.Report farm,RentalLife.Report rental,ConstructionLife.Report construction,
                List<GeoProjection.Choice> actions){}
    record Yard(GeoProjection.District anchor,GardenLife.Report garden,HenLife.Report hens,OrchardLife.Report orchard){}
    record Site(GeoProjection.District anchor,int stage,boolean active,boolean owned){}
    record Scenery(Yard yard,Site site){}
    static Scenery scenery(TownLife.Town t,WorldState s,GeoProjection map,GeoProjection.Point at,TownLife.Place room){
        if(room!=null)return new Scenery(null,null);
        Yard yard=null;Site site=null;
        if(t.player.alive&&t.player.ownsHouse&&!RebirthLife.active(t)&&map.point(HousingLife.HOUSE_DISTRICT).distance(at)<=80)
            yard=new Yard(map.district(HousingLife.HOUSE_DISTRICT),GardenLife.report(t,s.day,s.minute),HenLife.report(t,s.day),OrchardLife.report(t,s.day,s.minute));
        if(map.point(TownLife.Place.OFFICE).distance(at)<=110){
            var b=ConstructionLife.report(t);
            // The original building stays after sale or owner death.
            site=new Site(map.district(TownLife.Place.OFFICE),b.stage(),b.enabled(),b.owned());
        }
        return new Scenery(yard,site);
    }
    static View view(TownLife.Town t,WorldState s,GeoProjection map,GeoProjection.Point at,TownLife.Place room){
        var p=t.player;var a=new ArrayList<GeoProjection.Choice>();
        if(!p.alive||RebirthLife.active(t)||JusticeLife.confined(t)||p.travelRemaining>0||p.cycling
                ||IntercomLife.insideVisit(t,s.day,s.minute)||map.point(p.place).distance(at)>18)
            return new View(null,null,null,null,null,null,List.of());
        GardenLife.Report garden=null;HenLife.Report hens=null;OrchardLife.Report orchard=null;
        RentalLife.Report rental=null;ConstructionLife.Report construction=null;
        FarmLife.Report farm=p.place==TownLife.Place.FARM&&room==null?FarmLife.report(t):null;
        int water=FoodLife.quantity(p,"water",false),space=24-FoodLife.total(p,true);
        if(p.ownsHouse&&p.place==HousingLife.HOUSE_DISTRICT&&room==null){
            garden=GardenLife.report(t,s.day,s.minute);hens=HenLife.report(t,s.day);orchard=OrchardLife.report(t,s.day,s.minute);
            if(!garden.planted()&&(!garden.winter()||garden.greenhouse())&&p.money>=garden.seedPrice())add(a,"plant","Plant vegetables · €"+garden.seedPrice());
            if(!garden.greenhouse()&&p.money>=garden.greenhousePrice())add(a,"build-greenhouse","Build greenhouse · €"+garden.greenhousePrice());
            if(!garden.barrel()&&p.money>=garden.barrelPrice())add(a,"buy-barrel","Install rain barrel · €"+garden.barrelPrice());
            if(!garden.composter()&&p.money>=garden.composterPrice())add(a,"build-composter","Build composter · €"+garden.composterPrice());
            if(garden.composter()&&garden.scraps()>=GardenLife.COMPOST_INPUT&&garden.compostDueDay()==0&&garden.compost()==0)add(a,"start-compost","Start compost");
            if(garden.planted()&&garden.growth()<garden.required()){
                if(garden.compost()>0&&!garden.fertilized())add(a,"fertilize","Apply compost");
                if((!garden.winter()||garden.greenhouse())&&(!garden.rainy()||garden.greenhouse())&&!garden.wateredToday()&&s.minute<1080){
                    if(water>0)add(a,"water","Water vegetables");
                    if(garden.rainwater()>0)add(a,"water-rain","Water vegetables from barrel");
                }
            }
            if(garden.planted()&&garden.growth()>=garden.required()&&space>=garden.harvestUnits())add(a,"harvest","Harvest vegetables · "+garden.harvestUnits()+" units");
            if(!hens.coop()&&p.money>=hens.coopPrice())add(a,"build-coop","Build hen coop · €"+hens.coopPrice());
            if(hens.coop()){
                if(hens.hens()<hens.maxHens()&&!hens.fedToday()&&!hens.wateredToday()&&p.money>=hens.henPrice())add(a,"buy-hen","Buy hen · €"+hens.henPrice());
                if(hens.hens()>0&&s.minute<1080){
                    if(!hens.fedToday()&&FoodLife.quantity(p,"rice",false)>0)add(a,"feed-hens","Feed hens");
                    if(!hens.wateredToday()&&water>0)add(a,"water-hens","Water hens");
                }
                if(hens.eggs()>0&&space>0)add(a,"collect-eggs","Collect egg");
            }
            for(var plant:orchard.plants()){
                if(!plant.planted()&&!orchard.winter()&&!orchard.wateredToday()&&p.money>=plant.price())add(a,"orchard-plant-"+plant.id(),"Plant "+plant.label()+" · €"+plant.price());
                if(plant.ready()&&space>=OrchardLife.YIELD+(plant.pollinated()?1:0))add(a,"orchard-harvest-"+plant.id(),"Harvest "+plant.label());
            }
            boolean growing=orchard.plants().stream().anyMatch(plant->plant.planted()&&(plant.age()<OrchardLife.MATURE||
                plant.season().equals(CalendarLife.season(s.day).name())&&!plant.harvestedThisYear()&&plant.fruit()<OrchardLife.RIPE));
            if(growing&&!orchard.winter()&&!orchard.rainy()&&!orchard.wateredToday()&&s.minute<1080){
                if(water>0)add(a,"orchard-water","Water orchard");
                if(garden.rainwater()>0)add(a,"orchard-water-rain","Water orchard from barrel");
            }
            if(!orchard.hive()&&p.money>=orchard.hivePrice())add(a,"orchard-build-hive","Install beehive · €"+orchard.hivePrice());
            if(orchard.hive()&&!orchard.winter()&&!orchard.beesWateredToday()&&s.minute<1080&&water>0)add(a,"orchard-water-bees","Water bees");
            if(orchard.honey()>0&&space>0)add(a,"orchard-collect-honey","Collect honey");
            if(!orchard.cellar()&&p.money>=orchard.cellarPrice())add(a,"orchard-build-cellar","Install cellar · €"+orchard.cellarPrice());
            if(orchard.cellar()){
                int grapes=FoodLife.quantity(p,"grapes",true);
                if(grapes>=2)add(a,"orchard-press-juice","Press grape juice");
                if(grapes>=3&&orchard.wineDueDay()==0&&!orchard.wineReady())add(a,"orchard-start-wine","Start wine batch");
                if(orchard.wineReady()&&space>0)add(a,"orchard-collect-wine","Collect wine");
            }
        }
        if(p.place==TownLife.Place.MARKET&&room==p.place&&s.minute>=480&&s.minute<1200){
            int cash=t.employerFunds.getOrDefault("shop-assistant",0);
            sell(a,p,"vegetables","sell-vegetables",RetailLife.produceSpace(t),2,cash);
            sell(a,p,"eggs","sell-eggs",RetailLife.eggSpace(t),RetailLife.EGG_PRICE,cash);
            for(var crop:OrchardLife.CROPS)sell(a,p,crop.id(),"sell-fruit-"+crop.id(),RetailLife.fruitSpace(t,crop.id()),OrchardLife.SELL_PRICE,cash);
            sell(a,p,"honey","sell-honey",RetailLife.honeySpace(t),OrchardLife.HONEY_PRICE,cash);
            sell(a,p,"grape-juice","sell-grape-juice",RetailLife.cellarSpace(t,"grape-juice"),OrchardLife.JUICE_PRICE,cash);
            sell(a,p,"wine","sell-wine",RetailLife.cellarSpace(t,"wine"),OrchardLife.WINE_PRICE,cash);
        }
        if(p.place==TownLife.Place.OFFICE&&room==p.place){
            rental=RentalLife.report(t);construction=ConstructionLife.report(t);
            if(s.minute>=540&&s.minute<1020){
                int city=t.employerFunds.getOrDefault("city-maintenance",0);
                if(!rental.owned()&&p.money>=rental.price())add(a,"rental-buy","Buy Appartamento Verdi · €"+rental.price());
                if(rental.owned()){
                    add(a,rental.listed()?"rental-unlist":"rental-list",rental.listed()?"Stop new leases":"List apartment · €"+rental.weeklyRent()+"/7 days");
                    if(!rental.occupied()&&city>=rental.resale())add(a,"rental-sell","Sell vacant apartment · €"+rental.resale());
                }
                if(!construction.owned()&&ConstructionLife.get(t,ConstructionLife.OWNER)==0&&construction.stage()<ConstructionLife.STAGES&&p.money>=construction.landPrice())add(a,"construction-buy","Buy construction plot · €"+construction.landPrice());
                if(construction.owned()){
                    if(construction.reserve()>0)add(a,"construction-withdraw","Withdraw unused labor funds");
                    if(construction.stage()>=ConstructionLife.STAGES){
                        if(city>=construction.salePrice())add(a,"construction-sell","Sell completed house · €"+construction.salePrice());
                    }else{
                        int stage=construction.stage();
                        if(construction.factorySteel()>0&&construction.steel()<ConstructionLife.STAGES-stage&&p.money>=ConstructionLife.STEEL_PRICE)add(a,"construction-steel","Buy steel · €4");
                        if(construction.factoryParts()>0&&construction.parts()<(stage==0?2:ConstructionLife.STAGES-stage)&&p.money>=ConstructionLife.PART_PRICE)add(a,"construction-parts","Buy parts · €5");
                        if(p.money>=10)add(a,"construction-fund","Fund crew · €10");
                        add(a,construction.enabled()?"construction-pause":"construction-start",construction.enabled()?"Pause crew":"Authorize crew");
                    }
                }
            }
        }
        return new View(garden,hens,orchard,farm,rental,construction,List.copyOf(a));
    }
    private static void sell(List<GeoProjection.Choice> a,PlayerLife.Player p,String item,String id,int space,int price,int cash){
        if(space>0&&cash>=price&&FoodLife.quantity(p,item,false)>0)add(a,id,"Sell / Vendi "+item+" · €"+price);
    }
    private static void add(List<GeoProjection.Choice> a,String id,String label){a.add(new GeoProjection.Choice("property:"+id,label));}
}
