# Credits

ITU is licensed under the Apache License, Version 2.0 (see [LICENSE](LICENSE)). The calendar arithmetic in
`com.ethlo.time.internal.util.DateTimeMath` is ported from published algorithms; the ports are ITU's own code
and carry no other license, as their authors have confirmed.

## Ben Joffe

- `civilFromDaysSince0000` — the ["very fast 64-bit date algorithm"](https://www.benjoffe.com/fast-date-64) in
  its 32-bit form (four multiplications, no division), from `benjoffe_fast32_v2.hpp` in
  [fast-date-benchmarks](https://github.com/benjoffe/fast-date-benchmarks).
- `daysFromCivil` — [the inverse](https://www.benjoffe.com/fast-date#inverse) from the same file.
- `hourOfDay`, `minuteOfHour`, `secondOfMinute` — the ["fast time-of-day"](https://www.benjoffe.com/fast-time-of-day)
  V2.

The reference implementations are under the Boost Software License 1.0. ITU 1.16.0 shipped the BSL notice
alongside these ports; Ben confirmed in [#63](https://github.com/ethlo/itu/issues/63) that a port to another
language is not a copy of the Software and needs no notice, so from 1.16.1 the credit above is all that is
carried.

## Howard Hinnant

Both conversions followed [Hinnant's public domain date algorithms](https://howardhinnant.github.io/date_algorithms.html)
before 1.16.0, and the class still credits them for the shape of the problem.
