import java.util.*;

/**
 * Employee Shift Scheduler (Java version)
 * MSCS-632-M30 Assignment 4: Implementing Control Structures
 *
 * Rules enforced:
 *   - An employee works at most one shift per day.
 *   - An employee works at most 5 days per week.
 *   - Every shift on every day has at least 2 employees.
 *   - If a preferred shift is full, the employee is moved to another shift
 *     on the same day, or to the next day.
 *   - Bonus: employees give a ranked list of shift preferences.
 *
 * The file is named Main.java so it also runs on online compilers.
 */
public class Main {

    static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    static final String[] SHIFTS = {"Morning", "Afternoon", "Evening"};

    static final int MIN_PER_SHIFT = 2;   // company requirement
    static final int SHIFT_CAPACITY = 2;  // first pass fills each shift up to this many people
    static final int MAX_DAYS = 5;        // max days per employee per week

    static final Random random = new Random();
    static final Scanner scanner = new Scanner(System.in);

    // name -> 7 entries; each entry is a ranked list of shift indexes, or null for "off"
    static Map<String, List<List<Integer>>> preferences = new LinkedHashMap<>();
    // schedule[day][shift] -> employee names
    static List<List<List<String>>> schedule = new ArrayList<>();
    static Map<String, Integer> daysWorked = new HashMap<>();
    static Map<String, Set<Integer>> workedOn = new HashMap<>();
    static List<String> log = new ArrayList<>();

    // ---------------------------------------------------------------------
    // Input and storage
    // ---------------------------------------------------------------------

    /** Convert M / A / E to a shift index. Returns -1 for an invalid letter. */
    static int shiftFromLetter(char letter) {
        switch (Character.toUpperCase(letter)) {
            case 'M': return 0;
            case 'A': return 1;
            case 'E': return 2;
            default:  return -1;
        }
    }

    /** Turn "M A E" into a ranked list. Returns null for OFF, or an empty list if invalid. */
    static List<Integer> parseRanking(String text) {
        text = text.trim();
        if (text.equalsIgnoreCase("OFF")) {
            return null;
        }
        List<Integer> ranking = new ArrayList<>();
        for (String token : text.replace(",", " ").split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            int idx = shiftFromLetter(token.charAt(0));
            if (idx == -1) {
                return new ArrayList<>();       // invalid
            }
            if (!ranking.contains(idx)) {
                ranking.add(idx);
            }
        }
        return ranking;                          // empty list also means invalid
    }

    static void loadSampleData() {
        String[] names = {"Alice", "Bob", "Carol", "David", "Emma", "Frank",
                          "Grace", "Henry", "Isabel", "Jack", "Karen", "Liam"};
        int[][] base = {
            {0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {2, 1, 0}, {0, 1, 2}, {1, 2, 0},
            {0, 2, 1}, {2, 0, 1}, {1, 0, 2}, {0, 1, 2}, {2, 1, 0}, {1, 2, 0}
        };
        // days off, indexed the same as names (0 = Monday ... 6 = Sunday)
        int[][] off = {
            {6}, {5, 6}, {6}, {6}, {5}, {},
            {6}, {2}, {6}, {}, {5, 6}, {1}
        };
        for (int i = 0; i < names.length; i++) {
            List<List<Integer>> week = new ArrayList<>();
            for (int d = 0; d < 7; d++) {
                boolean isOff = false;
                for (int o : off[i]) {
                    if (o == d) {
                        isOff = true;
                    }
                }
                if (isOff) {
                    week.add(null);
                } else {
                    List<Integer> ranking = new ArrayList<>();
                    for (int s : base[i]) {
                        ranking.add(s);
                    }
                    week.add(ranking);
                }
            }
            preferences.put(names[i], week);
        }
    }

    static void collectManualInput() {
        int count = 0;
        while (count < 9) {
            System.out.print("How many employees? (at least 9 are needed to cover every shift): ");
            String raw = scanner.nextLine().trim();
            if (raw.matches("\\d+")) {
                count = Integer.parseInt(raw);
            }
            if (count < 9) {
                System.out.println("  Please enter a number that is 9 or more.");
            }
        }

        for (int i = 0; i < count; i++) {
            String name = "";
            while (name.isEmpty() || preferences.containsKey(name)) {
                System.out.print("Employee " + (i + 1) + " name: ");
                name = scanner.nextLine().trim();
                if (preferences.containsKey(name)) {
                    System.out.println("  That name is already used.");
                }
            }
            List<List<Integer>> week = new ArrayList<>();
            for (int d = 0; d < 7; d++) {
                while (true) {
                    System.out.print("  " + name + ", " + DAYS[d] + " preference (e.g. M A E, or OFF): ");
                    String text = scanner.nextLine();
                    List<Integer> ranking = parseRanking(text);
                    if (ranking == null || !ranking.isEmpty()) {
                        week.add(ranking);
                        break;
                    }
                    System.out.println("  Use the letters M, A, E in order of preference, or OFF.");
                }
            }
            preferences.put(name, week);
        }
    }

    // ---------------------------------------------------------------------
    // Scheduling logic
    // ---------------------------------------------------------------------

    static void assign(String name, int day, int shift) {
        schedule.get(day).get(shift).add(name);
        daysWorked.put(name, daysWorked.get(name) + 1);
        workedOn.get(name).add(day);
    }

    static boolean canWork(String name, int day) {
        return daysWorked.get(name) < MAX_DAYS && !workedOn.get(name).contains(day);
    }

    /** Place the employee in the first shift of 'order' that still has room. Returns -1 if none. */
    static int tryPlace(String name, int day, List<Integer> order) {
        for (int s : order) {
            if (schedule.get(day).get(s).size() < SHIFT_CAPACITY) {
                assign(name, day, s);
                return s;
            }
        }
        return -1;
    }

    static List<Integer> shiftsNotIn(List<Integer> ranked) {
        List<Integer> others = new ArrayList<>();
        for (int s = 0; s < 3; s++) {
            if (!ranked.contains(s)) {
                others.add(s);
            }
        }
        return others;
    }

    static String names(List<Integer> shiftIndexes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < shiftIndexes.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(SHIFTS[shiftIndexes.get(i)]);
        }
        return sb.toString();
    }

