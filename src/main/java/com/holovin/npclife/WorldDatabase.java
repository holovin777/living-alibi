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
        // v1.15: voluntary visitor presence and per-resident identity disclosure.
        // Neither table is included in world NPC identity or investigation evidence.
        try(Statement st=db.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS player_state (
                        id INTEGER PRIMARY KEY CHECK(id=1), place TEXT NOT NULL,
                        destination TEXT NOT NULL, travel_remaining INTEGER NOT NULL,
                        private_name TEXT,
                        hunger INTEGER NOT NULL DEFAULT 0,
                        thirst INTEGER NOT NULL DEFAULT 0,
                        fatigue INTEGER NOT NULL DEFAULT 0,
                        health INTEGER NOT NULL DEFAULT 100,
                        alive INTEGER NOT NULL DEFAULT 1,
                        incarnation INTEGER NOT NULL DEFAULT 1,
                        money INTEGER NOT NULL DEFAULT 50,
                        job_id TEXT,
                        engineering_skill INTEGER NOT NULL DEFAULT 0,
                        investigation_skill INTEGER NOT NULL DEFAULT 0,
                        last_work_day INTEGER NOT NULL DEFAULT 0,
                        last_technical_day INTEGER NOT NULL DEFAULT 0)
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS player_contacts (
                        resident_id TEXT PRIMARY KEY, known_name TEXT,
                        conversations INTEGER NOT NULL, last_day INTEGER NOT NULL,
                        last_minute INTEGER NOT NULL, last_heard TEXT)
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS player_life_contacts (
                        incarnation INTEGER NOT NULL,
                        resident_id TEXT NOT NULL, known_name TEXT,
                        conversations INTEGER NOT NULL, last_day INTEGER NOT NULL,
                        last_minute INTEGER NOT NULL, last_heard TEXT,
                        PRIMARY KEY(incarnation,resident_id))
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS player_lives (
                        incarnation INTEGER PRIMARY KEY, died_day INTEGER NOT NULL,
                        died_minute INTEGER NOT NULL, died_place TEXT NOT NULL,
                        private_name TEXT)
                    """);
            st.execute("CREATE TABLE IF NOT EXISTS visitor_ingredients (location TEXT NOT NULL, product_id TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity>0), PRIMARY KEY(location,product_id))");
            st.execute("CREATE TABLE IF NOT EXISTS retail_stock (product_id TEXT PRIMARY KEY, quantity INTEGER NOT NULL CHECK(quantity>=0))");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS career_accounts (
                        account_id TEXT PRIMARY KEY,
                        balance INTEGER NOT NULL CHECK(balance>=0))
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS player_business (
                        id INTEGER PRIMARY KEY CHECK(id=1),
                        registered INTEGER NOT NULL DEFAULT 0,
                        owner_incarnation INTEGER NOT NULL DEFAULT 0,
                        cash INTEGER NOT NULL DEFAULT 0 CHECK(cash>=0),
                        machine INTEGER NOT NULL DEFAULT 0,
                        machine_condition INTEGER NOT NULL DEFAULT 100
                            CHECK(machine_condition BETWEEN 0 AND 100),
                        employee_id TEXT,
                        spare_parts INTEGER NOT NULL DEFAULT 0 CHECK(spare_parts>=0),
                        last_pay_day INTEGER NOT NULL DEFAULT 0,
                        last_pay_minute INTEGER NOT NULL DEFAULT -1,
                        paid_hours INTEGER NOT NULL DEFAULT 0,
                        last_production_day INTEGER NOT NULL DEFAULT 0,
                        last_sale_day INTEGER NOT NULL DEFAULT 0)
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS technical_materials (
                        material_id TEXT PRIMARY KEY,
                        stock INTEGER NOT NULL CHECK(stock>=0))
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS technical_orders (
                        order_id TEXT PRIMARY KEY,
                        phase TEXT NOT NULL,
                        condition INTEGER NOT NULL CHECK(condition BETWEEN 0 AND 100),
                        completed_day INTEGER NOT NULL DEFAULT 0)
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS industry_stocks (
                        stock_id TEXT PRIMARY KEY,
                        quantity INTEGER NOT NULL CHECK(quantity>=0))
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS industry_schedule (
                        id INTEGER PRIMARY KEY CHECK(id=1),
                        extract_day INTEGER NOT NULL DEFAULT 0,
                        transport_day INTEGER NOT NULL DEFAULT 0,
                        refine_day INTEGER NOT NULL DEFAULT 0,
                        fabricate_day INTEGER NOT NULL DEFAULT 0,
                        deliver_day INTEGER NOT NULL DEFAULT 0,
                        arrival_day INTEGER NOT NULL DEFAULT 0,
                        recycle_day INTEGER NOT NULL DEFAULT 0)
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS municipal_services (
                        id INTEGER PRIMARY KEY CHECK(id=1),
                        clean_water INTEGER NOT NULL DEFAULT 0 CHECK(clean_water>=0),
                        last_pump_day INTEGER NOT NULL DEFAULT 0)
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS natural_resources (
                        id TEXT PRIMARY KEY, quantity INTEGER NOT NULL CHECK(quantity>=0))
                    """);
        }
        // Additive player-business migration: older saves retain working machines
        // as new, and already-paid hourly wages cannot replay after restart.
        if(!columnExists("player_business","machine_condition")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("""
                        ALTER TABLE player_business ADD COLUMN
                        machine_condition INTEGER NOT NULL DEFAULT 100
                        CHECK(machine_condition BETWEEN 0 AND 100)
                        """);
            }
        }
        if(!columnExists("player_business","last_pay_minute")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("""
                        ALTER TABLE player_business ADD COLUMN
                        last_pay_minute INTEGER NOT NULL DEFAULT -1
                        """);
                st.executeUpdate("""
                        UPDATE player_business SET last_pay_minute =
                            CASE WHEN paid_hours>=2 THEN 780
                                 WHEN paid_hours=1 THEN 720 ELSE -1 END
                        """);
            }
        }
        // Industrial migration: preserve existing ore, completed shipments,
        // and old saves made before freight and recycling were added.
        if(!columnExists("industry_schedule","arrival_day")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE industry_schedule ADD COLUMN arrival_day INTEGER NOT NULL DEFAULT 0");
            }
        }
        if(!columnExists("industry_schedule","recycle_day")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE industry_schedule ADD COLUMN recycle_day INTEGER NOT NULL DEFAULT 0");
            }
        }
        // v1.16: player physical needs.
        // Existing worlds receive healthy default values.
        // Never reset an existing player's saved condition.
        LinkedHashMap<String,String> playerColumns=new LinkedHashMap<>();
        playerColumns.put("hunger","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("thirst","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("fatigue","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("health","INTEGER NOT NULL DEFAULT 100");
        playerColumns.put("alive","INTEGER NOT NULL DEFAULT 1");
        playerColumns.put("incarnation","INTEGER NOT NULL DEFAULT 1");
        playerColumns.put("money","INTEGER NOT NULL DEFAULT 50");
        playerColumns.put("bread","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("bottled_water","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("room_until","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("stored_bread","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("stored_water","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("hygiene","INTEGER NOT NULL DEFAULT 100");
        playerColumns.put("owns_bicycle","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("cycling","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("bicycle_parked","TEXT");
        playerColumns.put("car_place","TEXT");
        playerColumns.put("car_fuel","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("car_condition","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("owns_house","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("job_id","TEXT");
        playerColumns.put("engineering_skill","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("investigation_skill","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("last_work_day","INTEGER NOT NULL DEFAULT 0");
        playerColumns.put("last_technical_day","INTEGER NOT NULL DEFAULT 0");

        for(var col:playerColumns.entrySet()) {
            if(!columnExists("player_state",col.getKey())) {
                try(Statement st=db.createStatement()) {
                    st.executeUpdate(
                            "ALTER TABLE player_state ADD COLUMN "+
                            col.getKey()+" "+col.getValue()
                            );
                }
            }
        }
        for(String table:List.of("player_contacts","player_life_contacts")) {
            for(var col:Map.of("trust","50","affinity","0","last_positive_day","0").entrySet()) {
                if(!columnExists(table,col.getKey())) {
                    try(Statement st=db.createStatement()) {
                        st.executeUpdate("ALTER TABLE "+table+" ADD COLUMN "+col.getKey()+
                                " INTEGER NOT NULL DEFAULT "+col.getValue());
                    }
                }
            }
        }
        // Additive migration for databases created by v1.1.
        if(!columnExists("town_meta","meeting_since")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE town_meta ADD COLUMN meeting_since INTEGER NOT NULL DEFAULT -1");
            }
        }
        // v1.3: preserve existing two citizens and make generated identities persistent.
        LinkedHashMap<String,String> citizenColumns=new LinkedHashMap<>();
        citizenColumns.put("name","TEXT NOT NULL DEFAULT ''");
        citizenColumns.put("occupation","TEXT NOT NULL DEFAULT ''");
        citizenColumns.put("home_place","TEXT NOT NULL DEFAULT 'NORTH_RESIDENCES'");
        citizenColumns.put("work_place","TEXT NOT NULL DEFAULT 'MARKET'");
        citizenColumns.put("sociability","INTEGER NOT NULL DEFAULT 62");
        citizenColumns.put("carefulness","INTEGER NOT NULL DEFAULT 55");
        citizenColumns.put("curiosity","INTEGER NOT NULL DEFAULT 50");
        citizenColumns.put("goal","TEXT NOT NULL DEFAULT 'STABILITY'");
        citizenColumns.put("schedule_offset","INTEGER NOT NULL DEFAULT 0");
        citizenColumns.put("generation_index","INTEGER NOT NULL DEFAULT 0");
        citizenColumns.put("personal_seed","INTEGER NOT NULL DEFAULT 0");
        citizenColumns.put("home_unit","TEXT NOT NULL DEFAULT ''");
        for(var col:citizenColumns.entrySet()) {
            if(!columnExists("town_people",col.getKey())) {
                try(Statement st=db.createStatement()) {
                    st.executeUpdate("ALTER TABLE town_people ADD COLUMN "+col.getKey()+" "+col.getValue());
                }
            }
        }
        // v1.5: encounter boundaries are persistent; older Marco/Elena relations remain intact.
        if(!columnExists("town_relationships","last_seen_tick")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE town_relationships ADD COLUMN last_seen_tick INTEGER NOT NULL DEFAULT -1");
            }
        }
        if(!columnExists("town_relationships","meeting_since_tick")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE town_relationships ADD COLUMN meeting_since_tick INTEGER NOT NULL DEFAULT -1");
            }
        }
        if(!columnExists("town_relationships","last_attempt_day")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE town_relationships ADD COLUMN last_attempt_day INTEGER NOT NULL DEFAULT -1");
            }
        }
        // v1.4: tag event scenes; historical rows keep an empty space ID because
        // their original exact interior location cannot be reconstructed.
        for(String table:List.of("town_events","town_observations")) {
            if(!columnExists(table,"space_id")) {
                try(Statement st=db.createStatement()) {
                    st.executeUpdate("ALTER TABLE "+table+" ADD COLUMN space_id TEXT NOT NULL DEFAULT ''");
                }
            }
        }
        // v1.6: claims are separate from the authoritative event log and old observations.
        try(Statement st=db.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS resident_beliefs (
                        resident_id TEXT NOT NULL, event_key TEXT NOT NULL,
                        event_day INTEGER NOT NULL, event_minute INTEGER NOT NULL,
                        actor_id TEXT NOT NULL, scene_id TEXT NOT NULL, statement TEXT NOT NULL,
                        origin TEXT NOT NULL CHECK(origin IN ('DIRECT','HEARD')),
                        learned_from TEXT NOT NULL, learned_day INTEGER NOT NULL,
                        learned_minute INTEGER NOT NULL, hops INTEGER NOT NULL,
                        confidence INTEGER NOT NULL,
                        PRIMARY KEY(resident_id,event_key))
                    """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_resident_beliefs_person ON resident_beliefs(resident_id,event_day,event_minute)");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS information_transfers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT, day INTEGER NOT NULL,
                        minute INTEGER NOT NULL, from_id TEXT NOT NULL, to_id TEXT NOT NULL,
                        event_key TEXT NOT NULL, confidence INTEGER NOT NULL,
                        hops INTEGER NOT NULL, statement TEXT NOT NULL)
                    """);
        }
        // v1.7: confidential eyewitness experiences and deliberate misstatements.
        try(Statement st=db.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS resident_secrets (
                        keeper_id TEXT NOT NULL, event_key TEXT NOT NULL,
                        day INTEGER NOT NULL, minute INTEGER NOT NULL, reason TEXT NOT NULL,
                        PRIMARY KEY(keeper_id,event_key))
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS deception_decisions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT, day INTEGER NOT NULL,
                        minute INTEGER NOT NULL, from_id TEXT NOT NULL, to_id TEXT NOT NULL,
                        event_key TEXT NOT NULL, kind TEXT NOT NULL CHECK(kind IN ('WITHHELD','LIE')),
                        statement TEXT NOT NULL)
                    """);
        }
        // v1.8: separate authoritative incident details from player-visible investigation notes.
        try(Statement st=db.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS investigation_cases (
                        id TEXT PRIMARY KEY, day INTEGER NOT NULL, minute INTEGER NOT NULL,
                        place TEXT NOT NULL, victim_id TEXT NOT NULL, offender_id TEXT NOT NULL,
                        item TEXT NOT NULL, amount INTEGER NOT NULL,
                        inspected INTEGER NOT NULL, solved INTEGER NOT NULL)
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS investigation_witnesses (
                        case_id TEXT NOT NULL, resident_id TEXT NOT NULL,
                        PRIMARY KEY(case_id,resident_id))
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS investigation_interviews (
                        case_id TEXT NOT NULL, resident_id TEXT NOT NULL, statement TEXT NOT NULL,
                        PRIMARY KEY(case_id,resident_id))
                    """);
        }
        // v1.9: incident-time scene records are immutable historical snapshots.
        // No legacy positions are inferred from current citizen locations.
        if(!columnExists("investigation_cases","fabric_color")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE investigation_cases ADD COLUMN fabric_color TEXT");
            }
        }
        try(Statement st=db.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS investigation_positions (
                        case_id TEXT NOT NULL, resident_id TEXT NOT NULL, space_id TEXT NOT NULL,
                        PRIMARY KEY(case_id,resident_id))
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS investigation_alibis (
                        case_id TEXT NOT NULL, resident_id TEXT NOT NULL, result TEXT NOT NULL,
                        PRIMARY KEY(case_id,resident_id))
                    """);
        }
        // v1.10: optional incident-time motive context, never reconstructed for old cases.
        if(!columnExists("investigation_cases","motive_code")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE investigation_cases ADD COLUMN motive_code TEXT");
            }
        }
        if(!columnExists("investigation_cases","motive_strength")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE investigation_cases ADD COLUMN motive_strength INTEGER");
            }
        }
        try(Statement st=db.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS investigation_backgrounds (
                        case_id TEXT NOT NULL, resident_id TEXT NOT NULL,
                        financial INTEGER NOT NULL, conflict INTEGER NOT NULL,
                        impulse INTEGER NOT NULL, PRIMARY KEY(case_id,resident_id))
                    """);
        }
        // v1.11: additive household arrears, bilateral loans and append-only ledger.
        // No historical debt is inferred from a resident's old cash balance.
        if(!columnExists("town_people","unpaid_bills")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE town_people ADD COLUMN unpaid_bills INTEGER NOT NULL DEFAULT 0");
            }
        }
        try(Statement st=db.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS economy_loans (
                        id TEXT PRIMARY KEY, lender_id TEXT NOT NULL, borrower_id TEXT NOT NULL,
                        original INTEGER NOT NULL CHECK(original>0),
                        outstanding INTEGER NOT NULL CHECK(outstanding>=0),
                        start_day INTEGER NOT NULL, due_day INTEGER NOT NULL,
                        status TEXT NOT NULL CHECK(status IN ('OPEN','OVERDUE','PAID')))
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS economy_ledger (
                        id INTEGER PRIMARY KEY AUTOINCREMENT, day INTEGER NOT NULL,
                        minute INTEGER NOT NULL, kind TEXT NOT NULL,
                        from_id TEXT NOT NULL, to_id TEXT NOT NULL,
                        loan_id TEXT, amount INTEGER NOT NULL CHECK(amount>0),
                        detail TEXT NOT NULL)
                    """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_economy_ledger_actor ON economy_ledger(from_id,to_id,id)");
        }
        // v1.12: existing residents keep their jobs; no retroactive illnesses or unemployment.
        LinkedHashMap<String,String> careerColumns=new LinkedHashMap<>();
        careerColumns.put("employed","INTEGER NOT NULL DEFAULT 1");
        careerColumns.put("unemployment_days","INTEGER NOT NULL DEFAULT 0");
        careerColumns.put("sick_until_day","INTEGER NOT NULL DEFAULT 0");
        careerColumns.put("last_hired_day","INTEGER NOT NULL DEFAULT 0");
        careerColumns.put("job_changes","INTEGER NOT NULL DEFAULT 0");
        for(var col:careerColumns.entrySet())if(!columnExists("town_people",col.getKey())) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE town_people ADD COLUMN "+col.getKey()+" "+col.getValue());
            }
        }
        try(Statement st=db.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS resident_life_changes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,day INTEGER NOT NULL,
                        minute INTEGER NOT NULL,resident_id TEXT NOT NULL,
                        kind TEXT NOT NULL,detail TEXT NOT NULL)
                    """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_life_changes_person ON resident_life_changes(resident_id,id)");
        }
        // v1.13: item custody is separate from legal title and from historic case reports.
        try(Statement st=db.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS property_items (
                        id TEXT PRIMARY KEY, type TEXT NOT NULL, owner_id TEXT NOT NULL,
                        holder_id TEXT, stored_space TEXT, stolen INTEGER NOT NULL,
                        value INTEGER NOT NULL, created_day INTEGER NOT NULL)
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS property_changes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,day INTEGER NOT NULL,
                        minute INTEGER NOT NULL,action TEXT NOT NULL,
                        item_id TEXT NOT NULL,from_id TEXT NOT NULL,to_id TEXT NOT NULL,
                        details TEXT NOT NULL)
                    """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_property_changes_item ON property_changes(item_id,id)");
        }
        if(!columnExists("investigation_cases","physical_item_id")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE investigation_cases ADD COLUMN physical_item_id TEXT");
            }
        }
        // v1.14: a returned case stays historically returned even if its wallet is stolen again.
        if(!columnExists("investigation_cases","returned")) {
            try(Statement st=db.createStatement()) {
                st.executeUpdate("ALTER TABLE investigation_cases ADD COLUMN returned INTEGER NOT NULL DEFAULT 0");
            }
        }
        // v1.14: preserve detective searches independently of hidden item custody.
        // Older saved cases start with zero searches; no retroactive search result exists.
        try(Statement st=db.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS investigation_searches (
                        case_id TEXT NOT NULL, resident_id TEXT NOT NULL, result TEXT NOT NULL,
                        PRIMARY KEY(case_id,resident_id))
                    """);
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
                PropertyLife.ensureStarterItems(town,s);
                persistTown(town,townEvents,observations);
                persistSocial(town,socialEvents);
                persistKnowledge(town);
                persistDeception(town);
                persistInvestigations(town);
                persistSearches(town);
                persistEconomy(town);
                persistLifeChanges(town);
                persistProperty(town);
                persistPlayer(town.player);
                try(Statement st=db.createStatement()) {st.executeUpdate("DELETE FROM visitor_ingredients");}
                try(PreparedStatement p=db.prepareStatement("INSERT INTO visitor_ingredients(location,product_id,quantity) VALUES(?,?,?)")) {
                    for(String location:List.of("bag","pantry")) {
                        var items=location.equals("bag")?town.player.bagItems:town.player.pantryItems;
                        for(var item:items.entrySet()) {
                            p.setString(1,location);p.setString(2,item.getKey());p.setInt(3,item.getValue());p.addBatch();
                        }
                    }
                    p.executeBatch();
                }
                persistLives(town);
                persistNature(town);
                persistCareerAccounts(town);
                try(PreparedStatement p=db.prepareStatement("INSERT INTO retail_stock(product_id,quantity) VALUES(?,?) ON CONFLICT(product_id) DO UPDATE SET quantity=excluded.quantity")) {
                    for(var item:town.retailStock.entrySet()) {
                        p.setString(1,item.getKey());p.setInt(2,item.getValue());p.addBatch();
                    }
                    p.executeBatch();
                }
                persistTechnicalWork(town);
                persistIndustry(town);
                persistBusiness(town);
                persistCityUtilities(town);
            }
            db.commit();
            if(town!=null) {
                town.pendingTransfers.clear();
                town.pendingDecisions.clear();
                town.pendingEconomy.clear();
                town.pendingLifeEvents.clear();
                town.pendingProperty.clear();
            }
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
                if(!town.people.containsKey(id)) {
                    // Never manufacture unknown IDs: only explicitly generated people can load.
                    if(!id.matches("resident-[0-9]{4}"))continue;
                    String fullName=rs.getString("name"),occupation=rs.getString("occupation");
                    if(fullName==null||fullName.isBlank()||occupation==null||occupation.isBlank())
                        throw new SQLException("Generated resident has missing identity: "+id);
                    town.people.put(id,new TownLife.Citizen(id,fullName,occupation,
                                TownLife.Place.valueOf(rs.getString("place"))));
                }
                var p=town.get(id);
                p.place=TownLife.Place.valueOf(rs.getString("place"));
                p.destination=TownLife.Place.valueOf(rs.getString("destination"));
                p.travelRemaining=rs.getInt("travel_remaining");p.activity=rs.getString("activity");
                p.hunger=rs.getInt("hunger");p.thirst=rs.getInt("thirst");p.fatigue=rs.getInt("fatigue");
                p.health=rs.getInt("health");p.money=rs.getInt("money");
                p.unpaidBills=rs.getInt("unpaid_bills");p.food=rs.getInt("food");
                p.employed=rs.getInt("employed")!=0;
                p.unemploymentDays=rs.getInt("unemployment_days");
                p.sickUntilDay=rs.getInt("sick_until_day");
                p.lastHiredDay=rs.getInt("last_hired_day");
                p.jobChanges=rs.getInt("job_changes");
                p.water=rs.getInt("water");p.age=rs.getInt("age");p.alive=rs.getInt("alive")!=0;
                String savedHomeUnit=rs.getString("home_unit");
                // Upgrading v1.3 saves assigns each existing resident a distinct unit.
                p.homeUnit=(savedHomeUnit==null||savedHomeUnit.isBlank())
                    ? TownLife.privateUnit(p.id) : savedHomeUnit;
                if(p.id.startsWith("resident-")) {
                    p.home=TownLife.Place.valueOf(rs.getString("home_place"));
                    p.workplace=TownLife.Place.valueOf(rs.getString("work_place"));
                    p.sociability=rs.getInt("sociability");
                    p.carefulness=rs.getInt("carefulness");
                    p.curiosity=rs.getInt("curiosity");
                    p.goal=rs.getString("goal");
                    p.scheduleOffset=rs.getInt("schedule_offset");
                    p.generationIndex=rs.getInt("generation_index");
                    p.personalSeed=rs.getLong("personal_seed");
                    if(p.generationIndex<=0)throw new SQLException("Invalid generation index: "+id);
                }
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
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT * FROM town_relationships")) {
            while(rs.next()) {
                String first=rs.getString("first_id"),second=rs.getString("second_id");
                if(!town.people.containsKey(first)||!town.people.containsKey(second))continue;
                boolean legacy=first.equals("marco") && second.equals("elena");
                SocialLife.Relationship r=legacy?town.relationship:new SocialLife.Relationship(first,second);
                r.affinity=rs.getInt("affinity");r.trust=rs.getInt("trust");r.tension=rs.getInt("tension");
                r.encounters=rs.getInt("encounters");r.conversations=rs.getInt("conversations");
                r.arguments=rs.getInt("arguments");r.lastInteractionTick=rs.getLong("last_interaction_tick");
                r.rng=rs.getLong("rng");r.lastSeenTick=rs.getLong("last_seen_tick");
                r.meetingSinceTick=rs.getLong("meeting_since_tick");
                r.lastAttemptDay=rs.getInt("last_attempt_day");
                if(!legacy)town.peerRelationships.put(SocialNetwork.key(first,second),r);
            }
                }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT * FROM resident_beliefs ORDER BY event_day,event_minute,event_key")) {
            while(rs.next()) {
                String resident=rs.getString("resident_id");
                if(!town.people.containsKey(resident))continue;
                var belief=new KnowledgeLife.Belief(resident,rs.getString("event_key"),
                        rs.getInt("event_day"),rs.getInt("event_minute"),rs.getString("actor_id"),
                        rs.getString("scene_id"),rs.getString("statement"),rs.getString("origin"),
                        rs.getString("learned_from"),rs.getInt("learned_day"),
                        rs.getInt("learned_minute"),rs.getInt("hops"),rs.getInt("confidence"));
                town.beliefs.computeIfAbsent(resident,ignored->new LinkedHashMap<>())
                    .put(belief.eventKey(),belief);
            }
                }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT * FROM resident_secrets ORDER BY day,minute,event_key")) {
            while(rs.next()) {
                String keeper=rs.getString("keeper_id");
                if(!town.people.containsKey(keeper))continue;
                var secret=new DeceptionLife.Secret(keeper,rs.getString("event_key"),
                        rs.getInt("day"),rs.getInt("minute"),rs.getString("reason"));
                town.secrets.computeIfAbsent(keeper,ignored->new LinkedHashMap<>())
                    .put(secret.eventKey(),secret);
            }
                }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT * FROM investigation_cases ORDER BY id")) {
            while(rs.next()) {
                if(!town.people.containsKey(rs.getString("victim_id")) ||
                        !town.people.containsKey(rs.getString("offender_id")))continue;
                var c=new InvestigationLife.CaseFile(rs.getString("id"),rs.getInt("day"),
                        rs.getInt("minute"),TownLife.Place.valueOf(rs.getString("place")),
                        rs.getString("victim_id"),rs.getString("offender_id"),
                        rs.getString("item"),rs.getInt("amount"));
                c.inspected=rs.getInt("inspected")!=0;c.solved=rs.getInt("solved")!=0;
                c.fabricColor=rs.getString("fabric_color");
                c.motiveCode=rs.getString("motive_code");
                c.motiveStrength=rs.getInt("motive_strength");
                c.physicalItemId=rs.getString("physical_item_id");
                c.returned=rs.getInt("returned")!=0;
                town.cases.put(c.id,c);
            }
                }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT case_id,resident_id FROM investigation_witnesses")) {
            while(rs.next()) {
                var c=town.cases.get(rs.getString(1));
                if(c!=null && town.people.containsKey(rs.getString(2)))c.witnesses.add(rs.getString(2));
            }
                }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT case_id,resident_id,statement FROM investigation_interviews")) {
            while(rs.next()) {
                var c=town.cases.get(rs.getString(1));
                if(c!=null && town.people.containsKey(rs.getString(2)))c.interviews.put(rs.getString(2),rs.getString(3));
            }
                }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT case_id,resident_id,space_id FROM investigation_positions")) {
            while(rs.next()) {
                var c=town.cases.get(rs.getString(1));
                if(c!=null && town.people.containsKey(rs.getString(2)))
                    c.incidentSpaces.put(rs.getString(2),rs.getString(3));
            }
                }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT case_id,resident_id,result FROM investigation_alibis")) {
            while(rs.next()) {
                var c=town.cases.get(rs.getString(1));
                if(c!=null && town.people.containsKey(rs.getString(2)))
                    c.alibiChecks.put(rs.getString(2),rs.getString(3));
            }
                }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT case_id,resident_id,result FROM investigation_searches")) {
            while(rs.next()) {
                var c=town.cases.get(rs.getString(1));
                if(c!=null && town.people.containsKey(rs.getString(2)))
                    c.searches.put(rs.getString(2),rs.getString(3));
            }
                }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT case_id,resident_id,financial,conflict,impulse FROM investigation_backgrounds")) {
            while(rs.next()) {
                var c=town.cases.get(rs.getString(1));
                if(c!=null && town.people.containsKey(rs.getString(2)))
                    c.incidentProfiles.put(rs.getString(2),new MotiveLife.Indicators(
                                rs.getInt(3),rs.getInt(4),rs.getInt(5)));
            }
                }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT * FROM economy_loans ORDER BY id")) {
            while(rs.next()) {
                String lender=rs.getString("lender_id"),borrower=rs.getString("borrower_id");
                if(!town.people.containsKey(lender)||!town.people.containsKey(borrower))continue;
                var loan=new EconomyLife.Loan(rs.getString("id"),lender,borrower,
                        rs.getInt("original"),rs.getInt("start_day"),rs.getInt("due_day"));
                loan.outstanding=rs.getInt("outstanding");
                loan.status=EconomyLife.Status.valueOf(rs.getString("status"));
                town.loans.put(loan.id,loan);
            }
                }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT * FROM property_items ORDER BY id")) {
            while(rs.next()) {
                String owner=rs.getString("owner_id"),holder=rs.getString("holder_id");
                if(!town.people.containsKey(owner) ||
                        (holder!=null && !town.people.containsKey(holder)))continue;
                var item=new PropertyLife.Item(rs.getString("id"),
                        PropertyLife.Type.valueOf(rs.getString("type")),owner,holder,
                        rs.getString("stored_space"),rs.getInt("stolen")!=0,
                        rs.getInt("value"),rs.getInt("created_day"));
                town.items.put(item.id,item);
            }
                }
        // Resources are shared town state; old worlds receive unclaimed starter stocks.
        NatureLife.ensureSources(town);
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT id,quantity FROM natural_resources")) {
            while(rs.next()) {
                var source=town.naturalResources.get(rs.getString("id"));
                if(source!=null)
                    source.stock=Math.max(0,Math.min(source.capacity,rs.getInt("quantity")));
            }
        }
        // Conserved employer/school balances survive restarts and worker payments.
        CareerLife.ensureEmployers(town);
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT account_id,balance FROM career_accounts")) {
            while(rs.next()) {
                if(town.employerFunds.containsKey(rs.getString("account_id")))
                    town.employerFunds.put(rs.getString("account_id"),
                            Math.max(0,rs.getInt("balance")));
            }
        }
        RetailLife.ensure(town);
        try(Statement st=db.createStatement();ResultSet rs=st.executeQuery("SELECT * FROM retail_stock")) {
            while(rs.next())if(town.retailStock.containsKey(rs.getString("product_id")))
                town.retailStock.put(rs.getString("product_id"),rs.getInt("quantity"));
        }
        // Physical infrastructure and materials survive the same atomic town save.
        TechnicalWorkLife.ensure(town);
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT material_id,stock FROM technical_materials")) {
            while(rs.next())if(town.technicalMaterials.containsKey(rs.getString(1)))
                town.technicalMaterials.put(rs.getString(1),Math.max(0,rs.getInt(2)));
        }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT order_id,phase,condition,completed_day FROM technical_orders")) {
            while(rs.next()) {
                var order=town.technicalOrders.get(rs.getString(1));
                if(order!=null) {
                    order.phase=TechnicalWorkLife.Phase.valueOf(rs.getString(2));
                    order.condition=Math.max(0,Math.min(100,rs.getInt(3)));
                    order.completedDay=rs.getInt(4);
                }
            }
        }
        IndustryLife.ensure(town);
        FarmLife.ensure(town);
        FuelLife.ensure(town);
        FreightLife.ensure(town);
        GardenLife.ensure(town);
        HenLife.ensure(town);
        OrchardLife.ensure(town);
        MarketStallLife.ensure(town);
        BankLife.ensure(town);
        RentalLife.ensure(town);
        ConstructionLife.ensure(town);
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT stock_id,quantity FROM industry_stocks")) {
            while(rs.next()) {
                if(town.industryStocks.containsKey(rs.getString(1)))
                    town.industryStocks.put(rs.getString(1),Math.max(0,rs.getInt(2)));
            }
        }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("""
                        SELECT extract_day,transport_day,refine_day,fabricate_day,deliver_day,
                               arrival_day,recycle_day
                        FROM industry_schedule WHERE id=1
                        """)) {
            if(rs.next()) {
                town.lastIndustryExtractDay=Math.max(0,rs.getInt(1));
                town.lastIndustryTransportDay=Math.max(0,rs.getInt(2));
                town.lastIndustryRefineDay=Math.max(0,rs.getInt(3));
                town.lastIndustryFabricateDay=Math.max(0,rs.getInt(4));
                town.lastIndustryDeliverDay=Math.max(0,rs.getInt(5));
                town.lastIndustryArrivalDay=Math.max(0,rs.getInt(6));
                town.lastIndustryRecycleDay=Math.max(0,rs.getInt(7));
            }
        }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT clean_water,last_pump_day FROM municipal_services WHERE id=1")) {
            if(rs.next()) {
                town.cleanWater=Math.max(0,Math.min(UtilityLife.TANK_CAPACITY,rs.getInt(1)));
                town.lastWaterPumpDay=Math.max(0,rs.getInt(2));
            }
        }
        // Anonymous by default on migration. No resident learns the player's identity.
        try(Statement st=db.createStatement();ResultSet rs=st.executeQuery("SELECT * FROM player_state WHERE id=1")) {
            if(rs.next()) {
                town.player.place=TownLife.Place.valueOf(rs.getString("place"));
                town.player.destination=TownLife.Place.valueOf(rs.getString("destination"));
                town.player.travelRemaining=rs.getInt("travel_remaining");
                town.player.privateName=rs.getString("private_name");
                town.player.hunger=rs.getInt("hunger");
                town.player.thirst=rs.getInt("thirst");
                town.player.fatigue=rs.getInt("fatigue");
                town.player.health=rs.getInt("health");
                town.player.alive=rs.getInt("alive")!=0;
                town.player.incarnation=Math.max(1,rs.getInt("incarnation"));
                town.player.money=rs.getInt("money");
                town.player.bread=rs.getInt("bread");
                town.player.bottledWater=rs.getInt("bottled_water");
                town.player.roomUntil=rs.getLong("room_until");
                town.player.storedBread=rs.getInt("stored_bread");
                town.player.storedWater=rs.getInt("stored_water");
                town.player.hygiene=rs.getInt("hygiene");
                town.player.ownsBicycle=rs.getInt("owns_bicycle")!=0;
                String carPlace=rs.getString("car_place");
                if(carPlace!=null)town.player.carPlace=TownLife.Place.valueOf(carPlace);
                town.player.carFuel=rs.getInt("car_fuel");town.player.carCondition=rs.getInt("car_condition");
                town.player.ownsHouse=rs.getInt("owns_house")!=0;
                String parked=rs.getString("bicycle_parked");
                if(town.player.ownsBicycle && parked!=null)town.player.bicycleParked=TownLife.Place.valueOf(parked);
                town.player.cycling=town.player.ownsBicycle && town.player.bicycleParked==null && rs.getInt("cycling")!=0;
                town.player.jobId=rs.getString("job_id");
                town.player.engineeringSkill=rs.getInt("engineering_skill");
                town.player.investigationSkill=rs.getInt("investigation_skill");
                town.player.lastWorkDay=rs.getInt("last_work_day");
                town.player.lastTechnicalDay=rs.getInt("last_technical_day");
            }
        }
        try(Statement st=db.createStatement();ResultSet rs=st.executeQuery("SELECT * FROM visitor_ingredients")) {
            while(rs.next()) {
                String id=rs.getString("product_id"),location=rs.getString("location");
                if(id.equals("bread") || id.equals("water") ||
                        RetailLife.PRODUCTS.stream().noneMatch(item->item.id().equals(id)))continue;
                if(location.equals("bag"))town.player.bagItems.put(id,rs.getInt("quantity"));
                else if(location.equals("pantry"))town.player.pantryItems.put(id,rs.getInt("quantity"));
            }
        }
        // v1.15 contacts belong to the first visitor; later visitors have
        // their own records and never inherit somebody else's introductions.
        String contactQuery=town.player.incarnation==1
                ? "SELECT * FROM player_contacts"
                : "SELECT * FROM player_life_contacts WHERE incarnation="+town.player.incarnation;
        try(Statement st=db.createStatement();ResultSet rs=st.executeQuery(contactQuery)) {
            while(rs.next()) {
                String id=rs.getString("resident_id");
                if(!town.people.containsKey(id))continue;
                PlayerLife.Contact c=new PlayerLife.Contact(id);
                c.knownName=rs.getString("known_name");
                c.conversations=rs.getInt("conversations");
                c.lastDay=rs.getInt("last_day");c.lastMinute=rs.getInt("last_minute");
                c.lastHeard=rs.getString("last_heard");
                c.trust=rs.getInt("trust");c.affinity=rs.getInt("affinity");
                c.lastPositiveDay=rs.getInt("last_positive_day");
                town.player.contacts.put(id,c);
            }
        }
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT * FROM player_lives ORDER BY incarnation")) {
            while(rs.next()) {
                int generation=rs.getInt("incarnation");
                town.pastLives.put(generation,new PlayerLife.LifeRecord(
                        generation,rs.getInt("died_day"),rs.getInt("died_minute"),
                        TownLife.Place.valueOf(rs.getString("died_place")),
                        rs.getString("private_name")));
            }
        }
        // The visitor's workshop is another independent persistent company.
        // Old save files have no row and correctly start without any business.
        BusinessLife.ensure(town);
        try(Statement st=db.createStatement();
                ResultSet rs=st.executeQuery("SELECT * FROM player_business WHERE id=1")) {
            if(rs.next()) {
                var b=town.business;
                b.registered=rs.getInt("registered")!=0;
                b.ownerIncarnation=Math.max(0,rs.getInt("owner_incarnation"));
                b.cash=Math.max(0,rs.getInt("cash"));
                b.machine=rs.getInt("machine")!=0;
                b.machineCondition=b.machine
                        ?Math.max(0,Math.min(100,rs.getInt("machine_condition"))):0;
                b.employeeId=rs.getString("employee_id");
                b.parts=Math.max(0,rs.getInt("spare_parts"));
                b.lastPayDay=Math.max(0,rs.getInt("last_pay_day"));
                b.lastPayMinute=rs.getInt("last_pay_minute");
                b.paidHours=Math.max(0,rs.getInt("paid_hours"));
                b.lastProductionDay=Math.max(0,rs.getInt("last_production_day"));
                b.lastSaleDay=Math.max(0,rs.getInt("last_sale_day"));
            }
        }
        PropertyLife.ensureStarterItems(town,state);
        return town;
    }

    private void persistLives(TownLife.Town town) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                INSERT OR IGNORE INTO player_lives
                (incarnation,died_day,died_minute,died_place,private_name)
                VALUES(?,?,?,?,?)
                """)) {
            for(var life:town.pastLives.values()) {
                p.setInt(1,life.incarnation());
                p.setInt(2,life.day());
                p.setInt(3,life.minute());
                p.setString(4,life.place().name());
                p.setString(5,life.privateName());
                p.addBatch();
            }
            p.executeBatch();
        }
    }

    private void persistCareerAccounts(TownLife.Town town) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                INSERT INTO career_accounts(account_id,balance) VALUES(?,?)
                ON CONFLICT(account_id) DO UPDATE SET balance=excluded.balance
                """)) {
            for(var entry:town.employerFunds.entrySet()) {
                p.setString(1,entry.getKey());
                p.setInt(2,entry.getValue());
                p.addBatch();
            }
            p.executeBatch();
        }
    }

    private void persistTechnicalWork(TownLife.Town town) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                INSERT INTO technical_materials(material_id,stock) VALUES(?,?)
                ON CONFLICT(material_id) DO UPDATE SET stock=excluded.stock
                """)) {
            for(var entry:town.technicalMaterials.entrySet()) {
                p.setString(1,entry.getKey());
                p.setInt(2,entry.getValue());
                p.addBatch();
            }
            p.executeBatch();
        }
        try(PreparedStatement p=db.prepareStatement("""
                INSERT INTO technical_orders(order_id,phase,condition,completed_day)
                VALUES(?,?,?,?) ON CONFLICT(order_id) DO UPDATE SET
                phase=excluded.phase,condition=excluded.condition,
                completed_day=excluded.completed_day
                """)) {
            for(var o:town.technicalOrders.values()) {
                p.setString(1,o.id);
                p.setString(2,o.phase.name());
                p.setInt(3,o.condition);
                p.setInt(4,o.completedDay);
                p.addBatch();
            }
            p.executeBatch();
        }
    }

    private void persistIndustry(TownLife.Town town) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                INSERT INTO industry_stocks(stock_id,quantity) VALUES(?,?)
                ON CONFLICT(stock_id) DO UPDATE SET quantity=excluded.quantity
                """)) {
            for(var entry:town.industryStocks.entrySet()) {
                p.setString(1,entry.getKey());
                p.setInt(2,entry.getValue());
                p.addBatch();
            }
            p.executeBatch();
        }
        try(PreparedStatement p=db.prepareStatement("""
                INSERT INTO industry_schedule(id,extract_day,transport_day,
                                              refine_day,fabricate_day,deliver_day,
                                              arrival_day,recycle_day)
                VALUES(1,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET
                extract_day=excluded.extract_day,
                transport_day=excluded.transport_day,
                refine_day=excluded.refine_day,
                fabricate_day=excluded.fabricate_day,
                deliver_day=excluded.deliver_day,
                arrival_day=excluded.arrival_day,
                recycle_day=excluded.recycle_day
                """)) {
            p.setInt(1,town.lastIndustryExtractDay);
            p.setInt(2,town.lastIndustryTransportDay);
            p.setInt(3,town.lastIndustryRefineDay);
            p.setInt(4,town.lastIndustryFabricateDay);
            p.setInt(5,town.lastIndustryDeliverDay);
            p.setInt(6,town.lastIndustryArrivalDay);
            p.setInt(7,town.lastIndustryRecycleDay);
            p.executeUpdate();
        }
    }

    private void persistBusiness(TownLife.Town town) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                INSERT INTO player_business(id,registered,owner_incarnation,cash,machine,
                                            machine_condition,employee_id,spare_parts,
                                            last_pay_day,last_pay_minute,paid_hours,
                                            last_production_day,last_sale_day)
                VALUES(1,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET
                registered=excluded.registered,
                owner_incarnation=excluded.owner_incarnation,
                cash=excluded.cash,
                machine=excluded.machine,
                machine_condition=excluded.machine_condition,
                employee_id=excluded.employee_id,
                spare_parts=excluded.spare_parts,
                last_pay_day=excluded.last_pay_day,
                last_pay_minute=excluded.last_pay_minute,
                paid_hours=excluded.paid_hours,
                last_production_day=excluded.last_production_day,
                last_sale_day=excluded.last_sale_day
                """)) {
            var b=town.business;
            p.setInt(1,b.registered?1:0);
            p.setInt(2,b.ownerIncarnation);
            p.setInt(3,b.cash);
            p.setInt(4,b.machine?1:0);
            p.setInt(5,b.machineCondition);
            p.setString(6,b.employeeId);
            p.setInt(7,b.parts);
            p.setInt(8,b.lastPayDay);
            p.setInt(9,b.lastPayMinute);
            p.setInt(10,b.paidHours);
            p.setInt(11,b.lastProductionDay);
            p.setInt(12,b.lastSaleDay);
            p.executeUpdate();
        }
    }

    private void persistCityUtilities(TownLife.Town town) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                INSERT INTO municipal_services(id,clean_water,last_pump_day)
                VALUES(1,?,?) ON CONFLICT(id) DO UPDATE SET
                clean_water=excluded.clean_water,last_pump_day=excluded.last_pump_day
                """)) {
            p.setInt(1,town.cleanWater);
            p.setInt(2,town.lastWaterPumpDay);
            p.executeUpdate();
        }
    }

    private void persistNature(TownLife.Town town) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                INSERT INTO natural_resources(id,quantity) VALUES(?,?)
                ON CONFLICT(id) DO UPDATE SET quantity=excluded.quantity
                """)) {
            for(var source:town.naturalResources.values()) {
                p.setString(1,source.id);
                p.setInt(2,source.stock);
                p.addBatch();
            }
            p.executeBatch();
        }
    }

    private void persistPlayer(PlayerLife.Player player) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                INSERT INTO player_state(id,place,destination,travel_remaining,private_name,
                                         hunger,thirst,fatigue,health,alive,incarnation,
                                         money,job_id,engineering_skill,investigation_skill,last_work_day,
                                         last_technical_day,bread,bottled_water,room_until,stored_bread,stored_water,hygiene,owns_bicycle,cycling,bicycle_parked,car_place,car_fuel,car_condition,owns_house)
                VALUES(1,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET
                place=excluded.place,destination=excluded.destination,
                travel_remaining=excluded.travel_remaining,private_name=excluded.private_name,
                hunger=excluded.hunger,thirst=excluded.thirst,
                fatigue=excluded.fatigue,health=excluded.health,
                alive=excluded.alive,incarnation=excluded.incarnation,
                money=excluded.money,job_id=excluded.job_id,
                engineering_skill=excluded.engineering_skill,
                investigation_skill=excluded.investigation_skill,
                last_work_day=excluded.last_work_day,
                last_technical_day=excluded.last_technical_day,
                bread=excluded.bread,bottled_water=excluded.bottled_water,room_until=excluded.room_until,
                stored_bread=excluded.stored_bread,stored_water=excluded.stored_water,hygiene=excluded.hygiene,
                owns_bicycle=excluded.owns_bicycle,cycling=excluded.cycling,bicycle_parked=excluded.bicycle_parked,
                car_place=excluded.car_place,car_fuel=excluded.car_fuel,car_condition=excluded.car_condition,owns_house=excluded.owns_house
                """)) {
            p.setString(1,player.place.name());
            p.setString(2,player.destination.name());
            p.setInt(3,player.travelRemaining);
            p.setString(4,player.privateName);
            p.setInt(5,player.hunger);
            p.setInt(6,player.thirst);
            p.setInt(7,player.fatigue);
            p.setInt(8,player.health);
            p.setInt(9,player.alive?1:0);
            p.setInt(10,player.incarnation);
            p.setInt(11,player.money);
            p.setString(12,player.jobId);
            p.setInt(13,player.engineeringSkill);
            p.setInt(14,player.investigationSkill);
            p.setInt(15,player.lastWorkDay);
            p.setInt(16,player.lastTechnicalDay);
            p.setInt(17,player.bread);p.setInt(18,player.bottledWater);
            p.setLong(19,player.roomUntil);
            p.setInt(20,player.storedBread);p.setInt(21,player.storedWater);p.setInt(22,player.hygiene);
            p.setInt(23,player.ownsBicycle?1:0);p.setInt(24,player.cycling?1:0);
            p.setString(25,player.bicycleParked==null?null:player.bicycleParked.name());
            p.setString(26,player.carPlace==null?null:player.carPlace.name());
            p.setInt(27,player.carFuel);p.setInt(28,player.carCondition);p.setInt(29,player.ownsHouse?1:0);
            p.executeUpdate();
        }
        String contactSql=player.incarnation==1 ? """
                INSERT INTO player_contacts
                (resident_id,known_name,conversations,last_day,last_minute,last_heard,trust,affinity,last_positive_day)
                VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT(resident_id) DO UPDATE SET
                known_name=excluded.known_name,conversations=excluded.conversations,
                last_day=excluded.last_day,last_minute=excluded.last_minute,
                last_heard=excluded.last_heard,trust=excluded.trust,affinity=excluded.affinity,
                last_positive_day=excluded.last_positive_day
                """ : """
                INSERT INTO player_life_contacts
                (incarnation,resident_id,known_name,conversations,last_day,last_minute,last_heard,trust,affinity,last_positive_day)
                VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT(incarnation,resident_id) DO UPDATE SET
                known_name=excluded.known_name,conversations=excluded.conversations,
                last_day=excluded.last_day,last_minute=excluded.last_minute,
                last_heard=excluded.last_heard,trust=excluded.trust,affinity=excluded.affinity,
                last_positive_day=excluded.last_positive_day
                """;
        try(PreparedStatement p=db.prepareStatement(contactSql)) {
            for(var c:player.contacts.values()) {
                int offset=0;
                if(player.incarnation!=1) {
                    p.setInt(1,player.incarnation);
                    offset=1;
                }
                p.setString(offset+1,c.residentId);
                p.setString(offset+2,c.knownName);
                p.setInt(offset+3,c.conversations);
                p.setInt(offset+4,c.lastDay);
                p.setInt(offset+5,c.lastMinute);
                p.setString(offset+6,c.lastHeard);
                p.setInt(offset+7,c.trust);p.setInt(offset+8,c.affinity);
                p.setInt(offset+9,c.lastPositiveDay);
                p.addBatch();
            }
            p.executeBatch();
        }
    }

    private void persistTown(TownLife.Town town,List<TownLife.Event> events,
            List<TownLife.Observation> observations) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO town_people(id,place,destination,travel_remaining,activity,hunger,thirst,fatigue,
                        health,money,food,water,age,alive,name,occupation,home_place,work_place,
                        sociability,carefulness,curiosity,goal,schedule_offset,generation_index,personal_seed,home_unit,unpaid_bills,employed,unemployment_days,sick_until_day,last_hired_day,job_changes)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT(id) DO UPDATE SET
                    place=excluded.place,destination=excluded.destination,
                    travel_remaining=excluded.travel_remaining,activity=excluded.activity,
                    hunger=excluded.hunger,thirst=excluded.thirst,fatigue=excluded.fatigue,
                    health=excluded.health,money=excluded.money,food=excluded.food,
                    water=excluded.water,age=excluded.age,alive=excluded.alive,
                    name=excluded.name,occupation=excluded.occupation,
                    home_place=excluded.home_place,work_place=excluded.work_place,
                    sociability=excluded.sociability,carefulness=excluded.carefulness,
                    curiosity=excluded.curiosity,goal=excluded.goal,
                    schedule_offset=excluded.schedule_offset,generation_index=excluded.generation_index,
                    personal_seed=excluded.personal_seed,home_unit=excluded.home_unit,
                    unpaid_bills=excluded.unpaid_bills,
                    employed=excluded.employed,unemployment_days=excluded.unemployment_days,
                    sick_until_day=excluded.sick_until_day,last_hired_day=excluded.last_hired_day,
                    job_changes=excluded.job_changes
                        """)) {
                            for(var c:town.people.values()) {
                                p.setString(1,c.id);p.setString(2,c.place.name());p.setString(3,c.destination.name());
                                p.setInt(4,c.travelRemaining);p.setString(5,c.activity);
                                p.setInt(6,c.hunger);p.setInt(7,c.thirst);p.setInt(8,c.fatigue);
                                p.setInt(9,c.health);p.setInt(10,c.money);p.setInt(11,c.food);
                                p.setInt(12,c.water);p.setInt(13,c.age);p.setInt(14,c.alive?1:0);
                                p.setString(15,c.name);p.setString(16,c.occupation);
                                p.setString(17,c.home.name());p.setString(18,c.workplace.name());
                                p.setInt(19,c.sociability);p.setInt(20,c.carefulness);
                                p.setInt(21,c.curiosity);p.setString(22,c.goal);
                                p.setInt(23,c.scheduleOffset);p.setInt(24,c.generationIndex);
                                p.setLong(25,c.personalSeed);p.setString(26,c.homeUnit);p.setInt(27,c.unpaidBills);
                                p.setInt(28,c.employed?1:0);p.setInt(29,c.unemploymentDays);
                                p.setInt(30,c.sickUntilDay);p.setInt(31,c.lastHiredDay);p.setInt(32,c.jobChanges);
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
                    INSERT INTO town_events(day,minute,actor_id,place,space_id,text) VALUES(?,?,?,?,?,?)
                    """)) {
                        for(var e:events) {
                            p.setInt(1,e.day());p.setInt(2,e.minute());p.setString(3,e.actorId());
                            p.setString(4,e.place().name());p.setString(5,e.spaceId());
                            p.setString(6,e.text());p.addBatch();
                        }
                        if(!events.isEmpty())p.executeBatch();
                    }
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO town_observations(witness_id,day,minute,place,space_id,text) VALUES(?,?,?,?,?,?)
                    """)) {
                        for(var o:observations) {
                            p.setString(1,o.witnessId());p.setInt(2,o.day());p.setInt(3,o.minute());
                            p.setString(4,o.place().name());p.setString(5,o.spaceId());
                            p.setString(6,o.text());p.addBatch();
                        }
                        if(!observations.isEmpty())p.executeBatch();
                    }
    }

    List<TownLife.Event> townEvents(int limit) throws SQLException {
        List<TownLife.Event> entries=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("""
                    SELECT day,minute,actor_id,place,space_id,text FROM town_events ORDER BY id DESC LIMIT ?
                    """)) {
                        p.setInt(1,limit);
                        try(ResultSet r=p.executeQuery()) {
                            while(r.next())entries.add(new TownLife.Event(r.getInt(1),r.getInt(2),
                                        r.getString(3),TownLife.Place.valueOf(r.getString(4)),r.getString(5),r.getString(6)));
                        }
                    }
        Collections.reverse(entries);return entries;
    }
    List<TownLife.Observation> observations(String witnessId,int limit) throws SQLException {
        List<TownLife.Observation> entries=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("""
                    SELECT day,minute,place,space_id,text FROM town_observations
                    WHERE witness_id=? ORDER BY id DESC LIMIT ?
                    """)) {
                        p.setString(1,witnessId);p.setInt(2,limit);
                        try(ResultSet r=p.executeQuery()) {
                            while(r.next())entries.add(new TownLife.Observation(witnessId,r.getInt(1),
                                        r.getInt(2),TownLife.Place.valueOf(r.getString(3)),r.getString(4),r.getString(5)));
                        }
                    }
        Collections.reverse(entries);return entries;
    }


    private void persistSocial(TownLife.Town town,List<SocialLife.Interaction> interactions) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO town_relationships(first_id,second_id,affinity,trust,tension,encounters,
                        conversations,arguments,last_interaction_tick,rng,last_seen_tick,meeting_since_tick,last_attempt_day)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT(first_id,second_id) DO UPDATE SET
                    affinity=excluded.affinity,trust=excluded.trust,tension=excluded.tension,
                    encounters=excluded.encounters,conversations=excluded.conversations,
                    arguments=excluded.arguments,last_interaction_tick=excluded.last_interaction_tick,
                    rng=excluded.rng,last_seen_tick=excluded.last_seen_tick,
                    meeting_since_tick=excluded.meeting_since_tick,last_attempt_day=excluded.last_attempt_day
                    """)) {
                        List<SocialLife.Relationship> all=new ArrayList<>();
                        all.add(town.relationship);
                        all.addAll(town.peerRelationships.values());
                        for(var r:all) {
                            p.setString(1,r.firstId);p.setString(2,r.secondId);p.setInt(3,r.affinity);
                            p.setInt(4,r.trust);p.setInt(5,r.tension);p.setInt(6,r.encounters);
                            p.setInt(7,r.conversations);p.setInt(8,r.arguments);p.setLong(9,r.lastInteractionTick);
                            p.setLong(10,r.rng);p.setLong(11,r.lastSeenTick);p.setLong(12,r.meetingSinceTick);
                            p.setInt(13,r.lastAttemptDay);
                            p.addBatch();
                        }
                        p.executeBatch();
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

    private void persistKnowledge(TownLife.Town town) throws SQLException {
        // UPSERT retains provenance on restart and lets a later direct witness
        // upgrade a second-hand report to direct observation of the same event.
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO resident_beliefs(resident_id,event_key,event_day,event_minute,
                        actor_id,scene_id,statement,origin,learned_from,learned_day,learned_minute,hops,confidence)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT(resident_id,event_key) DO UPDATE SET
                    statement=excluded.statement,actor_id=excluded.actor_id,
                    scene_id=excluded.scene_id,origin=excluded.origin,
                    learned_from=excluded.learned_from,learned_day=excluded.learned_day,learned_minute=excluded.learned_minute,
                    hops=excluded.hops,confidence=excluded.confidence
                    """)) {
                        for(var entries:town.beliefs.values())for(var b:entries.values()) {
                            p.setString(1,b.residentId());p.setString(2,b.eventKey());
                            p.setInt(3,b.eventDay());p.setInt(4,b.eventMinute());
                            p.setString(5,b.actorId());p.setString(6,b.sceneId());
                            p.setString(7,b.statement());p.setString(8,b.origin());
                            p.setString(9,b.learnedFrom());p.setInt(10,b.learnedDay());
                            p.setInt(11,b.learnedMinute());p.setInt(12,b.hops());
                            p.setInt(13,b.confidence());p.addBatch();
                        }
                        p.executeBatch();
                    }
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO information_transfers(day,minute,from_id,to_id,event_key,confidence,hops,statement)
                    VALUES(?,?,?,?,?,?,?,?)
                    """)) {
                        for(var t:town.pendingTransfers) {
                            p.setInt(1,t.day());p.setInt(2,t.minute());
                            p.setString(3,t.fromId());p.setString(4,t.toId());
                            p.setString(5,t.eventKey());p.setInt(6,t.confidence());
                            p.setInt(7,t.hops());p.setString(8,t.statement());p.addBatch();
                        }
                        if(!town.pendingTransfers.isEmpty())p.executeBatch();
                    }
    }

    private void persistDeception(TownLife.Town town) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT OR IGNORE INTO resident_secrets(keeper_id,event_key,day,minute,reason)
                    VALUES(?,?,?,?,?)
                    """)) {
                        for(var secrets:town.secrets.values())for(var s:secrets.values()) {
                            p.setString(1,s.keeperId());p.setString(2,s.eventKey());
                            p.setInt(3,s.day());p.setInt(4,s.minute());p.setString(5,s.reason());
                            p.addBatch();
                        }
                        p.executeBatch();
                    }
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO deception_decisions(day,minute,from_id,to_id,event_key,kind,statement)
                    VALUES(?,?,?,?,?,?,?)
                    """)) {
                        for(var d:town.pendingDecisions) {
                            p.setInt(1,d.day());p.setInt(2,d.minute());
                            p.setString(3,d.fromId());p.setString(4,d.toId());
                            p.setString(5,d.eventKey());p.setString(6,d.kind());
                            p.setString(7,d.statement());p.addBatch();
                        }
                        if(!town.pendingDecisions.isEmpty())p.executeBatch();
                    }
    }

    List<DeceptionLife.Secret> secrets(String keeperId,int limit) throws SQLException {
        List<DeceptionLife.Secret> result=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("""
                    SELECT * FROM resident_secrets WHERE keeper_id=? ORDER BY day DESC,minute DESC LIMIT ?
                    """)) {
                        p.setString(1,keeperId);p.setInt(2,limit);
                        try(ResultSet rs=p.executeQuery()) { while(rs.next())result.add(new DeceptionLife.Secret(
                                    rs.getString("keeper_id"),rs.getString("event_key"),rs.getInt("day"),
                                    rs.getInt("minute"),rs.getString("reason"))); }
                    }
        Collections.reverse(result);return result;
    }

    List<DeceptionLife.Decision> deceptions(int limit) throws SQLException {
        List<DeceptionLife.Decision> result=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("""
                    SELECT * FROM deception_decisions ORDER BY id DESC LIMIT ?
                    """)) {
                        p.setInt(1,limit);
                        try(ResultSet rs=p.executeQuery()) { while(rs.next())result.add(new DeceptionLife.Decision(
                                    rs.getInt("day"),rs.getInt("minute"),rs.getString("from_id"),
                                    rs.getString("to_id"),rs.getString("event_key"),rs.getString("kind"),
                                    rs.getString("statement"))); }
                    }
        Collections.reverse(result);return result;
    }

    List<KnowledgeLife.Belief> beliefs(String residentId,int limit) throws SQLException {
        List<KnowledgeLife.Belief> result=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("""
                    SELECT * FROM resident_beliefs WHERE resident_id=?
                    ORDER BY learned_day DESC,learned_minute DESC,event_key DESC LIMIT ?
                    """)) {
                        p.setString(1,residentId);p.setInt(2,limit);
                        try(ResultSet rs=p.executeQuery()) {
                            while(rs.next())result.add(new KnowledgeLife.Belief(residentId,
                                        rs.getString("event_key"),rs.getInt("event_day"),
                                        rs.getInt("event_minute"),rs.getString("actor_id"),
                                        rs.getString("scene_id"),rs.getString("statement"),rs.getString("origin"),
                                        rs.getString("learned_from"),rs.getInt("learned_day"),
                                        rs.getInt("learned_minute"),rs.getInt("hops"),rs.getInt("confidence")));
                        }
                    }
        Collections.reverse(result);return result;
    }

    List<KnowledgeLife.Transfer> informationTransfers(int limit) throws SQLException {
        List<KnowledgeLife.Transfer> result=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("""
                    SELECT * FROM information_transfers ORDER BY id DESC LIMIT ?
                    """)) {
                        p.setInt(1,limit);
                        try(ResultSet rs=p.executeQuery()) {
                            while(rs.next())result.add(new KnowledgeLife.Transfer(rs.getInt("day"),
                                        rs.getInt("minute"),rs.getString("from_id"),rs.getString("to_id"),
                                        rs.getString("event_key"),rs.getInt("confidence"),rs.getInt("hops"),
                                        rs.getString("statement")));
                        }
                    }
        Collections.reverse(result);return result;
    }

    private void persistInvestigations(TownLife.Town town) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO investigation_cases(id,day,minute,place,victim_id,offender_id,item,amount,inspected,solved,fabric_color,motive_code,motive_strength,physical_item_id,returned)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT(id) DO UPDATE SET inspected=excluded.inspected,solved=excluded.solved,
                    fabric_color=excluded.fabric_color,
                    motive_code=excluded.motive_code,motive_strength=excluded.motive_strength,
                    physical_item_id=excluded.physical_item_id,
                    returned=excluded.returned
                    """)) {
                        for(var c:town.cases.values()) {
                            p.setString(1,c.id);p.setInt(2,c.day);p.setInt(3,c.minute);
                            p.setString(4,c.place.name());p.setString(5,c.victimId);p.setString(6,c.offenderId);
                            p.setString(7,c.item);p.setInt(8,c.amount);
                            p.setInt(9,c.inspected?1:0);p.setInt(10,c.solved?1:0);
                            p.setString(11,c.fabricColor);p.setString(12,c.motiveCode);
                            if(c.motiveCode==null)p.setNull(13,java.sql.Types.INTEGER);
                            else p.setInt(13,c.motiveStrength);
                            p.setString(14,c.physicalItemId);p.setInt(15,c.returned?1:0);
                            p.addBatch();
                        }
                        p.executeBatch();
                    }
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT OR IGNORE INTO investigation_backgrounds(case_id,resident_id,financial,conflict,impulse)
                    VALUES(?,?,?,?,?)
                    """)) {
                        for(var c:town.cases.values())for(var entry:c.incidentProfiles.entrySet()) {
                            p.setString(1,c.id);p.setString(2,entry.getKey());
                            var v=entry.getValue();p.setInt(3,v.financial());p.setInt(4,v.conflict());
                            p.setInt(5,v.impulse());p.addBatch();
                        }
                        p.executeBatch();
                    }
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT OR IGNORE INTO investigation_witnesses(case_id,resident_id) VALUES(?,?)
                    """)) {
                        for(var c:town.cases.values())for(var witness:c.witnesses) {
                            p.setString(1,c.id);p.setString(2,witness);p.addBatch();
                        }
                        p.executeBatch();
                    }
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO investigation_interviews(case_id,resident_id,statement) VALUES(?,?,?)
                    ON CONFLICT(case_id,resident_id) DO UPDATE SET statement=excluded.statement
                    """)) {
                        for(var c:town.cases.values())for(var entry:c.interviews.entrySet()) {
                            p.setString(1,c.id);p.setString(2,entry.getKey());
                            p.setString(3,entry.getValue());p.addBatch();
                        }
                        p.executeBatch();
                    }
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT OR IGNORE INTO investigation_positions(case_id,resident_id,space_id) VALUES(?,?,?)
                    """)) {
                        for(var c:town.cases.values())for(var entry:c.incidentSpaces.entrySet()) {
                            p.setString(1,c.id);p.setString(2,entry.getKey());p.setString(3,entry.getValue());p.addBatch();
                        }
                        p.executeBatch();
                    }
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO investigation_alibis(case_id,resident_id,result) VALUES(?,?,?)
                    ON CONFLICT(case_id,resident_id) DO UPDATE SET result=excluded.result
                    """)) {
                        for(var c:town.cases.values())for(var entry:c.alibiChecks.entrySet()) {
                            p.setString(1,c.id);p.setString(2,entry.getKey());p.setString(3,entry.getValue());p.addBatch();
                        }
                        p.executeBatch();
                    }
    }

    private void persistSearches(TownLife.Town town) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO investigation_searches(case_id,resident_id,result) VALUES(?,?,?)
                    ON CONFLICT(case_id,resident_id) DO UPDATE SET result=excluded.result
                    """)) {
                        for(var c:town.cases.values())for(var entry:c.searches.entrySet()) {
                            p.setString(1,c.id);p.setString(2,entry.getKey());
                            p.setString(3,entry.getValue());p.addBatch();
                        }
                        p.executeBatch();
                    }
    }

    private void persistEconomy(TownLife.Town town) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO economy_loans(id,lender_id,borrower_id,original,outstanding,start_day,due_day,status)
                    VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET
                    outstanding=excluded.outstanding,status=excluded.status
                    """)) {
                        for(var loan:town.loans.values()) {
                            p.setString(1,loan.id);p.setString(2,loan.lenderId);p.setString(3,loan.borrowerId);
                            p.setInt(4,loan.original);p.setInt(5,loan.outstanding);
                            p.setInt(6,loan.day);p.setInt(7,loan.dueDay);p.setString(8,loan.status.name());p.addBatch();
                        }
                        p.executeBatch();
                    }
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO economy_ledger(day,minute,kind,from_id,to_id,loan_id,amount,detail)
                    VALUES(?,?,?,?,?,?,?,?)
                    """)) {
                        for(var entry:town.pendingEconomy) {
                            p.setInt(1,entry.day());p.setInt(2,entry.minute());p.setString(3,entry.kind().name());
                            p.setString(4,entry.fromId());p.setString(5,entry.toId());
                            p.setString(6,entry.loanId());p.setInt(7,entry.amount());
                            p.setString(8,entry.detail());p.addBatch();
                        }
                        if(!town.pendingEconomy.isEmpty())p.executeBatch();
                    }
    }

    List<EconomyLife.Entry> economyLedger(String residentId,int limit) throws SQLException {
        List<EconomyLife.Entry> result=new ArrayList<>();
        String sql=residentId==null
            ?"SELECT day,minute,kind,from_id,to_id,loan_id,amount,detail FROM economy_ledger ORDER BY id DESC LIMIT ?"
            :"SELECT day,minute,kind,from_id,to_id,loan_id,amount,detail FROM economy_ledger WHERE from_id=? OR to_id=? ORDER BY id DESC LIMIT ?";
        try(PreparedStatement p=db.prepareStatement(sql)) {
            int i=1;
            if(residentId!=null){p.setString(i++,residentId);p.setString(i++,residentId);}
            p.setInt(i,limit);
            try(ResultSet rs=p.executeQuery()){
                while(rs.next())result.add(new EconomyLife.Entry(rs.getInt(1),rs.getInt(2),
                            EconomyLife.Kind.valueOf(rs.getString(3)),rs.getString(4),rs.getString(5),
                            rs.getString(6),rs.getInt(7),rs.getString(8)));
            }
        }
        Collections.reverse(result);return result;
    }


    private void persistLifeChanges(TownLife.Town town) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO resident_life_changes(day,minute,resident_id,kind,detail) VALUES(?,?,?,?,?)
                    """)) {
                        for(var e:town.pendingLifeEvents) {
                            p.setInt(1,e.day());p.setInt(2,e.minute());p.setString(3,e.residentId());
                            p.setString(4,e.kind().name());p.setString(5,e.detail());p.addBatch();
                        }
                        if(!town.pendingLifeEvents.isEmpty())p.executeBatch();
                    }
    }

    List<EmploymentLife.Event> residentLifeEvents(String residentId,int limit) throws SQLException {
        List<EmploymentLife.Event> result=new ArrayList<>();
        String sql=residentId==null
            ? "SELECT day,minute,resident_id,kind,detail FROM resident_life_changes ORDER BY id DESC LIMIT ?"
            : "SELECT day,minute,resident_id,kind,detail FROM resident_life_changes WHERE resident_id=? ORDER BY id DESC LIMIT ?";
        try(PreparedStatement p=db.prepareStatement(sql)) {
            int i=1;
            if(residentId!=null)p.setString(i++,residentId);
            p.setInt(i,limit);
            try(ResultSet rs=p.executeQuery()) {
                while(rs.next())result.add(new EmploymentLife.Event(rs.getInt(1),rs.getInt(2),rs.getString(3),
                            EmploymentLife.Kind.valueOf(rs.getString(4)),rs.getString(5)));
            }
        }
        Collections.reverse(result);return result;
    }

    private void persistProperty(TownLife.Town town) throws SQLException {
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO property_items(id,type,owner_id,holder_id,stored_space,stolen,value,created_day)
                    VALUES(?,?,?,?,?,?,?,?)
                    ON CONFLICT(id) DO UPDATE SET
                    owner_id=excluded.owner_id,holder_id=excluded.holder_id,
                    stored_space=excluded.stored_space,stolen=excluded.stolen
                    """)) {
                        for(var item:town.items.values()) {
                            p.setString(1,item.id);p.setString(2,item.type.name());
                            p.setString(3,item.ownerId);p.setString(4,item.holderId);
                            p.setString(5,item.storedSpace);p.setInt(6,item.stolen?1:0);
                            p.setInt(7,item.value);p.setInt(8,item.createdDay);p.addBatch();
                        }
                        p.executeBatch();
                    }
        try(PreparedStatement p=db.prepareStatement("""
                    INSERT INTO property_changes(day,minute,action,item_id,from_id,to_id,details)
                    VALUES(?,?,?,?,?,?,?)
                    """)) {
                        for(var change:town.pendingProperty) {
                            p.setInt(1,change.day());p.setInt(2,change.minute());
                            p.setString(3,change.action().name());p.setString(4,change.itemId());
                            p.setString(5,change.fromId());p.setString(6,change.toId());
                            p.setString(7,change.details());p.addBatch();
                        }
                        if(!town.pendingProperty.isEmpty())p.executeBatch();
                    }
    }

    List<PropertyLife.Change> propertyChanges(int limit) throws SQLException {
        List<PropertyLife.Change> result=new ArrayList<>();
        try(PreparedStatement p=db.prepareStatement("""
                    SELECT day,minute,action,item_id,from_id,to_id,details
                    FROM property_changes ORDER BY id DESC LIMIT ?
                    """)) {
                        p.setInt(1,limit);
                        try(ResultSet rs=p.executeQuery()) {
                            while(rs.next())result.add(new PropertyLife.Change(
                                        rs.getInt(1),rs.getInt(2),PropertyLife.Action.valueOf(rs.getString(3)),
                                        rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7)));
                        }
                    }
        Collections.reverse(result);return result;
    }

    @Override public void close() throws SQLException {db.close();}
    }
