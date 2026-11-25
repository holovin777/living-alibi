package com.holovin.npclife;

/**
 * Reproducible fictional local weather derived from authoritative game time.
 *
 * No wall-clock time, external service, paid AI or additional database state:
 * the same saved day and minute always reconstruct the same weather.
 */
final class WeatherLife {
    private WeatherLife() { }

    enum Sky { CLEAR, CLOUDY, RAIN, SNOW }

    record Conditions(CalendarLife.Season season,Sky sky,int temperatureC) {
        boolean raining() {return sky==Sky.RAIN;}
        boolean snowing() {return sky==Sky.SNOW;}
    }

    static Conditions at(int day,int minute) {
        if(day<1 || minute<0 || minute>=1440)
            throw new IllegalArgumentException("Valid game date and minute required.");
        var season=CalendarLife.season(day);
        int pattern=(int)Math.floorMod(day*7L+5,12);
        Sky sky=switch(season) {
            case SPRING -> pattern<4?Sky.RAIN:pattern<8?Sky.CLOUDY:Sky.CLEAR;
            case SUMMER -> pattern<2?Sky.RAIN:pattern<6?Sky.CLOUDY:Sky.CLEAR;
            case AUTUMN -> pattern<5?Sky.RAIN:pattern<9?Sky.CLOUDY:Sky.CLEAR;
            case WINTER -> pattern<6?Sky.SNOW:pattern<9?Sky.CLOUDY:Sky.CLEAR;
        };
        int average=switch(season) {
            case SPRING -> 13;
            case SUMMER -> 26;
            case AUTUMN -> 11;
            case WINTER -> 0;
        };
        int dailyVariation=(int)Math.floorMod(day*13L,9)-4;
        int hour=minute/60;
        int timeShift=hour<6 || hour>=21 ? -5 : hour>=12 && hour<18 ? 3 : 0;
        int precipitationCooling=switch(sky) {
            case RAIN -> -2;
            case SNOW -> -3;
            default -> 0;
        };
        return new Conditions(season,sky,average+dailyVariation+timeShift+
                precipitationCooling);
    }

    /**
     * Additional *game-balance* health risk for exposed sleeping spots.
     * Re-evaluates the climate each hour, including midnight/year changes.
     * A covered public approach stays sheltered; it is not a private home.
     */
    static int sleepPenalty(TownLife.Place place,int day,int minute,int hours) {
        if(day<1 || minute<0 || minute>=1440 || hours<1 || hours>12)
            throw new IllegalArgumentException("Invalid sleep date or duration.");
        int exposure=PlayerLife.exposure(place);
        if(exposure==0)return 0;
        long start=((long)day-1)*1440+minute;
        int weightedRisk=0;
        for(int i=0;i<hours;i++) {
            long midpoint=start+i*60L+30;
            int sampleDay=(int)(midpoint/1440)+1;
            int sampleMinute=(int)(midpoint%1440);
            var weather=at(sampleDay,sampleMinute);
            int risk=weather.raining()?1:weather.snowing()?2:0;
            if(weather.temperatureC()<=0)risk+=2;
            else if(weather.temperatureC()<8 || weather.temperatureC()>=33)risk++;
            weightedRisk+=exposure>=2?risk:risk/2;
        }
        return weightedRisk/2;
    }
}
