package com.holovin.npclife;

import java.util.ArrayList;
import java.util.List;

/** Pure simulation logic: time, physical state, money and feasible autonomous actions. */
final class LifeSimulation {
    record LifeEvent(int day, int minute, String text) { }
    private LifeSimulation() { }
    private static int clamp(int n) { return Math.max(0, Math.min(100, n)); }

    // Persisted pseudo-random seed makes each world independent and reproducible across restarts.
    private static int dice(WorldState s, int bound) {
        s.rng = (s.rng * 6364136223846793005L + 1442695040888963407L);
        return (int) Long.remainderUnsigned(s.rng >>> 1, bound);
    }

    static List<LifeEvent> advance(WorldState s, long minutes) {
        if (minutes < 0 || minutes > 525_600L) {
            throw new IllegalArgumentException("Advance must be between 0 and 525600 minutes (one game year).");
        }
        List<LifeEvent> changes = new ArrayList<>();
        for (long i = 0; i < minutes; i++) tick(s, changes);
        return changes;
    }

    private static void tick(WorldState s, List<LifeEvent> log) {
        s.minute++;
        if (s.minute >= 1440) {
            s.minute -= 1440;
            s.day++;
            s.workMinutes = 0;
            daily(s, log);
        }
        if (!s.alive) return; // the world clock continues, but Marco cannot perform actions
        if (s.minute % 10 == 0) {
            s.hunger = clamp(s.hunger + 1);
            s.thirst = clamp(s.thirst + 1);
            s.hygiene = clamp(s.hygiene - 1);
        }
        if (s.minute % 15 == 0 && !s.activity.equals("SLEEPING")) {
            s.fatigue = clamp(s.fatigue + 1);
        }
        if (s.minute % 60 == 0) {
            s.loneliness = clamp(s.loneliness + 1);
            if (s.hunger > 93 || s.thirst > 93) s.health = clamp(s.health - (s.thirst > 93 ? 2 : 1));
            if (s.fatigue > 94) s.stress = clamp(s.stress + 2);
            if (s.illness > 30) s.health = clamp(s.health - Math.max(1, s.illness / 35));
            if (s.debt > 500) s.stress = clamp(s.stress + 1);
            if (s.hunger < 65 && s.thirst < 60 && s.fatigue < 70 && s.illness < 35) {
                s.health = clamp(s.health + (s.minute % 180 == 0 ? 1 : 0));
            }
            s.mood = clamp(90 - s.stress / 2 - s.loneliness / 3 - s.illness / 4);
        }
        if (s.health <= 0) { die(s, log, "complications from declining health"); return; }
        if (s.thirst >= 65) {
            if (s.water > 0) { s.water--; s.thirst = clamp(s.thirst - 52); change(s,"DRINKING",log); add(s,log,"Marco drank water."); return; }
            if (shopOpen(s.minute) && buyWater(s,log)) return;
            change(s,"LOOKING_FOR_WATER",log); return;
        }
        if (s.hunger >= 65) {
            if (s.food > 0) { s.food--; s.hunger = clamp(s.hunger - 51); change(s,"EATING",log); add(s,log,"Marco ate a meal."); return; }
            if (shopOpen(s.minute) && buyFood(s,log)) return;
            change(s,"LOOKING_FOR_FOOD",log); return;
        }
        boolean night = s.minute < 360 || s.minute >= 1320;
        if (night || s.fatigue >= 83) {
            change(s,"SLEEPING",log);
            if (s.minute % 5 == 0) s.fatigue = clamp(s.fatigue - 2);
            return;
        }
        if (s.illness >= 45) {
            if (shopOpen(s.minute) && s.money >= 30 && s.illness >= 65) {
                s.money -= 30; s.illness = Math.max(0,s.illness-25);
                add(s,log,"Marco visited a doctor and paid EUR 30.");
            }
            change(s,"RESTING_WHILE_SICK",log);
            if (s.minute % 60 == 0) s.fatigue = clamp(s.fatigue-2);
            return;
        }
        if (shopOpen(s.minute)) {
            if (s.food <= 1 && buyFood(s,log)) return;
            if (s.water <= 1 && buyWater(s,log)) return;
        }
        if (s.minute % 360 == 0 && s.hygiene < 55) {
            s.hygiene = clamp(s.hygiene + 45);
            change(s,"WASHING",log);
            add(s,log,"Marco took care of his hygiene.");
            return;
        }
        boolean workingTime = (s.day - 1) % 7 < 5 && s.minute >= 420 && s.minute < 960;
        if (workingTime && s.fatigue < 79 && s.stress < 90) {
            change(s,"WORKING",log);
            s.workMinutes++;
            if (s.workMinutes == 1) s.daysWorked++;
            if (s.workMinutes % 60 == 0) {
                int wage = s.ownsBakery ? 17 : 12;
                s.money += wage;
                add(s,log,"Marco earned EUR " + wage + " at the bakery.");
            }
            return;
        }
        // Goal-oriented but non-scripted leisure; personality affects how often he socializes.
        if (s.minute >= 960 && s.minute < 1200) {
            if (s.loneliness > (s.goal.equals("MAKE_FRIENDS") ? 35 : 90 - s.personalitySociable / 2)) {
                change(s,"SOCIALIZING",log);
                if (s.minute % 20 == 0) s.loneliness = clamp(s.loneliness - 8);
            } else if (s.goal.equals("IMPROVE_HEALTH")) {
                change(s,"WALKING_FOR_HEALTH",log);
                if (s.minute % 30 == 0) {s.stress = clamp(s.stress - 2); s.fitness=clamp(s.fitness+1);}
            } else if (s.personalityCurious >= 55 && s.minute < 1080) {
                change(s,"EXPLORING_TOWN",log);
                if (s.minute % 30 == 0) s.stress = clamp(s.stress - 2);
            } else {
                change(s,"RELAXING_AT_HOME",log);
            }
            if (s.minute % 60 == 0) { s.fatigue = clamp(s.fatigue-1); s.stress=clamp(s.stress-1); }
        } else {
            change(s,"RELAXING_AT_HOME",log);
            if (s.minute % 60 == 0) s.stress=clamp(s.stress-1);
        }
    }

