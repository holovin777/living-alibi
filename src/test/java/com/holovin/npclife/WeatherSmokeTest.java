package com.holovin.npclife;

/** The same world time produces the same weather, even after future restarts. */
public final class WeatherSmokeTest {
    private WeatherSmokeTest() { }

    public static void main(String[] args) {
        var first=WeatherLife.at(1,450);
        check(first.equals(WeatherLife.at(1,450)),"weather is deterministic");
        check(first.season()==CalendarLife.Season.SPRING,"fresh world begins in spring");
        check(WeatherLife.at(366,450).season()==CalendarLife.Season.SPRING,
                "weather calendar keeps advancing into year two");

        int rainy=find(1,92,WeatherLife.Sky.RAIN);
        int dry=find(1,92,WeatherLife.Sky.CLEAR);
        int snowy=find(276,365,WeatherLife.Sky.SNOW);
        int summer=find(93,185,WeatherLife.Sky.CLEAR);
        check(rainy>0 && dry>0 && snowy>0 && summer>0,
                "all expected seasonal weather patterns are reachable");
        check(WeatherLife.at(snowy,720).temperatureC()<
                        WeatherLife.at(summer,720).temperatureC(),
                "winter cold is stronger than summer heat");
        check(WeatherLife.sleepPenalty(TownLife.Place.WOODS,snowy,1320,8)>
                        WeatherLife.sleepPenalty(TownLife.Place.WOODS,summer,1320,8),
                "outdoor winter sleep is more dangerous than summer");
        check(WeatherLife.sleepPenalty(TownLife.Place.CAFE,snowy,1320,8)==0,
                "covered public approach protects against extra weather exposure");

        var town=TownLife.create(new WorldState());
        var puddle=town.naturalResources.get("puddle-water");
        puddle.stock=0;
        NatureLife.tick(town,dry,0);
        check(puddle.stock==0,"clear day does not magically refill a puddle");
        NatureLife.tick(town,rainy,0);
        check(puddle.stock==3,"rain provides finite real puddle water");
        NatureLife.tick(town,rainy,0);
        check(puddle.stock==5,"rain cannot exceed puddle carrying capacity");

        var berries=town.naturalResources.get("wild-berries");
        berries.stock=0;
        NatureLife.tick(town,snowy,0);
        check(berries.stock==0,"winter prevents wild berry growth");
        NatureLife.tick(town,summer,0);
        check(berries.stock==3,"summer produces more wild berries");
        var river=town.naturalResources.get("river-water");
        river.stock=100;
        NatureLife.tick(town,snowy,0);
        check(river.stock==120,"river abstraction replenishes without exceeding capacity");

        var a=new PlayerLife.Player();
        a.place=TownLife.Place.WOODS;
        a.destination=TownLife.Place.WOODS;
        a.health=70;
        var b=a.copy();
        var cold=PlayerLife.sleep(a,8,snowy,1320);
        var warm=PlayerLife.sleep(b,8,summer,1320);
        check(cold.healthChange()<warm.healthChange(),
                "identical visitors suffer more while sleeping in winter");
        check(a.contacts.isEmpty() && b.contacts.isEmpty(),
                "weather does not reveal private identities");

        boolean invalid=false;
        try {WeatherLife.at(0,0);}catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,"invalid world day rejected");
        System.out.println("WeatherSmokeTest: ALL CHECKS PASSED");
    }

    private static int find(int from,int to,WeatherLife.Sky desired) {
        for(int day=from;day<=to;day++)
            if(WeatherLife.at(day,720).sky()==desired)return day;
        return -1;
    }

    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError(label);
    }
}
