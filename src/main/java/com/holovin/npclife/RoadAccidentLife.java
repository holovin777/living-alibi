package com.holovin.npclife;

/** Fictional collision injury and limited own-damage coverage, not real insurance law. */
final class RoadAccidentLife {
    private RoadAccidentLife(){}
    static final int TREATMENT_FEE=8;
    private static String key(TownLife.Town t,String field){return "road-accident:"+t.player.incarnation+":"+field;}
    static int get(TownLife.Town t,String field){return t.industryStocks.getOrDefault(key(t,field),0);}
    static void add(TownLife.Town t,String field,int amount){t.industryStocks.merge(key(t,field),amount,Math::addExact);}
    static int damage(double speedKmh){return Math.max(1,Math.min(35,(int)Math.round(2+Math.max(0,speedKmh)*0.45)));}
    static int injury(double speedKmh,boolean twoWheels){
        return Math.max(0,Math.min(30,(int)Math.round(Math.max(0,speedKmh-8)*(twoWheels?0.55:0.30))));
    }
    record View(int accidents,int damage,int injury,int claims,int compensation){}
    static View view(TownLife.Town t){return new View(get(t,"accidents"),get(t,"damage"),get(t,"injury"),get(t,"claims"),get(t,"compensation"));}
    record Result(int injury,int compensation,String message){}
    static Result record(TownLife.Town t,WorldState s,String kind,boolean stolen,int actualDamage,double speedKmh){
        int loss=Math.min(Math.max(0,t.player.health-1),injury(speedKmh,kind.equals("scooter")||kind.equals("motorcycle")));
        t.player.health-=loss;
        add(t,"accidents",1);add(t,"damage",actualDamage);add(t,"injury",loss);
        int payout=0;
        if(!stolen&&VehicleDocumentsLife.KINDS.contains(kind)&&VehicleDocumentsLife.parked(t,kind)!=null
                &&VehicleDocumentsLife.insured(t,s,kind)&&DrivingSchoolLife.allowed(t,kind)){
            int requested=Math.max(0,Math.min(25,actualDamage*2-5));
            int funds=t.employerFunds.getOrDefault(VehicleDocumentsLife.INSURER,0);
            payout=Math.min(requested,Math.max(0,funds));
            if(payout>0){
                t.employerFunds.merge(VehicleDocumentsLife.INSURER,-payout,Integer::sum);
                t.player.money=Math.addExact(t.player.money,payout);
                add(t,"claims",1);add(t,"compensation",payout);
            }
        }
        return new Result(loss,payout,"Collision: damage "+actualDamage+", health -"+loss+", compensation EUR "+payout+".");
    }
    static boolean canTreat(TownLife.Town t,WorldState s){
        return t.player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&t.player.travelRemaining==0
                &&!t.player.cycling&&t.player.place==TownLife.Place.CLINIC&&s.minute>=480&&s.minute+30<=1080
                &&t.player.health<100&&get(t,"injury")>get(t,"treated")&&t.player.money>=TREATMENT_FEE;
    }
    static String treat(TownLife.Town t,WorldState s){
        if(!canTreat(t,s))throw new IllegalArgumentException("Accident treatment unavailable.");
        int gain=Math.min(15,Math.min(100-t.player.health,get(t,"injury")-get(t,"treated")));
        t.player.money-=TREATMENT_FEE;t.employerFunds.merge("doctor",TREATMENT_FEE,Math::addExact);
        t.player.health+=gain;add(t,"treated",gain);
        return "Accident treatment: health +"+gain+".";
    }
}
