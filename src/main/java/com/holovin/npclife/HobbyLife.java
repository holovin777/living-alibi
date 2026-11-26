package com.holovin.npclife;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Public recreational gatherings, distinct from formal dojo lessons and paid work. */
final class HobbyLife {
    private HobbyLife(){}
    record Club(String id,String name,TownLife.Place place,int weekday,int start,boolean outdoor,boolean sport){}
    static final List<Club> CLUBS=List.of(
        new Club("yoga","Yoga",TownLife.Place.PARK,1,1080,true,true),
        new Club("football","Football",TownLife.Place.PARK,3,1080,true,true),
        new Club("cycling","Cycling",TownLife.Place.RIVER,5,600,true,true),
        new Club("music","Music",TownLife.Place.CENTRO,4,1080,false,false),
        new Club("painting","Painting",TownLife.Place.LIBRARY,2,1080,false,false),
        new Club("fishing","Fishing",TownLife.Place.RIVER,6,600,true,false));
    static List<Club> interests(TownLife.Citizen c){
        int first=Math.floorMod(c.personalSeed,CLUBS.size());
        int second="STAY_HEALTHY".equals(c.goal)?Math.floorMod(c.personalSeed>>>8,3):
                c.curiosity>=65?3+Math.floorMod(c.personalSeed>>>8,3):Math.floorMod(c.personalSeed>>>8,CLUBS.size());
        if(second==first)second=(second+1)%CLUBS.size();
        return List.of(CLUBS.get(first),CLUBS.get(second));
    }
    static boolean open(Club club,int day){
        var weather=WeatherLife.at(day,club.start());
        return !club.outdoor()||(!weather.raining()&&!weather.snowing());
    }
    static Club scheduled(TownLife.Town t,TownLife.Citizen c,int day,int minute){
        if(c.generationIndex<=0||!c.alive||c.age<18||RebirthLife.linkedAdult(t,c)||EmploymentLife.sick(c,day)||
                c.health<65||c.fatigue>75||c.hunger>75||c.thirst>75)return null;
        for(var club:interests(c))if((day-1)%7==club.weekday()&&minute>=club.start()-20&&minute<club.start()+30&&open(club,day))
            return club;
        return null;
    }
    static String marker(Club c){return "HOBBY_"+c.id().toUpperCase(java.util.Locale.ROOT);}
    private static String key(TownLife.Citizen c,Club club,String field){return "hobby:"+c.id+":"+club.id()+":"+field;}
    static int sessions(TownLife.Town t,TownLife.Citizen c,Club club){return t.industryStocks.getOrDefault(key(c,club,"sessions"),0);}
    private static boolean attending(TownLife.Citizen c,Club club){
        return c.alive&&c.travelRemaining==0&&c.place==club.place()&&c.currentSpace().equals(TownLife.publicSpace(c.place))&&marker(club).equals(c.activity);
    }
    static void tick(TownLife.Town t,WorldState clock){
        for(var club:CLUBS){
            if((clock.day-1)%7!=club.weekday()||clock.minute!=club.start()+15||!open(club,clock.day))continue;
            var participants=new ArrayList<TownLife.Citizen>();
            for(var c:t.people.values()){
                if(!attending(c,club)||scheduled(t,c,clock.day,clock.minute)!=club||
                        t.industryStocks.getOrDefault(key(c,club,"day"),0)==clock.day)continue;
                t.industryStocks.put(key(c,club,"day"),clock.day);
                t.industryStocks.put(key(c,club,"sessions"),Math.addExact(sessions(t,c,club),1));
                if(club.sport()){
                    c.fatigue=Math.min(100,c.fatigue+(club.id().equals("yoga")?3:8));
                    c.thirst=Math.min(100,c.thirst+3);
                    EducationLife.learn(t,c,"fitness",1);
                }
                participants.add(c);
            }
            // At most three new shared-interest links per person per session.
            for(int i=0;i<participants.size();i++)for(int j=i+1;j<Math.min(i+4,participants.size());j++){
                var a=participants.get(i);var b=participants.get(j);
                var relation=t.peerRelationships.computeIfAbsent(SocialNetwork.key(a.id,b.id),k->new SocialLife.Relationship(a.id,b.id));
                relation.affinity=Math.min(100,relation.affinity+2);
                relation.trust=Math.min(100,relation.trust+1);
                relation.conversations++;
            }
        }
    }
    record Event(String name,String district,int day,int minute,int present,boolean cancelled){}
    static List<Event> report(TownLife.Town t,int day,int minute){
        var result=new ArrayList<Event>();
        for(var club:CLUBS){
            int next=day+Math.floorMod(club.weekday()-(day-1)%7,7);
            if(next==day&&minute>=club.start()+30)next+=7;
            int present=next==day?(int)t.people.values().stream().filter(c->attending(c,club)).count():0;
            result.add(new Event(club.name(),club.place().label,next,club.start(),present,!open(club,next)));
        }
        if(DojoCommunityLife.founded(t)){
            int next=day;
            while(!DojoLife.open(next,1080)||(next==day&&minute>=1260))next++;
            result.add(new Event("Karate",TownLife.Place.SCHOOL.label,next,1080,
                next==day?DojoCommunityLife.view(t,day,minute).present().size():0,false));
        }
        return result.stream().sorted(java.util.Comparator.comparingInt(Event::day).thenComparingInt(Event::minute)).toList();
    }
    static String reply(TownLife.Citizen c){
        var hobbies=interests(c);
        return "I like "+hobbies.get(0).name()+" and "+hobbies.get(1).name()+".";
    }
}
