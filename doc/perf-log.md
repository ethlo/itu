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

## Session S4 — 2026-09-20 · i9-13900H (20 threads) · JDK 25.0.4 (OpenJDK, Ubuntu 26.04) · governor: powersave

**Method for this session.** Time on this machine is ±3% (normal) / ±2% (thorough), which is the size of the
effects being chased, so the rows below are gated on **P-core instructions and branches per op** from
`-prof perfnorm` (`-f 1 -wi 3 -w 1s -i 3 -r 1s`, ±1–2% run-to-run from JIT decisions, no time noise), with ns/op
from the same quick run for orientation only. The cumulative result is confirmed with `--thorough` at the end of the
session. Inputs are the buffer path (`candidates.itu_buffer`) only: `ITU.parseLenient(char[], off, len, buffer)`.

How the counts were obtained: `perf/instr.sh` (perfnorm counters) and `perf/hotpath.sh` (`perf record` attached to the
forked JMH JVM plus `-XX:CompileCommand=print` and `objdump` on the raw nmethod bytes, no hsdis needed); the S4.0 hot
path for input B was 455 instructions: 97 branches, 72 xmm↔gpr moves + 31 stack spills (register pressure: `chars` was
kept in `xmm11`, the parsed fields in `xmm0–xmm5`), 26 char loads, 25 bounds checks, 65 `lea` (index arithmetic).
H3 from S2 is answered: PrintInlining shows the whole parser as one C2 compilation unit already.

| id   | date       | hyp | change (one line)                                                        | A instr / br | B instr / br | C instr / br | A / B / C ns | verdict | where |
|------|------------|-----|--------------------------------------------------------------------------|-------------:|-------------:|-------------:|-------------:|---------|-------|
| S4.0 | 2026-09-20 | —   | BASELINE buffer path @ `e431b8f`                                          | 562 / 106 | 465 / 99 | 325 / 70 | 20.3 / 17.5 / 12.0 | — | `e431b8f` |
| S4.1 | 2026-09-20 | H6  | `length >= 19` fast path: prefix parsed without per-field window checks or the `length ==` chain; short inputs keep the old code | 535 / 96 | 451 / 90 | 289 / 59 | 19.7 / 16.2 / 10.8 | KEPT | `5620e0d` |
| S4.2 | 2026-09-20 | H7  | Digit test in the fast-path helpers: `(c ^ '0') <= 9` — one signed compare per digit, no subtract, no negative test | 504 / 91 | 404 / 85 | 273 / 55 | 19.9 / 15.5 / 10.0 | KEPT | `20910dc` |
| S4.3 | 2026-09-20 | H8  | `ParseConfig`: check the first configured separator before looping over the array (`'T'` / `'.'` for DEFAULT) | 486 / 89 | 394 / 82 | 264 / 55 | 19.7 / 15.1 / 9.1 | KEPT | `13569e9` |
| S4.4 | 2026-09-20 | H9  | `finish`: OR-combined upper-bound test `((23-hour)\|(59-minute)\|(59-second)) < 0` and a `DAYS_IN_MONTH` table instead of the `switch`; generic `validate` only on doubt | 503 / 86 | 404 / 79 | 282 / 53 | 20.0 / 13.9 / 10.2 | REGRESSION | — |
| S4.5 | 2026-09-20 | H10 | `ParseConfig`: separators as a 128-bit ASCII set (shift + mask), array loop only for non-ASCII; replaces S4.3's primary check | 521 / 98 | 394 / 86 | 258 / 52 | 18.4 / 15.1 / 9.9 | KEPT (see S4.6) | |
| S4.6 | 2026-09-20 | H11 | Fraction: nested straight-line blocks for 3/6/9 digits (`digits3`, xor digit test) instead of the 3-at-a-time loop; remainder loop unchanged | 479 / 91 | 325 / 65 | 256 / 52 | 17.9 / 12.7 / 9.9 | KEPT | `ce4822c` |
| S4.7 | 2026-09-20 | H12 | `MutableDateTimeBuffer` stores the field ordinal (int) instead of the `Field` reference: no GC write barrier in `set` | 468 / 89 | 311 / 63 | 251 / 51 | 17.9 / 12.3 / 9.5 | KEPT | `2054234` |
| S4.8 | 2026-09-20 | H13 | Argument checks as one happy-path branch (`chars == null \|\| out == null \|\| (offset \| length) < 0 \|\| offset + length > chars.length`), messages from a slow path | 475 / 88 | 314 / 62 | 245 / 49 | 19.4 / 11.0 / 9.2 | NO-GAIN | — |
| S4.9 | 2026-09-20 | H14 | Seconds and zone-offset fields via `parse2In` / `assertCharAt` where the bound is already known (`length >= 19`, `left >= 6`); dead `length == 19` branch removed | 437 / 82 | 297 / 61 | 244 / 48 | 18.9 / 12.7 / 9.1 | KEPT | `09a5270` |
| S4.10 | 2026-09-20 | H15 | Write year/month/day and hour/minute into the buffer as soon as parsed (drops "untouched on failure"), validate from the buffer at the end | 432 / 81 | 297 / 61 | 242 / 47 | 18.5 / 12.0 / 9.5 | NO-GAIN | — |
| S4.t | 2026-09-20 | —   | **`--thorough` confirmation**, buffer path, S4.0 code (`e431b8f`) vs `09a5270`: 20.34 ±0.40 / 17.02 ±0.22 / 12.14 ±0.28 → **19.72 ±0.91 / 11.71 ±0.40 / 9.00 ±0.19** ns (floor 0.47) | | | | −3% / −31% / −26% | KEPT | |

