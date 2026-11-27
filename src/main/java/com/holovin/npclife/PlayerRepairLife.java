package com.holovin.npclife;

import java.util.List;

/** A personal service call using a fictional, isolated diagnostic simulator.
 * No real electrical measurement or live-voltage instructions. */
final class PlayerRepairLife {
    private PlayerRepairLife(){}
    static int get(TownLife.Town t,String f){return PlayerServiceBusiness.get(t,"manual:"+f);}
    static void set(TownLife.Town t,String f,int n){PlayerServiceBusiness.set(t,"manual:"+f,n);}
    static TownLife.Citizen customer(TownLife.Town t){return NpcVehicleCrimeLife.person(t,get(t,"customer"));}
    static boolean reserved(TownLife.Town t,TownLife.Citizen c){return get(t,"customer")>0&&get(t,"customer")==c.generationIndex;}
    static boolean dispatched(TownLife.Town t,TownLife.Citizen c){
        for(int i=0;i<ResidentServiceLife.MAX_PARALLEL;i++)if(ResidentServiceLife.slot(t,i,"day")>0
            &&ResidentServiceLife.slot(t,i,"customer")==c.generationIndex)return true;
        return false;
    }
    static boolean offer(TownLife.Town t,TownLife.Citizen c){
        return ResidentServiceLife.needsService(t,c)&&ResidentServiceLife.get(t,c,"queued-day")>0
            &&!dispatched(t,c)&&c.money>=ResidentServiceLife.RESERVE+PlayerServiceBusiness.price(t)
            &&(ResidentServiceLife.condition(t,c)>0||VocationalLife.certified(t,"mechanical")||VocationalLife.certified(t,"electrical"));
    }
    static void clear(TownLife.Town t){for(String f:List.of("customer","day","price","phase","fault"))set(t,f,0);}
    static void tick(TownLife.Town t,WorldState s){
        if(get(t,"customer")>0&&(!PlayerServiceBusiness.operating(t,s.day)||!ResidentServiceLife.needsService(t,customer(t))
            ||s.day>get(t,"day")||s.day==get(t,"day")&&s.minute>=ResidentServiceLife.END))clear(t);
    }
    static boolean ready(TownLife.Town t,WorldState s){
        var c=customer(t);
        return PlayerServiceBusiness.operating(t,s.day)&&ResidentServiceLife.needsService(t,c)
            &&s.day==get(t,"day")&&s.minute>=ResidentServiceLife.START&&s.minute+10<=ResidentServiceLife.END
            &&t.player.place==c.home&&t.player.travelRemaining==0&&t.player.fatigue<85
            &&HomeRoutineLife.atHome(c)&&c.money>=ResidentServiceLife.RESERVE+get(t,"price");
    }
    record Step(int minutes,String message){}
    static Step act(TownLife.Town t,WorldState s,String action){
        if(!PlayerServiceBusiness.operating(t,s.day))throw new IllegalArgumentException("An operating repair business is required.");
        if(action.equals("cancel")){if(get(t,"customer")==0)throw new IllegalArgumentException("No reserved call.");clear(t);return new Step(1,"Released customer request.");}
        if(action.startsWith("claim:")){
            if(get(t,"customer")>0||!PlayerServiceBusiness.office(t,s.minute)||s.minute>=ResidentServiceLife.START)
                throw new IllegalArgumentException("Reserve one customer at the workshop before the evening visit.");
            var c=t.people.get(action.substring(6));
            if(!offer(t,c))throw new IllegalArgumentException("Customer request is unavailable.");
            set(t,"customer",c.generationIndex);set(t,"day",s.day);set(t,"price",PlayerServiceBusiness.price(t));
            set(t,"phase",1);set(t,"fault",1+Math.floorMod(c.generationIndex+s.day,3));
            return new Step(1,"Visit the customer's district at 19:30–21:30.");
        }
        if(!ready(t,s))throw new IllegalArgumentException("Visit the reserved customer during the appointment window.");
        int phase=get(t,"phase");
        if(action.equals("inspect")&&phase==1){set(t,"phase",2);return new Step(5,"Visual inspection finished. Run the isolated diagnostic simulator.");}
        if(action.equals("measure")&&phase==2){set(t,"phase",3);return new Step(5,reading(get(t,"fault")));}
        if(action.startsWith("choose:")&&phase==3){
            if(!action.substring(7).equals(part(get(t,"fault"))))return new Step(5,"Diagnosis does not match the simulated fault. Check the readings.");
            set(t,"phase",4);return new Step(10,"Correct component selected. Ready for final simulated functional test.");
        }
        if(action.equals("test")&&phase==4){
            if(!PlayerServiceBusiness.available(t))throw new IllegalArgumentException("No spare part available.");
            var c=customer(t);int price=get(t,"price");
            c.money-=price;t.technicalMaterials.merge("spare-parts",-1,Math::addExact);
            t.employerFunds.merge("city-maintenance",HomeServiceLife.PART_PRICE+1,Math::addExact);
            PlayerServiceBusiness.add(t,"cash",price-HomeServiceLife.PART_PRICE-1);
            PlayerServiceBusiness.add(t,"revenue",price);PlayerServiceBusiness.add(t,"tax",1);PlayerServiceBusiness.add(t,"jobs",1);
            PlayerServiceBusiness.add(t,"manual-jobs",1);
            ResidentServiceLife.set(t,c,"wear",0);ResidentServiceLife.set(t,c,"queued-day",0);
            t.player.fatigue=Math.min(100,t.player.fatigue+4);
            clear(t);return new Step(5,"Functional test passed; customer paid the company. One spare part consumed.");
        }
        throw new IllegalArgumentException("Complete the current diagnostic step first.");
    }
    static String part(int fault){return switch(fault){case 1->"pump";case 2->"sensor";default->"controller";};}
    static String reading(int fault){return switch(fault){case 1->"Diagnostic code P: drain pump failed.";case 2->"Diagnostic code S: water-level sensor failed.";default->"Diagnostic code C: control module failed.";};}
    record Offer(String id,String district,int condition){}
    record View(int phase,String district,int price,String reading,int experience,boolean ready,List<Offer> offers){}
    static View view(TownLife.Town t,WorldState s){
        if(!PlayerServiceBusiness.yours(t))return new View(0,"",0,"",0,false,List.of());
        var c=customer(t);int phase=get(t,"phase");
        var offers=phase==0&&PlayerServiceBusiness.operating(t,s.day)&&PlayerServiceBusiness.office(t,s.minute)
            ?t.people.values().stream().filter(x->offer(t,x)).limit(12).map(x->new Offer(x.id,x.home.name(),ResidentServiceLife.condition(t,x))).toList():List.<Offer>of();
        return new View(phase,c==null?"":c.home.name(),get(t,"price"),phase>=3?reading(get(t,"fault")):"",
            PlayerServiceBusiness.get(t,"manual-jobs"),ready(t,s),offers);
    }
}
