

import java.util.List;

public class EventSchedulerTests {
    public static void main(String[] args) {
        EventScheduler scheduler = new EventScheduler();

        int[][] events1 = { {9, 10}, {10, 11}, {11, 12} };
        System.out.println(scheduler.canAttendAll(events1));     // true
        System.out.println(scheduler.minRoomsRequired(events1)); // 1

        int[][] events2 = { {9, 10}, {9, 11}, {10, 12} };
        System.out.println(scheduler.canAttendAll(events2));     // false
        System.out.println(scheduler.minRoomsRequired(events2)); // 2

        List<EventScheduler.ScheduledEvent> assigned = scheduler.assignRooms(events2);
        System.out.println(assigned);
    }
}
