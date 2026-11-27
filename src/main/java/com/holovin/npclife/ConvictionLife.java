package com.holovin.npclife;

/** Private personal convictions. Family exposure is not a religious commitment.
 * No belief changes ability, legal rights, income, or eligibility for services.
 */
final class ConvictionLife {
    private ConvictionLife(){}
    enum Belief {
        UNDECIDED("Undecided / Indeciso",false),
        CATHOLIC("Catholic / Cattolico",true),
        CHRISTIAN("Other Christian / Altro cristiano",true),
        MUSLIM("Muslim / Musulmano",true),
        JEWISH("Jewish / Ebreo",true),
        BUDDHIST("Buddhist / Buddhista",true),
        HINDU("Hindu / Induista",true),
        OTHER("Other faith / Altra fede",true),
        AGNOSTIC("Agnostic / Agnostico",false),
        ATHEIST("Atheist / Ateo",false);
        final String label;final boolean religious;
        Belief(String label,boolean religious){this.label=label;this.religious=religious;}
    }
    private static String key(TownLife.Citizen c,String f){return "conviction:"+c.id+":"+f;}
    private static int get(TownLife.Town t,TownLife.Citizen c,String f){return t.industryStocks.getOrDefault(key(c,f),0);}
    private static void set(TownLife.Town t,TownLife.Citizen c,String f,int n){t.industryStocks.put(key(c,f),n);}
    private static Belief draw(long seed){
        // Fictional world-generation weights, not statistics about real residents.
        int n=Math.floorMod(seed,100);
        if(n<45)return Belief.CATHOLIC;if(n<55)return Belief.CHRISTIAN;
        if(n<61)return Belief.MUSLIM;if(n<64)return Belief.JEWISH;
        if(n<68)return Belief.BUDDHIST;if(n<71)return Belief.HINDU;if(n<74)return Belief.OTHER;
        return n<87?Belief.AGNOSTIC:Belief.ATHEIST;
    }
    static Belief belief(TownLife.Town t,TownLife.Citizen c){
        if(c==null)return Belief.UNDECIDED;
        int n=get(t,c,"belief");
        if(n>0&&n<=Belief.values().length)return Belief.values()[n-1];
        if(c.age<18||get(t,c,"family-recorded")>0)return Belief.UNDECIDED;
        return draw(c.personalSeed^0x42a8L);
    }
    static void upbringing(TownLife.Town t,TownLife.Citizen child,TownLife.Citizen a,TownLife.Citizen b){
        set(t,child,"family-recorded",1);
        set(t,child,"exposure-a",belief(t,a).ordinal()+1);
        set(t,child,"exposure-b",belief(t,b).ordinal()+1);
        set(t,child,"belief",Belief.UNDECIDED.ordinal()+1);
    }
    private static Belief exposure(TownLife.Town t,TownLife.Citizen c,String field){
        int n=get(t,c,field);return n>0&&n<=Belief.values().length?Belief.values()[n-1]:Belief.UNDECIDED;
    }
    static void choose(TownLife.Town t,TownLife.Citizen c,Belief belief){
        if(c==null||!c.alive||c.age<13||!RebirthLife.awakePresent(c)||c!=RebirthLife.child(t))
            throw new IllegalArgumentException("Personal conviction choice unavailable.");
        set(t,c,"belief",belief.ordinal()+1);
    }
    static boolean canReflect(TownLife.Town t,TownLife.Citizen c,WorldState s){
        return c!=null&&c.alive&&c.age>=13&&RebirthLife.awakePresent(c)
            &&(HomeRoutineLife.atHome(c)||c.place==TownLife.Place.PARK||c.place==TownLife.Place.LIBRARY)
            &&get(t,c,"reflection-day")<s.day;
    }
    static void reflect(TownLife.Town t,TownLife.Citizen c,WorldState s){
        if(!canReflect(t,c,s))throw new IllegalArgumentException("Find a quiet place to reflect.");
        set(t,c,"reflection-day",s.day);
        set(t,c,"reflections",Math.addExact(get(t,c,"reflections"),1));
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute!=360)return;
        for(var c:t.people.values()){
            if(!c.alive||c.generationIndex<=0||c.age<18||RelationshipBoundaryLife.controlled(t,c))continue;
            if(get(t,c,"last-review")>=s.day)continue;
            boolean undecided=belief(t,c)==Belief.UNDECIDED;
            if(!undecided&&Math.floorMod(s.day+c.generationIndex,365)!=0)continue;
            set(t,c,"last-review",s.day);
            long seed=c.personalSeed^((long)s.day*0x9E3779B97F4A7C15L);
            if(!undecided&&Math.floorMod(seed,100)>=3)continue;
            Belief next=draw(seed);
            if(undecided&&Math.floorMod(seed>>>12,100)<55){
                Belief family=exposure(t,c,Math.floorMod(seed,2)==0?"exposure-a":"exposure-b");
                if(family!=Belief.UNDECIDED)next=family;
            }
            set(t,c,"belief",next.ordinal()+1);
        }
    }
    record View(String belief,String upbringingA,String upbringingB,int reflections){}
    static View view(TownLife.Town t,TownLife.Citizen c){
        return c==null?null:new View(belief(t,c).label,exposure(t,c,"exposure-a").label,
                exposure(t,c,"exposure-b").label,get(t,c,"reflections"));
    }
}
