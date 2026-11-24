package com.holovin.npclife;

/** Seasons are stable across world years and require no existing save. */
public final class CalendarSmokeTest {
    private CalendarSmokeTest() { }

    public static void main(String[] args) {
        check(CalendarLife.season(1)==CalendarLife.Season.SPRING,"new world starts in spring");
        check(CalendarLife.season(92)==CalendarLife.Season.SPRING,"spring last day");
        check(CalendarLife.season(93)==CalendarLife.Season.SUMMER,"summer starts");
        check(CalendarLife.season(185)==CalendarLife.Season.SUMMER,"summer last day");
        check(CalendarLife.season(186)==CalendarLife.Season.AUTUMN,"autumn starts");
        check(CalendarLife.season(275)==CalendarLife.Season.AUTUMN,"autumn last day");
        check(CalendarLife.season(276)==CalendarLife.Season.WINTER,"winter starts");
        check(CalendarLife.season(365)==CalendarLife.Season.WINTER,"winter last day");
        check(CalendarLife.season(366)==CalendarLife.Season.SPRING,
                "spring begins again without resetting the world");
        check(CalendarLife.year(366)==2 && CalendarLife.dayOfYear(366)==1,
                "year rollover without changing global day");
        boolean rejected=false;
        try {CalendarLife.season(0);} catch(IllegalArgumentException ignored) {rejected=true;}
        check(rejected,"invalid days rejected");
        System.out.println("CalendarSmokeTest: ALL CHECKS PASSED");
    }

    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError(label);
    }
}
