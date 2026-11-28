package com.holovin.npclife;

/** Fictional homeowner policy for the existing single visitor house. */
final class HomeInsuranceLife {
    private HomeInsuranceLife(){}
    static final String ACCOUNT="home-insurance";
    static final int PREMIUM=12, TERM=30, MAX_CLAIM=60, DEDUCTIBLE=5;
    private static String key(TownLife.Town t,String f){return "home-insurance:"+t.player.incarnation+":"+f;}
    static int get(TownLife.Town t,String f){return t.industryStocks.getOrDefault(key(t,f),0);}
    private static void set(TownLife.Town t,String f,int n){t.industryStocks.put(key(t,f),n);}
    static boolean active(TownLife.Town t,WorldState s){return t.player.ownsHouse&&get(t,"until")>s.day;}
    private static boolean office(TownLife.Town t,WorldState s){
        return t.player.alive&&!RebirthLife.active(t)&&!JusticeLife.confined(t)&&t.player.travelRemaining==0&&!t.player.cycling
            &&t.player.place==TownLife.Place.OFFICE&&s.minute>=480&&s.minute+15<=1020;
    }
    static boolean canBuy(TownLife.Town t,WorldState s){
        return office(t,s)&&t.player.ownsHouse&&get(t,"until")-s.day<=7&&t.player.money>=PREMIUM
            &&FireServiceLife.site(t)!=HousingLife.HOUSE_DISTRICT;
    }
    static String buy(TownLife.Town t,WorldState s){
        if(!canBuy(t,s))throw new IllegalArgumentException("Home policy requires ownership, office hours and no active home-district fire.");
        t.player.money-=PREMIUM;t.employerFunds.merge(ACCOUNT,PREMIUM,Math::addExact);
        set(t,"until",Math.addExact(Math.max(s.day,get(t,"until")),TERM));
        return "Home insurance / Assicurazione casa: valid for 30 additional days.";
    }
    static void incidentStarted(TownLife.Town t,WorldState s,TownLife.Place p){
        if(p!=HousingLife.HOUSE_DISTRICT||!active(t,s)||!t.player.alive||RebirthLife.active(t))return;
        set(t,"incident",FireServiceLife.get(t,"started"));set(t,"loss",0);
    }
    static void damage(TownLife.Town t,TownLife.Place p,int loss){
        int incident=FireServiceLife.get(t,"started");
        if(p!=HousingLife.HOUSE_DISTRICT||incident<=0||get(t,"incident")!=incident||!t.player.ownsHouse||!t.player.alive)return;
        set(t,"loss",Math.min(100,get(t,"loss")+Math.max(0,loss)));
    }
    static void incidentEnded(TownLife.Town t){
        int incident=FireServiceLife.get(t,"started");
        if(incident<=0||get(t,"incident")!=incident)return;
        if(t.player.ownsHouse&&t.player.alive){
            int eligible=Math.min(MAX_CLAIM,Math.max(0,get(t,"loss")-DEDUCTIBLE));
            set(t,"pending",Math.addExact(get(t,"pending"),eligible));
        }
        set(t,"incident",0);set(t,"loss",0);
    }
    static boolean canCollect(TownLife.Town t,WorldState s){
        return office(t,s)&&get(t,"pending")>0&&t.employerFunds.getOrDefault(ACCOUNT,0)>0;
    }
    static String collect(TownLife.Town t,WorldState s){
        if(!canCollect(t,s))throw new IllegalArgumentException("No payable home claim or insurer funds.");
        int paid=Math.min(get(t,"pending"),t.employerFunds.getOrDefault(ACCOUNT,0));
        t.employerFunds.merge(ACCOUNT,-paid,Integer::sum);t.player.money=Math.addExact(t.player.money,paid);
        set(t,"pending",get(t,"pending")-paid);set(t,"paid",Math.addExact(get(t,"paid"),paid));
        return "Home insurance compensation / Indennizzo: EUR "+paid+". Outstanding EUR "+get(t,"pending")+".";
    }
    record View(boolean owned,int days,int pending,int paid,int insurerFunds){}
    static View view(TownLife.Town t,WorldState s){
        return new View(t.player.ownsHouse,t.player.ownsHouse?Math.max(0,get(t,"until")-s.day):0,get(t,"pending"),get(t,"paid"),t.employerFunds.getOrDefault(ACCOUNT,0));
    }
}
