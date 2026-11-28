package com.holovin.npclife;

import java.util.List;

/** District-level fictional incidents and finite emergency resources. */
final class FireServiceLife {
    private FireServiceLife(){}
    static final TownLife.Place DEPOT=TownLife.Place.WORKSHOP;
    static final List<TownLife.Place> SITES=List.of(TownLife.Place.FACTORY,TownLife.Place.MARKET,TownLife.Place.NORTH_RESIDENCES,TownLife.Place.SOUTH_RESIDENCES);
    static int get(TownLife.Town t,String field){return t.industryStocks.getOrDefault("fire-service:"+field,0);}
    static void set(TownLife.Town t,String field,int value){t.industryStocks.put("fire-service:"+field,value);}
    static void add(TownLife.Town t,String field,int value){set(t,field,Math.addExact(get(t,field),value));}
    static int now(WorldState s){return Math.toIntExact((s.day-1L)*1440+s.minute+1);}
    static TownLife.Place site(TownLife.Town t){
        int n=get(t,"site");return n>0&&n<=SITES.size()?SITES.get(n-1):null;
    }
    static int responseMinutes(TownLife.Place p){return Math.min(40,5+CityExpansionLife.distance(DEPOT,p));}
    static int fuel(TownLife.Place p){return Math.min(4,Math.max(1,(CityExpansionLife.distance(DEPOT,p)+3)/4));}
    static boolean start(TownLife.Town t,WorldState s,TownLife.Place p){
        if(site(t)!=null||!SITES.contains(p))return false;
        set(t,"site",SITES.indexOf(p)+1);set(t,"started",now(s));set(t,"reported",0);set(t,"arrive",0);set(t,"end",0);
        add(t,"incidents",1);return true;
    }
    static boolean report(TownLife.Town t){
        if(site(t)==null||get(t,"reported")!=0)return false;
        set(t,"reported",1);return true;
    }
    static boolean canReport(TownLife.Town t){
        return t.player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&t.player.travelRemaining==0&&t.player.place==site(t)&&get(t,"reported")==0;
    }
    static String playerReport(TownLife.Town t){
        if(!canReport(t)||!report(t))throw new IllegalArgumentException("No unreported fire in this district.");
        return "Fire reported / Incendio segnalato.";
    }
    static boolean dispatch(TownLife.Town t,WorldState s){
        var p=site(t);
        if(p==null||get(t,"reported")==0||get(t,"arrive")!=0)return false;
        int units=fuel(p),cost=units*FuelLife.PRICE+6;
        int water=WaterNetworkLife.active(t)?WaterNetworkLife.get(t,"treated"):t.cleanWater;
        if(FuelLife.stock(t)<units||water<4||t.employerFunds.getOrDefault("city-maintenance",0)<cost)return false;
        t.employerFunds.merge("city-maintenance",-cost,Integer::sum);
        t.employerFunds.merge(FuelLife.ACCOUNT,units*FuelLife.PRICE,Math::addExact);
        t.employerFunds.merge("fire-service",6,Math::addExact);
        t.industryStocks.merge(FuelLife.STOCK,-units,Integer::sum);
        if(WaterNetworkLife.active(t))WaterNetworkLife.add(t,"treated",-4);else t.cleanWater-=4;
        set(t,"arrive",now(s)+responseMinutes(p));set(t,"end",get(t,"arrive")+15);
        return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        // Opt-in to the geographic world's transport simulation. Sparse incidents, no random save migration.
        if(NpcTransportLife.enabled(t)&&s.day%5==0&&s.minute==780&&get(t,"last-day")!=s.day){
            set(t,"last-day",s.day);start(t,s,SITES.get(Math.floorMod(s.day/5,SITES.size())));
        }
        if(site(t)==null)return;
        int elapsed=now(s)-get(t,"started");
        if(elapsed>=10)report(t); // abstract resident notification
        if(get(t,"reported")!=0&&get(t,"arrive")==0&&s.minute%5==0)dispatch(t,s);
        if(s.minute%5==0&&get(t,"arrive")==0)add(t,"damage",1);
        if(get(t,"end")>0&&now(s)>=get(t,"end")){
            add(t,"extinguished",1);clear(t);
        }else if(get(t,"arrive")==0&&elapsed>=120){
            add(t,"uncontrolled",1);clear(t);
        }
    }
    private static void clear(TownLife.Town t){for(String k:List.of("site","started","reported","arrive","end"))set(t,k,0);}
    record View(String district,String status,int minutesRemaining,int incidents,int extinguished,int uncontrolled,int damage){}
    static View view(TownLife.Town t,WorldState s){
        var p=site(t);int arrival=get(t,"arrive");
        String status=p==null?"NONE":arrival==0?(get(t,"reported")==0?"UNREPORTED":"WAITING_RESOURCES"):now(s)<arrival?"EN_ROUTE":"FIGHTING";
        return new View(p==null?"":p.name(),status,arrival==0?0:Math.max(0,(status.equals("FIGHTING")?get(t,"end"):arrival)-now(s)),get(t,"incidents"),get(t,"extinguished"),get(t,"uncontrolled"),get(t,"damage"));
    }
}
