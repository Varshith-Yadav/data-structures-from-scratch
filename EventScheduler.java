

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Event Scheduler
 *
 * Scheduling system for a meeting platform.
 *
 * 
 *   canAttendAll       — O(n log n): can one person attend every event?</li>
 *   minRoomsRequired   — O(n log n): minimum rooms needed for all events.</li>
 *   assignRooms      — O(n log n): assign a named room to every event.</li>
 * 
 *
 * Adjacent events (where one ends exactly when the next begins) are
 * NOT considered overlapping — strict inequality check:  prevEnd > currStart.
 *
 * 
 * Example:
 *   EventScheduler s = new EventScheduler();
 *   int[][] events = {{9,10},{10,11},{11,12}};
 *   s.canAttendAll(events);       // true
 *   s.minRoomsRequired(events);   // 1
 * 
 */
public class EventScheduler {

    
    //   An enriched event record returned by assignRooms.
    public static class ScheduledEvent {
        public final int    start;
        public final int    end;
        public final String room;

        public ScheduledEvent(int start, int end, String room) {
            this.start = start;
            this.end   = end;
            this.room  = room;
        }

        @Override
        public String toString() {
            return "ScheduledEvent{start=" + start +
                   ", end=" + end +
                   ", room='" + room + "'}";
        }
    }

    
    /**
     * Returns true if a single person can attend every event
     * without any time conflicts.
     *
     * Logic: Sort events by start time. Walk consecutive pairs:
     * if the previous event's end time is strictly greater than the next
     * event's start time, they overlap → return false.
     * Adjacent events (prevEnd == currStart) are allowed.
     *
     * Time : O(n log n) — dominated by the sort; the scan is O(n).
     * Space: O(n) — sorted copy of the input array.
     *
     * @param events array of {start, end} pairs
     * @return true if all events are attendable without overlap
     */
    public boolean canAttendAll(int[][] events) {
        if (events == null || events.length == 0) return true;

        // Sort a copy — never mutate the caller's array.
        int[][] sorted = events.clone();
        Arrays.sort(sorted, Comparator.comparingInt(e -> e[0]));

        for (int i = 1; i < sorted.length; i++) {
            int prevEnd   = sorted[i - 1][1];
            int currStart = sorted[i][0];
            if (prevEnd > currStart) {   // strict: adjacent is fine
                return false;
            }
        }
        return true;
    }

    

    /**
     * Returns the minimum number of rooms needed to host all events
     * simultaneously, with no two overlapping events sharing a room.
     *
     * Logic: Sort events by start time. Maintain a min-heap of
     * end times of currently-running events (one slot per room in use).
     *
     * For each new event:
     *   If the earliest-ending room finishes at or before this event
     *       starts (heap.peek() <= currStart), that room is free —
     *       pop it and reuse it for the new event.
     *       Otherwise every room is still occupied — allocate a new one.
     *
     * Push this event's end time onto the heap. The heap size at the end
     * equals the number of rooms needed.
     *
     * Why a min-heap? We only care whether the soonest-ending room
     * is free. A min-heap gives us that in O(1) peek and O(log n) push/poll,
     * which is cheaper than re-scanning a plain list each time.
     *
     * Time : O(n log n) — sort + n heap operations each costing O(log n).
     * Space: O(n) — heap holds at most n end times.
     *
     * @param events array of {start, end} pairs
     * @return minimum number of rooms required
     */
    public int minRoomsRequired(int[][] events) {
        if (events == null || events.length == 0) return 0;

        int[][] sorted = events.clone();
        Arrays.sort(sorted, Comparator.comparingInt(e -> e[0]));

        // Min-heap of room end times.
        PriorityQueue<Integer> endTimes = new PriorityQueue<>();

        for (int[] event : sorted) {
            int start = event[0];
            int end   = event[1];

            if (!endTimes.isEmpty() && endTimes.peek() <= start) {
                // Earliest-ending room is free — reuse it.
                endTimes.poll();
            }
            // Either reusing a freed room or opening a new one.
            endTimes.offer(end);
        }

        return endTimes.size();
    }

    
    /**
     * Assigns a specific named room to each event and returns the schedule.
     *
     * Strategy: The heap now stores (endTime, roomLabel) pairs.
     * When a room frees up we pop it, grab its label, and reuse it for the
     * next event. New rooms are named sequentially: "Room A", "Room B", …
     * "Room Z", then "Room AA", "Room AB", … (spreadsheet-style), so the
     * scheduler never runs out of unique names.
     *
     * Time : O(n log n)
     * Space: O(n)
     *
     * @param events     array of {start, end} pairs
     * @param roomPrefix prefix string for room labels (e.g. "Room", "Conf")
     * @return list of  ScheduledEvent sorted by start time
     */
    public List<ScheduledEvent> assignRooms(int[][] events, String roomPrefix) {
        List<ScheduledEvent> result = new ArrayList<>();
        if (events == null || events.length == 0) return result;

        int[][] sorted = events.clone();
        Arrays.sort(sorted, Comparator.comparingInt(e -> e[0]));

        // Min-heap ordered by end time; ties broken by room label (consistent ordering).
        PriorityQueue<long[]> heap = new PriorityQueue<>(
            Comparator.comparingLong((long[] entry) -> entry[0])
        );
        // Store room labels separately — parallel list indexed by roomIndex.
        List<String> roomLabels = new ArrayList<>();
        int roomCounter = 0;

        for (int[] event : sorted) {
            int start = event[0];
            int end   = event[1];
            String roomLabel;

            if (!heap.isEmpty() && heap.peek()[0] <= start) {
                // Reuse the earliest-ending room.
                long[] freed = heap.poll();
                int    idx   = (int) freed[1];
                roomLabel    = roomLabels.get(idx);
            } else {
                // Allocate a brand-new room.
                roomLabel = roomLabel(roomCounter, roomPrefix);
                roomLabels.add(roomLabel);
                roomCounter++;
            }

            // Find the index of this label so we can reuse it later.
            int labelIndex = roomLabels.indexOf(roomLabel);
            heap.offer(new long[]{ end, labelIndex });
            result.add(new ScheduledEvent(start, end, roomLabel));
        }

        return result;
    }

    
    //   Convenience overload using "Room" as the default prefix.

    public List<ScheduledEvent> assignRooms(int[][] events) {
        return assignRooms(events, "Room");
    }

    
    /**
     * Converts a zero-based index to a spreadsheet-style column name.
     *
     * 0  → "A"
     * 25 → "Z"
     * 26 → "AA"
     * 51 → "AZ"
     * 
     *
     * This ensures the scheduler never runs out of unique room names
     * regardless of how many concurrent rooms are needed.
     */
    private String roomLabel(int index, String prefix) {
        StringBuilder label = new StringBuilder();
        int n = index;
        while (true) {
            label.insert(0, (char) ('A' + n % 26));
            n = n / 26 - 1;
            if (n < 0) break;
        }
        return prefix + " " + label;
    }
}