    private static void daily(WorldState s, List<LifeEvent> log) {
        if (!s.alive) return;
        if ((s.day - 1) % 365 == 0 && s.day > 1) {
            s.age++;
            add(s,log,"Marco turned " + s.age + ".");
        }
        if (s.day > 1 && (s.day - 1) % 30 == 0) {
            int rent = s.ownsBakery ? 440 : 360;
            if (s.money >= rent) { s.money -= rent; add(s,log,"Marco paid EUR " + rent + " in monthly expenses."); }
            else { s.debt += rent-s.money; s.money=0; s.missedRent++; s.stress=clamp(s.stress+16);
                add(s,log,"Marco could not cover his monthly expenses and accumulated debt."); }
        }
        if (s.debt > 0 && s.money >= 80) {
            int payment = Math.min(s.debt,Math.max(20,s.money/3));
            s.debt-=payment; s.money-=payment; add(s,log,"Marco repaid EUR " + payment + " of debt.");
        }
        if (!s.ownsBakery && s.money >= 4800 && s.health >= 55 && s.personalityCareful >= 45) {
            s.money-=4500; s.ownsBakery=true; s.goal="KEEP_BAKERY";
            add(s,log,"Marco invested EUR 4500 and opened his own bakery.");
        }
        if (s.illness > 0) {
            s.daysSick++;
            s.illness = clamp(s.illness - (s.health > 45 ? 7 : 3));
            if (s.illness == 0) add(s,log,"Marco recovered from an illness.");
        } else if (dice(s,1000) < Math.min(75, 8+Math.max(0,s.age-50)/3+(100-s.health)/7)) {
            s.illness = 25+dice(s,30);
            add(s,log,"Marco developed symptoms of an illness.");
        }
        // Longevity is stochastic; nobody is assigned a fixed death date.
        int ageRisk = s.age < 60 ? 0 : Math.min(800, (s.age-59)*(s.age-59)/12);
        if (dice(s,100000) < ageRisk + (s.illness>=65?45:0)) {
            die(s,log,"age-related illness"); return;
        }
        if (s.health<=0) {die(s,log,"illness or deprivation");return;}
        if (s.stress >= 75) s.health=clamp(s.health-1);
        if (s.fitness > 0 && s.fatigue > 80 && s.hunger>70) s.fitness--;
        if (s.health > 60 && s.fatigue < 65 && s.personalityCurious >= 55 && s.day%7==0) s.fitness=clamp(s.fitness+1);
        if (s.day%7==0) add(s,log,"Weekly reflection: health "+s.health+", mood "+s.mood+", savings EUR "+s.money+".");
    }

