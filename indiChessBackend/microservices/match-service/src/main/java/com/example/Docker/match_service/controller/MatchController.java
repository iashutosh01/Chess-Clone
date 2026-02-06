package com.example.Docker.match_service.controller;

import com.example.Docker.match_service.service.MatchService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class MatchController {

    private final MatchService matchService;

    // Match creation endpoints
    @PostMapping("")
    public ResponseEntity<Map<String, Long>> createMatch(
            @RequestBody(required = false) Map<String, String> requestBody,
            HttpServletRequest request) {

        String gameType = "classical";
        if (requestBody != null && requestBody.containsKey("gameType")) {
            gameType = requestBody.get("gameType");
        }

        System.out.println("Creating " + gameType + " game...");

        Optional<Long> matchIdOpt = matchService.createMatch(request, gameType);

        Map<String, Long> response = new HashMap<>();
        if (matchIdOpt.isPresent()) {
            response.put("matchId", matchIdOpt.get());
            return ResponseEntity.ok(response);
        } else {
            response.put("matchId", -2L);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/rapid")
    public ResponseEntity<Map<String, Long>> createRapidMatch(HttpServletRequest request) {
        System.out.println("Creating rapid game...");

        Optional<Long> matchIdOpt = matchService.createMatch(request, "rapid");

        Map<String, Long> response = new HashMap<>();
        if (matchIdOpt.isPresent()) {
            response.put("matchId", matchIdOpt.get());
            return ResponseEntity.ok(response);
        } else {
            response.put("matchId", -2L);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/classical")
    public ResponseEntity<Map<String, Long>> createClassicalMatch(HttpServletRequest request) {
        System.out.println("Creating classical game...");

        Optional<Long> matchIdOpt = matchService.createMatch(request, "classical");

        Map<String, Long> response = new HashMap<>();
        if (matchIdOpt.isPresent()) {
            response.put("matchId", matchIdOpt.get());
            return ResponseEntity.ok(response);
        } else {
            response.put("matchId", -2L);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/check-match")
    public ResponseEntity<?> checkMatch(HttpServletRequest request) {
        String userName = request.getHeader("x-header-username");
        System.out.println("Extracted username: " + userName);

        if (userName == null) {
            System.out.println("Username is NULL, returning empty");
            return ResponseEntity.badRequest().body(Optional.empty());
        }

        Optional<Long> matchIdOpt = matchService.checkMatch(request);

        Map<String, Long> response = new HashMap<>();
        if (matchIdOpt.isPresent()) {
            response.put("matchId", matchIdOpt.get());
            return ResponseEntity.ok(response);
        } else {
            response.put("matchId", -2L);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/cancel-waiting")
    public ResponseEntity<Map<String, Boolean>> cancelWaiting(HttpServletRequest request) {
        boolean cancelled = matchService.cancelWaiting(request);

        Map<String, Boolean> response = new HashMap<>();
        response.put("cancelled", cancelled);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{matchId}/details")
    public ResponseEntity<Map<String, Object>> getGameDetails(
            @PathVariable Long matchId,
            HttpServletRequest request) {

        try {
            Map<String, Object> gameDetails = matchService.getGameDetails(matchId, request);
            return ResponseEntity.ok(gameDetails);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/queue-status")
    public ResponseEntity<Map<String, Object>> getQueueStatus(HttpServletRequest request) {
        Map<String, Object> status = matchService.getQueueStatus(request);
        return ResponseEntity.ok(status);
    }

    // REST endpoints for game actions (WebSocket is primary, these are fallback/alternative)
    @PostMapping("/{matchId}/offer-draw")
    public ResponseEntity<Map<String, Object>> offerDraw(
            @PathVariable Long matchId,
            HttpServletRequest request) {

        try {
            Map<String, Object> result = matchService.offerDraw(matchId, request);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("Not authenticated")) {
                return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("Not authorized")) {
                return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Game is already over")) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/{matchId}/respond-draw")
    public ResponseEntity<Map<String, Object>> respondToDrawOffer(
            @PathVariable Long matchId,
            @RequestBody Map<String, Boolean> drawResponse,
            HttpServletRequest request) {

        try {
            Boolean accept = drawResponse.get("accept");
            if (accept == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Draw response (accept) is required"));
            }

            Map<String, Object> result = matchService.respondToDrawOffer(matchId, accept, request);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("Not authenticated")) {
                return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("Not authorized")) {
                return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("No draw offer") ||
                    e.getMessage().contains("not the player")) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/{matchId}/resign")
    public ResponseEntity<Map<String, Object>> resign(
            @PathVariable Long matchId,
            HttpServletRequest request) {

        try {
            Map<String, Object> result = matchService.resign(matchId, request);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("Not authenticated")) {
                return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("Not authorized")) {
                return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Game is already over")) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    // Get comprehensive game details for frontend
    @GetMapping("/{matchId}")
    public ResponseEntity<Map<String, Object>> getGameDetailsForFrontend(
            @PathVariable Long matchId,
            HttpServletRequest request) {

        try {
            Map<String, Object> response = matchService.getGameDetails(matchId, request);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("Not authenticated")) {
                return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("Not authorized")) {
                return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    // Helper endpoint to record move (optional - for sync between WebSocket and REST)
    @PostMapping("/{matchId}/record-move")
    public ResponseEntity<Map<String, Object>> recordMove(
            @PathVariable Long matchId,
            @RequestBody Map<String, String> moveData,
            HttpServletRequest request) {

        try {
            String from = moveData.get("from");
            String to = moveData.get("to");
            String promotion = moveData.get("promotion");
            String fenAfter = moveData.get("fenAfter");

            Map<String, Object> result = matchService.recordMove(matchId, from, to, promotion, fenAfter, request);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("Not authenticated")) {
                return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("Not authorized")) {
                return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}
