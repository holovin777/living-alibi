package com.holovin.npclife;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

/** Persistent family selection and linked newborn preview; adult controls remain locked. */
final class RebirthLife {
    private RebirthLife(){}
    static final String PHASE="rebirth:phase", CHILD="rebirth:child";
    static final int ADULT=0, CHOICE=1, CHILDHOOD=2, LINKED_ADULT=3;
    static void ensure(TownLife.Town t){
        t.industryStocks.putIfAbsent(PHASE,ADULT);t.industryStocks.putIfAbsent(CHILD,0);
        t.industryStocks.putIfAbsent(FOCUS,0);t.industryStocks.putIfAbsent(LAST_CHAT,0);
        t.industryStocks.putIfAbsent(TRAVEL,0);
    }
    static int phase(TownLife.Town t){return t.industryStocks.getOrDefault(PHASE,ADULT);}
    static boolean active(TownLife.Town t){return phase(t)!=ADULT;}
    static TownLife.Citizen child(TownLife.Town t){return DemographyLife.byIndex(t,t.industryStocks.getOrDefault(CHILD,0));}
    static void prepare(TownLife.Town t,int day,int minute){
        if(active(t))return;
        PlayerLife.beginNewLife(t,day,minute);
        t.player.alive=false;t.player.health=0;t.player.money=0;
        t.industryStocks.put(PHASE,CHOICE);t.industryStocks.put(CHILD,0);
    }
    record Family(int first,int second,String firstName,String secondName,String firstJob,String secondJob,String district){}
    static List<Family> families(TownLife.Town t,int day){
        var result=new ArrayList<Family>();
        for(var a:t.people.values())if(DemographyLife.parentAge(a)){
            var b=DemographyLife.byIndex(t,DemographyLife.get(t,a,"partner"));
            if(b!=null&&a.generationIndex<b.generationIndex&&DemographyLife.canBirth(t,a,b,day))
                result.add(new Family(a.generationIndex,b.generationIndex,a.name,b.name,a.occupation,b.occupation,a.home.label));
        }
        return List.copyOf(result);
    }
    static String choose(TownLife.Town t,int day,int first,int second){
        if(phase(t)!=CHOICE)throw new IllegalArgumentException("No family selection is pending.");
        if(families(t,day).stream().noneMatch(f->f.first()==first&&f.second()==second))
            throw new IllegalArgumentException("This family is not currently eligible.");
        var a=DemographyLife.byIndex(t,first);var b=DemographyLife.byIndex(t,second);
        var newborn=DemographyLife.birth(t,a,b,day);
        if(newborn==null)throw new IllegalArgumentException("This family is no longer available.");
        t.industryStocks.put(CHILD,newborn.generationIndex);t.industryStocks.put(PHASE,CHILDHOOD);
        DemographyLife.set(t,newborn,"played-incarnation",t.player.incarnation);
        t.industryStocks.put(FOCUS,0);t.industryStocks.put(LAST_CHAT,0);t.industryStocks.put(TRAVEL,0);
        synchronize(t);
        return "Born as "+newborn.name+". Follow family life and discover new choices as you grow.";
    }
    static void synchronize(TownLife.Town t){
        if(phase(t)!=CHILDHOOD&&phase(t)!=LINKED_ADULT)return;
        var c=child(t);
        if(c==null||!c.alive)return; // Death settlement needs the authoritative clock.

        // The child is the only living identity; the old visitor shell cannot
        // earn, shop, travel independently, vote or consume duplicate supplies.
        t.player.place=c.place;t.player.destination=c.place;
        t.player.health=c.health;t.player.hunger=c.hunger;t.player.thirst=c.thirst;t.player.fatigue=c.fatigue;
    }
    /** Bind old saves to the existing citizen; never create a replacement person. */
    static boolean restoreIdentity(TownLife.Town t){
        if(phase(t)!=CHILDHOOD&&phase(t)!=LINKED_ADULT)return false;
        var c=child(t);
        if(c==null)throw new IllegalStateException("Linked resident is missing from the save.");
        int previous=DemographyLife.get(t,c,"played-incarnation");
        if(previous==t.player.incarnation)return false;
        if(previous!=0)throw new IllegalStateException("Linked resident has a different incarnation.");
        DemographyLife.set(t,c,"played-incarnation",t.player.incarnation);
        return true;
    }
    /** Settle a real resident, not the inactive visitor shell, exactly once. */
    static boolean settleLinkedDeath(TownLife.Town t,int day,int minute){
        if(phase(t)!=CHILDHOOD&&phase(t)!=LINKED_ADULT)return false;
        restoreIdentity(t);
        var c=child(t);
        if(c.alive)return false;
        int incarnation=t.player.incarnation;
        if(incarnation==Integer.MAX_VALUE)throw new IllegalStateException("Maximum incarnation reached.");
        if(t.pastLives.containsKey(incarnation))throw new IllegalStateException("Life was already archived.");
        if(DemographyLife.get(t,c,"estate")==0){
            ArchiveLife.record(t,"death:"+c.id,day,minute,"Population",c.name+" was recorded as deceased.");
            DemographyLife.estate(t,c);
        }
        t.pastLives.put(incarnation,new PlayerLife.LifeRecord(incarnation,day,minute,c.place,c.name));
        // Legacy assets were already settled when this family life began.
        // Do not run visitor cleanup again or distribute the resident's wallet twice.
        var next=new PlayerLife.Player();
        next.incarnation=incarnation+1;next.alive=false;next.health=0;next.money=0;
        next.place=c.place;next.destination=c.place;
        t.player=next;
        t.industryStocks.put(CHILD,0);t.industryStocks.put(PHASE,CHOICE);
        t.industryStocks.put(FOCUS,0);t.industryStocks.put(LAST_CHAT,0);t.industryStocks.put(TRAVEL,0);
        return true;
    }
    record PastLife(int incarnation,String name,String residentId,int day,int minute,int age,String district){}
    static List<PastLife> history(TownLife.Town t){
        var result=new ArrayList<PastLife>();
        for(var record:t.pastLives.values()){
            var person=t.people.values().stream().filter(c->DemographyLife.get(t,c,"played-incarnation")==record.incarnation()).findFirst().orElse(null);
            result.add(new PastLife(record.incarnation(),record.privateName(),person==null?null:person.id,
                    record.day(),record.minute(),person==null?-1:person.age,record.place().label));
        }
        result.sort(java.util.Comparator.comparingInt(PastLife::incarnation).reversed());
        return List.copyOf(result);
    }
    private static final String FOCUS="rebirth:focus", LAST_CHAT="rebirth:last-chat", TRAVEL="rebirth:travel";
    static boolean linkedAdult(TownLife.Town t,TownLife.Citizen c){
        return c!=null&&phase(t)==LINKED_ADULT&&c==child(t)&&c.alive&&c.age>=18;
    }
    static TownLife.Citizen requireAdult(TownLife.Town t){
        var c=child(t);
        if(!linkedAdult(t,c))throw new IllegalArgumentException("Continue after graduation first.");
        return c;
    }
    static String continueAdult(TownLife.Town t){
        var c=child(t);
        if(phase(t)!=CHILDHOOD||c==null||!c.alive||c.age<18||EducationLife.get(t,c,"graduate")==0)
            throw new IllegalArgumentException("Adulthood requires the existing graduate.");
        t.industryStocks.put(PHASE,LINKED_ADULT);t.industryStocks.put(TRAVEL,0);
        return "Continue as "+c.name+". Your savings, education and relationships remain yours.";
    }
    static TownLife.Place manualDestination(TownLife.Town t,TownLife.Citizen c){
        if(!linkedAdult(t,c))return null;
        int n=t.industryStocks.getOrDefault(TRAVEL,0);
        if(n==HOME_DESTINATION)return c.home;
        var places=PlayerLife.publicPlaces();
        return n>=1&&n<=places.size()?places.get(n-1):null;
    }
    static int travel(TownLife.Town t,TownLife.Place destination,int day){
        var c=requireAdult(t);
        if(c.travelRemaining>0||(EmploymentLife.sick(c,day)&&destination!=TownLife.Place.CLINIC))throw new IllegalArgumentException("Finish traveling or visit the clinic while ill.");
        int index=PlayerLife.publicPlaces().indexOf(destination);
        if(index<0)throw new IllegalArgumentException("Choose a public destination.");
        t.industryStocks.put(TRAVEL,index+1);
        if(c.place==destination){if(destination==c.home)c.activity="OUTSIDE_HOME";return 0;}
        c.destination=destination;c.travelRemaining=TownLife.travelTime(c.place,destination);
        c.activity="TRAVELING";
        return c.travelRemaining;
    }
    // Same public grid as Godot TownMap, validated independently of visual input.
    private static final List<List<String>> ROAD_GRID=List.of(
            List.of("NORTH_RESIDENCES","CLINIC","SCHOOL","OFFICE","FACTORY"),
            List.of("MARKET","CENTRO","PARK","LIBRARY","WORKSHOP"),
            List.of("SOUTH_RESIDENCES","PORTICI","RIVER","WOODS","QUARRY"),
            List.of("CAFE","BAKERY","FARM","LOGISTICS_DEPOT","FUEL_STATION"));
    static boolean adjacent(TownLife.Place a,TownLife.Place b){
        int ax=-1,ay=-1,bx=-1,by=-1;
        for(int y=0;y<ROAD_GRID.size();y++){
            int x=ROAD_GRID.get(y).indexOf(a.name());if(x>=0){ax=x;ay=y;}
            x=ROAD_GRID.get(y).indexOf(b.name());if(x>=0){bx=x;by=y;}
        }
        return ax>=0&&bx>=0&&Math.abs(ax-bx)+Math.abs(ay-by)==1;
    }
    static int roadTravel(TownLife.Town t,TownLife.Place destination,int day){
        var c=requireAdult(t);
        if(atHome(c)||!adjacent(c.place,destination))throw new IllegalArgumentException("Leave your home and follow an adjacent road.");
        return travel(t,destination,day);
    }
    static boolean outsideHome(TownLife.Town t,TownLife.Citizen c){
        return linkedAdult(t,c)&&c.place==c.home&&manualDestination(t,c)==c.home&&!stayingHome(t,c);
    }
    static String leaveHome(TownLife.Town t,int day){
        var c=requireAdult(t);
        if(!atHome(c)||EmploymentLife.sick(c,day))throw new IllegalArgumentException("You must be awake at home and well enough to leave.");
        travel(t,c.home,day);
        return "Left the building for the public street.";
    }
    private static final int HOME_DESTINATION=1000;
    static boolean stayingHome(TownLife.Town t,TownLife.Citizen c){
        return linkedAdult(t,c)&&t.industryStocks.getOrDefault(TRAVEL,0)==HOME_DESTINATION;
    }
    static int goHome(TownLife.Town t){
        var c=requireAdult(t);
        if(c.travelRemaining>0)throw new IllegalArgumentException("Finish traveling first.");
        t.industryStocks.put(TRAVEL,HOME_DESTINATION);
        int duration=TownLife.travelTime(c.place,c.home);
        if(duration>0){c.destination=c.home;c.travelRemaining=duration;c.activity="TRAVELING";}
        else c.activity="AT_HOME";
        return duration;
    }
    static boolean atHome(TownLife.Citizen c){
        return awakePresent(c)&&c.place==c.home&&c.currentSpace().equals(c.homeUnit);
    }
    static List<TownLife.Citizen> homeFamily(TownLife.Town t,TownLife.Citizen c){
        if(!atHome(c))return List.of();
        int a=DemographyLife.get(t,c,"parent-a"),b=DemographyLife.get(t,c,"parent-b"),p=DemographyLife.get(t,c,"partner");
        return t.people.values().stream().filter(x->x!=c&&x.generationIndex>0&&atHome(x)&&together(c,x)
                &&(x.generationIndex==a||x.generationIndex==b||x.generationIndex==p
                ||DemographyLife.get(t,x,"parent-a")==c.generationIndex||DemographyLife.get(t,x,"parent-b")==c.generationIndex)).toList();
    }
    static int hygiene(TownLife.Town t,TownLife.Citizen c){
        return 100-Math.min(100,Math.max(0,EducationLife.get(t,c,"dirt")));
    }
    static int furnitureLevel(TownLife.Town t,TownLife.Citizen c){
        return Math.max(0,Math.min(2,EducationLife.get(t,c,"furniture-level")));
    }
    static int furniturePrice(int level){return level==0?80:level==1?180:0;}
    record HomeOptions(String district,boolean atHome,boolean stayingHome,int food,int water,List<String> family,List<String> actions,int hygiene,int furnitureLevel,int upgradePrice,boolean outsideHome,List<HomeMarketLife.Offer> properties,String interiorStyle,List<InteriorLife.Offer> furnishings,DomesticLife.View domestic,WardrobeLife.View wardrobe){} 
    static HomeOptions homeOptions(TownLife.Town t,int day){return homeOptions(t,day,-1);}
    static HomeOptions homeOptions(TownLife.Town t,int day,int minute){
        var c=child(t);
        if(!linkedAdult(t,c))return null;
        var family=homeFamily(t,c);
        var actions=new ArrayList<String>();
        if(c.travelRemaining==0)actions.add("go");
        if(atHome(c)){
            DomesticLife.actions(t,c,actions);
            if(c.food>0&&c.hunger>0)actions.add("meal");
            if(c.water>0&&c.thirst>0)actions.add("drink");
            if(c.fatigue>0)actions.add("rest");
            if(hygiene(t,c)<100&&c.water>0)actions.add("wash");
            int price=furniturePrice(furnitureLevel(t,c));
            if(price>0&&c.money>=price+50)actions.add("upgrade");
            if(!family.isEmpty()&&EducationLife.get(t,c,"last-family-time")<day)actions.add("family");
        }
        return new HomeOptions(c.home.label,atHome(c),stayingHome(t,c),c.food,c.water,
                family.stream().map(p->p.name).toList(),List.copyOf(actions),hygiene(t,c),furnitureLevel(t,c),furniturePrice(furnitureLevel(t,c)),outsideHome(t,c),HomeMarketLife.offers(t,minute),InteriorLife.style(c),InteriorLife.offers(t,c),DomesticLife.view(t,c),WardrobeLife.view(t,c,day,minute));
    }
    record HomeAction(int minutes,String message){}
    static HomeAction homeAction(TownLife.Town t,int day,String action){
        var c=requireAdult(t);
        if(action.startsWith("furnish-"))return InteriorLife.purchase(t,c,action.substring("furnish-".length()));
        if(!homeOptions(t,day).actions().contains(action)||"go".equals(action))
            throw new IllegalArgumentException("This home activity is not available.");
        t.industryStocks.put(TRAVEL,HOME_DESTINATION);
        if(List.of("microwave","clean","laundry").contains(action))return DomesticLife.act(t,c,action);
        return switch(action){
            case "meal" -> {c.food--;c.hunger=Math.max(0,c.hunger-50);yield new HomeAction(15,"Prepared a meal from your food supplies.");}
            case "drink" -> {c.water--;c.thirst=Math.max(0,c.thirst-52);yield new HomeAction(5,"Drank water from your supplies.");}
            case "rest" -> {c.fatigue=Math.max(0,c.fatigue-20-5*furnitureLevel(t,c));yield new HomeAction(60,"Rested at home.");}
            case "wash" -> {c.water--;EducationLife.set(t,c,"dirt",0);yield new HomeAction(10,"Washed using one unit of water.");}
            case "upgrade" -> {
                int level=furnitureLevel(t,c),price=furniturePrice(level);
                int shop=Math.addExact(t.employerFunds.getOrDefault("shop-assistant",0),price);
                c.money-=price;t.employerFunds.put("shop-assistant",shop);
                EducationLife.set(t,c,"furniture-level",level+1);
                yield new HomeAction(30,"Purchased home furnishings for EUR "+price+".");
            }
            case "family" -> {
                for(var relative:homeFamily(t,c)){
                    var relation=t.peerRelationships.computeIfAbsent(SocialNetwork.key(c.id,relative.id),
                            k->new SocialLife.Relationship(c.id.compareTo(relative.id)<=0?c.id:relative.id,c.id.compareTo(relative.id)<=0?relative.id:c.id));
                    relation.affinity=Math.min(100,relation.affinity+2);
                    relation.trust=Math.min(100,relation.trust+1);
                    relation.conversations++;relation.encounters++;
                }
                EducationLife.set(t,c,"last-family-time",day);
                yield new HomeAction(30,"Spent time with family at home.");
            }
            default -> throw new IllegalArgumentException("Unknown home activity.");
        };
    }
    static void holdPublicPlace(TownLife.Town t,TownLife.Citizen c){
        t.industryStocks.put(TRAVEL,PlayerLife.publicPlaces().indexOf(c.place)+1);
    }
    static String resumeRoutine(TownLife.Town t){
        requireAdult(t);
        t.industryStocks.put(TRAVEL,0);
        return "Resumed daily work, shopping and home routine.";
    }
    static boolean continueAdultWaiting(TownLife.Town t){
        return linkedAdult(t,child(t));
    }
    record AdultOptions(String occupation,boolean employed,String activity,String destination,List<String> jobs,List<String> destinations){}
    static AdultOptions adultOptions(TownLife.Town t,int day){
        var c=child(t);
        if(!linkedAdult(t,c))return null;
        var destination=manualDestination(t,c);
        return new AdultOptions(c.occupation,c.employed,c.activity,destination==null?null:destination.name(),
                EmploymentLife.openRoles(t,c,day),c.travelRemaining==0&&!EmploymentLife.sick(c,day)?PlayerLife.publicPlaces().stream().map(Enum::name).toList():List.of());
    }
    static boolean linkedChild(TownLife.Town t,TownLife.Citizen c){
        return c!=null&&phase(t)==CHILDHOOD&&c==child(t)&&c.alive&&c.age<18;
    }
    // A voluntary afternoon outing never replaces school, sleep or care at home.
    static int outingEnd(TownLife.Citizen c){return c.age<13?1080:1200;}
    static boolean outingHours(TownLife.Citizen c,int minute){return minute>=780&&minute<outingEnd(c);}
    static List<TownLife.Place> childDestinations(TownLife.Citizen c){
        if(c.age<6)return List.of();
        if(c.age<13)return List.of(TownLife.Place.PARK,TownLife.Place.LIBRARY);
        return List.of(TownLife.Place.PARK,TownLife.Place.LIBRARY,TownLife.Place.CENTRO,TownLife.Place.PORTICI,TownLife.Place.MARKET);
    }
    static TownLife.Place childOuting(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(!linkedChild(t,c))return null;
        int n=t.industryStocks.getOrDefault(TRAVEL,0);
        var places=PlayerLife.publicPlaces();
        if(n<=0||n>places.size())return null;
        var destination=places.get(n-1);
        if(!outingHours(c,minute)||EmploymentLife.sick(c,day)||!childDestinations(c).contains(destination)){
            t.industryStocks.put(TRAVEL,0);
            return null;
        }
        return destination;
    }
    static int childTravel(TownLife.Town t,TownLife.Place destination,int day,int minute){
        var c=child(t);
        if(!linkedChild(t,c)||!awakePresent(c)||EmploymentLife.sick(c,day)||!outingHours(c,minute)
                ||!childDestinations(c).contains(destination))
            throw new IllegalArgumentException("This outing is not available at your age, time or condition.");
        int duration=TownLife.travelTime(c.place,destination);
        if(minute+duration>=outingEnd(c))throw new IllegalArgumentException("It is time to return home.");
        t.industryStocks.put(TRAVEL,PlayerLife.publicPlaces().indexOf(destination)+1);
        if(duration>0){c.destination=destination;c.travelRemaining=duration;c.activity="TRAVELING";}
        return duration;
    }
    static int childReturn(TownLife.Town t){
        var c=child(t);
        if(!linkedChild(t,c)||c.age<6||c.travelRemaining>0||t.industryStocks.getOrDefault(TRAVEL,0)==0)
            throw new IllegalArgumentException("No active outing to end.");
        t.industryStocks.put(TRAVEL,0);
        int duration=TownLife.travelTime(c.place,c.home);
        if(duration>0){c.destination=c.home;c.travelRemaining=duration;c.activity="TRAVELING";}
        return duration;
    }
    record Hobby(String id,String label,int minutes){}
    static List<Hobby> hobbies(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(!linkedChild(t,c)||c.age<6||!awakePresent(c)||EmploymentLife.sick(c,day)
                ||!outingHours(c,minute)||minute+30>=outingEnd(c)||c.fatigue>=85
                ||c.hunger>=85||c.thirst>=85||EducationLife.get(t,c,"last-hobby")>=day)return List.of();
        var result=new ArrayList<Hobby>();
        if(c.place==c.home&&t.industryStocks.getOrDefault(TRAVEL,0)==0){
            result.add(new Hobby("read","Read at home",30));
            result.add(new Hobby("build","Build and tinker at home",30));
        }
        if(childOutingAt(t,c,day,minute,TownLife.Place.LIBRARY))
            result.add(new Hobby("read","Read at the library",30));
        if(childOutingAt(t,c,day,minute,TownLife.Place.PARK))
            result.add(new Hobby("sport","Exercise at the park",30));
        return List.copyOf(result);
    }
    // Read-only check for reports; unlike childOuting(), never clears stored state.
    private static boolean childOutingAt(TownLife.Town t,TownLife.Citizen c,int day,int minute,TownLife.Place place){
        return c.place==place&&outingHours(c,minute)&&!EmploymentLife.sick(c,day)
                &&t.industryStocks.getOrDefault(TRAVEL,0)==PlayerLife.publicPlaces().indexOf(place)+1;
    }
    static String hobby(TownLife.Town t,int day,int minute,String id){
        var c=child(t);
        var option=hobbies(t,c,day,minute).stream().filter(h->h.id().equals(id)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("This hobby is not available now."));
        String skill=switch(id){case "read" -> "academic";case "build" -> "technical";case "sport" -> "fitness";default -> throw new IllegalArgumentException("Unknown hobby.");};
        EducationLife.learn(t,c,skill,1);
        if("read".equals(id))c.curiosity=Math.min(100,c.curiosity+1);
        if("build".equals(id))c.carefulness=Math.min(100,c.carefulness+1);
        c.fatigue=Math.min(100,c.fatigue+("sport".equals(id)?8:3));
        EducationLife.set(t,c,"last-hobby",day);
        EducationLife.set(t,c,"hobby-days",Math.addExact(EducationLife.get(t,c,"hobby-days"),1));
        return option.label()+". Practiced "+skill+" for 30 minutes.";
    }
    static int allowanceAmount(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(!linkedChild(t,c)||c.age<6||!awakePresent(c)||c.place!=c.home
                ||!outingHours(c,minute)||EmploymentLife.sick(c,day)
                ||EducationLife.get(t,c,"allowance-week")>=1+(day-1)/7)return 0;
        int amount=c.age<13?3:5;
        for(String f:List.of("parent-a","parent-b")){
            var parent=DemographyLife.byIndex(t,DemographyLife.get(t,c,f));
            if(awakePresent(parent)&&parent.currentSpace().equals(c.currentSpace())&&parent.money>=80+amount)return amount;
        }
        return 0;
    }
    static String allowance(TownLife.Town t,int day,int minute){
        var c=child(t);int amount=allowanceAmount(t,c,day,minute);
        if(amount==0)throw new IllegalArgumentException("No affordable weekly allowance from a parent at home.");
        for(String f:List.of("parent-a","parent-b")){
            var parent=DemographyLife.byIndex(t,DemographyLife.get(t,c,f));
            if(!awakePresent(parent)||!parent.currentSpace().equals(c.currentSpace())||parent.money<80+amount)continue;
            int wallet=Math.addExact(c.money,amount);
            int total=Math.addExact(EducationLife.get(t,c,"allowance-total"),amount);
            parent.money-=amount;c.money=wallet;
            EducationLife.set(t,c,"allowance-week",1+(day-1)/7);
            EducationLife.set(t,c,"allowance-total",total);
            return "Received EUR "+amount+" pocket money from "+parent.name+".";
        }
        throw new IllegalStateException("Allowance parent no longer available.");
    }
    record ChildOptions(List<String> destinations,boolean outing,int returnMinute,String activity,List<Hobby> hobbies,int allowanceAvailable){}