H6: for a full date-time the ten "is the window long enough" checks and four `length ==` branches are dead weight;
removing them is −14 branches and their index arithmetic. Errors for short inputs are unchanged because they take the
old path; errors for bad characters in long inputs are raised by the same helpers.
Found in the S4.1 disassembly: with no window checks between the loads, C2's range-check smearing collapses the
per-character bounds checks of the prefix into one `offset + 19 < chars.length` — most of the C gain is that.

H7: `(d0 | d1) >= 0 && d0 <= 9 && d1 <= 9` is 3 branches per pair. Neither `x + MIN_VALUE <= 9 + MIN_VALUE` nor
`Integer.compareUnsigned(x, 9) <= 0` made C2 emit an unsigned compare (both: `lea 0x7fffffd0(..)`, `cmp $0x80000009`,
signed jump — same instruction count, fewer branches). `c ^ '0'` is the digit value and ≥ 10 for any non-digit char, so
one signed compare does the whole test and the subtract goes away: −1 instruction and −½ branch per digit.

H8: `isDateTimeSeparator` / `isFractionSeparator` loop over a `char[]` field (array load, length load, loop control)
to accept what is almost always `'T'` / `'.'`. Comparing against the first configured separator first is one load and
one compare on the common path; the loop still runs for the alternatives, so behaviour is unchanged.

H9 (failed): fewer branches should mean fewer instructions. The disassembly says no: each `cmp $imm; jcc` pair was
one macro-fused µop, and the OR-tree became `mov $imm; sub; or …; jl` — 9 instructions for the three time fields
where the compares were 6 — while the table index `m` was rebuilt from three stack spills. Predictable compare
chains are already optimal on x86; combining them only pays when the branches mispredict, and here they never do.

H10: the S4.3 primary check still fell into the array loop (with a safepoint poll) for every `Z`, `+` or `-` after
the seconds, since `isFractionSeparator` is asked first. Two `long`s per separator kind make the test a shift and a
mask for all of ASCII. On its own the row reads as mixed: C −7, B ±0, but A +35 — the A listing showed C2 had fully
unrolled the fraction loop with loop-predication checks and a spill per digit, a code-shape change unrelated to the
separators. S4.6 removes that loop, so S4.5 is judged together with it.

H11: the 3-at-a-time fraction loop is at the JIT's mercy (unrolled or not, predicated or not). Written as nested
straight-line blocks there is nothing to unroll; each block reads three chars with the xor digit test and either
takes them or leaves everything to the one-at-a-time remainder loop, which is unchanged. Semantics are identical
(the differential and fuzz tests exercise 0–12 fraction digits). B's `.123` now costs ~70 instructions over C, not 149.

H12: the S4.0 listing ended with `cmpb $0x0,0x48(%r15)` (G1 pre-barrier check), the reference store, then the
cross-region `xor/shr/je` of the post-barrier: ~10 instructions and 2–3 branches for storing which `Field` was
parsed. An int ordinal is a plain store; `getMostGranularField()` indexes `Field.values()` on the way out.

H13 (no gain): five entry branches into one. Mixed within JIT variance (A +7, B +3, C −6); the OR loses the
individual `offset >= 0` / `length >= 0` facts C2 had from the separate compares. Same lesson as S4.4.

H14: the timezone and seconds fields were still parsed with the window-checked `parse2` and the old three-branch digit
test, although `left >= 6` / `length >= 19` had already been established. Same change as S4.1/S4.2 applied there.

H15 (no gain): the S4.0 listing had 72 xmm↔gpr moves and 31 spills because year…minute stay live until `finish`.
Storing them into the buffer as they are parsed (and reading them back for the validation at the end, so error
precedence is unchanged) should have freed five registers. It changed nothing measurable: the pressure comes from
the index temporaries, `chars.length`, `end` and the fraction/offset state, not from the five field values. The
"buffer untouched on failure" contract therefore stays; there is nothing to buy by giving it up.

### S4 findings

- **Session result** (`--thorough`, ±2–4%): B −31%, C −26%, A −3% (inside error). Instructions 562/465/325 →
  437/297/244, branches 106/99/70 → 82/61/48. A's path is latency-bound (chained `nanos * 1000 + …` multiplies,
  offset arithmetic), so its instruction count fell 22% without the time following; B and C are front-end-bound.
