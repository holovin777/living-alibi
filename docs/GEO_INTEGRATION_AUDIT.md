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

### Rebirth integration phase 2
Fixed Java lambda capture in the first phase. Linked residents aged six or older may walk locally when awake, not traveling, not sick and otherwise eligible. Outdoors the client limits movement to 24 metres around the authoritative district; interiors retain collision boundaries. F still invokes existing district travel; E at a linked adult's home exit invokes the original leaveHome action. T uses the existing typed conversation path with current-space and proximity validation. Eye height and walking speed vary by age (capsule still adult-sized). The existing clock advances for linked children aged six and older and linked adults, while family choice and early childhood remain explicitly advanced. This is local first-person movement, not continuous citywide linked-character navigation; work and school schedules can still relocate the camera.

### Linked professional workspace
F now renders the existing linked-adult college, Borgo Digital onboarding, four programming tickets, Git stages, CI/review/deployment, career promotions/audits, simulated DevOps incidents and IT consultancy operations (including staff hiring/delegation). TextEdit submissions are bounded and checked against current server-generated input prefixes, then forwarded to the original gardenAction handlers and their business rules. No arbitrary Java or shell execution, real Git access or new economic system. Qualification, office location, hours, fatigue and payroll are still enforced in Java. The UI is a scrollable menu rather than a dedicated computer scene; the game clock and NPC schedules continue during editing, so stale tasks can be rejected. Full localization, dedicated workstation visuals and end-to-end playtesting remain pending.

### Physical office workstation
The modular Offices interior now has two player desks with monitors, keyboards, PCs and a live project board. E near either computer opens a dedicated workspace menu; the server additionally validates proximity (2 m to either local interaction point) and linked office identity for software, DevOps and consultancy actions. College enrollment and lessons remain available through F at school. F outside a computer shows a workstation hint instead of code editors. The project board uses existing software project phases; colleagues are existing office residents projected by their schedules. This is still a shared modular office, not an independently mapped Borgo Digital building or a dedicated boss NPC. No real computer OS, new jobs or financial logic is created.

### Office atmosphere and coffee breaks
GeoOffice projects brief deterministic situational remarks from real visible adult office workers, using their existing sociability, curiosity, carefulness, fatigue and current project/outage status. Godot shows a short Italian speech caption only near the speaker, with no permanent name label. Remarks are presentation-only, not new social events or actual meetings, and do not generate wages or relationships. The existing linked-adult Bar Centrale orders, daily rest and eligible guest conversations now appear in F at the cafe. No duplicate physiology or payment rules. Dedicated manager/meeting scheduling, automatic NPC coffee breaks, audible speech and selectable subtitle language remain pending.

### Commerce and carrier integration
The first-life visitor can manage the existing stall from inside Market during opening hours: license, shelving/pricing, seller wages, supply reserves and standing farm orders. The existing carrier is managed at the workshop interior or outdoor logistics depot anchor: registration, cash, up to two trucks, servicing, drivers and automatic dispatch. Freight loading, route legs and unloading are available outdoors at the real logical route anchors; the original FreightLife handles stock reservations, fuel, escrow, payment and journey time. The menu exposes current reports and uses the existing gardenAction/freightAction dispatcher. Some options remain subject to detailed engine validation (fuel, spare parts, cargo, buyer budgets, relocation costs). Truck visuals and continuous freight driving are not added here, and visitor-owned companies remain separate from linked-life careers. No new ledger or database schema.

### Agriculture and property
The first-life visitor now has local F actions for the owned home's outdoor vegetable garden, greenhouse, rain barrel, compost, hens, orchard, apiary and cellar. Existing Java reports and readiness checks determine available actions. All pantry items can be transferred between home and bag, and all available recipes use the original cooking/compost logic. Market purchases of home produce reuse existing finite shelf space and cash budgets. The Farm anchor exposes existing field/barn counts (farm work remains the existing career action). Inside Offices during opening hours, buy/list/unlist/sell the existing rental apartment and manage the Via Verdi plot, materials and labor reserve. The original daily ticks handle growth, rent, construction and sale. No new property records, lease logic, or farm economy. These are menu/report integrations; separate outdoor garden meshes, individual hens, building-stage visualization and new farm maps remain pending.

### Outdoor property scenery
The geographic snapshot adds nearby outdoor scenery independently of local action availability. The owned garden shows crop growth, greenhouse, rain barrel, compost, orchard age/fruit, hive, cellar and up to three animated hen models from the existing reports. The Via Verdi site shows the original stage (including a completed building after sale). Godot caches procedural placement and rebuilds only changed reports; it checks known OSM building bounding boxes, roads, rail, trees and benches for clearance. The yard is searched within 12 m of the residential anchor and the fictional construction site within 60 m of Offices. If no candidate fits, the HUD reports that visual placement is unavailable; Java F actions remain usable. These are approximate fictional placements, not cadastral parcels, and are not tested against real-map terrain/bounds or unknown obstacles. Hen movements are visual only; no individual animal identities or pathfinding. No new save state.