    static void firstPass() {
        for (int d = 0; d < 7; d++) {
            final int day = d;
            List<String> candidates = new ArrayList<>();
            for (String n : preferences.keySet()) {
                if (preferences.get(n).get(day) != null && canWork(n, day)) {
                    candidates.add(n);
                }
            }
            Collections.shuffle(candidates, random);
            candidates.sort(Comparator.comparingInt(n -> daysWorked.get(n)));   // fewest days first (stable sort)

            // Only schedule about as many people as there are open slots (plus one spare).
            int filled = 0;
            for (int s = 0; s < 3; s++) {
                filled += schedule.get(day).get(s).size();
            }
            int openSlots = 3 * SHIFT_CAPACITY - filled;
            if (candidates.size() > openSlots + 1) {
                candidates = new ArrayList<>(candidates.subList(0, openSlots + 1));
            }

            for (String name : candidates) {
                if (!canWork(name, day)) {
                    continue;   // already placed through an earlier conflict move
                }

                List<Integer> ranked = preferences.get(name).get(day);
                int placed = tryPlace(name, day, ranked);
                if (placed != -1) {
                    if (placed != ranked.get(0)) {
                        log.add("CONFLICT: " + name + "'s first choice (" + SHIFTS[ranked.get(0)] + ") was full on "
                                + DAYS[day] + ". Moved to " + SHIFTS[placed] + " the same day.");
                    }
                    continue;
                }

                // Conflict: every preferred shift is full
                placed = tryPlace(name, day, shiftsNotIn(ranked));
                if (placed != -1) {
                    log.add("CONFLICT: " + name + " wanted " + names(ranked) + " on " + DAYS[day]
                            + " (full). Moved to " + SHIFTS[placed] + " the same day.");
                } else if (day + 1 < 7 && preferences.get(name).get(day + 1) != null && canWork(name, day + 1)) {
                    List<Integer> nextRanked = preferences.get(name).get(day + 1);
                    List<Integer> order = new ArrayList<>(nextRanked);
                    order.addAll(shiftsNotIn(nextRanked));
                    placed = tryPlace(name, day + 1, order);
                    if (placed != -1) {
                        log.add("CONFLICT: every shift on " + DAYS[day] + " was full for " + name
                                + ". Moved to " + SHIFTS[placed] + " on " + DAYS[day + 1] + ".");
                    } else {
                        log.add("CONFLICT: no open shift for " + name + " on " + DAYS[day] + " or "
                                + DAYS[day + 1] + ". Not scheduled on those days.");
                    }
                } else {
                    log.add("CONFLICT: no open shift for " + name + " on " + DAYS[day] + ". Not scheduled that day.");
                }
            }
        }
    }

