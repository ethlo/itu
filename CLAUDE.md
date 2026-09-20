# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`com.ethlo.time:itu` — a zero-dependency, Java 8 compatible library for parsing and formatting RFC-3339 /
W3C date-times and a strict ISO-8601 duration subset. Single Maven module, no wrapper (use the system `mvn`).

## Commands

```bash
mvn -ntp verify                              # full build (compile, tests, jar, javadoc, sources)
mvn -ntp test                                # tests only
mvn -ntp test -Dtest=DurationTest            # single test class
mvn -ntp test -Dtest=DurationTest#testPositiveNormalized  # single test method
mvn -ntp -Pcoverage verify                   # what CI runs; jacoco report in target/site/jacoco
mvn -ntp -Preadme process-resources          # regenerate README.md (see "Generated README")
JAZZER_FUZZ=1 mvn -ntp test -Dtest=ParseDurationFuzzTest   # actual fuzzing (30m per @FuzzTest)
```

CI (`.github/workflows/build.yml`) runs `mvn -Pcoverage verify` on JDK 8, 11, 17 and 21, so **all code must
compile and run on Java 8** (`source`/`target` 1.8, no newer APIs, no `var`).

Profiles activate automatically by JDK: `proguard` on `[9,22)` builds the published `-small` jar,
`java-module` on `[9,)` adds `module-info` via moditect. On a newer local JDK neither/only the latter runs,
so a local `verify` is not byte-for-byte what a release produces.

The `license-maven-plugin` rewrites Apache license headers into `src/main/java` and `src/test` at
`process-sources`; new files get their header on the next build, don't hand-write it.

## Architecture

`ITU` (`src/main/java/com/ethlo/time/ITU.java`) is the static facade over everything and the only entry point
most users see. It delegates to three engines:

- **`internal/fixed/ITUParser`** — the fast path. A hand-rolled, index-arithmetic parser over the known
  RFC-3339 character layout (`parseDateTime`, `parseLenient`). No regex, no `DateTimeFormatter`, no
  intermediate collections.
- **`internal/fixed/ITUCharArrayParser`** — the same algorithm over a `char[]` window into a
  `MutableDateTimeBuffer`, zero allocation. Kept method-for-method in step with `ITUParser`; the corpus runs every
  case through both.
- **`internal/fixed/ITUValidator`** — `ITU.isValid(String)` as a boolean walk of the strict grammar, no exceptions.
  A third copy of the grammar, held to `parseDateTime` by `IsValidDifferential` (corpus, `IsValidTest`, fuzz target).
- **`internal/fixed/ITUFormatter`** — formatting, writing straight into a `char[]`.
- **`internal/ItuDurationParser`** — duration parsing, feeding a `DurationPartsConsumer` state machine that
  validates unit order/duplication as it goes.

Alongside the fixed path there is a **configurable token path**: `DateTimeParsers.of(DateTimeTokens.digits(...),
separators(...), fractions(), zoneOffset())` composes `internal/token/*` tokens into a
`token.ConfigurableDateTimeParser`. Both paths implement the same `DateTimeParser` interface and produce
`DateTime`.

### Key types

- **`DateTime`** — granularity-aware intermediate value (`TemporalAccessor`). Lenient parsing returns this;
  strict RFC-3339 parsing returns `OffsetDateTime`. `Field` carries both the granularity ordering and the
  character offset each field ends at, which the parsers rely on.
- **`Duration`** — value type of `long` seconds + positive `int` nanos (sign lives on seconds).
  `normalized(DurationUnit)` renders with the largest unit capped; `internal/util/DurationFormatter` does the
  work. `DurationUnit.WEEKS` output (`P4W...`) is deliberately *not* readable by `java.time.Duration.parse`.
- **`ParseConfig`** — allowed date/time and fraction separators plus `failOnTrailingJunk`. Immutable,
  `DEFAULT` and `STRICT` constants; `withX` copies.
- **`TemporalHandler` / `TemporalConsumer`** — visitor-style dispatch on parsed granularity.
- **Leap seconds** — `internal/util/DefaultLeapSecondHandler` loads `src/main/resources/leap_second_dates.csv`
  at class init; parsing a valid leap second throws `LeapSecondException` (Java cannot represent second 60),
  which exposes the nearest valid date-time. Add new leap seconds to that CSV.

### Package boundary

`com.ethlo.time` and `com.ethlo.time.token` are public API; everything under `com.ethlo.time.internal` is
excluded from javadoc, from `Export-Package` in the OSGi bundle, and from `module-info` exports. Internals can
be reshaped freely; public signatures cannot. ProGuard `-keep` rules in `pom.xml` list the public classes —
a new public class that needs to survive obfuscation must be added there.

### Performance discipline

This library exists to be much faster than `java.time`, and the code is shaped accordingly — deviating from
these patterns silently regresses the benchmarks:

- No runtime dependencies, ever. All non-JDK deps are `test` scope.
- Hot methods are kept small and branch-light so the JIT inlines them (see the comments in
  `LimitedCharArrayIntegerUtil.parse2` and the token-kind `byte[]` cache in `ConfigurableDateTimeParser`,
  which exists to avoid a megamorphic interface call).
- `LimitedCharArrayIntegerUtil` holds a precomputed 4-digit conversion table and `POW10` lookups instead of
  doing arithmetic per digit.
- Avoid streams, lambdas, boxing, string concatenation and defensive copies on parse/format paths. Error
  message construction is fine — it only runs on failure.
