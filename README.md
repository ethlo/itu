# Internet Time Utilty
This projects aims to do one thing and to do it right: 
*Make it easy to handle [Date and Time on the Internet: Timestamps](https://www.ietf.org/rfc/rfc3339.txt) in Java.*

## Features
* No external dependencies, minimalistic JAR
* MIT licensed
* Configurable validator, formatter and parser within the boundaries of the specification
* Correct time-zone handling
* High test coverage
* High performance

## Build status
[![Build Status](https://travis-ci.org/ethlo/itu.png?branch=master)](https://travis-ci.org/ethlo/itu)

## Maven repository
http://ethlo.com/maven

## What is RFC-3339?
This is a subset/profile defined by [W3C](https://www.w3.org/) of the formats defined in [ISO-8601(http://www.iso.org/iso/home/standards/iso8601.htm), to simplify date and time exhange in modern Internet protocols. 

Typical formats include: 
* `2017-12-27T23:45:32Z` (No fractional seconds, UTC/Zulu time)
* `2017-12-27T23:45:32.999Z` (Millisecond fractions, UTC/Zulu time)
* `2017-12-27T23:45:32.999999Z`(microsecond fractions, UTC/Zulu time)
* `2017-12-27T23:45:32.999999999Z` (nanosecond fractions, UTC/Zulu time)
* `2017-12-27T18:45:32-0500` (No fractional seconds, EST time)
* `2017-12-27T18:45:32.999Z` (Millisecond fractions, EST time)
* `2017-12-27T18:45:32.999999Z`(microsecond fractions, EST time)
* `2017-12-27T18:45:32.999999999Z` (nanosecond fractions, EST time)
