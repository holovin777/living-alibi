package com.holovin.npclife;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A small, deterministic, headless town. Truth and witness knowledge are separate. */
final class TownLife {
    private TownLife() { }

    enum Place {
        MARCO_HOME("Marco's home"), ELENA_HOME("Elena's home"), BAKERY("Bakery"),
        MARKET("Market"), PARK("Park"), CAFE("Cafe"),
        NORTH_RESIDENCES("North residences"), SOUTH_RESIDENCES("South residences"),
        OFFICE("Office"), CLINIC("Clinic"), SCHOOL("School"),
        WORKSHOP("Workshop"), LIBRARY("Library"),
        RIVER("River"), WOODS("Woods"), QUARRY("Quarry"), FACTORY("Factory"), CENTRO("Centro Storico"), PORTICI("Via dei Portici"), FARM("Cascina delle Risaie"), FUEL_STATION("Stazione di servizio"), LOGISTICS_DEPOT("Deposito Logistico");
        final String label;
        Place(String label) { this.label=label; }
    }

    static final class Citizen {
        final String id;
        final String name;
        String occupation;
        Place place;
        Place destination;
        int travelRemaining;
        String activity;
        int hunger;
        int thirst;
        int fatigue;
        int health;
        int money;
        int unpaidBills;
        boolean employed=true;
        int unemploymentDays;
        int sickUntilDay;
        int lastHiredDay;
        int jobChanges;
        int food;
        int water;
        int age;
        int sociability;
        int carefulness;
        int curiosity;
        int scheduleOffset;
        int generationIndex;
        long personalSeed;
        String goal;
        Place home;
        Place workplace;
        /** Individual private residence within a coarse neighborhood location. */
        String homeUnit;
        boolean alive;

        Citizen(String id, String name, String occupation, Place place) {
            this.id=id;this.name=name;this.occupation=occupation;
            this.place=place;this.destination=place;this.activity="RELAXING";
            this.hunger=20;this.thirst=20;this.fatigue=20;this.health=100;
            this.money=55;this.food=3;this.water=3;this.age=32;this.alive=true;this.sociability=62;
            this.carefulness=55;this.curiosity=50;this.goal="STABILITY";
            this.home=place;this.workplace=place;this.personalSeed=0;
            this.homeUnit=privateUnit(id);
        }
        Citizen copy() {
            Citizen c=new Citizen(id,name,occupation,place);
            c.destination=destination;c.travelRemaining=travelRemaining;c.activity=activity;
            c.hunger=hunger;c.thirst=thirst;c.fatigue=fatigue;c.health=health;
            c.money=money;c.unpaidBills=unpaidBills;c.food=food;c.water=water;c.age=age;c.alive=alive;c.sociability=sociability;
            c.carefulness=carefulness;c.curiosity=curiosity;c.scheduleOffset=scheduleOffset;
            c.generationIndex=generationIndex;c.personalSeed=personalSeed;c.goal=goal;
            c.home=home;c.workplace=workplace;c.homeUnit=homeUnit;
            c.employed=employed;c.unemploymentDays=unemploymentDays;
            c.sickUntilDay=sickUntilDay;c.lastHiredDay=lastHiredDay;c.jobChanges=jobChanges;
            return c;
        }
        boolean presentAt(Place p) {return alive && travelRemaining==0 && place==p;}
        /** A shared neighborhood does not imply a shared apartment or line of sight. */
        String currentSpace() {
            return place==home && !"OUTSIDE_HOME".equals(activity) ? homeUnit : publicSpace(place);
        }
        String locationText() {
            if(!alive)return place.label+" (deceased)";
            if(travelRemaining>0)return "in transit to "+destination.label+" ("+travelRemaining+" min remaining)";
            return sceneLabel(place,currentSpace());
        }
    }

