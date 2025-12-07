package com.suvidha.backend.controller;

import com.suvidha.backend.model.UserVisit;
import com.suvidha.backend.repository.UserVisitRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TrackingController {

    @Autowired
    private UserVisitRepository repository;

    @PostMapping("/track")
    public UserVisit trackUser(@RequestParam String userIdentifier, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        
        Optional<UserVisit> existingVisit = repository.findByUserIdentifier(userIdentifier);
        
        UserVisit visit;
        if (existingVisit.isPresent()) {
            visit = existingVisit.get();
            visit.setLastSeen(LocalDateTime.now());
            visit.setVisitCount(visit.getVisitCount() + 1);
            // Optionally update IP if needed
            visit.setIpAddress(ipAddress);
        } else {
            visit = new UserVisit();
            visit.setUserIdentifier(userIdentifier);
            visit.setIpAddress(ipAddress);
            visit.setFirstSeen(LocalDateTime.now());
            visit.setLastSeen(LocalDateTime.now());
            visit.setVisitCount(1);
        }
        
        return repository.save(visit);
    }
}
