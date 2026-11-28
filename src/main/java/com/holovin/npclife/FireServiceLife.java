package com.holovin.npclife;

import java.util.List;

/** District-level fictional incidents and finite emergency resources. */
final class FireServiceLife {
    private FireServiceLife(){}
    static final String ROLE="Firefighter", ACCOUNT="fire-service";
    static final int CREW_SIZE=2, WAGE=3;
    static final TownLife.Place DEPOT=TownLife.Place.WORKSHOP;
    static final List<TownLife.Place> SITES=List.of(TownLife.Place.FACTORY,TownLife.Place.MARKET,TownLife.Place.NORTH_RESIDENCES,TownLife.Place.SOUTH_RESIDENCES);
    static int get(TownLife.Town t,String field){return t.industryStocks.getOrDefault("fire-service:"+field,0);}
    static void set(TownLife.Town t,String field,int value){t.industryStocks.put("fire-service:"+field,value);}
    static void add(TownLife.Town t,String field,int value){set(t,field,Math.addExact(get(t,field),value));}
    static int now(WorldState s){return Math.toIntExact((s.day-1L)*1440+s.minute+1);}
    static TownLife.Place site(TownLife.Town t){
        int n=get(t,"site");return n>0&&n<=SITES.size()?SITES.get(n-1):null;
    }
    static boolean firefighter(TownLife.Citizen c){return c.alive&&c.employed&&ROLE.equals(c.occupation)&&c.workplace==DEPOT;}
    static boolean scheduled(TownLife.Citizen c,int minute){
        int start=Math.floorMod(c.generationIndex,3)*480;
        return firefighter(c)&&minute>=start&&minute<start+480;
    }
    static boolean assigned(TownLife.Town t,TownLife.Citizen c){
        return site(t)!=null&&get(t,"arrive")>0&&firefighter(c)&&
            (get(t,"crew-1")==c.generationIndex||get(t,"crew-2")==c.generationIndex)&&c.generationIndex>0;
    }
    static TownLife.Place destination(TownLife.Town t,TownLife.Citizen c){
        return assigned(t,c)?site(t):DEPOT;
    }
    static List<TownLife.Citizen> available(TownLife.Town t,WorldState s){
        return t.people.values().stream().filter(c->scheduled(c,s.minute)&&c.generationIndex>0
            &&c.presentAt(DEPOT)&&"WORKING".equals(c.activity)&&!EmploymentLife.sick(c,s.day)
            &&!RebirthLife.linkedAdult(t,c)&&!NpcJusticeLife.confined(t,c,s)&&!TownLife.inPlayerConversation(t,c)&&c.fatigue<85)
            .sorted(java.util.Comparator.comparingInt(c->c.generationIndex)).toList();
    }
    static void pay(TownLife.Town t,TownLife.Citizen c){
        if(t.employerFunds.getOrDefault("city-maintenance",0)<WAGE)return;
        t.employerFunds.merge("city-maintenance",-WAGE,Integer::sum);
        c.money=Math.addExact(c.money,WAGE);
    }
    static final int EQUIPMENT_MIN=40, SERVICE_WAGE=5, PART_COST=4;
    static int equipment(TownLife.Town t){return t.industryStocks.getOrDefault("fire-service:equipment",100);}
    static boolean serviceResources(TownLife.Town t){
        return get(t,"arrive")==0&&equipment(t)<80&&t.technicalMaterials.getOrDefault("spare-parts",0)>0
                &&t.employerFunds.getOrDefault("city-maintenance",0)>=PART_COST+SERVICE_WAGE;
    }
    private static void service(TownLife.Town t){
        t.technicalMaterials.merge("spare-parts",-1,Integer::sum);
        t.employerFunds.merge("city-maintenance",-PART_COST-SERVICE_WAGE,Integer::sum);
        t.employerFunds.merge("technician",PART_COST,Math::addExact);
        set(t,"equipment",Math.min(100,equipment(t)+35));add(t,"services",1);
    }
    static boolean canService(TownLife.Town t,WorldState s){
        return t.player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&t.player.travelRemaining==0
                &&t.player.place==DEPOT&&t.player.engineeringSkill>=3&&s.minute>=480&&s.minute+30<=1080&&serviceResources(t);
    }
    static String playerService(TownLife.Town t,WorldState s){
        if(!canService(t,s))throw new IllegalArgumentException("Equipment service requires engineering skill 3, a spare part and municipal funds.");
        service(t);t.player.money=Math.addExact(t.player.money,SERVICE_WAGE);
        return "Fire station equipment serviced / Attrezzatura revisionata. EUR "+SERVICE_WAGE+" earned.";
    }
    static void maintain(TownLife.Town t,WorldState s){
        if(!StaffingLife.enabled(t)||s.minute%60!=0||get(t,"last-service")==now(s)||!serviceResources(t))return;
        var worker=available(t,s).stream().findFirst().orElse(null);if(worker==null)return;
        service(t);worker.money=Math.addExact(worker.money,SERVICE_WAGE);set(t,"last-service",now(s));
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
        if(p==null||get(t,"reported")==0||get(t,"arrive")!=0||equipment(t)<EQUIPMENT_MIN)return false;
        var crew=available(t,s);
        if(StaffingLife.enabled(t)&&crew.size()<CREW_SIZE)return false;
        int units=fuel(p),cost=units*FuelLife.PRICE+6;
        int water=WaterNetworkLife.active(t)?WaterNetworkLife.get(t,"treated"):t.cleanWater;
        if(FuelLife.stock(t)<units||water<4||t.employerFunds.getOrDefault("city-maintenance",0)<cost)return false;
        t.employerFunds.merge("city-maintenance",-cost,Integer::sum);
        t.employerFunds.merge(FuelLife.ACCOUNT,units*FuelLife.PRICE,Math::addExact);
        t.employerFunds.merge("fire-service",6,Math::addExact);
        t.industryStocks.merge(FuelLife.STOCK,-units,Integer::sum);
        if(WaterNetworkLife.active(t))WaterNetworkLife.add(t,"treated",-4);else t.cleanWater-=4;
        set(t,"equipment",Math.max(0,equipment(t)-12));
        if(crew.size()>=CREW_SIZE){set(t,"crew-1",crew.get(0).generationIndex);set(t,"crew-2",crew.get(1).generationIndex);}
        set(t,"arrive",now(s)+responseMinutes(p));set(t,"end",get(t,"arrive")+15);
        return true;
    }
    static void tick(TownLife.Town t,WorldState s){
        // Opt-in to the geographic world's transport simulation. Sparse incidents, no random save migration.
        if(NpcTransportLife.enabled(t)&&s.day%5==0&&s.minute==780&&get(t,"last-day")!=s.day){
            set(t,"last-day",s.day);start(t,s,SITES.get(Math.floorMod(s.day/5,SITES.size())));
        }
        maintain(t,s);
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
    private static void clear(TownLife.Town t){for(String k:List.of("site","started","reported","arrive","end","crew-1","crew-2"))set(t,k,0);}
    record View(String district,String status,int minutesRemaining,int incidents,int extinguished,int uncontrolled,int damage,int employed,int available,int assigned,int equipment,int services,int evacuating,int sheltered,int evacuatedTotal){}
    static View view(TownLife.Town t,WorldState s){
        var p=site(t);int arrival=get(t,"arrive");
        String status=p==null?"NONE":arrival==0?(get(t,"reported")==0?"UNREPORTED":StaffingLife.enabled(t)&&available(t,s).size()<CREW_SIZE?"WAITING_CREW":equipment(t)<EQUIPMENT_MIN?"WAITING_EQUIPMENT":"WAITING_RESOURCES"):now(s)<arrival?"EN_ROUTE":"FIGHTING";
        return new View(p==null?"":p.name(),status,arrival==0?0:Math.max(0,(status.equals("FIGHTING")?get(t,"end"):arrival)-now(s)),get(t,"incidents"),get(t,"extinguished"),get(t,"uncontrolled"),get(t,"damage"),(int)t.people.values().stream().filter(FireServiceLife::firefighter).count(),available(t,s).size(),(int)t.people.values().stream().filter(c->assigned(t,c)).count(),equipment(t),get(t,"services"),FireEvacuationLife.affectedCount(t),FireEvacuationLife.shelteredCount(t),get(t,"evacuated-total"));
    }
}
