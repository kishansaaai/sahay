#!/usr/bin/env python3
"""Thermal chart and verdict for issue A14 (PRD 11.1, 11.3). No third-party packages: writes an SVG.

    python bench/thermal_chart.py results/sustained_governor_off.csv results/sustained_governor_on.csv -o bench/results/thermal.svg

Input: CSVs written by BenchmarkActivity (elapsed_s,tok_per_s,thermal,battery_pct,charging,mode,tokens). Prints, for each run,
peak tokens/s (95th percentile of the first 3 minutes), the average of the last 5 minutes, the share of peak kept, and whether the
governor met the target of staying within 25% of peak. If it did not, the shortfall is reported as it is.
"""
import argparse
import csv
import statistics
import sys


def load(path):
    with open(path, newline="") as f:
        return [{k: (float(v) if k in ("elapsed_s", "tok_per_s", "thermal", "battery_pct", "tokens") else v) for k, v in r.items()} for r in csv.DictReader(f)]


def summary(rows):
    first = sorted(r["tok_per_s"] for r in rows if r["elapsed_s"] <= 180) or [0]
    peak = first[min(len(first) - 1, int(0.95 * len(first)))]
    end = max(r["elapsed_s"] for r in rows)
    tail = [r["tok_per_s"] for r in rows if r["elapsed_s"] >= end - 300]
    tail_avg = statistics.mean(tail) if tail else 0.0
    return {"peak": peak, "tail": tail_avg, "kept": tail_avg / peak if peak else 0.0, "tokens": sum(r["tokens"] for r in rows),
            "max_thermal": max(r["thermal"] for r in rows), "minutes": end / 60}


def svg(series, path):
    W, H, M = 760, 380, 50
    xmax = max(r["elapsed_s"] for _, rows in series for r in rows) or 1
    ymax = max(r["tok_per_s"] for _, rows in series for r in rows) * 1.1 or 1
    colors = ["#c0392b", "#1a7f37", "#1f6feb"]
    out = ['<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" font-family="sans-serif" font-size="12">' % (W, H),
           '<rect width="100%" height="100%" fill="white"/>',
           '<line x1="%d" y1="%d" x2="%d" y2="%d" stroke="black"/><line x1="%d" y1="%d" x2="%d" y2="%d" stroke="black"/>' % (M, H - M, W - 10, H - M, M, 10, M, H - M)]
    for i in range(5):
        y = H - M - (H - M - 10) * i / 4
        out.append('<text x="5" y="%.0f">%.1f</text><line x1="%d" x2="%d" y1="%.0f" y2="%.0f" stroke="#ddd"/>' % (y + 4, ymax * i / 4, M, W - 10, y, y))
        x = M + (W - M - 10) * i / 4
        out.append('<text x="%.0f" y="%d">%.0f min</text>' % (x - 12, H - M + 18, xmax * i / 4 / 60))
    out.append('<text x="%d" y="%d">tokens per second over time</text>' % (M, 20))
    for k, (name, rows) in enumerate(series):
        pts = " ".join("%.1f,%.1f" % (M + (W - M - 10) * r["elapsed_s"] / xmax, H - M - (H - M - 10) * r["tok_per_s"] / ymax) for r in rows)
        out.append('<polyline fill="none" stroke="%s" stroke-width="2" points="%s"/>' % (colors[k % 3], pts))
        out.append('<text x="%d" y="%d" fill="%s">%s</text>' % (W - 220, 30 + 16 * k, colors[k % 3], name))
    out.append("</svg>")
    open(path, "w").write("\n".join(out))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("csv", nargs="+")
    ap.add_argument("-o", "--out", default="thermal.svg")
    a = ap.parse_args()
    series = [(p.rsplit("/", 1)[-1].replace(".csv", ""), load(p)) for p in a.csv]
    svg(series, a.out)
    for name, rows in series:
        s = summary(rows)
        verdict = "MET (within 25% of peak)" if s["kept"] >= 0.75 else "NOT MET: kept only %.0f%% of peak; report this shortfall" % (100 * s["kept"])
        print("%-28s %4.1f min  peak %.2f tok/s  last-5-min %.2f tok/s  kept %.0f%%  max thermal status %d  %s" % (name, s["minutes"], s["peak"], s["tail"], 100 * s["kept"], s["max_thermal"], verdict if name.endswith("_on") else ""))
    print("wrote", a.out)


if __name__ == "__main__":
    main()
