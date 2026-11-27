package com.holovin.npclife;

import java.util.ArrayList;
import java.util.List;

/** Abstract fictional exposure categories. No recipes, doses, sourcing, prices or
 * clinical predictions. Adult choices are explicit; no autonomous player use.
 */
final class SubstanceLife {
    private SubstanceLife(){}
    enum Kind {
        ALCOHOL("Alcohol / Alcol"), NICOTINE("Nicotine / Nicotina"), CAFFEINE("Caffeine / Caffeina"),
        CANNABINOIDS("Cannabinoids / Cannabinoidi"), OPIOIDS("Opioids / Oppioidi"),
        STIMULANTS("Stimulants / Stimolanti"), SEDATIVES("Sedatives / Sedativi"),
        PSYCHEDELICS("Psychedelics / Psichedelici"), DISSOCIATIVES("Dissociatives / Dissociativi"),
        ENTACTOGENS("Entactogens / Entactogeni"), INHALANTS("Inhalants / Inalanti"),
        SYNTHETIC_CANNABINOIDS("Synthetic cannabinoids / Cannabinoidi sintetici"),
        OTHER("Other psychoactive substances / Altre sostanze");
        final String label;Kind(String label){this.label=label;}
    }
    static String key(TownLife.Citizen c,String f){return "substance:"+c.id+":"+f;}
    static int get(TownLife.Town t,TownLife.Citizen c,String f){return t.industryStocks.getOrDefault(key(c,f),0);}
    static void set(TownLife.Town t,TownLife.Citizen c,String f,int n){t.industryStocks.put(key(c,f),n);}
    static int dependence(TownLife.Town t,TownLife.Citizen c,Kind kind){return get(t,c,"dependence:"+kind.name());}
    static boolean canUse(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.alive&&c.age>=18&&RebirthLife.awakePresent(c)&&!NpcJusticeLife.confined(t,c,s)
            &&get(t,c,"last-exposure")<s.day;
    }
    static void exposure(TownLife.Town t,TownLife.Citizen c,WorldState s,Kind kind){
        if(!canUse(t,c,s))throw new IllegalArgumentException("Exposure unavailable.");
        set(t,c,"last-exposure",s.day);set(t,c,"last-kind",kind.ordinal()+1);
        int increase=switch(kind){case CAFFEINE->2;case NICOTINE,OPIOIDS,STIMULANTS->8;default->4;};
        set(t,c,"dependence:"+kind.name(),Math.min(100,dependence(t,c,kind)+increase));
        set(t,c,"impairment",kind==Kind.CAFFEINE||kind==Kind.NICOTINE?0:30);
        // Illustrative gameplay penalty, never a real-world risk estimate.
        int harm=kind==Kind.CAFFEINE?0:kind==Kind.NICOTINE?1:3;
        c.health=Math.max(1,c.health-harm);
        c.fatigue=Math.min(100,c.fatigue+harm);
    }
    static boolean canSupport(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.alive&&c.age>=18&&RebirthLife.awakePresent(c)&&c.place==TownLife.Place.CLINIC
            &&get(t,c,"support-day")<s.day&&total(t,c)>0;
    }
    static void support(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(!canSupport(t,c,s))throw new IllegalArgumentException("Visit the clinic for support.");
        set(t,c,"support-day",s.day);set(t,c,"support-until",s.day+14);
    }
    static int total(TownLife.Town t,TownLife.Citizen c){
        int max=0;for(var k:Kind.values())max=Math.max(max,dependence(t,c,k));return max;
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute!=1260||t.industryStocks.getOrDefault("substance:last-day",0)>=s.day)return;
        t.industryStocks.put("substance:last-day",s.day);
        for(var c:t.people.values()){
            if(!c.alive||c.generationIndex<=0||c.age<18)continue;
            int last=get(t,c,"last-exposure");
            set(t,c,"impairment",Math.max(0,get(t,c,"impairment")-30));
            if(last==s.day)continue;
            boolean help=get(t,c,"support-until")>=s.day;
            int max=total(t,c);
            if(max>=35){
                PsychologyLife.adjust(t,c,"stress",20,help?1:4);
                c.fatigue=Math.min(100,c.fatigue+(help?1:4));
            }
            for(var k:Kind.values()){
                int n=dependence(t,c,k);
                if(n>0)set(t,c,"dependence:"+k.name(),Math.max(0,n-(help?3:1)));
            }
            // Small fictional NPC-only prevalence; never a decision for a player's character.
            if(!RelationshipBoundaryLife.controlled(t,c)&&get(t,c,"support-until")<s.day&&canUse(t,c,s)){
                long seed=c.personalSeed^((long)s.day*0x9E3779B97F4A7C15L);
                if(Math.floorMod(seed,1000)<(max>=35?60:4)){
                    int previous=get(t,c,"last-kind");
                    Kind kind=previous>0&&previous<=Kind.values().length?Kind.values()[previous-1]:Kind.values()[Math.floorMod(seed>>>16,Kind.values().length)];
                    exposure(t,c,s,kind);
                }
            }
        }
    }
    record Category(String name,int dependence){}
    record View(int dependence,int impairment,boolean supportActive,List<Category> history){}
    static View view(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(c==null)return null;var history=new ArrayList<Category>();
        for(var k:Kind.values())if(dependence(t,c,k)>0)history.add(new Category(k.label,dependence(t,c,k)));
        return new View(total(t,c),get(t,c,"impairment"),get(t,c,"support-until")>=s.day,List.copyOf(history));
    }
}
