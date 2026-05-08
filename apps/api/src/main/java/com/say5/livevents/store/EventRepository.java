package com.say5.livevents.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface EventRepository extends JpaRepository<EventEntity, UUID>,
        JpaSpecificationExecutor<EventEntity> {
}
