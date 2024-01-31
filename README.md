# Internet Time Utility

[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.time/itu.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.ethlo.time%22%20a%3A%22itu%22)
[![javadoc](https://javadoc.io/badge2/com.ethlo.time/itu/javadoc.svg)](https://javadoc.io/doc/com.ethlo.time/itu/latest/com/ethlo/time/ITU.html)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](LICENSE)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/598913bc1fe9405c82be73d9a4f105c8)](https://app.codacy.com/gh/ethlo/itu/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![codecov](https://codecov.io/gh/ethlo/itu/graph/badge.svg?token=V3H15LKC5V)](https://codecov.io/gh/ethlo/itu)

An extremely fast parser and formatter of specific ISO-8601 format date and date-times.

This project's goal is to  do one thing: Make it easy to
handle [RFC-3339 Timestamps](https://www.ietf.org/rfc/rfc3339.txt) and W3C [Date and Time Formats](https://www.w3.org/TR/NOTE-datetime) in Java.

## Features
* Very easy to use.
* Aim for 100% specification compliance.
* No dependencies, minimal JAR size.
* Apache 2 licensed, can be used in any project, even commercial.
* Handling leap-seconds (if you want to).
* [Well-documented API](https://javadoc.io/doc/com.ethlo.time/itu/latest/com/ethlo/time/ITU.html).
* Works 

## Performance

**TL;DR:** Typically **10x to 30x faster** than parsing and formatting with Java JDK classes.

The details and tests are available in a separate repository, [date-time-wars](https://github.com/ethlo/date-time-wars).

## Usage
Add dependency

```xml
<dependency>
  <groupId>com.ethlo.time</groupId>
  <artifactId>itu</artifactId>
  <version>1.9.0</version>
  <!-- If you want to use minified JAR -->  
  <classifier>small</classifier>
</dependency>
```

Below you find some samples of usage of this library. Please check out the [javadoc](https://javadoc.io/doc/com.ethlo.time/itu/latest/com/ethlo/time/ITU.html) for more details.

### Parsing

#### RFC-3339 with offset

```java
final OffsetDateTime dateTime = ITU.parseDateTime("2012-12-27T19:07:22.123456789-03:00");
```

#### Parse leniently
```java
final DateTime dateTime = ITU.parseLenient("2012-12-27T19:07.23.123");
final String formatted = dateTime.toString(); // 2012-12-27T19:07.23.123 (Note the tracking of resolution)
```

#### Parse leniently, configurable separators
```java
final ParseConfig config = ParseConfig.DEFAULT
                .withFractionSeparators('.', ',')
                .withDateTimeSeparators('T', '|');
ITU.parseLenient("1999-11-22|11:22:17,191", config);
```

#### Parse with ParsePosition
This allows you to track where to start reading. Note that the check for trailing junk is disabled when using ParsePosition.
```java
final ParsePosition pos = new ParsePosition(10);
ITU.parseDateTime("some-data,1999-11-22T11:22:00+05:30,some-other-data", pos);
```

#### Handling different levels of granularity explicitly
This is useful if you need to handle different granularity with different logic or interpolation.
```java
ITU.parse("2017-12-06", new TemporalHandler<>() {
    @Override
    public OffsetDateTime handle(final LocalDate localDate) {
        return localDate.atTime(OffsetTime.of(LocalTime.of(0, 0), ZoneOffset.UTC));
    }

    @Override
    public OffsetDateTime handle(final OffsetDateTime offsetDateTime) {
        return offsetDateTime;
    }
});
```
#### Parsing leniently to a timestamp
In some real world scenarios, it is useful to parse a best-effort timestamp. To ease usage, we can easily convert a raw `com.ethlo.time.DateTime` instance into `java.time.Instant`. Note the limitations and the assumption of UTC time-zone, as mentioned in the javadoc.

We can use `ITU.parseLenient()` with `DateTime.toInstant()` like this:

```java
final Instant instant = ITU.parseLenient("2017-12-06").toInstant();
```

### Formatting

#### Format with seconds (no fraction digits)
```java
final String formatted = ITU.formatUtc(dateTime); // 2012-12-27T22:07:22Z
``` 

#### Format with microsecond precision
```java
final String formattedMicro = ITU.formatUtcMicro(dateTime); // 2012-12-27T22:07:22.123457Z
```

### Handle leap-seconds
```java
try {
    final OffsetDateTime dateTime = ITU.parseDateTime("1990-12-31T15:59:60-08:00");
} catch (LeapSecondException exc) {
    // The following helper methods are available let you decide how to progress
    exc.getSecondsInMinute(); // 60
    exc.getNearestDateTime(); // 1991-01-01T00:00:00Z
    exc.isVerifiedValidLeapYearMonth(); // true
}
```

### Validate

#### Validate as RFC-3339 date-time
```java
ITU.isValid("2017-12-06"); // false
```

#### Validate as specified type
```java
ITU.isValid("2017-12-06", TemporalType.LOCAL_DATE_TIME); // true
```


## Q & A

### Why this little project?

There are an endless amount of APIs with non-standard date/time exchange, and the goal of this project is to make it a
no-brainer to do-the-right-thing(c).

### Why the performance optimized version?

Some projects use epoch time-stamps for date-time exchange, and from a performance perspective this *may* make sense
in *some* cases. With this project one can do-the-right-thing and maintain performance in date-time handling.

This project is _not_ a premature optimization! In real-life scenarios there are examples of date-time parsing hindering optimal performance. The samples include data ingestion into databases and search engines, to importing/exporting data on less powerful devices, like cheaper Android devices.  

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
