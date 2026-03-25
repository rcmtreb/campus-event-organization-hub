package com.example.campus_event_org_hub.util;

/**
 * Implemented by fragments that can reload their displayed data on demand.
 * MainActivity calls {@link #refresh()} from the RealtimeSyncManager callback
 * so that newly-approved (or otherwise changed) events appear immediately
 * without the user having to swipe-to-refresh manually.
 */
public interface Refreshable {
    void refresh();
}
