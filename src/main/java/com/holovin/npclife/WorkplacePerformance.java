package com.holovin.npclife;

/** Hourly observations of actual workplace presence, not completed work orders.
 * Scores are private, bounded and persisted in the existing stock map.
 * Absence is not a failed task, and no money or production is created here. */
final class WorkplacePerformance {
    private WorkplacePerformance(){}
    record Report(int samples,int quality,int pace,int needsReview){}
    static String key(TownLife.Citizen c,String field){
        return "workplace-performance:"+c.id+":"+field;
    }
    private static int get(TownLife.Town t,TownLife.Citizen c,String f){
        return t.industryStocks.getOrDefault(key(c,f),0);
    }
    private static void set(TownLife.Town t,TownLife.Citizen c,String f,int value){
        t.industryStocks.put(key(c,f),value);
    }
    private static int clamp(int x){return Math.max(0,Math.min(100,x));}
    private static boolean sameEmployment(TownLife.Town t,TownLife.Citizen c){
        return get(t,c,"job-changes")==c.jobChanges
                &&get(t,c,"hired")==c.lastHiredDay
                &&get(t,c,"role")==(c.occupation.hashCode() & Integer.MAX_VALUE)
                &&get(t,c,"site")==c.workplace.ordinal()+1;
    }
    static Report report(TownLife.Town t,TownLife.Citizen c){
        if(!WorkplaceHierarchy.eligible(c)||!sameEmployment(t,c))return new Report(0,0,0,0);
        return new Report(get(t,c,"samples"),get(t,c,"quality"),get(t,c,"pace"),get(t,c,"review"));
    }
    static void tick(TownLife.Town t,WorldState s){
        if(s.minute%60!=0)return;
        for(var c:t.people.values()){
            if(!WorkplaceHierarchy.onDuty(c,s.day)||RebirthLife.linkedAdult(t,c)
                    ||TownLife.inPlayerConversation(t,c))continue;
            if(!sameEmployment(t,c)){
                for(String field:new String[]{"samples","quality","pace","review","day","minute"})set(t,c,field,0);
                set(t,c,"job-changes",c.jobChanges);set(t,c,"hired",c.lastHiredDay);
                set(t,c,"role",(c.occupation.hashCode() & Integer.MAX_VALUE));set(t,c,"site",c.workplace.ordinal()+1);
            }
            if(get(t,c,"day")==s.day&&get(t,c,"minute")==s.minute)continue;
            int skill=WorkplaceHierarchy.skill(t,c);
            int needs=(c.hunger+c.thirst)/2;
            int quality=clamp(35+skill/2+c.carefulness/3-c.fatigue/4-needs/5-(100-c.health)/5);
            int pace=clamp(55+skill/2-c.fatigue/3-needs/4-(100-c.health)/4);
            int samples=get(t,c,"samples"),weight=Math.min(samples,19);
            set(t,c,"quality",(get(t,c,"quality")*weight+quality)/(weight+1));
            set(t,c,"pace",(get(t,c,"pace")*weight+pace)/(weight+1));
            set(t,c,"samples",Math.min(20,samples+1));
            set(t,c,"review",quality<45?Math.min(20,get(t,c,"review")+1):Math.max(0,get(t,c,"review")-1));
            set(t,c,"day",s.day);set(t,c,"minute",s.minute);
        }
    }
}
