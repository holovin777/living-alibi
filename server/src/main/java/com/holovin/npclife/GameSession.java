package com.holovin.npclife;

import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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

    static final String POPULATION_SEED_TARGET="population:seed-target";

    @Autowired
    public GameSession(@Value("${living-alibi.database:data/world.db}") String databasePath,
                       @Value("${living-alibi.population:28}") int population)
            throws Exception {
        this(Path.of(databasePath),population);
    }

    // Small deterministic fixtures remain available to existing regression tests.
    GameSession(Path databasePath) throws Exception {
        this(databasePath,12);
    }

    GameSession(Path databasePath,int population) throws Exception {
        if(population<12||population>PopulationGenerator.MAX_GENERATED)
            throw new IllegalArgumentException("Initial generated population must be between 12 and "+PopulationGenerator.MAX_GENERATED);
        boolean firstLaunch=Files.notExists(databasePath);
        db=new WorldDatabase(databasePath);
        state=db.load();
        town=db.loadTown(state);
        int previousTarget=town.industryStocks.getOrDefault(POPULATION_SEED_TARGET,12);
        boolean changed=false;
        if(firstLaunch||population>previousTarget) {
            // One-time immigration, not an automatic respawn after deaths.
            int missing=Math.max(0,population-(int)DemographyLife.living(town));
            while(missing>0){
                int batch=Math.min(20,missing);
                PopulationGenerator.add(town,batch);
                missing-=batch;
            }
            town.industryStocks.put(POPULATION_SEED_TARGET,population);
            changed=true;
        }
        // Old save files might contain a deceased visitor.
        changed=RebirthLife.restoreIdentity(town)||changed;
        if(!town.player.alive&&!RebirthLife.active(town)) {
            RebirthLife.prepare(town,state.day,state.minute);
            changed=true;
        }
        if(RebirthLife.settleLinkedDeath(town,state.day,state.minute))changed=true;
        RebirthLife.synchronize(town);
        if(changed)db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
    }

    public record PlayerView(int incarnation,String place,String placeId,String privateName,
                             int hunger,int thirst,int fatigue,int health,boolean alive,
                             int money,String jobId,int engineeringSkill,int investigationSkill,
                             int bread,int bottledWater,long roomUntil,boolean roomActive,int storedBread,int storedWater,int hygiene,
                             java.util.Map<String,Integer> bag,java.util.Map<String,Integer> pantry,boolean ownsBicycle,boolean cycling,String bicycleParked,String carPlace,int carFuel,int carCondition,List<String> carDestinations,boolean ownsHouse) { }
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
    public record ResidentView(String id,String name,String activity,String knownName,int conversations,String relationship,String mood,int personalDebt,int personalDebtDueDay) { }
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
    public record WorldView(int day,int minute,int year,int dayOfYear,String season,
                            WeatherView weather,PlayerView player,List<ResidentView> residents,
                            List<SourceView> sources,List<MutualAidLife.Notice> aidNotices,ServicesView services,List<RetailView> shop,FarmLife.Report farm,FuelLife.Report fuel,FreightLife.Report freight,LogisticsLife.Report logistics,GardenLife.Report garden,HenLife.Report hens,OrchardLife.Report orchard,MarketStallLife.Report stall,BankLife.Report bank,RentalLife.Report rental,ConstructionLife.Report construction,MunicipalLife.Report comune,DemographyLife.Report demography,EducationLife.Report education,ArchiveLife.Report archive,NewspaperLife.Report newspaper,OpinionLife.Report opinion,ElectionLife.Report election,RebirthLife.Report rebirth,PhoneLife.Report phone) { }
    public record ActionView(String message,WorldView world) { }

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
                p.carPlace==p.place?MotorLife.neighbors(p.place).stream().map(Enum::name).toList():List.of(),p.ownsHouse);
        var residents=(RebirthLife.active(town)?RebirthLife.visible(town):PlayerLife.visible(town)).stream()
                .map(c->{
                    var contact=p.contacts.get(c.id);
                    return new ResidentView(c.id,c.name,c.activity,
                            contact==null?null:contact.knownName,
                            contact==null?0:contact.conversations,PlayerLife.relationship(contact),EmotionLife.mood(town,c,state.day,state.minute).name(),PersonalLoanLife.outstanding(town,c.id),PersonalLoanLife.dueDay(town,c.id));
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
                            town.retailStock.getOrDefault(item.id(),0))).toList() : List.of(),FarmLife.report(town),FuelLife.report(town),FreightLife.report(town),LogisticsLife.report(town,state.day),GardenLife.report(town,state.day,state.minute),HenLife.report(town,state.day),OrchardLife.report(town,state.day,state.minute),MarketStallLife.report(town,state.day,state.minute),BankLife.report(town),RentalLife.report(town),ConstructionLife.report(town),MunicipalLife.report(town),DemographyLife.report(town),EducationLife.report(town),p.alive&&p.travelRemaining==0&&p.place==TownLife.Place.LIBRARY?ArchiveLife.report(town,state.day,archiveYear,archivePage):null,p.alive&&p.travelRemaining==0&&p.place==TownLife.Place.LIBRARY?NewspaperLife.report(town,state.day):null,OpinionLife.report(town),ElectionLife.report(town,state.day),RebirthLife.report(town,state.day,state.minute),PhoneLife.report(town));
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
                        town.employerFunds.getOrDefault(v.id(),0))).toList();
    }

    public synchronized ActionView applyJob(String jobId) throws SQLException {
        var candidate=town.copy();
        String message=CareerLife.apply(candidate,jobId);
        return apply(candidate,15,message);
    }

    public synchronized ActionView study(String subject) throws SQLException {
        var candidate=town.copy();
        String message=CareerLife.study(candidate,subject,state.minute);
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
        if(residentId==null || message==null)
            throw new IllegalArgumentException("Resident and message are required.");
        var copy=town.copy();
        var resident=PlayerLife.nearby(copy,residentId);
        String reply=message.strip().equalsIgnoreCase("Any public news for the newspaper?")?NewspaperLife.interview(copy,resident,state.day,state.minute):PlayerLife.speak(copy,residentId,message,state.day,state.minute);
        return apply(copy,1,resident.name+": "+reply);
    }

    public synchronized ActionView waitMinutes(int minutes) throws SQLException {
        if(minutes<1 || minutes>1440)
            throw new IllegalArgumentException("Wait between 1 and 1440 game minutes.");
        return apply(town.copy(),minutes,"Waited "+minutes+" minute(s).");
    }

    public synchronized ActionView move(String destination) throws SQLException {
        var copy=town.copy();
        var place=PlayerLife.destination(destination);
        int minutes=PlayerLife.startTravel(copy.player,place);
        if(minutes==0)return new ActionView("Already at "+place.label+".",snapshot());
        return apply(copy,minutes,(copy.player.cycling?"Cycled to ":"Walked to ")+place.label+".");
    }

    public synchronized ActionView carAction(String action) throws SQLException {
        var copy=town.copy();
        String message=switch(action==null?"":action) {
            case "buy" -> MotorLife.buy(copy,state.minute);
            case "refuel" -> MotorLife.refuel(copy,state.minute);
            case "repair" -> MotorLife.repair(copy,state.minute);
            default -> throw new IllegalArgumentException("Unknown car action.");
        };
        return apply(copy,action.equals("repair")?30:5,message);
    }
    public synchronized ActionView drive(String destination) throws SQLException {
        var copy=town.copy();
        var place=PlayerLife.destination(destination);
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
        if(action!=null&&action.startsWith("phone-reply:")){
            String[] parts=action.split(":",3);
            if(parts.length!=3||!java.util.Set.of("1","2").contains(parts[1]))throw new IllegalArgumentException("Choose a message reply.");
            var copy=town.copy();
            return apply(copy,1,PhoneLife.reply(copy,parts[2],Integer.parseInt(parts[1]),state.day));
        }
        if(action!=null&&action.startsWith("rebirth-")){
            var copy=town.copy();
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
        if(RebirthLife.active(town)&&!rebirthAction)throw new IllegalArgumentException("Choose a family or use childhood actions first.");
        var candidateState=state.copy();
        ArchiveLife.transitions(town,candidateTown,state.day,state.minute);
        var events=new ArrayList<LifeSimulation.LifeEvent>();
        var truth=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();
        var interactions=new ArrayList<SocialLife.Interaction>();
        int initialPhase=RebirthLife.phase(candidateTown);
        int initialMaxDays=RebirthLife.maxWaitDays(candidateTown);
        events.addAll(LifeSimulation.advance(candidateState,minutes,
                current->TownLife.tick(candidateTown,current,truth,observations,interactions),
                ()->!rebirthAction||minutes==0||(initialPhase==RebirthLife.LINKED_ADULT?RebirthLife.continueAdultWaiting(candidateTown):RebirthLife.canContinueWaiting(candidateTown,initialPhase,initialMaxDays))));
        if(rebirthAction&&minutes>0){
            long elapsed=(candidateState.day-(long)state.day)*1440+candidateState.minute-state.minute;
            message+=" Elapsed: "+elapsed/1440+" day(s), "+elapsed%1440+" minute(s).";
        }
        if(!candidateTown.player.alive&&!RebirthLife.active(candidateTown)) {
            RebirthLife.prepare(candidateTown,candidateState.day,candidateState.minute);
            message+=" Your previous life ended. Choose a family for your next life.";
        }
        if(RebirthLife.settleLinkedDeath(candidateTown,candidateState.day,candidateState.minute))
            message+=" Your life ended. Its history is preserved. Choose a new family.";
        RebirthLife.synchronize(candidateTown);
        db.save(candidateState,events,List.of(),candidateTown,truth,observations,interactions);
        state=candidateState;
        town=candidateTown;
        return new ActionView(message,snapshot());
    }

    @Override
    @PreDestroy
    public synchronized void close() throws SQLException {
        db.close();
    }
}
