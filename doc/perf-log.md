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
| S5.3 | 2026-09-20 | H11 | String fraction: nested straight-line 3/6/9-digit blocks (`digits3`, xor test) instead of the 3-at-a-time loop; remainder loop xor test (S4.6 transferred) | 519 / 96 | 353 / 68 | 274 / 55 | 18.8 / 13.6 / 10.4 | KEPT | |
| S5.4 | 2026-09-20 | H14 | String seconds and zone-offset fields via `parse2In` / `assertCharAt` where the bound is already known (S4.9 transferred) | 466 / 86 | 343 / 65 | 269 / 53 | 17.2 / 13.5 / 10.8 | KEPT | |
| S5.5 | 2026-09-20 | H7  | Checked `parse2`/`parse4` (String and char[]; used by `parseShort` and the token path): xor digit test. Gate: `candidates.itu_configurable` A / B, baseline 1693 / 310 · 1652 / 324 (65.7 / 62.9 ns) → 1668 / 301 · 1625 / 317 (two runs; a first run read B as 1350, JIT variance) | — | — | — | 71.2 / 63.7 | KEPT | |
| S5.6 | 2026-09-20 | H16 | Both parsers, 9-digit fraction: `millis * 1_000_000 + micros * 1_000 + nanosPart` (independent products) instead of the serial `nanos * 1000 + …` chain. Instructions unchanged by construction (466 / 85 · buffer 453 / 83); judged on `--thorough` A: String 17.63 ±1.22 → 17.45 ±1.40, buffer 19.39 ±0.24 → 19.63 ±0.48 | 466 / 85 | 344 / 66 | 273 / 53 | 17.5 / 14.4 / 10.4 | NO-GAIN | — |
| S5.7 | 2026-09-20 | H17 | `ITU.isValid(String)`: a boolean walk (`ITUValidator`) instead of parse-and-catch. Gate: `.*\.isValid$` (`common.IsValidBenchmark`), valid A / B / C and invalid (trailing junk / month 13 / truncated / not a date): 709 / 519 / 455 instr, 28.6 / 20.5 / 18.0 ns · 14157 / 13661 / 13239 / — instr, 860 / 871 / 809 / — ns → 292 / 216 / 151 instr, 11.3 / 9.2 / 6.4 ns · 150 / 132 / 31 / 31 instr, 6.3 / 5.6 / 1.2 / 1.2 ns | — | — | — | — | KEPT | |
| S5.t | 2026-09-20 | —   | **`--thorough` confirmation**, String path, S5.0 code (`f83dfcd`) vs S5.7: 22.65 ±0.51 / 18.76 ±0.40 / 12.05 ±0.31 → **16.58 ±0.27 / 12.54 ±0.26 / 9.61 ±0.18** ns (floor 0.44); buffer path same runs 19.51 / 11.66 / 9.25 → 19.20 / 11.31 / 8.60 | | | | −27% / −33% / −20% | KEPT | |

H16 (no gain): the S4 findings blamed A's flat time on the serial `nanos * 1000 + …` chain. Making the three
products independent changed nothing measurable, on either path, so the latency is elsewhere. The `--thorough`
pair also showed the buffer path *slower* than the String path on A (19.4 vs 17.6 ns) while ahead on B and C, with
fewer instructions (453 vs 466): the buffer path has an A-specific stall that a `hotpath.sh` listing should find.

H17: a validator is called on invalid input by design, and parse-and-catch pays a formatted message plus a stack
trace for each one: 13–14k instructions and 800+ ns, 130× the valid case. A straight-line boolean walk with the same
acceptance (held to `parseDateTime` by the corpus, a directed edge-case list and a differential fuzz target) makes
an invalid answer cost 1–6 ns and a valid one 6–11 ns; the valid case also drops the `OffsetDateTime` construction
that `parseDateTime` needs and `isValid` never did.

### S5 findings

- **Session result** (`--thorough`, ±2–3%): String path A −27%, B −33%, C −20%; instructions 617/521/342 →
  466/343/269, branches 123/117/69 → 86/65/53. The S4 shapes transferred one for one; the String path is now
  within 1–3 ns of the buffer path on B and C and ahead of it on A (see H16). `String.charAt` costs a coder check
  and a bounds check per access that `chars[i]` does not, which is where the remaining B/C gap is.
