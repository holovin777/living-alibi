package com.holovin.npclife;

/** Fictional roadside administrative checks, independent of criminal custody. */
final class RoadPoliceLife {
    private RoadPoliceLife(){}
    static final int LICENSE_FINE=35, CONDITION_FINE=20, CHECK_INTERVAL=30;
    private static String key(TownLife.Town t,String field){return "road-police:"+t.player.incarnation+":"+field;}
    static int get(TownLife.Town t,String field){return t.industryStocks.getOrDefault(key(t,field),0);}
    private static void set(TownLife.Town t,String field,int n){t.industryStocks.put(key(t,field),n);}
    private static int clock(WorldState s){return Math.addExact(Math.multiplyExact(s.day-1,1440),s.minute)+1;}
    static boolean due(TownLife.Town t,WorldState s){
        int last=get(t,"last-check");
        return last==0||clock(s)-last>=CHECK_INTERVAL;
    }
    record View(int checks,int violations,int paid,int outstanding){}
    static View view(TownLife.Town t){return new View(get(t,"checks"),get(t,"violations"),get(t,"paid"),get(t,"outstanding"));}
    record Result(boolean checked,int fine,String message){}
    static Result check(TownLife.Town t,WorldState s,String kind,int condition,String plate){
        if(!due(t,s))return new Result(false,0,"");
        set(t,"last-check",clock(s));set(t,"checks",Math.addExact(get(t,"checks"),1));
        int fine=0;
        String reason="";
        if(!DrivingSchoolLife.allowed(t,kind)){fine+=LICENSE_FINE;reason="Missing category "+DrivingSchoolLife.required(kind);}
        if(condition<25){fine+=CONDITION_FINE;reason+=(reason.isEmpty()?"":"; ")+"Unsafe vehicle condition";}
        if(fine==0)return new Result(true,0,"Police check "+plate+": documents and condition accepted.");
        set(t,"violations",Math.addExact(get(t,"violations"),1));
        set(t,"outstanding",Math.addExact(get(t,"outstanding"),fine));
        return new Result(true,fine,"Police check "+plate+": "+reason+". Fine EUR "+fine+". Pay at municipal office.");
    }
    static boolean canPay(TownLife.Town t,WorldState s){
        return t.player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&t.player.travelRemaining==0
                &&t.player.place==TownLife.Place.OFFICE&&s.minute>=480&&s.minute<1020&&get(t,"outstanding")>0&&t.player.money>0;
    }
    static String pay(TownLife.Town t,WorldState s){
        if(!canPay(t,s))throw new IllegalArgumentException("Visit the municipal office 08:00–17:00 with funds to pay traffic fines.");
        int amount=Math.min(t.player.money,get(t,"outstanding"));
        t.player.money-=amount;t.employerFunds.merge("city-maintenance",amount,Math::addExact);
        set(t,"outstanding",get(t,"outstanding")-amount);set(t,"paid",Math.addExact(get(t,"paid"),amount));
        return "Traffic fine payment: EUR "+amount+".";
    }
}
