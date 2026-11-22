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
        try (Statement st=db.createStatement();ResultSet rs=st.executeQuery("PRAGMA table_info(world_state)")) {
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
        // Every state mutation and its event log are committed atomically.
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
    @Override public void close() throws SQLException {db.close();}
}
