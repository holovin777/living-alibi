package com.holovin.npclife;

import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Single authoritative game session for a local Godot client.
 * All state changes are serialized and committed before a response is returned.
 */
@Service
public final class GameSession implements AutoCloseable {
    private final WorldDatabase db;
    private WorldState state;
    private TownLife.Town town;

    @Autowired
    public GameSession(@Value("${living-alibi.database:data/world.db}") String databasePath)
            throws Exception {
        this(Path.of(databasePath));
    }

    GameSession(Path databasePath) throws Exception {
        boolean firstLaunch=Files.notExists(databasePath);
        db=new WorldDatabase(databasePath);
        state=db.load();
        town=db.loadTown(state);
        if(firstLaunch) {
            // A fresh city should already contain a small, active population.
            PopulationGenerator.add(town,12);
            db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }
        // Old save files might contain a deceased visitor.
        if(!town.player.alive) {
            PlayerLife.beginNewLife(town,state.day,state.minute);
            db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }
    }

    public record PlayerView(int incarnation,String place,String privateName,
                             int hunger,int thirst,int fatigue,int health,boolean alive,
                             int money,String jobId,int engineeringSkill,int investigationSkill) { }
    public record JobView(String id,String title,String workplace,String subject,
                          int requiredSkill,int wage,int remainingPayroll) { }
    public record ResidentView(String id,String name,String activity) { }
    public record SourceView(String id,String label,String kind,int stock,int capacity) { }
    public record WeatherView(String sky,int temperatureC,boolean raining,boolean snowing) { }
    public record WorldView(int day,int minute,int year,int dayOfYear,String season,
                            WeatherView weather,PlayerView player,List<ResidentView> residents,
                            List<SourceView> sources) { }
    public record ActionView(String message,WorldView world) { }

    public synchronized WorldView snapshot() {
        var p=town.player;
        var player=new PlayerView(p.incarnation,PlayerLife.position(p),p.privateName,
                p.hunger,p.thirst,p.fatigue,p.health,p.alive,
                p.money,p.jobId,p.engineeringSkill,p.investigationSkill);
        var residents=PlayerLife.visible(town).stream()
                .map(c->new ResidentView(c.id,c.name,c.activity)).toList();
        var resources=NatureLife.available(town).values().stream()
                .map(s->new SourceView(s.id,s.label,s.kind.name(),s.stock,s.capacity)).toList();
        var weather=WeatherLife.at(state.day,state.minute);
        return new WorldView(state.day,state.minute,CalendarLife.year(state.day),
                CalendarLife.dayOfYear(state.day),weather.season().name(),
                new WeatherView(weather.sky().name(),weather.temperatureC(),
                        weather.raining(),weather.snowing()),
                player,residents,resources);
    }

    public synchronized List<JobView> jobs() {
        return CareerLife.offers().stream()
                .map(v->new JobView(v.id(),v.title(),v.workplace().name(),v.subject(),
                        v.requiredSkill(),v.wage(),
                        town.employerFunds.getOrDefault(v.id(),0))).toList();
    }

    public synchronized ActionView applyJob(String jobId) throws SQLException {
        var candidate=town.copy();
        String message=CareerLife.apply(candidate,jobId);
        return apply(candidate,15,message);
    }

    public synchronized ActionView study(String subject) throws SQLException {
        var candidate=town.copy();
        String message=CareerLife.study(candidate,subject,state.minute);
        return apply(candidate,CareerLife.LESSON_MINUTES,message);
    }

    public synchronized ActionView work() throws SQLException {
        var candidate=town.copy();
        String message=CareerLife.work(candidate,state.day,state.minute);
        return apply(candidate,CareerLife.SHIFT_MINUTES,message);
    }

    public synchronized ActionView waitMinutes(int minutes) throws SQLException {
        if(minutes<1 || minutes>1440)
            throw new IllegalArgumentException("Wait between 1 and 1440 game minutes.");
        return apply(town.copy(),minutes,"Waited "+minutes+" minute(s).");
    }

    public synchronized ActionView move(String destination) throws SQLException {
        var copy=town.copy();
        var place=PlayerLife.destination(destination);
        int minutes=PlayerLife.startTravel(copy.player,place);
        if(minutes==0)return new ActionView("Already at "+place.label+".",snapshot());
        return apply(copy,minutes,"Walked to "+place.label+".");
    }

    public synchronized ActionView rest(int minutes) throws SQLException {
        var copy=town.copy();
        int recovered=PlayerLife.rest(copy.player,minutes);
        return apply(copy,minutes,"Rested for "+minutes+" minute(s); recovered up to "+
                recovered+" fatigue.");
    }

    public synchronized ActionView sleep(int hours) throws SQLException {
        var copy=town.copy();
        var result=PlayerLife.sleep(copy.player,hours,state.day,state.minute);
        return apply(copy,result.minutes(),
                "Slept "+hours+" hour(s). Health changed by "+result.healthChange()+".");
    }

    public synchronized ActionView drink(String sourceId) throws SQLException {
        var copy=town.copy();
        String message=NatureLife.drink(copy,sourceId,state.day,state.minute);
        return apply(copy,5,message);
    }

    public synchronized ActionView forage(String sourceId) throws SQLException {
        var copy=town.copy();
        String message=NatureLife.forage(copy,sourceId);
        return apply(copy,15,message);
    }

    private ActionView apply(TownLife.Town candidateTown,int minutes,String message)
            throws SQLException {
        var candidateState=state.copy();
        var events=new ArrayList<LifeSimulation.LifeEvent>();
        var truth=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();
        var interactions=new ArrayList<SocialLife.Interaction>();
        events.addAll(LifeSimulation.advance(candidateState,minutes,
                current->TownLife.tick(candidateTown,current,truth,observations,interactions)));
        if(!candidateTown.player.alive) {
            var ended=PlayerLife.beginNewLife(candidateTown,candidateState.day,candidateState.minute);
            message+=" Visitor #"+ended.incarnation()+" died; visitor #"+
                    candidateTown.player.incarnation+" begins a new life.";
        }
        db.save(candidateState,events,List.of(),candidateTown,truth,observations,interactions);
        state=candidateState;
        town=candidateTown;
        return new ActionView(message,snapshot());
    }

    @Override
    @PreDestroy
    public synchronized void close() throws SQLException {
        db.close();
    }
}