- Benchmarks live in the separate [date-time-wars](https://github.com/ethlo/date-time-wars) repo, not here.
- `doc/perf-log.md` records every performance experiment on the parse path with its result and verdict.
  Read its *Dead ends* section before proposing an optimisation, and add a row (hypothesis first, then the
  numbers) for every attempt, kept or reverted.

#### Measuring: count instructions, not nanoseconds

A parse is 10–20 ns. On a laptop the timing noise (±2–3% even with `bench.sh --thorough`, worse under the
`powersave` governor) is the size of the effects worth chasing, so a single change usually cannot be judged
by ns/op. Instructions and branches per op from the hardware counters are deterministic to ~1% and are the
gate for individual changes; elapsed time confirms the accumulated result at the end of a session.

- `perf/instr.sh` — rebuilds itu and date-time-wars, runs the char[] buffer benchmark (or any JMH regex) with
  `-prof perfnorm` and prints instructions / branches / branch-misses / ns per op for each input. ~1 minute.
  A jump of a few percent that the change cannot explain is the JIT choosing a different register
  allocation or inlining — rerun before believing it.
- `perf/hotpath.sh '<input>' <tag>` — dumps the C2 code of the parser (`-XX:CompileCommand=print`, no hsdis
  needed), samples it with `perf record` and writes `perf/out/dis-<tag>.txt`: every machine instruction with
  its share of samples. `awk '$1 != "0.00%"'` on it is the executed path; count what is in it (bounds checks,
  `vmovq`/`vmovd` xmm↔gpr shuffles and `(%rsp)` spills mean register pressure) before guessing at a cause.
- Both need `perf` (`linux-tools`) with `kernel.perf_event_paranoid <= 2` and a date-time-wars checkout next
  to this repo (`DTW_DIR` overrides). Output goes to `perf/out/` (gitignored).
- Things learned this way that a source-level reading would not show: C2 only removes the per-character
  bounds checks when no other `if` sits between the `chars[offset + k]` loads (range-check smearing); it does
  not emit unsigned compares for `x + MIN_VALUE` or `Integer.compareUnsigned`; a reference store into the
  buffer costs a GC barrier; a local that only an inlined callee's slow path needs is still materialised when
  that path is an uncommon trap, so pass `(base, constant)` positions, never a precomputed `offset + k`.
  See the S4 and S6 findings in `doc/perf-log.md`.

### Error reporting

All parse failures funnel through `internal/util/ErrorUtil` and surface as `java.time.format.DateTimeParseException`
with a **1-based position in the message text** and a **0-based `getErrorIndex()`**. Tests
(`ErrorOffsetTest`, `date-time-corpus.json`, `duration-corpus.json`) assert on both, so message wording and index
arithmetic are effectively part of the contract.

## Tests

- Parsing is tested from two declarative corpora, and a case belongs there whenever it is "this input gives
  this output":
  - `src/test/resources/date-time-corpus.json` — one entry per input (`lenient`, `offset`, `config`, canonical
    `expected` text, `instant`, `parse_length`, `error` + `error_index`, `leap_second`, `note`; the field
    contract is the javadoc of `DateTimeCase`). `DateTimeCorpusTest` runs every entry through the String path
    with the overloads the entry names, and through the char[] path differentially (`CharArrayDifferential`),
    so a case recorded once covers every implementation of the grammar.
  - `src/test/resources/duration-corpus.json` (`DurationCase`, `DurationCorpusTest`) — every success is also
    round-tripped through `normalized()` and, when it has no weeks, compared with `java.time.Duration.parse`.
  - Put the *why* in the entry's `note`; that is where the regression stories from the old test names went.
- Java tests are for API behaviour that is not an input/output pair: `DurationTest` (arithmetic),
  `TimezoneOffsetTest`, `TemporalAccessorTest`, `ConfigurableDateTimeParserTest`, the window/buffer tests in
  `CharArrayParseTest`, `ParseConfig` withers, `ErrorOffsetTest` (error index vs the JDK's), and `IsValidTest`
  (the edges `isValid` decides itself, each also checked against `parseDateTime`).
- `CorrectnessRegressionTest` is organised as one `@Nested` class per historical defect, with a javadoc
  describing the bug. Parse-shaped regressions go in the corpus with a `note`; API-shaped ones follow that
  pattern.
- `src/test/java/com/ethlo/time/fuzzer/*` are jazzer `@FuzzTest`s. Under a normal `mvn test` they only replay
  the seed corpus (fast); set `JAZZER_FUZZ=1` to fuzz for real.
- `src/test/java/samples/**` are real tests *and* the source of the README examples — see below.

## Generated README

`README.md` at the repo root is generated; **do not edit it directly**. The source is `src/site/README.md`,
where `${src/test/java/samples/parsing}`-style placeholders are expanded by `source-extractor-maven-plugin`
(template: `src/site/sample-code.template.md`) from the javadoc and bodies of the `samples/**` test methods.
So: change a sample by editing the sample test, change prose by editing `src/site/README.md`, then run
`mvn -Preadme process-resources`.

Two things in the generated file come from elsewhere: the `<!-- BENCH:START -->` table is produced by
date-time-wars' `report.py`, and the advertised dependency version comes from the `itu.released.version`
property in `pom.xml` (bump on each release — `project.version` intentionally stays on `-SNAPSHOT`).

## Releasing

`project.version` stays `-SNAPSHOT` on `main`. Pushing a `v*.*.*` tag triggers
`.github/workflows/release.yml`, which derives the version from the tag, runs `versions:set`, and
`mvn -Dgpg.skip=false clean deploy` to Central Portal on JDK 21 (chosen so the `proguard` profile is active).
`gpg.skip` is `true` by default so local builds don't need a signing key.

## Code style

`.editorconfig` is authoritative: 4-space indent, 8-space continuation, max line length 200, **braces on their
own line** (Allman, including for methods and classes), and no wildcard imports (import-on-demand threshold is
9999). Fields and locals are `final` wherever possible throughout the codebase.
