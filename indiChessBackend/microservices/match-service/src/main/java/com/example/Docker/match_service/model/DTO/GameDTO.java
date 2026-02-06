package com.example.Docker.match_service.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameDTO {
    private Long id;
    private Long player1Id;
    private Long player2Id;
    private String status;
    private String playerColor; // For the requesting player: "white" or "black"
    private boolean isMyTurn;
    private String[][] board;
    private String fen;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}