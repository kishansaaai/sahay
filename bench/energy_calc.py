#!/usr/bin/env python3
"""Energy per token and battery per session for issue A15 (PRD 11.1). Method in bench/ENERGY.md.

Battery-counter method (coarse, 1% steps):
    python bench/energy_calc.py battery --capacity-mah 5000 --start 90 --end 84 --tokens 52000 --idle-pct-per-hour 1.5 --minutes 20
Power-meter method (USB meter CSV with columns t_s,watts):
    python bench/energy_calc.py meter meter.csv --tokens 52000
"""
import argparse
import csv

NOMINAL_V = 3.85  # typical Li-ion nominal voltage; replace with the phone's value if known


def battery(a):
    drop = a.start - a.end
    idle = a.idle_pct_per_hour * a.minutes / 60.0
    net = max(0.0, drop - idle)
    wh = lambda pct: pct / 100.0 * a.capacity_mah / 1000.0 * NOMINAL_V
    j = lambda pct: wh(pct) * 3600.0
    lo, hi = max(0.0, net - 1.0), net + 1.0  # the counter moves in whole percent: +-1 point at each end is a pessimistic bound
    print("battery drop %d points, idle drain %.2f points, net %.2f points" % (drop, idle, net))
    print("energy %.0f J (range %.0f to %.0f J, uncertainty from 1%% counter steps)" % (j(net), j(lo), j(hi)))
    if a.tokens:
        print("energy per token %.1f mJ (range %.1f to %.1f mJ)" % (1000 * j(net) / a.tokens, 1000 * j(lo) / a.tokens, 1000 * j(hi) / a.tokens))
    print("battery per %d-minute session: %.1f%% (idle-corrected %.1f%%)" % (a.minutes, drop, net))
    if drop < 5:
        print("WARNING: a drop under 5 points is dominated by counter resolution; run longer or use a USB meter.")


def meter(a):
    rows = [(float(r["t_s"]), float(r["watts"])) for r in csv.DictReader(open(a.csv))]
    j = sum((t2 - t1) * (w1 + w2) / 2 for (t1, w1), (t2, w2) in zip(rows, rows[1:]))
    print("energy %.0f J over %.0f s (mean %.2f W)" % (j, rows[-1][0] - rows[0][0], j / (rows[-1][0] - rows[0][0])))
    if a.tokens:
        print("energy per token %.1f mJ" % (1000 * j / a.tokens))


def main():
    ap = argparse.ArgumentParser()
    s = ap.add_subparsers(dest="cmd", required=True)
    b = s.add_parser("battery")
    b.add_argument("--capacity-mah", type=float, required=True); b.add_argument("--start", type=int, required=True); b.add_argument("--end", type=int, required=True)
    b.add_argument("--tokens", type=int); b.add_argument("--idle-pct-per-hour", type=float, default=0.0); b.add_argument("--minutes", type=float, default=20)
    m = s.add_parser("meter"); m.add_argument("csv"); m.add_argument("--tokens", type=int)
    a = ap.parse_args()
    battery(a) if a.cmd == "battery" else meter(a)


if __name__ == "__main__":
    main()
