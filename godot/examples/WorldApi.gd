extends Node
## Godot 4 adapter for the authoritative local Living Alibi Java world.
## One request at a time prevents out-of-order scene changes and duplicate wages.

signal world_updated(snapshot: Dictionary)
signal locations_updated(locations: Array)
signal job_offers_updated(jobs: Array)
signal work_orders_updated(orders: Array)
signal industry_updated(report: Dictionary)
signal business_updated(report: Dictionary)
signal business_candidates_updated(candidates: Array)
signal action_feedback(message: String)
signal request_started(route: String)
signal request_finished(route: String)
signal request_failed(message: String)

const BASE_URL := "http://127.0.0.1:8080/api/v1"
var http_request: HTTPRequest
var _busy := false
var _pending_route := ""

func _ready() -> void:
    http_request = HTTPRequest.new()
    add_child(http_request)
    http_request.request_completed.connect(_on_request_completed)

func is_busy() -> bool:
    return _busy

func refresh_world() -> void:
    _send("/world", HTTPClient.METHOD_GET, {})

func list_locations() -> void:
    _send("/locations", HTTPClient.METHOD_GET, {})

func list_jobs() -> void:
    _send("/jobs", HTTPClient.METHOD_GET, {})

func list_work_orders() -> void:
    _send("/work-orders", HTTPClient.METHOD_GET, {})

func view_industry() -> void:
    _send("/industry", HTTPClient.METHOD_GET, {})

func view_business() -> void:
    _send("/business", HTTPClient.METHOD_GET, {})

func business_candidates() -> void:
    _send("/business/candidates", HTTPClient.METHOD_GET, {})

func business_open() -> void:
    _send("/actions/business/open", HTTPClient.METHOD_POST, {})

func business_invest(amount: int) -> void:
    _send("/actions/business/invest", HTTPClient.METHOD_POST, {"amount": amount})

func business_machine() -> void:
    _send("/actions/business/machine", HTTPClient.METHOD_POST, {})

func business_service() -> void:
    _send("/actions/business/service", HTTPClient.METHOD_POST, {})

func business_hire(resident_id: String) -> void:
    _send("/actions/business/hire", HTTPClient.METHOD_POST, {"residentId": resident_id})

func business_withdraw(amount: int) -> void:
    _send("/actions/business/withdraw", HTTPClient.METHOD_POST, {"amount": amount})

func diagnose(order_id: String) -> void:
    _send("/actions/diagnose", HTTPClient.METHOD_POST, {"orderId": order_id})

func repair(order_id: String) -> void:
    _send("/actions/repair", HTTPClient.METHOD_POST, {"orderId": order_id})

func apply_for_job(job_id: String) -> void:
    _send("/actions/apply", HTTPClient.METHOD_POST, {"jobId": job_id})

func study_subject(subject: String) -> void:
    _send("/actions/study", HTTPClient.METHOD_POST, {"subject": subject})

func work_shift() -> void:
    _send("/actions/work", HTTPClient.METHOD_POST, {})

func prospect() -> void:
    _send("/actions/prospect", HTTPClient.METHOD_POST, {})

func speak_to(resident_id: String, message: String) -> void:
    _send("/actions/speak", HTTPClient.METHOD_POST, {"residentId": resident_id, "message": message})

func car_action(action: String) -> void:
    _send("/actions/car",HTTPClient.METHOD_POST,{"action":action})

func freight_action(action: String,destination: String = "") -> void:
    _send("/actions/freight",HTTPClient.METHOD_POST,{"action":action,"destination":destination})

func drive_to(destination: String) -> void:
    _send("/actions/drive",HTTPClient.METHOD_POST,{"destination":destination})

func buy_bicycle() -> void:
    _send("/actions/buy-bicycle",HTTPClient.METHOD_POST,{})

func transport_mode(mode: String) -> void:
    _send("/actions/transport",HTTPClient.METHOD_POST,{"mode":mode})

func go_to(destination: String) -> void:
    _send("/actions/go", HTTPClient.METHOD_POST, {"destination": destination})

