# ITU parse performance log

One line per experiment, written **before** the run. This file exists so that "we already tried that"
is a lookup, not a memory. Read the *Dead ends* section before proposing a change to the parse path.

Numbers are only comparable inside one session block (same machine, JDK, command). Every session starts
with a `BASELINE` row of the code as it is before the first experiment. Cross-session comparisons are
made by re-running the baseline, never by eyeballing two blocks.

**Rules**

- One variable per row; two changes cannot be reverted independently.
- `hyp` and `change` are filled in before `bench.sh` runs.
- Always `--gc`. B/op is a gate: a row that allocates on the buffer path cannot be `KEPT`.
- Under 3% or inside ± error is `NOISE`; one `--thorough` rerun decides, still ambiguous is `NO-GAIN`.
- Sessions are append-only. A retry of an old idea is a new row citing the old id.
- Commits touching the parse path reference their row (`perf-log: S1.3`); `where` holds the SHA once merged.

Verdicts: `KEPT` · `NO-GAIN` (reverted; within ± err or < 3%) · `REGRESSION` (reverted) ·
`BLOCKED` (correctness or API) · `NOISE` (rerun `--thorough`).

Benchmarks live in [date-time-wars](https://github.com/ethlo/date-time-wars):
`mvn -q -DskipTests install` here, `mvn -q package` there, then `./bench.sh --lenient --floor --gc`.
`floor` is the harness alone (JMH loop, state loads, Blackhole); *net* = score − floor is the parser's share.

Inputs: **A** = `2023-01-01T23:38:34.987654321+06:00` · **B** = `5050-01-01T12:02:01.123Z` · **C** = `3074-07-01T12:02:01Z`

---

## Session S1 — 2026-09-20 · i9-13900H (20 threads) · JDK 25.0.4 (OpenJDK, Ubuntu 26.04) · governor: powersave

bench: `./bench.sh --lenient --floor --gc` (normal: 1 fork, 3×2s warm, 5×2s measure) · date-time-wars @ ac55492 + floor benchmark
floor: 0.54 ns (identical on all three inputs)
results: `date-time-wars/results/20260920-132801-lenient+floor-normal-s1-baseline`
note: 12–22% slower than the 2026-09-18 run of the same code on the same machine — thermal/governor drift, which is why every session re-measures its baseline.

| id   | date       | hyp | change (one line)                                                        | A ns      | B ns      | C ns      | B/op (A/B/C) | verdict | where |
|------|------------|-----|--------------------------------------------------------------------------|----------:|----------:|----------:|-------------:|---------|-------|
| S1.0 | 2026-09-20 | —   | BASELINE `parseLenient(String)`, main as-is                                 | 24.7 ±1.7 | 20.5 ±1.3 | 14.0 ±1.8 | 80 / 56 / 56 | —       | `3816879` |

## Session S2 — 2026-09-20 · i9-13900H (20 threads) · JDK 25.0.4 (OpenJDK, Ubuntu 26.04) · governor: performance

bench: `./bench.sh --lenient --floor --gc` (normal: 1 fork, 3×2s warm, 5×2s measure) · date-time-wars @ ac55492 + floor benchmark
floor: 0.46 ns (identical on all three inputs)
results: `date-time-wars/results/20260920-133410-lenient+floor-normal-s2-baseline`, `…-135344-lenient+floor-normal-s2-m2-port`
note: S1 was closed after one row because the governor changed from powersave to performance; nothing else differs.

| id   | date       | hyp | change (one line)                                                        | A ns      | B ns      | C ns      | B/op (A/B/C) | verdict | where |
|------|------------|-----|--------------------------------------------------------------------------|----------:|----------:|----------:|-------------:|---------|-------|
| S2.0 | 2026-09-20 | —   | BASELINE `parseLenient(String)`, main as-is                                 | 22.7 ±3.8 | 17.8 ±1.2 | 12.7 ±0.8 | 80 / 56 / 56 | —       | `3816879` |
| S2.1 | 2026-09-20 | H1+H2 | `parseLenient(char[],off,len,MutableDateTimeBuffer)`: 1:1 port, no allocation (String path same run: 22.5 / 18.4 / 12.5) | 20.1 ±0.5 | 17.2 ±1.8 | 12.7 ±0.6 | 0 / 0 / 0 | KEPT | M2 |

### S2 findings

- **S2.1 — allocation is not where the time goes.** Removing both objects (80 B) and every `String.charAt` bought
  −2.4 / −1.2 / +0.2 ns, against an H1+H2 estimate of 5–9 ns. The remaining 12–20 ns is control flow: call depth,
  branches, the separator loops and validation. H3 (one compiled unit, checked with `-XX:+PrintInlining`) is next,
  before any micro-tuning of digit handling.

## Dead ends — do not retry without a new reason

- (S2.1) Expecting a large win from "zero allocation" alone on this parser: the objects were cheap TLAB bumps. Zero
  allocation is still the gate for the buffer path, just not a speed-up in itself.

## Before the log existed (from git history; numbers were not recorded — the gap this file closes)

- 2026-09-17 `83773f9` — `DateTime.validated()`: arithmetic fast path, `LocalDate.of`/`ChronoField` only on the failure path; straight-line `parse2`/`parse4` — KEPT.
- 2026-09-18 `0b5a211` — configurable path reuses the unrolled `parse2`/`parse4` and caches token kinds in a `byte[]` to avoid a megamorphic call: input A 115 → 69.7 ns, 200 B/op — KEPT.
- 2026-09-18 date-time-wars `20260918-133553-all-normal` (same machine, JDK 25.0.4): `parseLenient` A 21.4 · B 16.8 · C 12.1 ns; B/op 80 / 56 / 56 (`DateTime` 56 B + `TimezoneOffset` 24 B).
