package com.holovin.npclife;

/** Deterministic calendar shared by the engine, Spring API, and future Godot view. */
final class CalendarLife {
    private CalendarLife() { }

    enum Season { SPRING, SUMMER, AUTUMN, WINTER }

    static int dayOfYear(int worldDay) {
        if(worldDay<1)throw new IllegalArgumentException("Game day starts at 1.");
        return (worldDay-1)%365+1;
    }

    /** The world begins in spring; year 1 and later years have 365 days. */
    static Season season(int worldDay) {
        int day=dayOfYear(worldDay);
        if(day<=92)return Season.SPRING;
        if(day<=185)return Season.SUMMER;
        if(day<=275)return Season.AUTUMN;
        return Season.WINTER;
    }

    static int year(int worldDay) {
        if(worldDay<1)throw new IllegalArgumentException("Game day starts at 1.");
        return (worldDay-1)/365+1;
    }
}
