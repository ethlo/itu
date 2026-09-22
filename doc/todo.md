# Todo

Things decided but not started. Performance experiments themselves go in [perf-log.md](perf-log.md); this is the
list of what to open a session or a branch for.

## Format side: the mirror of what parsing got in 1.16.0

`formatUtc` today: for a non-UTC input, `atZoneSameInstant(UTC).toOffsetDateTime()` (five `java.time` objects and
the JDK's own days-to-civil arithmetic), then a `char[]` scratch buffer, then `new String(char[])` with its
compress-to-Latin-1 loop and a second allocation. Three allocations minimum on a 20-byte result.

1. **Session S10, `String` path** — two rows: fields from `toEpochSecond()` through `DateTimeMath` (Joffe both
   ways, already there) instead of `atZoneSameInstant`; `byte[]` scratch and `new String(bytes, 0, len,
   ISO_8859_1)` instead of `char[]`. Possibly a third: a two-digit pair table in
   `LimitedCharArrayIntegerUtil.toString` if it does not have one. Expect 16 → 9–10 ns on `formatSeconds` with an
   offset input; the `String` + `byte[]` pair is a floor of ~4 ns under a `String`-returning API.
2. **Zero-allocation overloads**, the release feature — `formatUtc(OffsetDateTime, byte[] dst, int offset)` (and
   the `Field` / fraction-digit variants), plus an `Appendable`/`StringBuilder` variant, writing straight into the
   buffer that goes to the socket or file. Same argument as the `char[]` parse path: a pipeline pays the digit
   writes alone, ~5 ns. Bench rows in date-time-wars beside `formatSeconds` etc., and a floor row.

## Smaller

- `ITU.isValid(String, TemporalType...)` is still parse-and-catch; `isValid(String)` got the boolean grammar walk
  in S5.7.
- A `byte[]` overload of the buffer parsers: the throughput harness showed the byte→char copy is a real share of a
  10 ns parse, and files and sockets are bytes.
- The String path's mixed-shape penalty in the 1 GB harness (+55 ns against +4 for the buffer path) is
  unexplained; a shuffled-input `@Param` row would show whether it is `parseDateTime` or `OffsetDateTime`
  construction.
- Sonar wants `DurationParsingSamples` renamed `*Test`; left, because the other three sample classes are named
  the same way.

## Outside this repo

- **`itu-jackson`**, a separate project: Jackson (de)serialisers for `OffsetDateTime`, `Instant` and `DateTime`
  on ITU, registered as a module in one line. Jackson is where most Java timestamps are parsed, and its built-in
  `InstantDeserializer` is the `DateTimeFormatter` path; this is the one thing that would put ITU in front of
  people who never choose a date-time parser.
