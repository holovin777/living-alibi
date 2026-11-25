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
    public record ProductRequest(String productId) { }
    public record JobRequest(String jobId) { }
    public record StudyRequest(String subject) { }
    public record OrderRequest(String orderId) { }
    public record AmountRequest(int amount) { }
    public record ResidentRequest(String residentId) { }
    public record SpeakRequest(String residentId,String message) { }
    @GetMapping("/world")
    public GameSession.WorldView world() {
        return session.snapshot();
    }

    @GetMapping("/business")
    public GameSession.BusinessView business() {
        return session.business();
    }

    @GetMapping("/business/candidates")
    public List<GameSession.CandidateView> businessCandidates() {
        return session.businessCandidates();
    }

    @PostMapping("/actions/business/open")
    public GameSession.ActionView openBusiness() throws SQLException {
        return session.openBusiness();
    }

    @PostMapping("/actions/business/invest")
    public GameSession.ActionView investBusiness(@RequestBody AmountRequest request)
            throws SQLException {
        return session.investBusiness(request.amount());
    }

    @PostMapping("/actions/business/machine")
    public GameSession.ActionView buyMachine() throws SQLException {
        return session.buyBusinessMachine();
    }

    @PostMapping("/actions/business/service")
    public GameSession.ActionView serviceMachine() throws SQLException {
        return session.serviceBusinessMachine();
    }

    @PostMapping("/actions/business/hire")
    public GameSession.ActionView hireWorker(@RequestBody ResidentRequest request)
            throws SQLException {
        return session.hireBusinessWorker(request.residentId());
    }

    @PostMapping("/actions/business/withdraw")
    public GameSession.ActionView withdrawBusiness(@RequestBody AmountRequest request)
            throws SQLException {
        return session.withdrawBusiness(request.amount());
    }

    @GetMapping("/industry")
    public GameSession.IndustryView industry() {
        return session.industry();
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

    @PostMapping("/actions/prospect")
    public GameSession.ActionView prospect() throws SQLException {
        return session.prospect();
    }

    @PostMapping("/actions/work")
    public GameSession.ActionView work() throws SQLException {
        return session.work();
    }

    @PostMapping("/actions/speak")
    public GameSession.ActionView speak(@RequestBody SpeakRequest request) throws SQLException {
        return session.speak(request.residentId(),request.message());
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

    @PostMapping("/actions/rent-room")
    public GameSession.ActionView rentRoom() throws SQLException {
        return session.rentRoom();
    }
    @PostMapping("/actions/sleep-room")
    public GameSession.ActionView sleepRoom(@RequestBody HoursRequest request) throws SQLException {
        return session.sleepInRoom(request.hours());
    }

    @PostMapping("/actions/sleep")
    public GameSession.ActionView sleep(@RequestBody HoursRequest request)
            throws SQLException {
        return session.sleep(request.hours());
    }

    @PostMapping("/actions/buy")
    public GameSession.ActionView buy(@RequestBody ProductRequest request) throws SQLException {
        return session.buy(request.productId());
    }
    @PostMapping("/actions/consume")
    public GameSession.ActionView consume(@RequestBody ProductRequest request) throws SQLException {
        return session.consume(request.productId());
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