func buy_product(id: String) -> void:
    _send("/actions/buy", HTTPClient.METHOD_POST, {"productId": id})

func consume_product(id: String) -> void:
    _send("/actions/consume", HTTPClient.METHOD_POST, {"productId": id})

func drink_from(source_id: String) -> void:
    _send("/actions/drink", HTTPClient.METHOD_POST, {"sourceId": source_id})

func forage(source_id: String) -> void:
    _send("/actions/forage", HTTPClient.METHOD_POST, {"sourceId": source_id})

func wait_for(minutes: int) -> void:
    _send("/actions/wait", HTTPClient.METHOD_POST, {"minutes": minutes})

func room_storage(item: String, deposit: bool) -> void:
    _send("/actions/room-storage",HTTPClient.METHOD_POST,{"item":item,"deposit":deposit})

func room_cook(recipe_id: String = "bread-soup") -> void:
    _send("/actions/room-cook",HTTPClient.METHOD_POST,{"recipeId":recipe_id})

func room_wash() -> void:
    _send("/actions/room-wash",HTTPClient.METHOD_POST,{})

func garden_action(action: String) -> void:
    _send("/actions/garden",HTTPClient.METHOD_POST,{"action":action})

func buy_house() -> void:
    _send("/actions/buy-house", HTTPClient.METHOD_POST, {})

func rent_room() -> void:
    _send("/actions/rent-room", HTTPClient.METHOD_POST, {})

func sleep_in_room(hours: int) -> void:
    _send("/actions/sleep-room", HTTPClient.METHOD_POST, {"hours":hours})

func sleep_for(hours: int) -> void:
    _send("/actions/sleep", HTTPClient.METHOD_POST, {"hours": hours})

func _send(route: String, method: int, payload: Dictionary) -> void:
    if _busy:
        request_failed.emit("Another request is still running.")
        return
    _busy = true
    _pending_route = route
    request_started.emit(route)
    var body := "" if method == HTTPClient.METHOD_GET else JSON.stringify(payload)
    var headers := PackedStringArray(["Content-Type: application/json"])
    var error := http_request.request(BASE_URL + route, headers, method, body)
    if error != OK:
        _busy = false
        _pending_route = ""
        request_finished.emit(route)
        request_failed.emit("Request could not be started: %s" % error)

func _on_request_completed(
    result: int, response_code: int, _headers: PackedStringArray,
    body: PackedByteArray
) -> void:
    var route := _pending_route
    _pending_route = ""
    _busy = false
    request_finished.emit(route)
    if result != HTTPRequest.RESULT_SUCCESS:
        request_failed.emit("Java server connection failed: %s" % result)
        return

    var response = JSON.parse_string(body.get_string_from_utf8())
    if response_code < 200 or response_code >= 300:
        var error_message := "HTTP %s" % response_code
        if typeof(response) == TYPE_DICTIONARY:
            error_message = str(response.get("error", error_message))
        request_failed.emit(error_message)
        return

    if route == "/locations" or route == "/jobs" or route == "/work-orders" or route == "/business/candidates":
        if typeof(response) != TYPE_ARRAY:
            request_failed.emit("Server returned an invalid location/job list.")
            return
        if route == "/locations":
            locations_updated.emit(response)
        elif route == "/jobs":
            job_offers_updated.emit(response)
        elif route == "/business/candidates":
            business_candidates_updated.emit(response)
        else:
            work_orders_updated.emit(response)
        return

    if typeof(response) != TYPE_DICTIONARY:
        request_failed.emit("Server returned invalid world JSON.")
        return
    if route == "/industry":
        industry_updated.emit(response)
        return
    if route == "/business":
        business_updated.emit(response)
        return
    if route.begins_with("/actions/"):
        action_feedback.emit(str(response.get("message", "Action completed.")))
    var snapshot = response.get("world", response)
    if typeof(snapshot) != TYPE_DICTIONARY:
        request_failed.emit("Server returned invalid world state.")
        return
    world_updated.emit(snapshot)
