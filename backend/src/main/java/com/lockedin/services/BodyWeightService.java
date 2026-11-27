package com.lockedin.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lockedin.api.dto.BodyWeightRequest;
import com.lockedin.api.dto.BodyWeightResponse;
import com.lockedin.models.BodyWeight;
import com.lockedin.persistence.repositories.BodyWeightRepository;
import com.lockedin.persistence.repositories.UserRepository;

@Service
@Transactional
public class BodyWeightService {

    private final BodyWeightRepository bodyWeights;
    private final UserRepository users;

    public BodyWeightService(BodyWeightRepository bodyWeights, UserRepository users) {
        this.bodyWeights = bodyWeights;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<BodyWeightResponse> list(UUID userId) {
        return bodyWeights.findByUserIdOrderByCreatedAtAsc(userId).stream().map(Responses::of).toList();
    }

    public BodyWeightResponse create(UUID userId, BodyWeightRequest request) {
        return Responses.of(bodyWeights.save(new BodyWeight(users.getReferenceById(userId), request.weight())));
    }

    public void delete(UUID userId, UUID id) {
        bodyWeights.delete(bodyWeights.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new NotFoundException("Body weight entry not found")));
    }
}