- **What C2 does with this code** (all from the disassembly, none visible at source level): range-check smearing
  removes the per-character bounds checks only when no other `if` sits between the loads (S4.1); `x + MIN_VALUE`
  and `Integer.compareUnsigned` do not become unsigned compares (S4.2); a 3-iteration loop may or may not be
  unrolled, and when it is it carries predication checks and a spill per digit (S4.5/S4.6); a `char[]` lookup loop
  over a one-element array still costs a loop with a safepoint poll (S4.5); a reference store into a long-lived
  object costs a G1 barrier (S4.7); OR-combined range checks are more instructions than macro-fused `cmp/jcc`
  chains (S4.4, S4.8).
- **Not done, candidates for a later session**: (1) transfer S4.1/S4.2/S4.6/S4.9 to the String path in
  `ITUParser` — same structure, `charAt` instead of `chars[i]`; (2) the `parse2`/`parse4` checked variants used by
  `parseShort` still have the old three-branch digit test; (3) A's latency chain: accumulate the three fraction
  blocks independently (`millis * 1_000_000 + micros * 1_000 + nanosPart`) instead of the serial `* 1000 + …`.

## Session S5 — 2026-09-20 · i9-13900H (20 threads) · JDK 25.0.4 (OpenJDK, Ubuntu 26.04) · governor: powersave

**Scope.** The String path, `ITU.parseLenient(String)` (`candidates.itu`), gets the S4 shapes that were left as
candidates: the `length >= 19` fast path (S4.1), the xor digit test (S4.2), the straight-line fraction blocks (S4.6)
and unchecked seconds/zone-offset helpers (S4.9). Then the leftovers that apply to both paths: the checked
`parse2`/`parse4` used by the short path (still the three-branch test), and A's fraction latency chain.
Same method as S4: instructions and branches per op from `perf/instr.sh` (buffer path measured in the same run as a
control), `--thorough` timing for the cumulative result at the end.

| id   | date       | hyp | change (one line)                                                        | A instr / br | B instr / br | C instr / br | A / B / C ns | verdict | where |
|------|------------|-----|--------------------------------------------------------------------------|-------------:|-------------:|-------------:|-------------:|---------|-------|
| S5.0 | 2026-09-20 | —   | BASELINE String path @ `f83dfcd` (buffer path same run: 447 / 82 · 302 / 61 · 243 / 48; 19.7 / 11.7 / 8.9 ns) | 617 / 123 | 521 / 117 | 342 / 69 | 24.3 / 21.4 / 13.1 | — | `f83dfcd` |
| S5.1 | 2026-09-20 | H6  | String path: `availableLength >= 19` fast path with `parse4In`/`parse2In(String)` and `assertCharAt`; short inputs keep the old code (S4.1 transferred) | 593 / 107 | 446 / 119 | 309 / 60 | 24.8 / 18.7 / 12.2 | KEPT | |
| S5.2 | 2026-09-20 | H7  | String `parse2In`/`parse4In`: `(c ^ '0') <= 9` digit test (S4.2 transferred) | 544 / 107 | 441 / 100 | 278 / 55 | 23.1 / 18.4 / 11.1 | KEPT | |

## Dead ends — do not retry without a new reason

- (S2.1) Expecting a large win from "zero allocation" alone on this parser: the objects were cheap TLAB bumps. Zero
  allocation is still the gate for the buffer path, just not a speed-up in itself.
- (S3.1) Unifying the String and char[] parsers by `toCharArray()` on the String side: ≈4 ns flat, 17–29% on the
  String path. The two algorithms stay; keep them aligned through `DateTimeValidator`, `LimitedCharArrayIntegerUtil`,
  `ErrorUtil` and the differential tests, not by sharing the walk.
- (S4.4) Replacing predictable `cmp/jcc` chains with OR-combined arithmetic (`(a - x) | (b - y) < 0`) or a
  lookup table to "save branches": +10–18 instructions. Macro-fused compare-and-branch is the cheapest form of a
  predictable range check on x86; only an unpredictable branch is worth removing.
- (S4.2) Expecting C2 to emit an unsigned compare for `x + MIN_VALUE <= 9 + MIN_VALUE` or
  `Integer.compareUnsigned(x, 9) <= 0`: it does not (`lea`/`cmp $0x80000009`/signed jump). `(c ^ '0') <= 9` is the
  form that works for chars.

## Before the log existed (from git history; numbers were not recorded — the gap this file closes)

- 2026-09-17 `83773f9` — `DateTime.validated()`: arithmetic fast path, `LocalDate.of`/`ChronoField` only on the failure path; straight-line `parse2`/`parse4` — KEPT.
- 2026-09-18 `0b5a211` — configurable path reuses the unrolled `parse2`/`parse4` and caches token kinds in a `byte[]` to avoid a megamorphic call: input A 115 → 69.7 ns, 200 B/op — KEPT.
- 2026-09-18 date-time-wars `20260918-133553-all-normal` (same machine, JDK 25.0.4): `parseLenient` A 21.4 · B 16.8 · C 12.1 ns; B/op 80 / 56 / 56 (`DateTime` 56 B + `TimezoneOffset` 24 B).
