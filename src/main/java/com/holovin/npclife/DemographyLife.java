package com.holovin.npclife;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

/** Deterministic generations; historical resident IDs are never reused. */
final class DemographyLife {
    private DemographyLife(){}
    static final String LAST="demography:last-day", BIRTHS="demography:births", DEATHS="demography:deaths",
            PENSIONS="demography:pensions", INHERITED="demography:inherited";
    static final int CHILD_DAILY_CASH=18, WEEKLY_PENSION=35;
    static final List<String> FIELDS=List.of("parent-a","parent-b","partner","union-day","born","last-birth","children","estate","played-incarnation");
    static String key(TownLife.Citizen c,String field){return "demography:"+c.id+":"+field;}
    static int get(TownLife.Town t,String k){return t.industryStocks.getOrDefault(k,0);}
    static void set(TownLife.Town t,String k,int n){t.industryStocks.put(k,n);}
    static int get(TownLife.Town t,TownLife.Citizen c,String f){return get(t,key(c,f));}
    static void set(TownLife.Town t,TownLife.Citizen c,String f,int n){set(t,key(c,f),n);}
    static void ensure(TownLife.Town t){
        for(String k:List.of(LAST,BIRTHS,DEATHS,PENSIONS,INHERITED))t.industryStocks.putIfAbsent(k,0);
        for(var c:t.people.values())if(c.generationIndex>0)
            for(String f:FIELDS)t.industryStocks.putIfAbsent(key(c,f),0);
    }
    static TownLife.Citizen byIndex(TownLife.Town t,int n){
        if(n<=0)return null;
        return t.people.values().stream().filter(c->c.generationIndex==n).findFirst().orElse(null);
    }
    static boolean child(TownLife.Citizen c){return c.generationIndex>0&&c.age<18;}
    static boolean workingAge(TownLife.Citizen c){return !child(c)&&!"Retired".equals(c.occupation);}
    static long living(TownLife.Town t){return t.people.values().stream().filter(c->c.generationIndex>0&&c.alive).count();}
    static boolean parentAge(TownLife.Citizen c){return c.alive&&c.generationIndex>0&&c.age>=21&&c.age<=42;}
    static Set<Integer> ancestors(TownLife.Town t,TownLife.Citizen c){
        Set<Integer> result=new HashSet<>();
        collect(t,c,result);
        return result;
    }
    private static void collect(TownLife.Town t,TownLife.Citizen c,Set<Integer> result){
        if(c==null||!result.add(c.generationIndex))return;
        collect(t,byIndex(t,get(t,c,"parent-a")),result);
        collect(t,byIndex(t,get(t,c,"parent-b")),result);
    }
    static boolean canPair(TownLife.Town t,TownLife.Citizen a,TownLife.Citizen b){
        if(a==null||b==null||a==b||!parentAge(a)||!parentAge(b)||get(t,a,"partner")!=0||get(t,b,"partner")!=0)return false;
        var relation=t.peerRelationships.get(SocialNetwork.key(a.id,b.id));
        if(relation==null||relation.trust<60||relation.affinity<65||relation.tension>25)return false;
        return java.util.Collections.disjoint(ancestors(t,a),ancestors(t,b));
    }
    static boolean pair(TownLife.Town t,TownLife.Citizen a,TownLife.Citizen b,int day){
        if(!canPair(t,a,b))return false;
        set(t,a,"partner",b.generationIndex);set(t,b,"partner",a.generationIndex);
        set(t,a,"union-day",day);set(t,b,"union-day",day);
        return true;
    }
    static boolean canBirth(TownLife.Town t,TownLife.Citizen a,TownLife.Citizen b,int day){
        if(a==null||b==null||a==b||!parentAge(a)||!parentAge(b)||get(t,a,"partner")!=b.generationIndex||get(t,b,"partner")!=a.generationIndex||
                day-get(t,a,"union-day")<270||get(t,a,"children")>=2||get(t,b,"children")>=2||
                (get(t,a,"last-birth")>0&&day-get(t,a,"last-birth")<1095)||
                (get(t,b,"last-birth")>0&&day-get(t,b,"last-birth")<1095)||
                a.money<100||b.money<100||living(t)>=PopulationGenerator.MAX_GENERATED)return false;
        return t.people.values().stream().mapToInt(c->c.generationIndex).max().orElse(0)<9999;
    }
    static TownLife.Citizen birth(TownLife.Town t,TownLife.Citizen a,TownLife.Citizen b,int day){
        if(!canBirth(t,a,b,day))return null;
        int next=t.people.values().stream().mapToInt(c->c.generationIndex).max().orElse(0)+1;
        if(next>9999)return null;
        String id="resident-%04d".formatted(next);
        String[] names={"Anna","Lorenzo","Mia","Tommaso","Emma","Matteo","Giulia","Leo"};
        String surname=a.name.substring(a.name.lastIndexOf(' ')+1);
        var c=new TownLife.Citizen(id,names[Math.floorMod(next,names.length)]+" "+surname,"Child",a.home);
        c.age=0;c.money=20;c.food=2;c.water=2;c.employed=false;c.occupation="Child";c.activity="AT_HOME";
        c.generationIndex=next;c.personalSeed=a.personalSeed^Long.rotateLeft(b.personalSeed,17)^next;
        c.sociability=GenealogyLife.inheritedTrait(a.sociability,b.sociability,c.personalSeed);
        c.carefulness=GenealogyLife.inheritedTrait(a.carefulness,b.carefulness,Long.rotateLeft(c.personalSeed,21));
        c.curiosity=GenealogyLife.inheritedTrait(a.curiosity,b.curiosity,Long.rotateLeft(c.personalSeed,43));
        c.goal="FIND_BALANCE";
        c.home=a.home;c.homeUnit=a.homeUnit;c.workplace=TownLife.Place.SCHOOL;
        a.money-=10;b.money-=10;t.people.put(id,c);
        ensure(t);
        EducationLife.ensureResident(t,c);
        set(t,c,"parent-a",a.generationIndex);set(t,c,"parent-b",b.generationIndex);set(t,c,"born",day);
        for(var p:List.of(a,b)){set(t,p,"children",get(t,p,"children")+1);set(t,p,"last-birth",day);}
        set(t,BIRTHS,Math.addExact(get(t,BIRTHS),1));
        ArchiveLife.record(t,"birth:"+c.id,day,360,"Population",c.name+" was born.");
        return c;
    }
    static TownLife.Citizen guardian(TownLife.Town t,TownLife.Citizen c){
        var a=byIndex(t,get(t,c,"parent-a"));var b=byIndex(t,get(t,c,"parent-b"));
        if(a!=null&&a.alive)return a;
        return b!=null&&b.alive?b:null;
    }
    static void support(TownLife.Town t,TownLife.Citizen c){
        if(c.money>=CHILD_DAILY_CASH)return;
        for(String f:List.of("parent-a","parent-b")){
            var p=byIndex(t,get(t,c,f));
            if(p!=null&&p.alive){
                int n=Math.min(CHILD_DAILY_CASH-c.money,Math.max(0,p.money-20));
                p.money-=n;c.money+=n;
            }
        }
        if(c.money<CHILD_DAILY_CASH){
            int city=t.employerFunds.getOrDefault(MunicipalLife.CITY,0);
            int n=Math.min(CHILD_DAILY_CASH-c.money,Math.max(0,city-MunicipalLife.RESERVE));
            t.employerFunds.put(MunicipalLife.CITY,city-n);c.money+=n;
        }
    }
    static void estate(TownLife.Town t,TownLife.Citizen c){
        if(c.alive||get(t,c,"estate")>0)return;
        var heirs=t.people.values().stream().filter(x->x.alive&&(get(t,x,"parent-a")==c.generationIndex||get(t,x,"parent-b")==c.generationIndex)).toList();
        ITBusinessLife.liquidate(t,c);
        HomeMarketLife.estate(t,c);
        FamilyBudgetLife.releaseEstate(t,c);
        int amount=c.money;
        if(!heirs.isEmpty()){
            int share=amount/heirs.size(),remainder=amount%heirs.size();
            for(int i=0;i<heirs.size();i++){
                var heir=heirs.get(i);heir.money=Math.addExact(heir.money,share+(i<remainder?1:0));
            }
            set(t,INHERITED,Math.addExact(get(t,INHERITED),amount));
        }else t.employerFunds.merge(MunicipalLife.CITY,amount,Math::addExact);
        c.money=0;c.employed=false;set(t,c,"estate",1);
        var partner=byIndex(t,get(t,c,"partner"));
        if(partner!=null&&get(t,partner,"partner")==c.generationIndex){
            FamilyBudgetLife.releaseEstate(t,partner);
            set(t,partner,"partner",0);
        }
        set(t,DEATHS,Math.addExact(get(t,DEATHS),1));
    }
    static void tick(TownLife.Town t,WorldState clock,List<TownLife.Event> events,List<TownLife.Observation> observations){
        int day=clock.day;
        if(clock.minute!=360||get(t,LAST)>=day)return;
        set(t,LAST,day);ensure(t);
        var residents=t.people.values().stream().filter(c->c.generationIndex>0).toList();
        for(var c:residents){
            if(c.alive){
                int born=get(t,c,"born");
                if((born>0&&day>born&&(day-born)%365==0)||(born==0&&day>1&&(day-1)%365==0)){
                    c.age++;
                    if(c.age==18){
                        EducationLife.graduate(t,c);
                        ArchiveLife.record(t,"adulthood:"+c.id,day,360,"Population",c.name+" reached adulthood.");
                        c.occupation="Job seeker";c.employed=false;c.unemploymentDays=0;
                        RentalLife.moveHome(t,c,c.home,TownLife.privateUnit(c.id));
                    }
                    if(c.age>=67&&!"Retired".equals(c.occupation)){
                        c.occupation="Retired";c.employed=false;
                    }
                    if(c.age>=82+Math.floorMod(c.personalSeed,19)){
                        c.alive=false;c.activity="DECEASED";
                    }
                }
                if(c.alive&&child(c)){
                    var guardian=guardian(t,c);
                    if(guardian!=null&&(c.home!=guardian.home||!c.homeUnit.equals(guardian.homeUnit)))
                        RentalLife.moveHome(t,c,guardian.home,guardian.homeUnit);
                    support(t,c);
                }
                // Pension claims are settled together below so the first
                // resident in insertion order cannot consume the whole fund.
            }
            if(!c.alive&&get(t,c,"estate")==0)
                ArchiveLife.record(t,"death:"+c.id,day,360,"Population",c.name+" was recorded as deceased.");
            estate(t,c);
        }
        PensionLife.tick(t,clock);
        if((day-1)%30!=0)return;
        for(int i=0;i<residents.size();i++){
            var a=residents.get(i);
            if(!parentAge(a))continue;
            for(int j=i+1;j<residents.size()&&get(t,a,"partner")==0;j++){
                var candidate=residents.get(j);
                if(!RebirthLife.linkedAdult(t,a)&&!RebirthLife.linkedAdult(t,candidate))pair(t,a,candidate,day);
            }
            var b=byIndex(t,get(t,a,"partner"));
            if(b!=null&&a.generationIndex<b.generationIndex&&!RebirthLife.linkedAdult(t,a)&&!RebirthLife.linkedAdult(t,b)){
                var newborn=birth(t,a,b,day);
                if(newborn!=null)TownLife.publish(t,clock,a.id,a.place,a.name+" welcomed a new family member.",events,observations);
            }
        }
    }
    record Report(int alive,int children,int workingAge,int retired,int births,int deaths,int pensions,int inherited,int capacity){}
    static Report report(TownLife.Town t){
        return new Report((int)living(t),(int)t.people.values().stream().filter(c->c.alive&&child(c)).count(),
            (int)t.people.values().stream().filter(c->c.alive&&c.generationIndex>0&&workingAge(c)).count(),
            (int)t.people.values().stream().filter(c->c.alive&&"Retired".equals(c.occupation)).count(),
            get(t,BIRTHS),get(t,DEATHS),get(t,PENSIONS),get(t,INHERITED),PopulationGenerator.MAX_GENERATED);
    }
}
