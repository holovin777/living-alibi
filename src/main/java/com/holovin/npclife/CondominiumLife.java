package com.holovin.npclife;

/** Shared maintenance reserve: allocate part of an existing housing charge,
 * never add a second utilities bill. All values are fictional game amounts. */
final class CondominiumLife {
    private CondominiumLife(){}
    static final int MONTH=30, CONTRIBUTION=12, LIVING_RESERVE=20;
    private static final String PREFIX="condominium:", RESERVE=PREFIX+"reserve", ARREARS=PREFIX+"arrears";
    private static String monthKey(TownLife.Citizen c){return PREFIX+"unit:"+c.homeUnit+":period";}
    private static String debtKey(TownLife.Citizen c){return PREFIX+"payer:"+c.id+":due";}
    private static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    private static void set(TownLife.Town t,String k,int value){t.industryStocks.put(k,value);}
    private static int period(int day){return (day-1)/MONTH+1;}

    /** Called only when a regular housing bill is due; one share per household per period. */
    static int allocate(TownLife.Town t,WorldState clock,TownLife.Citizen c,int housingCharge){
        if(!c.alive||c.generationIndex<=0||c.age<18||AddressLife.building(c)!=1||
                RentalLife.housed(t,c,clock.day)||housingCharge<=CONTRIBUTION||
                get(t,monthKey(c))>=period(clock.day))return 0;
        set(t,monthKey(c),period(clock.day));
        set(t,debtKey(c),Math.addExact(get(t,debtKey(c)),CONTRIBUTION));
        set(t,ARREARS,Math.addExact(get(t,ARREARS),CONTRIBUTION));
        t.pendingEconomy.add(new EconomyLife.Entry(clock.day,clock.minute,EconomyLife.Kind.BILL,
                c.id,"magnolia-condominium",null,CONTRIBUTION,"Common building maintenance share"));
        return CONTRIBUTION;
    }

