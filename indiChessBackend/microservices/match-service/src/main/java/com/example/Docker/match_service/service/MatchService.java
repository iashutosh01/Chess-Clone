package com.example.Docker.match_service.service;
import com.example.Docker.match_service.clients.UserServiceClient;
import com.example.Docker.match_service.model.GameType;
import com.example.Docker.match_service.model.Match;
import com.example.Docker.match_service.model.MatchStatus;
import com.example.Docker.match_service.repo.MatchRepo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MatchService {
    private static final Map<String, WaitingPlayer> waitingPlayers = new ConcurrentHashMap<>();
    private static final Map<Long, String[]> matchPlayers = new ConcurrentHashMap<>();
    private final UserServiceClient userServiceClient;
    private final MatchRepo matchRepo;
    private final GameService gameService;

    @Data
    private static class WaitingPlayer {
        private String username;
        private String gameType;
        private Long userId;
        private Long tempMatchId;

        public WaitingPlayer(String username, String gameType, Long userId, Long tempMatchId) {
            this.username = username;
            this.gameType = gameType;
            this.userId = userId;
            this.tempMatchId = tempMatchId;
        }
    }

    public Optional<Long> createMatch(HttpServletRequest request, String gameType) {
        String userName = request.getHeader("x-header-username"); // Extract from the header
        Long userId = Long.parseLong(request.getHeader("x-header-userid"));

        if (userName == null) {
            return Optional.empty();
        }

        System.out.println("User " + userName + " requesting " + gameType + " match");

        synchronized(this) {
            for (WaitingPlayer waitingPlayer : waitingPlayers.values()) {
                if (!waitingPlayer.getUsername().equals(userName) &&
                        waitingPlayer.getGameType().equals(gameType)) {

                    // Fetching user IDs using UserServiceClient
                    Long player1Id = userId;
                    Long player2Id = waitingPlayer.userId;

                    if (player1Id != null && player2Id != null) {
                        Match newMatch = new Match(player1Id, player2Id, MatchStatus.IN_PROGRESS, 1);
                        newMatch.setGameType(GameType.valueOf(gameType.toUpperCase()));

                        // Time configuration based on game type
                        if ("rapid".equalsIgnoreCase(gameType)) {
                            newMatch.setTimeControlMinutes(10);
                            newMatch.setTimeIncrementSeconds(0);
                        } else if ("classical".equalsIgnoreCase(gameType)) {
                            newMatch.setTimeControlMinutes(0);
                            newMatch.setTimeIncrementSeconds(0);
                        }

                        newMatch.setFenCurrent("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
                        newMatch = matchRepo.save(newMatch);
                        Long matchId = newMatch.getId();

                        matchPlayers.put(matchId, new String[]{waitingPlayer.getUsername(), userName});
                        waitingPlayers.remove(waitingPlayer.getUsername());

                        System.out.println(gameType + " match created: " + matchId);
                        return Optional.of(matchId);
                    }
                }
            }

            WaitingPlayer newWaitingPlayer = new WaitingPlayer(userName, gameType, userId, -1L);
            waitingPlayers.put(userName, newWaitingPlayer);
            System.out.println("User " + userName + " added to " + gameType + " waiting queue");

            return Optional.of(-1L);
        }
    }

    public Optional<Long> checkMatch(HttpServletRequest request) {
        String userName = request.getHeader("x-header-username");

        if (userName == null) {
            return Optional.empty();
        }

        synchronized(this) {
            if (waitingPlayers.containsKey(userName)) {
                return Optional.of(-1L);
            }

            for (Map.Entry<Long, String[]> entry : matchPlayers.entrySet()) {
                String[] players = entry.getValue();
                if (players[0].equals(userName) || players[1].equals(userName)) {
                    Long matchId = entry.getKey();
                    matchPlayers.remove(matchId);
                    waitingPlayers.remove(players[0]);
                    waitingPlayers.remove(players[1]);
                    System.out.println("Returning match " + matchId + " to " + userName);
                    return Optional.of(matchId);
                }
            }
        }

        return Optional.empty();
    }

    public boolean cancelWaiting(HttpServletRequest request) {
        String userName = request.getHeader("x-header-username");

        if (userName == null) {
            return false;
        }

        synchronized(this) {
            boolean removed = waitingPlayers.remove(userName) != null;
            if (removed) {
                System.out.println("User " + userName + " cancelled waiting");
            }
            return removed;
        }
    }

    public Map<String, Object> getGameDetails(Long matchId, HttpServletRequest request) {
        String username = request.getHeader("x-header-username");
        if (username == null) {
            throw new RuntimeException("Invalid token");
        }

        Optional<Match> matchOpt = matchRepo.findById(matchId);
        if (matchOpt.isEmpty()) {
            throw new RuntimeException("Game not found");
        }

        Match match = matchOpt.get();

        if ( !userServiceClient.getUserById(match.getPlayer1Id()).getUsername().equals(username) &&
                (match.getPlayer2Id() == null || !userServiceClient.getUserById(match.getPlayer2Id()).getUsername().equals(username))) {
            throw new RuntimeException("Not authorized");
        }

        Map<String, Object> details = new HashMap<>();
        details.put("matchId", match.getId());
        details.put("status", match.getStatus().toString());
        details.put("gameType", match.getGameType() != null ? match.getGameType().toString() : "CLASSICAL");
        details.put("createdAt", match.getCreatedAt());
        details.put("fenCurrent", match.getFenCurrent());
        details.put("currentPly", match.getCurrentPly());
        details.put("whiteTimeRemaining", match.getWhiteTimeRemaining());
        details.put("blackTimeRemaining", match.getBlackTimeRemaining());
        details.put("player1Id", match.getPlayer1Id());
        if (match.getPlayer2Id() != null) {
            details.put("player2", match.getPlayer2Id());
        }

        return details;
    }

    public Map<String, Object> getQueueStatus(HttpServletRequest request) {
        String username = request.getHeader("x-header-username");
        if (username == null) {
            throw new RuntimeException("Invalid token");
        }

        Map<String, Object> status = new HashMap<>();
        boolean isInQueue = waitingPlayers.containsKey(username);
        status.put("isInQueue", isInQueue);

        if (isInQueue) {
            WaitingPlayer waitingPlayer = waitingPlayers.get(username);
            status.put("gameType", waitingPlayer.getGameType());
        }

        long classicalCount = waitingPlayers.values().stream()
                .filter(wp -> "classical".equals(wp.getGameType()))
                .count();
        long rapidCount = waitingPlayers.values().stream()
                .filter(wp -> "rapid".equals(wp.getGameType()))
                .count();

        status.put("classicalQueueSize", classicalCount);
        status.put("rapidQueueSize", rapidCount);
        status.put("totalQueueSize", waitingPlayers.size());

        return status;
    }

    public Map<String, Object> offerDraw(Long matchId, HttpServletRequest request) {
        String username = request.getHeader("userName");
        if (username == null) {
            throw new RuntimeException("Invalid token");
        }

        Optional<Match> matchOpt = matchRepo.findById(matchId);
        if (matchOpt.isEmpty()) {
            throw new RuntimeException("Game not found");
        }

        Match match = matchOpt.get();

        if ( !userServiceClient.getUserById(match.getPlayer1Id()).getUsername().equals(username) &&
                (match.getPlayer2Id() == null || !userServiceClient.getUserById(match.getPlayer2Id()).getUsername().equals(username))) {
            throw new RuntimeException("Not authorized");
        }

        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new RuntimeException("Game is already over");
        }

        boolean isPlayer1 = userServiceClient.getUserById(match.getPlayer1Id()).getUsername().equals(username);
        match.setDrawOfferedBy(isPlayer1 ? 1 : 2);
        matchRepo.save(match);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Draw offer sent to opponent");
        result.put("drawOfferedBy", isPlayer1 ?
                userServiceClient.getUserById(match.getPlayer1Id()).getUsername() :
                userServiceClient.getUserById(match.getPlayer2Id()).getUsername());

        return result;
    }

    public Map<String, Object> respondToDrawOffer(Long matchId, boolean accept, HttpServletRequest request) {
        String username = request.getHeader("userName");
        if (username == null) {
            throw new RuntimeException("Invalid token");
        }

        Optional<Match> matchOpt = matchRepo.findById(matchId);
        if (matchOpt.isEmpty()) {
            throw new RuntimeException("Game not found");
        }

        Match match = matchOpt.get();
        boolean isPlayer1 = userServiceClient.getUserById(match.getPlayer1Id()).getUsername().equals(username);
        boolean isPlayer2 = match.getPlayer2Id() != null && userServiceClient.getUserById(match.getPlayer2Id()).getUsername().equals(username);

        if (!isPlayer1 && !isPlayer2) {
            throw new RuntimeException("Not authorized");
        }

        Integer drawOfferedBy = match.getDrawOfferedBy();
        if (drawOfferedBy == null) {
            throw new RuntimeException("No draw offer pending");
        }

        boolean isDrawOfferFromPlayer1 = drawOfferedBy == 1;
        if ((isDrawOfferFromPlayer1 && isPlayer1) || (!isDrawOfferFromPlayer1 && isPlayer2)) {
            throw new RuntimeException("You cannot respond to your own draw offer");
        }
        Map<String, Object> result = new HashMap<>();
        if (accept) {
            match.setStatus(MatchStatus.DRAW);
            match.setDrawOfferedBy(null);
            matchRepo.save(match);

            result.put("success", true);
            result.put("message", "Draw accepted");
            result.put("matchStatus", "DRAW");
        } else {
            match.setDrawOfferedBy(null);
            matchRepo.save(match);
            result.put("success", true);
            result.put("message", "Draw declined");
            result.put("matchStatus", match.getStatus().toString());
        }
        return result;
    }

    public Map<String, Object> resign(Long matchId, HttpServletRequest request) {
        String username = request.getHeader("userName");
        if (username == null) {
            throw new RuntimeException("Invalid token");
        }

        Optional<Match> matchOpt = matchRepo.findById(matchId);
        if (matchOpt.isEmpty()) {
            throw new RuntimeException("Game not found");
        }

        Match match = matchOpt.get();

        boolean isPlayer1 = userServiceClient.getUserById(match.getPlayer1Id()).getUsername().equals(username);
        boolean isPlayer2 = match.getPlayer2Id() != null && userServiceClient.getUserById(match.getPlayer2Id()).getUsername().equals(username);
        if (!isPlayer1 && !isPlayer2) {
            throw new RuntimeException("Not authorized");
        }

        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new RuntimeException("Game is already over");
        }

        if (isPlayer1) {
            match.resignByPlayer1();
        } else {
            match.resignByPlayer2();
        }

        matchRepo.save(match);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Resignation accepted");
        result.put("matchStatus", match.getStatus().toString());
        result.put("winner", userServiceClient.getUserById(match.getWinner()));
        result.put("resignedBy", isPlayer1 ?
                userServiceClient.getUserById(match.getPlayer1Id()).getUsername() :
                userServiceClient.getUserById(match.getPlayer2Id()).getUsername());

        return result;
    }

    public Map<String, Object> recordMove(Long matchId, String from, String to, String promotion, String fenAfter, HttpServletRequest request) {
        String username = request.getHeader("userName");
        if (username == null) {
            throw new RuntimeException("Invalid token");
        }

        Optional<Match> matchOpt = matchRepo.findById(matchId);
        if (matchOpt.isEmpty()) {
            throw new RuntimeException("Game not found");
        }

        Match match = matchOpt.get();

        boolean isPlayer1 = userServiceClient.getUserById(match.getPlayer1Id()).getUsername().equals(username);
        boolean isPlayer2 = match.getPlayer2Id() != null && userServiceClient.getUserById(match.getPlayer2Id()).getUsername().equals(username);

        if (!isPlayer1 && !isPlayer2) {
            throw new RuntimeException("Not authorized");
        }

        match.setCurrentPly(match.getCurrentPly() + 1);
        match.setLastMoveUci(from + to + (promotion != null ? promotion : ""));

        if (fenAfter != null) {
            match.setFenCurrent(fenAfter);
        }

        matchRepo.save(match);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("matchId", matchId);
        result.put("currentPly", match.getCurrentPly());
        result.put("fenCurrent", match.getFenCurrent());

        return result;
    }
}
