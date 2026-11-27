package com.holovin.npclife;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Fictional creative work and a finite local market. Each sale transfers
 * existing money, preserves an authorship record, and never changes a job. */
final class CultureLife {
    private CultureLife(){}
    enum Art {
        MUSIC("Music / Musica",TownLife.Place.SCHOOL),
        PAINTING("Painting / Pittura",TownLife.Place.LIBRARY),
        WRITING("Writing / Scrittura",TownLife.Place.LIBRARY),
        ACTING("Acting / Recitazione",TownLife.Place.CENTRO),
        PHOTOGRAPHY("Photography / Fotografia",TownLife.Place.CENTRO);
        final String label;final TownLife.Place venue;
        Art(String label,TownLife.Place venue){this.label=label;this.venue=venue;}
    }
    static String key(TownLife.Citizen c,String f){return "culture:"+c.id+":"+f;}
    static int get(TownLife.Town t,TownLife.Citizen c,String f){return t.industryStocks.getOrDefault(key(c,f),0);}
    static void set(TownLife.Town t,TownLife.Citizen c,String f,int n){t.industryStocks.put(key(c,f),n);}
    static String field(Art art,String field){return art.name()+":"+field;}
    static int skill(TownLife.Town t,TownLife.Citizen c,Art art){return get(t,c,field(art,"skill"));}
    static boolean ready(TownLife.Citizen c,WorldState s){
        return c!=null&&c.alive&&c.age>=6&&RebirthLife.awakePresent(c)&&!EmploymentLife.sick(c,s.day)
            &&c.health>=60&&c.fatigue<80&&c.hunger<85&&c.thirst<85;
    }
    static boolean canPractice(TownLife.Town t,TownLife.Citizen c,WorldState s,Art art){
        return ready(c,s)&&c.place==art.venue&&!RebirthLife.atHome(c)&&s.minute>=1020&&s.minute<1200
            &&get(t,c,"last-workshop")<s.day;
    }
    static void practice(TownLife.Town t,TownLife.Citizen c,WorldState s,Art art){
        if(!canPractice(t,c,s,art))throw new IllegalArgumentException("Visit the creative workshop in the evening.");
        set(t,c,"last-workshop",s.day);
        set(t,c,field(art,"skill"),Math.min(100,skill(t,c,art)+2));
        c.fatigue=Math.min(100,c.fatigue+5);
        PsychologyLife.adjust(t,c,"stress",20,-2);
    }
    static boolean canCreate(TownLife.Town t,TownLife.Citizen c,WorldState s,Art art){
        return canPractice(t,c,s,art)&&skill(t,c,art)>=10&&get(t,c,field(art,"pending"))==0;
    }
    static void create(TownLife.Town t,TownLife.Citizen c,WorldState s,Art art){
        if(!canCreate(t,c,s,art))throw new IllegalArgumentException("Develop the skill and finish the previous work first.");
        int serial=Math.addExact(t.industryStocks.getOrDefault("culture:serial",0),1);
        t.industryStocks.put("culture:serial",serial);
        t.industryStocks.put("culture:work:"+serial+":author",c.generationIndex);
        t.industryStocks.put("culture:work:"+serial+":art",art.ordinal()+1);
        t.industryStocks.put("culture:work:"+serial+":quality",skill(t,c,art));
        t.industryStocks.put("culture:work:"+serial+":day",s.day);
        set(t,c,field(art,"pending"),serial);set(t,c,"last-workshop",s.day);
        set(t,c,"works",Math.addExact(get(t,c,"works"),1));
        c.fatigue=Math.min(100,c.fatigue+8);
    }
    static boolean festivalDay(int day){return Math.floorMod(day-1,7)==6;}
    static boolean canExhibit(TownLife.Town t,TownLife.Citizen c,WorldState s,Art art){
        return ready(c,s)&&c.age>=18&&c.place==TownLife.Place.LIBRARY&&!RebirthLife.atHome(c)
            &&get(t,c,field(art,"pending"))>0&&get(t,c,field(art,"listed"))==0;
    }
    static boolean canWithdraw(TownLife.Town t,TownLife.Citizen c,Art art){return get(t,c,field(art,"listed"))>0;}
    static int price(TownLife.Town t,int serial){return 5+t.industryStocks.getOrDefault("culture:work:"+serial+":quality",0)/5;}
    static boolean fairVisitor(TownLife.Citizen c,WorldState s){
        return c.age>=18&&ready(c,s)&&c.place==TownLife.Place.LIBRARY&&!RebirthLife.atHome(c);
    }
    static void fair(TownLife.Town t,WorldState s){
        if(t.industryStocks.getOrDefault("culture:last-fair",0)>=s.day)return;
        t.industryStocks.put("culture:last-fair",s.day);
        var visitors=t.people.values().stream().filter(c->fairVisitor(c,s)&&!RelationshipBoundaryLife.controlled(t,c))
            .sorted(Comparator.comparing(c->c.id)).toList();
        var used=new java.util.HashSet<String>();int sales=0;
        for(var author:t.people.values())if(author.alive&&author.age>=18)for(var art:Art.values()){
            int serial=get(t,author,field(art,"pending"));
            if(serial<=0||get(t,author,field(art,"listed"))!=serial)continue;
            int price=price(t,serial);
            var buyer=visitors.stream().filter(v->v!=author&&!used.contains(v.id)&&v.money>=price+50).findFirst().orElse(null);
            if(buyer==null)continue;
            int wallet=Math.addExact(author.money,price);
            buyer.money-=price;author.money=wallet;used.add(buyer.id);
            t.industryStocks.put("culture:work:"+serial+":holder",buyer.generationIndex);
            set(t,author,field(art,"pending"),0);set(t,author,field(art,"listed"),0);
            set(t,author,"sales",Math.addExact(get(t,author,"sales"),1));
            set(t,author,"reputation",Math.min(100,get(t,author,"reputation")+2));sales++;
            ArchiveLife.record(t,"artwork:"+serial,s.day,s.minute,"Culture",
                author.name+" presented an original "+art.label+" work at the city cultural fair.");
            PsychologyLife.adjust(t,buyer,"satisfaction",60,2);
        }
        if(!visitors.isEmpty())ArchiveLife.record(t,"culture-fair:"+s.day,s.day,s.minute,"Culture",
            "The city cultural fair welcomed "+visitors.size()+" visitors and recorded "+sales+" sales.");
    }
    static Art interest(TownLife.Citizen c){return Art.values()[Math.floorMod(c.personalSeed>>>12,Art.values().length)];}
    static boolean interested(TownLife.Citizen c){return c.age>=6&&(Math.floorMod(c.personalSeed,5)==0||"LEARN_SKILLS".equals(c.goal));}
    static TownLife.Place destination(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(!ready(c,s)||RelationshipBoundaryLife.controlled(t,c)||CityExpansionLife.outer(c.home))return null;
        if(festivalDay(s.day)&&s.minute>=900&&s.minute<=1020&&c.age>=18
            &&(interested(c)||Math.floorMod(c.personalSeed,7)==0))return TownLife.Place.LIBRARY;
        if(interested(c)&&s.day%3==1&&s.minute>=990&&s.minute<=1110)return interest(c).venue;
        return null;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute==1110&&s.day%3==1)for(var c:t.people.values()){
            if(!interested(c)||RelationshipBoundaryLife.controlled(t,c))continue;
            var art=interest(c);
            if(canCreate(t,c,s,art))create(t,c,s,art);
            else if(canPractice(t,c,s,art))practice(t,c,s,art);
        }
        if(s.minute==1000&&festivalDay(s.day)){
            for(var c:t.people.values())if(!RelationshipBoundaryLife.controlled(t,c))for(var art:Art.values())
                if(canExhibit(t,c,s,art))set(t,c,field(art,"listed"),get(t,c,field(art,"pending")));
            fair(t,s);
        }
    }
    static String act(TownLife.Town t,WorldState s,String action){
        var c=RebirthLife.child(t);
        if(!ready(c,s))throw new IllegalArgumentException("Creative action unavailable.");
        int dash=action.indexOf('-');if(dash<0)throw new IllegalArgumentException("Unknown creative action.");
        var art=Art.valueOf(action.substring(dash+1));
        return switch(action.substring(0,dash)){
            case "practice"->{practice(t,c,s,art);yield "Completed creative practice.";}
            case "create"->{create(t,c,s,art);yield "Created an original work.";}
            case "exhibit"->{if(!canExhibit(t,c,s,art))throw new IllegalArgumentException("Visit the library with an available work.");
                set(t,c,field(art,"listed"),get(t,c,field(art,"pending")));yield "Work offered at the next cultural fair.";}
            case "withdraw"->{if(!canWithdraw(t,c,art))throw new IllegalArgumentException("No active offer.");
                set(t,c,field(art,"listed"),0);yield "Offer withdrawn.";}
            default->throw new IllegalArgumentException("Unknown creative action.");
        };
    }
    record Discipline(String name,int skill,int pendingWork,boolean listed){}
    record View(int works,int sales,int reputation,List<Discipline> disciplines){}
    static View view(TownLife.Town t,TownLife.Citizen c){
        if(c==null)return null;var entries=new ArrayList<Discipline>();
        for(var art:Art.values())entries.add(new Discipline(art.label,skill(t,c,art),get(t,c,field(art,"pending")),get(t,c,field(art,"listed"))>0));
        return new View(get(t,c,"works"),get(t,c,"sales"),get(t,c,"reputation"),List.copyOf(entries));
    }
}
