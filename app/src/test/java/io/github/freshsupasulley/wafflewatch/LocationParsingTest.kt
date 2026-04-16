package io.github.freshsupasulley.wafflewatch

import io.github.freshsupasulley.wafflewatch.model.LocationStatus
import io.github.freshsupasulley.wafflewatch.model.parseLocations
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationParsingTest {

    @Test
    fun `parseLocations handles valid JSON`() {
        val json = """
            {
                "timestamp": 1625097600000,
                "locations": [
                    {
                        "code": "123",
                        "status": "open",
                        "addr": "123 Waffle St",
                        "lat": 33.7490,
                        "long": -84.3880,
                        "bizHours": "Open 24/7"
                    }
                ]
            }
        """.trimIndent()

        val response = parseLocations(json)

        assertEquals(1625097600000L, response.timestamp)
        assertEquals(1, response.locations.size)
        val loc = response.locations[0]
        assertEquals("123", loc.locationId)
        assertEquals(LocationStatus.GREEN, loc.status)
        assertEquals("123 Waffle St", loc.address)
    }

    @Test
    fun `parseLocations handles empty locations list`() {
        val json = """{"timestamp": 123, "locations": []}"""
        val response = parseLocations(json)
        assertEquals(0, response.locations.size)
    }

    @Test
    fun `parseLocations handles closed status`() {
        val json = """
            {
                "timestamp": 123,
                "locations": [
                    { "code": "1", "status": "closed" }
                ]
            }
        """.trimIndent()
        val response = parseLocations(json)
        assertEquals(LocationStatus.RED, response.locations[0].status)
    }
}