    private static boolean buyFood(WorldState s,List<LifeEvent> log) {
        if (s.money < 9 || !shopOpen(s.minute)) return false;
        s.money-=9;s.food+=3;
        change(s,"BUYING_GROCERIES",log);
        add(s,log,"Marco bought 3 meals for EUR 9.");return true;
    }
    private static boolean buyWater(WorldState s,List<LifeEvent> log) {
        if (s.money < 3 || !shopOpen(s.minute)) return false;
        s.money-=3;s.water+=3;
        change(s,"BUYING_GROCERIES",log);
        add(s,log,"Marco bought 3 water portions for EUR 3.");return true;
    }
    private static boolean shopOpen(int m) {return m>=360 && m<1320;}
    private static void die(WorldState s,List<LifeEvent> log,String reason) {
        s.alive=false;s.health=0; s.activity="DECEASED";
        add(s,log,"Marco died at age "+s.age+" due to "+reason+". His history is preserved.");
    }
    private static void add(WorldState s,List<LifeEvent> log,String text) {
        log.add(new LifeEvent(s.day,s.minute,text));
    }
    private static void change(WorldState s,String next,List<LifeEvent> log) {
        if (next.equals(s.activity))return;
        s.activity=next;
        if (next.equals("SLEEPING")) add(s,log,"Marco went to sleep.");
        else if (next.equals("WORKING")) add(s,log,"Marco started working at the bakery.");
        else if (next.equals("EXPLORING_TOWN")) add(s,log,"Marco went exploring around town.");
        else if (next.equals("WALKING_FOR_HEALTH")) add(s,log,"Marco went for a long walk to improve his health.");
        else if (next.equals("SOCIALIZING")) add(s,log,"Marco decided to spend time with other people.");
        else if (next.equals("RESTING_WHILE_SICK")) add(s,log,"Marco stayed home to recover.");
        else if (next.startsWith("LOOKING_FOR")) add(s,log,"Marco needs supplies but cannot currently obtain them.");
    }
    static String readableActivity(String code) {
        return switch (code) {
            case "WORKING" -> "working at the bakery";
            case "SLEEPING" -> "sleeping";
            case "EATING" -> "eating";
            case "DRINKING" -> "drinking water";
            case "BUYING_GROCERIES" -> "buying groceries";
            case "LOOKING_FOR_FOOD" -> "looking for food";
            case "LOOKING_FOR_WATER" -> "looking for water";
            case "RELAXING_AT_HOME" -> "relaxing at home";
            case "EXPLORING_TOWN" -> "exploring the town";
            case "WALKING_FOR_HEALTH" -> "walking for exercise";
            case "SOCIALIZING" -> "socializing";
            case "WASHING" -> "washing";
            case "RESTING_WHILE_SICK" -> "resting while sick";
            case "DECEASED" -> "deceased";
            default -> "going about his day";
        };
    }
}