- `isValid` went from 18–29 ns valid / 800+ ns invalid to 6–11 ns / 1–6 ns (S5.7); its grammar is a third walk, but
  a boolean one with no error contract, guarded differentially rather than by shared code.
- **Not done, candidates for a later session**: (1) the buffer path's A-specific stall (H16: slower than the String
  path on A alone, with fewer instructions); (2) `ITU.isValid(String, TemporalType...)` still parses and catches.

## Session S6 — 2026-09-20 · i9-13900H (20 threads) · JDK 25.0.4 (OpenJDK, Ubuntu 26.04) · governor: powersave

**Scope.** The S5 lead: the buffer path is slower than the String path on input A alone (19.4 vs 17.6 ns) with fewer
instructions. `hotpath.sh` on both paths first, then diagnostics that isolate the cause before any candidate. Gate as
in S4/S5: `perf/instr.sh` instructions and branches per op, buffer path unless stated; `--thorough` at the end.

What the two A listings showed (`dis-A-buf.txt` vs `dis-A-str.txt`): the buffer path spends 32% of its samples on
xmm↔gpr moves against 8% for the String path; `chars` is moved to `xmm9` at entry and pulled back into a GPR before
every one of ~30 loads (`vmovq %xmm9,%r10; movzwl 0x2a(%r10,%r8,2)`), `length` is spilled to the stack at entry, and
the parsed digits go through `xmm1–xmm5`. It is register pressure, and the String path does not have it because the
benchmark calls `ITU.parseLenient(String)`, where `offset` is the constant 0: no `offset + k` index temporaries, no
sign-extended copy of `offset` for addressing, and bounds checks that fold against `length >= 19`.

| id   | date       | hyp | change (one line)                                                        | A instr / br | B instr / br | C instr / br | A / B / C ns | verdict | where |
|------|------------|-----|--------------------------------------------------------------------------|-------------:|-------------:|-------------:|-------------:|---------|-------|
| S6.0 | 2026-09-20 | —   | BASELINE buffer path @ `869c38d` (String path same code: 461 / 85 · 344 / 66 · 272 / 53; 16.8 / 12.6 / 9.7 ns) | 449 / 82 | 304 / 62 | 242 / 48 | 19.7 / 11.9 / 8.4 | — | `869c38d` |
| S6.1 | 2026-09-20 | H18 | DIAGNOSTIC, not a candidate: `offset` replaced by the constant 0 inside the parser — the cost of a variable window start | 354 / 76 | 241 / 56 | 194 / 45 | 13.2 / 8.7 / 6.4 | (−95 / −63 / −48 instr) | — |
| S6.2 | 2026-09-20 | H18 | DIAGNOSTIC: S6.0 code with `-XX:-UseCompressedOops` (frees `r12` as a 14th GPR), input A only | 423 / 83 | — | — | 16.3 (one run: 18.4) | (−27 instr) | — |
| S6.3 | 2026-09-20 | H19 | `ParseConfig.isDateTimeSeparator` / `isFractionSeparator`: `needle == primary \|\| set test`, the first configured separator compared before the 128-bit set. String path: 421 / 78 · 305 / 57 · 271 / 51 (14.9 / 11.4 / 10.0 ns); strict `parse`: 702 / 110 · 521 / 84 · 465 / 72 → 685 / 103 · 500 / 76 · 451 / 70 | 395 / 73 | 269 / 52 | 238 / 47 | 15.6 / 9.5 / 8.7 | KEPT | |
| S6.4 | 2026-09-20 | H20 | Year/month/day and hour/minute packed into two ints (7 bits per field) after the prefix, unpacked in `finish`: fewer values live across the fraction | 410 / 73 | 284 / 52 | 246 / 47 | 15.8 / 10.1 / 8.7 | REGRESSION | — |
| S6.5 | 2026-09-20 | H21 | Fraction block guards as `length >= 23 / 26 / 29` instead of `idx + 3 <= end`, so `end` is not live in the blocks | 404 / 73 | 273 / 52 | 239 / 47 | 15.3 / 9.8 / 8.9 | NO-GAIN | — |
| S6.6 | 2026-09-20 | H22 | Positions passed as `(base, constant)` pairs, never as a precomputed `offset + k`: `parse2In`/`parse4In`/`assertCharAt`/`assertNoMoreChars` take `base, rel`; the fraction blocks index `offset + 20/23/26` directly and derive `idx` afterwards | 363 / 73 | 261 / 52 | 224 / 47 | 13.1 / 9.0 / 8.1 | KEPT | |
| S6.7 | 2026-09-20 | H23 | `TimezoneOffset.toZoneOffset`: quarter-hour `ZoneOffset`s from a 145-entry array (`totalSeconds / 900 + 72`) instead of `ZoneOffset.ofHoursMinutes` → `ofTotalSeconds` → `ConcurrentHashMap.get(Integer)`; UTC keeps returning the constant. Gate: strict `parse` (`candidates.itu.ItuParseBenchmark`), same-session baseline 683 / 103 · 497 / 76 · 455 / 71 (25.6 / 19.2 / 16.4 ns) | 660 / 99 | 512 / 78 | 458 / 71 | 25.6 / 19.5 / 16.9 | NO-GAIN | — |
| S6.t | 2026-09-20 | —   | **`--thorough` confirmation** vs the S6.0 run (`20260920-202242-all-thorough`, floor 0.44): buffer path 19.1 / 11.4 / 8.84 → **13.0 ±0.2 / 8.89 ±0.2 / 7.81 ±0.2** ns; String path 16.5 / 12.6 / 9.61 → **15.1 ±0.4 / 11.1 ±0.2 / 10.0 ±0.2**; strict `parse` 25.1 / 18.4 / 16.2 → 24.9 / 17.8 / 16.4 | | | | −32% / −22% / −12% (buffer) | KEPT | `20260920-213655`, `-214854` |

