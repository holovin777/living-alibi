package com.holovin.npclife;

import java.util.*;

/** Local fictional dispatch, driven by the same report state as the old game. */
final class GeoResponse {
    final String officer;
    String from,to;
    double progress;
    long lastUpdate;
    GeoResponse(String officer,String node,long now){this.officer=officer;from=to=node;lastUpdate=now;}
    GeoProjection.Point position(GeoProjection map){return map.motorNodes.get(from).lerp(map.motorNodes.get(to),progress);}
    double heading(GeoProjection map){
        var a=map.motorNodes.get(from);var b=map.motorNodes.get(to);
        return Math.atan2(-(b.x()-a.x()),-(b.z()-a.z()));
    }
    static String next(GeoProjection map,String start,String target){
        if(start.equals(target))return null;
        record Entry(String id,double cost){}
        var queue=new PriorityQueue<Entry>(Comparator.comparingDouble(Entry::cost).thenComparing(Entry::id));
        var distance=new HashMap<String,Double>();var previous=new HashMap<String,String>();
        distance.put(start,0.0);queue.add(new Entry(start,0));
        while(!queue.isEmpty()){
            var e=queue.remove();if(e.cost()>distance.getOrDefault(e.id(),Double.POSITIVE_INFINITY))continue;
            if(e.id().equals(target))break;
            for(String n:map.motorEdges.getOrDefault(e.id(),List.of())){
                double d=e.cost()+map.motorNodes.get(e.id()).distance(map.motorNodes.get(n));
                if(d<distance.getOrDefault(n,Double.POSITIVE_INFINITY)){
                    distance.put(n,d);previous.put(n,e.id());queue.add(new Entry(n,d));
                }
            }
        }
        if(!previous.containsKey(target))return null;
        String at=target;
        while(!start.equals(previous.get(at)))at=previous.get(at);
        return at;
    }
    void advance(GeoProjection map,String target,double meters){
        for(int i=0;i<24&&meters>.001;i++){
            if(from.equals(to)||progress>=1){
                String n=next(map,to,target);if(n==null)break;
                from=to;to=n;progress=0;
            }
            double length=map.motorNodes.get(from).distance(map.motorNodes.get(to));
            if(length<.01){progress=1;continue;}
            double step=Math.min(meters,length*(1-progress));
            progress=Math.min(1,progress+step/length);meters-=step;
        }
    }
    GeoProjection.Patrol view(GeoProjection map){
        var p=position(map);return new GeoProjection.Patrol("patrol-"+officer,p.x(),p.z(),heading(map),true);
    }
    /** Arcade contact with a parked vehicle, using a swept segment to avoid tunneling. */
    static boolean contact(GeoProjection.Point a,GeoProjection.Point b,GeoProjection.Point obstacle){
        double dx=b.x()-a.x(),dz=b.z()-a.z(),length=dx*dx+dz*dz;
        if(length<.001)return false;
        double t=Math.max(0,Math.min(1,((obstacle.x()-a.x())*dx+(obstacle.z()-a.z())*dz)/length));
        return a.lerp(b,t).distance(obstacle)<2.1;
    }
}