    static void fillMinimums() {
        for (int d = 0; d < 7; d++) {
            for (int s = 0; s < 3; s++) {
                while (schedule.get(d).get(s).size() < MIN_PER_SHIFT) {
                    List<String> eligible = new ArrayList<>();
                    for (String n : preferences.keySet()) {
                        if (canWork(n, d)) {
                            eligible.add(n);
                        }
                    }
                    if (eligible.isEmpty()) {
                        log.add("WARNING: no one left to cover " + SHIFTS[s] + " on " + DAYS[d] + ".");
                        break;
                    }
                    String pick = eligible.get(random.nextInt(eligible.size()));
                    String note = preferences.get(pick).get(d) == null ? " (had marked this day OFF)" : "";
                    assign(pick, d, s);
                    log.add("RANDOM FILL: " + pick + " added to " + SHIFTS[s] + " on " + DAYS[d] + note
                            + " to reach " + MIN_PER_SHIFT + " people.");
                }
            }
        }
    }

    static void buildSchedule() {
        schedule = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            List<List<String>> day = new ArrayList<>();
            for (int s = 0; s < 3; s++) {
                day.add(new ArrayList<>());
            }
            schedule.add(day);
        }
        for (String name : preferences.keySet()) {
            daysWorked.put(name, 0);
            workedOn.put(name, new HashSet<>());
        }
        firstPass();
        fillMinimums();
    }

    // ---------------------------------------------------------------------
    // Output
    // ---------------------------------------------------------------------

    static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    static void printSchedule() {
        int width = 30;
        String line = "+" + repeat('-', 11) + ("+" + repeat('-', width)).repeat(3) + "+";
        System.out.println();
        System.out.println("FINAL WEEKLY SCHEDULE");
        System.out.println(line);
        StringBuilder header = new StringBuilder("| " + String.format("%-10s", "Day"));
        for (String s : SHIFTS) {
            header.append("| ").append(String.format("%-" + (width - 1) + "s", s));
        }
        System.out.println(header + "|");
        System.out.println(line);
        for (int d = 0; d < 7; d++) {
            StringBuilder row = new StringBuilder("| " + String.format("%-10s", DAYS[d]));
            for (int s = 0; s < 3; s++) {
                row.append("| ").append(String.format("%-" + (width - 1) + "s", String.join(", ", schedule.get(d).get(s))));
            }
            System.out.println(row + "|");
            System.out.println(line);
        }
    }

    static void printEmployeeSummary() {
        System.out.println();
        System.out.println("EMPLOYEE SUMMARY");
        System.out.println(String.format("%-10s %-5s %s", "Employee", "Days", "Assignments"));
        for (String name : preferences.keySet()) {
            List<String> parts = new ArrayList<>();
            for (int d = 0; d < 7; d++) {
                for (int s = 0; s < 3; s++) {
                    if (schedule.get(d).get(s).contains(name)) {
                        parts.add(DAYS[d].substring(0, 3) + "-" + SHIFTS[s].charAt(0));
                    }
                }
            }
            System.out.println(String.format("%-10s %-5d %s", name, daysWorked.get(name), String.join(", ", parts)));
        }
    }

    static void printLog() {
        System.out.println();
        System.out.println("SCHEDULING NOTES (conflicts and random assignments)");
        if (log.isEmpty()) {
            System.out.println("  None");
        }
        for (String entry : log) {
            System.out.println("  " + entry);
        }
    }

    /** Re-check every rule against the finished schedule. */
    static void validate() {
        List<String> problems = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            List<String> seen = new ArrayList<>();
            for (int s = 0; s < 3; s++) {
                if (schedule.get(d).get(s).size() < MIN_PER_SHIFT) {
                    problems.add(DAYS[d] + " " + SHIFTS[s] + " has fewer than " + MIN_PER_SHIFT + " employees");
                }
                for (String n : schedule.get(d).get(s)) {
                    if (seen.contains(n)) {
                        problems.add(n + " works more than one shift on " + DAYS[d]);
                    }
                    seen.add(n);
                }
            }
        }
        for (String name : preferences.keySet()) {
            if (daysWorked.get(name) > MAX_DAYS) {
                problems.add(name + " works more than " + MAX_DAYS + " days");
            }
        }
        System.out.println();
        System.out.println("RULE CHECK");
        if (problems.isEmpty()) {
            System.out.println("  All rules satisfied: 1 shift per day, max 5 days, at least 2 per shift.");
        } else {
            for (String p : problems) {
                System.out.println("  PROBLEM: " + p);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Employee Shift Scheduler (Java) ===");
        String choice = "";
        while (!choice.equals("1") && !choice.equals("2")) {
            System.out.print("1) Enter employees manually\n2) Use sample data (12 employees)\nChoose 1 or 2: ");
            choice = scanner.nextLine().trim();
        }
        if (choice.equals("1")) {
            collectManualInput();
        } else {
            loadSampleData();
            System.out.println("Loaded " + preferences.size() + " sample employees.");
        }

        buildSchedule();
        printSchedule();
        printEmployeeSummary();
        printLog();
        validate();
    }
}
