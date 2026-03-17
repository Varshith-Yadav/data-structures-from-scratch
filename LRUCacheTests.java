

public class LRUCacheTests {

    private static String printable(Object value) {
        return value == null ? "-1" : value.toString();
    }

    public static void main(String[] args) {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        cache.put(1, "A");
        cache.put(2, "B");

        System.out.println(printable(cache.get(1)));  // Output: "A"

        // test 2
        cache.put(3, "C");  // Evicts key 2
        System.out.println(printable(cache.get(2)));  // Output: -1 (not found)
        System.out.println(printable(cache.get(3)));  // Output: "C"

        // test 3
        cache.put(1, "A_update");
        System.out.println(printable(cache.get(1)));  // Output: "A_update"

        // test 4
        cache = new LRUCache<>(2);
        cache.put(1, "A");
        cache.put(2, "B");

        cache.get(1);  // Access key 1 to make it recently used
        cache.put(3, "C");  // Evicts key 2
        System.out.println(printable(cache.get(2)));  // Output: -1 (not found)
        System.out.println(printable(cache.get(1)));  // Output: "A"
        System.out.println(printable(cache.get(3)));  // Output: "C"
    }
}
