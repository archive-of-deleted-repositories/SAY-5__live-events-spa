package com.say5.livevents.api;

import java.util.List;

/**
 * Cursor-paginated history response. {@code nextCursor} is null when the
 * server has no more rows for this filter.
 */
public record HistoryResponse(List<EventDto> items, String nextCursor) {
}
