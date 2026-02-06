package com.example.Docker.match_service.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
@Data
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private Long player1Id;


    private Long player2Id;

    @Enumerated(EnumType.STRING)
    private MatchStatus status; // PLAYER1_WON, DRAW, PLAYER2_WON, IN_PROGRESS

    private Integer currentPly; // helps with sync & validation

    @Column(name = "fen_current", length = 200)
    private String fenCurrent;

    @Column(name = "last_move_uci", length = 10)
    private String lastMoveUci;

    @OneToMany(
            mappedBy = "match",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("ply ASC") // VERY IMPORTANT
    private List<Move> moves = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private GameType gameType;

    // Time control fields
    private Integer timeControlMinutes;
    private Integer timeIncrementSeconds;

    // Individual player times (in seconds)
    private Integer whiteTimeRemaining;
    private Integer blackTimeRemaining;

    // Track draw offers
    // null = no draw offer, 1 = offered by player1, 2 = offered by player2
    private Integer drawOfferedBy;

    // Track who resigned
    // null = no resignation, 1 = resigned by player1, 2 = resigned by player2
    private Integer resignedBy;

    // Single timestamp for both creation and start
    private LocalDateTime createdAt;

    // Single timestamp for both last update and last move
    private LocalDateTime updatedAt;

    // Constructor
    public Match(Long player1Id, Long player2Id, MatchStatus matchStatus, int i) {
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.status = matchStatus;
        this.currentPly = i;
        this.createdAt = LocalDateTime.now();
        this.fenCurrent = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        this.drawOfferedBy = null;
        this.resignedBy = null;

        // Initialize time based on game type (to be set later)
        this.whiteTimeRemaining = 0;
        this.blackTimeRemaining = 0;
    }

    public Match(){}

    // Draw management helper methods
    public boolean isDrawOfferPending() {
        return drawOfferedBy != null;
    }

    public boolean isDrawOfferFromPlayer1() {
        return drawOfferedBy != null && drawOfferedBy == 1;
    }

    public boolean isDrawOfferFromPlayer2() {
        return drawOfferedBy != null && drawOfferedBy == 2;
    }

    public void offerDrawByPlayer1() {
        this.drawOfferedBy = 1;
    }

    public void offerDrawByPlayer2() {
        this.drawOfferedBy = 2;
    }

    public void clearDrawOffer() {
        this.drawOfferedBy = null;
    }

    // Resignation helper methods
    public boolean isResigned() {
        return resignedBy != null;
    }

    public boolean resignedByPlayer1() {
        return resignedBy != null && resignedBy == 1;
    }

    public boolean resignedByPlayer2() {
        return resignedBy != null && resignedBy == 2;
    }

    public void resignByPlayer1() {
        this.resignedBy = 1;
        this.status = MatchStatus.PLAYER2_WON;
        this.clearDrawOffer();
    }

    public void resignByPlayer2() {
        this.resignedBy = 2;
        this.status = MatchStatus.PLAYER1_WON;
        this.clearDrawOffer();
    }

    // Time management methods
    public void setTimeForGameType(String gameType) {
        if ("rapid".equalsIgnoreCase(gameType)) {
            this.timeControlMinutes = 10;
            this.timeIncrementSeconds = 0;
            this.whiteTimeRemaining = 600; // 10 minutes in seconds
            this.blackTimeRemaining = 600;
        } else if ("classical".equalsIgnoreCase(gameType)) {
            this.timeControlMinutes = 0;
            this.timeIncrementSeconds = 0;
            this.whiteTimeRemaining = 0; // 0 means unlimited
            this.blackTimeRemaining = 0;
        } else if ("blitz".equalsIgnoreCase(gameType)) {
            this.timeControlMinutes = 5;
            this.timeIncrementSeconds = 0;
            this.whiteTimeRemaining = 300; // 5 minutes in seconds
            this.blackTimeRemaining = 300;
        } else if ("bullet".equalsIgnoreCase(gameType)) {
            this.timeControlMinutes = 1;
            this.timeIncrementSeconds = 0;
            this.whiteTimeRemaining = 60; // 1 minute in seconds
            this.blackTimeRemaining = 60;
        }
    }

    public void decrementTimeForPlayer(boolean isWhite, int seconds) {
        if (isWhite) {
            if (this.whiteTimeRemaining > 0) {
                this.whiteTimeRemaining = Math.max(0, this.whiteTimeRemaining - seconds);
            }
        } else {
            if (this.blackTimeRemaining > 0) {
                this.blackTimeRemaining = Math.max(0, this.blackTimeRemaining - seconds);
            }
        }
    }

    public boolean isTimeUpForPlayer(boolean isWhite) {
        if (isWhite) {
            return this.whiteTimeRemaining != null && this.whiteTimeRemaining <= 0;
        } else {
            return this.blackTimeRemaining != null && this.blackTimeRemaining <= 0;
        }
    }

    public boolean isTimeControlGame() {
        return this.timeControlMinutes != null && this.timeControlMinutes > 0;
    }

    // Method to get the player who won (if any)
    public Long getWinner() {
        if (status == MatchStatus.PLAYER1_WON) {
            return player1Id;
        } else if (status == MatchStatus.PLAYER2_WON) {
            return player2Id;
        }
        return null;
    }

    // Method to get the player who lost (if any)
    public Long getLoser() {
        if (status == MatchStatus.PLAYER1_WON) {
            return player2Id;
        } else if (status == MatchStatus.PLAYER2_WON) {
            return player1Id;
        }
        return null;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.fenCurrent == null) {
            this.fenCurrent = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        }
        if (this.currentPly == null) {
            this.currentPly = 0;
        }
        if (this.drawOfferedBy == null) {
            this.drawOfferedBy = null;
        }
        if (this.resignedBy == null) {
            this.resignedBy = null;
        }
        // Initialize times if not set
        if (this.whiteTimeRemaining == null) {
            this.whiteTimeRemaining = 0;
        }
        if (this.blackTimeRemaining == null) {
            this.blackTimeRemaining = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();

        // Clear draw offer if game ends
        if (this.status != null &&
                this.status != MatchStatus.IN_PROGRESS) {
            this.clearDrawOffer();
        }
    }
}