package com.example.IndiChessBackend.model;

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

    @ManyToOne
    @JoinColumn(name = "player1_id", nullable = false)
    private User player1;

    @ManyToOne
    @JoinColumn(name = "player2_id", nullable = false)
    private User player2;

    @Enumerated(EnumType.STRING)
    private MatchStatus status; // PLAYER1_WON, DRAW, PLAYER2_WON

    private Integer currentPly; // helps with sync & validation

    @Column(name = "fen_current", nullable = false, length = 200)
    private String fenCurrent;

    @Column(name = "last_move_uci", length = 10)
    private String lastMoveUci;

//    @OneToMany(
//            mappedBy = "match",
//            cascade = CascadeType.ALL,
//            orphanRemoval = true,
//            fetch = FetchType.LAZY
//    )
//    @OrderBy("ply ASC") // VERY IMPORTANT
//    private List<Move> moves = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private GameType gameType;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

//    // helper method
//    public void addMove(Move move) {
//        moves.add(move);
//        move.setMatch(this);
//        this.currentPly = move.getPly();
//    }
}

