package com.example.Docker.match_service.repo;

import com.example.Docker.match_service.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepo extends JpaRepository<Match, Long> {
}
