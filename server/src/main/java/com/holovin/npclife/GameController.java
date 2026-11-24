package com.holovin.npclife;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Narrow, versioned HTTP contract for a future Godot UI. */
@RestController
@RequestMapping("/api/v1")
public class GameController {
    private final GameSession session;

    public GameController(GameSession session) {
        this.session=session;
    }

    public record MinutesRequest(int minutes) { }
    public record HoursRequest(int hours) { }
    public record DestinationRequest(String destination) { }
    public record SourceRequest(String sourceId) { }
    public record JobRequest(String jobId) { }
    public record StudyRequest(String subject) { }
    public record OrderRequest(String orderId) { }
    @GetMapping("/world")
    public GameSession.WorldView world() {
        return session.snapshot();
    }

    @GetMapping("/utilities")
    public GameSession.ServicesView utilities() {
        return session.services();
    }

    @GetMapping("/locations")
    public List<GameSession.DestinationView> locations() {
        return session.destinations();
    }

    @GetMapping("/work-orders")
    public List<GameSession.WorkOrderView> workOrders() {
        return session.workOrders();
    }

    @PostMapping("/actions/diagnose")
    public GameSession.ActionView diagnose(@RequestBody OrderRequest request)
            throws SQLException {
        return session.diagnose(request.orderId());
    }

    @PostMapping("/actions/repair")
    public GameSession.ActionView repair(@RequestBody OrderRequest request)
            throws SQLException {
        return session.repair(request.orderId());
    }

    @GetMapping("/jobs")
    public List<GameSession.JobView> jobs() {
        return session.jobs();
    }

    @PostMapping("/actions/apply")
    public GameSession.ActionView apply(@RequestBody JobRequest request)
            throws SQLException {
        return session.applyJob(request.jobId());
    }

    @PostMapping("/actions/study")
    public GameSession.ActionView study(@RequestBody StudyRequest request)
            throws SQLException {
        return session.study(request.subject());
    }

    @PostMapping("/actions/work")
    public GameSession.ActionView work() throws SQLException {
        return session.work();
    }

    @PostMapping("/actions/wait")
    public GameSession.ActionView waitMinutes(@RequestBody MinutesRequest request)
            throws SQLException {
        return session.waitMinutes(request.minutes());
    }

    @PostMapping("/actions/go")
    public GameSession.ActionView go(@RequestBody DestinationRequest request)
            throws SQLException {
        return session.move(request.destination());
    }

    @PostMapping("/actions/rest")
    public GameSession.ActionView rest(@RequestBody MinutesRequest request)
            throws SQLException {
        return session.rest(request.minutes());
    }

    @PostMapping("/actions/sleep")
    public GameSession.ActionView sleep(@RequestBody HoursRequest request)
            throws SQLException {
        return session.sleep(request.hours());
    }

    @PostMapping("/actions/drink")
    public GameSession.ActionView drink(@RequestBody SourceRequest request)
            throws SQLException {
        return session.drink(request.sourceId());
    }

    @PostMapping("/actions/forage")
    public GameSession.ActionView forage(@RequestBody SourceRequest request)
            throws SQLException {
        return session.forage(request.sourceId());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> badAction(IllegalArgumentException ex) {
        return Map.of("error",ex.getMessage());
    }
}