    static ChildOptions childOptions(TownLife.Town t,int day,int minute){
        var c=child(t);
        if(!linkedChild(t,c)||c.age<6)return null;
        var destinations=awakePresent(c)&&!EmploymentLife.sick(c,day)&&outingHours(c,minute)
                ?childDestinations(c).stream().filter(p->minute+TownLife.travelTime(c.place,p)<outingEnd(c)).map(Enum::name).toList():List.<String>of();
        boolean outing=t.industryStocks.getOrDefault(TRAVEL,0)>0&&outingHours(c,minute)&&!EmploymentLife.sick(c,day);
        return new ChildOptions(destinations,outing,outingEnd(c),c.activity,hobbies(t,c,day,minute),allowanceAmount(t,c,day,minute));
    }
    static String focus(TownLife.Town t,TownLife.Citizen c){
        if(!linkedChild(t,c)||c.age<6)return null;
        return storedFocus(t);
    }
    private static String storedFocus(TownLife.Town t){
        int n=t.industryStocks.getOrDefault(FOCUS,0);
        return n>=1&&n<=EducationLife.SUBJECTS.size()?EducationLife.SUBJECTS.get(n-1):null;
    }
    static String chooseFocus(TownLife.Town t,String subject){
        var c=child(t);
        if(c==null||!linkedChild(t,c)||c.age<6)
            throw new IllegalArgumentException("School focus is available from age six until adulthood.");
        int n=EducationLife.SUBJECTS.indexOf(subject);
        if(n<0&&!"balanced".equals(subject))throw new IllegalArgumentException("Unknown school subject.");
        t.industryStocks.put(FOCUS,n+1);
        return "School focus: "+subject+". Skills grow only during actual lessons.";
    }
    static boolean awakePresent(TownLife.Citizen c){
        return c!=null&&c.alive&&c.travelRemaining==0&&!"SLEEPING".equals(c.activity);
    }
    static List<TownLife.Citizen> visible(TownLife.Town t){
        var c=child(t);
        if(c==null||(!linkedChild(t,c)&&!linkedAdult(t,c))||c.travelRemaining>0)return List.of();
        return t.people.values().stream().filter(p->p!=c&&p.alive&&p.travelRemaining==0
                &&p.currentSpace().equals(c.currentSpace())).toList();
    }
    static List<TownLife.Citizen> conversationPartners(TownLife.Town t,int day){
        var c=child(t);
        if(c==null||!awakePresent(c)||c.age<3||t.industryStocks.getOrDefault(LAST_CHAT,0)>=day)return List.of();
        return visible(t).stream().filter(p->awakePresent(p)&&
                (p.generationIndex>0&&(p.generationIndex==DemographyLife.get(t,c,"parent-a")
                ||p.generationIndex==DemographyLife.get(t,c,"parent-b"))
                ||(c.age>=18?p.age>=18:(p.age<18&&Math.abs(p.age-c.age)<=3)))).toList();
    }
    static String chat(TownLife.Town t,int day,String id){
        var c=child(t);
        var other=conversationPartners(t,day).stream().filter(p->p.id.equals(id)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("No available childhood conversation with this person today."));
        String key=SocialNetwork.key(c.id,other.id);
        var relation=t.peerRelationships.computeIfAbsent(key,k->new SocialLife.Relationship(
                c.id.compareTo(other.id)<=0?c.id:other.id,c.id.compareTo(other.id)<=0?other.id:c.id));
        // Very poor personal cleanliness may make an ordinary adult conversation less comfortable.
        boolean untidy=linkedAdult(t,c)&&(DomesticLife.clothes(t,c)<30||hygiene(t,c)<30);
        relation.affinity=Math.min(100,relation.affinity+(untidy?1:2));
        relation.trust=Math.min(100,relation.trust+1);
        relation.conversations++;
        c.sociability=Math.min(100,c.sociability+1);
        t.industryStocks.put(LAST_CHAT,day);
        return "Spent time talking with "+other.name+".";
    }
    record Conversation(String id,String name){}
    // Bound each request: early childhood is faster, adolescence is slower.
    static int maxWaitDays(TownLife.Town t){
        if(phase(t)==CHOICE)return 1;
        var c=child(t);
        if(phase(t)!=CHILDHOOD||c==null||!c.alive||c.age>=18)return 0;
        return c.age<6?7:c.age<13?3:1;
    }
    static boolean canContinueWaiting(TownLife.Town t,int initialPhase,int initialMaxDays){
        return phase(t)==initialPhase&&maxWaitDays(t)>0&&maxWaitDays(t)==initialMaxDays;
    }
    static String adultFallback(TownLife.Town t){
        if(phase(t)!=CHOICE)throw new IllegalArgumentException("Only available before choosing a family.");
        var p=new PlayerLife.Player();p.incarnation=t.player.incarnation;t.player=p;
        t.industryStocks.put(PHASE,ADULT);t.industryStocks.put(CHILD,0);
        return "Started an adult life in the park.";
    }
    record HouseholdMember(String id,String name,int age,boolean alive,String role,int savings){}
    record FamilyOptions(List<Conversation> proposals,HouseholdMember partner,List<HouseholdMember> children,
                         boolean livingTogether,boolean canMoveTogether,boolean canHaveChild,boolean canSupport,int supportAmount,FamilyBudgetLife.View budget){}
    static List<Conversation> proposals(TownLife.Town t,TownLife.Citizen c){
        if(!linkedAdult(t,c)||!awakePresent(c))return List.of();
        return visible(t).stream().filter(p->awakePresent(p)&&DemographyLife.canPair(t,c,p))
                .map(p->new Conversation(p.id,p.name)).toList();
    }
    static TownLife.Citizen partner(TownLife.Town t,TownLife.Citizen c){
        var p=DemographyLife.byIndex(t,DemographyLife.get(t,c,"partner"));
        return p!=null&&p.alive&&DemographyLife.get(t,p,"partner")==c.generationIndex?p:null;
    }
    static boolean together(TownLife.Citizen a,TownLife.Citizen b){
        return a.home==b.home&&a.homeUnit.equals(b.homeUnit);
    }
    static boolean meeting(TownLife.Citizen a,TownLife.Citizen b){
        return awakePresent(a)&&awakePresent(b)&&a.currentSpace().equals(b.currentSpace());
    }
    static boolean willing(TownLife.Town t,TownLife.Citizen c,TownLife.Citizen p){
        if(p==null)return false;
        var r=t.peerRelationships.get(SocialNetwork.key(c.id,p.id));
        return r!=null&&r.affinity>=65&&r.trust>=60&&r.tension<=25;
    }
    static boolean canMoveTogether(TownLife.Town t,TownLife.Citizen c,TownLife.Citizen p){
        return p!=null&&!together(c,p)&&meeting(c,p)&&willing(t,c,p)
                &&TownLife.privateUnit(c.id).equals(c.homeUnit)&&TownLife.privateUnit(p.id).equals(p.homeUnit);
    }
    static boolean canHaveChild(TownLife.Town t,TownLife.Citizen c,TownLife.Citizen p,int day){
        return p!=null&&together(c,p)&&meeting(c,p)&&willing(t,c,p)&&DemographyLife.canBirth(t,c,p,day);
    }
    static HouseholdMember householdMember(TownLife.Citizen c,String role){
        return new HouseholdMember(c.id,c.name,c.age,c.alive,role,c.money);
    }
    static FamilyOptions familyOptions(TownLife.Town t,int day){
        var c=child(t);
        if(!linkedAdult(t,c))return null;
        var p=partner(t,c);
        var children=t.people.values().stream().filter(x->DemographyLife.get(t,x,"parent-a")==c.generationIndex
                ||DemographyLife.get(t,x,"parent-b")==c.generationIndex).map(x->householdMember(x,"Child")).toList();
        return new FamilyOptions(proposals(t,c),p==null?null:householdMember(p,"Partner"),children,
                p!=null&&together(c,p),canMoveTogether(t,c,p),canHaveChild(t,c,p,day),
                p!=null&&meeting(c,p)&&c.money>=60,10,FamilyBudgetLife.view(t,c,p));
    }
    static String propose(TownLife.Town t,String id,int day,int minute){
        var c=requireAdult(t);
        if(proposals(t,c).stream().noneMatch(p->p.id().equals(id)))throw new IllegalArgumentException("This resident is not ready for a partnership.");
        var p=t.people.get(id);
        if(!DemographyLife.pair(t,c,p,day))throw new IllegalArgumentException("Partnership unavailable.");
        ArchiveLife.record(t,"union:"+c.id+":"+p.id+":"+day,day,minute,"Population",c.name+" and "+p.name+" formed a household partnership.");
        return "You and "+p.name+" agreed to become partners.";
    }
    static String moveTogether(TownLife.Town t){
        var c=requireAdult(t);var p=partner(t,c);
        if(!canMoveTogether(t,c,p))throw new IllegalArgumentException("Both partners must meet and have compatible private homes.");
        RentalLife.moveHome(t,p,c.home,c.homeUnit);
        return "You and "+p.name+" now share a home.";
    }
    static String haveChild(TownLife.Town t,int day){
        var c=requireAdult(t);var p=partner(t,c);
        if(!canHaveChild(t,c,p,day))throw new IllegalArgumentException("Family birth requirements are not met.");
        var newborn=DemographyLife.birth(t,c,p,day);
        if(newborn==null)throw new IllegalArgumentException("Birth unavailable.");
        return "Welcome "+newborn.name+" to your family.";
    }
    static String supportPartner(TownLife.Town t){
        var c=requireAdult(t);var p=partner(t,c);
        if(p==null||!meeting(c,p)||c.money<60)throw new IllegalArgumentException("Meet your partner and retain EUR 50 in savings.");
        int wallet=Math.addExact(p.money,10);
        c.money-=10;p.money=wallet;
        return "Transferred EUR 10 to "+p.name+".";
    }
    record KnownPerson(String id,String name,boolean family,boolean alive,int affinity,int trust,int conversations,String bond){}
    private static String bond(boolean family,SocialLife.Relationship r){
        if(family)return "Family";
        if(r==null)return "Stranger";
        if(r.tension>=58||r.affinity<27)return "Strained";
        if(r.affinity>=82&&r.trust>=75&&r.encounters>=5)return "Close friend";
        if(r.affinity>=65&&r.trust>=58&&r.encounters>=3)return "Friend";
        return "Acquaintance";
    }
    static final List<String> AMBITIONS=List.of("LEARN_SKILLS","MAKE_FRIENDS","SAVE_MONEY","STAY_HEALTHY","FIND_BALANCE");
    static boolean canChooseAmbition(TownLife.Town t,TownLife.Citizen c){
        return linkedAdult(t,c)||(linkedChild(t,c)&&c.age>=13);
    }
    static String chooseAmbition(TownLife.Town t,String goal){
        var c=child(t);
        if(!canChooseAmbition(t,c)||!AMBITIONS.contains(goal))
            throw new IllegalArgumentException("Choose a valid ambition from age thirteen.");
        c.goal=goal;
        return "Life ambition: "+goal.replace('_',' ').toLowerCase(java.util.Locale.ROOT)+".";
    }
    record Ambition(String id,String description,int progress,int target,String unit){}
    record Personality(int sociability,int carefulness,int curiosity,Ambition ambition,List<String> choices){}
    static Personality personality(TownLife.Town t,TownLife.Citizen c,List<KnownPerson> known){
        if(c==null)return null;
        int best=EducationLife.SUBJECTS.stream().mapToInt(s->EducationLife.get(t,c,s)).max().orElse(0);
        int friends=(int)known.stream().filter(p->p.alive()&&("Friend".equals(p.bond())||"Close friend".equals(p.bond()))).count();
        var ambition=switch(c.goal){
            case "LEARN_SKILLS" -> new Ambition(c.goal,"Develop a strong skill",best,60,"skill points");
            case "MAKE_FRIENDS" -> new Ambition(c.goal,"Build lasting friendships",friends,3,"friends");
            case "SAVE_MONEY" -> new Ambition(c.goal,"Build personal savings",c.money,500,"EUR");
            case "STAY_HEALTHY" -> new Ambition(c.goal,"Improve physical fitness",EducationLife.get(t,c,"fitness"),50,"fitness points");
            case "FIND_BALANCE" -> new Ambition(c.goal,"Develop varied abilities",EducationLife.SUBJECTS.stream().mapToInt(s->EducationLife.get(t,c,s)).min().orElse(0),25,"minimum skill");
            default -> null;
        };
        return new Personality(c.sociability,c.carefulness,c.curiosity,ambition,canChooseAmbition(t,c)?AMBITIONS:List.of());
    }
    record Identity(String residentId,int generationIndex,int bornDay,int savings,boolean graduated,List<KnownPerson> relationships,int fitness,int hobbyDays,int allowanceTotal,Personality personality,BirthdayLife.Memory birthday){}
    static Identity identity(TownLife.Town t,TownLife.Citizen c,int day){
        if(c==null)return null;
        int a=DemographyLife.get(t,c,"parent-a"),b=DemographyLife.get(t,c,"parent-b");
        var known=new ArrayList<KnownPerson>();
        for(var p:t.people.values()){
            if(p==c)continue;
            boolean family=p.generationIndex>0&&(p.generationIndex==a||p.generationIndex==b
                    ||p.generationIndex==DemographyLife.get(t,c,"partner")
                    ||DemographyLife.get(t,p,"parent-a")==c.generationIndex||DemographyLife.get(t,p,"parent-b")==c.generationIndex);
            var relation=t.peerRelationships.get(SocialNetwork.key(c.id,p.id));
            if(!family&&(relation==null||relation.conversations==0))continue;
            known.add(new KnownPerson(p.id,p.name,family,p.alive,relation==null?0:relation.affinity,
                    relation==null?0:relation.trust,relation==null?0:relation.conversations,bond(family,relation)));
        }
        known.sort(java.util.Comparator.comparing(KnownPerson::family).reversed()
                .thenComparing(KnownPerson::name).thenComparing(KnownPerson::id));
        return new Identity(c.id,c.generationIndex,DemographyLife.get(t,c,"born"),c.money,
                EducationLife.get(t,c,"graduate")>0,List.copyOf(known),EducationLife.get(t,c,"fitness"),EducationLife.get(t,c,"hobby-days"),EducationLife.get(t,c,"allowance-total"),personality(t,c,known));
    }
    // Linked residents shop with their actual wallet and the same finite retail stock
    // as first-life visitors. Food is represented by the existing resident portions.
    static final List<String> GROCERY_ITEMS=List.of("bread","water","pasta","vegetables","eggs","milk","rice");
    record GroceryItem(String id,String label,int price,int stock,boolean available){}
    record Grocery(String name,boolean open,int food,int water,int capacity,List<GroceryItem> items,String hours){}
    static Grocery grocery(TownLife.Town t,int day,int minute){
        var c=child(t);
        if(!linkedAdult(t,c)||!awakePresent(c)||atHome(c))return null;
        boolean bakery=c.place==TownLife.Place.BAKERY;
        if(!bakery&&c.place!=TownLife.Place.PORTICI)return null;
        boolean open=minute>=(bakery?420:480)&&minute+5<=(bakery?1080:1200)&&!EmploymentLife.sick(c,day);
        int capacity=24;
        var items=(bakery?List.of("bread","water"):GROCERY_ITEMS).stream().map(id->{
            var product=RetailLife.product(id);
            int stock=t.retailStock.getOrDefault(id,0);
            return new GroceryItem(id,product.label(),product.price(),stock,open&&stock>0&&c.money>=product.price()
                    &&(long)c.food+c.water<capacity);
        }).toList();
        return new Grocery(bakery?"Panetteria del Borgo":"Bottega Alimentare",open,c.food,c.water,capacity,items,bakery?"07:00–18:00":"08:00–20:00");
    }
    static String buyGrocery(TownLife.Town t,int day,int minute,String id){
        var c=requireAdult(t);
        var shop=grocery(t,day,minute);
        if(shop==null||!shop.open())throw new IllegalArgumentException("Visit an open grocery or bakery counter.");
        var item=shop.items().stream().filter(p->p.id().equals(id)&&p.available()).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Product unavailable, insufficient money or carrying capacity."));
        int funds=Math.addExact(t.employerFunds.getOrDefault("shop-assistant",0),item.price());
        t.retailStock.put(id,item.stock()-1);
        c.money-=item.price();t.employerFunds.put("shop-assistant",funds);
        if("water".equals(id))c.water++;else c.food++;
        t.industryStocks.put(TRAVEL,PlayerLife.publicPlaces().indexOf(c.place)+1);
        return "Bought "+item.label()+" for EUR "+item.price()+".";
    }
    record CafeOption(String id,String label,int price,int minutes,boolean available){}
    record Cafe(String name,String hours,boolean open,List<CafeOption> orders,boolean canRest,List<Conversation> guests){}
    static Cafe cafe(TownLife.Town t,int day,int minute){
        var c=child(t);
        if(!linkedAdult(t,c)||c.place!=TownLife.Place.CAFE||!awakePresent(c)||atHome(c))return null;
        boolean open=minute>=420&&minute<1260&&!EmploymentLife.sick(c,day);
        var orders=List.of(
                new CafeOption("espresso","Espresso",1,5,open&&minute+5<=1260&&c.money>=1&&c.fatigue>0),
                new CafeOption("water","Bottled water",1,5,open&&minute+5<=1260&&c.money>=1&&c.thirst>0&&t.retailStock.getOrDefault("water",0)>0),
                new CafeOption("bread","Bread snack",3,5,open&&minute+5<=1260&&c.money>=3&&c.hunger>0&&t.retailStock.getOrDefault("bread",0)>0));
        return new Cafe("Bar Centrale","07:00–21:00",open,orders,
                open&&minute+20<=1260&&c.fatigue>0&&EducationLife.get(t,c,"last-cafe-rest")<day,
                open&&minute+10<=1260?conversationPartners(t,day).stream().map(p->new Conversation(p.id,p.name)).toList():List.of());
    }
    static HomeAction cafeAction(TownLife.Town t,int day,int minute,String action){
        var c=requireAdult(t);var cafe=cafe(t,day,minute);
        if(cafe==null||!cafe.open())throw new IllegalArgumentException("Visit Bar Centrale during opening hours.");
        t.industryStocks.put(TRAVEL,PlayerLife.publicPlaces().indexOf(TownLife.Place.CAFE)+1);
        if("rest".equals(action)){
            if(!cafe.canRest())throw new IllegalArgumentException("Rest is available once a day while the cafe is open.");
            c.fatigue=Math.max(0,c.fatigue-8);
            EducationLife.set(t,c,"last-cafe-rest",day);
            return new HomeAction(20,"Rested at a cafe table.");
        }
        if(action.startsWith("chat-")){
            String id=action.substring(5);
            if(cafe.guests().stream().noneMatch(p->p.id().equals(id)))throw new IllegalArgumentException("This guest is not available.");
            return new HomeAction(10,chat(t,day,id));
        }
        var order=cafe.orders().stream().filter(p->p.id().equals(action)&&p.available()).findFirst()
                .orElseThrow(()->new IllegalArgumentException("This order is not available."));
        int funds=Math.addExact(t.employerFunds.getOrDefault("shop-assistant",0),order.price());
        c.money-=order.price();t.employerFunds.put("shop-assistant",funds);
        switch(action){
            case "espresso" -> c.fatigue=Math.max(0,c.fatigue-5);
            case "water" -> {t.retailStock.merge("water",-1,Integer::sum);c.thirst=Math.max(0,c.thirst-40);}
            case "bread" -> {t.retailStock.merge("bread",-1,Integer::sum);c.hunger=Math.max(0,c.hunger-35);}
            default -> throw new IllegalArgumentException("Unknown order.");
        }
        return new HomeAction(order.minutes(),"Enjoyed "+order.label()+" for EUR "+order.price()+".");
    }
    record Medical(int health,boolean ill,int sickUntilDay,boolean canVisit,boolean atClinic,boolean open,
                   boolean canConsult,boolean canFirstAid,int consultPrice,int firstAidPrice,MedicalFollowupLife.View followup){}
    static Medical medical(TownLife.Town t,int day,int minute){
        var c=child(t);
        if(!linkedAdult(t,c))return null;
        boolean ill=EmploymentLife.sick(c,day),awake=awakePresent(c);
        boolean present=awake&&c.place==TownLife.Place.CLINIC&&!atHome(c);
        boolean open=minute>=480&&minute<1140;
        return new Medical(c.health,ill,c.sickUntilDay,awake&&!present&&open&&(ill||c.health<100),present,open,
                present&&open&&minute+30<=1140&&(ill||c.health<100)&&c.money>=15&&EducationLife.get(t,c,"last-consult")<day,
                present&&open&&minute+10<=1140&&c.health<100&&c.money>=5&&EducationLife.get(t,c,"last-first-aid")<day,15,5,MedicalFollowupLife.view(t,c,day,minute));
    }
    static int medicalVisit(TownLife.Town t,int day,int minute){
        var m=medical(t,day,minute);
        if(m==null||!m.canVisit())throw new IllegalArgumentException("No medical visit available.");
        return travel(t,TownLife.Place.CLINIC,day);
    }
    static HomeAction medicalAction(TownLife.Town t,int day,int minute,String action){
        var c=requireAdult(t);var m=medical(t,day,minute);
        if(List.of("exam","collect","recover").contains(action)){
            var result=MedicalFollowupLife.act(t,c,day,minute,action);
            t.industryStocks.put(TRAVEL,action.equals("recover")?HOME_DESTINATION:PlayerLife.publicPlaces().indexOf(TownLife.Place.CLINIC)+1);
            return result;
        }
        if("consult".equals(action)&&m.canConsult()){
            int funds=Math.addExact(t.employerFunds.getOrDefault("city-maintenance",0),15);
            c.money-=15;t.employerFunds.put("city-maintenance",funds);
            c.health=Math.min(100,c.health+10);
            if(m.ill()&&c.sickUntilDay>day)c.sickUntilDay--;
            EducationLife.set(t,c,"last-consult",day);
            t.industryStocks.put(TRAVEL,PlayerLife.publicPlaces().indexOf(TownLife.Place.CLINIC)+1);
            return new HomeAction(30,"Completed a medical consultation.");
        }
        if("first-aid".equals(action)&&m.canFirstAid()){
            int funds=Math.addExact(t.employerFunds.getOrDefault("shop-assistant",0),5);
            c.money-=5;t.employerFunds.put("shop-assistant",funds);
            c.health=Math.min(100,c.health+4);
            EducationLife.set(t,c,"last-first-aid",day);
            t.industryStocks.put(TRAVEL,PlayerLife.publicPlaces().indexOf(TownLife.Place.CLINIC)+1);
            return new HomeAction(10,"Received basic pharmacy care.");
        }
        throw new IllegalArgumentException("This medical service is unavailable.");
    }
    record Report(String phase,List<Family> families,String childName,int age,String parentA,String parentB,String district,boolean preview,int maxWaitDays,String schoolFocus,Map<String,Integer> skills,int schoolDays,List<Conversation> conversations,Identity identity,AdultOptions adultOptions,List<PastLife> pastLives,ChildOptions childOptions,FamilyOptions familyOptions,HomeOptions homeOptions,Grocery grocery,Cafe cafe,Medical medical,EducationLife.College college,List<TechnicalWorkLife.LinkedOrder> engineeringOrders,List<SoftwareLife.ProjectView> softwareProjects,JavaTicketLife.Workspace javaWorkspace,SportLife.View sports,DojoLife.View dojo,DojoCommunityLife.View dojoCommunity,KumiteLife.View kumite){}
    static Report report(TownLife.Town t,int day){return report(t,day,0);}
    static Report report(TownLife.Town t,int day,int minute){
        int phase=phase(t);var c=child(t);
        var a=c==null?null:DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-a"));
        var b=c==null?null:DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-b"));
        var skills=new LinkedHashMap<String,Integer>();
        if(c!=null)for(String subject:EducationLife.SUBJECTS)skills.put(subject,EducationLife.get(t,c,subject));
        var conversations=conversationPartners(t,day).stream().map(p->new Conversation(p.id,p.name)).toList();
        return new Report(phase==CHOICE?"CHOICE":phase==CHILDHOOD?"CHILDHOOD":phase==LINKED_ADULT?"LINKED_ADULT":"ADULT",phase==CHOICE?families(t,day):List.of(),
            c==null?null:c.name,c==null?0:c.age,a==null?null:a.name,b==null?null:b.name,c==null?null:c.home.label,phase==CHILDHOOD,maxWaitDays(t),c==null||c.age<6?null:storedFocus(t),skills,c==null?0:EducationLife.get(t,c,"school-days"),conversations,identity(t,c),adultOptions(t,day),history(t),childOptions(t,day,minute),familyOptions(t,day),homeOptions(t,day,minute),grocery(t,day,minute),cafe(t,day,minute),medical(t,day,minute),EducationLife.college(t,day,minute),TechnicalWorkLife.linkedOrders(t,day,minute),SoftwareLife.projects(t,day,minute),JavaTicketLife.workspace(t,day,minute),SportLife.view(t,c,day,minute),DojoLife.view(t,c,day,minute),DojoCommunityLife.view(t,day,minute),KumiteLife.view(t,c,day,minute));
    }
}
