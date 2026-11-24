extends Node
## Example Godot 4 adapter for the local Living Alibi Spring API.
## Attach to a Node and connect to world_updated to render the scene.

signal world_updated(snapshot: Dictionary)
signal request_failed(message: String)

const BASE_URL := "http://127.0.0.1:8080/api/v1"
var http_request: HTTPRequest

func _ready() -> void:
    http_request = HTTPRequest.new()
    add_child(http_request)
    http_request.request_completed.connect(_on_request_completed)

func refresh_world() -> void:
    _send("/world", HTTPClient.METHOD_GET, {})

func go_to(destination: String) -> void:
    _send("/actions/go", HTTPClient.METHOD_POST, {"destination": destination})

func drink_from(source_id: String) -> void:
    _send("/actions/drink", HTTPClient.METHOD_POST, {"sourceId": source_id})

func wait_for(minutes: int) -> void:
    _send("/actions/wait", HTTPClient.METHOD_POST, {"minutes": minutes})

func sleep_for(hours: int) -> void:
    _send("/actions/sleep", HTTPClient.METHOD_POST, {"hours": hours})

func _send(route: String, method: int, payload: Dictionary) -> void:
    var body := "" if method == HTTPClient.METHOD_GET else JSON.stringify(payload)
    var headers := PackedStringArray(["Content-Type: application/json"])
    var error := http_request.request(BASE_URL + route, headers, method, body)
    if error != OK:
        request_failed.emit("Request could not be started: %s" % error)

func _on_request_completed(
    result: int, response_code: int, _headers: PackedStringArray,
    body: PackedByteArray
) -> void:
    if result != HTTPRequest.RESULT_SUCCESS:
        request_failed.emit("Connection failed: %s" % result)
        return
    var response = JSON.parse_string(body.get_string_from_utf8())
    if typeof(response) != TYPE_DICTIONARY:
        request_failed.emit("Server returned invalid JSON.")
        return
    if response_code < 200 or response_code >= 300:
        request_failed.emit(str(response.get("error", "HTTP %s" % response_code)))
        return
    # GET returns a world object; POST returns {"message":..., "world":...}.
    var snapshot: Dictionary = response.get("world", response)
    world_updated.emit(snapshot)
