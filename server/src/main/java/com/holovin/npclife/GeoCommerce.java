package com.holovin.npclife;
import java.util.*;

/** Local controls for existing visitor-owned businesses and freight manifests. */
final class GeoCommerce {
    record View(MarketStallLife.Report stall,FreightLife.Report freight,LogisticsLife.Report logistics,
                List<GeoProjection.Choice> actions){}
    static View view(TownLife.Town t,WorldState s,GeoProjection map,GeoProjection.Point at,TownLife.Place room){
        var p=t.player;var actions=new ArrayList<GeoProjection.Choice>();
        if(!p.alive||RebirthLife.active(t)||JusticeLife.confined(t)||p.travelRemaining>0||p.cycling
                ||IntercomLife.insideVisit(t,s.day,s.minute)||map.point(p.place).distance(at)>18)
            return new View(null,null,null,List.of());
        MarketStallLife.Report stall=null;
        FreightLife.Report freight=FreightLife.active(t)?FreightLife.report(t):null;
        LogisticsLife.Report logistics=null;
        if(p.place==TownLife.Place.MARKET&&room==p.place){
            stall=MarketStallLife.report(t,s.day,s.minute);
            if(s.minute>=480&&s.minute<1200){
                if(!stall.registered()){
                    if(p.money>=stall.licensePrice())add(actions,"stall-register","Register market stall / Licenza · €"+stall.licensePrice());
                }else{
                    add(actions,stall.open()?"stall-close":"stall-open",stall.open()?"Close stall / Chiudi":"Open stall / Apri");
                    for(var item:stall.items()){
                        if(stall.stocked()+stall.supply().cargo()<stall.capacity()&&FoodLife.quantity(p,item.id(),false)>0)
                            add(actions,"stall-stock-"+item.id(),"Stock / Esponi "+item.id());
                        if(item.stock()>0&&FoodLife.total(p,false)<12)add(actions,"stall-take-"+item.id(),"Return to bag / Riprendi "+item.id());
                        if(item.price()<MarketStallLife.MAX_PRICE)add(actions,"stall-price-up-"+item.id(),item.id()+" · €"+item.price()+" → +€1");
                        if(item.price()>1)add(actions,"stall-price-down-"+item.id(),item.id()+" · €"+item.price()+" → −€1");
                    }
                    if(p.money>=10){add(actions,"stall-fund","Fund payroll / Stipendi · €10");add(actions,"stall-supply-fund","Fund supply / Fornitura · €10");}
                    if(stall.reserve()>=10)add(actions,"stall-withdraw","Withdraw payroll reserve · €10");
                    if(stall.supply().reserve()>=10)add(actions,"stall-supply-withdraw","Withdraw supply reserve · €10");
                    add(actions,stall.supply().enabled()?"stall-supply-disable":"stall-supply-enable",stall.supply().enabled()?"Pause farm orders":"Enable farm orders");
                    if(stall.sellerName()!=null)add(actions,"stall-dismiss","End seller contract / Termina contratto");
                    else if(stall.reserve()>=2*MarketStallLife.WAGE)
                        for(var c:stall.candidates())add(actions,"stall-hire-"+c.id(),"Hire seller / Assumi "+c.name());
                }
            }
        }
        boolean office=LogisticsLife.isOffice(p.place)&&(p.place==TownLife.Place.LOGISTICS_DEPOT?room==null:room==p.place);
        if(office){
            logistics=LogisticsLife.report(t,s.day);
            if(!logistics.yours()){
                int owner=LogisticsLife.get(t,LogisticsLife.OWNER);
                if(!FreightLife.active(t)&&p.money>=LogisticsLife.REGISTRATION&&(owner==0||t.pastLives.containsKey(owner)))
                    freight(actions,"register","","Register carrier / Registra trasporti · €20");
            }else{
                if(!FreightLife.active(t)){
                    if(p.money>=25)freight(actions,"invest-25","","Invest €25");
                    if(p.money>=100)freight(actions,"invest-100","","Invest €100");
                    if(logistics.cash()>=25)freight(actions,"withdraw-25","","Withdraw €25");
                    if(!logistics.truck()&&logistics.cash()>=LogisticsLife.TRUCK_PRICE)freight(actions,"buy-truck","","Buy truck / Compra camion · €220");
                    if(logistics.truck()&&logistics.condition()<100&&logistics.cash()>=LogisticsLife.SERVICE)
                        freight(actions,"service-truck","","Service truck / Manutenzione");
                    if(logistics.truck()&&logistics.driverName()==null&&logistics.cash()>=40)
                        for(var c:logistics.candidates())freight(actions,"hire-driver",c.id(),"Hire driver / Autista: "+c.name());
                    if(logistics.driverName()!=null)freight(actions,logistics.automatic()?"auto-off":"auto-on","","Automatic dispatch: "+(logistics.automatic()?"OFF":"ON"));
                }
                var second=logistics.secondTruck();
                if(logistics.truck()&&!second.purchased()&&logistics.cash()>=LogisticsLife.TRUCK_PRICE)
                    freight(actions,"buy-truck-2","","Buy second truck · €220");
                if(second.purchased()&&!second.active()&&second.driverName()==null&&logistics.cash()>=40)
                    for(var c:logistics.candidates())freight(actions,"hire-driver-2",c.id(),"Hire second driver: "+c.name());
                if(second.driverName()!=null)freight(actions,second.automatic()?"auto-off-2":"auto-on-2","","Truck two automatic dispatch: "+(second.automatic()?"OFF":"ON"));
                if(second.purchased()&&!second.active()&&second.condition()<100&&logistics.cash()>=LogisticsLife.SERVICE)
                    freight(actions,"service-truck-2","","Service second truck");
            }
        }
        if(room==null&&(!p.ownsBicycle||p.bicycleParked!=null)){
            if(!FreightLife.active(t)&&p.lastWorkDay!=s.day&&s.minute>=480&&s.minute<960
                    &&FreightLife.ROUTES.stream().anyMatch(r->r.places().getFirst()==p.place)){
                if(FreightLife.JOB.equals(p.jobId))freight(actions,"load","","Load employer truck / Carica");
                if(LogisticsLife.yours(t)&&LogisticsLife.hasTruck(t))freight(actions,"load-company","","Load company truck / Carica camion");
            }
            if(freight!=null&&!freight.automated()&&p.place.name().equals(freight.truckPlace())
                    &&(freight.companyRun()?LogisticsLife.yours(t):FreightLife.JOB.equals(p.jobId))){
                if(freight.nextDestination()!=null)
                    freight(actions,"drive",freight.nextDestination(),"Deliver to / Consegna → "+TownLife.Place.valueOf(freight.nextDestination()).label);
                else freight(actions,"unload","","Unload / Scarica · €"+freight.wage());
            }
        }
        return new View(stall,freight,logistics,List.copyOf(actions));
    }
    private static void add(List<GeoProjection.Choice> a,String id,String label){a.add(new GeoProjection.Choice(id,label));}
    private static void freight(List<GeoProjection.Choice> a,String id,String arg,String label){add(a,"freight:"+id+":"+arg,label);}
}
