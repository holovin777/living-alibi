package com.holovin.npclife;

import java.util.SplittableRandom;

/** Private identity for an unlinked visitor; born lives use their real resident data. */
final class PassportLife {
    private PassportLife(){}
    private static final String[] FIRST={"Alessandro","Lorenzo","Matteo","Andrea","Luca","Davide","Marco","Federico","Giulia","Elena","Sofia","Chiara","Anna","Martina","Sara","Alice"};
    private static final String[] LAST={"Moretti","Rossi","Ferrari","Romano","Ricci","Colombo","Gallo","Conti","Rinaldi","Marino","Greco","Fontana","Costa","Villa","Sala","Lombardi"};
    private static final int OFFSET=1000000;
    private static String key(int life,String field){return "passport:"+life+":"+field;}
    private static int get(TownLife.Town t,int life,String field){return t.industryStocks.getOrDefault(key(life,field),0);}
    private static void put(TownLife.Town t,int life,String field,int value){t.industryStocks.put(key(life,field),value);}
    static boolean ensure(TownLife.Town t,WorldState s){
        if(RebirthLife.active(t))return false;
        int life=t.player.incarnation;
        if(get(t,life,"first")>0)return false;
        var rng=new SplittableRandom(s.rng^((long)life*0x9E3779B97F4A7C15L));
        put(t,life,"first",rng.nextInt(FIRST.length)+1);
        put(t,life,"last",rng.nextInt(LAST.length)+1);
        // Migration of the original visitor assumes entry on world day 1.
        int entry=life==1?1:s.day;
        int age=rng.nextInt(18,36);
        put(t,life,"birth",OFFSET+entry-age*365-rng.nextInt(365));
        return true;
    }
    static String visitorName(TownLife.Town t,int life){
        int a=get(t,life,"first"),b=get(t,life,"last");
        return a>0&&a<=FIRST.length&&b>0&&b<=LAST.length?FIRST[a-1]+" "+LAST[b-1]:null;
    }
    static int visitorAge(TownLife.Town t,int life,int day){
        return visitorName(t,life)==null?-1:Math.max(0,(day-(get(t,life,"birth")-OFFSET))/365);
    }
    record View(int incarnation,String name,String alias,int age,Integer birthYear,Integer birthDayOfYear,
                String birthplace,String occupation,String residence,String parentA,String parentB){}
    private static View card(int incarnation,String name,String alias,int age,Integer born,String place,String job,String home,String a,String b){
        return new View(incarnation,name,alias,age,born==null?null:Math.floorDiv(born-1,365)+1,
                born==null?null:Math.floorMod(born-1,365)+1,place,job,home,a,b);
    }
    static View view(TownLife.Town t,WorldState s){
        int life=t.player.incarnation;
        if(RebirthLife.phase(t)==RebirthLife.CHOICE)return null;
        var c=RebirthLife.child(t);
        if(c!=null&&RebirthLife.active(t)){
            var a=DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-a"));
            var b=DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-b"));
            int born=DemographyLife.get(t,c,"born");
            return card(life,c.name,null,c.age,born>0?born:null,"Vigevano",c.occupation,c.home.label,
                    a==null?null:a.name,b==null?null:b.name);
        }
        var p=t.player;
        String name=visitorName(t,life);
        return card(life,name,p.privateName,visitorAge(t,life,s.day),name==null?null:get(t,life,"birth")-OFFSET,
                "Vigevano",p.jobId,p.ownsHouse?HousingLife.HOUSE_DISTRICT.label:
                HousingLife.active(p,s.day,s.minute)?"Rented room / Camera in affitto":null,null,null);
    }
}
