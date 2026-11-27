package com.holovin.npclife;
import java.util.Comparator;
import static com.holovin.npclife.TownLife.Place.*;

/** Fictional paid defensive services and bounded property insurance. */
final class SecurityLife {
    private SecurityLife(){}
    static final String JOB="security-guard", OCCUPATION="Security guard", INSURER="property-insurance";
    static final int PACKAGE_PRICE=12, INSURANCE_PRICE=8, DAYS=7;
    static final String PREFIX="security:";
    static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(PREFIX+k,0);}
    static void set(TownLife.Town t,String k,int v){t.industryStocks.put(PREFIX+k,v);}
    static boolean guard(TownLife.Citizen c){return c.employed&&OCCUPATION.equals(c.occupation);}
    static boolean workshopOwner(TownLife.Town t){return t.business.registered&&t.business.ownerIncarnation==t.player.incarnation;}
    static boolean workshopContract(TownLife.Town t,int day,String field){
        return t.business.registered&&get(t,"workshop-owner")==t.business.ownerIncarnation&&get(t,field)>day;
    }
    static boolean marketContract(TownLife.Town t,int day){return get(t,"market-until")>day;}
    static boolean guardPresent(TownLife.Town t,WorldState s,TownLife.Place place){
        return t.people.values().stream().anyMatch(c->guard(c)&&c.presentAt(place)&&"WORKING".equals(c.activity))
            ||place==MARKET&&get(t,"visitor-shift-day")==s.day&&get(t,"visitor-shift-end")>s.minute;
    }
    static boolean prevent(TownLife.Town t,WorldState s,NpcPropertyCrimeLife.Kind kind,TownLife.Place place){
        boolean protectedSite=guardPresent(t,s,place)||(kind==NpcPropertyCrimeLife.Kind.SHOP&&marketContract(t,s.day))
                ||(kind==NpcPropertyCrimeLife.Kind.WORKSHOP&&workshopContract(t,s.day,"workshop-until"));
        if(!protectedSite)return false;
        // Abstract protective effect; no security bypass mechanics.
        if(Math.floorMod(Long.hashCode((long)NpcVehicleCrimeLife.now(s)*0x9e3779b97f4a7c15L),4)==0)return false;
        set(t,"prevented",Math.addExact(get(t,"prevented"),1));
        ArchiveLife.record(t,"security-prevented:"+s.day+":"+s.minute,s.day,s.minute,"Public safety",
                "A property incident was prevented by protective services.");
        return true;
    }
    static String purchase(TownLife.Town t,WorldState s,boolean insurance){
        TransportLife.ready(t.player);JusticeLife.requireFree(t);
        if(t.player.place!=WORKSHOP||!workshopOwner(t)||RebirthLife.active(t))
            throw new IllegalArgumentException("Visit your own workshop to manage protection.");
        String field=insurance?"insurance-until":"workshop-until";
        if(workshopContract(t,s.day,field))throw new IllegalArgumentException("Protection is already active.");
        int cost=insurance?INSURANCE_PRICE:PACKAGE_PRICE;
        if(t.business.cash<cost)throw new IllegalArgumentException("The business cannot afford protection.");
        if(get(t,"workshop-owner")!=t.business.ownerIncarnation){
            set(t,"insurance-until",0);set(t,"workshop-until",0);
        }
        set(t,"workshop-owner",t.business.ownerIncarnation);set(t,field,s.day+DAYS);
        t.business.cash-=cost;t.employerFunds.merge(insurance?INSURER:JOB,cost,Math::addExact);
        return insurance?"Workshop insurance purchased for seven days.":"Workshop protection purchased for seven days.";
    }
    static int compensate(TownLife.Town t,int owner,int loss){
        if(!t.business.registered||t.business.ownerIncarnation!=owner||loss<=0)return 0;
        int paid=Math.min(loss/2,t.employerFunds.getOrDefault(INSURER,0));
        t.employerFunds.merge(INSURER,-paid,Math::addExact);t.business.cash=Math.addExact(t.business.cash,paid);
        set(t,"claims",Math.addExact(get(t,"claims"),paid));return paid;
    }
    static void visitorShift(TownLife.Town t,int day,int minute){
        set(t,"visitor-shift-day",day);set(t,"visitor-shift-end",minute+CareerLife.SHIFT_MINUTES);
    }
    static void pay(TownLife.Town t,TownLife.Citizen c){
        int wage=Math.min(5,t.employerFunds.getOrDefault(JOB,0));
        if(wage>0){t.employerFunds.merge(JOB,-wage,Math::addExact);c.money=Math.addExact(c.money,wage);}
    }
    static void tick(TownLife.Town t,WorldState s){
        if(!NpcTransportLife.enabled(t)||s.minute!=360||t.people.size()<24)return;
        if(!marketContract(t,s.day)&&t.employerFunds.getOrDefault("shop-assistant",0)>=PACKAGE_PRICE+40){
            t.employerFunds.merge("shop-assistant",-PACKAGE_PRICE,Math::addExact);
            t.employerFunds.merge(JOB,PACKAGE_PRICE,Math::addExact);set(t,"market-until",s.day+DAYS);
        }
        if(t.people.values().stream().anyMatch(c->c.alive&&guard(c))||t.employerFunds.getOrDefault(JOB,0)<25)return;
        var candidate=t.people.values().stream().filter(c->c.alive&&!c.employed&&c.generationIndex>0
                &&DemographyLife.workingAge(c)&&!RebirthLife.linkedAdult(t,c)&&!NpcJusticeLife.confined(t,c,s))
                .min(Comparator.comparingInt(c->c.generationIndex)).orElse(null);
        if(candidate!=null){
            candidate.employed=true;candidate.occupation=OCCUPATION;candidate.workplace=MARKET;
            candidate.lastHiredDay=s.day;candidate.unemploymentDays=0;candidate.jobChanges++;
            EmploymentContractLife.signCitizen(t,candidate,s.day);SocialProtectionLife.hired(t,candidate);
        }
    }
    record View(boolean workshopOwned,boolean workshopProtected,boolean workshopInsured,int protectionDays,int insuranceDays,
                boolean marketProtected,boolean localGuard,int prevented,int compensation,int protectionPrice,int insurancePrice){}
    static View view(TownLife.Town t,WorldState s){
        boolean owner=workshopOwner(t),protect=owner&&workshopContract(t,s.day,"workshop-until"),insured=owner&&workshopContract(t,s.day,"insurance-until");
        return new View(owner,protect,insured,protect?get(t,"workshop-until")-s.day:0,
                insured?get(t,"insurance-until")-s.day:0,marketContract(t,s.day),guardPresent(t,s,t.player.place),
                get(t,"prevented"),get(t,"claims"),PACKAGE_PRICE,INSURANCE_PRICE);
    }
}
