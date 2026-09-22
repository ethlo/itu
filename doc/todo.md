# Todo

Things decided but not started. Performance experiments themselves go in [perf-log.md](perf-log.md); this is the
list of what to open a session or a branch for.

## Format side — done in 1.17.0 (perf-log S10)

`formatUtc`/`format` and `Duration.normalized` into a caller's `char[]`/`byte[]`; the String rows −14% / −23% /
−48% (seconds / millis / nanos); offset inputs converted with the library's own arithmetic. Left from it: the
`String` API is bounded by its two allocations and the compress (S10.2, S10.3 dead ends), so the `String` duration
row stays a shade behind `java.time.Duration.toString()` on short inputs; the buffer overload is the answer there.

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