H18 (diagnostics): the S5 lead is not A-specific in cause, only in size. With `offset` a compile-time constant the
buffer path is 95 / 63 / 48 instructions and 6.5 / 3.2 / 2.0 ns cheaper on A / B / C and ends up *ahead* of the String
path on all three, so the whole gap is what a variable window start costs: a 32-bit `offset` for the index
arithmetic, a sign-extended 64-bit copy for addressing, `chars.length` for the bounds checks that no longer fold, and
`end`/`length` both live — 3–4 registers on a path that is already at the edge, which is where A (nine fraction
digits and an offset, the most live values) tips over. One extra GPR (S6.2) is worth 27 instructions and 3 ns on A
by itself. The API is the window, so the constant cannot be had; the candidates below try to give back registers.

H19: the S4.5 bit-set membership test is a diamond (`c < 64 ? lo : hi`, a shift, a mask, a phi) and C2's
range-check smearing does not walk through it: every bounds check *after* the fraction separator (the three digit
blocks and the zone offset) survived, keeping `chars.length` and the `lea k(%rdx)` index temporaries live to the end
of the method. A plain compare against the first configured separator in front of the set — the S4.3 form, which
S4.5 had replaced — makes the common case a single `cmp/jcc`, and the smearing then folds every remaining check into
the prefix's: the A listing goes from 8 bounds checks to 0, hot-path instructions 338 → 302, xmm↔gpr share 32% → 16%.
The String path uses the same `ParseConfig` and gains the same way (A −40, B −39), as does strict `parse`.

H20 (regression), H21 (no gain): both tried to give registers back by shortening live ranges at source level, and
C2 undid both. The packed fields are computed where they are used — global code motion schedules a pure expression
late, next to its consumer in `finish` — so the five inputs stay live exactly as before and the pack/unpack is pure
cost (+8–15 instructions). `length >= 23` in place of `idx + 3 <= end` only moved the arithmetic around. The same
lesson as S4.10: what is live is decided by the graph, not by where a local is declared.

