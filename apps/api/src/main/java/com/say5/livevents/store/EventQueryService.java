package com.say5.livevents.store;

import com.say5.livevents.domain.EventFilter;
import com.say5.livevents.domain.LiveEvent;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Service
public class EventQueryService {

    private final EventRepository repository;

    public EventQueryService(EventRepository repository) {
        this.repository = repository;
    }

    /**
     * Cursor-paginated history. The cursor is the timestamp of the last seen
     * event (exclusive). Results are returned newest-first.
     */
    @Transactional(readOnly = true)
    public List<LiveEvent> history(EventFilter filter, Instant cursor, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        Specification<EventEntity> spec = baseSpec(filter, cursor);
        return repository.findAll(spec, PageRequest.of(0, safeLimit,
                        Sort.by(Sort.Direction.DESC, "timestamp")))
                .map(EventEntity::toDomain)
                .getContent();
    }

    /**
     * Streaming export. Uses {@link #history(EventFilter, Instant, int)} in
     * pages so we never materialise the entire result set in memory. The
     * caller is responsible for closing the consumer.
     */
    @Transactional(readOnly = true)
    public void exportStream(EventFilter filter, Consumer<LiveEvent> sink) {
        final int pageSize = 500;
        Instant cursor = null;
        while (true) {
            List<LiveEvent> page = history(filter, cursor, pageSize);
            if (page.isEmpty()) {
                return;
            }
            for (LiveEvent ev : page) {
                sink.accept(ev);
            }
            if (page.size() < pageSize) {
                return;
            }
            cursor = page.get(page.size() - 1).timestamp();
        }
    }

    public java.util.Optional<LiveEvent> findById(UUID id) {
        return repository.findById(id).map(EventEntity::toDomain);
    }

    private Specification<EventEntity> baseSpec(EventFilter filter, Instant cursor) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            filter.type().ifPresent(t -> predicates.add(cb.equal(root.get("type"), t)));
            filter.severity().ifPresent(s -> predicates.add(cb.equal(root.get("severity"), s)));
            filter.from().ifPresent(f -> predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), f)));
            filter.to().ifPresent(t -> predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), t)));
            if (cursor != null) {
                predicates.add(cb.lessThan(root.get("timestamp"), cursor));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Used only by tests / utility callers.
    Stream<EventEntity> rawAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "timestamp")).stream();
    }
}
