# Benchmarks (issues A06, A14, A15, A17; PRD 11)

| What | How | Output |
| --- | --- | --- |
| Speed and RAM, median and p95 over 10 runs (A06) | Debug APK: `adb shell am start -n com.sahay.app/.llm.BenchmarkActivity --ei runs 10` | `bench/results/<phone>.md` |
| 20-minute thermal run, governor on and off (A14) | `--es mode sustained --ei minutes 20 --ez governor true` and `false`, then `python bench/thermal_chart.py ...` | CSVs, `thermal.svg`, verdict |
| Energy per token, battery per session (A15) | [ENERGY.md](ENERGY.md) and `python bench/energy_calc.py` | numbers in `bench/results/energy.md` |
| Prefix / KV-cache reuse (A17) | [PREFIX_REUSE.md](PREFIX_REUSE.md) | TTFT table or the reason it is not possible |

Every result file starts with the phone model, SoC, RAM, Android version, model file and its SHA-256, thread count, charging state,
cool or heated start, brightness and room temperature. Without those a number cannot go in the report.

## Run A06 (one command)

    ./gradlew :app:installDebug -Psahay.executorch=true
    adb shell am start -n com.sahay.app/.llm.BenchmarkActivity --ei runs 10
    adb shell run-as com.sahay.app cat files/bench_<Model>.md > bench/results/<phone>.md

It warms up, then runs a fixed 500-token prompt with 200 generated tokens ten times, and prints median, p95, min and max for time to
first token, decode tokens/s, prompt tokens/s and peak RAM (PSS). Prompt-processing speed is computed from time to first token, so it
includes sampling the first token; say so in the report. Edit the "Conditions" line by hand before committing.

## Run A14

    adb shell am start -n com.sahay.app/.llm.BenchmarkActivity --es mode sustained --ei minutes 20 --ez governor false
    adb shell run-as com.sahay.app cat files/sustained_governor_off.csv > bench/results/sustained_governor_off.csv
    # let the phone cool to the same starting temperature, then repeat with --ez governor true -> sustained_governor_on.csv
    python bench/thermal_chart.py bench/results/sustained_governor_off.csv bench/results/sustained_governor_on.csv -o bench/results/thermal.svg

The script prints whether the governor kept tokens/s within 25% of peak. If it did not, commit the numbers anyway and write the shortfall
in the report (PRD 11.3). Tune thresholds in `Governor.kt` only between full runs, and record the version used.

## Status

No on-device numbers exist yet: this folder holds the tooling. Nothing here may be quoted in the report until the result files are committed.
