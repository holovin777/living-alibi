# Geographic client integration audit

Baseline: e0d275d (Java and Godot CI successful). Reviewed Java Life classes, GameSession public actions and gardenAction dispatch, GeoProjection, GeoController, and selected old ParkDemo controls. This is an interface/coverage audit, not a full behavioral validation of every engine subsystem.

## Architectural rule

Java remains the authoritative simulation, SQLite the only durable world, and Godot the view/controller. Reuse GameSession actions; add location and availability checks at the geographic boundary. Do not reproduce financial or family logic in GDScript. Preserve original mode compatibility and test with the same save. Distinguish visitor capabilities from linked/born character capabilities.

| Domain | Existing implementation | Geographic status after this change |
| --- | --- | --- |
| Physiology, clock, weather, permanent death | PlayerLife, TownLife, CalendarLife, WeatherLife | Partial: health, hunger, thirst, rest, weather visible; new life flow missing |
| Population, employment, home routines, households | PopulationLife, EmploymentLife, HouseholdLife, HomeRoutineLife | Partial: existing residents and domestic/work poses; capacity and entrances limited |
| Contact, beliefs, secrets, motives, memories, dialogue | PlayerLife, KnowledgeLife, DeceptionLife, SocialMemoryLife, LocalDialogue | Text conversation connected; case/evidence interface missing |
| Investigation and NPC justice | InvestigationLife, NpcJusticeLife, ArchiveLife | Simulation shared; visitor case/evidence workflow not exposed |
| Player combat, license, justice | CombatLife, WeaponLicenseLife, JusticeLife, VehicleTheftLife | Vehicle theft and detention connected; regular combat/license/justice recovery UI incomplete |
| Phone and meetings | PhoneLife, AddressLife, IntercomLife | Connected for visitor, preset responses; limited apartment entry |
| Housing and pantry | HousingLife, FoodLife | Rent, buy, sleep, wash, basic soup and limited pantry connected; broader recipes and storage catalogue pending |
| Retail, careers, technical orders | RetailLife, CareerLife, TechnicalWorkLife | Partial visitor actions; finite stock and wages reused; missing services and complete catalog |
| Transport | TransportLife, MotorLife, FuelLife, BusLife, NpcTransportLife | Partial: movement, commuting, fuel, repair; no continuous physical traffic system |
| Bank | BankLife | Phone read-only plus in-person deposit, withdrawal, borrow and repay now connected |
| Fabrication company | BusinessLife | Registration, investments, withdrawal, machine acquisition/service, local hiring and status now connected |
| Market stall and produce | MarketStallLife, StallSupplyLife, RetailLife | Not connected: existing gardenAction routes |
| Freight/logistics/second truck | FreightLife, LogisticsLife, SecondFleetLife | Not connected: existing freightAction routes |
| Garden, orchard, hens, compost, greenhouse | GardenLife, OrchardLife, HenLife | Not connected: existing gardenAction routes |
| Property rental and construction | PropertyLife, RentalLife, ConstructionLife | Not connected: existing rental/construction actions |
| Utilities, municipal budget, industry | UtilityLife, MunicipalLife, IndustryLife | Simulation shared; status and specialized controls incomplete |
| Newspaper and local civic systems | NewspaperLife, OpinionLife, ElectionLife, ArchiveLife | Basic journalist offer exists; reading/archive/civic interface missing |
| Barber, clinic and sport | barber actions, MedicalFollowupLife, SportLife, DojoLife, KumiteLife | Partial visual projection; existing linked-life services not available to visitor |
| Birth, childhood, family budget, marriage, wardrobe | RebirthLife, DemographyLife, FamilyBudgetLife, WardrobeLife | Major blocker: geographic client freezes on rebirth; original mode has linked-character actions |
| Programming and DevOps | JavaTicketLife, SoftwareLife, DeveloperCareerLife, DevOpsLife | Existing linked-life actions/editor in ParkDemo; not connected to geographic mode |
| IT company | ITBusinessLife, ITStaffLife | Existing linked-life company and contracts; geographic client missing |
| Clothing and appearance | ResidentAppearance, GameSession outfitVisual | Coarse appearance connected; detailed original assets not fully reused |

## Integration order

1. Visitor economy and public services: bank/workshop done in this change; next stall, freight, rental, construction, garden, retail catalog.
2. Rebirth/linked life parity: authoritative player identity, current location, private space, age restrictions, custody and new-life transitions; do not bypass existing prerequisites.
3. Reuse existing Java ticket, DevOps and IT business actions and old Godot editors for linked adults.
4. Investigation, civic life and public service interfaces; ensure witness knowledge and private boundaries.
5. Asset/detail and navigation parity, full real-map end-to-end tests, performance profiling.

The audit does not claim each Java subsystem is complete or that the old client exposes every action. Test evidence is tracked separately in GitHub Actions; visual, linked-life and long-running economic scenarios still require playtesting.

### Rebirth integration phase 1
F now exposes existing life phase, linked identity, family choice, bounded childhood fast-forward, focus/hobbies/allowance/outings, graduation continuation and basic linked-adult jobs, travel, home and routine actions through the existing gardenAction dispatcher. The camera follows the authoritative district and home/public interior; no new save state or duplicated player is created. Free movement and automatic clock progression remain locked during linked life for now. Use menu actions to travel and advance time. Text dialogue, full adult services and family choices, IT editors and continuous movement remain pending. Prior visitor controls remain unchanged.
