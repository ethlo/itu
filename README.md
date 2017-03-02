# Internet Time Utility
[![Build Status](https://travis-ci.org/ethlo/itu.svg?branch=master)](https://travis-ci.org/ethlo/itu)
[![Coverage Status](https://coveralls.io/repos/github/ethlo/itu/badge.svg?1)](https://coveralls.io/github/ethlo/itu?1)
[![Coverage Status](https://img.shields.io/badge/greencode-✓-green.svg?style=flat)](http://greencode.io/p/itu)

An extremely fast parser and formatter of standardized date-times.

> Date and time formats cause a lot of confusion and interoperability problems on the Internet.
This document addresses many of the problems encountered and makes recommendations to improve consistency and interoperability when representing and using date and time in Internet protocols.

This project's goal it to do one thing and to do it right; make it easy to handle [Date and Time on the Internet: Timestamps](https://www.ietf.org/rfc/rfc3339.txt) in Java.

## Features
* No external dependencies, minimalistic JAR
* Apache license
* Configurable validator, formatter and parser within the boundaries of the specification
* Correct time-zone handling
* High test coverage
* Very high performance

## Performance
Implementation | Parse | Format 
---------------|---------:|-----------:
java.util (Java 7) * |  742 850 parse/sec | 1 837 811 format/sec
java.time (Java 8) |  545 333 parse/sec | 2 101 431 format/sec
Apache FastDateUtils * |  1 076 995 parse/sec | 1 989 163 format/sec
Internet Time Utililties   | 15 569 458 parse/sec    | 12 726 932 format/sec

* Single hard-coded format. Lenient parsing would require multiple patterns to be tested (4-6).

Your milage may vary. The tests are included in this repository.

## Example use
```java
final FastInternetDateTimeUtil itu = new FastInternetDateTimeUtil();
final String s = "2012-12-27T19:07:22.123456789-03:00";

// Parse a string
final OffsetDateTime dateTime = itu.parse(s);

// Format with no fraction digits
final String formatted = itu.formatUtc(dateTime); // 2012-12-27T22:07:22Z

// Format with microsecond precision
final String formattedMicro = itu.formatUtcMicro(dateTime); // 2012-12-27T22:07:22.123457Z
```
## Q & A

*Why this little project?*

There are an endless amount of APIs with non-standard date/time exchange, and the goal of this project is to make it a no-brainer to do-the-right-thing(c).

*Why the performance optimized version?*

Some projects use epoch time-stamps for date-time exchange, and from a performance perspective this *may* make sense in *extreme* cases. With this project one can do-the-right-thing and maintain performance in date-time handling.

*What is wrong with epoch timestamps?*

* It is not human-readable, so debugging and direct manipulation is harder
* Limited resolution and/or time-range available
* Unclear resolution and/or time-range

## Maven repository
http://ethlo.com/maven

## What is RFC-3339?
[RFC-3339](https://www.ietf.org/rfc/rfc3339.txt) is a subset/profile defined by [W3C](https://www.w3.org/) of the formats defined in [ISO-8601](http://www.iso.org/iso/home/standards/iso8601.htm), to simplify date and time exhange in modern Internet protocols. 

Typical formats include: 
* `2017-12-27T23:45:32Z` (No fractional seconds, UTC/Zulu time)
* `2017-12-27T23:45:32.999Z` (Millisecond fractions, UTC/Zulu time)
* `2017-12-27T23:45:32.999999Z`(microsecond fractions, UTC/Zulu time)
* `2017-12-27T23:45:32.999999999Z` (nanosecond fractions, UTC/Zulu time)
* `2017-12-27T18:45:32-05:00` (No fractional seconds, EST time)
* `2017-12-27T18:45:32.999-05:00` (Millisecond fractions, EST time)
* `2017-12-27T18:45:32.999999-05:00`(microsecond fractions, EST time)
* `2017-12-27T18:45:32.999999999-05:00` (nanosecond fractions, EST time)

## Limitations

For the sake of avoiding data integrity issues, this library will not allow offset of `-00:00`. 
Such offset is described in RFC3339 section 4.3., named "Unknown Local Offset Convention". Such offset is explicitly prohibited in ISO-8601 as well.

>   If the time in UTC is known, but the offset to local time is unknown,
   this can be represented with an offset of "-00:00".  This differs
   semantically from an offset of "Z" or "+00:00", which imply that UTC
   is the preferred reference point for the specified time.
