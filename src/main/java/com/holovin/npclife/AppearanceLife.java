package com.holovin.npclife;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

/** Fictional polygenic appearance approximation, derived from persistent family links.
 * Traits are stable across reloads; no association with ability or personality.
 */
final class AppearanceLife {
    private AppearanceLife(){}
    record Profile(int skin,int hair,int eye,int height,int build,int face,int nose){}
    static Profile profile(TownLife.Town t,TownLife.Citizen c){
        return profile(t,c,new HashMap<>(),new HashSet<>());
    }
    private static Profile founder(TownLife.Citizen c){
        var rng=new SplittableRandom(c.personalSeed^((long)c.id.hashCode()*0x9E3779B97F4A7C15L));
        return new Profile(rng.nextInt(5),rng.nextInt(5),rng.nextInt(4),rng.nextInt(86,111),
                rng.nextInt(80,121),rng.nextInt(90,111),rng.nextInt(85,116));
    }
    private static int blend(int a,int b,int variation,int low,int high,SplittableRandom rng){
        int value=(a+b)/2+rng.nextInt(-variation,variation+1);
        return Math.max(low,Math.min(high,value));
    }
    private static Profile profile(TownLife.Town t,TownLife.Citizen c,Map<String,Profile> memo,Set<String> visiting){
        if(c==null)return null;
        if(memo.containsKey(c.id))return memo.get(c.id);
        if(!visiting.add(c.id))return founder(c);
        var a=DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-a"));
        var b=DemographyLife.byIndex(t,DemographyLife.get(t,c,"parent-b"));
        var first=profile(t,a,memo,visiting);var second=profile(t,b,memo,visiting);
        var rng=new SplittableRandom(c.personalSeed^((long)c.generationIndex*0xD1B54A32D192ED03L));
        Profile result;
        if(first==null&&second==null)result=founder(c);
        else {
            if(first==null)first=founder(c);
            if(second==null)second=founder(c);
            result=new Profile(blend(first.skin,second.skin,1,0,4,rng),
                    rng.nextBoolean()?first.hair:second.hair,rng.nextBoolean()?first.eye:second.eye,
                    blend(first.height,second.height,5,80,120,rng),blend(first.build,second.build,6,75,125,rng),
                    blend(first.face,second.face,4,85,115,rng),blend(first.nose,second.nose,5,80,120,rng));
        }
        visiting.remove(c.id);memo.put(c.id,result);return result;
    }
}
