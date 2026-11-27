package com.holovin.npclife;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Geographic rendering adapter for the original simulation, not a second world. */
final class GeoProjection {
    record Point(double x,double z){
        double distance(Point p){return Math.hypot(x-p.x,z-p.z);}
        Point lerp(Point p,double f){return new Point(x+(p.x-x)*f,z+(p.z-z)*f);}
    }
    record District(String id,String label,double x,double z){}
    record Actor(String id,double x,double z,String activity,boolean talkable){}
    record Choice(String id,String label){}
    record ActionResult(String message,District arrival){}
    record View(int day,int minute,int population,int commuting,List<Actor> residents,
                District playerDistrict,District nearestDistrict,boolean canEnter,
                boolean alive,boolean confined,boolean rebirth,int money,int hunger,int thirst,int health,
                String carDistrict,boolean cycling,District parkedCar,District parkedBicycle,
                int carFuel,int carCondition,String jobId,List<Choice> actions){}
    record Step(String id,double cost){}
    record Route(List<Point> points,double meters){
        Point at(double fraction){
            double remaining=Math.max(0,Math.min(1,fraction))*meters;
            for(int i=1;i<points.size();i++){
                Point a=points.get(i-1),b=points.get(i);double d=a.distance(b);
                if(d>0&&remaining<=d)return a.lerp(b,remaining/d);
                remaining-=d;
            }
            return points.get(points.size()-1);
        }
    }
    final Map<String,Point> nodes=new TreeMap<>();
    final Map<String,List<String>> edges=new TreeMap<>();
    final EnumMap<TownLife.Place,String> anchors=new EnumMap<>(TownLife.Place.class);
    final Map<String,Route> routes=new LinkedHashMap<>(128,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,Route> e){return size()>512;}
    };
    GeoProjection(Path graph)throws IOException{
        var rows=Files.readAllLines(graph);
        for(String row:rows){
            String[] p=row.split("\t");
            if(p[0].equals("N")){nodes.put(p[1],new Point(Double.parseDouble(p[2]),Double.parseDouble(p[3])));edges.put(p[1],new ArrayList<>());}
        }
        for(String row:rows){
            String[] p=row.split("\t");
            if(p[0].equals("E")&&nodes.containsKey(p[1])&&nodes.containsKey(p[2])){
                edges.get(p[1]).add(p[2]);edges.get(p[2]).add(p[1]);
            }
        }
        Set<String> largest=largest();
        if(largest.size()<2)throw new IllegalArgumentException("No connected walking network");
        var available=new TreeSet<>(largest);
        double minX=largest.stream().mapToDouble(id->nodes.get(id).x).min().orElseThrow();
        double maxX=largest.stream().mapToDouble(id->nodes.get(id).x).max().orElseThrow();
        double minZ=largest.stream().mapToDouble(id->nodes.get(id).z).min().orElseThrow();
        double maxZ=largest.stream().mapToDouble(id->nodes.get(id).z).max().orElseThrow();
        var places=new ArrayList<>(PlayerLife.publicPlaces());
        places.remove(TownLife.Place.CENTRO);places.add(0,TownLife.Place.CENTRO);
        for(var p:places){
            var c=CityExpansionLife.cell(p);
            Point desired=p==TownLife.Place.CENTRO?new Point(0,0):new Point(
                minX+(maxX-minX)*(c.x()+.5)/CityExpansionLife.WIDTH,
                minZ+(maxZ-minZ)*(c.y()+.5)/CityExpansionLife.HEIGHT);
            Set<String> candidates=available.isEmpty()?largest:available;
            String id=candidates.stream().min(Comparator.comparingDouble((String n)->nodes.get(n).distance(desired)).thenComparing(n->n)).orElseThrow();
            anchors.put(p,id);available.remove(id);
        }
        anchors.put(TownLife.Place.MARCO_HOME,anchors.get(TownLife.Place.NORTH_RESIDENCES));
        anchors.put(TownLife.Place.ELENA_HOME,anchors.get(TownLife.Place.SOUTH_RESIDENCES));
    }
    Set<String> largest(){
        Set<String> seen=new HashSet<>(),largest=Set.of();
        for(String root:nodes.keySet()){
            if(!seen.add(root))continue;
            var component=new HashSet<String>();var queue=new ArrayDeque<String>();queue.add(root);
            while(!queue.isEmpty()){
                String id=queue.remove();component.add(id);
                for(String next:edges.get(id))if(seen.add(next))queue.add(next);
            }
            if(component.size()>largest.size())largest=component;
        }
        return largest;
    }
    Point point(TownLife.Place p){return nodes.get(anchors.get(p));}
    District district(TownLife.Place p){Point at=point(p);return new District(p.name(),p.label,at.x,at.z);}
    TownLife.Place nearest(Point at){
        return PlayerLife.publicPlaces().stream().min(Comparator.comparingDouble((TownLife.Place p)->point(p).distance(at))).orElseThrow();
    }
    Route route(TownLife.Place from,TownLife.Place to){
        String a=anchors.get(from),b=anchors.get(to),key=a+":"+b;
        var cached=routes.get(key);if(cached!=null)return cached;
        var distance=new HashMap<String,Double>();var prev=new HashMap<String,String>();
        var queue=new PriorityQueue<Step>(Comparator.comparingDouble(Step::cost).thenComparing(Step::id));
        distance.put(a,0.0);queue.add(new Step(a,0));
        while(!queue.isEmpty()){
            var e=queue.remove();if(e.cost>distance.getOrDefault(e.id,Double.POSITIVE_INFINITY))continue;
            if(e.id.equals(b))break;
            for(String next:edges.get(e.id)){
                double d=e.cost+nodes.get(e.id).distance(nodes.get(next));
                if(d<distance.getOrDefault(next,Double.POSITIVE_INFINITY)){
                    distance.put(next,d);prev.put(next,e.id);queue.add(new Step(next,d));
                }
            }
        }
        var points=new ArrayList<Point>();
        for(String at=b;at!=null;at=prev.get(at)){points.add(nodes.get(at));if(at.equals(a))break;}
        Collections.reverse(points);
        var result=new Route(List.copyOf(points),distance.getOrDefault(b,0.0));routes.put(key,result);return result;
    }
    Point position(TownLife.Citizen c){
        if(c.travelRemaining>0&&c.place!=c.destination){
            int duration=Math.max(1,TownLife.travelTime(c.place,c.destination));
            return route(c.place,c.destination).at(1.0-c.travelRemaining/(double)duration);
        }
        Point at=point(c.place);int h=c.id.hashCode();
        return new Point(at.x+(Math.floorMod(h,7)-3)*.6,at.z+(Math.floorMod(h/7,7)-3)*.6);
    }
    static boolean outdoor(TownLife.Citizen c){
        if(!c.alive||"IN_CUSTODY".equals(c.activity))return false;
        if(c.travelRemaining>0)return true;
        return c.currentSpace().equals(TownLife.publicSpace(c.place))&&!"SLEEPING".equals(c.activity)
            &&!"STUDYING".equals(c.activity)&&(!"WORKING".equals(c.activity)||PoliceLife.officer(c));
    }
    List<Choice> choices(TownLife.Town t,WorldState s,Point player){
        var p=t.player;var result=new ArrayList<Choice>();
        if(!p.alive||p.travelRemaining>0||JusticeLife.confined(t)||RebirthLife.active(t)||IntercomLife.insideVisit(t,s.day,s.minute))return result;
        result.add(new Choice("rest","Rest / Riposa · 15 min"));
        for(String id:List.of("bread","water","pizza","gelato","grape-juice"))
            if(FoodLife.quantity(p,id,false)>0)result.add(new Choice("consume:"+id,"Consume / Consuma "+RetailLife.product(id).label()));
        if(point(p.place).distance(player)>18)return result;
        if(p.place==TownLife.Place.PARK)result.add(new Choice("fountain","Drink at fountain / Fontana"));
        if(RetailLife.open(p.place,s.minute))
            for(var item:RetailLife.PRODUCTS)
                if(RetailLife.availableAt(p.place,item)&&t.retailStock.getOrDefault(item.id(),0)>0)
                    result.add(new Choice("buy:"+item.id(),"Buy / Compra "+item.label()+" · €"+item.price()));
        for(var job:CareerLife.offers())if(job.workplace()==p.place){
            if(job.id().equals(p.jobId))result.add(new Choice("work","Work / Lavora · "+job.title()));
            else result.add(new Choice("apply:"+job.id(),"Apply / Candidati · "+job.title()+" (€"+job.wage()+"/shift)"));
        }
        if(p.place==TownLife.Place.SCHOOL)result.add(new Choice("study:engineering","Study engineering / Ingegneria"));
        if(p.place==TownLife.Place.LIBRARY)result.add(new Choice("study:investigation","Study investigation / Investigazione"));
        for(var order:TechnicalWorkLife.orders(t))if(order.place==p.place){
            result.add(new Choice("diagnose:"+order.id,"Inspect / Ispeziona "+order.title));
            result.add(new Choice("repair-order:"+order.id,"Repair / Ripara "+order.title));
        }
        if(p.place==TownLife.Place.WORKSHOP){
            if(p.carPlace==null)result.add(new Choice("car:buy","Buy used car / Auto usata · €180"));
            if(!p.ownsBicycle)result.add(new Choice("bike-buy","Buy bicycle / Bicicletta · €45"));
        }
        if(p.ownsBicycle){
            if(p.bicycleParked==null){
                result.add(new Choice(p.cycling?"bike:walk":"bike:cycle",p.cycling?"Walk / Cammina":"Cycle / Pedala"));
                if(TransportLife.hasRack(p.place))result.add(new Choice("bike:park","Park bicycle / Parcheggia bici"));
            }else if(p.bicycleParked==p.place)result.add(new Choice("bike:retrieve","Retrieve bicycle / Ritira bici"));
        }
        if(p.carPlace==p.place){
            if(p.place==TownLife.Place.WORKSHOP||p.place==TownLife.Place.FUEL_STATION||CityExpansionLife.serviceHub(p.place))
                result.add(new Choice("car:refuel","Refuel / Rifornisci"));
            if(p.place==TownLife.Place.WORKSHOP||CityExpansionLife.serviceHub(p.place))
                result.add(new Choice("car:repair","Service car / Ripara auto"));
            for(var to:MotorLife.neighbors(p.place))
                result.add(new Choice("drive:"+to.name(),"Car journey / Viaggio in auto → "+to.label));
        }
        var bus=BusLife.view(t,s);
        if(bus.available())for(var route:bus.routes())
            result.add(new Choice("bus:"+route.id(),"Bus → "+route.label()+" · "+route.minutes()+" min · €"+bus.fare()));
        return List.copyOf(result);
    }
    View view(TownLife.Town t,WorldState s,Point player){
        var p=t.player;
        boolean confined=JusticeLife.confined(t),rebirth=RebirthLife.active(t);
        var talking=new HashSet<>(PlayerLife.visible(t));
        var nearby=new ArrayList<Actor>();int population=0,commuting=0;
        for(var c:t.people.values()){
            if(!c.alive)continue;population++;
            if(c.travelRemaining>0)commuting++;
            if(!outdoor(c))continue;
            Point at=position(c);
            if(at.distance(player)<=140)nearby.add(new Actor(c.id,at.x,at.z,c.activity,talking.contains(c)&&!rebirth));
        }
        nearby.sort(Comparator.comparingDouble((Actor a)->new Point(a.x,a.z).distance(player)).thenComparing(Actor::id));
        var nearest=nearest(player);
        boolean canEnter=p.alive&&!confined&&!rebirth&&p.travelRemaining==0&&point(nearest).distance(player)<=18;
        return new View(s.day,s.minute,population,commuting,List.copyOf(nearby.subList(0,Math.min(40,nearby.size()))),
            district(p.place),district(nearest),canEnter,p.alive,confined,rebirth,p.money,p.hunger,p.thirst,p.health,
            p.carPlace==null?null:p.carPlace.name(),p.cycling,p.carPlace==null?null:district(p.carPlace),
            p.bicycleParked==null?null:district(p.bicycleParked),p.carFuel,p.carCondition,p.jobId,choices(t,s,player));
    }
}
