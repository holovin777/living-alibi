package com.holovin.npclife;
import java.util.*;
import static com.holovin.npclife.TownLife.Place.*;

/** Small fictional household economy. All quantities and tariffs are game units. */
final class HomeApplianceLife {
    private HomeApplianceLife(){}
    static final List<String> CHILLED=List.of("milk","eggs","vegetables","apples","pears","plums","grapes");
    static final int CAPACITY=12, CREDIT_LIMIT=12;
    static int get(TownLife.Town t,String k){return HomeGoodsLife.get(t,"appliance:"+k);}
    static void set(TownLife.Town t,String k,int n){HomeGoodsLife.set(t,"appliance:"+k,n);}
    static boolean power(TownLife.Town t){return t.player.ownsHouse&&get(t,"bill")<CREDIT_LIMIT&&EnergyLife.publicPower(t);}
    static int quantity(TownLife.Town t,String id){return get(t,"cold:"+id);}
    static int total(TownLife.Town t){return CHILLED.stream().mapToInt(id->quantity(t,id)).sum();}
    static void home(TownLife.Town t,WorldState s){
        PersonalGoodsLife.ready(t);
        if(!t.player.ownsHouse||t.player.place!=SOUTH_RESIDENCES||IntercomLife.insideVisit(t,s.day,s.minute))
            throw new IllegalArgumentException("Visit your own house to use appliances.");
    }
    static void require(TownLife.Town t,WorldState s,String appliance){
        home(t,s);
        if(!HomeGoodsLife.installed(t,appliance))throw new IllegalArgumentException("Purchase the appliance first.");
        if(!EnergyLife.publicPower(t))throw new IllegalArgumentException("Municipal grid is currently unavailable.");
        if(!power(t))throw new IllegalArgumentException("Electricity disconnected. Pay your bill.");
        if(!HomeServiceLife.working(t,appliance))throw new IllegalArgumentException("The appliance needs repair.");
    }
    static void consume(TownLife.Town t){set(t,"bill",Math.addExact(get(t,"bill"),1));}
    static String pay(TownLife.Town t,WorldState s){
        home(t,s);int amount=Math.min(t.player.money,get(t,"bill"));
        if(amount<1)throw new IllegalArgumentException("No payable bill or insufficient cash.");
        t.player.money-=amount;t.employerFunds.merge(EnergyLife.enabled(t)?EnergyLife.ACCOUNT:"city-maintenance",amount,Math::addExact);
        set(t,"bill",get(t,"bill")-amount);
        return "Electricity payment: "+amount+" EUR.";
    }
    static String transfer(TownLife.Town t,WorldState s,String id,boolean store){
        home(t,s);
        if(!HomeGoodsLife.installed(t,"fridge")||!CHILLED.contains(id))throw new IllegalArgumentException("No compatible refrigerator storage.");
        if(store){
            if(!power(t)||!HomeServiceLife.working(t,"fridge")||total(t)>=CAPACITY||FoodLife.quantity(t.player,id,false)<1)
                throw new IllegalArgumentException("Refrigerator unavailable, full, or no food to store.");
            FoodLife.change(t.player,id,false,-1);set(t,"cold:"+id,quantity(t,id)+1);
        }else{
            if(quantity(t,id)<1||FoodLife.total(t.player,false)>=12)throw new IllegalArgumentException("No food to retrieve or bag full.");
            FoodLife.change(t.player,id,false,1);set(t,"cold:"+id,quantity(t,id)-1);
        }
        return store?"Food stored in refrigerator.":"Food retrieved from refrigerator.";
    }
    static String watch(TownLife.Town t,WorldState s){
        require(t,s,"television");
        if(get(t,"tv-day")==s.day)throw new IllegalArgumentException("You already watched today's bulletin.");
        var editions=NewspaperLife.report(t,s.day).editions();
        if(editions.isEmpty())throw new IllegalArgumentException("No local news bulletin is available yet.");
        set(t,"tv-day",s.day);consume(t);HomeServiceLife.wear(t,"television",2);t.player.fatigue=Math.max(0,t.player.fatigue-4);
        return editions.getFirst().text();
    }
    static int cleanliness(TownLife.Town t){return Math.max(0,100-get(t,"laundry-dirt"));}
    static String wash(TownLife.Town t,WorldState s){
        require(t,s,"washer");
        if(get(t,"laundry-dirt")==0)throw new IllegalArgumentException("Your clothes are already clean.");
        if(FoodLife.quantity(t.player,"water",false)<1)throw new IllegalArgumentException("Need one water unit for laundry.");
        FoodLife.change(t.player,"water",false,-1);consume(t);HomeServiceLife.wear(t,"washer",4);set(t,"laundry-dirt",0);
        return "Laundry completed. Clothes are clean.";
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!t.player.alive||RebirthLife.active(t)||!t.player.ownsHouse||s.minute!=0||get(t,"daily")>=s.day)return;
        set(t,"daily",s.day);set(t,"laundry-dirt",Math.min(100,get(t,"laundry-dirt")+12));
        if(HomeGoodsLife.installed(t,"fridge")){
            if(power(t)&&HomeServiceLife.working(t,"fridge")){consume(t);HomeServiceLife.wear(t,"fridge",1);}
            else for(String id:CHILLED)if(quantity(t,id)>0){
                set(t,"cold:"+id,quantity(t,id)-1);set(t,"spoiled",Math.addExact(get(t,"spoiled"),1));break;
            }
        }
    }
    record View(boolean power,int bill,int creditLimit,Map<String,Integer> refrigerator,int capacity,int spoiled,
                int clothingCleanliness,boolean canWatch,boolean canWash,String bulletin){}
    static View view(TownLife.Town t,WorldState s){
        var cold=new LinkedHashMap<String,Integer>();for(String id:CHILLED)cold.put(id,quantity(t,id));
        var editions=NewspaperLife.report(t,s.day).editions();
        boolean atHome=t.player.alive&&t.player.travelRemaining==0&&t.player.ownsHouse&&t.player.place==SOUTH_RESIDENCES&&!RebirthLife.active(t);
        return new View(power(t),get(t,"bill"),CREDIT_LIMIT,cold,CAPACITY,get(t,"spoiled"),cleanliness(t),
            atHome&&power(t)&&HomeServiceLife.working(t,"television")&&get(t,"tv-day")!=s.day&&!editions.isEmpty(),
            atHome&&power(t)&&HomeServiceLife.working(t,"washer")&&get(t,"laundry-dirt")>0&&FoodLife.quantity(t.player,"water",false)>0,
            HomeGoodsLife.installed(t,"television")&&get(t,"tv-day")==s.day&&!editions.isEmpty()?editions.getFirst().text():"");
    }
}