    static final class Town {
        final Map<String,Citizen> people=new LinkedHashMap<>();
        PlayerLife.Player player=new PlayerLife.Player();
        BusinessLife.Shop business=new BusinessLife.Shop();
        final Map<Integer,PlayerLife.LifeRecord> pastLives=new LinkedHashMap<>();
        final Map<String,NatureLife.Source> naturalResources=new LinkedHashMap<>();
        final Map<String,Integer> employerFunds=new LinkedHashMap<>();
        final Map<String,Integer> retailStock=new LinkedHashMap<>();
        final Map<String,Integer> technicalMaterials=new LinkedHashMap<>();
        final Map<String,TechnicalWorkLife.Order> technicalOrders=new LinkedHashMap<>();
        final Map<String,Integer> industryStocks=new LinkedHashMap<>();
        final Map<String,ArchiveLife.Entry> archive=new LinkedHashMap<>();
        final List<ArchiveLife.Entry> pendingArchive=new ArrayList<>();
        int lastIndustryExtractDay,lastIndustryTransportDay,lastIndustryRefineDay;
        int lastIndustryFabricateDay,lastIndustryDeliverDay;
        int lastIndustryArrivalDay,lastIndustryRecycleDay;
        int cleanWater;
        int lastWaterPumpDay;
        boolean together;
        long meetingSinceTick=-1;
        SocialLife.Relationship relationship=new SocialLife.Relationship("marco","elena");
        /** Other pairs are created on first encounter, rather than preallocating N² relationships. */
        final Map<String,SocialLife.Relationship> peerRelationships=new LinkedHashMap<>();
        /** New in v1.6: per-resident claims with source, confidence and provenance. */
        final Map<String,LinkedHashMap<String,KnowledgeLife.Belief>> beliefs=new LinkedHashMap<>();
        final List<KnowledgeLife.Transfer> pendingTransfers=new ArrayList<>();
        final Map<String,LinkedHashMap<String,DeceptionLife.Secret>> secrets=new LinkedHashMap<>();
        final List<DeceptionLife.Decision> pendingDecisions=new ArrayList<>();
        final Map<String,InvestigationLife.CaseFile> cases=new LinkedHashMap<>();
        final Map<String,EconomyLife.Loan> loans=new LinkedHashMap<>();
        final Map<String,PropertyLife.Item> items=new LinkedHashMap<>();
        final List<PropertyLife.Change> pendingProperty=new ArrayList<>();
        final List<EconomyLife.Entry> pendingEconomy=new ArrayList<>();
        final List<EmploymentLife.Event> pendingLifeEvents=new ArrayList<>();
        SocialLife.Relationship existingRelationship(String id1,String id2) {
            String a=id1.compareTo(id2)<=0?id1:id2;
            String b=id1.compareTo(id2)<=0?id2:id1;
            if(a.equals(b))throw new IllegalArgumentException("Choose two different residents.");
            if(a.equals("elena")&&b.equals("marco"))return relationship;
            return peerRelationships.get(a+"|"+b);
        }
        Citizen get(String id) {
            Citizen p=people.get(id.toLowerCase());
            if(p==null)throw new IllegalArgumentException("Unknown resident: "+id+". Use population to list IDs.");
            return p;
        }
        Town copy() {
            Town t=new Town();t.player=player.copy();t.together=together;t.meetingSinceTick=meetingSinceTick;
            t.relationship=relationship.copy();
            for(var e:peerRelationships.entrySet())t.peerRelationships.put(e.getKey(),e.getValue().copy());
            for(var e:people.entrySet())t.people.put(e.getKey(),e.getValue().copy());
            for(var e:naturalResources.entrySet())
                t.naturalResources.put(e.getKey(),e.getValue().copy());
            t.pastLives.putAll(pastLives); // Immutable history records.
            t.employerFunds.putAll(employerFunds);
            t.retailStock.putAll(retailStock);
            t.technicalMaterials.putAll(technicalMaterials);
            for(var e:technicalOrders.entrySet())
                t.technicalOrders.put(e.getKey(),e.getValue().copy());
            t.business=business.copy();
            t.industryStocks.putAll(industryStocks);
            t.archive.putAll(archive);t.pendingArchive.addAll(pendingArchive);
            t.lastIndustryExtractDay=lastIndustryExtractDay;
            t.lastIndustryTransportDay=lastIndustryTransportDay;
            t.lastIndustryRefineDay=lastIndustryRefineDay;
            t.lastIndustryFabricateDay=lastIndustryFabricateDay;
            t.lastIndustryDeliverDay=lastIndustryDeliverDay;
            t.lastIndustryArrivalDay=lastIndustryArrivalDay;
            t.lastIndustryRecycleDay=lastIndustryRecycleDay;
            t.cleanWater=cleanWater;t.lastWaterPumpDay=lastWaterPumpDay;
            for(var e:beliefs.entrySet())t.beliefs.put(e.getKey(),new LinkedHashMap<>(e.getValue()));
            t.pendingTransfers.addAll(pendingTransfers);
            for(var e:secrets.entrySet())t.secrets.put(e.getKey(),new LinkedHashMap<>(e.getValue()));
            t.pendingDecisions.addAll(pendingDecisions);
            for(var e:cases.entrySet())t.cases.put(e.getKey(),e.getValue().copy());
            for(var e:loans.entrySet())t.loans.put(e.getKey(),e.getValue().copy());
            for(var e:items.entrySet())t.items.put(e.getKey(),e.getValue().copy());
            t.pendingProperty.addAll(pendingProperty);
            t.pendingEconomy.addAll(pendingEconomy);
            t.pendingLifeEvents.addAll(pendingLifeEvents);
            return t;
        }
        void copyFrom(Town other) {
            people.clear();for(var e:other.people.entrySet())people.put(e.getKey(),e.getValue().copy());
            player=other.player.copy();
            pastLives.clear();
            pastLives.putAll(other.pastLives);
            employerFunds.clear();
            employerFunds.putAll(other.employerFunds);
            retailStock.clear();retailStock.putAll(other.retailStock);
            technicalMaterials.clear();
            technicalMaterials.putAll(other.technicalMaterials);
            technicalOrders.clear();
            for(var e:other.technicalOrders.entrySet())
                technicalOrders.put(e.getKey(),e.getValue().copy());
            business=other.business.copy();
            industryStocks.clear();industryStocks.putAll(other.industryStocks);
            archive.clear();archive.putAll(other.archive);pendingArchive.clear();pendingArchive.addAll(other.pendingArchive);
            lastIndustryExtractDay=other.lastIndustryExtractDay;
            lastIndustryTransportDay=other.lastIndustryTransportDay;
            lastIndustryRefineDay=other.lastIndustryRefineDay;
            lastIndustryFabricateDay=other.lastIndustryFabricateDay;
            lastIndustryDeliverDay=other.lastIndustryDeliverDay;
            lastIndustryArrivalDay=other.lastIndustryArrivalDay;
            lastIndustryRecycleDay=other.lastIndustryRecycleDay;
            cleanWater=other.cleanWater;lastWaterPumpDay=other.lastWaterPumpDay;
            naturalResources.clear();
            for(var e:other.naturalResources.entrySet())
                naturalResources.put(e.getKey(),e.getValue().copy());
            together=other.together;meetingSinceTick=other.meetingSinceTick;
            relationship=other.relationship.copy();
            peerRelationships.clear();
            for(var e:other.peerRelationships.entrySet())peerRelationships.put(e.getKey(),e.getValue().copy());
            beliefs.clear();
            for(var e:other.beliefs.entrySet())beliefs.put(e.getKey(),new LinkedHashMap<>(e.getValue()));
            pendingTransfers.clear();pendingTransfers.addAll(other.pendingTransfers);
            secrets.clear();
            for(var e:other.secrets.entrySet())secrets.put(e.getKey(),new LinkedHashMap<>(e.getValue()));
            pendingDecisions.clear();pendingDecisions.addAll(other.pendingDecisions);
            cases.clear();for(var e:other.cases.entrySet())cases.put(e.getKey(),e.getValue().copy());
            loans.clear();for(var e:other.loans.entrySet())loans.put(e.getKey(),e.getValue().copy());
            pendingEconomy.clear();pendingEconomy.addAll(other.pendingEconomy);
            items.clear();for(var e:other.items.entrySet())items.put(e.getKey(),e.getValue().copy());
            pendingProperty.clear();pendingProperty.addAll(other.pendingProperty);
            pendingLifeEvents.clear();pendingLifeEvents.addAll(other.pendingLifeEvents);
        }
    }

