# Energy-per-token and battery-per-session protocol (issue A15)

Phone battery counters report whole percent, so we state the method and its uncertainty rather than claim precision.

## Conditions (write them in the result file)

- Same phone, same model file (SHA-256 noted), same build, same thread count.
- Start at a fixed charge (for example 90%), unplugged, airplane mode on, screen on at fixed brightness (for example 50%), all other apps closed, auto-brightness and adaptive battery off.
- Start from a cool phone (same room, 25 C if possible); wait 10 minutes between runs. Repeat each measurement at least 3 times and report all runs.
- Idle baseline: leave the phone in the same state with the app idle for 20 minutes; record the drain (percent per hour).

## Runs

1. Scripted 20-minute session: `--es mode sustained --ei minutes 20 --ez governor true` (the session length of PRD 11.1). Record battery at start and end and total tokens (the script prints it).
2. `python bench/energy_calc.py battery --capacity-mah <rated> --start <s> --end <e> --tokens <n> --idle-pct-per-hour <idle> --minutes 20`
3. The script subtracts idle drain, converts using the rated capacity at 3.85 V nominal, and prints a range from the 1-percent counter step. If the drop is under 5 points it warns that the result is dominated by resolution.

## Better: USB power meter

If any team member has an inline USB power meter that logs, charge the phone to 100%, run it from a power bank through the meter with the screen state fixed, export `t_s,watts`, and use `python bench/energy_calc.py meter meter.csv --tokens <n>`. Subtract a screen-on idle run of the same length. Report both methods if both exist.

## Report wording

State: method, starting charge, brightness, number of repeats, idle correction, capacity assumption, and the range. Do not quote a single mJ/token figure without its range.

## Results (1B INT4)

FILL IN after the device run: `bench/results/energy.md`.
