package com.holovin.npclife;

import java.util.*;

/** Road-constrained manual driving. Java advances the car along legal directed edges. */
final class GeoDrive {
    record Pose(boolean active,boolean stolen,double x,double z,double heading,int fuel,int condition,double speedKmh,int speedLimitKmh,String message){}
    final boolean stolen;
    String from,to;
    double progress,meters,speed;
    static final double MAX_SPEED=10.0; // legacy default, m/s
    static final double HIGHWAY_SPEED=20.0; // prototype safety/performance cap
    long lastStep;
    GeoDrive(boolean stolen,String from){this.stolen=stolen;this.from=from;this.to=from;lastStep=System.nanoTime();}
    GeoDrive copy(){
        var c=new GeoDrive(stolen,from);c.to=to;c.progress=progress;c.meters=meters;c.speed=speed;c.lastStep=lastStep;return c;
    }
    GeoProjection.Point position(GeoProjection map){return map.motorNodes.get(from).lerp(map.motorNodes.get(to),progress);}
    double heading(GeoProjection map){
        var a=map.motorNodes.get(from);var b=map.motorNodes.get(to);
        return Math.atan2(-(b.x()-a.x()),-(b.z()-a.z()));
    }
    String next(GeoProjection map,int turn){
        var options=new ArrayList<>(map.motorEdges.getOrDefault(to,List.of()));
        if(options.size()>1)options.remove(from);
        if(options.isEmpty())return null;
        if(from.equals(to))return options.get(0);
        var a=map.motorNodes.get(from);var b=map.motorNodes.get(to);
        double heading=Math.atan2(b.z()-a.z(),b.x()-a.x());
        Comparator<String> cmp=Comparator.comparingDouble(n->{
            var c=map.motorNodes.get(n);
            double angle=Math.atan2(c.z()-b.z(),c.x()-b.x())-heading;
            angle=Math.atan2(Math.sin(angle),Math.cos(angle));
            return Math.abs(angle-turn*Math.PI/2);
        });
        return options.stream().min(cmp.thenComparing(n->n)).orElse(null);
    }
    /** Bound elapsed time so pauses or network stalls cannot teleport the car. */
    double controlBudget(String command,double elapsed){return controlBudget(command,elapsed,MAX_SPEED);}
    double controlBudget(String command,double elapsed,double limit){
        limit=Math.max(0,Math.min(HIGHWAY_SPEED,limit));
        speed=Math.min(speed,limit);
        double dt=Math.max(0,Math.min(0.8,elapsed));
        if(elapsed>1.5)speed=0;
        double initial=speed;
        double acceleration=switch(command){case "step" -> 3.0;case "brake" -> -8.0;default -> -1.8;};
        speed=Math.max(0,Math.min(limit,speed+acceleration*dt));
        if(acceleration<0&&initial+acceleration*dt<0)dt=initial/-acceleration;
        return (initial+speed)*0.5*dt;
    }
    double advance(GeoProjection map,int turn,double budget){
        double traveled=0;
        for(int i=0;i<24&&budget>0.001;i++){
            if(from.equals(to)||progress>=1){
                String n=next(map,turn);if(n==null)break;
                from=to;to=n;progress=0;turn=0;
            }
            double length=map.motorNodes.get(from).distance(map.motorNodes.get(to));
            if(length<0.01){progress=1;continue;}
            double step=Math.min(budget,length*(1-progress));
            progress=Math.min(1,progress+step/length);
            traveled+=step;budget-=step;
        }
        meters+=traveled;return traveled;
    }
}
