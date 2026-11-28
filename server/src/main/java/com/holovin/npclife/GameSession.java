package com.holovin.npclife;

import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Single authoritative game session for a local Godot client.
 * All state changes are serialized and committed before a response is returned.
 */
@Service
public final class GameSession implements AutoCloseable {
    private final WorldDatabase db;
    private WorldState state;
    private TownLife.Town town;
    private int archiveYear,archivePage=1;
    private final boolean localOfflineProgress;
    private final boolean persistentWorld;
    private OfflineRoutineLife.View offlineReport=new OfflineRoutineLife.View(0,false,false);
    private boolean closed;
    private final List<SocialLife.Interaction> recentPeerEvents=new ArrayList<>();

    static final String POPULATION_SEED_TARGET="population:seed-target";

    @Autowired
    public GameSession(@Value("${living-alibi.database:data/world.db}") String databasePath,
                       @Value("${living-alibi.population:240}") int population,
                       @Value("${living-alibi.persistent-world:false}") boolean persistentWorld)
            throws Exception {
        this(Path.of(databasePath),population,true,persistentWorld,true);
    }

    // Small deterministic fixtures remain available to existing regression tests.
    GameSession(Path databasePath) throws Exception {
        this(databasePath,12);
    }

    GameSession(Path databasePath,int population) throws Exception {
        this(databasePath,population,false);
    }
    GameSession(Path databasePath,int population,boolean randomBirth) throws Exception {
        this(databasePath,population,randomBirth,false);
    }
    GameSession(Path databasePath,int population,boolean randomBirth,boolean persistentWorld) throws Exception {
        this(databasePath,population,randomBirth,persistentWorld,false);
    }
    GameSession(Path databasePath,int population,boolean randomBirth,boolean persistentWorld,boolean chooseFirstSex) throws Exception {
        this.persistentWorld=persistentWorld;
        if(population<12||population>PopulationGenerator.MAX_GENERATED)
            throw new IllegalArgumentException("Initial generated population must be between 12 and "+PopulationGenerator.MAX_GENERATED);
        localOfflineProgress=randomBirth&&!persistentWorld;
        boolean firstLaunch=Files.notExists(databasePath);
        db=new WorldDatabase(databasePath);
        state=db.load();
        long previousSave=state.lastSavedEpoch;
        town=db.loadTown(state);
        recentPeerEvents.addAll(db.socialEvents(512));
        prunePeerEvents((long)(state.day-1)*1440L+state.minute);
        int previousTarget=town.industryStocks.getOrDefault(POPULATION_SEED_TARGET,12);
        boolean changed=false;
        int firstNew=0;
        if(firstLaunch||population>previousTarget) {
            // One-time immigration, not an automatic respawn after deaths.
            // Reserve room for initial newborns when the requested population
            // is close to the hard cap. Never remove an existing resident.
            int adultTarget=firstLaunch&&randomBirth?Math.min(population,PopulationGenerator.MAX_GENERATED-4):population;
            int missing=Math.max(0,adultTarget-(int)DemographyLife.living(town));
            if(missing>0)firstNew=town.people.values().stream().mapToInt(c->c.generationIndex).max().orElse(0)+1;
            while(missing>0){
                int batch=Math.min(20,missing);
                PopulationGenerator.add(town,batch);
                missing-=batch;
            }
            town.industryStocks.put(POPULATION_SEED_TARGET,population);
            changed=true;
        }
        changed=PoliceLife.establish(town,state,population)||changed;
        changed=StaffingLife.establish(town,state,population,firstNew)||changed;
        changed=FactoryNetworkLife.ensure(town)||changed;
        if(population>=24&&!NpcTransportLife.enabled(town)){town.industryStocks.put(NpcTransportLife.ENABLED,1);changed=true;}
        if(randomBirth){
            changed=RebirthLife.enableRandomBirth(town,firstLaunch)||changed;
            if(firstLaunch&&chooseFirstSex){FirstLifeChoice.require(town);changed=true;}
            if(!FirstLifeChoice.pending(town))changed=InitialFamilyLife.start(town,state)||changed;
        }
        // Old save files might contain a deceased visitor.
        changed=PassportLife.ensure(town,state)||changed;
        changed=RebirthLife.restoreIdentity(town)||changed;
        if(!town.player.alive&&!RebirthLife.active(town)) {
            RebirthLife.prepare(town,state.day,state.minute);
            changed=true;
        }
        if(RebirthLife.settleLinkedDeath(town,state.day,state.minute))changed=true;
        RebirthLife.synchronize(town);
        if(localOfflineProgress){
            state.offlineProgress=true;
            int minutes=firstLaunch?0:OfflineRoutineLife.minutes(previousSave,java.time.Instant.now().getEpochSecond());
            if(minutes>0){
                int dayBefore=state.day,minuteBefore=state.minute,lifeBefore=town.player.incarnation;
                var copy=town.copy();
                OfflineRoutineLife.prepare(copy);
                boolean confined=JusticeLife.confined(copy);
                if(confined)minutes=Math.min(minutes,JusticeLife.waitDuration(copy,state,minutes));
                applyInternal(copy,minutes,"Routine while away.",RebirthLife.active(copy),state.copy(),new ArrayList<>(),new ArrayList<>(),confined);
                int advanced=(state.day-dayBefore)*1440+state.minute-minuteBefore;
                offlineReport=new OfflineRoutineLife.View(advanced,OfflineRoutineLife.minutes(previousSave,java.time.Instant.now().getEpochSecond())==OfflineRoutineLife.MAX_MINUTES,town.player.incarnation!=lifeBefore);
            }else db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }else if(changed)db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
    }

