# Employee Shift Scheduler: Python and Java

MSCS-632-M30 Advanced Programming Languages, Assignment 4: Implementing Control Structures.
University of the Cumberlands, Fall 2026. Author: Anulekha Kavutarapu.

The same shift scheduling application is written in two languages with different designs: **Python** (dynamically typed, interpreted) and **Java** (statically typed, compiled). Both use the same algorithm so the control structures can be compared side by side.

## What the program does

The company is open 7 days a week with three shifts per day (Morning, Afternoon, Evening).

1. **Input and storage:** collect employee names and a ranked shift preference for every day (for example `M A E` means Morning first, then Afternoon, then Evening, or `OFF` if unavailable). Stored as a dictionary (Python) or `LinkedHashMap` (Java) of name to a list of 7 daily rankings.
2. **Scheduling rules:**
   * No employee works more than one shift per day.
   * No employee works more than 5 days per week.
   * Every shift has at least 2 employees. If a shift is short, employees who have not reached 5 days are picked at random to fill it.
3. **Conflict handling:** if an employee's preferred shift is full, they get their next ranked choice, then any other open shift on the same day, then an open shift on the next day.
4. **Output:** a weekly schedule table, a per employee summary, a log of every conflict and random assignment, and a final rule check that re-verifies all constraints.
5. **Bonus:** ranked shift preferences are supported in both versions.

## Project structure

```
python/scheduler.py    Python version
java/Main.java         Java version
README.md              this file
```

## How to run

### Python (3.8 or newer)

```
cd python
python scheduler.py
```

On some systems use `python3 scheduler.py` or `py scheduler.py`.

### Java (JDK 11 or newer)

```
cd java
javac Main.java
java Main
```

### Using the program

Both programs start with a menu:

* Enter `2` to load 12 sample employees (quickest way to see the schedule).
* Enter `1` to type your own employees. You need at least 9 employees, since 21 shifts with 2 people each is 42 assignments and each person can work at most 5 days. For each day, type a ranking such as `M A E`, `e m`, or `OFF`.

The schedule uses random choices for tie breaking and for filling short shifts, so the exact names can change between runs. The rules never change, and the RULE CHECK section at the bottom confirms them every time.

## Control structures demonstrated

| Structure | Python | Java |
|---|---|---|
| Conditionals | `if / elif / else` | `if / else if / else` |
| Multi way branch | `if / elif` chain in `shift_from_letter` | `switch` in `shiftFromLetter` |
| Loops | `for`, `while`, list comprehensions | `for`, enhanced `for`, `while` |
| Loop control | `continue`, `break` | `continue`, `break` |
| Input validation | `while` loop until valid | `while (true)` with `break` |

## Sample output

See the screenshots in the assignment report. Example of the final section:

```
RULE CHECK
  All rules satisfied: 1 shift per day, max 5 days, at least 2 per shift.
```
