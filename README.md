# Internet Time Utilty
This projects aims to do one thing and to do it right: 
*Make it easy to handle [Date and Time on the Internet: Timestamps](https://www.ietf.org/rfc/rfc3339.txt) in Java.*

## Features
* No external dependencies, minimalistic JAR
* MIT licensed
* Configurable validator, formatter and parser within the boundaries of the specification
* Correct time-zone handling
* High test coverage

## Performance
Implementation | Parse | Format 
---------------|---------|-----------
StdJdkInternetDateTimeUtil |  692 650 | 1 960 673
FastInternetDateTimeUtil   | 10 330 331    | 8 952 794
Difference | 14.91x | 4.56x
* Your milage may vary. The tests are included in this repository.

## Build status
[![Build Status](https://travis-ci.org/ethlo/itu.png?branch=master)](https://travis-ci.org/ethlo/itu)
[![Coverage Status](https://coveralls.io/repos/github/ethlo/itu/badge.svg?branch=master)](https://coveralls.io/github/ethlo/itu?branch=master)

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
