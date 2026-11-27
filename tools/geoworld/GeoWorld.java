import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Executors;

/** Independent, deterministic geographic population prototype. No actual resident data. */
public class GeoWorld {
    record Point(double x,double z) {
        double distance(Point p){return Math.hypot(x-p.x,z-p.z);}
        Point lerp(Point p,double f){return new Point(x+(p.x-x)*f,z+(p.z-z)*f);}
    }
    record Anchor(String building,String node){}
    record Step(String id,double distance){}
    record Journey(List<Point> points,double meters){
        Point position(double fraction){
            double left=Math.max(0,Math.min(1,fraction))*meters;
            for(int i=1;i<points.size();i++){
                Point a=points.get(i-1),b=points.get(i);
                double n=a.distance(b);
                if(n>0&&left<=n)return a.lerp(b,left/n);
                left-=n;
            }
            return points.get(points.size()-1);
        }
    }
    record Resident(String id,String name,int age,String household,String job,Anchor home,Anchor work,
                    int start,int finish,boolean ownsCar,Journey trip,int duration){}
    record Visible(Resident resident,Point position,String activity,double distance){}
    final Map<String,Point> nodes=new TreeMap<>();
    final Map<String,List<String>> edges=new TreeMap<>();
    final List<Anchor> anchors=new ArrayList<>();
    final List<Resident> residents=new ArrayList<>();
    final Path save;
    final String fingerprint;
    final int population;
    final double initialMinute;
    final long started=System.nanoTime();
    static final String[] FIRST={"Giulia","Luca","Elena","Matteo","Sofia","Marco","Chiara","Andrea","Sara","Davide","Irene","Pietro","Marta","Paolo","Alice","Nico"};
    static final String[] LAST={"Rossi","Conti","Ferrari","Riva","Bruni","Villa","Costa","Galli","Greco","Fontana","Ricci","Moretti","Leone","Marino","Serra","Monti"};
    static final String[] JOBS={"Shop assistant","Mechanic","Teacher","Office clerk","Nurse","Baker","Engineer","Barista","Driver","Electrician"};
    GeoWorld(Path graph,Path save,int population) throws Exception {
        if(population<1||population>2000)throw new IllegalArgumentException("Population must be 1–2000 for this prototype");
        this.population=population;this.save=save;
        fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(graph)));
        for(String line:Files.readAllLines(graph)){
            String[] p=line.split("\t");
            if(p[0].equals("N")){nodes.put(p[1],new Point(Double.parseDouble(p[2]),Double.parseDouble(p[3])));edges.put(p[1],new ArrayList<>());}
        }
        for(String line:Files.readAllLines(graph)){
            String[] p=line.split("\t");
            if(p[0].equals("E")&&nodes.containsKey(p[1])&&nodes.containsKey(p[2])){
                edges.get(p[1]).add(p[2]);edges.get(p[2]).add(p[1]);
            }else if(p[0].equals("B")&&nodes.containsKey(p[2]))anchors.add(new Anchor(p[1],p[2]));
        }
        Set<String> largest=largestComponent();
        if(largest.size()<2)throw new IllegalArgumentException("Map has no connected walking network");
        anchors.removeIf(a->!largest.contains(a.node));
        if(anchors.stream().map(Anchor::node).distinct().count()<2){
            anchors.clear();
            for(String id:new TreeSet<>(largest))anchors.add(new Anchor("street-"+id,id));
        }
        anchors.sort(Comparator.comparing(Anchor::building).thenComparing(Anchor::node));
        if(Files.exists(save)){
            Properties old=new Properties();
            try(var in=Files.newInputStream(save)){old.load(in);}
            if(!fingerprint.equals(old.getProperty("map"))||!Integer.toString(population).equals(old.getProperty("population")))
                throw new IllegalStateException("Geographic map or population changed. Existing save preserved; choose another save file.");
            initialMinute=Double.parseDouble(old.getProperty("minute"));
            if(!Double.isFinite(initialMinute)||initialMinute<0)throw new IllegalStateException("Invalid saved clock");
        }else initialMinute=465;
        generate();
        persist();
    }
    Set<String> largestComponent(){
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
    Journey route(String from,String to){
        var distance=new HashMap<String,Double>();var previous=new HashMap<String,String>();
        var queue=new PriorityQueue<Step>(Comparator.comparingDouble(Step::distance).thenComparing(Step::id));
        distance.put(from,0.0);queue.add(new Step(from,0));
        while(!queue.isEmpty()){
            Step step=queue.remove();
            if(step.distance>distance.getOrDefault(step.id,Double.POSITIVE_INFINITY))continue;
            if(step.id.equals(to))break;
            for(String next:edges.get(step.id)){
                double d=step.distance+nodes.get(step.id).distance(nodes.get(next));
                if(d<distance.getOrDefault(next,Double.POSITIVE_INFINITY)){
                    distance.put(next,d);previous.put(next,step.id);queue.add(new Step(next,d));
                }
            }
        }
        if(!distance.containsKey(to))throw new IllegalStateException("Disconnected commute");
        var path=new ArrayList<Point>();
        for(String at=to;at!=null;at=previous.get(at)){path.add(nodes.get(at));if(at.equals(from))break;}
        Collections.reverse(path);return new Journey(List.copyOf(path),distance.get(to));
    }
    void generate(){
        var rng=new SplittableRandom(721788L);
        Anchor home=null;String surname="",household="";
        int householdNumber=0,left=0;
        var cache=new HashMap<String,Journey>();
        for(int i=1;i<=population;i++){
            if(left==0){
                home=anchors.get(rng.nextInt(anchors.size()));
                surname=LAST[rng.nextInt(LAST.length)];
                household="household-"+(++householdNumber);
                left=rng.nextInt(1,5);
            }
            left--;
            Anchor work=anchors.get(rng.nextInt(anchors.size()));
            for(int attempt=0;attempt<12&&work.node.equals(home.node);attempt++)work=anchors.get(rng.nextInt(anchors.size()));
            String key=home.node+":"+work.node;
            Journey journey=cache.get(key);
            if(journey==null){journey=route(home.node,work.node);cache.put(key,journey);}
            int duration=Math.max(2,(int)Math.ceil(journey.meters/75.0));
            int start=480+rng.nextInt(-30,61);
            residents.add(new Resident("geo-%06d".formatted(i),FIRST[rng.nextInt(FIRST.length)]+" "+surname,
                rng.nextInt(19,70),household,JOBS[rng.nextInt(JOBS.length)],home,work,
                start,start+480,rng.nextInt(3)==0,journey,duration));
        }
    }
    double minute(){return initialMinute+(System.nanoTime()-started)/5_000_000_000.0;}
    synchronized void persist() throws IOException {
        Path parent=save.toAbsolutePath().getParent();Files.createDirectories(parent);
        Properties p=new Properties();p.setProperty("map",fingerprint);
        p.setProperty("population",Integer.toString(population));p.setProperty("minute",Double.toString(minute()));
        Path tmp=Files.createTempFile(parent,"geo-save-",".tmp");
        try{
            try(var out=Files.newOutputStream(tmp)){p.store(out,"Living Alibi geographic clock and deterministic roster identity");}
            try{Files.move(tmp,save,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(tmp,save,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(tmp);}
    }
    Visible visible(Resident r,double absolute,Point center){
        double m=absolute%1440;
        double fraction;String activity;
        if(m>=r.start-r.duration&&m<r.start){fraction=(m-(r.start-r.duration))/r.duration;activity="COMMUTING_TO_WORK";}
        else if(m>=r.finish&&m<r.finish+r.duration){fraction=1-(m-r.finish)/r.duration;activity="RETURNING_HOME";}
        else return null; // people at home or work are indoors, not on the road
        Point position=r.trip.position(fraction);
        return new Visible(r,position,activity,position.distance(center));
    }
    String snapshot(double absolute,Point center){
        var nearby=new ArrayList<Visible>();int commuting=0;
        for(var resident:residents){
            Visible v=visible(resident,absolute,center);
            if(v!=null){commuting++;if(v.distance<=140)nearby.add(v);}
        }
        nearby.sort(Comparator.comparingDouble(Visible::distance).thenComparing(v->v.resident.id));
        StringBuilder out=new StringBuilder("{\"day\":").append((long)(absolute/1440)+1)
            .append(",\"minute\":").append((int)(absolute%1440)).append(",\"population\":").append(residents.size())
            .append(",\"commuting\":").append(commuting).append(",\"residents\":[");
        int n=Math.min(40,nearby.size());
        for(int i=0;i<n;i++){
            if(i>0)out.append(',');
            var v=nearby.get(i);
            // Do not expose the private roster, names, homes or work locations to the client.
            out.append("{\"id\":\"").append(v.resident.id).append("\",\"x\":").append(v.position.x)
                .append(",\"z\":").append(v.position.z).append(",\"activity\":\"").append(v.activity).append("\"}");
        }
        return out.append("]}").toString();
    }
    void serve(int port) throws IOException {
        var http=HttpServer.create(new InetSocketAddress("127.0.0.1",port),0);
        http.createContext("/geo/nearby",exchange->{
            int status=200;String body;
            try{
                if(!exchange.getRequestMethod().equals("GET"))throw new IllegalArgumentException("GET only");
                Map<String,String> query=new HashMap<>();
                String raw=exchange.getRequestURI().getRawQuery();
                if(raw!=null)for(String part:raw.split("&")){
                    String[] pair=part.split("=",2);if(pair.length==2)query.put(pair[0],pair[1]);
                }
                double x=Double.parseDouble(query.getOrDefault("x","0")),z=Double.parseDouble(query.getOrDefault("z","0"));
                if(!Double.isFinite(x)||!Double.isFinite(z))throw new IllegalArgumentException("Invalid coordinates");
                body=snapshot(minute(),new Point(x,z));
            }catch(RuntimeException e){status=400;body="{\"error\":\"Invalid request\"}";}
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.getResponseHeaders().set("Cache-Control","no-store");
            exchange.sendResponseHeaders(status,bytes.length);
            try(var stream=exchange.getResponseBody()){stream.write(bytes);}
        });
        var timer=Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(()->{try{persist();}catch(IOException e){System.err.println("Geo save: "+e.getMessage());}},15,15,java.util.concurrent.TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(()->{http.stop(0);timer.shutdown();try{persist();}catch(IOException e){System.err.println(e.getMessage());}}));
        http.start();
        System.out.println("GeoWorld: "+population+" deterministic residents on "+nodes.size()+" road nodes, http://127.0.0.1:"+port);
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("GeoWorld graph.tsv save.properties population port");
        new GeoWorld(Path.of(args[0]),Path.of(args[1]),Integer.parseInt(args[2])).serve(Integer.parseInt(args[3]));
    }
}
