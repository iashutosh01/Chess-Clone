package com.example.Docker.match_service.service;

import com.example.Docker.match_service.clients.UserServiceClient;
import com.example.Docker.match_service.model.DTO.*;
import com.example.Docker.match_service.model.Match;
import com.example.Docker.match_service.model.MatchStatus;
import com.example.Docker.match_service.repo.MatchRepo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GameService {

    private final MatchRepo matchRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserServiceClient userServiceClient;
//    private final MyUserDetailsService userDetailsService;

    private final Map<Long, GameState> activeGames = new ConcurrentHashMap<>();
    private final Map<Long, List<String>> gamePlayers = new ConcurrentHashMap<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GameState {
        private String[][] board;
        private boolean isWhiteTurn;
        private String status;
        private String player1Username;
        private String player2Username;
        private LocalDateTime lastMoveTime;
    }

    public GameDTO getGameDetails(Long matchId, HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) {
            throw new RuntimeException("User not authenticated");
        }

        Optional<Match> matchOpt = matchRepo.findById(matchId);
        if (matchOpt.isEmpty()) {
            throw new RuntimeException("Game not found");
        }

        Match match = matchOpt.get();

        String playerColor = determinePlayerColor(match, username);
        boolean isMyTurn = determineMyTurn(match, username);

        GameState gameState = activeGames.get(matchId);
        if (gameState == null) {
            gameState = initializeGameState(match);
            activeGames.put(matchId, gameState);

            List<String> players = new ArrayList<>();
            players.add(userServiceClient.
                    getUserById(match.getPlayer1Id()).getUsername());
            players.add(userServiceClient.
                    getUserById(match.getPlayer2Id()).getUsername());
            gamePlayers.put(matchId, players);
        }

        GameDTO gameDTO = new GameDTO();
        gameDTO.setId(match.getId());
        gameDTO.setPlayer1Id(match.getPlayer1Id());
        gameDTO.setPlayer2Id(match.getPlayer2Id());
        gameDTO.setStatus(gameState.getStatus());
        gameDTO.setPlayerColor(playerColor);
        gameDTO.setMyTurn(isMyTurn);
        gameDTO.setBoard(gameState.getBoard());
        gameDTO.setFen(convertBoardToFEN(gameState.getBoard(), gameState.isWhiteTurn()));
        gameDTO.setCreatedAt(match.getCreatedAt());
        gameDTO.setUpdatedAt(match.getUpdatedAt());

        return gameDTO;
    }

    private String determinePlayerColor(Match match, String username) {
        if (userServiceClient.getUserById(match.getPlayer1Id()).getUsername().equals(username)) {
            return "white";
        } else if (userServiceClient.getUserById(match.getPlayer2Id()).getUsername().equals(username)) {
            return "black";
        }
        throw new RuntimeException("User not part of this game");
    }

    private boolean determineMyTurn(Match match, String username) {
        GameState gameState = activeGames.get(match.getId());
        if (gameState == null) {
            return userServiceClient.getUserById(match.getPlayer1Id()).getUsername().equals(username);
        }

        boolean isWhiteTurn = gameState.isWhiteTurn();
        if (isWhiteTurn) {
            return userServiceClient.getUserById(match.getPlayer1Id()).getUsername().equals(username);
        } else {
            return userServiceClient.getUserById(match.getPlayer2Id()).getUsername().equals(username);
        }
    }

    private GameState initializeGameState(Match match) {
        String[][] initialBoard = {
                {"r", "n", "b", "q", "k", "b", "n", "r"},
                {"p", "p", "p", "p", "p", "p", "p", "p"},
                {"", "", "", "", "", "", "", ""},
                {"", "", "", "", "", "", "", ""},
                {"", "", "", "", "", "", "", ""},
                {"", "", "", "", "", "", "", ""},
                {"P", "P", "P", "P", "P", "P", "P", "P"},
                {"R", "N", "B", "Q", "K", "B", "N", "R"}
        };

        GameState gameState = new GameState();
        gameState.setBoard(initialBoard);
        gameState.setWhiteTurn(true);
        gameState.setStatus("IN_PROGRESS");
        gameState.setPlayer1Username(userServiceClient.getUserById(match.getPlayer1Id()).getUsername());
        gameState.setPlayer2Username(userServiceClient.getUserById(match.getPlayer2Id()).getUsername());
        gameState.setLastMoveTime(LocalDateTime.now());

        return gameState;
    }

    public MoveDTO processMove(Long matchId, MoveRequestDTO moveRequest, Principal principal) {
        String username = principal.getName();

        if (moveRequest.getFromRow() == null || moveRequest.getFromCol() == null ||
                moveRequest.getToRow() == null || moveRequest.getToCol() == null) {
            throw new RuntimeException("Move coordinates cannot be null");
        }

        if (moveRequest.getPiece() == null || moveRequest.getPiece().isEmpty()) {
            throw new RuntimeException("Piece cannot be null or empty");
        }

        if (moveRequest.getPlayerColor() == null) {
            throw new RuntimeException("Player color cannot be null");
        }

        System.out.println("🎮 Processing move for game: " + matchId);
        System.out.println("👤 Player: " + username + ", Color: " + moveRequest.getPlayerColor());

        if (moveRequest.getWhiteTimeRemaining() != null) {
            // Update match with remaining time
            Match match = matchRepo.findById(matchId).orElseThrow();
            match.setWhiteTimeRemaining(moveRequest.getWhiteTimeRemaining());
            match.setBlackTimeRemaining(moveRequest.getBlackTimeRemaining());
            match.setUpdatedAt(LocalDateTime.now());
            matchRepo.save(match);
        }

        GameState gameState = activeGames.get(matchId);
        if (gameState == null) {
            System.out.println("❌ Game not found in active games: " + matchId);
            throw new RuntimeException("Game not found or not active");
        }

        boolean isWhiteTurn = gameState.isWhiteTurn();
        String expectedPlayer = isWhiteTurn ? gameState.getPlayer1Username() : gameState.getPlayer2Username();

        if (!username.equals(expectedPlayer)) {
            System.out.println("❌ Not player's turn. Expected: " + expectedPlayer + ", Got: " + username);
            throw new RuntimeException("Not your turn");
        }

        String playerColor = moveRequest.getPlayerColor();
        if (isWhiteTurn && !"white".equals(playerColor)) {
            throw new RuntimeException("Invalid move: White's turn but player is " + playerColor);
        }
        if (!isWhiteTurn && !"black".equals(playerColor)) {
            throw new RuntimeException("Invalid move: Black's turn but player is " + playerColor);
        }

        String[][] newBoard = moveRequest.getBoard();
        if (newBoard == null) {
            throw new RuntimeException("Board cannot be null");
        }

        gameState.setBoard(newBoard);
        gameState.setWhiteTurn(!isWhiteTurn);
        gameState.setLastMoveTime(LocalDateTime.now());

        // Check for game end conditions
        String gameStatus = checkGameStatus(newBoard, !isWhiteTurn);
        gameState.setStatus(gameStatus);

        activeGames.put(matchId, gameState);
        System.out.println("✅ Game state updated. Now it's " + (!isWhiteTurn ? "White" : "Black") + "'s turn");

        // Update match in database
        try {
            updateMatchInDatabase(matchId, moveRequest, gameStatus);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to update database: " + e.getMessage());
        }

        String moveNotation = createMoveNotation(moveRequest);
        System.out.println("📝 Move notation: " + moveNotation);

        MoveDTO moveDTO = new MoveDTO();
        moveDTO.setFromRow(moveRequest.getFromRow());
        moveDTO.setFromCol(moveRequest.getFromCol());
        moveDTO.setToRow(moveRequest.getToRow());
        moveDTO.setToCol(moveRequest.getToCol());
        moveDTO.setPiece(moveRequest.getPiece());
        moveDTO.setPromotedTo(moveRequest.getPromotedTo());
        moveDTO.setCapturedPiece(moveRequest.getCapturedPiece());
        moveDTO.setCastled(moveRequest.getCastled() != null ? moveRequest.getCastled() : false);
        moveDTO.setIsEnPassant(moveRequest.getIsEnPassant() != null ? moveRequest.getIsEnPassant() : false);
        moveDTO.setIsPromotion(moveRequest.getIsPromotion() != null ? moveRequest.getIsPromotion() : false);
        moveDTO.setFenBefore(moveRequest.getFenBefore());
        moveDTO.setFenAfter(moveRequest.getFenAfter());
        moveDTO.setBoard(newBoard);
        moveDTO.setIsWhiteTurn(!isWhiteTurn);
        moveDTO.setPlayerColor(playerColor);
        moveDTO.setMatchId(matchId);
        moveDTO.setTimestamp(LocalDateTime.now());
        moveDTO.setMoveNotation(moveNotation);
        moveDTO.setPlayerUsername(username);
//        moveDTO.set(gameStatus);

        System.out.println("📤 Prepared MoveDTO for broadcasting");
        System.out.println("🎯 Broadcasting to: /topic/moves/" + matchId);

        return moveDTO;
    }

    private String checkGameStatus(String[][] board, boolean isWhiteTurn) {
        // Simplified game status check
        // In a real implementation, you would implement proper chess logic here
        // For now, just return IN_PROGRESS
        return "IN_PROGRESS";
    }

    private String createMoveNotation(MoveRequestDTO move) {
        int fromRow = move.getFromRow();
        int fromCol = move.getFromCol();
        int toRow = move.getToRow();
        int toCol = move.getToCol();
        String piece = move.getPiece();

        String fromSquare = colToFile(fromCol) + (8 - fromRow);
        String toSquare = colToFile(toCol) + (8 - toRow);

        if (move.getCastled() != null && move.getCastled()) {
            return toCol == 6 ? "O-O" : "O-O-O";
        }

        String pieceSymbol = piece.toUpperCase();
        if ("p".equalsIgnoreCase(piece)) {
            pieceSymbol = "";
        }

        String capture = move.getCapturedPiece() != null && !move.getCapturedPiece().isEmpty() ? "x" : "";

        return pieceSymbol + capture + toSquare;
    }

    private String colToFile(int col) {
        return String.valueOf((char) ('a' + col));
    }

    private void updateMatchInDatabase(Long matchId, MoveRequestDTO moveRequest, String gameStatus) {
        try {
            Optional<Match> matchOpt = matchRepo.findById(matchId);
            if (matchOpt.isPresent()) {
                Match match = matchOpt.get();

                if (moveRequest.getFenAfter() != null) {
                    match.setFenCurrent(moveRequest.getFenAfter());
                    System.out.println("📝 Updated FEN to: " + moveRequest.getFenAfter());
                }

                String uci = createUCI(moveRequest);
                if (!uci.isEmpty()) {
                    match.setLastMoveUci(uci);
                    System.out.println("📝 Updated last move UCI: " + uci);
                }

                Integer currentPly = match.getCurrentPly();
                if (currentPly == null) {
                    currentPly = 0;
                }
                match.setCurrentPly(currentPly + 1);
                System.out.println("📝 Updated ply to: " + (currentPly + 1));

                // Update match status based on game status
                if ("CHECKMATE".equals(gameStatus)) {
                    boolean whiteMated = moveRequest.getPlayerColor().equals("white");
                    match.setStatus(whiteMated ? MatchStatus.PLAYER2_WON : MatchStatus.PLAYER1_WON);
                } else if ("STALEMATE".equals(gameStatus) || "DRAW".equals(gameStatus)) {
                    match.setStatus(MatchStatus.DRAW);
                    match.setDrawOfferedBy(null); // Clear any pending draw offers
                }

                matchRepo.save(match);
                System.out.println("💾 Database updated for match: " + matchId);
            } else {
                System.out.println("⚠️ Match not found in database: " + matchId);
            }
        } catch (Exception e) {
            System.err.println("❌ Error updating match in database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String createUCI(MoveRequestDTO move) {
        if (move.getFromCol() == null || move.getFromRow() == null ||
                move.getToCol() == null || move.getToRow() == null) {
            return "";
        }

        try {
            String fromFile = Character.toString((char) ('a' + move.getFromCol()));
            int fromRank = 8 - move.getFromRow();
            String toFile = Character.toString((char) ('a' + move.getToCol()));
            int toRank = 8 - move.getToRow();

            String uci = fromFile + fromRank + toFile + toRank;

            if (Boolean.TRUE.equals(move.getIsPromotion()) && move.getPromotedTo() != null) {
                String promotedPiece = move.getPromotedTo().toLowerCase();
                if (promotedPiece.equals("q")) uci += "q";
                else if (promotedPiece.equals("r")) uci += "r";
                else if (promotedPiece.equals("b")) uci += "b";
                else if (promotedPiece.equals("n")) uci += "n";
            }

            return uci;
        } catch (Exception e) {
            System.err.println("Error creating UCI notation: " + e.getMessage());
            return "";
        }
    }

    public GameStatusDTO handlePlayerJoin(Long matchId, JoinRequest joinRequest, Principal principal) {
        String username = principal.getName();

        GameState gameState = activeGames.get(matchId);
        if (gameState == null) {
            Optional<Match> matchOpt = matchRepo.findById(matchId);
            if (matchOpt.isPresent()) {
                gameState = initializeGameState(matchOpt.get());
                activeGames.put(matchId, gameState);
            } else {
                throw new RuntimeException("Game not found");
            }
        }

        GameStatusDTO statusDTO = new GameStatusDTO();
        statusDTO.setMatchId(matchId);
        statusDTO.setStatus(gameState.getStatus());
        statusDTO.setPlayerColor(joinRequest.getPlayerColor());
        statusDTO.setMyTurn(determineMyTurn(matchId, username));
        statusDTO.setBoard(gameState.getBoard());
        statusDTO.setFen(convertBoardToFEN(gameState.getBoard(), gameState.isWhiteTurn()));

        return statusDTO;
    }

    private boolean determineMyTurn(Long matchId, String username) {
        GameState gameState = activeGames.get(matchId);
        if (gameState == null) return false;

        boolean isWhiteTurn = gameState.isWhiteTurn();
        if (isWhiteTurn) {
            return gameState.getPlayer1Username().equals(username);
        } else {
            return gameState.getPlayer2Username().equals(username);
        }
    }

    private String convertBoardToFEN(String[][] board, boolean isWhiteTurn) {
        StringBuilder fen = new StringBuilder();

        for (int row = 0; row < 8; row++) {
            int emptyCount = 0;
            for (int col = 0; col < 8; col++) {
                String piece = board[row][col];
                if (piece == null || piece.isEmpty()) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(piece);
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (row < 7) {
                fen.append("/");
            }
        }

        fen.append(" ").append(isWhiteTurn ? "w" : "b");
        fen.append(" ").append("KQkq");
        fen.append(" ").append("-");
        fen.append(" ").append("0 1");

        return fen.toString();
    }

    public void handleResignation(Long matchId, String username) {
        GameState gameState = activeGames.get(matchId);
        if (gameState != null) {
            gameState.setStatus("RESIGNED");
            activeGames.put(matchId, gameState);

            GameStatusDTO statusDTO = new GameStatusDTO();
            statusDTO.setMatchId(matchId);
            statusDTO.setStatus("RESIGNED");
            statusDTO.setPlayerColor(getPlayerColor(matchId, username));

            messagingTemplate.convertAndSend("/topic/game-state/" + matchId, statusDTO);
        }
    }

    public void handleDrawOffer(Long matchId, String username) {
        GameState gameState = activeGames.get(matchId);
        if (gameState != null) {
            String opponent = getOpponentUsername(matchId, username);

            Map<String, Object> drawOffer = new HashMap<>();
            drawOffer.put("type", "DRAW_OFFER");
            drawOffer.put("from", username);
            drawOffer.put("matchId", matchId);
            drawOffer.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSendToUser(opponent, "/queue/draw-offers", drawOffer);
        }
    }

    private String getPlayerColor(Long matchId, String username) {
        List<String> players = gamePlayers.get(matchId);
        if (players != null && players.size() >= 2) {
            if (players.get(0).equals(username)) {
                return "white";
            } else if (players.get(1).equals(username)) {
                return "black";
            }
        }
        return null;
    }

    private String getOpponentUsername(Long matchId, String username) {
        List<String> players = gamePlayers.get(matchId);
        if (players != null && players.size() >= 2) {
            if (players.get(0).equals(username)) {
                return players.get(1);
            } else if (players.get(1).equals(username)) {
                return players.get(0);
            }
        }
        return null;
    }

    private String getUsernameFromRequest(HttpServletRequest request) {
        // Extract the username directly from the header
        String username = request.getHeader("x-header-username");

        // Check if the username is present
        if (username != null && !username.isEmpty()) {
            return username;
        }

        // If username is not found, throw an exception or handle accordingly
        throw new RuntimeException("Username not found in request header");
    }



    public void cleanupInactiveGames() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        activeGames.entrySet().removeIf(entry ->
                entry.getValue().getLastMoveTime().isBefore(cutoff)
        );
    }
}
