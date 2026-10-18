"""
Employee Shift Scheduler (Python version)
MSCS-632-M30 Assignment 4: Implementing Control Structures

Rules enforced:
  * An employee works at most one shift per day.
  * An employee works at most 5 days per week.
  * Every shift on every day has at least 2 employees.
  * If a preferred shift is full, the employee is moved to another shift
    on the same day, or to the next day.
  * Bonus: employees give a ranked list of shift preferences.
"""

import random

DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
SHIFTS = ["Morning", "Afternoon", "Evening"]

MIN_PER_SHIFT = 2      # company requirement
SHIFT_CAPACITY = 2     # first pass fills each shift up to this many people
MAX_DAYS = 5           # max days per employee per week

# Global state (kept simple on purpose so the control flow is easy to follow)
preferences = {}       # name -> list of 7 entries; each entry is a ranked list of shift indexes, or None for "off"
schedule = []          # schedule[day][shift] -> list of employee names
days_worked = {}       # name -> number of days assigned
worked_on = {}         # name -> set of day indexes already assigned
log = []               # messages about conflicts and random assignments


# ---------------------------------------------------------------------------
# Input and storage
# ---------------------------------------------------------------------------

def shift_from_letter(letter):
    """Convert M / A / E to a shift index. Returns -1 if the letter is invalid."""
    letter = letter.upper()
    if letter == "M":
        return 0
    elif letter == "A":
        return 1
    elif letter == "E":
        return 2
    else:
        return -1


def parse_ranking(text):
    """Turn input such as 'M A E' or 'e m' into a ranked list. 'OFF' means unavailable."""
    text = text.strip()
    if text.upper() == "OFF":
        return None
    ranking = []
    for token in text.replace(",", " ").split():
        idx = shift_from_letter(token[0])
        if idx == -1:
            return "invalid"
        if idx not in ranking:
            ranking.append(idx)
    if len(ranking) == 0:
        return "invalid"
    return ranking


def load_sample_data():
    """12 sample employees. Most want Morning, so conflicts will happen."""
    base = {
        "Alice":  [0, 1, 2], "Bob":    [0, 2, 1], "Carol": [1, 0, 2],
        "David":  [2, 1, 0], "Emma":   [0, 1, 2], "Frank": [1, 2, 0],
        "Grace":  [0, 2, 1], "Henry":  [2, 0, 1], "Isabel": [1, 0, 2],
        "Jack":   [0, 1, 2], "Karen":  [2, 1, 0], "Liam":  [1, 2, 0],
    }
    days_off = {
        "Alice": [6], "Bob": [5, 6], "Carol": [6], "David": [6],
        "Emma": [5], "Grace": [6], "Henry": [2], "Isabel": [6],
        "Karen": [5, 6], "Liam": [1],
    }
    for name, ranking in base.items():
        week = []
        for d in range(7):
            if d in days_off.get(name, []):
                week.append(None)
            else:
                week.append(list(ranking))
        preferences[name] = week


def collect_manual_input():
    """Ask the user for names and a ranked shift preference for each day."""
    count = 0
    while count < 9:
        raw = input("How many employees? (at least 9 are needed to cover every shift): ").strip()
        if raw.isdigit():
            count = int(raw)
        if count < 9:
            print("  Please enter a number that is 9 or more.")

    for i in range(count):
        name = ""
        while name == "" or name in preferences:
            name = input(f"Employee {i + 1} name: ").strip()
            if name in preferences:
                print("  That name is already used.")
        week = []
        for d in range(7):
            ranking = "invalid"
            while ranking == "invalid":
                text = input(f"  {name}, {DAYS[d]} preference (e.g. M A E, or OFF): ")
                ranking = parse_ranking(text)
                if ranking == "invalid":
                    print("  Use the letters M, A, E in order of preference, or OFF.")
            week.append(ranking)
        preferences[name] = week


# ---------------------------------------------------------------------------
# Scheduling logic
# ---------------------------------------------------------------------------

def assign(name, day, shift):
    schedule[day][shift].append(name)
    days_worked[name] += 1
    worked_on[name].add(day)


def can_work(name, day):
    return days_worked[name] < MAX_DAYS and day not in worked_on[name]


def try_place(name, day, order):
    """Place the employee in the first shift of 'order' that still has room."""
    for s in order:
        if len(schedule[day][s]) < SHIFT_CAPACITY:
            assign(name, day, s)
            return s
    return -1


