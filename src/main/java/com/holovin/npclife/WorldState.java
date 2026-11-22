package com.holovin.npclife;

/** Mutable, persisted snapshot of exactly one person. Game units are fictional, not clinical. */
final class WorldState {
    int day = 1;
    int minute = 450;
    int age = 36;
    int friendship = 0;
    int hunger = 25;
    int thirst = 20;
    int fatigue = 20;
    int health = 100;
    int money = 45;
    int food = 2;
    int water = 2;
    int workMinutes = 0;
    String activity = "WORKING";

    int stress = 18;
    int loneliness = 24;
    int mood = 70;
    int hygiene = 80;
    int illness = 0;
    int debt = 0;
    int fitness = 60;
    int personalitySociable = 57;
    int personalityCareful = 68;
    int personalityCurious = 74;
    int daysWorked = 0;
    int missedRent = 0;
    int daysSick = 0;
    boolean alive = true;
    boolean ownsBakery = false;
    String goal = "OPEN_BAKERY";
    long rng = 171127L;
    long lastSavedEpoch = 0;
    boolean offlineProgress = false;
    boolean aiAutonomy = false;
    int speed = 1;

    WorldState copy() {
        WorldState s = new WorldState();
        s.day = day; s.minute = minute; s.age = age; s.friendship = friendship;
        s.hunger = hunger; s.thirst = thirst; s.fatigue = fatigue; s.health = health;
        s.money = money; s.food = food; s.water = water; s.workMinutes = workMinutes;
        s.activity = activity; s.stress = stress; s.loneliness = loneliness;
        s.mood = mood; s.hygiene = hygiene; s.illness = illness; s.debt = debt;
        s.fitness = fitness; s.personalitySociable = personalitySociable;
        s.personalityCareful = personalityCareful; s.personalityCurious = personalityCurious;
        s.daysWorked = daysWorked; s.missedRent = missedRent; s.daysSick = daysSick;
        s.alive = alive; s.ownsBakery = ownsBakery; s.goal = goal; s.rng = rng;
        s.lastSavedEpoch = lastSavedEpoch; s.offlineProgress = offlineProgress;
        s.aiAutonomy = aiAutonomy; s.speed = speed;
        return s;
    }
}
