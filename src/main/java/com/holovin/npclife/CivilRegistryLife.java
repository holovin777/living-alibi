package com.holovin.npclife;

/** Fictional civil records. Only the controlled visitor's document is public to
 * its client; other residents' private records are never exported in snapshots. */
final class CivilRegistryLife {
    private CivilRegistryLife(){}
    static final int CARD_FEE=5, VALID_DAYS=3650, HEALTH_VALID_DAYS=1825,
            CONSULT_PRICE=15, CONSULT_SUBSIDY=10, CONSULT_MINUTES=30;
    private static final String PREFIX="registry:";
    private static String citizen(TownLife.Citizen c,String field){return PREFIX+"citizen:"+c.id+":"+field;}
    private static String visitor(TownLife.Town t,String field){return PREFIX+"visitor:"+t.player.incarnation+":"+field;}
    private static int get(TownLife.Town t,String key){return t.industryStocks.getOrDefault(key,0);}
    private static void put(TownLife.Town t,String key,int n){t.industryStocks.put(key,n);}
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute!=480||get(t,PREFIX+"last-day")>=s.day)return;
        put(t,PREFIX+"last-day",s.day);
        for(var c:t.people.values()){
            if(get(t,citizen(c,"registered"))==0){
                // Existing residents are registered on first observation; never
                // pretend the registration day is their actual birth date.
                put(t,citizen(c,"registered"),s.day);
                put(t,citizen(c,"born"),DemographyLife.get(t,c,"born"));
                put(t,citizen(c,"district"),c.home.ordinal()+1);
            }
            if(!c.alive){
                if(get(t,citizen(c,"death-observed"))==0)put(t,citizen(c,"death-observed"),s.day);
                continue;
            }
            int district=c.home.ordinal()+1;
            if(get(t,citizen(c,"district"))!=district){
                put(t,citizen(c,"district"),district);
                put(t,citizen(c,"last-move"),s.day);
                put(t,citizen(c,"moves"),Math.addExact(get(t,citizen(c,"moves")),1));
            }
        }
    }
    private static String legalName(TownLife.Town t){
        String prefix=visitor(t,"name:");
        return t.industryStocks.keySet().stream().filter(k->k.startsWith(prefix))
                .map(k->k.substring(prefix.length())).findFirst().orElse(null);
    }
    static boolean fiscalRegistered(TownLife.Town t){return get(t,visitor(t,"issued"))>0;}
    private static boolean valid(TownLife.Town t,WorldState s){return get(t,visitor(t,"expires"))>s.day;}
    private static boolean registered(TownLife.Town t){return t.player.ownsHouse&&get(t,visitor(t,"residence"))>0;}
    private static boolean healthValid(TownLife.Town t,WorldState s){
        return get(t,visitor(t,"health-expires"))>s.day;
    }
    static int consultPrice(TownLife.Town t,WorldState s){
        return CONSULT_PRICE-(healthValid(t,s)&&MunicipalLife.balance(t,MunicipalLife.CLINIC)>=CONSULT_SUBSIDY?CONSULT_SUBSIDY:0);
    }
    static boolean canConsult(TownLife.Town t,WorldState s){
        var p=t.player;
        return !RebirthLife.active(t)&&p.alive&&p.travelRemaining==0&&p.place==TownLife.Place.CLINIC&&
                s.minute>=480&&s.minute+CONSULT_MINUTES<=1140&&p.health<100&&
                p.money>=consultPrice(t,s)&&get(t,visitor(t,"last-consult"))<s.day;
    }
    static String consult(TownLife.Town t,WorldState s){
        if(!canConsult(t,s))throw new IllegalArgumentException("Visit the clinic 08:00–19:00, with sufficient funds and a need for care; one consultation per day.");
        int price=consultPrice(t,s),aid=CONSULT_PRICE-price;
        int provider=Math.addExact(MunicipalLife.balance(t,MunicipalLife.CARE),CONSULT_PRICE);
        t.player.money-=price;
        if(aid>0)t.employerFunds.merge(MunicipalLife.CLINIC,-aid,Integer::sum);
        t.employerFunds.put(MunicipalLife.CARE,provider);
        t.player.health=Math.min(100,t.player.health+10);
        put(t,visitor(t,"last-consult"),s.day);
        return "Completed a medical consultation. Paid "+price+" EUR"+(aid>0?"; health service covered "+aid+" EUR.":".");
    }
    static String act(TownLife.Town t,WorldState s,String action){
        if(RebirthLife.active(t))throw new IllegalArgumentException("Visitor document service is not available in a born life yet.");
        var p=t.player;TransportLife.ready(p);
        if(p.place!=TownLife.Place.OFFICE||s.minute<540||s.minute>=780||(s.day-1)%7>=5)
            throw new IllegalArgumentException("Visit the municipal office Monday–Friday, 09:00–13:00.");
        if("registry-identity".equals(action)){
            if(valid(t,s))throw new IllegalArgumentException("Your identity card is already valid.");
            String name=legalName(t);
            if(name==null)name=PlayerLife.validateName(p.privateName==null?"":p.privateName);
            if(p.money<CARD_FEE)throw new IllegalArgumentException("An identity card costs 5 EUR.");
            int balance=Math.addExact(t.employerFunds.getOrDefault(MunicipalLife.CITY,0),CARD_FEE);
            p.money-=CARD_FEE;t.employerFunds.put(MunicipalLife.CITY,balance);
            put(t,visitor(t,"name:")+name,1);
            put(t,visitor(t,"issued"),s.day);put(t,visitor(t,"expires"),Math.addExact(s.day,VALID_DAYS));
            return "Municipality issued your fictional identity card.";
        }
        if("registry-health".equals(action)){
            if(!valid(t,s)||healthValid(t,s))throw new IllegalArgumentException("A valid identity card and an unissued or expired health card are required.");
            put(t,visitor(t,"health-expires"),Math.addExact(s.day,HEALTH_VALID_DAYS));
            return "Your fictional health card has been issued.";
        }
        if("registry-residence".equals(action)){
            if(!valid(t,s)||!p.ownsHouse||registered(t))
                throw new IllegalArgumentException("A valid identity card and an unregistered owned home are required.");
            put(t,visitor(t,"residence"),s.day);
            return "Your owned home is now registered with the municipality.";
        }
        throw new IllegalArgumentException("Unknown civil registry service.");
    }
    record Report(boolean identityValid,String identityNumber,String legalName,int expiresDay,
                  boolean residenceRegistered,boolean canRegisterResidence,String fiscalCode,
                  boolean healthCardValid,int healthCardExpiresDay,boolean canRequestHealthCard,
                  int consultationPrice,boolean canConsult){}
    static Report report(TownLife.Town t,WorldState s){
        if(RebirthLife.active(t)||!t.player.alive)return new Report(false,null,null,0,false,false,null,false,0,false,CONSULT_PRICE,false);
        boolean valid=valid(t,s),registered=registered(t),health=healthValid(t,s);
        boolean issued=get(t,visitor(t,"issued"))>0;
        return new Report(valid,valid?"LA-V-"+t.player.incarnation:null,valid?legalName(t):null,
                get(t,visitor(t,"expires")),registered,valid&&t.player.ownsHouse&&!registered,
                issued?"LA-CF-V-"+t.player.incarnation:null,health,get(t,visitor(t,"health-expires")),
                valid&&!health,consultPrice(t,s),canConsult(t,s));
    }
}
