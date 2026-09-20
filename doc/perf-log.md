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

## Session S3 — 2026-09-20 · i9-13900H (20 threads) · JDK 25.0.4 (OpenJDK, Ubuntu 26.04) · governor: powersave

bench: `./bench.sh --gc 'candidates\.itu.*\.parseLenient$|.*\.floor$'` (normal) · date-time-wars @ ac55492 + floor benchmark
floor: 0.47 ns (identical on all three inputs)
results: `date-time-wars/results/20260920-140940-custom-normal-s3-baseline`, `…-141255-custom-normal-s3-1-unified`,
`…-141551-custom-thorough-s3-1-unified-thorough`, `…-141959-custom-thorough-s3-0-baseline-thorough`
note: governor back on powersave, so normal-mode ± is wide again (S1-like); the S3.1 verdict rests on the `--thorough` pair.

| id   | date       | hyp | change (one line)                                                        | A ns      | B ns      | C ns      | B/op (A/B/C) | verdict | where |
|------|------------|-----|--------------------------------------------------------------------------|----------:|----------:|----------:|-------------:|---------|-------|
| S3.0 | 2026-09-20 | —   | BASELINE `parseLenient(String)` @ `c422ced` (buffer path same run: 22.3 / 18.5 / 13.6, 0 B/op) | 25.1 ±3.2 | 19.3 ±1.9 | 14.3 ±3.9 | 80 / 56 / 56 | —       | `c422ced` |
| S3.1 | 2026-09-20 | H4  | One algorithm: String path = `toCharArray()` + `MutableDateTimeBuffer` + `toDateTime()`; `ITUCharArrayParser` takes `textStart` + junk flag to keep the String contract | 26.2 ±2.5 | 24.8 ±3.7 | 16.7 ±2.9 | 168 / 176 / 112 | NOISE → rerun | — |
| S3.1t | 2026-09-20 | H4 | `--thorough` pair, same code. Baseline: 21.8 ±0.9 / 17.9 ±0.3 / 12.1 ±0.3 (buffer path 19.4 / 15.8 / 12.1) | 25.5 ±0.5 | 22.1 ±0.9 | 15.7 ±0.3 | 168 / 176 / 112 | REGRESSION | — |

H4: the copy (≈30 chars, `StringLatin1.inflate` intrinsic) plus the two extra objects cost < 3% on the String path, so
the duplicate String algorithm (≈250 lines guarded only by the differential tests) can go. B/op on the String path will
rise (char[] + buffer); that is accepted if the time is flat, since the gate applies to the buffer path.

### S3 findings

- **S3.1 — the String path cannot be routed through the char[] algorithm by copying.** +3.7 / +4.2 / +3.5 ns
  (+17% / +24% / +29%) with ±2–4% bars: a flat ≈4 ns per call for `toCharArray()` (TLAB bump + Latin-1 inflate) and
  the `MutableDateTimeBuffer` (scalar-replaced on input A only, per the B/op). On a 12–22 ns operation that is the
  whole margin over the buffer path. The buffer path itself moved +0.4–1.0 ns from the two extra parameters
  (`textStart`, junk flag) threaded through every private method — at the edge of the error bars, but not free either.
- All 495 tests passed with the shared core (the `textStart` / junk-flag plumbing reproduced the String contract:
  absolute error indices, whole text in messages, no trailing-junk check from a non-zero offset), so the duplication
  is a performance decision, not a correctness one. The differential + fuzz tests remain the guard.
- Not tried: one algorithm over an abstract char source (`String` vs `char[]`, bimorphic `charAt` with a type guard
  per access, ≈35 accesses). If someone wants the unification badly enough, that is the next row (H5), with the same
  gate: String path flat, buffer path 0 B/op.

## Dead ends — do not retry without a new reason

- (S2.1) Expecting a large win from "zero allocation" alone on this parser: the objects were cheap TLAB bumps. Zero
  allocation is still the gate for the buffer path, just not a speed-up in itself.
- (S3.1) Unifying the String and char[] parsers by `toCharArray()` on the String side: ≈4 ns flat, 17–29% on the
  String path. The two algorithms stay; keep them aligned through `DateTimeValidator`, `LimitedCharArrayIntegerUtil`,
  `ErrorUtil` and the differential tests, not by sharing the walk.

## Before the log existed (from git history; numbers were not recorded — the gap this file closes)

- 2026-09-17 `83773f9` — `DateTime.validated()`: arithmetic fast path, `LocalDate.of`/`ChronoField` only on the failure path; straight-line `parse2`/`parse4` — KEPT.
- 2026-09-18 `0b5a211` — configurable path reuses the unrolled `parse2`/`parse4` and caches token kinds in a `byte[]` to avoid a megamorphic call: input A 115 → 69.7 ns, 200 B/op — KEPT.
- 2026-09-18 date-time-wars `20260918-133553-all-normal` (same machine, JDK 25.0.4): `parseLenient` A 21.4 · B 16.8 · C 12.1 ns; B/op 80 / 56 / 56 (`DateTime` 56 B + `TimezoneOffset` 24 B).
