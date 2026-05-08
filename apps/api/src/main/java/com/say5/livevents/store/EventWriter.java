package com.say5.livevents.store;

import com.say5.livevents.domain.LiveEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventWriter {

    private final EventRepository repository;

    public EventWriter(EventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void persist(LiveEvent event) {
        repository.save(EventEntity.from(event));
    }
}