H22: the S6.3 listing still had ~12 `mov 0x28(%rsp),%r10d; add $0x1d,%r10d; vmovd %r10d,%xmm3` sequences — `offset`
reloaded from its stack slot, a constant added, the sum parked in an xmm register — and none of those sums fed a
load (the loads already carry the constant in their displacement, `movzwl 0x4c(%r10,%rcx,2)`). They are the
`start` / `index` / `lastUsed` / `idx` locals of the inlined helpers: each helper's slow path is an uncommon trap once
inlined, and every local the interpreter would need there must be materialised before the branch, because debug
info can name a register, a stack slot or a constant but cannot recompute `offset + 5`. Passing `(base, 5)` makes the
interpreter-visible locals `base` (already live) and a constant. Buffer path A −32 / B −8 / C −14 instructions, and A
is now within 9 of the constant-offset diagnostic (S6.1). The String path is unaffected in the benchmark (its
`offset` is the constant 0 there, so those sums were constants already) and keeps its shape.

H23 (no gain): the strict path's extra ~8 ns over `parseLenient` is `toOffsetDatetime()`, and for a non-UTC offset
that includes the JDK's cache lookup for `ZoneOffset`. A direct table takes 23 instructions off A but the time does
not move (25.6 → 25.6 ns), and B / C — UTC, which never touch the table — read +15 / +3 from the JIT reshuffling
around it. A first version without the explicit UTC branch was worse still (B +32, C +38): returning the table's
entry instead of the `ZoneOffset.UTC` constant costs C2 the folding it does around a constant offset downstream in
`OffsetDateTime.of`. The hash lookup is not where the strict path's time goes; the `LocalDate`/`LocalTime`/
`LocalDateTime`/`OffsetDateTime` construction and re-validation is, and that is JDK code.

### S6 findings

- **Session result** (`--thorough`): buffer path A −32%, B −22%, C −12% (19.1 / 11.4 / 8.84 → 13.0 / 8.89 / 7.81 ns);
  instructions 449 / 304 / 242 → 363 / 261 / 224, branches 82 / 62 / 48 → 73 / 52 / 47. The S5 lead is closed: the
  buffer path is now ahead of the String path on all three inputs, A included (13.0 vs 15.1 ns). The String path
  took −8% / −12% on A / B from S6.3 alone (C +4%, at the edge of the error bars with unchanged instructions).
- **Why the buffer path was slower on A**: not an A-specific stall but register pressure that A's live set (nine
  fraction digits, an offset, five fields) tipped over, and the pressure came from the variable window start — the
  String benchmark parses at the constant offset 0. Two of its three costs were removable: bounds checks that the
  separator set's diamond kept from smearing (S6.3), and `offset + k` locals materialised for the inlined helpers'
  uncommon traps (S6.6). What remains — `chars` parked in an xmm register and copied to a GPR before each load,
  `offset` and `length` on the stack — is the allocator's choice with a 64-bit sign-extended copy of `offset` for
  addressing and the int for the window checks both live; one more GPR is worth another ~25 instructions (S6.2).
- **What C2 does with this code**, added to the S4 list: range-check smearing stops at the separator set's
  `c < 64 ? lo : hi` diamond, not only at loop merges (S6.3); a local that is only needed on an inlined callee's
  slow path still costs a materialised value when that path is an uncommon trap, because deopt state cannot
  recompute an expression (S6.6); a pure expression is scheduled next to its consumer, so packing fields early
  does not shorten their live ranges (S6.4, cf. S4.10).
- **Not done, candidates for a later session**: (1) the same `(base, constant)` rule in `ITUParser` for callers
  parsing at a non-zero `ParsePosition` — invisible in the benchmark, which parses at 0; (2) the String path's
  remaining gap to the buffer path (15.1 vs 13.0 on A) is `charAt`'s coder and bounds checks plus the `DateTime`
  and `TimezoneOffset` allocations; (3) `ITU.isValid(String, TemporalType...)` still parses and catches.

## Session S7 — 2026-09-21 · i9-13900H (20 threads) · JDK 25.0.4 (OpenJDK, Ubuntu 26.04) · governor: powersave