    /** spaceId distinguishes a private unit from a public scene within the same Place. */
    record Event(int day,int minute,String actorId,Place place,String spaceId,String text) { }
    record Observation(String witnessId,int day,int minute,Place place,String spaceId,String text) { }

    static String privateUnit(String id) {return "unit:"+id;}
    static String publicSpace(Place where) {return "public:"+where.name();}
    static String sceneLabel(Place place,String spaceId) {
        if(spaceId!=null && spaceId.startsWith("unit:"))
            return place.label+" / private unit "+spaceId.substring(5);
        return place.label;
    }
    static Town create(WorldState marco) {
        Town t=new Town();
        Place initial=desiredMarco(marco,null);
        t.people.put("marco",new Citizen("marco","Marco Bianchi","Baker",initial));
        t.get("marco").home=Place.MARCO_HOME;t.get("marco").workplace=Place.BAKERY;
        t.get("marco").carefulness=marco.personalityCareful;
        t.people.put("elena",new Citizen("elena","Elena Rossi","Shopkeeper",desiredElena(marco.day,marco.minute)));
        t.get("elena").home=Place.ELENA_HOME;t.get("elena").workplace=Place.MARKET;
        t.get("elena").activity="WORKING";
        NatureLife.ensureSources(t);
        CareerLife.ensureEmployers(t);
        RetailLife.ensure(t);
        BusinessLife.ensure(t);
        TechnicalWorkLife.ensure(t);
        IndustryLife.ensure(t);
        FarmLife.ensure(t);
        FuelLife.ensure(t);
        FreightLife.ensure(t);
        GardenLife.ensure(t);
        HenLife.ensure(t);
        OrchardLife.ensure(t);
        MarketStallLife.ensure(t);
        BankLife.ensure(t);
        RentalLife.ensure(t);
        ConstructionLife.ensure(t);
        MunicipalLife.ensure(t);
        DemographyLife.ensure(t);
        EducationLife.ensure(t);
        SoftwareLife.ensure(t);
        DevOpsLife.ensure(t);
        NewspaperLife.ensure(t);
        OpinionLife.ensure(t);
        ElectionLife.ensure(t);
        RebirthLife.ensure(t);
        t.together=initial==t.get("elena").place;
        if(t.together)t.meetingSinceTick=(long)(marco.day-1)*1440L+marco.minute;
        return t;
    }