def first_pass():
    """Go day by day. Give each available employee their highest ranked open shift."""
    for d in range(7):
        candidates = [n for n in preferences if preferences[n][d] is not None and can_work(n, d)]
        random.shuffle(candidates)
        candidates.sort(key=lambda n: days_worked[n])   # fewest days worked first (stable sort)

        # Only schedule about as many people as there are open slots (plus one spare).
        # This spreads the week's work out instead of using everyone on day one.
        open_slots = 3 * SHIFT_CAPACITY - sum(len(schedule[d][s]) for s in range(3))
        candidates = candidates[:open_slots + 1]

        for name in candidates:
            if not can_work(name, d):
                continue                                # already placed through an earlier conflict move

            ranked = preferences[name][d]
            placed = try_place(name, d, ranked)
            if placed != -1:
                if placed != ranked[0]:
                    log.append(f"CONFLICT: {name}'s first choice ({SHIFTS[ranked[0]]}) was full on {DAYS[d]}. "
                               f"Moved to {SHIFTS[placed]} the same day.")
                continue

            # Conflict: every preferred shift is full
            wanted = ", ".join(SHIFTS[s] for s in ranked)
            others = [s for s in range(3) if s not in ranked]
            placed = try_place(name, d, others)
            if placed != -1:
                log.append(f"CONFLICT: {name} wanted {wanted} on {DAYS[d]} (full). "
                           f"Moved to {SHIFTS[placed]} the same day.")
            elif d + 1 < 7 and preferences[name][d + 1] is not None and can_work(name, d + 1):
                order = list(preferences[name][d + 1]) + [s for s in range(3) if s not in preferences[name][d + 1]]
                placed = try_place(name, d + 1, order)
                if placed != -1:
                    log.append(f"CONFLICT: every shift on {DAYS[d]} was full for {name}. "
                               f"Moved to {SHIFTS[placed]} on {DAYS[d + 1]}.")
                else:
                    log.append(f"CONFLICT: no open shift for {name} on {DAYS[d]} or {DAYS[d + 1]}. "
                               f"Not scheduled on those days.")
            else:
                log.append(f"CONFLICT: no open shift for {name} on {DAYS[d]}. Not scheduled that day.")


def fill_minimums():
    """If a shift has fewer than 2 people, randomly add employees who are under 5 days."""
    for d in range(7):
        for s in range(3):
            while len(schedule[d][s]) < MIN_PER_SHIFT:
                eligible = [n for n in preferences if can_work(n, d)]
                if len(eligible) == 0:
                    log.append(f"WARNING: no one left to cover {SHIFTS[s]} on {DAYS[d]}.")
                    break
                pick = random.choice(eligible)
                note = ""
                if preferences[pick][d] is None:
                    note = " (had marked this day OFF)"
                assign(pick, d, s)
                log.append(f"RANDOM FILL: {pick} added to {SHIFTS[s]} on {DAYS[d]}{note} to reach {MIN_PER_SHIFT} people.")


def build_schedule():
    global schedule
    schedule = [[[] for _ in SHIFTS] for _ in DAYS]
    for name in preferences:
        days_worked[name] = 0
        worked_on[name] = set()
    first_pass()
    fill_minimums()


# ---------------------------------------------------------------------------
# Output
# ---------------------------------------------------------------------------

def print_schedule():
    width = 30
    line = "+" + "-" * 11 + ("+" + "-" * width) * 3 + "+"
    print()
    print("FINAL WEEKLY SCHEDULE")
    print(line)
    print("| " + "Day".ljust(10) + "".join("| " + s.ljust(width - 1) for s in SHIFTS) + "|")
    print(line)
    for d in range(7):
        row = "| " + DAYS[d].ljust(10)
        for s in range(3):
            row += "| " + ", ".join(schedule[d][s]).ljust(width - 1)
        print(row + "|")
        print(line)


def print_employee_summary():
    print()
    print("EMPLOYEE SUMMARY")
    print(f"{'Employee':<10} {'Days':<5} Assignments")
    for name in preferences:
        parts = []
        for d in range(7):
            for s in range(3):
                if name in schedule[d][s]:
                    parts.append(f"{DAYS[d][:3]}-{SHIFTS[s][0]}")
        print(f"{name:<10} {days_worked[name]:<5} {', '.join(parts)}")


def print_log():
    print()
    print("SCHEDULING NOTES (conflicts and random assignments)")
    if len(log) == 0:
        print("  None")
    for entry in log:
        print("  " + entry)


def validate():
    """Re-check every rule against the finished schedule."""
    problems = []
    for d in range(7):
        seen = []
        for s in range(3):
            if len(schedule[d][s]) < MIN_PER_SHIFT:
                problems.append(f"{DAYS[d]} {SHIFTS[s]} has fewer than {MIN_PER_SHIFT} employees")
            for n in schedule[d][s]:
                if n in seen:
                    problems.append(f"{n} works more than one shift on {DAYS[d]}")
                seen.append(n)
    for name in preferences:
        if days_worked[name] > MAX_DAYS:
            problems.append(f"{name} works more than {MAX_DAYS} days")
    print()
    print("RULE CHECK")
    if len(problems) == 0:
        print("  All rules satisfied: 1 shift per day, max 5 days, at least 2 per shift.")
    else:
        for p in problems:
            print("  PROBLEM: " + p)


def main():
    print("=== Employee Shift Scheduler (Python) ===")
    choice = ""
    while choice != "1" and choice != "2":
        choice = input("1) Enter employees manually\n2) Use sample data (12 employees)\nChoose 1 or 2: ").strip()
    if choice == "1":
        collect_manual_input()
    else:
        load_sample_data()
        print(f"Loaded {len(preferences)} sample employees.")

    build_schedule()
    print_schedule()
    print_employee_summary()
    print_log()
    validate()


if __name__ == "__main__":
    main()
