package com.holovin.npclife;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

/** Reproducible procedural resident generation, without network or AI calls. */
final class PopulationGenerator {
    private PopulationGenerator() { }

    static final int MAX_GENERATED = 64; // Bounded population; larger worlds need performance profiling.
    private static final String[] FIRST = {"Amelia","Luca","Giulia","Pietro","Sofia","Davide",
            "Chiara","Matteo","Aurora","Nico","Beatrice","Alessandro","Marta","Andrea",
            "Valentina","Filippo","Elisa","Riccardo","Irene","Samuele","Sara","Leonardo",
            "Noemi","Federico","Alice","Gabriele","Caterina","Tommaso","Greta","Edoardo"};
    private static final String[] LAST = {"Conti","Ferri","Ricci","Moretti","Russo","Marino","Galli",
            "Greco","Costa","Rinaldi","Serra","De Luca","Fontana","Caruso","Lombardi","Mancini",
            "Barbieri","Villa","Grassi","Leone","Monti","Riva","Martini","Bruni","Santoro"};
    private static final String[] GOALS = {"SAVE_MONEY","MAKE_FRIENDS","LEARN_SKILLS","STAY_HEALTHY","FIND_BALANCE"};
    private static final String[] JOBS = {"Teacher","Mechanic","Librarian","Barista","Office clerk",
            "Nurse","Baker","Market vendor"};
    private static final TownLife.Place[] WORKPLACES = {TownLife.Place.SCHOOL,TownLife.Place.WORKSHOP,
            TownLife.Place.LIBRARY,TownLife.Place.CAFE,TownLife.Place.OFFICE,TownLife.Place.CLINIC,
            TownLife.Place.BAKERY,TownLife.Place.MARKET};

    static List<TownLife.Citizen> add(TownLife.Town town,int count) {
        if(count<1 || count>20)throw new IllegalArgumentException("Generate 1–20 residents per command.");
        long existing=DemographyLife.living(town);
        if(existing+count>MAX_GENERATED)
            throw new IllegalArgumentException("Maximum "+MAX_GENERATED+" generated residents in this prototype.");
        int next=town.people.values().stream().mapToInt(p->p.generationIndex).max().orElse(0)+1;
        if(next+count-1>9999)throw new IllegalArgumentException("Historical resident ID capacity reached.");
        Set<String> names=new HashSet<>();
        for(var citizen:town.people.values())names.add(citizen.name);
        List<TownLife.Citizen> generated=new ArrayList<>();
        for(int i=0;i<count;i++) {
            int index=next+i;
            var rng=new SplittableRandom(0x5EEDCAFE3A12L+index*0x9E3779B97F4A7C15L);
            String name;
            name=FIRST[rng.nextInt(FIRST.length)]+" "+LAST[rng.nextInt(LAST.length)];
            if(!names.add(name)){name=name+" "+index;names.add(name);}
            int job=rng.nextInt(JOBS.length);
            var home=rng.nextBoolean()?TownLife.Place.NORTH_RESIDENCES:TownLife.Place.SOUTH_RESIDENCES;
            String id="resident-%04d".formatted(index);
            var c=new TownLife.Citizen(id,name,JOBS[job],home);
            c.home=home;c.workplace=WORKPLACES[job];
            // Two genuinely employed industrial workers per initial 12 residents,
            // with real wages, hours, travel, sickness and independent choices.
            if(index%12==6) {
                c.occupation="Miner";c.workplace=TownLife.Place.QUARRY;
            } else if(index%12==7) {
                c.occupation="Factory machinist";c.workplace=TownLife.Place.FACTORY;
            } else if(index%12==8) {
                c.occupation="Transport driver";c.workplace=TownLife.Place.QUARRY;
            } else if(index%12==9) {
                // Every new twelve-resident world has a real mechanic visiting
                // the Workshop, so a player's first business can find a human
                // candidate rather than relying on a random lucky job draw.
                c.occupation="Mechanic";c.workplace=TownLife.Place.WORKSHOP;
            }
            // Reserve two food-shop jobs in larger worlds without changing earlier identities.
            if(index==29||index==30){c.occupation=index==29?"Pizzaiolo":"Gelataio";c.workplace=TownLife.Place.CENTRO;}
            if(index==31){c.occupation="Pharmacist";c.workplace=TownLife.Place.CLINIC;}
            c.age=rng.nextInt(19,71);
            if(index==31)c.age=Math.max(23,Math.min(c.age,64));
            if(index==29||index==30)c.age=Math.min(c.age,64);
            c.sociability=rng.nextInt(15,91);
            c.carefulness=rng.nextInt(15,91);
            c.curiosity=rng.nextInt(15,91);
            c.scheduleOffset=rng.nextInt(-30,91);
            c.personalSeed=rng.nextLong();
            c.goal=GOALS[rng.nextInt(GOALS.length)];
            c.money=rng.nextInt(40,161);
            c.food=3;c.water=3;c.activity="SETTLING_IN";
            c.generationIndex=index;
            town.people.put(id,c);
            EducationLife.ensureResident(town,c);
            generated.add(c);
        }
        return generated;
    }
}