    static void tick(Town town,WorldState marco,List<Event> truth,List<Observation> observations) {
        tick(town,marco,truth,observations,new ArrayList<>());
    }
    static void tick(Town town,WorldState marco,List<Event> truth,List<Observation> observations,
                     List<SocialLife.Interaction> interactions) {
        PlayerLife.tick(town.player,marco.minute);
        HousingLife.expire(town.player,marco.day,marco.minute);
        GardenLife.tick(town,marco.day,marco.minute);
        HenLife.tick(town,marco.day,marco.minute);
        OrchardLife.tick(town,marco.day,marco.minute);
        NatureLife.tick(town,marco.day,marco.minute);
        RetailLife.tick(town,marco.minute);
        FarmLife.tick(town,marco.day,marco.minute);
        FuelLife.tick(town,marco.day,marco.minute);
        StallSupplyLife.tick(town,marco.day,marco.minute);
        BankLife.tick(town,marco.day,marco.minute);
        TechnicalWorkLife.tick(town,marco.day,marco.minute);
        ElectionLife.tick(town,marco.day,marco.minute);
        MunicipalLife.tick(town,marco.day,marco.minute);
        PropertyLife.ensureStarterItems(town,marco);
        Citizen m=town.get("marco"), e=town.get("elena");
        m.alive=marco.alive;
        m.carefulness=marco.personalityCareful;
        m.age=marco.age;
        m.hunger=marco.hunger;m.thirst=marco.thirst;m.fatigue=marco.fatigue;
        m.health=marco.health;m.money=marco.money;m.activity=marco.activity;
        m.food=marco.food;m.water=marco.water;
        if(m.alive)move(town,m,desiredMarco(marco,m.place),marco,truth,observations);
        elenaNeeds(e,marco,truth,observations,town);
        if(e.alive)move(town,e,desiredElena(marco.day,marco.minute,e,town),marco,truth,observations);
        DemographyLife.tick(town,marco,truth,observations);
        EmploymentLife.tick(town,marco,truth,observations);
        PopulationLife.tick(town,marco,truth,observations);
        IndustryLife.tick(town,marco,truth,observations);
        ConstructionLife.tick(town,marco.day,marco.minute);
        BusinessLife.tick(town,marco,truth,observations);
        LogisticsLife.tick(town,marco,truth,observations);
        SecondFleetLife.tick(town,marco,truth,observations);
        RentalLife.tick(town,marco.day,marco.minute);
        EconomyLife.tick(town,marco,truth,observations);
        PropertyLife.tick(town,marco);
        InvestigationLife.tick(town,marco,truth,observations);
        UtilityLife.tick(town,marco,truth,observations);
        DevOpsLife.tick(town,marco.day,marco.minute);
        ITStaffLife.tick(town,marco.day,marco.minute);
        ArchiveLife.annual(town,marco.day,marco.minute);
        NewspaperLife.tick(town,marco.day,marco.minute);
        boolean nowTogether=m.alive&&e.alive&&m.travelRemaining==0&&e.travelRemaining==0&&m.place==e.place;
        if(nowTogether&&!town.together) {
            town.meetingSinceTick=(long)(marco.day-1)*1440L+marco.minute;
            SocialLife.onEncounter(town);
            publish(town,marco,"marco",m.place,"Marco Bianchi and Elena Rossi crossed paths at "+m.place.label+".",truth,observations);
        }
        if(!nowTogether)town.meetingSinceTick=-1;
        town.together=nowTogether;
        int before=interactions.size();
        SocialLife.tick(town,marco,truth,observations,interactions);
        SocialNetwork.tick(town,marco,truth,observations,interactions);
        MutualAidLife.tick(town,marco);
        PeerAidLife.tick(town,marco,truth,observations);
        for(int i=before;i<interactions.size();i++) {
            var conversation=interactions.get(i);
            KnowledgeLife.exchange(town,marco,conversation.firstId(),conversation.secondId());
            if(conversation.kind()==SocialLife.Kind.FRIENDLY_CHAT) {
                var a=town.people.get(conversation.firstId());var b=town.people.get(conversation.secondId());
                if(MarketStallLife.recommend(town,a,b))
                    publish(town,marco,a.id,a.place,a.name+" recommended the private market stall to "+b.name+".",truth,observations);
                if(MarketStallLife.recommend(town,b,a))
                    publish(town,marco,b.id,b.place,b.name+" recommended the private market stall to "+a.name+".",truth,observations);
            }
        }
    }