**Scope.** `parseEpochMilli` — epoch-millis text into civil fields — new on this branch and at 30 ns for both the
String and the buffer path, against 9–15 ns for the date-time string itself. The rows are the epoch-millis text of
A / B / C: **A** = `1672594714987` (13 digits), **B** = `97195464121123`, **C** = `34854580921000` (14 digits).
Gate: `perf/instr.sh 'candidates\.itu_epoch.*'` — one run covers both rows, the buffer row
(`itu_epoch_buffer`) is the gate, the String row (`itu_epoch`) is reported with it. `--thorough` at the end.

What the S7.0 listing showed (`dis-E0.txt`, input B, buffer path; the method is 58 bytecodes, so it is inlined
into the JMH stub and that is the nmethod to print): the digit loop is unrolled ×4 and cheap — `movzwl`,
`lea -0x30`, `cmp $0xa`, two `lea`s for the ×10 — but every digit also runs the `MAX_DIGITS` guard (`cmp $0x12`,
`cmovg`). The conversion is a chain of 64-bit divisions by constants — 1000, 86400, then Hinnant's 146097, 1460,
36524, 146096, 365, 153, 5 — each `movabs magic; imul; sar` plus, because the dividend may be negative, a
`mov; sar $0x3f; sub` sign correction, and `floorDiv`/`floorMod` add a `test`/`cmp` pair each. The buffer stores
and the `civilFromDays` unpack are a handful of instructions. So roughly a third is the digits and two thirds the
arithmetic, and the arithmetic is all sign handling and 64-bit magic multiplies on values that are provably
non-negative and mostly fit in an int.

| id   | date       | hyp | change (one line)                                                        | A instr / br | B instr / br | C instr / br | A / B / C ns | verdict | where |
|------|------------|-----|--------------------------------------------------------------------------|-------------:|-------------:|-------------:|-------------:|---------|-------|
| S7.0 | 2026-09-21 | —   | BASELINE buffer path, `8b7bc49` + epoch parser (String path same code: 367 / 42 · 386 / 46 · 381 / 46; 30.3 / 31.2 / 30.4 ns) | 353 / 35 | 368 / 39 | 371 / 39 | 30.0 / 31.2 / 30.8 | — | `3b8fe2b` |
| S7.1 | 2026-09-21 | H24 | Range-check the raw value, then rebase to seconds since 0000-01-01 (always ≥ 0) and shift the era arithmetic by one era: plain `/` and multiply-subtract everywhere, no `floorDiv`/`floorMod`. String path: 363 / 38 · 380 / 43 · 375 / 43 | 348 / 32 | 367 / 37 | 362 / 36 | 30.7 / 33.3 / 31.8 | NO-GAIN (kept anyway: −3 branches, and the simpler code; not a speed-up) | |
| S7.2 | 2026-09-21 | H25 | `civilFromDaysSince0000` in `int`: the day count is 22 bits, so the seven era/year/month divisions become 32-bit magic multiplies instead of 64-bit high multiplies. String path: 389 / 40 · 394 / 43 · 347 / 38 | 351 / 32 | 366 / 36 | 374 / 37 | 32.5 / 35.8 / 33.5 | NO-GAIN (kept anyway, as the simpler code with S7.1; not a speed-up) | |
| S7.3 | 2026-09-21 | H26 | Non-negativity hints: no-op `&` masks on every dividend (`& Long.MAX_VALUE` on the two long ones, `& 0x3FFFFF` / `0x3FFFF` / `0x1FF` on days, doe, yoe, doy, `& 0x1FFFF` on secondOfDay) so C2's type says ≥ 0 and it omits the sign correction of each magic division. String path: 363 / 39 · 378 / 43 · 376 / 43 | 353 / 32 | 369 / 36 | 375 / 37 | 31.2 / 32.4 / 32.8 | NO-GAIN (reverted: the corrections went, the masks and the add-back magics cost the same) | — |
| S7.4 | 2026-09-21 | —   | DIAGNOSTIC, not a candidate: conversion removed, the parsed long stored as-is — what the digit walk and the stores cost by themselves | 209 / 30 | 224 / 34 | 225 / 34 | 11.5 / 12.3 / 12.4 | (conversion = ~145 instr, ~19 ns) | — |

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
