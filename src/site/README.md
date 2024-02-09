# Internet Time Utility

[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.time/itu.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.ethlo.time%22%20a%3A%22itu%22)
[![javadoc](https://javadoc.io/badge2/com.ethlo.time/itu/javadoc.svg)](https://javadoc.io/doc/com.ethlo.time/itu/latest/com/ethlo/time/ITU.html)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](../../LICENSE)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/598913bc1fe9405c82be73d9a4f105c8)](https://app.codacy.com/gh/ethlo/itu/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![codecov](https://codecov.io/gh/ethlo/itu/graph/badge.svg?token=V3H15LKC5V)](https://codecov.io/gh/ethlo/itu)

An extremely fast parser and formatter of specific ISO-8601 format date and date-times.

This project's goal is to  do one thing: Make it easy to
handle [RFC-3339 Timestamps](https://www.ietf.org/rfc/rfc3339.txt) and W3C [Date and Time Formats](https://www.w3.org/TR/NOTE-datetime) in Java.

## Features
* Very easy to use.
* [Well-documented API](https://javadoc.io/doc/com.ethlo.time/itu/latest/com/ethlo/time/ITU.html).
* Aim for 100% specification compliance.
* Aware of leap-seconds
* No dependencies, small jar.
* Apache 2 licensed, can be used in any project, even commercial.


## Performance

**TL;DR:** Typically **10x to 30x faster** than parsing and formatting with Java JDK classes.

The details and tests are available in a separate repository, [date-time-wars](https://github.com/ethlo/date-time-wars).

## Usage
Add dependency

```xml
<dependency>
  <groupId>com.ethlo.time</groupId>
  <artifactId>itu</artifactId>
  <version>${project.version}</version>
  <!-- If you want to use minified JAR -->  
  <classifier>small</classifier>
</dependency>
```

Below you find some samples of usage of this library. Please check out the [javadoc](https://javadoc.io/doc/com.ethlo.time/itu/latest/com/ethlo/time/ITU.html) for more details.

${src/test/java/samples/parsing}

${src/test/java/samples/formatting}

${src/test/java/samples/leapsecond}

## Q & A

### Why this little project?

There are an endless amount of APIs with non-standard date/time exchange, and the goal of this project is to make it a
no-brainer to do-the-right-thing(c).

### Why the performance focus?

Some projects use epoch time-stamps for date-time exchange, and from a performance perspective this *may* make sense
in some cases. With this project one can do-the-right-thing and maintain performance in date-time handling.

Importantly, this project is _not_ a premature optimization. In real-life scenarios there are examples of date-time parsing hindering optimal performance. The samples include data ingestion into databases and search engines, to importing/exporting data on less powerful devices, like cheaper Android devices.  

### What is wrong with epoch timestamps?

* It is not human-readable, so debugging and direct manipulation is harder
* Limited resolution and/or time-range available
* Unclear resolution and/or time-range

## What is RFC-3339?

[RFC-3339](https://www.ietf.org/rfc/rfc3339.txt) is a subset/profile defined by [W3C](https://www.w3.org/) of the
formats defined in [ISO-8601](http://www.iso.org/iso/home/standards/iso8601.htm), to simplify date and time exhange in
modern Internet protocols.

Typical formats include:

* `2017-12-27T23:45:32Z` - No fractional seconds, UTC/Zulu time
* `2017-12-27T23:45:32.999Z` - Millisecond fractions, UTC/Zulu time
* `2017-12-27T23:45:32.999999Z` - Microsecond fractions, UTC/Zulu time
* `2017-12-27T23:45:32.999999999Z` - Nanosecond fractions, UTC/Zulu time
* `2017-12-27T18:45:32-05:00` - No fractional seconds, EST time
* `2017-12-27T18:45:32.999-05:00` - Millisecond fractions, EST time
* `2017-12-27T18:45:32.999999-05:00` - Microsecond fractions, EST time
* `2017-12-27T18:45:32.999999999-05:00` - Nanosecond fractions, EST time

## What is W3C - Date and Time Formats

[Date and Time Formats](https://www.w3.org/TR/NOTE-datetime) is a _note_, meaning it is not endorsed, but it still
serves as a sane subset of ISO-8601, just like RFC-3339.

Typical formats include:

* `2017-12-27T23:45Z` - Minute resolution, UTC/Zulu time
* `2017-12-27` - Date only, no timezone (like someone's birthday)
* `2017-12` - Year and month only. Like an expiry date.

## Limitations

### Local offset

For the sake of avoiding data integrity issues, this library will not allow offset of `-00:00`. Such offset is described
in RFC3339 section 4.3., named "Unknown Local Offset Convention". Such offset is explicitly prohibited in ISO-8601 as
well.

> If the time in UTC is known, but the offset to local time is unknown, this can be represented with an offset of "-00:00". This differs semantically from an offset of "Z" or "+00:00", which imply that UTC is the preferred reference point for the specified time.

### Leap second parsing

Since Java's `java.time` classes do not support storing leap seconds, ITU will throw a `LeapSecondException` if one is
encountered to signal that this is a leap second. The exception can then be queried for the second-value. Storing such
values is not possible in a `java.time.OffsetDateTime`, the `60` is therefore abandoned and the date-time will use `59`
instead of `60`.