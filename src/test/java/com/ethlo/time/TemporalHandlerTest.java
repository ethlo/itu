package com.ethlo.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TemporalHandlerTest
{
    @Test
    void handle()
    {
        final TemporalHandler<TemporalType> handler = new TemporalHandler<TemporalType>()
        {
        };

        Assertions.assertThrows(UnsupportedOperationException.class, () -> handler.handle(LocalDate.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> handler.handle(LocalDateTime.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> handler.handle(OffsetDateTime.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> handler.handle(Year.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> handler.handle(YearMonth.now()));
    }

    @Test
    void handleConsumer()
    {
        final TemporalConsumer consumer = new TemporalConsumer()
        {
        };

        Assertions.assertThrows(UnsupportedOperationException.class, () -> consumer.handle(LocalDate.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> consumer.handle(LocalDateTime.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> consumer.handle(OffsetDateTime.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> consumer.handle(Year.now()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> consumer.handle(YearMonth.now()));
    }
}