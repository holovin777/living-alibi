package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** SQLite persistence and additive migration from the original v0.1/v0.2 worlds. */
final class WorldDatabase implements AutoCloseable {
    record Memory(String speaker, String text, int day, int minute) { }
    private static final String NPC = "marco";
    private final Connection db;

    WorldDatabase(Path file) throws Exception {
        Files.createDirectories(file.toAbsolutePath().getParent());
        db = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement st = db.createStatement()) {
            st.execute("PRAGMA busy_timeout=5000");
            st.execute("CREATE TABLE IF NOT EXISTS world_state (id INTEGER PRIMARY KEY CHECK(id=1), day INTEGER NOT NULL, minute INTEGER NOT NULL, friendship INTEGER NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS memories (id INTEGER PRIMARY KEY AUTOINCREMENT, npc_id TEXT NOT NULL, speaker TEXT NOT NULL, text TEXT NOT NULL, day INTEGER NOT NULL, minute INTEGER NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS life_events (id INTEGER PRIMARY KEY AUTOINCREMENT, npc_id TEXT NOT NULL, day INTEGER NOT NULL, minute INTEGER NOT NULL, text TEXT NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS character_facts (id INTEGER PRIMARY KEY AUTOINCREMENT, npc_id TEXT NOT NULL, text TEXT NOT NULL, day INTEGER NOT NULL, minute INTEGER NOT NULL)");
            st.execute("INSERT OR IGNORE INTO world_state(id,day,minute,friendship) VALUES(1,1,450,0)");
            st.execute("""
                CREATE TABLE IF NOT EXISTS town_people (
                    id TEXT PRIMARY KEY, place TEXT NOT NULL, destination TEXT NOT NULL,
                    travel_remaining INTEGER NOT NULL, activity TEXT NOT NULL,
                    hunger INTEGER NOT NULL, thirst INTEGER NOT NULL, fatigue INTEGER NOT NULL,
                    health INTEGER NOT NULL, money INTEGER NOT NULL,
                    food INTEGER NOT NULL, water INTEGER NOT NULL, age INTEGER NOT NULL,
                    alive INTEGER NOT NULL)
                """);
            st.execute("CREATE TABLE IF NOT EXISTS town_meta (id INTEGER PRIMARY KEY CHECK(id=1), together INTEGER NOT NULL)");
            st.execute("""
                CREATE TABLE IF NOT EXISTS town_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, day INTEGER NOT NULL,
                    minute INTEGER NOT NULL, actor_id TEXT NOT NULL, place TEXT NOT NULL,
                    text TEXT NOT NULL)
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS town_observations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, witness_id TEXT NOT NULL,
                    day INTEGER NOT NULL, minute INTEGER NOT NULL, place TEXT NOT NULL,
                    text TEXT NOT NULL)
                """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_town_observations_witness ON town_observations(witness_id,id)");
            st.execute("""
                CREATE TABLE IF NOT EXISTS town_relationships (
                    first_id TEXT NOT NULL, second_id TEXT NOT NULL,
                    affinity INTEGER NOT NULL, trust INTEGER NOT NULL, tension INTEGER NOT NULL,
                    encounters INTEGER NOT NULL, conversations INTEGER NOT NULL, arguments INTEGER NOT NULL,
                    last_interaction_tick INTEGER NOT NULL, rng INTEGER NOT NULL,
                    PRIMARY KEY(first_id,second_id))
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS social_interactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, day INTEGER NOT NULL, minute INTEGER NOT NULL,
                    first_id TEXT NOT NULL, second_id TEXT NOT NULL, place TEXT NOT NULL,
                    kind TEXT NOT NULL, description TEXT NOT NULL)
                """);


        }
        // Additive migration for databases created by v1.1.
        if(!columnExists("town_meta","meeting_since")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE town_meta ADD COLUMN meeting_since INTEGER NOT NULL DEFAULT -1");
            }
        }
        LinkedHashMap<String,String> add = new LinkedHashMap<>();
        add.put("age", "INTEGER NOT NULL DEFAULT 36");
        add.put("hunger", "INTEGER NOT NULL DEFAULT 25");
        add.put("thirst", "INTEGER NOT NULL DEFAULT 20");
        add.put("fatigue", "INTEGER NOT NULL DEFAULT 20");
        add.put("health", "INTEGER NOT NULL DEFAULT 100");
        add.put("money", "INTEGER NOT NULL DEFAULT 45");
        add.put("food", "INTEGER NOT NULL DEFAULT 2");
        add.put("water", "INTEGER NOT NULL DEFAULT 2");
        add.put("work_minutes", "INTEGER NOT NULL DEFAULT 0");
        add.put("activity", "TEXT NOT NULL DEFAULT 'WORKING'");
        add.put("stress", "INTEGER NOT NULL DEFAULT 18");
        add.put("loneliness", "INTEGER NOT NULL DEFAULT 24");
        add.put("mood", "INTEGER NOT NULL DEFAULT 70");
        add.put("hygiene", "INTEGER NOT NULL DEFAULT 80");
        add.put("illness", "INTEGER NOT NULL DEFAULT 0");
        add.put("debt", "INTEGER NOT NULL DEFAULT 0");
        add.put("fitness", "INTEGER NOT NULL DEFAULT 60");
        add.put("personality_sociable", "INTEGER NOT NULL DEFAULT 57");
        add.put("personality_careful", "INTEGER NOT NULL DEFAULT 68");
        add.put("personality_curious", "INTEGER NOT NULL DEFAULT 74");
        add.put("days_worked", "INTEGER NOT NULL DEFAULT 0");
        add.put("missed_rent", "INTEGER NOT NULL DEFAULT 0");
        add.put("days_sick", "INTEGER NOT NULL DEFAULT 0");
        add.put("alive", "INTEGER NOT NULL DEFAULT 1");
        add.put("owns_bakery", "INTEGER NOT NULL DEFAULT 0");
        add.put("goal", "TEXT NOT NULL DEFAULT 'OPEN_BAKERY'");
        add.put("rng", "INTEGER NOT NULL DEFAULT 171127");
        add.put("last_saved_epoch", "INTEGER NOT NULL DEFAULT 0");
        add.put("offline_progress", "INTEGER NOT NULL DEFAULT 0");
        add.put("ai_autonomy", "INTEGER NOT NULL DEFAULT 0");
        add.put("speed", "INTEGER NOT NULL DEFAULT 1");
        for (var entry : add.entrySet()) {
            if (!columnExists(entry.getKey())) {
                try (Statement st = db.createStatement()) {
                    st.executeUpdate("ALTER TABLE world_state ADD COLUMN " + entry.getKey() + " " + entry.getValue());
                }
            }
        }
    }

    private boolean columnExists(String col) throws SQLException {
        return columnExists("world_state",col);
    }
    private boolean columnExists(String table,String col) throws SQLException {
        // Names are fixed internal identifiers; never accept user input here.
        try (Statement st=db.createStatement();ResultSet rs=st.executeQuery("PRAGMA table_info("+table+")")) {
            while (rs.next()) if (col.equals(rs.getString("name"))) return true;
        }
        return false;
    }

    WorldState load() throws SQLException {
        WorldState s = new WorldState();
        try (Statement st=db.createStatement();ResultSet rs=st.executeQuery("SELECT * FROM world_state WHERE id=1")) {
            if (!rs.next()) throw new SQLException("World state missing");
            s.day=rs.getInt("day");s.minute=rs.getInt("minute");s.friendship=rs.getInt("friendship");
            s.age=rs.getInt("age");s.hunger=rs.getInt("hunger");s.thirst=rs.getInt("thirst");
            s.fatigue=rs.getInt("fatigue");s.health=rs.getInt("health");s.money=rs.getInt("money");
            s.food=rs.getInt("food");s.water=rs.getInt("water");s.workMinutes=rs.getInt("work_minutes");
            s.activity=rs.getString("activity");s.stress=rs.getInt("stress");s.loneliness=rs.getInt("loneliness");
            s.mood=rs.getInt("mood");s.hygiene=rs.getInt("hygiene");s.illness=rs.getInt("illness");
            s.debt=rs.getInt("debt");s.fitness=rs.getInt("fitness");
            s.personalitySociable=rs.getInt("personality_sociable");
            s.personalityCareful=rs.getInt("personality_careful");
            s.personalityCurious=rs.getInt("personality_curious");
            s.daysWorked=rs.getInt("days_worked");s.missedRent=rs.getInt("missed_rent");
            s.daysSick=rs.getInt("days_sick");s.alive=rs.getInt("alive")!=0;
            s.ownsBakery=rs.getInt("owns_bakery")!=0;s.goal=rs.getString("goal");s.rng=rs.getLong("rng");
            s.lastSavedEpoch=rs.getLong("last_saved_epoch");
            s.offlineProgress=rs.getInt("offline_progress")!=0;
            s.aiAutonomy=rs.getInt("ai_autonomy")!=0;s.speed=rs.getInt("speed");
        }
        return s;
    }

    void save(WorldState s,List<LifeSimulation.LifeEvent> events,List<Memory> messages) throws SQLException {
        save(s,events,messages,null,List.of(),List.of(),List.of());
    }

    void save(WorldState s,List<LifeSimulation.LifeEvent> events,List<Memory> messages,
              TownLife.Town town,List<TownLife.Event> townEvents,
              List<TownLife.Observation> observations) throws SQLException {
        save(s,events,messages,town,townEvents,observations,List.of());
    }

    void save(WorldState s,List<LifeSimulation.LifeEvent> events,List<Memory> messages,
              TownLife.Town town,List<TownLife.Event> townEvents,
              List<TownLife.Observation> observations,List<SocialLife.Interaction> socialEvents) throws SQLException {
        // Marco, the other residents, world events, and their knowledge commit atomically.
        s.lastSavedEpoch=Instant.now().getEpochSecond();
        db.setAutoCommit(false);
        try {
            String sql="""
              UPDATE world_state SET day=?,minute=?,friendship=?,age=?,hunger=?,thirst=?,fatigue=?,health=?,money=?,food=?,water=?,
              work_minutes=?,activity=?,stress=?,loneliness=?,mood=?,hygiene=?,illness=?,debt=?,fitness=?,personality_sociable=?,
              personality_careful=?,personality_curious=?,days_worked=?,missed_rent=?,days_sick=?,alive=?,owns_bakery=?,goal=?,rng=?,
              last_saved_epoch=?,offline_progress=?,ai_autonomy=?,speed=? WHERE id=1
              """;
            try (PreparedStatement p=db.prepareStatement(sql)) {
                int i=1;
                p.setInt(i++,s.day);p.setInt(i++,s.minute);p.setInt(i++,s.friendship);p.setInt(i++,s.age);
                p.setInt(i++,s.hunger);p.setInt(i++,s.thirst);p.setInt(i++,s.fatigue);p.setInt(i++,s.health);
                p.setInt(i++,s.money);p.setInt(i++,s.food);p.setInt(i++,s.water);p.setInt(i++,s.workMinutes);
                p.setString(i++,s.activity);p.setInt(i++,s.stress);p.setInt(i++,s.loneliness);p.setInt(i++,s.mood);
                p.setInt(i++,s.hygiene);p.setInt(i++,s.illness);p.setInt(i++,s.debt);p.setInt(i++,s.fitness);
                p.setInt(i++,s.personalitySociable);p.setInt(i++,s.personalityCareful);p.setInt(i++,s.personalityCurious);
                p.setInt(i++,s.daysWorked);p.setInt(i++,s.missedRent);p.setInt(i++,s.daysSick);
                p.setInt(i++,s.alive?1:0);p.setInt(i++,s.ownsBakery?1:0);p.setString(i++,s.goal);
                p.setLong(i++,s.rng);p.setLong(i++,s.lastSavedEpoch);p.setInt(i++,s.offlineProgress?1:0);
                p.setInt(i++,s.aiAutonomy?1:0);p.setInt(i++,s.speed);
                p.executeUpdate();
            }
            try(PreparedStatement p=db.prepareStatement("INSERT INTO life_events(npc_id,day,minute,text) VALUES (?,?,?,?)")) {
                for(var e:events) {p.setString(1,NPC);p.setInt(2,e.day());p.setInt(3,e.minute());p.setString(4,e.text());p.addBatch();}
                if(!events.isEmpty())p.executeBatch();
            }
            try(PreparedStatement p=db.prepareStatement("INSERT INTO memories(npc_id,speaker,text,day,minute) VALUES(?,?,?,?,?)")) {
                for(var m:messages) {p.setString(1,NPC);p.setString(2,m.speaker());p.setString(3,m.text());p.setInt(4,m.day());p.setInt(5,m.minute());p.addBatch();}
                if(!messages.isEmpty())p.executeBatch();
            }
            if(town!=null) {
                persistTown(town,townEvents,observations);
                persistSocial(town,socialEvents);
            }
            db.commit();
        } catch(SQLException ex) {db.rollback();throw ex;}
        finally{db.setAutoCommit(true);}
    }

    List<Memory> memories(int limit) throws SQLException {
        List<Memory> list=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("SELECT speaker,text,day,minute FROM memories WHERE npc_id=? ORDER BY id DESC LIMIT ?")) {
            p.setString(1,NPC);p.setInt(2,limit);
            try(ResultSet r=p.executeQuery()) {while(r.next())list.add(new Memory(r.getString(1),r.getString(2),r.getInt(3),r.getInt(4)));}
        }
        Collections.reverse(list);return list;
    }
    List<LifeSimulation.LifeEvent> events(int limit) throws SQLException {
        List<LifeSimulation.LifeEvent> list=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("SELECT day,minute,text FROM life_events WHERE npc_id=? ORDER BY id DESC LIMIT ?")) {
            p.setString(1,NPC);p.setInt(2,limit);
            try(ResultSet r=p.executeQuery()) {while(r.next())list.add(new LifeSimulation.LifeEvent(r.getInt(1),r.getInt(2),r.getString(3)));}
        }
        Collections.reverse(list);return list;
    }
    void rememberFact(WorldState state,String fact) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("INSERT INTO character_facts(npc_id,text,day,minute) VALUES(?,?,?,?)")) {
            p.setString(1,NPC);p.setString(2,fact);p.setInt(3,state.day);p.setInt(4,state.minute);
            p.executeUpdate();
        }
    }
    List<String> facts(int limit) throws SQLException {
        List<String> result=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("SELECT text FROM character_facts WHERE npc_id=? ORDER BY id DESC LIMIT ?")) {
            p.setString(1,NPC);p.setInt(2,limit);
            try(ResultSet r=p.executeQuery()) {while(r.next())result.add(r.getString(1));}
        }
        Collections.reverse(result);return result;
    }
    List<LifeSimulation.LifeEvent> milestones(int limit) throws SQLException {
        List<LifeSimulation.LifeEvent> result=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("SELECT day,minute,text FROM life_events WHERE npc_id=? AND ("+
                "text LIKE '%turned%' OR text LIKE '%opened his own bakery%' OR text LIKE '%died%' OR text LIKE '%recovered%'"+
                ") ORDER BY id DESC LIMIT ?")) {
            p.setString(1,NPC);p.setInt(2,limit);
            try(ResultSet r=p.executeQuery()) {while(r.next())result.add(new LifeSimulation.LifeEvent(r.getInt(1),r.getInt(2),r.getString(3)));}
        }
        Collections.reverse(result);return result;
    }
    TownLife.Town loadTown(WorldState state) throws SQLException {
        TownLife.Town town=TownLife.create(state);
        try(Statement st=db.createStatement();ResultSet rs=st.executeQuery("SELECT * FROM town_people")) {
            while(rs.next()) {
                String id=rs.getString("id");
                if(!town.people.containsKey(id))continue;
                var p=town.get(id);
                p.place=TownLife.Place.valueOf(rs.getString("place"));
                p.destination=TownLife.Place.valueOf(rs.getString("destination"));
                p.travelRemaining=rs.getInt("travel_remaining");p.activity=rs.getString("activity");
                p.hunger=rs.getInt("hunger");p.thirst=rs.getInt("thirst");p.fatigue=rs.getInt("fatigue");
                p.health=rs.getInt("health");p.money=rs.getInt("money");p.food=rs.getInt("food");
                p.water=rs.getInt("water");p.age=rs.getInt("age");p.alive=rs.getInt("alive")!=0;
            }
        }
        try(Statement st=db.createStatement();ResultSet rs=st.executeQuery("SELECT together, meeting_since FROM town_meta WHERE id=1")) {
            if(rs.next()) {
                town.together=rs.getInt(1)!=0;
                town.meetingSinceTick=rs.getLong(2);
                if(town.together && town.meetingSinceTick<0)
                    town.meetingSinceTick=(long)(state.day-1)*1440L+state.minute;
            }
        }
        try(PreparedStatement p=db.prepareStatement("""
                SELECT * FROM town_relationships WHERE first_id='marco' AND second_id='elena'
                """);ResultSet rs=p.executeQuery()) {
            if(rs.next()) {
                var r=town.relationship;
                r.affinity=rs.getInt("affinity");r.trust=rs.getInt("trust");r.tension=rs.getInt("tension");
                r.encounters=rs.getInt("encounters");r.conversations=rs.getInt("conversations");
                r.arguments=rs.getInt("arguments");r.lastInteractionTick=rs.getLong("last_interaction_tick");
                r.rng=rs.getLong("rng");
            }
        }
        return town;
    }

    private void persistTown(TownLife.Town town,List<TownLife.Event> events,
                             List<TownLife.Observation> observations) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
            INSERT INTO town_people(id,place,destination,travel_remaining,activity,hunger,thirst,fatigue,
                                    health,money,food,water,age,alive)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET
                place=excluded.place,destination=excluded.destination,
                travel_remaining=excluded.travel_remaining,activity=excluded.activity,
                hunger=excluded.hunger,thirst=excluded.thirst,fatigue=excluded.fatigue,
                health=excluded.health,money=excluded.money,food=excluded.food,
                water=excluded.water,age=excluded.age,alive=excluded.alive
            """)) {
            for(var c:town.people.values()) {
                p.setString(1,c.id);p.setString(2,c.place.name());p.setString(3,c.destination.name());
                p.setInt(4,c.travelRemaining);p.setString(5,c.activity);
                p.setInt(6,c.hunger);p.setInt(7,c.thirst);p.setInt(8,c.fatigue);
                p.setInt(9,c.health);p.setInt(10,c.money);p.setInt(11,c.food);
                p.setInt(12,c.water);p.setInt(13,c.age);p.setInt(14,c.alive?1:0);
                p.addBatch();
            }
            p.executeBatch();
        }
        try(PreparedStatement p=db.prepareStatement("""
            INSERT INTO town_meta(id,together,meeting_since) VALUES(1,?,?)
            ON CONFLICT(id) DO UPDATE SET together=excluded.together,
                                          meeting_since=excluded.meeting_since
            """)) {
            p.setInt(1,town.together?1:0);p.setLong(2,town.meetingSinceTick);p.executeUpdate();
        }
        try(PreparedStatement p=db.prepareStatement("""
            INSERT INTO town_events(day,minute,actor_id,place,text) VALUES(?,?,?,?,?)
            """)) {
            for(var e:events) {
                p.setInt(1,e.day());p.setInt(2,e.minute());p.setString(3,e.actorId());
                p.setString(4,e.place().name());p.setString(5,e.text());p.addBatch();
            }
            if(!events.isEmpty())p.executeBatch();
        }
        try(PreparedStatement p=db.prepareStatement("""
            INSERT INTO town_observations(witness_id,day,minute,place,text) VALUES(?,?,?,?,?)
            """)) {
            for(var o:observations) {
                p.setString(1,o.witnessId());p.setInt(2,o.day());p.setInt(3,o.minute());
                p.setString(4,o.place().name());p.setString(5,o.text());p.addBatch();
            }
            if(!observations.isEmpty())p.executeBatch();
        }
    }

    List<TownLife.Event> townEvents(int limit) throws SQLException {
        List<TownLife.Event> entries=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("""
            SELECT day,minute,actor_id,place,text FROM town_events ORDER BY id DESC LIMIT ?
            """)) {
            p.setInt(1,limit);
            try(ResultSet r=p.executeQuery()) {
                while(r.next())entries.add(new TownLife.Event(r.getInt(1),r.getInt(2),
                        r.getString(3),TownLife.Place.valueOf(r.getString(4)),r.getString(5)));
            }
        }
        Collections.reverse(entries);return entries;
    }
    List<TownLife.Observation> observations(String witnessId,int limit) throws SQLException {
        List<TownLife.Observation> entries=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("""
            SELECT day,minute,place,text FROM town_observations
            WHERE witness_id=? ORDER BY id DESC LIMIT ?
            """)) {
            p.setString(1,witnessId);p.setInt(2,limit);
            try(ResultSet r=p.executeQuery()) {
                while(r.next())entries.add(new TownLife.Observation(witnessId,r.getInt(1),
                        r.getInt(2),TownLife.Place.valueOf(r.getString(3)),r.getString(4)));
            }
        }
        Collections.reverse(entries);return entries;
    }


    private void persistSocial(TownLife.Town town,List<SocialLife.Interaction> interactions) throws SQLException {
        var r=town.relationship;
        try(PreparedStatement p=db.prepareStatement("""
            INSERT INTO town_relationships(first_id,second_id,affinity,trust,tension,encounters,
                                           conversations,arguments,last_interaction_tick,rng)
            VALUES(?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(first_id,second_id) DO UPDATE SET
                affinity=excluded.affinity,trust=excluded.trust,tension=excluded.tension,
                encounters=excluded.encounters,conversations=excluded.conversations,
                arguments=excluded.arguments,last_interaction_tick=excluded.last_interaction_tick,
                rng=excluded.rng
            """)) {
            p.setString(1,r.firstId);p.setString(2,r.secondId);p.setInt(3,r.affinity);
            p.setInt(4,r.trust);p.setInt(5,r.tension);p.setInt(6,r.encounters);
            p.setInt(7,r.conversations);p.setInt(8,r.arguments);p.setLong(9,r.lastInteractionTick);
            p.setLong(10,r.rng);p.executeUpdate();
        }
        try(PreparedStatement p=db.prepareStatement("""
            INSERT INTO social_interactions(day,minute,first_id,second_id,place,kind,description)
            VALUES(?,?,?,?,?,?,?)
            """)) {
            for(var i:interactions) {
                p.setInt(1,i.day());p.setInt(2,i.minute());p.setString(3,i.firstId());
                p.setString(4,i.secondId());p.setString(5,i.place().name());
                p.setString(6,i.kind().name());p.setString(7,i.description());p.addBatch();
            }
            if(!interactions.isEmpty())p.executeBatch();
        }
    }

    List<SocialLife.Interaction> socialEvents(int limit) throws SQLException {
        List<SocialLife.Interaction> entries=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("""
                SELECT day,minute,first_id,second_id,place,kind,description
                FROM social_interactions ORDER BY id DESC LIMIT ?
                """)) {
            p.setInt(1,limit);
            try(ResultSet rs=p.executeQuery()) {
                while(rs.next())entries.add(new SocialLife.Interaction(rs.getInt(1),rs.getInt(2),
                        rs.getString(3),rs.getString(4),TownLife.Place.valueOf(rs.getString(5)),
                        SocialLife.Kind.valueOf(rs.getString(6)),rs.getString(7)));
            }
        }
        Collections.reverse(entries);return entries;
    }

    @Override public void close() throws SQLException {db.close();}
}