    private static int clamp(int n) {return Math.max(0,Math.min(100,n));}
    private static void elenaNeeds(Citizen e,WorldState clock,List<Event> events,List<Observation> observations,Town town) {
        if(!e.alive)return;
        int minute=clock.minute;
        if(minute==0 && clock.day>1 && (clock.day-1)%365==0) {
            e.age++;
            publish(town,clock,"elena",e.place,"Elena Rossi turned "+e.age+".",events,observations);
        }
        if(minute%12==0)e.hunger=clamp(e.hunger+1);
        if(minute%10==0)e.thirst=clamp(e.thirst+1);
        boolean sleeping=minute<390||minute>=1320;
        e.activity=sleeping?"SLEEPING":(isWorkTime(clock.day,minute)?"WORKING":"FREE_TIME");
        if(minute%15==0)e.fatigue=clamp(e.fatigue+(sleeping?-3:1));
        if(e.travelRemaining>0)return;
        if(!sleeping&&e.hunger>=65&&e.food>0&&(e.place==Place.ELENA_HOME||e.place==Place.MARKET)) {
            e.food--;e.hunger=clamp(e.hunger-48);
            publish(town,clock,"elena",e.place,"Elena Rossi ate a meal.",events,observations);
        }
        if(!sleeping&&e.thirst>=65&&e.water>0&&(e.place==Place.ELENA_HOME||e.place==Place.MARKET)) {
            e.water--;e.thirst=clamp(e.thirst-50);
            publish(town,clock,"elena",e.place,"Elena Rossi drank water.",events,observations);
        }
        if(e.place==Place.MARKET && minute>=420 && minute<1200) {
            if(e.food<2 && e.money>=9) {
                e.food+=3;e.money-=9;
                publish(town,clock,"elena",e.place,"Elena Rossi purchased groceries.",events,observations);
            }
            if(e.water<2 && e.money>=3) {
                e.water+=3;e.money-=3;
                publish(town,clock,"elena",e.place,"Elena Rossi purchased bottled water.",events,observations);
            }
            if(isWorkTime(clock.day,minute)&&minute%60==0) {
                e.money+=12;
                publish(town,clock,"elena",e.place,"Elena Rossi earned her hourly wage at the market.",events,observations);
            }
        }
        if(minute%60==0 && (e.hunger>93||e.thirst>93)) e.health=clamp(e.health-2);
        // Elena can recover after a shortage instead of remaining permanently at low health.
        if(minute%180==0 && e.hunger<65 && e.thirst<60 && e.fatigue<85)
            e.health=clamp(e.health+1);
        if(e.health==0) {
            e.alive=false;e.activity="DECEASED";
            publish(town,clock,"elena",e.place,"Elena Rossi died from declining health.",events,observations);
        }
    }
    private static boolean isWorkTime(int day,int minute) {
        return (day-1)%7<5 && minute>=420 && minute<960;
    }
    private static Place desiredElena(int day,int minute) {
        // Initial placement, before the citizen has a local inventory snapshot.
        if(minute<390||minute>=1320)return Place.ELENA_HOME;
        if(isWorkTime(day,minute))return Place.MARKET;
        if(minute>=960 && minute<1200)return (day%3==0)?Place.CAFE:Place.PARK;
        return Place.ELENA_HOME;
    }
    private static Place desiredElena(int day,int minute,Citizen e,Town town) {
        if(minute<390 || minute>=1320)return Place.ELENA_HOME;
        if(isWorkTime(day,minute))return Place.MARKET;
        // Shopping is a separate need, not an effect of having a weekday job.
        // On weekends she still visits the market when food/water runs low.
        if(minute>=420 && minute<1200 &&
                ((e.food<2 && e.money>=9) || (e.water<2 && e.money>=3)))
            return Place.MARKET;
        if(minute>=960 && minute<1200) {
            if(day%3==0 || (minute>=1080 && !UtilityLife.status(town).lightsWorking()))
                return Place.CAFE;
            return Place.PARK;
        }
        return Place.ELENA_HOME;
    }
    private static Place desiredMarco(WorldState s,Place current) {
        if(!s.alive)return current==null?Place.MARCO_HOME:current;
        return switch(s.activity) {
            case "WORKING" -> Place.BAKERY;
            case "SOCIALIZING" -> Place.CAFE;
            case "EXPLORING_TOWN", "WALKING_FOR_HEALTH" -> Place.PARK;
            case "SLEEPING", "RESTING_WHILE_SICK", "RELAXING_AT_HOME", "WASHING" -> Place.MARCO_HOME;
            // Short-lived food, water and shopping actions are not yet spatially simulated.
            default -> current==null?Place.MARCO_HOME:current;
        };
    }
    static int travelTime(Place from,Place to) {
        // Coarse walking times; no route geometry yet.
        return from==to?0:(from==Place.PARK||to==Place.PARK?10:8);
    }
    static void move(Town town,Citizen c,Place desired,WorldState clock,List<Event> events,List<Observation> observations) {
        if(c.travelRemaining>0) {
            c.travelRemaining--;
            if(c.travelRemaining==0) {
                c.place=c.destination;
                publish(town,clock,c.id,c.place,c.name+" arrived at "+c.place.label+".",events,observations);
            }
            return;
        }
        if(c.place!=desired) {
            c.destination=desired;
            c.travelRemaining=travelTime(c.place,desired);
            c.activity="TRAVELING";
        }
    }
    static void publish(Town t,WorldState clock,String actor,Place where,String text,
                                List<Event> truth,List<Observation> observations) {
        Citizen source=t.people.get(actor);
        // The event has one authoritative scene. Being in the same neighborhood
        // is not enough to witness an action inside somebody else's apartment.
        String scene=source!=null && source.place==where
                ? source.currentSpace() : publicSpace(where);
        Event event=new Event(clock.day,clock.minute,actor,where,scene,text);
        truth.add(event);
        for(Citizen witness:t.people.values()) {
            if(witness.presentAt(where) && witness.currentSpace().equals(scene)) {
                observations.add(new Observation(witness.id,clock.day,clock.minute,where,scene,text));
                KnowledgeLife.witnessed(t,event,witness.id);
            }
        }
    }
}
