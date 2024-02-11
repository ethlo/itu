# Internet Time Utility

[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.time/itu.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.ethlo.time%22%20a%3A%22itu%22)
[![javadoc](https://javadoc.io/badge2/com.ethlo.time/itu/javadoc.svg)](https://javadoc.io/doc/com.ethlo.time/itu/latest/com/ethlo/time/ITU.html)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](LICENSE)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/598913bc1fe9405c82be73d9a4f105c8)](https://app.codacy.com/gh/ethlo/itu/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![codecov](https://codecov.io/gh/ethlo/itu/graph/badge.svg?token=V3H15LKC5V)](https://codecov.io/gh/ethlo/itu)

An extremely fast parser and formatter of ISO-8601 date-times. Handle
 [RFC-3339 Timestamps](https://www.ietf.org/rfc/rfc3339.txt) and W3C [Date and Time Formats](https://www.w3.org/TR/NOTE-datetime) with ease!

## Features
* Very easy to use.
* [Well-documented API](https://javadoc.io/doc/com.ethlo.time/itu/latest/com/ethlo/time/ITU.html).
* Aim for 100% specification compliance.
* Aware of leap-seconds
* No dependencies, small jar.
* Apache 2 licensed, can be used in any project, even commercial.
* Java 8+ compatible


## Performance

Typically, **10x to 30x faster** than parsing and formatting with Java JDK classes.

The details and tests are available in a separate repository, [date-time-wars](https://github.com/ethlo/date-time-wars).

## Usage
Add dependency

```xml
<dependency>
  <groupId>com.ethlo.time</groupId>
  <artifactId>itu</artifactId>
  <version>1.10.1</version>
  <!-- If you want to use minified JAR -->  
  <classifier>small</classifier>
</dependency>
```

Below you find some samples of usage of this library. Please check out the [javadoc](https://javadoc.io/doc/com.ethlo.time/itu/latest/com/ethlo/time/ITU.html) for more details.



## Parsing

This is a collection of usage examples for parsing.



#### parseRfc3339 <span style="font-weight: normal">[&raquo; source](src/test/java/samples/parsing/ITUParserSamples.java#L60C5-L69C6)</span>

The simplest and fastest way to parse an RFC-3339 timestamp by far!
```java
final String text = "2012-12-27T19:07:22.123456789-03:00";
final OffsetDateTime dateTime = ITU.parseDateTime(text);
assertThat(dateTime.toString()).isEqualTo(text);
```


#### parseLenient <span style="font-weight: normal">[&raquo; source](src/test/java/samples/parsing/ITUParserSamples.java#L71C5-L83C6)</span>

Parses a date-time with flexible granularity. Works for anything from a year to a timestamp with nanoseconds, with or without timezone offset.
```java
final String text = "2012-12-27T19:07:23.123";
final DateTime dateTime = ITU.parseLenient(text);
final String formatted = dateTime.toString();
assertThat(formatted).isEqualTo(text);
```


#### parseLenientWithCustomSeparators <span style="font-weight: normal">[&raquo; source](src/test/java/samples/parsing/ITUParserSamples.java#L85C5-L97C6)</span>

In case you encounter the need for a somewhat different time-separator or fraction separator
 you can use the `ParseConfig` to set up you preferred delimiters.
```java
final ParseConfig config = ParseConfig.DEFAULT
                .withDateTimeSeparators('T', '|')
                .withFractionSeparators('.', ',');
final DateTime result = ITU.parseLenient("1999-11-22|11:22:17,191", config);
assertThat(result.toString()).isEqualTo("1999-11-22T11:22:17.191");
```


#### parsePosition <span style="font-weight: normal">[&raquo; source](src/test/java/samples/parsing/ITUParserSamples.java#L99C5-L109C6)</span>

This allows you to track where to start reading. Note that the check for trailing junk is disabled when using `ParsePosition`.
```java
final ParsePosition pos = new ParsePosition(10);
final OffsetDateTime result = ITU.parseDateTime("some-data,1999-11-22T11:22:19+05:30,some-other-data", pos);
assertThat(result.toString()).isEqualTo("1999-11-22T11:22:19+05:30");
assertThat(pos.getIndex()).isEqualTo(35);
```


#### explicitGranularity <span style="font-weight: normal">[&raquo; source](src/test/java/samples/parsing/ITUParserSamples.java#L111C5-L134C6)</span>

This is useful if you need to handle different granularity with different logic or interpolation.
```java
final TemporalHandler<OffsetDateTime> handler = new TemporalHandler<OffsetDateTime>()
        {
            @Override
            public OffsetDateTime handle(final LocalDate localDate)
            {
                return localDate.atTime(OffsetTime.of(LocalTime.of(0, 0), ZoneOffset.UTC));
            }

            @Override
            public OffsetDateTime handle(final OffsetDateTime offsetDateTime)
            {
                return offsetDateTime;
            }
        };
final OffsetDateTime result = ITU.parse("2017-12-06", handler);
assertThat(result.toString()).isEqualTo("2017-12-06T00:00Z");
```


#### lenientTimestamp <span style="font-weight: normal">[&raquo; source](src/test/java/samples/parsing/ITUParserSamples.java#L136C5-L146C6)</span>

In some real world scenarios, it is useful to parse a best-effort timestamp. To ease usage, we can easily convert a raw `DateTime` instance into `Instant`.

 Note the limitations and the assumption of UTC time-zone, as mentioned in the javadoc.
```java
final Instant instant = ITU.parseLenient("2017-12-06").toInstant();
assertThat(instant.toString()).isEqualTo("2017-12-06T00:00:00Z");
```


#### parseCustomFormat <span style="font-weight: normal">[&raquo; source](src/test/java/samples/parsing/ITUParserSamples.java#L148C5-L170C6)</span>

In case the format is not supported directly, you can build your own parser.
```java
final DateTimeParser parser = DateTimeParsers.of(
                digits(DAY, 2),
                separators('-'),
                digits(MONTH, 2),
                separators('-'),
                digits(YEAR, 4),
                separators(' '),
                digits(HOUR, 2),
                digits(MINUTE, 2),
                digits(SECOND, 2),
                separators(','),
                fractions()
        );
final String text = "31-12-2000 235937,123456";
final DateTime result = parser.parse(text);
assertThat(result.toString()).isEqualTo("2000-12-31T23:59:37.123456");
```


#### parseUsingInterfaceRfc33939 <span style="font-weight: normal">[&raquo; source](src/test/java/samples/parsing/ITUParserSamples.java#L172C5-L182C6)</span>

`DateTimerParser` interface for RFC-3339.
```java
final DateTimeParser parser = DateTimeParsers.rfc3339();
final String text = "2000-12-31 23:59:37.123456";
final DateTime result = parser.parse(text);
assertThat(result.toString()).isEqualTo("2000-12-31T23:59:37.123456");
```


#### parseUsingInterfaceLocalTime <span style="font-weight: normal">[&raquo; source](src/test/java/samples/parsing/ITUParserSamples.java#L184C5-L194C6)</span>

`DateTimerParser` interface for local time.
```java
final DateTimeParser parser = DateTimeParsers.localTime();
final String text = "23:59:37.123456";
final LocalTime result = parser.parse(text).toLocalTime();
assertThat(result.toString()).isEqualTo(text);
```


#### parseUsingInterfaceLocalDate <span style="font-weight: normal">[&raquo; source](src/test/java/samples/parsing/ITUParserSamples.java#L196C5-L206C6)</span>

`DateTimerParser` interface for local date.
```java
final DateTimeParser parser = DateTimeParsers.localDate();
final String text = "2013-12-24";
final LocalDate result = parser.parse(text).toLocalDate();
assertThat(result.toString()).isEqualTo(text);
```





## Formatting

This is a collection of usage examples for formatting.



#### formatRfc3339WithUTC <span style="font-weight: normal">[&raquo; source](src/test/java/samples/formatting/ITUFormattingSamples.java#L43C5-L54C6)</span>

The simplest and fastest way to format an RFC-3339 timestamp by far!
```java
final OffsetDateTime input = OffsetDateTime.of(2012, 12, 27, 19, 7, 22, 123456789, ZoneOffset.ofHoursMinutes(-3, 0));
assertThat(ITU.formatUtcNano(input)).isEqualTo("2012-12-27T22:07:22.123456789Z");
assertThat(ITU.formatUtcMicro(input)).isEqualTo("2012-12-27T22:07:22.123456Z");
assertThat(ITU.formatUtcMilli(input)).isEqualTo("2012-12-27T22:07:22.123Z");
assertThat(ITU.formatUtc(input)).isEqualTo("2012-12-27T22:07:22Z");
```


#### formatWithDateTime <span style="font-weight: normal">[&raquo; source](src/test/java/samples/formatting/ITUFormattingSamples.java#L56C5-L65C6)</span>

Format with `DateTime`.
```java
final DateTime input = DateTime.of(2020, 11, 27, 12, 39, 19, null);
assertThat(input.toString(Field.MINUTE)).isEqualTo("2020-11-27T12:39");
assertThat(input.toString(Field.SECOND)).isEqualTo("2020-11-27T12:39:19");
```





## Leap-second handling



#### parseLeapSecond <span style="font-weight: normal">[&raquo; source](src/test/java/samples/leapsecond/ITULeapSecondSamples.java#L40C5-L57C6)</span>

Parse a valid leap-second (i.e. it is on a date that would allow for it, and it is also in the list of known actual leap-seconds).
```java
try
{
    ITU.parseDateTime("1990-12-31T15:59:60-08:00");
}
catch (LeapSecondException exc)
{
    // The following helper methods are available let you decide how to progress
    assertThat(exc.getSecondsInMinute()).isEqualTo(60);
    assertThat(exc.getNearestDateTime()).isEqualTo(OffsetDateTime.of(1990, 12, 31, 16, 0, 0, 0, ZoneOffset.ofHours(-8)));
    assertThat(exc.isVerifiedValidLeapYearMonth()).isTrue();
}
```



## Q & A

### Why this little project?

There are an endless amount of APIs with non-standard date/time exchange, and the goal of this project is to make it a
breeze to do the right thing!

### Why the performance focus?

Some projects use epoch time-stamps for date-time exchange, and from a performance perspective this *may* make sense
in some cases. With this project one can do-the-right-thing and maintain performance in date-time handling.

Importantly, this project is _not_ a premature optimization. In real-life scenarios there are examples of date-time parsing hindering optimal performance. The samples include data ingestion into databases and search engines, to importing/exporting data on less powerful devices, like cheaper Android devices.  

### What is wrong with epoch timestamps?

* It is not human-readable, so debugging and direct manipulation is harder
* Limited resolution and/or time-range available
* Unclear resolution and/or time-range

### What is RFC-3339?

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

### What is W3C - Date and Time Formats

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
