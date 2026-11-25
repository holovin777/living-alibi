# Weather, temperature and seasons — v1.16 prototype

## Authoritative game weather

The **Java engine** determines season and local climate using only the
authoritative game day and minute. It does not call a weather API, use the
real date, contact OpenAI, or create additional SQLite weather columns.
A saved time reconstructs the same sky and temperature after reopening.

The calendar has 365-day years, starting in spring. Weather remains
constant throughout each game day in this first implementation, while
temperature varies by time of day. These are **fictional game-balance
values**, not accurate climatic predictions for a real location.

The game supports four sky states:
- `CLEAR` — clear conditions
- `CLOUDY` — cloud cover
- `RAIN` — liquid precipitation (spring, summer and autumn)
- `SNOW` — frozen precipitation (winter)

Temperature is expressed in integer degrees Celsius. Day/night variation
and precipitation cooling are part of the deterministic world model.

## Consequences that already work

- **Outdoor sleep:** `sleep [hours]` uses conditions during each hour
  of the rest interval, including after midnight. Cold, rain and snow
  impose additional health risk at exposed places. Covered public approaches
  have no extra weather penalty, but do not grant a house or private room.
- **Rainfall:** puddles refill by up to three accessible units on rainy
  day transitions, never beyond capacity five. They do not regenerate
  on clear days, and snowfall is not immediately drinkable rain.
- **Wild berries:** replenishment at midnight is seasonal: spring +2,
  summer +3, autumn +1 and winter +0, with capacity still capped at 16.
  This models accessible harvest growth, not instantaneous plant biology.
- **River water:** the existing abstract river-accessible supply replenishes
  by 40 per game day, capped at 120. Advanced hydrology is future work.

## View and API

In the console, `weather` displays year, season, sky and temperature.
The `me` command shows the season.

The Spring Boot `GET /api/v1/world` snapshot includes:

```json
{
  "day": 1,
  "minute": 450,
  "year": 1,
  "dayOfYear": 1,
  "season": "SPRING",
  "weather": {
    "sky": "RAIN",
    "temperatureC": 11,
    "raining": true,
    "snowing": false
  }
}
```

This snippet illustrates the climate fields only; real responses also include
player, nearby residents and local natural resources. Godot should draw
rain, snow and daylight from the snapshot rather than computing another
weather simulation in GDScript.

## Persistence, tests and current limits

`WeatherSmokeTest` covers stable weather generation, seasonal temperatures,
sleep exposure and resource changes. `WeatherDatabaseSmokeTest` advances
through a rainy midnight and confirms that SQLite retains its resource
effect without refilling again on reload. Existing `FreshWorldSmokeTest`
and Spring HTTP first-run checks remain required.

Not implemented: storm fronts moving across map cells, wind, soil
moisture, rainfall duration within the day, seasons affecting NPC behavior,
weather history snapshots, indoor temperatures, clothing, fires, building
insulation, nor a rendered Godot scene. Do not apply this fictional
model to real-world survival or medical judgments.
