package com.example.Docker.match_service.repo;

import com.example.Docker.match_service.model.Move;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoveRepo extends JpaRepository<Move, Long> {
}