    public record PlayerView(int incarnation,String place,String placeId,String privateName,
                             int hunger,int thirst,int fatigue,int health,boolean alive,
                             int money,String jobId,int engineeringSkill,int investigationSkill,
                             int bread,int bottledWater,long roomUntil,boolean roomActive,int storedBread,int storedWater,int hygiene,
                             java.util.Map<String,Integer> bag,java.util.Map<String,Integer> pantry,boolean ownsBicycle,boolean cycling,String bicycleParked,String carPlace,int carFuel,int carCondition,List<String> carDestinations,boolean ownsHouse,boolean ownsBaton,int policeReports,JusticeLife.View justice,WeaponLicenseLife.View weaponLicense,PassportLife.View passport) { }
    public record BusinessView(boolean registered,boolean yours,int ownerIncarnation,
                               boolean machine,int machineCondition,int cash,String employeeId,
                               String employeeName,int spareParts,int paidHoursToday,
                               int lastProductionDay,int lastSaleDay) { }
    public record CandidateView(String id,String name,String currentOccupation) { }
    public record JobView(String id,String title,String workplace,String subject,
                          int requiredSkill,int wage,int remainingPayroll) { }
    public record WorkOrderView(String id,String title,String jobId,String place,
                                String phase,int condition,int completedDay,
                                String materialA,int requiredA,String materialB,int requiredB) { }
    public record DestinationView(String id,String label,int travelMinutes) { }
    public record ResidentView(String id,String name,String activity,String knownName,int conversations,String relationship,String mood,int personalDebt,int personalDebtDueDay,String workVisual,String outfitVisual,String appearanceStage,String walkingCompanion,int conversationMinutes,String conversationReason,String factoryStation) { }
    // Only the observable workstation of an on-duty resident, never private employment history.
    static String factoryStation(TownLife.Citizen c){
        if(!c.alive||c.travelRemaining>0||!"WORKING".equals(c.activity)||c.place!=TownLife.Place.FACTORY)return "";
        if("Factory machinist".equals(c.occupation))return "metalworks";
        return FactoryNetworkLife.PLANTS.stream().filter(p->p.site()==c.place&&p.occupation().equals(c.occupation))
            .map(FactoryNetworkLife.Plant::id).findFirst().orElse("");
    }
    // Observable equipment, not a disclosure of a stranger's name or private employment record.
    static String workVisual(TownLife.Citizen c){
        if(!"WORKING".equals(c.activity)||c.travelRemaining>0)return "";
        String role=c.occupation==null?"":c.occupation;
        if(PoliceLife.officer(c)||PoliceLife.detective(c))return "";
        if(PostalLife.postalWorker(c))return "MAIL_BAG";
        return switch(c.place){
            case CENTRO -> role.equals("Pizzaiolo")?"PIZZA":role.equals("Gelataio")?"GELATO":role.equals("Barber")?"BARBER":"";
            case BAKERY -> role.equals("Baker")?"BREAD":"";
            case CAFE -> role.equals("Barista")?"COFFEE":"";
            case MARKET -> role.contains("vendor")||role.contains("seller")||role.equals("Shopkeeper")?"CRATE":"";
            case QUARRY -> role.equals("Miner")?"PICK":"";
            case WORKSHOP -> role.equals("Mechanic")||role.equals("Engineer")||role.equals("Independent fabricator")?"WRENCH":"";
            case FACTORY -> factoryStation(c).isEmpty()?"":"WRENCH";
            case SCHOOL -> role.equals("Teacher")?"TEACHING":"PAPERS";
            case CLINIC -> switch(role){
                case "Doctor" -> "DOCTOR";
                case "Nurse" -> "NURSE";
                case "Pharmacist" -> "PHARMACY";
                default -> "PAPERS";
            };
            case LIBRARY -> role.equals("Librarian")?"LIBRARY":"PAPERS";
            case OFFICE -> "PAPERS";
            default -> "";
        };
    }
    // Coarse visible age group, not a private birth date or exact age.
    static String appearanceStage(TownLife.Citizen c){
        if(c.age<6)return "PRESCHOOL";
        if(c.age<12)return "CHILD";
        if(c.age<18)return "TEEN";
        if(c.age<45)return "ADULT";
        if(c.age<67)return "MATURE";
        return "SENIOR";
    }
    // Visible clothing only; do not expose a stranger's age or occupation string.
    static String outfitVisual(TownLife.Citizen c){
        if(!c.alive||c.travelRemaining>0)return "";
        if(c.place==TownLife.Place.SCHOOL&&c.age>=6&&c.age<18&&"STUDYING".equals(c.activity))return "BACKPACK";
        if(!"WORKING".equals(c.activity))return "";
        String role=c.occupation==null?"":c.occupation;
        if(FireServiceLife.firefighter(c))return "FIREFIGHTER";
        if(PoliceLife.officer(c))return "POLICE";
        if(PoliceLife.detective(c))return "DETECTIVE";
        if(PostalLife.postalWorker(c))return "POSTAL";
        return switch(c.place){
            case CENTRO -> role.equals("Pizzaiolo")?"BAKER_APRON":role.equals("Gelataio")?"CAFE_APRON":"";
            case BAKERY -> role.equals("Baker")?"BAKER_APRON":"";
            case CAFE -> role.equals("Barista")?"CAFE_APRON":"";
            case CLINIC -> role.equals("Doctor")||role.equals("Pharmacist")?"DOCTOR_COAT":role.equals("Nurse")?"SCRUBS":"";
            case QUARRY -> role.equals("Miner")?"HIGH_VIS":"";
            case FACTORY -> factoryStation(c).isEmpty()?"":"HIGH_VIS";
            case WORKSHOP -> switch(role){
                case "Engineer","Electrician","Technician","Mechanic","Independent fabricator" -> "TECHNICIAN";
                case "Construction worker","Builder" -> "HIGH_VIS";
                default -> "";
            };
            case LOGISTICS_DEPOT -> role.equals("Transport driver")?"HIGH_VIS":"";
            default -> "";
        };
    }
    public record SourceView(String id,String label,String kind,int stock,int capacity) { }
    public record WeatherView(String sky,int temperatureC,boolean raining,boolean snowing) { }
    public record ServicesView(int pumpCondition,boolean fountainWorking,int cleanWater,
                               int cleanWaterCapacity,int lightingCondition,boolean lightsWorking) { }
    public record IndustryView(int ironRemaining,int copperRemaining,int oreAtQuarry,
                               int oreAtFactory,int factorySteel,int factoryCopper,
                               int factoryParts,int workshopSteel,int workshopCopper,
                               int workshopParts,int minersAtWork,int factoryWorkersAtWork,
                               int oreOnTruck,int scrapRemaining,boolean westDepositDiscovered,
                               int quarryPayroll,int factoryPayroll,int cityBudget) { }
    public record RetailView(String id,String label,int price,int stock) { }
    public synchronized List<RetailView> retailCatalog(boolean lowStock){
        if(!SoftwareLife.deployed(town,lowStock?"retail-sql":"retail-api"))
            throw new IllegalArgumentException("This retail service has not been deployed yet.");
        if(DevOpsLife.unavailable(town))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Retail service temporarily unavailable.");
        return RetailLife.PRODUCTS.stream()
                .map(p->new RetailView(p.id(),p.label(),p.price(),town.retailStock.getOrDefault(p.id(),0)))
                .filter(p->!lowStock||p.stock()<3)
                .sorted(java.util.Comparator.comparing(RetailView::id)).toList();
    }
    private record WalkingPair(TownLife.Citizen a,TownLife.Citizen b,int score) { }
    static java.util.Map<String,String> walkingCompanions(TownLife.Town town,List<TownLife.Citizen> visible,int minute){
        if(minute<480||minute>=1200)return java.util.Map.of();
        var candidates=visible.stream().filter(c->leisureEncounter(c)&&c.age>=3&&!RebirthLife.linkedAdult(town,c)
                &&java.util.Set.of(TownLife.Place.PARK,TownLife.Place.CENTRO,TownLife.Place.PORTICI,TownLife.Place.RIVER).contains(c.place)
                &&c.currentSpace().equals(TownLife.publicSpace(c.place))&&c.hunger<70&&c.thirst<70&&c.fatigue<80)
                .sorted(java.util.Comparator.comparing(c->c.id)).toList();
        var pairs=new ArrayList<WalkingPair>();
        for(int i=0;i<candidates.size();i++)for(int j=i+1;j<candidates.size();j++){
            var a=candidates.get(i);var b=candidates.get(j);
            if(a.place!=b.place||!a.currentSpace().equals(b.currentSpace()))continue;
            boolean family=a.generationIndex>0&&b.generationIndex>0&&(
                (a.age<18&&b.age>=18&&(DemographyLife.get(town,a,"parent-a")==b.generationIndex||DemographyLife.get(town,a,"parent-b")==b.generationIndex))
                ||(b.age<18&&a.age>=18&&(DemographyLife.get(town,b,"parent-a")==a.generationIndex||DemographyLife.get(town,b,"parent-b")==a.generationIndex)));
            boolean couple=a.age>=18&&b.age>=18&&a.generationIndex>0&&b.generationIndex>0
                &&DemographyLife.get(town,a,"partner")==b.generationIndex&&DemographyLife.get(town,b,"partner")==a.generationIndex;
            var r=town.existingRelationship(a.id,b.id);
            if(family)pairs.add(new WalkingPair(a,b,1000));
            else if(couple&&(r==null||r.tension<=35))pairs.add(new WalkingPair(a,b,500));
            else if(a.age>=18&&b.age>=18&&r!=null&&r.conversations>=2&&r.trust>=58&&r.affinity>=65&&r.tension<=25)
                pairs.add(new WalkingPair(a,b,r.affinity+r.trust-r.tension));
        }
        pairs.sort(java.util.Comparator.comparingInt(WalkingPair::score).reversed().thenComparing(p->p.a.id).thenComparing(p->p.b.id));
        var result=new java.util.LinkedHashMap<String,String>();
        for(var p:pairs){
            if(result.containsKey(p.a.id)||result.containsKey(p.b.id))continue;
            result.put(p.a.id,p.b.id);result.put(p.b.id,p.a.id);
        }
        return java.util.Map.copyOf(result);
    }
    public record PeerEncounterView(String firstId,String secondId,long tick,String tone,String reaction) { }
    private void prunePeerEvents(long now){
        recentPeerEvents.removeIf(e->now-((long)(e.day()-1)*1440L+e.minute())>15);
    }
    static String peerReaction(SocialLife.Kind kind,String tone){
        if(kind==SocialLife.Kind.TENSE_EXCHANGE)return "ARGUMENT";
        if(kind==SocialLife.Kind.RECONCILIATION)return "RECONCILE";
        if(kind==SocialLife.Kind.FRIENDLY_CHAT&&"WARM".equals(tone))return "LAUGH";
        return "CHAT";
    }
    static boolean leisureEncounter(TownLife.Citizen c){
        return c.alive&&c.travelRemaining==0&&java.util.Set.of("WALKING","WANDERING","STROLLING","FREE_TIME","RELAXING","RESTING").contains(c.activity);
    }
    // Only render recent, real simulation interactions between currently visible peers.
    static List<PeerEncounterView> peerEncounters(TownLife.Town town,List<TownLife.Citizen> visible,long now){
        return peerEncounters(town,visible,now,List.of());
    }
    static List<PeerEncounterView> peerEncounters(TownLife.Town town,List<TownLife.Citizen> visible,long now,List<SocialLife.Interaction> events){
        var result=new java.util.ArrayList<PeerEncounterView>();
        var used=new java.util.HashSet<String>();
        var eligible=visible.stream().filter(GameSession::leisureEncounter).sorted(java.util.Comparator.comparing(c->c.id)).toList();
        for(int i=0;i<eligible.size();i++)for(int j=i+1;j<eligible.size();j++){
            var a=eligible.get(i);var b=eligible.get(j);
            if(used.contains(a.id)||used.contains(b.id)||!a.currentSpace().equals(b.currentSpace()))continue;
            var r=town.existingRelationship(a.id,b.id);
            if(r==null||r.conversations==0||r.lastInteractionTick<0||r.lastInteractionTick>now||now-r.lastInteractionTick>15)continue;
            String tone=r.tension>=58?"TENSE":r.affinity>=65&&r.trust>=58?"WARM":"CASUAL";
            SocialLife.Kind kind=null;
            for(var event:events){
                long tick=(long)(event.day()-1)*1440L+event.minute();
                if(tick==r.lastInteractionTick&&event.place()==a.place&&SocialNetwork.key(event.firstId(),event.secondId()).equals(SocialNetwork.key(a.id,b.id))){
                    kind=event.kind();break;
                }
            }
            result.add(new PeerEncounterView(a.id,b.id,r.lastInteractionTick,tone,peerReaction(kind,tone)));
            used.add(a.id);used.add(b.id);
        }
        return List.copyOf(result);
    }
    public record BarberService(String id,String label,int price,int minutes) { }
    public record BarberView(String hair,String beard,List<BarberService> services,boolean open) { }
    static final List<BarberService> BARBER_SERVICES=List.of(
            new BarberService("short","Short haircut",8,20),
            new BarberService("classic","Classic haircut",8,20),
            new BarberService("shave","Clean shave",6,15),
            new BarberService("trim","Hair and beard trim",12,30),
            new BarberService("style","New hairstyle",15,35));
    static String barberKey(TownLife.Town town,String field){return "barber:"+town.player.incarnation+":"+field;}
    static BarberView barberView(TownLife.Town town,int minute){
        int hair=town.industryStocks.getOrDefault(barberKey(town,"hair"),0);
        int beard=town.industryStocks.getOrDefault(barberKey(town,"beard"),0);
        return new BarberView(switch(hair){case 1 -> "SHORT";case 2 -> "CLASSIC";case 3 -> "STYLED";default -> "NATURAL";},
                switch(beard){case 1 -> "SHAVED";case 2 -> "TRIMMED";default -> "NATURAL";},BARBER_SERVICES,
                !RebirthLife.active(town)&&town.player.alive&&town.player.place==TownLife.Place.CENTRO&&town.player.travelRemaining==0&&minute>=540&&minute<1140);
    }
    static BarberService barberVisit(TownLife.Town town,String id,int minute){
        var service=BARBER_SERVICES.stream().filter(s->s.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown barber service."));
        if(!barberView(town,minute).open()||minute+service.minutes()>1140)throw new IllegalArgumentException("Visit the barber in Centro between 09:00 and 19:00.");
        var p=town.player;
        if(p.money<service.price())throw new IllegalArgumentException("Not enough money.");
        p.money-=service.price();
        town.employerFunds.merge("barber",service.price(),Math::addExact);
        switch(id){
            case "short" -> town.industryStocks.put(barberKey(town,"hair"),1);
            case "classic" -> town.industryStocks.put(barberKey(town,"hair"),2);
            case "shave" -> town.industryStocks.put(barberKey(town,"beard"),1);
            case "trim" -> {town.industryStocks.put(barberKey(town,"hair"),2);town.industryStocks.put(barberKey(town,"beard"),2);}
            case "style" -> town.industryStocks.put(barberKey(town,"hair"),3);
        }
        return service;
    }
    public record StairwellResident(String id,String name,String appearanceStage,String outfitVisual,String knownName,int conversations,String relationship,String mood) { }
    public record WorldView(int day,int minute,int year,int dayOfYear,String season,
                            WeatherView weather,PlayerView player,List<ResidentView> residents,
                            List<SourceView> sources,List<MutualAidLife.Notice> aidNotices,ServicesView services,List<RetailView> shop,FarmLife.Report farm,FuelLife.Report fuel,FreightLife.Report freight,LogisticsLife.Report logistics,GardenLife.Report garden,HenLife.Report hens,OrchardLife.Report orchard,MarketStallLife.Report stall,BankLife.Report bank,RentalLife.Report rental,ConstructionLife.Report construction,MunicipalLife.Report comune,DemographyLife.Report demography,EducationLife.Report education,ArchiveLife.Report archive,NewspaperLife.Report newspaper,OpinionLife.Report opinion,ElectionLife.Report election,RebirthLife.Report rebirth,PhoneLife.Report phone,List<AddressLife.Entry> contacts,IntercomLife.ApartmentVisit apartmentVisit,List<PeerEncounterView> peerEncounters,BarberView barber,PublicSafetyLife.View publicSafety,BusLife.View bus,List<StairwellResident> stairwellResidents,TechnicalWorkLife.MagnoliaServices magnoliaServices,CondominiumLife.Report condominium,PostalLife.Report postal,CivilRegistryLife.Report registry,EmploymentContractLife.Report employmentContract,PensionLife.Report pension,SocialProtectionLife.Report socialProtection,CaritasLife.Report caritas,ShelterLife.Report shelter,EmploymentCenterLife.Report jobCentre,List<InterviewLife.Application> jobApplications,LocalVehicleCrimeLife.View vehicleCrime,NpcVehicleCrimeLife.View residentVehicleCrime,NpcPropertyCrimeLife.View residentPropertyCrime,SecurityLife.View security,List<IntercomLife.Access> apartmentAccess,PersonalGoodsLife.View personalGoods,HomeGoodsLife.View homeGoods,HomeApplianceLife.View homeAppliances,HomeServiceLife.View homeServices,PlayerServiceBusiness.View repairBusiness,PlayerRepairLife.View personalRepair,VocationalLife.View vocational,ApprenticeshipLife.View apprenticeship,IndustrialRepairLife.View industrialRepair,EnergyLife.View energy,WaterNetworkLife.View waterNetwork,GasNetworkLife.View gasNetwork,WasteLife.View waste,AgriFoodLife.View agriFood,StaffingLife.View staffing,FactoryNetworkLife.View factories,List<SpontaneousSpeechLife.Offer> speechOffers,OfflineRoutineLife.View offlineReport) { }
    public record ActionView(String message,WorldView world,String dialogueLanguage,boolean dialogueRendered,String dialogueLocale,String dialogueFarewell) {
        public ActionView(String message,WorldView world){this(message,world,null,false,null,null);}
        public ActionView(String message,WorldView world,String language,boolean rendered,String locale){this(message,world,language,rendered,locale,null);}
        public ActionView(String message,WorldView world,String language,boolean rendered){this(message,world,language,rendered,language==null?null:WorldSpeechRegion.CURRENT.localeFor(language));}
    }

    public synchronized WorldView snapshot() {
        var p=town.player;
        var linked=RebirthLife.child(town);
        boolean controlled=RebirthLife.linkedAdult(town,linked);
        var player=new PlayerView(p.incarnation,controlled?linked.locationText():PlayerLife.position(p),p.place.name(),controlled?linked.name:p.privateName,
                p.hunger,p.thirst,p.fatigue,p.health,controlled||p.alive,
                controlled?linked.money:p.money,controlled&&linked.employed?linked.occupation:p.jobId,p.engineeringSkill,p.investigationSkill,p.bread,p.bottledWater,p.roomUntil,
                HousingLife.active(p,state.day,state.minute),p.storedBread,p.storedWater,controlled?RebirthLife.hygiene(town,linked):p.hygiene,
                FoodLife.inventory(p,false),FoodLife.inventory(p,true),p.ownsBicycle,p.cycling,p.bicycleParked==null?null:p.bicycleParked.name(),
                p.carPlace==null?null:p.carPlace.name(),p.carFuel,p.carCondition,
                p.carPlace==p.place?MotorLife.neighbors(p.place).stream().map(Enum::name).toList():List.of(),p.ownsHouse,CombatLife.ownsBaton(town),CombatLife.reports(town),JusticeLife.view(town,state),WeaponLicenseLife.view(town,state),PassportLife.view(town,state));
        var visible=RebirthLife.active(town)?RebirthLife.visible(town):PlayerLife.visible(town);
        var companions=walkingCompanions(town,visible,state.minute);
        var residents=visible.stream()
                .map(c->{
                    var contact=p.contacts.get(c.id);
                    return new ResidentView(c.id,PlayerLife.displayName(town,c),c.activity,
                            FamilyDialogueLife.family(town,c)?linked.name:contact==null?null:contact.knownName,
                            contact==null?0:contact.conversations,FamilyDialogueLife.family(town,c)?"FAMILY":PlayerLife.relationship(contact),EmotionLife.mood(town,c,state.day,state.minute).name(),PersonalLoanLife.outstanding(town,c.id),PersonalLoanLife.dueDay(town,c.id),workVisual(c),outfitVisual(c),appearanceStage(c),companions.getOrDefault(c.id,""),ConversationAvailability.minutes(town,c,state.day,state.minute),ConversationAvailability.reason(c,state.day,state.minute),factoryStation(c));
                }).toList();
        var resources=NatureLife.available(town).values().stream()
                .map(s->new SourceView(s.id,s.label,s.kind.name(),s.stock,s.capacity)).toList();
        var weather=WeatherLife.at(state.day,state.minute);
        return new WorldView(state.day,state.minute,CalendarLife.year(state.day),
                CalendarLife.dayOfYear(state.day),weather.season().name(),
                new WeatherView(weather.sky().name(),weather.temperatureC(),
                        weather.raining(),weather.snowing()),
                player,residents,resources,MutualAidLife.notices(town,state.day),services(),RetailLife.hasCounter(p.place)
                        ? RetailLife.PRODUCTS.stream().filter(item->RetailLife.availableAt(p.place,item)).map(item->new RetailView(item.id(),item.label(),item.price(),
                            town.retailStock.getOrDefault(item.id(),0))).toList() : List.of(),FarmLife.report(town),FuelLife.report(town),FreightLife.report(town),LogisticsLife.report(town,state.day),GardenLife.report(town,state.day,state.minute),HenLife.report(town,state.day),OrchardLife.report(town,state.day,state.minute),MarketStallLife.report(town,state.day,state.minute),BankLife.report(town),RentalLife.report(town),ConstructionLife.report(town),MunicipalLife.report(town),DemographyLife.report(town),EducationLife.report(town),p.alive&&p.travelRemaining==0&&p.place==TownLife.Place.LIBRARY?ArchiveLife.report(town,state.day,archiveYear,archivePage):null,p.alive&&p.travelRemaining==0&&p.place==TownLife.Place.LIBRARY?NewspaperLife.report(town,state.day):null,OpinionLife.report(town),ElectionLife.report(town,state.day),RebirthLife.report(town,state.day,state.minute),PhoneLife.report(town,state.day,state.minute),AddressLife.contacts(town),IntercomLife.apartment(town,state.day,state.minute),peerEncounters(town,visible,(long)(state.day-1)*1440L+state.minute,recentPeerEvents),barberView(town,state.minute),PublicSafetyLife.view(town,state),BusLife.view(town,state),
                ResidentialArrivalLife.visible(town,state,1).stream()
                        .map(c->{var contact=p.contacts.get(c.id);
                            return new StairwellResident(c.id,PlayerLife.displayName(town,c),appearanceStage(c),outfitVisual(c),
                                    contact==null?null:contact.knownName,contact==null?0:contact.conversations,
                                    PlayerLife.relationship(contact),EmotionLife.mood(town,c,state.day,state.minute).name());
                        }).toList(),TechnicalWorkLife.magnolia(town),CondominiumLife.report(town),PostalLife.report(town),CivilRegistryLife.report(town,state),EmploymentContractLife.report(town,state.day),PensionLife.report(town),SocialProtectionLife.report(town,state.day),CaritasLife.report(town,state),ShelterLife.report(town,state),EmploymentCenterLife.report(town,state),InterviewLife.report(town,state),LocalVehicleCrimeLife.view(town,state),NpcVehicleCrimeLife.view(town),NpcPropertyCrimeLife.view(town),SecurityLife.view(town,state),IntercomLife.access(town,state.day,state.minute),PersonalGoodsLife.view(town,state),HomeGoodsLife.view(town,state),HomeApplianceLife.view(town,state),HomeServiceLife.view(town,state),PlayerServiceBusiness.view(town,state),PlayerRepairLife.view(town,state),VocationalLife.view(town,state),ApprenticeshipLife.view(town,state),IndustrialRepairLife.view(town,state),EnergyLife.view(town),WaterNetworkLife.view(town),GasNetworkLife.view(town),WasteLife.view(town),AgriFoodLife.view(town),StaffingLife.view(town),FactoryNetworkLife.view(town),SpontaneousSpeechLife.view(town,state.day,state.minute),offlineReport);
    }

    public synchronized BusinessView business() {
        var b=BusinessLife.status(town,state.day);
        return new BusinessView(b.registered(),b.yours(),b.ownerIncarnation(),
                b.machine(),b.machineCondition(),b.cash(),b.employeeId(),b.employeeName(),
                b.spareParts(),b.paidHoursToday(),b.lastProductionDay(),b.lastSaleDay());
    }

    public synchronized List<CandidateView> businessCandidates() {
        // Only names of actual nearby, available town residents.
        return BusinessLife.candidates(town,state.day).stream()
                .map(c->new CandidateView(c.id,c.name,c.occupation)).toList();
    }

    public synchronized ActionView openBusiness() throws SQLException {
        var copy=town.copy();
        String message=BusinessLife.open(copy);
        return apply(copy,15,message);
    }

    public synchronized ActionView investBusiness(int amount) throws SQLException {
        var copy=town.copy();
        String message=BusinessLife.invest(copy,amount);
        return apply(copy,5,message);
    }

    public synchronized ActionView buyBusinessMachine() throws SQLException {
        var copy=town.copy();
        String message=BusinessLife.machine(copy);
        return apply(copy,30,message);
    }

    public synchronized ActionView serviceBusinessMachine() throws SQLException {
        var copy=town.copy();
        String message=BusinessLife.service(copy);
        return apply(copy,BusinessLife.SERVICE_MINUTES,message);
    }

    public synchronized ActionView hireBusinessWorker(String residentId)
            throws SQLException {
        var copy=town.copy();
        String message=BusinessLife.hire(copy,residentId,state.day);
        return apply(copy,15,message);
    }

    public synchronized ActionView withdrawBusiness(int amount) throws SQLException {
        var copy=town.copy();
        String message=BusinessLife.withdraw(copy,amount);
        return apply(copy,5,message);
    }

    /** Authoritative condition of water and lighting infrastructure for the UI. */
    public synchronized ServicesView services() {
        var u=UtilityLife.status(town);
        return new ServicesView(u.pumpCondition(),u.fountainWorking(),u.cleanWater(),
                u.cleanWaterCapacity(),u.lightingCondition(),u.lightsWorking());
    }

    /** Public inventories of the local mining and manufacturing enterprises. */
    public synchronized IndustryView industry() {
        var r=IndustryLife.report(town);
        return new IndustryView(r.ironRemaining(),r.copperRemaining(),r.oreAtQuarry(),
                r.oreAtFactory(),r.factorySteel(),r.factoryCopper(),r.factoryParts(),
                r.workshopSteel(),r.workshopCopper(),r.workshopParts(),
                r.minersAtWork(),r.factoryWorkersAtWork(),r.oreOnTruck(),
                r.scrapRemaining(),r.westDepositDiscovered(),r.quarryPayroll(),
                r.factoryPayroll(),r.cityBudget());
    }

    /** Travel is visible to the client, but distance and time remain Java-owned. */
    public synchronized List<DestinationView> destinations() {
        if(RebirthLife.active(town)){
            var c=RebirthLife.child(town);
            if(c==null||!c.alive||c.travelRemaining>0)return List.of();
            return PlayerLife.publicPlaces().stream().map(place->new DestinationView(place.name(),place.label,
                    TownLife.travelTime(c.place,place))).toList();
        }
        return PlayerLife.publicPlaces().stream()
                .map(place->new DestinationView(place.name(),place.label,
                        PlayerLife.travelMinutes(town.player,place))).toList();
    }

    public synchronized List<WorkOrderView> workOrders() {
        return TechnicalWorkLife.orders(town).stream()
                .map(o->new WorkOrderView(o.id,o.title,o.jobId,o.place.name(),
                        o.phase.name(),o.condition,o.completedDay,
                        o.materialA,o.requiredA,o.materialB,o.requiredB)).toList();
    }

    public synchronized ActionView diagnose(String id) throws SQLException {
        var candidate=town.copy();
        String message=TechnicalWorkLife.inspect(candidate,id,state.minute);
        return apply(candidate,TechnicalWorkLife.INSPECTION_MINUTES,message);
    }

    public synchronized ActionView repair(String id) throws SQLException {
        var candidate=town.copy();
        String message=TechnicalWorkLife.complete(candidate,id,state.day,state.minute);
        return apply(candidate,TechnicalWorkLife.COMPLETION_MINUTES,message);
    }

    public synchronized List<JobView> jobs() {
        return CareerLife.offers().stream()
                .map(v->new JobView(v.id(),v.title(),v.workplace().name(),v.subject(),
                        v.requiredSkill(),v.wage(),
                        town.employerFunds.getOrDefault(CareerLife.payrollAccount(v.id()),0))).toList();
    }

    public synchronized ActionView applyJob(String jobId) throws SQLException {
        var candidate=town.copy();
        String message=CareerLife.apply(candidate,jobId,state.day);
        return apply(candidate,15,message);
    }

    public synchronized ActionView study(String subject) throws SQLException {
        var candidate=town.copy();
        String message=EmploymentCenterLife.study(candidate,state,subject);
        return apply(candidate,CareerLife.LESSON_MINUTES,message);
    }

    public synchronized ActionView work() throws SQLException {
        var candidate=town.copy();
        String message=CareerLife.work(candidate,state.day,state.minute);
        return apply(candidate,CareerLife.SHIFT_MINUTES,message);
    }

    public synchronized ActionView prospect() throws SQLException {
        var copy=town.copy();
        String message=IndustryLife.prospectPlayer(copy,state.minute);
        return apply(copy,120,message);
    }

    /** Reuses the existing per-visitor, per-resident memory and atomic save. */
    public synchronized ActionView speak(String residentId,String message) throws SQLException {
        return speak(residentId,message,null);
    }
    /** A declared spoken language is separate from the UI locale. Null preserves legacy clients. */
    public synchronized ActionView speak(String residentId,String message,String spokenLanguage) throws SQLException {
        return speak(residentId,message,spokenLanguage,false);
    }
    public synchronized ActionView speak(String residentId,String message,String spokenLanguage,boolean stairwell) throws SQLException {
        JusticeLife.requireFree(town);
        if(residentId==null || message==null)
            throw new IllegalArgumentException("Resident and message are required.");
        var copy=town.copy();
        var resident=stairwell?ResidentialArrivalLife.nearby(copy,residentId,state.day,state.minute):PlayerLife.nearby(copy,residentId,state.day,state.minute);
        var region=WorldSpeechRegion.CURRENT;
        String language=spokenLanguage==null?null:ResidentLanguages.normalize(spokenLanguage);
        if(message.isBlank()||message.strip().length()>1000)
            throw new IllegalArgumentException("Speak 1–1000 characters.");
        if(LostChildAidLife.request(copy,message)){
            // Emergencies bypass ordinary conversation patience. This also
            // changes the saved missing-child search instead of just chatting.
            String replyLanguage=language==null?LostChildAidLife.inferredLanguage(message)
                    :!ResidentLanguages.speaks(region,resident.id,language)?region.nativeLanguage():language;
            var contact=copy.player.contacts.computeIfAbsent(resident.id,PlayerLife.Contact::new);
            var previous=contact.copy();
            String assistance=LostChildAidLife.respond(copy,resident,state,replyLanguage);
            contact.conversations++;
            contact.lastDay=state.day;contact.lastMinute=state.minute;contact.lastHeard=message.strip();
            ConversationMemory.remember(contact,state.day,state.minute,replyLanguage,message.strip(),assistance);
            ConversationMemory.captureMilestones(town,copy,previous,contact,contact.dialogue.getLast());
            var applied=applyInternal(copy,1,PlayerLife.displayName(copy,resident)+": "+assistance,true);
            return new ActionView(applied.message(),applied.world(),replyLanguage,true,
                    region.localeFor(replyLanguage),null);
        }
        // Medical customer service is part of the shift, not a leisure chat.
        String serviceLanguage=language==null?"en":language;
        var service=language==null||ResidentLanguages.speaks(region,resident.id,language)
                ?MedicalServiceDialogue.reply(copy,resident,message,state.day,state.minute,serviceLanguage):null;
        if(service!=null){
            var contact=copy.player.contacts.computeIfAbsent(resident.id,PlayerLife.Contact::new);
            var previous=contact.copy();
            String answer=service.text();
            int duration=1;
            var person=RebirthLife.child(copy);
            if(RebirthLife.linkedAdult(copy,person)&&!RebirthLife.atHome(person))
                RebirthLife.holdPublicPlace(copy,person);
            if(service.action()!=null){
                var performed=RebirthLife.medicalAction(copy,state.day,state.minute,service.action());
                duration=performed.minutes();
                answer="it".equals(serviceLanguage)?switch(service.action()){
                    case "collect"->"Ecco il suo kit di recupero. Pagamento: 9 euro.";
                    case "first-aid"->"Assistenza di base completata. Pagamento: 5 euro.";
                    default->"Visita completata. Pagamento: 15 euro.";
                }:performed.message();
            }
            contact.conversations++;
            contact.lastDay=state.day;contact.lastMinute=state.minute;contact.lastHeard=message.strip();
            ConversationMemory.remember(contact,state.day,state.minute,serviceLanguage,message.strip(),answer);
            ConversationMemory.captureMilestones(town,copy,previous,contact,contact.dialogue.getLast());
            var applied=applyInternal(copy,duration,PlayerLife.displayName(copy,resident)+": "+answer,RebirthLife.active(town));
            return new ActionView(applied.message(),applied.world(),serviceLanguage,true,region.localeFor(serviceLanguage),null);
        }
        if(!ConversationAvailability.allowed(copy,resident,state.day,state.minute)){
            String actual=language!=null&&!ResidentLanguages.speaks(region,resident.id,language)?region.nativeLanguage():language;
            String farewell=ConversationAvailability.farewell(resident,state.day,state.minute,actual);
            var refused=applyInternal(copy,1,PlayerLife.displayName(copy,resident)+": "+farewell,RebirthLife.active(town));
            return new ActionView(refused.message(),refused.world(),actual,true,actual==null?null:region.localeFor(actual),farewell);
        }
        String reply=null;
        boolean rendered=false;
        boolean understood=true;
        if(FamilyDialogueLife.family(copy,resident))copy.player.contacts.computeIfAbsent(resident.id,PlayerLife.Contact::new).knownName=RebirthLife.child(copy).name;
        var existingContact=copy.player.contacts.get(resident.id);
        var prior=existingContact==null?null:existingContact.copy();
        if(language!=null){
            if(message.isBlank()||message.strip().length()>1000)
                throw new IllegalArgumentException("Speak 1–1000 characters.");
            if("SLEEPING".equals(resident.activity)||"TRAVELING".equals(resident.activity))
                throw new IllegalArgumentException("This person is not available for a conversation.");
            if(!ResidentLanguages.speaks(region,resident.id,language)){
                understood=false;
                reply=ResidentLanguages.misunderstanding(region,language);
                language=region.nativeLanguage();
                rendered=true;
            } else {
                reply=ResidentLanguages.languageQuestion(region,message,resident.id,language);
                rendered=reply!=null;
            }
        }
        if(reply==null&&understood){
            reply=FamilyDialogueLife.reply(copy,resident,message,language);
            if(reply!=null){
                rendered=language!=null;
                var self=RebirthLife.child(copy);
                if(language!=null&&self!=null&&self.age>=3&&LocalDialogue.conversational(message,prior==null?null:prior.lastHeard,reply)){
                    String generated=LocalDialogue.grounded(copy,resident,prior,prior==null?null:prior.lastHeard,message,region,language,state);
                    if(generated!=null){reply=generated;rendered=true;}
                }
            }
        }
        if(reply==null&&understood){
            reply=StreetAidLife.dialogue(copy,resident,message,state.day,state.minute);
        }
        if(reply==null&&understood&&java.util.Set.of("can i meet your family","introduce me to your family","mi presenti la tua famiglia","posso conoscere la tua famiglia","mi fai conoscere la tua famiglia").contains(NaturalDialogue.normalize(message))){
            reply=IntercomLife.introduceFamily(copy,resident.id,state.day,state.minute,language);
            rendered=language!=null;
        }
        if(reply==null&&understood&&java.util.Set.of("can i give you my number","can i give you my phone number","ti posso dare il mio numero","posso darti il mio numero","share phone number").contains(NaturalDialogue.normalize(message))){
            boolean shared=PhoneLife.shareNumber(copy,resident);
            reply="it".equals(language)?(shared?"Certo, ti chiamerò ogni tanto.":"Preferisco aspettare di conoscerti meglio."):(shared?"Sure, I'll call you sometime.":"I'd rather get to know you better first.");
            rendered=language!=null;
        }
        if(reply==null){
            String previous=prior==null?null:prior.lastHeard;
            boolean recall=ConversationMemory.recallQuestion(message);
            reply=message.strip().equalsIgnoreCase("Any public news for the newspaper?")
                    ?NewspaperLife.interview(copy,resident,state.day,state.minute)
                    :PlayerLife.speak(copy,residentId,message,state.day,state.minute,stairwell);
            if(LifetimeMemoryLife.question(message)&&LifetimeMemoryLife.recollect(copy,resident,language)!=null){
                reply=LifetimeMemoryLife.recollect(copy,resident,language);
                rendered=language!=null;
            }else if(ConversationMemory.importantQuestion(message)){
                reply=ConversationMemory.firstMeeting(prior,language);
                rendered=language!=null;
            } else if(recall){
                reply=ConversationMemory.recall(prior,language);
                rendered=language!=null;
            } else if(language!=null&&LocalDialogue.conversational(message,previous,reply)){
                String generated=LocalDialogue.grounded(copy,resident,copy.player.contacts.get(resident.id),previous,message,region,language,state);
                if(generated!=null){reply=generated;rendered=true;}
            }
        }
        if(understood){
            var contact=copy.player.contacts.computeIfAbsent(resident.id,PlayerLife.Contact::new);
            if(FamilyDialogueLife.family(copy,resident))contact.knownName=RebirthLife.child(copy).name;
            contact.lastDay=state.day;contact.lastMinute=state.minute;
            ConversationMemory.remember(contact,state.day,state.minute,language,message.strip(),reply);
            ConversationMemory.captureMilestones(town,copy,prior,contact,contact.dialogue.getLast());
        }
        var controlled=RebirthLife.child(copy);
        if(RebirthLife.linkedAdult(copy,controlled)&&!RebirthLife.atHome(controlled)
                &&PlayerLife.publicPlaces().contains(controlled.place)) {
            // The person explicitly chose to stop and talk. Keep their actual
            // public location under player control during the one-minute tick,
            // instead of an NPC work/home schedule teleporting them away.
            // "Resume work and daily routine" restores autonomous scheduling.
            RebirthLife.holdPublicPlace(copy,controlled);
        }
        boolean ending=ConversationAvailability.record(copy,resident,state.day,state.minute);
        String farewell=ending?ConversationAvailability.farewell(resident,state.day,state.minute,language):null;
        // A willing resident may finish this reply before resuming the
        // schedule, including a worker. Never reserve an urgent resident or
        // block a missing-child search. The hold expires after this tick.
        if(!stairwell&&!ending&&!ConversationAvailability.urgent(resident)
                &&!"SEARCHING_FOR_CHILD".equals(resident.activity))
            copy.industryStocks.put(TownLife.conversationHoldKey(resident.id),1);
        // Conversation is valid in both a visitor life and a born/linked
        // life. The latter must use the rebirth-aware time/commit path.
        ActionView applied=applyInternal(copy,1,PlayerLife.displayName(copy,resident)+": "+reply,
                RebirthLife.active(town));
        return new ActionView(applied.message(),applied.world(),language,rendered,language==null?null:region.localeFor(language),farewell);
    }

    public synchronized ActionView waitMinutes(int minutes) throws SQLException {
        if(persistentWorld)throw new IllegalArgumentException("The server clock runs at 1:24. Waiting cannot advance the entire world.");
        if(minutes<1 || minutes>1440)
            throw new IllegalArgumentException("Wait between 1 and 1440 game minutes.");
        if(JusticeLife.confined(town)){
            int duration=JusticeLife.waitDuration(town,state,minutes);
            return applyInternal(town.copy(),duration,"Time passed in custody.",false,state.copy(),new ArrayList<>(),new ArrayList<>(),true);
        }
        if(RebirthLife.linkedAdult(town,RebirthLife.child(town)))
            return applyInternal(town.copy(),minutes,"Waited "+minutes+" minute(s).",true);
        return apply(town.copy(),minutes,"Waited "+minutes+" minute(s).");
    }

    private GeoDrive geoDrive;
    private TownLife.Place geoInterior;
    private GeoResponse geoResponse;
    private long geoLastImpact;
    private void updateGeoResponse(GeoProjection map,GeoProjection.Point target)throws SQLException{
        if(!"REPORTED".equals(JusticeLife.view(town,state).status())||RebirthLife.active(town)||!town.player.alive){geoResponse=null;return;}
        if(geoResponse!=null){var officer=town.people.get(geoResponse.officer);if(officer==null||!officer.alive||!PoliceLife.officer(officer)||!PoliceLife.scheduled(officer,state.day,state.minute))geoResponse=null;}
        String goal=map.motorNode(target);if(goal==null)return;
        long now=System.nanoTime();
        if(geoResponse==null){
            var officers=town.people.values().stream().filter(c->c.alive&&PoliceLife.officer(c)&&PoliceLife.scheduled(c,state.day,state.minute))
                .sorted(java.util.Comparator.comparingDouble(c->map.position(c).distance(target))).toList();
            for(var officer:officers){
                String start=map.motorNode(map.position(officer));
                if(start!=null&&(start.equals(goal)||GeoResponse.next(map,start,goal)!=null)){
                    geoResponse=new GeoResponse(officer.id,start,now);break;
                }
            }
            if(geoResponse==null)return;
        }
        double elapsed=Math.min(1.5,Math.max(0,(now-geoResponse.lastUpdate)/1e9));
        geoResponse.lastUpdate=now;
        geoResponse.advance(map,goal,elapsed*9);
        if(geoResponse.position(map).distance(target)<=4){
            var copy=town.copy();var clock=state.copy();var events=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
            JusticeLife.dispatchDetain(copy,clock,events,observations);
            applyInternal(copy,0,"Police detained you.",false,clock,events,observations);
            geoResponse=null;geoDrive=null;
        }
    }
    synchronized GeoDrive.Pose geoDriveControl(GeoProjection map,String command,double x,double z,int turn)throws SQLException{
        if(geoInterior!=null)throw new IllegalArgumentException("Leave the building first.");
        var point=new GeoProjection.Point(x,z);
        if(JusticeLife.confined(town)||!town.player.alive||RebirthLife.active(town)){
            geoDrive=null;throw new IllegalArgumentException("Driving unavailable.");
        }
        if(command.equals("status"))return drivePose(map,"");
        if(command.equals("start")||command.equals("steal")||command.equals("steal-bus")||command.equals("steal-truck")||command.equals("steal-scooter")||command.equals("steal-motorcycle")||command.equals("start-scooter")||command.equals("start-motorcycle")){
            if(geoDrive!=null)throw new IllegalArgumentException("Already driving.");
            var p=town.player;TransportLife.ready(p);
            if(p.ownsBicycle&&p.bicycleParked==null)throw new IllegalArgumentException("Park your bicycle first.");
            if(IntercomLife.insideVisit(town,state.day,state.minute))throw new IllegalArgumentException("Leave the apartment first.");
            boolean bus=command.equals("steal-bus");
            boolean truck=command.equals("steal-truck");
            String lightKind=command.endsWith("-scooter")?"scooter":command.endsWith("-motorcycle")?"motorcycle":"";
            boolean stolen=command.startsWith("steal");
            if(!stolen)TowServiceLife.requireAvailable(town,lightKind.isEmpty()?"car":lightKind);
            var civilian=map.civilian(town,point);
            if(!lightKind.isEmpty()){
                if(!stolen)LightMotorLife.present(town,lightKind);
                var parked=!stolen?map.ownedLight(town,point,lightKind):lightKind.equals("scooter")?map.parkedScooter(town,point):map.parkedMotorcycle(town,point);
                if(parked==null||Math.hypot(parked.x()-x,parked.z()-z)>5)throw new IllegalArgumentException("Approach the parked two-wheeler.");
            }else if(truck){
                var parked=map.parkedTruck(town,point);
                if(parked==null||Math.hypot(parked.x()-x,parked.z()-z)>5)throw new IllegalArgumentException("Approach the reserve truck at the workshop.");
            }else if(bus){
                var parked=map.parkedBus(town,point);
                if(parked==null||Math.hypot(parked.x()-x,parked.z()-z)>5)throw new IllegalArgumentException("Approach the municipal reserve bus at the station.");
            }else if(stolen){
                if(civilian==null||!civilian.district().equals(p.place.name())||Math.hypot(civilian.x()-x,civilian.z()-z)>5)
                    throw new IllegalArgumentException("Approach a civilian car in your current district.");
            }else if(!map.carNear(town,point))throw new IllegalArgumentException("Approach your own parked car.");
            String node=map.motorNode(point);
            if(node==null||map.motorNodes.get(node).distance(point)>30)throw new IllegalArgumentException("No motor road nearby.");
            if(stolen){
                var copy=town.copy();var clock=state.copy();var truth=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
                if(!lightKind.isEmpty())VehicleTheftLife.takeLight(copy,clock,lightKind,truth,observations);
                else if(truck)VehicleTheftLife.takeTruck(copy,clock,truth,observations);
                else if(bus)VehicleTheftLife.takeBus(copy,clock,truth,observations);
                else{
                    var owner=copy.people.values().stream().filter(c->c.generationIndex==civilian.ownerIndex()).findFirst().orElseThrow();
                    VehicleTheftLife.take(copy,clock,owner,truth,observations);
                }
                applyInternal(copy,1,!lightKind.isEmpty()?"Two-wheeler reported stolen.":truck?"Freight truck reported stolen.":bus?"Municipal bus reported stolen.":"Civilian car reported stolen.",false,clock,truth,observations);
                if(JusticeLife.confined(town))return drivePose(map,"Police detained you.");
            }else if(!lightKind.isEmpty()){
                if(LightMotorLife.get(town,lightKind,"fuel")<1||LightMotorLife.get(town,lightKind,"condition")<3)throw new IllegalArgumentException("Two-wheeler needs fuel or service.");
            }else if(p.carFuel<1||p.carCondition<3)throw new IllegalArgumentException("Car needs fuel or service.");
            geoDrive=new GeoDrive(stolen,bus,truck,lightKind,node);
            geoDrive.to=map.motorEdges.get(node).get(0);
            geoDrive.meters=(!stolen&&!lightKind.isEmpty()?LightMotorLife.get(town,lightKind,"remainder"):VehicleTheftLife.get(town,stolen?"stolen-remainder":"owned-remainder"))/1000.0;
            return drivePose(map,stolen?"Stolen vehicle reported to police.":DrivingSchoolLife.allowed(town,lightKind)?"In your own vehicle.":"Driving without a license / Guida senza patente.");
        }
        if(geoDrive==null)throw new IllegalArgumentException("You are not driving.");
        if(command.equals("exit")){
            var pointAt=geoDrive.position(map);
            var destination=map.nearest(pointAt);
            var copy=town.copy();copy.player.place=destination;copy.player.destination=destination;
            if(!geoDrive.stolen&&!geoDrive.lightKind.isEmpty())LightMotorLife.park(copy,geoDrive.lightKind,destination);
            else if(!geoDrive.stolen)copy.player.carPlace=destination;
            else VehicleTheftLife.recover(copy); // abandoned civilian car is recovered
            apply(copy,0,"Parked.");
            geoDrive=null;
            var park=map.point(destination);
            return new GeoDrive.Pose(false,false,park.x(),park.z(),0,town.player.carFuel,town.player.carCondition,0,0,"CAR","Parked at "+destination.label+".");
        }
        if(!Set.of("step","coast","brake","reverse","handbrake").contains(command)||turn<-1||turn>1)throw new IllegalArgumentException("Unknown driving control.");
        long now=System.nanoTime();
        if(now<geoDrive.checkUntil){geoDrive.lastStep=now;return drivePose(map,"Police document check / Controllo documenti.");}
        if(now-geoDrive.lastStep<400_000_000L)return drivePose(map,"");
        int fuel=driveResource(geoDrive,"fuel");
        int condition=driveResource(geoDrive,"condition");
        if(fuel<1||condition<3){geoDrive.speed=0;geoDrive.lastStep=now;return drivePose(map,"No fuel or service required. Exit the car.");}
        var next=geoDrive.copy();next.lastStep=now;
        var previousPosition=geoDrive.position(map);
        double budget=next.controlBudget(command,(now-geoDrive.lastStep)/1e9,map.speedLimit(next.from,next.to)/3.6);
        if(budget<0.001){geoDrive=next;return drivePose(map,"");}
        int oldUnits=(int)(next.meters/100);
        boolean reversing=geoDrive.speed<0||next.speed<0;
        double traveled=reversing?next.reverse(map,budget):next.advance(map,turn,budget);
        if(traveled+0.001<budget)next.speed=0;
        else next.speed=Math.min(next.speed,next.vehicleLimit(map.speedLimit(next.from,next.to))/3.6);
        if(next.waitingForSignal&&traveled<0.001){geoDrive=next;return drivePose(map,"Waiting for green light.");}
        int units=(int)(next.meters/100)-oldUnits;
        int impact=0;
        double impactSpeed=0;
        if(traveled>0.01&&now-geoLastImpact>2_000_000_000L){
            var parked=map.civilian(town,previousPosition);
            boolean collision=parked!=null&&GeoResponse.contact(previousPosition,next.position(map),new GeoProjection.Point(parked.x(),parked.z()));
            if(next.stolen&&town.player.carPlace!=null){var own=map.point(town.player.carPlace);collision|=GeoResponse.contact(previousPosition,next.position(map),new GeoProjection.Point(own.x()+2.5,own.z()));}
            if(collision){impactSpeed=Math.max(Math.abs(geoDrive.speed),Math.abs(next.speed))*3.6;impact=RoadAccidentLife.damage(impactSpeed);next.speed=0;geoLastImpact=now;}
        }
        var copy=town.copy();var destination=map.nearest(next.position(map));
        copy.player.place=destination;copy.player.destination=destination;copy.player.cycling=false;
        if(next.stolen){
            VehicleTheftLife.set(copy,"fuel",fuel-units);VehicleTheftLife.set(copy,"condition",condition-3*units-impact);
        }else if(!next.lightKind.isEmpty()){
            LightMotorLife.park(copy,next.lightKind,destination);
            LightMotorLife.set(copy,next.lightKind,"fuel",fuel-units);
            LightMotorLife.set(copy,next.lightKind,"condition",condition-3*units-impact);
        }else{
            copy.player.carPlace=destination;copy.player.carFuel=Math.max(0,fuel-units);copy.player.carCondition=Math.max(0,condition-3*units-impact);
        }
        int remainder=(int)Math.round((next.meters%100)*1000);
        if(!next.stolen&&!next.lightKind.isEmpty())LightMotorLife.set(copy,next.lightKind,"remainder",remainder);
        else VehicleTheftLife.set(copy,next.stolen?"stolen-remainder":"owned-remainder",remainder);
        String accidentMessage="";
        if(impact>0){
            String kind=next.truck?"truck":next.bus?"bus":next.lightKind.isEmpty()?"car":next.lightKind;
            int actualDamage=Math.min(impact,Math.max(0,condition-3*units));
            accidentMessage=RoadAccidentLife.record(copy,state,kind,next.stolen,actualDamage,impactSpeed).message();
        }
        String checkMessage="";
        // Criminal reports keep the existing dispatch/arrest path; ordinary inspections are administrative.
        if(impact==0&&!next.stolen&&traveled>0.001&&RoadPoliceLife.due(copy,state)
                &&map.patrols(town,state,next.position(map)).stream().anyMatch(patrol->!patrol.responding()&&new GeoProjection.Point(patrol.x(),patrol.z()).distance(next.position(map))<=12)){
            String kind=next.lightKind.isEmpty()?"car":next.lightKind;
            String plate=VehicleRegistrationLife.personal(copy,kind);
            var check=RoadPoliceLife.check(copy,state,kind,next.lightKind.isEmpty()?copy.player.carCondition:LightMotorLife.get(copy,kind,"condition"),plate);
            if(check.checked()){
                next.speed=0;next.checkUntil=now+4_000_000_000L;
                checkMessage=check.message();
            }
        }
        apply(copy,0,!accidentMessage.isEmpty()?accidentMessage:checkMessage.isEmpty()?"Driving.":checkMessage);geoDrive=next;
        return drivePose(map,!checkMessage.isEmpty()?checkMessage:impact>0?accidentMessage:next.waitingForSignal?"Waiting for green light.":traveled<0.001?"End of this directed road. Exit the vehicle.":"");
    }
    private int driveResource(GeoDrive drive,String field){
        if(drive.stolen)return VehicleTheftLife.get(town,field);
        if(!drive.lightKind.isEmpty())return LightMotorLife.get(town,drive.lightKind,field);
        return field.equals("fuel")?town.player.carFuel:town.player.carCondition;
    }
    private GeoDrive.Pose drivePose(GeoProjection map,String message){
        if(geoDrive==null)return new GeoDrive.Pose(false,false,0,0,0,town.player.carFuel,town.player.carCondition,0,0,"CAR",message);
        var p=geoDrive.position(map);
        return new GeoDrive.Pose(true,geoDrive.stolen,p.x(),p.z(),geoDrive.heading(map),
            driveResource(geoDrive,"fuel"),driveResource(geoDrive,"condition"),geoDrive.speed*3.6,(int)geoDrive.vehicleLimit(map.speedLimit(geoDrive.from,geoDrive.to)),!geoDrive.lightKind.isEmpty()?geoDrive.lightKind.toUpperCase(java.util.Locale.ROOT):geoDrive.truck?"TRUCK":geoDrive.bus?"BUS":"CAR",message);
    }
    synchronized GeoProjection.View geoNearby(GeoProjection map,double x,double z)throws SQLException{
        var target=geoDrive==null?new GeoProjection.Point(x,z):geoDrive.position(map);
        updateGeoResponse(map,target);
        if(RebirthLife.active(town)){
            var linked=RebirthLife.child(town);
            if(!GeoLife.canWalk(town,state)||linked==null||geoInterior!=linked.place)geoInterior=null;
        }else if(JusticeLife.confined(town)||!town.player.alive||geoInterior!=null&&town.player.place!=geoInterior)geoInterior=null;
        if(geoInterior!=null&&GeoProjection.residential(geoInterior)&&!GeoProjection.ownHome(town,state)&&!IntercomLife.insideVisit(town,state.day,state.minute))geoInterior=null;
        return map.view(town,state,target,geoResponse==null?null:geoResponse.view(map),geoInterior);
    }
    synchronized long persistentMinute(){return (state.day-1L)*1440+state.minute;}
    synchronized void persistentTickTo(long target,boolean justWentOffline)throws SQLException{
        if(closed)return;
        int minutes=(int)Math.max(0,Math.min(15,target-persistentMinute()));
        if(minutes==0&&!justWentOffline)return;
        var copy=town.copy();
        if(justWentOffline)OfflineRoutineLife.prepare(copy);
        boolean confined=JusticeLife.confined(copy);
        if(confined&&minutes>0)minutes=Math.min(minutes,JusticeLife.waitDuration(copy,state,minutes));
        applyInternal(copy,minutes,"Persistent world routine.",RebirthLife.active(copy),state.copy(),new ArrayList<>(),new ArrayList<>(),confined);
    }
    synchronized void geoTick(int minutes) throws SQLException {
        if(persistentWorld)return;
        if(!NpcTransportLife.enabled(town)&&!JusticeLife.confined(town)&&!RebirthLife.active(town)){
            var copy=town.copy();copy.industryStocks.put(NpcTransportLife.ENABLED,1);apply(copy,0,"Resident transport enabled.");
        }
        if(!RebirthLife.active(town))waitMinutes(minutes);
        else if(RebirthLife.linkedAdult(town,RebirthLife.child(town))||RebirthLife.linkedChild(town,RebirthLife.child(town))&&RebirthLife.child(town).age>=6)
            applyInternal(town.copy(),minutes,"Time passed.",true);
        if(JusticeLife.confined(town)||RebirthLife.active(town)||!town.player.alive)geoDrive=null;
    }
    synchronized void geoEnter(GeoProjection map,String destination,double x,double z) throws SQLException {
        JusticeLife.requireFree(town);
        if(geoDrive!=null)throw new IllegalArgumentException("Exit the vehicle first.");
        if(RebirthLife.active(town)){
            var c=RebirthLife.child(town);
            if(!GeoLife.canWalk(town,state)||!RebirthLife.linkedAdult(town,c))throw new IllegalArgumentException("Use F for permitted travel.");
            var door=map.point(c.place);
            if(GeoLife.atHome(c)||geoInterior==c.place){
                if(Math.hypot(x-door.x(),z-(door.z()+5.6))>3)throw new IllegalArgumentException("Approach the exit door.");
                if(GeoLife.atHome(c))gardenAction("rebirth-home-exit");
                geoInterior=null;return;
            }
            var requested=PlayerLife.destination(destination);
            if(requested!=c.place||Math.hypot(x-door.x(),z-door.z())>18)
                throw new IllegalArgumentException("Approach a building in your current district. Use F for travel.");
            if(c.place==c.home){gardenAction("rebirth-home-go");return;}
            if(!GeoProjection.hasInterior(requested)||FireRecoveryLife.closed(town,requested))
                throw new IllegalArgumentException("This building is closed or has no interior.");
            geoInterior=requested;return;
        }
        PlayerLife.requireAlive(town.player);
        if(geoInterior!=null){
            var door=map.point(geoInterior);
            if(Math.hypot(x-door.x(),z-(door.z()+5.6))>3)throw new IllegalArgumentException("Approach the exit door.");
            var visit=IntercomLife.apartment(town,state.day,state.minute);
            if(visit!=null&&"VISITING".equals(visit.status()))gardenAction("intercom:leave:1:"+visit.residentId());
            geoInterior=null;return;
        }
        var place=PlayerLife.destination(destination);
        var view=map.view(town,state,new GeoProjection.Point(x,z));
        if(!view.canEnter()||!view.nearestDistrict().id().equals(place.name()))
            throw new IllegalArgumentException("Approach the district marker.");
        if(IntercomLife.insideVisit(town,state.day,state.minute))
            throw new IllegalArgumentException("Finish the apartment visit.");
        var copy=town.copy();
        // The player has walked there in Godot; avoid charging travel twice.
        copy.player.place=place;copy.player.destination=place;copy.player.travelRemaining=0;
        apply(copy,0,"Entered "+place.label+".");
        if(GeoProjection.hasInterior(place)&&!FireRecoveryLife.closed(town,place))geoInterior=place;
    }
    synchronized String geoSpeak(GeoProjection map,String id,String message,String language,double x,double z) throws SQLException {
        if(geoDrive!=null)throw new IllegalArgumentException("Exit the vehicle first.");
        if(RebirthLife.active(town)&&!RebirthLife.linkedAdult(town,RebirthLife.child(town))&&map.point(town.player.place).distance(new GeoProjection.Point(x,z))>24)throw new IllegalArgumentException("Stay in your current district.");
        var actor=geoNearby(map,x,z).residents().stream().filter(a->a.id().equals(id)&&a.talkable()
                &&Math.hypot(a.x()-x,a.z()-z)<=4).findFirst().orElseThrow(()->new IllegalArgumentException("Approach an available resident in your current district."));
        return speak(actor.id(),message,language).message();
    }

    synchronized GeoProjection.ActionResult geoAction(GeoProjection map,String action,double x,double z)throws SQLException{
        if(geoDrive!=null)throw new IllegalArgumentException("Exit the vehicle first.");
        if("phone-digital-pay".equals(action)||"phone-telecom-subscribe".equals(action)){
            if(map.phone(town,state).actions().stream().noneMatch(a->a.id().equals(action)))throw new IllegalArgumentException("Digital action unavailable.");
            return new GeoProjection.ActionResult(gardenAction(action).message(),null);
        }
        if(RebirthLife.active(town)){
            boolean lifeAction=action!=null&&GeoLife.choices(town,state,map).stream().anyMatch(a->a.id().equals(action));
            boolean workAction=GeoWork.accepts(GeoWork.view(town,state),action);
            if(!lifeAction&&!workAction)throw new IllegalArgumentException("This life action is not available. Check qualification, location, hours and current task.");
            if(workAction&&!GeoWork.campusAction(action)&&!GeoWork.nearComputer(map,town,x,z))
                throw new IllegalArgumentException("Approach an office computer to work.");

            var result=gardenAction(action);
            var linked=RebirthLife.child(town);
            if(linked==null||linked.place!=geoInterior||linked.travelRemaining>0||GeoLife.atHome(linked))geoInterior=null;
            geoDrive=null;
            return new GeoProjection.ActionResult(result.message(),null);
        }
        if("phone-fire-report".equals(action)){
            if(map.phone(town,state).actions().stream().noneMatch(a->a.id().equals(action)))throw new IllegalArgumentException("Fire report unavailable.");
            var copy=town.copy();String message=FireServiceLife.playerReport(copy);apply(copy,1,message);
            return new GeoProjection.ActionResult(message,null);
        }
        if("phone-ambulance".equals(action)){
            if(map.phone(town,state).actions().stream().noneMatch(a->a.id().equals(action))||geoInterior!=null)throw new IllegalArgumentException("Ambulance call unavailable here.");
            var copy=town.copy();String message=AmbulanceLife.call(copy,state);
            apply(copy,1,message);return new GeoProjection.ActionResult(message,null);
        }
        if(action!=null&&action.startsWith("phone-")){
            if(map.phone(town,state).actions().stream().noneMatch(a->a.id().equals(action)))
                throw new IllegalArgumentException("Phone action unavailable.");
            var result=gardenAction(action);
            return new GeoProjection.ActionResult(result.message(),null);
        }
        if(geoInterior!=null&&action!=null&&GeoProjection.outsideAction(action))throw new IllegalArgumentException("Leave the building first.");
        var at=new GeoProjection.Point(x,z);
        if(action==null||map.view(town,state,at,null,geoInterior).actions().stream().noneMatch(a->a.id().equals(action)))
            throw new IllegalArgumentException("This action is not available here.");
        var before=town.player.place;
        if(action.equals("home-enter")){geoInterior=before;return new GeoProjection.ActionResult("Welcome home.",null);}
        if(action.startsWith("intercom:")){
            var result=gardenAction(action);
            if(action.startsWith("intercom:enter:")&&IntercomLife.insideVisit(town,state.day,state.minute))geoInterior=before;
            return new GeoProjection.ActionResult(result.message(),null);
        }
        if(action.startsWith("property:")){
            var result=gardenAction(action.substring("property:".length()));
            return new GeoProjection.ActionResult(result.message(),null);
        }
        if(action.startsWith("stall-")){
            var result=gardenAction(action);
            return new GeoProjection.ActionResult(result.message(),null);
        }
        if(action.startsWith("freight:")){
            String[] command=action.split(":",3);
            var result=freightAction(command[1],command[2]);
            return new GeoProjection.ActionResult(result.message(),before!=town.player.place?map.district(town.player.place):null);
        }
        if(action.startsWith("bank-")){
            var result=gardenAction(action);
            return new GeoProjection.ActionResult(result.message(),null);
        }
        if(action.startsWith("enterprise:")){
            String[] command=action.split(":",3);
            ActionView result=switch(command[1]){
                case "open" -> openBusiness();
                case "invest" -> investBusiness(Integer.parseInt(command[2]));
                case "withdraw" -> withdrawBusiness(Integer.parseInt(command[2]));
                case "machine" -> buyBusinessMachine();
                case "service" -> serviceBusinessMachine();
                case "hire" -> hireBusinessWorker(command[2]);
                default -> throw new IllegalArgumentException("Unknown workshop action.");
            };
            return new GeoProjection.ActionResult(result.message(),null);
        }
        if(action.startsWith("firm:")){
            String[] parts=action.split(":",3);var copy=town.copy();
            String message=switch(parts[1]){
                case "buy" -> BusinessOwnershipLife.buy(copy,state,parts[2]);
                case "invest" -> BusinessOwnershipLife.invest(copy,state,parts[2]);
                case "dividend" -> BusinessOwnershipLife.dividend(copy,state,parts[2]);
                default -> BusinessManagementLife.action(copy,state,parts[2],parts[1]);
            };
            apply(copy,15,message);return new GeoProjection.ActionResult(message,null);
        }
        if(action.equals("home-insurance-buy")||action.equals("home-insurance-collect")){
            var copy=town.copy();String message=action.endsWith("-buy")?HomeInsuranceLife.buy(copy,state):HomeInsuranceLife.collect(copy,state);
            apply(copy,15,message);return new GeoProjection.ActionResult(message,null);
        }
        if(action.equals("fire-recovery-repair")){
            var copy=town.copy();String message=FireRecoveryLife.playerRepair(copy,state,copy.player.place);apply(copy,60,message);
            return new GeoProjection.ActionResult(message,null);
        }
        if(action.equals("fire-duty-gear")){
            var copy=town.copy();String message=FireDutyLife.issueGear(copy,state);apply(copy,5,message);
            return new GeoProjection.ActionResult(message,null);
        }
        if(action.equals("fire-duty-standby")){
            var copy=town.copy();String message=FireDutyLife.standby(copy,state);apply(copy,1,message);
            return new GeoProjection.ActionResult(message,null);
        }
        if(action.equals("fire-duty-join")){
            var copy=town.copy();var journey=FireDutyLife.join(copy,state);
            apply(copy,journey.minutes(),journey.message());geoInterior=null;
            return new GeoProjection.ActionResult(journey.message(),map.district(town.player.place));
        }
        if(action.equals("fire-duty-assist")){
            var copy=town.copy();var result=FireDutyLife.assist(copy,state);
            apply(copy,result.minutes(),result.message());return new GeoProjection.ActionResult(result.message(),null);
        }
        if(action.equals("fire-training")){
            var copy=town.copy();String message=FireTrainingLife.train(copy,state);apply(copy,30,message);
            return new GeoProjection.ActionResult(message,null);
        }
        if(action.equals("fire-equipment-service")){
            var copy=town.copy();String message=FireServiceLife.playerService(copy,state);apply(copy,30,message);
            return new GeoProjection.ActionResult(message,null);
        }
        if(action.equals("fire-report")){
            var copy=town.copy();String message=FireServiceLife.playerReport(copy);apply(copy,1,message);
            return new GeoProjection.ActionResult(message,null);
        }
        if(action.equals("ambulance-call")){
            var copy=town.copy();String message=AmbulanceLife.call(copy,state);
            apply(copy,1,message);return new GeoProjection.ActionResult(message,null);
        }
        if(action.equals("ambulance-board")){
            var copy=town.copy();var result=AmbulanceLife.board(copy,state);
            apply(copy,result.minutes(),result.message());geoInterior=null;
            return new GeoProjection.ActionResult(result.message(),map.district(town.player.place));
        }
        if(action.startsWith("tow:")){
            var copy=town.copy();String message=TowServiceLife.request(copy,state,action.substring(4));
            apply(copy,1,message);return new GeoProjection.ActionResult(message,null);
        }
        if(action.equals("accident-treatment")){
            var copy=town.copy();String message=RoadAccidentLife.treat(copy,state);
            apply(copy,30,message);return new GeoProjection.ActionResult(message,null);
        }
        if(action.startsWith("vehicle-docs:")){
            String[] command=action.split(":",3);
            var copy=town.copy();String message=VehicleDocumentsLife.action(copy,state,command[1],command[2]);
            apply(copy,15,message);return new GeoProjection.ActionResult(message,null);
        }
        if(action.equals("road-fine-pay")){
            var copy=town.copy();String message=RoadPoliceLife.pay(copy,state);
            apply(copy,5,message);return new GeoProjection.ActionResult(message,null);
        }
        if(action.startsWith("license:")){
            String[] command=action.split(":",3);
            var copy=town.copy();
            var result=DrivingSchoolLife.action(copy,state,command[1],command[2]);
            apply(copy,result.minutes(),result.message());
            return new GeoProjection.ActionResult(result.message(),null);
        }
        if(action.startsWith("light:")){
            String[] command=action.split(":",3);
            var copy=town.copy();
            String message=LightMotorLife.action(copy,command[2],command[1],state.minute);
            apply(copy,command[1].equals("repair")?30:5,message);
            return new GeoProjection.ActionResult(message,null);
        }
        String[] parts=action.split(":",2);
        String arg=parts.length>1?parts[1]:"";
        ActionView result=switch(parts[0]){
            case "home-rent" -> rentRoom();
            case "home-buy" -> buyHouse();
            case "home-wash" -> washInRoom();
            case "home-cook" -> cookRoomMeal(arg.isEmpty()?"bread-soup":arg);
            case "home-sleep" -> sleepInRoom(Integer.parseInt(arg));
            case "home-store" -> transferRoomItem(arg,true);
            case "home-take" -> transferRoomItem(arg,false);
            case "rest" -> rest(15);
            case "consume" -> consume(arg);
            case "fountain" -> drink("park-fountain");
            case "buy" -> buy(arg);
            case "apply" -> applyJob(arg);
            case "work" -> work();
            case "study" -> study(arg);
            case "diagnose" -> diagnose(arg);
            case "repair-order" -> repair(arg);
            case "car" -> carAction(arg);
            case "bike-buy" -> buyBicycle();
            case "bike" -> transportMode(arg);
            case "drive" -> drive(arg);
            case "bus" -> gardenAction("bus-ride:"+arg);
            default -> throw new IllegalArgumentException("Unknown geographic action.");
        };
        return new GeoProjection.ActionResult(result.message(),before!=town.player.place?map.district(town.player.place):null);
    }

    public synchronized ActionView move(String destination) throws SQLException {
        var copy=town.copy();
        var place=PlayerLife.destination(destination);
        int minutes=PlayerLife.startTravel(copy.player,place);
        if(minutes==0)return new ActionView("Already at "+place.label+".",snapshot());
        return apply(copy,minutes,(copy.player.cycling?"Cycled to ":"Walked to ")+place.label+".");
    }

    // Reuse the established motor economy with the living adult's actual
    // wallet. The inactive visitor shell is enabled only during validation,
    // never during world ticks or persistence.
    static TownLife.Citizen beginLinkedMotorAction(TownLife.Town copy,int day){
        var c=RebirthLife.requireAdult(copy);
        if(!RebirthLife.awakePresent(c)||RebirthLife.atHome(c)||EmploymentLife.sick(c,day))
            throw new IllegalArgumentException("Go outside and finish traveling before driving.");
        var p=copy.player;
        p.place=c.place;p.destination=c.place;p.travelRemaining=0;
        p.money=c.money;p.alive=true;
        return c;
    }
    static void endLinkedMotorAction(TownLife.Town copy,TownLife.Citizen c){
        c.money=copy.player.money;
        copy.player.money=0;
        copy.player.alive=false;
        copy.player.travelRemaining=0;
        RebirthLife.synchronize(copy);
    }
    public synchronized ActionView carAction(String action) throws SQLException {
        TowServiceLife.requireAvailable(town,"car");
        var copy=town.copy();
        var linked=RebirthLife.active(copy)?beginLinkedMotorAction(copy,state.day):null;
        String message=switch(action==null?"":action) {
            case "buy" -> MotorLife.buy(copy,state.minute);
            case "refuel" -> MotorLife.refuel(copy,state.minute);
            case "repair" -> MotorLife.repair(copy,state.minute);
            default -> throw new IllegalArgumentException("Unknown car action.");
        };
        if(linked!=null){
            endLinkedMotorAction(copy,linked);
            RebirthLife.holdPublicPlace(copy,linked);
            return applyInternal(copy,action.equals("repair")?30:5,message,true);
        }
        return apply(copy,action.equals("repair")?30:5,message);
    }
    public synchronized ActionView drive(String destination) throws SQLException {
        var copy=town.copy();
        var place=PlayerLife.destination(destination);
        if(RebirthLife.active(copy)){
            var linked=beginLinkedMotorAction(copy,state.day);
            int minutes=MotorLife.drive(copy.player,place);
            endLinkedMotorAction(copy,linked);
            RebirthLife.travel(copy,place,state.day);
            linked.travelRemaining=minutes;
            return applyInternal(copy,minutes,"Drove to "+place.label+". Your car is parked there.",true);
        }
        int minutes=MotorLife.drive(copy.player,place);
        return apply(copy,minutes,"Drove to "+place.label+". Your car is parked there.");
    }

    public synchronized ActionView freightAction(String action,String destination) throws SQLException {
        var copy=town.copy();
        return switch(action==null?"":action) {
            case "load" -> apply(copy,5,FreightLife.load(copy,state.day,state.minute));
            case "service-truck-2" -> apply(copy,30,SecondFleetLife.manage(copy,action,destination,state.day));
            case "buy-truck-2","hire-driver-2","auto-on-2","auto-off-2" -> apply(copy,5,SecondFleetLife.manage(copy,action,destination,state.day));
            case "hire-driver" -> apply(copy,5,LogisticsLife.hire(copy,destination,state.day));
            case "load-company" -> apply(copy,5,FreightLife.load(copy,state.day,state.minute,true));
            case "service-truck" -> apply(copy,30,LogisticsLife.manage(copy,action));
            case "register","invest-25","invest-100","withdraw-25","buy-truck","auto-on","auto-off" -> apply(copy,5,LogisticsLife.manage(copy,action));
            case "drive" -> {
                var place=PlayerLife.destination(destination);
                int minutes=FreightLife.drive(copy,place);
                yield apply(copy,minutes,"Truck arrived at "+place.label+".");
            }
            case "unload" -> apply(copy,5,FreightLife.unload(copy,state.day));
            default -> throw new IllegalArgumentException("Unknown freight action.");
        };
    }

    public synchronized ActionView buyBicycle() throws SQLException {
        var copy=town.copy();
        return apply(copy,5,TransportLife.buyBicycle(copy,state.minute));
    }
    public synchronized ActionView transportMode(String mode) throws SQLException {
        var copy=town.copy();
        return apply(copy,1,TransportLife.mode(copy.player,mode));
    }

    public synchronized ActionView rest(int minutes) throws SQLException {
        var copy=town.copy();
        int recovered=PlayerLife.rest(copy.player,minutes);
        return apply(copy,minutes,"Rested for "+minutes+" minute(s); recovered up to "+
                recovered+" fatigue.");
    }

    public synchronized ActionView transferRoomItem(String item,boolean deposit) throws SQLException {
        var copy=town.copy();
        return apply(copy,1,HousingLife.transfer(copy.player,item,deposit,state.day,state.minute));
    }
    public synchronized ActionView cookRoomMeal() throws SQLException {
        return cookRoomMeal("bread-soup");
    }
    public synchronized ActionView cookRoomMeal(String recipeId) throws SQLException {
        var recipe=FoodLife.recipe(recipeId);
        var copy=town.copy();
        return apply(copy,recipe.minutes(),GardenLife.cookWithCompost(copy,recipe,state.day,state.minute));
    }
    public synchronized ActionView washInRoom() throws SQLException {
        var copy=town.copy();
        return apply(copy,10,HousingLife.wash(copy.player,state.day,state.minute));
    }

    public synchronized ActionView gardenAction(String action) throws SQLException {
        if("phone-telecom-subscribe".equals(action)){
            var copy=town.copy();TelecomLife.subscribe(copy,RebirthLife.requireAdult(copy),state);
            return applyInternal(copy,0,"SIM activated for 30 days.",true);
        }
        if("phone-digital-pay".equals(action)){
            var copy=town.copy();var message=DigitalLife.pay(copy,state);
            return applyInternal(copy,0,message,true);
        }
        if(persistentWorld&&action!=null&&action.startsWith("rebirth-wait"))
            throw new IllegalArgumentException("The server clock runs at 1:24. Waiting cannot advance the entire world.");
        if("justice-pay-fine".equals(action)){
            var copy=town.copy();return apply(copy,1,JusticeLife.payFine(copy));
        }
        JusticeLife.requireFree(town);
        if(action!=null&&action.startsWith("industrial-repair:")){
            var copy=town.copy();var step=IndustrialRepairLife.act(copy,state,action.substring("industrial-repair:".length()));
            return apply(copy,step.minutes(),step.message());
        }
        if(action!=null&&action.startsWith("apprentice:")){
            var copy=town.copy();var step=ApprenticeshipLife.act(copy,state,action.substring("apprentice:".length()));
            return apply(copy,step.minutes(),step.message());
        }
        if(action!=null&&action.startsWith("vocational:")){
            var copy=town.copy();var step=VocationalLife.act(copy,state,action.substring("vocational:".length()));
            return apply(copy,step.minutes(),step.message());
        }
        if(action!=null&&action.startsWith("personal-repair:")){
            var copy=town.copy();var step=PlayerRepairLife.act(copy,state,action.substring("personal-repair:".length()));
            return apply(copy,step.minutes(),step.message());
        }
        if(action!=null&&action.startsWith("repair-business:")){
            var copy=town.copy();return apply(copy,1,PlayerServiceBusiness.act(copy,state,action.substring("repair-business:".length())));
        }
        if(action!=null&&action.startsWith("home-service:")){
            var copy=town.copy();String command=action.substring("home-service:".length());
            if(command.equals("pay-gas"))return apply(copy,1,HomeServiceLife.payGas(copy,state));
            if(command.equals("cancel"))return apply(copy,1,HomeServiceLife.cancel(copy,state));
            if(command.startsWith("thermostat:"))return apply(copy,1,HomeServiceLife.thermostat(copy,state,Integer.parseInt(command.substring(11))));
            if(command.startsWith("repair:"))return apply(copy,1,HomeServiceLife.request(copy,state,command.substring(7)));
            throw new IllegalArgumentException("Unknown home service.");
        }
        if(action!=null&&action.startsWith("home-appliance:")){
            var copy=town.copy();String command=action.substring("home-appliance:".length());
            if(command.equals("pay"))return apply(copy,1,HomeApplianceLife.pay(copy,state));
            if(command.equals("watch"))return apply(copy,10,HomeApplianceLife.watch(copy,state));
            if(command.equals("wash"))return apply(copy,30,HomeApplianceLife.wash(copy,state));
            if(command.startsWith("store:")||command.startsWith("take:"))return apply(copy,1,HomeApplianceLife.transfer(copy,state,command.substring(command.indexOf(':')+1),command.startsWith("store:")));
            throw new IllegalArgumentException("Unknown household appliance action.");
        }
        if(action!=null&&action.startsWith("home-good:")){
            var copy=town.copy();return apply(copy,5,HomeGoodsLife.buy(copy,state,action.substring("home-good:".length())));
        }
        if("home-outfit:new".equals(action)||"home-outfit:original".equals(action)){
            var copy=town.copy();return apply(copy,2,HomeGoodsLife.wear(copy,state,"home-outfit:new".equals(action)));
        }
        if(action!=null&&action.startsWith("personal-good:")){
            String[] parts=action.split(":",4);var copy=town.copy();
            if(parts.length<3)throw new IllegalArgumentException("Choose a personal item.");
            String reply=switch(parts[1]){
                case "buy" -> PersonalGoodsLife.buy(copy,state,parts[2]);
                case "sell" -> PersonalGoodsLife.sell(copy,state,parts[2]);
                case "gift","lend","return" -> {if(parts.length!=4)throw new IllegalArgumentException("Choose a resident.");yield PersonalGoodsLife.exchange(copy,state,parts[1],parts[2],parts[3]);}
                default -> throw new IllegalArgumentException("Unknown personal goods action.");
            };return apply(copy,2,reply);
        }
        if("security-protection".equals(action)||"security-insurance".equals(action)){
            var copy=town.copy();return apply(copy,5,SecurityLife.purchase(copy,state,"security-insurance".equals(action)));
        }
        if(action!=null&&action.startsWith("vehicle-steal:")){
            var copy=town.copy();var clock=state.copy();var events=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
            LocalVehicleCrimeLife.take(copy,clock,action.substring("vehicle-steal:".length()),events,observations);
            return applyInternal(copy,1,"Vehicle reported stolen.",false,clock,events,observations);
        }
        if(action!=null&&action.startsWith("vehicle-stolen-drive:")){
            var copy=town.copy();var destination=PlayerLife.destination(action.substring("vehicle-stolen-drive:".length()));
            return apply(copy,LocalVehicleCrimeLife.drive(copy,destination),"Stolen vehicle journey recorded.");
        }
        if("vehicle-stolen-abandon".equals(action)){
            var copy=town.copy();return apply(copy,1,LocalVehicleCrimeLife.abandon(copy));
        }
        if("registry-identity".equals(action)||"registry-residence".equals(action)||"registry-health".equals(action)){
            var copy=town.copy();
            return apply(copy,15,CivilRegistryLife.act(copy,state,action));
        }
        if("employment-resign".equals(action)){
            var copy=town.copy();
            return apply(copy,5,EmploymentContractLife.resign(copy,state.day));
        }
        if(action!=null&&action.startsWith("job-application:")){
            var copy=town.copy();
            return apply(copy,15,InterviewLife.submit(copy,state,action.substring("job-application:".length())));
        }
        if(action!=null&&action.startsWith("job-interview:")){
            var copy=town.copy();
            return apply(copy,InterviewLife.DURATION,InterviewLife.attend(copy,state,action.substring("job-interview:".length())));
        }
        if("job-centre-register".equals(action)){
            var copy=town.copy();
            return apply(copy,15,EmploymentCenterLife.register(copy,state));
        }
        if("shelter-sleep".equals(action)){
            var copy=town.copy();
            var result=ShelterLife.sleep(copy,state);
            return apply(copy,result.minutes(),"Slept in the shelter for eight hours.");
        }
        if("shelter-housing".equals(action)){
            var copy=town.copy();
            return apply(copy,15,ShelterLife.housing(copy,state));
        }
        if("shelter-wash".equals(action)){
            var copy=town.copy();
            return apply(copy,10,ShelterLife.wash(copy,state));
        }
        if(action!=null&&action.startsWith("caritas-")){
            var copy=town.copy();
            return apply(copy,5,CaritasLife.act(copy,state,action));
        }
        if("registry-consult".equals(action)){
            var copy=town.copy();
            return apply(copy,CivilRegistryLife.CONSULT_MINUTES,CivilRegistryLife.consult(copy,state));
        }
        if(action!=null&&action.startsWith("bus-ride:")){
            var copy=town.copy();var destination=PlayerLife.destination(action.substring("bus-ride:".length()));
            int minutes=BusLife.ride(copy,state,destination);
            return apply(copy,minutes,"Bus arrived at "+destination.label+". Paid 2 EUR.");
        }
        if("weapon-license-apply".equals(action)){
            var copy=town.copy();return apply(copy,10,WeaponLicenseLife.apply(copy,state));
        }
        if("weapon-buy-sport".equals(action)){
            var copy=town.copy();return apply(copy,2,WeaponLicenseLife.buySport(copy,state));
        }
        if("weapon-refill".equals(action)){
            var copy=town.copy();return apply(copy,2,WeaponLicenseLife.refill(copy,state));
        }
        if("weapon-practice".equals(action)){
            var copy=town.copy();return apply(copy,2,WeaponLicenseLife.practice(copy,state));
        }
        if(action!=null&&(action.equals("weapon-fire")||action.startsWith("weapon-fire:"))){
            String id=action.equals("weapon-fire")?"":action.substring("weapon-fire:".length());
            var copy=town.copy();var clock=state.copy();
            var truth=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
            var result=CombatLife.fire(copy,clock,id,truth,observations);
            return applyInternal(copy,result.minutes(),result.message(),false,clock,truth,observations);
        }
        if("combat-buy-baton".equals(action)){
            var copy=town.copy();
            return apply(copy,2,CombatLife.buyBaton(copy,state));
        }
        if(action!=null&&action.startsWith("combat-strike:")){
            String[] parts=action.split(":",3);
            if(parts.length!=3||!List.of("fists","baton").contains(parts[1]))throw new IllegalArgumentException("Choose an attack and resident.");
            var copy=town.copy();var clock=state.copy();
            var truth=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
            var result=CombatLife.strike(copy,clock,parts[2],parts[1],truth,observations);
            return applyInternal(copy,result.minutes(),result.message(),false,clock,truth,observations);
        }
        if(action!=null&&action.startsWith("barber-")){
            var copy=town.copy();
            var service=barberVisit(copy,action.substring(7),state.minute);
            return apply(copy,service.minutes(),"Barber: "+service.label()+". Paid "+service.price()+" EUR.");
        }
        if(action!=null&&action.startsWith("intercom:")){
            String[] parts=action.split(":",4);
            if(parts.length!=4||!java.util.Set.of("ring","visit","invite","enter","leave","talk","coffee","snack","rest","dinner","request-key","use-key","return-key").contains(parts[1])||!java.util.Set.of("1","2","3").contains(parts[2]))
                throw new IllegalArgumentException("Choose a known door and contact.");
            var copy=town.copy();
            int building=Integer.parseInt(parts[2]);
            if(java.util.Set.of("talk","coffee","snack","rest","dinner").contains(parts[1])){
                var result=IntercomLife.socialize(copy,parts[3],building,parts[1],state.day,state.minute);
                return apply(copy,result.minutes(),result.message());
            }
            boolean visit=parts[1].equals("visit");
            String reply=switch(parts[1]){
                case "visit" -> IntercomLife.visit(copy,parts[3],building,state.day,state.minute);
                case "invite" -> IntercomLife.invite(copy,parts[3],building,state.day,state.minute);
                case "enter" -> IntercomLife.enter(copy,parts[3],building,state.day,state.minute);
                case "leave" -> IntercomLife.leave(copy,parts[3],building,state.day,state.minute);
                case "request-key" -> IntercomLife.requestKey(copy,parts[3],building,state.day,state.minute);
                case "use-key" -> IntercomLife.useKey(copy,parts[3],building,state.day,state.minute);
                case "return-key" -> IntercomLife.returnKey(copy,parts[3],building,state.day,state.minute);
                default -> IntercomLife.ring(copy,parts[3],building,state.day,state.minute);
            };
            return apply(copy,visit?5:1,reply);
        }
        if(action!=null&&action.startsWith("host-cook:")){
            var copy=town.copy();
            String id=action.substring("host-cook:".length());
            String result=PhoneLife.cookTogether(copy,id,state.day,state.minute);
            return apply(copy,25,result);
        }
        if(action!=null&&action.startsWith("phone-out:")){
            String[] parts=action.split(":",3);
            if(parts.length!=3||!java.util.Set.of("dial","hangup","invite-coffee","ask-home-visit","invite-my-home").contains(parts[1]))throw new IllegalArgumentException("Choose an outgoing call action.");
            var copy=town.copy();
            return apply(copy,1,PhoneLife.outgoing(copy,parts[2],parts[1],state.day,state.minute));
        }
        if(action!=null&&action.startsWith("phone-call:")){
            String[] parts=action.split(":",3);
            if(parts.length!=3||!java.util.Set.of("answer","decline","accept").contains(parts[1]))throw new IllegalArgumentException("Choose a call action.");
            var copy=town.copy();
            return apply(copy,1,PhoneLife.answerCall(copy,parts[2],parts[1],state.day,state.minute));
        }
        if(action!=null&&action.startsWith("phone-reply:")){
            String[] parts=action.split(":",3);
            if(parts.length!=3||!java.util.Set.of("1","2").contains(parts[1]))throw new IllegalArgumentException("Choose a message reply.");
            var copy=town.copy();
            return apply(copy,1,PhoneLife.reply(copy,parts[2],Integer.parseInt(parts[1]),state.day));
        }
        if(action!=null&&action.startsWith("rebirth-")){
            var copy=town.copy();
            if(action.equals("rebirth-return"))return applyInternal(copy,0,RebirthLife.resumeAfterDeath(copy,state.day),true);
            if(RebirthLife.returnPending(copy))throw new IllegalArgumentException("Continue your next life first.");
            if(action.equals("rebirth-first-boy")||action.equals("rebirth-first-girl")){
                FirstLifeChoice.choose(copy,action.endsWith("boy")?1:2);
                InitialFamilyLife.start(copy,state);
                return applyInternal(copy,0,"Birth preference recorded. / Preferenza di nascita registrata.",true);
            }
            if(FirstLifeChoice.pending(copy))throw new IllegalArgumentException("Choose your first character first.");
            if(action.equals("rebirth-door-lock")||action.equals("rebirth-door-unlock")){
                FamilyDoorLife.act(copy,state,action.endsWith("-lock"));
                return applyInternal(copy,0,action.endsWith("-lock")?"Family door temporarily locked.":"Family door unlocked.",true);
            }
            if(NpcJusticeLife.confined(copy,RebirthLife.child(copy),state))throw new IllegalArgumentException("You are in custody.");
            if(action.startsWith("rebirth-culture-")){
                String message=CultureLife.act(copy,state,action.substring("rebirth-culture-".length()));
                return applyInternal(copy,0,message,true);
            }
            if(action.startsWith("rebirth-athletics-")){
                String message=AthleticsLife.act(copy,state,action.substring("rebirth-athletics-".length()));
                return applyInternal(copy,0,message,true);
            }
            if(action.startsWith("rebirth-clinical-request-")){
                var person=RebirthLife.requireAdult(copy);
                ClinicalLife.request(copy,person,state,ClinicalLife.Specialty.valueOf(action.substring("rebirth-clinical-request-".length())));
                return applyInternal(copy,0,"Appointment requested. Stay at the clinic until a clinician is available.",true);
            }
            if(action.startsWith("rebirth-substance-")){
                var person=RebirthLife.requireAdult(copy);
                String command=action.substring("rebirth-substance-".length());
                if(command.equals("support")){SubstanceLife.support(copy,person,state);return applyInternal(copy,0,"Support visit recorded.",true);}
                if(!command.startsWith("use-"))throw new IllegalArgumentException("Unknown substance action.");
                SubstanceLife.exposure(copy,person,state,SubstanceLife.Kind.valueOf(command.substring(4)));
                return applyInternal(copy,0,"Fictional exposure recorded; health consequences may follow.",true);
            }
            if(action.startsWith("rebirth-conviction-")){
                String choice=action.substring("rebirth-conviction-".length());
                var person=RebirthLife.child(copy);
                if(choice.equals("reflect")){ConvictionLife.reflect(copy,person,state);return applyInternal(copy,15,"Quiet reflection.",true);}
                ConvictionLife.choose(copy,person,ConvictionLife.Belief.valueOf(choice));
                return applyInternal(copy,0,"Personal belief recorded.",true);
            }
            if(action.startsWith("rebirth-corruption-")){
                String message=CorruptionLife.act(copy,state,action.substring("rebirth-corruption-".length()));
                return applyInternal(copy,0,message,true);
            }
            if(action.startsWith("rebirth-election-")){
                String message=ElectionLife.act(copy,action.substring("rebirth-".length()),state.day,state.minute);
                return applyInternal(copy,0,message,true);
            }
            if(action.equals("rebirth-separate")){
                var self=RebirthLife.requireAdult(copy);var partner=RebirthLife.partner(copy,self);
                if(partner==null)throw new IllegalArgumentException("No current partner.");
                RelationshipBoundaryLife.separate(copy,self,partner,state.day,state.minute);
                return applyInternal(copy,0,"Your partnership ended. / La relazione è terminata.",true);
            }
            if(action.equals("rebirth-wait-hour")||action.equals("rebirth-wait-adult-day")){
                RebirthLife.requireAdult(copy);
                return applyInternal(copy,action.equals("rebirth-wait-hour")?60:1440,"Advanced adult life.",true);
            }
            if(action.startsWith("rebirth-it:")){
                var result=ITBusinessLife.action(copy,state.day,state.minute,action.substring("rebirth-it:".length()));
                return applyInternal(copy,result.minutes(),result.message(),true);
            }
            if(action.equals("rebirth-devops-train")){
                return applyInternal(copy,30,DevOpsLife.train(copy,state.day,state.minute),true);
            }
            if(action.startsWith("rebirth-devops-command:")){
                var result=DevOpsLife.command(copy,state.day,state.minute,action.substring("rebirth-devops-command:".length()));
                return applyInternal(copy,result.minutes(),result.message(),true);
            }
            if(action.equals("rebirth-developer-promote")){
                return applyInternal(copy,15,DeveloperCareerLife.promote(copy,state.day,state.minute),true);
            }
            if(action.startsWith("rebirth-developer-audit:")){
                var result=DeveloperCareerLife.audit(copy,state.day,state.minute,action.substring("rebirth-developer-audit:".length()));
                return applyInternal(copy,result.minutes(),result.message(),true);
            }
            if(action.startsWith("rebirth-git:")){
                String[] parts=action.split(":",3);
                if(parts.length!=3)throw new IllegalArgumentException("Choose a Git step and ticket.");
                var result=SoftwareLife.git(copy,state.day,state.minute,parts[2],parts[1]);
                return applyInternal(copy,result.minutes(),result.message(),true);
            }
            if(action.equals("rebirth-java-onboard")){
                return applyInternal(copy,15,JavaTicketLife.onboard(copy,state.day,state.minute),true);
            }
            if(action.startsWith("rebirth-java-submit:")){
                String[] parts=action.split(":",3);
                if(parts.length!=3)throw new IllegalArgumentException("Provide ticket and Java return statement.");
                var result=SoftwareLife.submit(copy,state.day,state.minute,parts[1],parts[2]);
                return applyInternal(copy,result.minutes(),result.message(),true);
            }
            if(action.startsWith("rebirth-software-")){
                var result=SoftwareLife.advance(copy,state.day,state.minute,action.substring("rebirth-software-".length()));
                return applyInternal(copy,result.minutes(),result.message(),true);
            }
            if(action.startsWith("rebirth-engineer-inspect-")){
                String result=TechnicalWorkLife.linkedInspect(copy,action.substring("rebirth-engineer-inspect-".length()),state.day,state.minute);
                return applyInternal(copy,TechnicalWorkLife.INSPECTION_MINUTES,result,true);
            }
            if(action.startsWith("rebirth-engineer-repair-")){
                String result=TechnicalWorkLife.linkedComplete(copy,action.substring("rebirth-engineer-repair-".length()),state.day,state.minute);
                return applyInternal(copy,TechnicalWorkLife.COMPLETION_MINUTES,result,true);
            }
            if(action.startsWith("rebirth-enroll-")){
                return applyInternal(copy,0,EducationLife.enroll(copy,state.day,state.minute,action.substring("rebirth-enroll-".length())),true);
            }
            if(action.equals("rebirth-study")){
                String result=EducationLife.study(copy,state.day,state.minute);
                RebirthLife.travel(copy,TownLife.Place.SCHOOL,state.day);
                return applyInternal(copy,60,result,true);
            }
            if(action.startsWith("rebirth-dojo-match-")){
                var exchange=KumiteLife.act(copy,state.day,state.minute,action.substring("rebirth-dojo-match-".length()));
                return applyInternal(copy,exchange.minutes(),exchange.message(),true);
            }
            if(action.equals("rebirth-dojo-rei")){
                var greeting=DojoCommunityLife.greet(copy,state.day,state.minute);
                return applyInternal(copy,greeting.minutes(),greeting.message(),true);
            }
            if(action.startsWith("rebirth-dojo-")){
                var session=DojoLife.act(copy,state.day,state.minute,action.substring("rebirth-dojo-".length()));
                return applyInternal(copy,session.minutes(),session.message(),true);
            }
            if(action.startsWith("rebirth-sport-")){
                var session=SportLife.train(copy,state.day,state.minute,action.substring("rebirth-sport-".length()));
                return applyInternal(copy,session.minutes(),session.message(),true);
            }
            if(action.equals("rebirth-medical-visit")){
                int duration=RebirthLife.medicalVisit(copy,state.day,state.minute);
                return applyInternal(copy,duration,"Went to the medical district.",true);
            }
            if(action.startsWith("rebirth-medical-")){
                var service=RebirthLife.medicalAction(copy,state.day,state.minute,action.substring("rebirth-medical-".length()));
                return applyInternal(copy,service.minutes(),service.message(),true);
            }
            if(action.startsWith("rebirth-cafe-")){
                var visit=RebirthLife.cafeAction(copy,state.day,state.minute,action.substring("rebirth-cafe-".length()));
                return applyInternal(copy,visit.minutes(),visit.message(),true);
            }
            if(action.startsWith("rebirth-grocery-")){
                String message=RebirthLife.buyGrocery(copy,state.day,state.minute,action.substring("rebirth-grocery-".length()));
                return applyInternal(copy,5,message,true);
            }
            if(action.equals("rebirth-child-exit")){
                return applyInternal(copy,0,RebirthLife.childExit(copy,state),true);
            }
            if(action.startsWith("rebirth-child-road-")){
                var destination=PlayerLife.destination(action.substring("rebirth-child-road-".length()));
                int duration=RebirthLife.childRoad(copy,destination,state.day,state.minute);
                return applyInternal(copy,duration,"Walked to "+destination.label+".",true);
            }
            if(action.equals("rebirth-home-exit")){
                return applyInternal(copy,0,RebirthLife.leaveHome(copy,state.day),true);
            }
            if(action.startsWith("rebirth-road-")){
                var destination=PlayerLife.destination(action.substring("rebirth-road-".length()));
                int duration=RebirthLife.roadTravel(copy,destination,state.day);
                return applyInternal(copy,duration,"Walked along the road to "+destination.label+".",true);
            }
            if(action.equals("rebirth-home-go")){
                int duration=RebirthLife.goHome(copy);
                return applyInternal(copy,duration,"Returned to your own home.",true);
            }
            if(action.startsWith("rebirth-home-")){
                var home=RebirthLife.homeAction(copy,state.day,action.substring("rebirth-home-".length()));
                return applyInternal(copy,home.minutes(),home.message(),true);
            }
            if(action.equals("rebirth-prologue-step")){
                String message=ChildhoodPrologue.step(copy);
                if(RebirthLife.phase(copy)==RebirthLife.LINKED_ADULT)ParentingLife.prompt(copy,RebirthLife.child(copy),state.day);
                return applyInternal(copy,persistentWorld?0:1,message,true);
            }
            if(action.startsWith("rebirth-parent:")){
                var result=ParentingLife.act(copy,action.substring("rebirth-parent:".length()));
                return applyInternal(copy,result.minutes(),result.message(),true);
            }
            if(action.startsWith("rebirth-early-")){
                var result=EarlyChildhoodLife.act(copy,action.substring("rebirth-early-".length()));
                return applyInternal(copy,result.minutes(),result.message(),true);
            }
            if(action.equals("rebirth-child-hour")){
                var child=RebirthLife.child(copy);
                if(!RebirthLife.linkedChild(copy,child)||child.age<6)throw new IllegalArgumentException("Available from age six.");
                return applyInternal(copy,60,"Advanced childhood by one hour.",true);
            }
            if(action.startsWith("rebirth-hobby-")){
                String message=RebirthLife.hobby(copy,state.day,state.minute,action.substring("rebirth-hobby-".length()));
                return applyInternal(copy,30,message,true);
            }
            if(action.equals("rebirth-child-home")){
                int duration=RebirthLife.childReturn(copy);
                return applyInternal(copy,duration,"Return home.",true);
            }
            if(action.startsWith("rebirth-child-travel-")){
                var destination=PlayerLife.destination(action.substring("rebirth-child-travel-".length()));
                int duration=RebirthLife.childTravel(copy,destination,state.day,state.minute);
                return applyInternal(copy,duration,"Outing to "+destination.label+".",true);
            }
            if(action.startsWith("rebirth-travel-")){
                var destination=PlayerLife.destination(action.substring("rebirth-travel-".length()));
                int duration=RebirthLife.travel(copy,destination,state.day);
                return applyInternal(copy,duration,"Travel to "+destination.label+".",true);
            }
            if(action.equals("rebirth-wait-extended")){
                int days=RebirthLife.extendedWaitDays(town);
                if(days==0)throw new IllegalArgumentException("Extended waiting is only available during childhood.");
                return applyInternal(copy,days*1440,"Advanced childhood in the shared world.",true);
            }
            if(action.equals("rebirth-wait")||action.equals("rebirth-wait-fast")){
                int maxDays=RebirthLife.maxWaitDays(town);
                if(maxDays==0)throw new IllegalArgumentException("No childhood waiting is available.");
                if(action.equals("rebirth-wait-fast")&&maxDays<=1)
                    throw new IllegalArgumentException("Fast waiting is not available at this age or phase.");
                int days=action.equals("rebirth-wait-fast")?maxDays:1;
                return applyInternal(copy,days*1440,"Advanced the shared world.",true);
            }
            String message;
            if(action.equals("rebirth-adult"))message=RebirthLife.adultFallback(copy);
            else if(action.equals("rebirth-allowance"))message=RebirthLife.allowance(copy,state.day,state.minute);
            else if(action.equals("rebirth-continue"))message=RebirthLife.continueAdult(copy);
            else if(action.startsWith("rebirth-propose-"))message=RebirthLife.propose(copy,action.substring("rebirth-propose-".length()),state.day,state.minute);
            else if(action.equals("rebirth-move-together"))message=RebirthLife.moveTogether(copy);
            else if(action.equals("rebirth-have-child"))message=RebirthLife.haveChild(copy,state.day);
            else if(action.equals("rebirth-support-partner"))message=RebirthLife.supportPartner(copy);
            else if(action.startsWith("rebirth-wardrobe-"))message=WardrobeLife.act(copy,action.substring("rebirth-wardrobe-".length()),state.day,state.minute);
            else if(action.startsWith("rebirth-property-"))message=HomeMarketLife.act(copy,action.substring("rebirth-property-".length()),state.minute);
            else if(action.startsWith("rebirth-budget-"))message=FamilyBudgetLife.action(copy,action.substring("rebirth-budget-".length()),state);
            else if(action.equals("rebirth-routine"))message=RebirthLife.resumeRoutine(copy);
            else if(action.startsWith("rebirth-job-"))message=EmploymentLife.chooseRole(copy,state,action.substring("rebirth-job-".length()));
            else if(action.startsWith("rebirth-ambition-"))message=RebirthLife.chooseAmbition(copy,action.substring("rebirth-ambition-".length()));
            else if(action.startsWith("rebirth-focus-"))message=RebirthLife.chooseFocus(copy,action.substring("rebirth-focus-".length()));
            else if(action.startsWith("rebirth-chat-"))message=RebirthLife.chat(copy,state.day,action.substring("rebirth-chat-".length()));
            else if(action.startsWith("rebirth-family-")){
                String[] values=action.substring("rebirth-family-".length()).split("-");
                if(values.length!=2)throw new IllegalArgumentException("Choose two eligible parents.");
                message=RebirthLife.choose(copy,state.day,Integer.parseInt(values[0]),Integer.parseInt(values[1]));
            }else throw new IllegalArgumentException("Unknown rebirth action.");
            return applyInternal(copy,0,message,true);
        }
        if(action!=null&&action.startsWith("archive-year-")){
            TransportLife.ready(town.player);
            if(town.player.place!=TownLife.Place.LIBRARY)throw new IllegalArgumentException("Visit the public library.");
            String[] values=action.substring("archive-year-".length()).split("-");
            if(values.length!=2)throw new IllegalArgumentException("Choose an archive year and page.");
            var page=ArchiveLife.report(town,state.day,Integer.parseInt(values[0]),Integer.parseInt(values[1]));
            archiveYear=page.year();archivePage=page.page();
            return new ActionView("Opened public historical archive, year "+archiveYear+".",snapshot());
        }
        var copy=town.copy();
        return apply(copy,"plant".equals(action)||"harvest".equals(action)||"buy-barrel".equals(action)||"build-greenhouse".equals(action)||"build-composter".equals(action)||"build-coop".equals(action)||(action!=null&&(action.startsWith("orchard-plant-")||action.startsWith("orchard-harvest-")||action.equals("orchard-build-cellar")||action.equals("orchard-press-juice")||action.equals("orchard-start-wine")))?15:5,GardenLife.act(copy,action,state.day,state.minute));
    }
    public synchronized ActionView buyHouse() throws SQLException {
        var copy=town.copy();
        return apply(copy,5,HousingLife.buyHouse(copy,state.day,state.minute));
    }
    public synchronized ActionView rentRoom() throws SQLException {
        var copy=town.copy();
        return apply(copy,5,HousingLife.rent(copy,state.day,state.minute));
    }
    public synchronized ActionView sleepInRoom(int hours) throws SQLException {
        var copy=town.copy();
        var result=HousingLife.sleep(copy.player,hours,state.day,state.minute);
        return apply(copy,result.minutes(),"Slept in your home for "+hours+" hours.");
    }

    public synchronized ActionView sleep(int hours) throws SQLException {
        var copy=town.copy();
        var result=PlayerLife.sleep(copy.player,hours,state.day,state.minute);
        return apply(copy,result.minutes(),
                "Slept "+hours+" hour(s). Health changed by "+result.healthChange()+".");
    }

    public synchronized ActionView buy(String productId) throws SQLException {
        var copy=town.copy();
        return apply(copy,2,RetailLife.buy(copy,productId,state.minute));
    }
    public synchronized ActionView consume(String productId) throws SQLException {
        var copy=town.copy();
        return apply(copy,5,RetailLife.consume(copy.player,productId));
    }

    public synchronized ActionView drink(String sourceId) throws SQLException {
        var copy=town.copy();
        String message="park-fountain".equals(sourceId)
                ? UtilityLife.drinkAtFountain(copy)
                : NatureLife.drink(copy,sourceId,state.day,state.minute);
        return apply(copy,5,message);
    }

    public synchronized ActionView forage(String sourceId) throws SQLException {
        var copy=town.copy();
        String message=NatureLife.forage(copy,sourceId);
        return apply(copy,15,message);
    }

    private ActionView apply(TownLife.Town candidateTown,int minutes,String message)
            throws SQLException {
        return applyInternal(candidateTown,minutes,message,false);
    }
    private ActionView applyInternal(TownLife.Town candidateTown,int minutes,String message,boolean rebirthAction) throws SQLException {
        return applyInternal(candidateTown,minutes,message,rebirthAction,state.copy(),new ArrayList<>(),new ArrayList<>());
    }
    private ActionView applyInternal(TownLife.Town candidateTown,int minutes,String message,boolean rebirthAction,
            WorldState candidateState,List<TownLife.Event> truth,List<TownLife.Observation> observations) throws SQLException {
        return applyInternal(candidateTown,minutes,message,rebirthAction,candidateState,truth,observations,false);
    }
    private ActionView applyInternal(TownLife.Town candidateTown,int minutes,String message,boolean rebirthAction,
            WorldState candidateState,List<TownLife.Event> truth,List<TownLife.Observation> observations,boolean custodyWait) throws SQLException {
        if(!custodyWait)JusticeLife.requireFree(town);
        if(RebirthLife.active(town)&&!rebirthAction)throw new IllegalArgumentException("Wait for birth or use childhood actions first.");
        if("You shared dinner.".equals(message)||"You cooked and ate together.".equals(message)){
            var host=candidateTown.get("marco");
            candidateState.food=host.food;candidateState.water=host.water;
            candidateState.hunger=host.hunger;candidateState.thirst=host.thirst;
        }
        ArchiveLife.transitions(town,candidateTown,state.day,state.minute);
        var events=new ArrayList<LifeSimulation.LifeEvent>();
        var interactions=new ArrayList<SocialLife.Interaction>();
        boolean initiallyConfined=JusticeLife.confined(candidateTown);
        int initialPhase=RebirthLife.phase(candidateTown);
        int initialMaxDays=RebirthLife.maxWaitDays(candidateTown);
        boolean background="Persistent world routine.".equals(message)||"Routine while away.".equals(message);
        events.addAll(LifeSimulation.advance(candidateState,minutes,
                current->TownLife.tick(candidateTown,current,truth,observations,interactions),
                ()->(initiallyConfined||!JusticeLife.confined(candidateTown))&&(!rebirthAction||minutes==0||background&&RebirthLife.returnPending(candidateTown)||(initialPhase==RebirthLife.LINKED_ADULT?RebirthLife.continueAdultWaiting(candidateTown):RebirthLife.canContinueWaiting(candidateTown,initialPhase,initialMaxDays)))));
        if(!initiallyConfined&&JusticeLife.confined(candidateTown))message+=" Police detained you.";
        TownLife.clearConversationHolds(candidateTown);
        if(rebirthAction&&minutes>1){ // Keep one-minute dialogue replies free of clock diagnostics.
            long elapsed=(candidateState.day-(long)state.day)*1440+candidateState.minute-state.minute;
            message+=" Elapsed: "+elapsed/1440+" day(s), "+elapsed%1440+" minute(s).";
        }
        if(!candidateTown.player.alive&&!RebirthLife.active(candidateTown)) {
            RebirthLife.prepare(candidateTown,candidateState.day,candidateState.minute);
            message+=RebirthLife.randomBirth(candidateTown)?" Your previous life ended. Await your next birth.":" Your previous life ended. Choose a family for your next life.";
        }
        if(RebirthLife.settleLinkedDeath(candidateTown,candidateState.day,candidateState.minute)){
            message+=" Your life ended. Its history is preserved. Your next life awaits your return.";
        }
        if(initialPhase==RebirthLife.CHOICE&&RebirthLife.phase(candidateTown)==RebirthLife.CHILDHOOD&&RebirthLife.randomBirth(candidateTown))
            message+=" Born as "+RebirthLife.child(candidateTown).name+".";
        RebirthLife.synchronize(candidateTown);
        PassportLife.ensure(candidateTown,candidateState);
        db.save(candidateState,events,List.of(),candidateTown,truth,observations,interactions);
        state=candidateState;
        town=candidateTown;
        recentPeerEvents.addAll(interactions);
        prunePeerEvents((long)(state.day-1)*1440L+state.minute);
        return new ActionView(message,snapshot());
    }

    @Override
    @PreDestroy
    public synchronized void close() throws SQLException {
        if(closed)return;
        closed=true;
        try{
            if(localOfflineProgress||persistentWorld)db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }finally{db.close();}
    }
}