    /** Transfer only actual available cash, after ordinary bills and above food reserve. */
    static int collect(TownLife.Town t,WorldState clock,TownLife.Citizen c){
        if(!c.alive||c.age<18)return 0;
        int due=get(t,debtKey(c));
        int amount=Math.min(due,Math.max(0,c.money-LIVING_RESERVE));
        if(amount==0)return 0;
        int balance=Math.addExact(get(t,RESERVE),amount);
        c.money-=amount;
        if(c.id.equals("marco"))clock.money=c.money;
        set(t,debtKey(c),due-amount);
        set(t,ARREARS,get(t,ARREARS)-amount);
        set(t,RESERVE,balance);
        t.pendingEconomy.add(new EconomyLife.Entry(clock.day,clock.minute,EconomyLife.Kind.BILL_PAYMENT,
                c.id,"magnolia-condominium",null,amount,"Paid common building maintenance share"));
        return amount;
    }
    static final int LABOR=24, MATERIAL_COST=12, REPAIR_PRICE=LABOR+MATERIAL_COST, REPAIR_MINUTES=90;
    private static final String WORKER=PREFIX+"repair-worker", ORDER=PREFIX+"repair-order",
            PROGRESS=PREFIX+"repair-progress", COMPLETED=PREFIX+"repairs-completed";
    private static TechnicalWorkLife.Order repairOrder(TownLife.Town t){
        return switch(get(t,ORDER)){
            case 1 -> t.technicalOrders.get("magnolia-lift");
            case 2 -> t.technicalOrders.get("magnolia-lighting");
            default -> null;
        };
    }
    private static boolean qualified(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.generationIndex>0&&c.alive&&c.age>=18&&c.age<65&&c.employed&&
                "Mechanic".equals(c.occupation)&&c.workplace==TownLife.Place.WORKSHOP&&
                c.home!=TownLife.Place.NORTH_RESIDENCES&&!BusinessLife.isEmployee(t,c)&&
                !RebirthLife.linkedAdult(t,c)&&!EmploymentLife.sick(c,s.day)&&
                !NpcJusticeLife.confined(t,c,s)&&!ConversationAvailability.urgent(c)&&
                c.health>=50&&c.hunger<75&&c.thirst<75&&c.fatigue<75;
    }
    // Traveling itself is not a reason to cancel an already accepted dispatch.
    private static boolean assignedAvailable(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.generationIndex>0&&c.alive&&c.age>=18&&c.age<65&&c.employed&&
                "Mechanic".equals(c.occupation)&&c.workplace==TownLife.Place.WORKSHOP&&
                c.home!=TownLife.Place.NORTH_RESIDENCES&&!BusinessLife.isEmployee(t,c)&&
                !RebirthLife.linkedAdult(t,c)&&!EmploymentLife.sick(c,s.day)&&
                !NpcJusticeLife.confined(t,c,s)&&c.health>=50&&c.hunger<75&&c.thirst<75&&c.fatigue<75;
    }
    private static boolean hours(WorldState s){return (s.day-1)%7<5&&s.minute>=540&&s.minute<1020;}
    private static boolean ready(TownLife.Town t,TechnicalWorkLife.Order o){
        return o!=null&&o.phase==TechnicalWorkLife.Phase.OPEN&&o.condition<=65&&
                get(t,RESERVE)>=REPAIR_PRICE&&TechnicalWorkLife.materialsReady(t,o);
    }
    private static void clear(TownLife.Town t){
        set(t,WORKER,0);set(t,ORDER,0);set(t,PROGRESS,0);
    }
    static boolean assigned(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c.generationIndex>0&&get(t,WORKER)==c.generationIndex&&hours(s)&&
                assignedAvailable(t,c,s)&&ready(t,repairOrder(t));
    }
    static void tick(TownLife.Town t,WorldState s){
        if(get(t,WORKER)>0){
            var worker=DemographyLife.byIndex(t,get(t,WORKER));
            var order=repairOrder(t);
            if(worker==null||!assigned(t,worker,s)){clear(t);return;}
            if(worker.travelRemaining>0||worker.place!=TownLife.Place.NORTH_RESIDENCES||
                    !"WORKING".equals(worker.activity))return;
            int progress=get(t,PROGRESS)+1;
            set(t,PROGRESS,progress);
            if(progress<REPAIR_MINUTES)return;
            // All requirements rechecked above. Pay the worker and the supplier
            // exactly once, from the building's finite cash, using shared stock.
            int wallet=Math.addExact(worker.money,LABOR);
            int supplier=Math.addExact(t.employerFunds.getOrDefault("technician",0),MATERIAL_COST);
            int completed=Math.addExact(get(t,COMPLETED),1);
            t.technicalMaterials.merge(order.materialA,-order.requiredA,Integer::sum);
            if(order.materialB!=null)t.technicalMaterials.merge(order.materialB,-order.requiredB,Integer::sum);
            set(t,RESERVE,get(t,RESERVE)-REPAIR_PRICE);
            worker.money=wallet;t.employerFunds.put("technician",supplier);
            worker.fatigue=Math.min(100,worker.fatigue+7);
            order.condition=Math.min(100,order.condition+55);
            // A very worn asset needs another paid repair rather than being declared fixed.
            order.phase=order.condition>65?TechnicalWorkLife.Phase.COMPLETED:TechnicalWorkLife.Phase.OPEN;
            order.completedDay=s.day;set(t,COMPLETED,completed);
            clear(t);
            return;
        }
        if(!hours(s)||s.minute>840||s.minute%30!=0)return;
        var lift=t.technicalOrders.get("magnolia-lift");
        var lighting=t.technicalOrders.get("magnolia-lighting");
        int choice=ready(t,lift)?1:ready(t,lighting)?2:0;
        if(choice==0)return;
        var worker=t.people.values().stream().filter(c->qualified(t,c,s)&&c.travelRemaining==0&&
                c.place==TownLife.Place.WORKSHOP&&"WORKING".equals(c.activity))
                .sorted(java.util.Comparator.comparing(c->c.id)).findFirst().orElse(null);
        if(worker==null)return;
        set(t,WORKER,worker.generationIndex);set(t,ORDER,choice);set(t,PROGRESS,0);
    }
    record Report(int reserve,int arrears,int contribution,int periodDays,String maintenanceStatus,int repairProgress,int repairsCompleted){}
    static Report report(TownLife.Town t){
        String status;
        var order=repairOrder(t);
        if(get(t,WORKER)>0&&order!=null)status=get(t,PROGRESS)>0?"REPAIRING":"DISPATCHED";
        else if(TechnicalWorkLife.magnolia(t).liftWorking()&&TechnicalWorkLife.magnolia(t).lightingWorking())status="OPERATIONAL";
        else if(get(t,RESERVE)<REPAIR_PRICE)status="WAITING_FUNDS";
        else if(!ready(t,t.technicalOrders.get("magnolia-lift"))&&!ready(t,t.technicalOrders.get("magnolia-lighting")))status="WAITING_ORDER_OR_STOCK";
        else status="WAITING_TECHNICIAN";
        return new Report(get(t,RESERVE),get(t,ARREARS),CONTRIBUTION,MONTH,status,get(t,PROGRESS),get(t,COMPLETED));
    }
}
