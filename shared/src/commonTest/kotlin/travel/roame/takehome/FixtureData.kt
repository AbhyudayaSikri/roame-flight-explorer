package travel.roame.takehome

/** The real fixtures/flights.json, embedded at build time by :shared:generateTestFixture. */
internal val fixtureFlights: List<Flight> by lazy { decodeFlights(FIXTURE_JSON) }

internal val fixtureBounds: DataBounds by lazy { checkNotNull(DataBounds.of(fixtureFlights)) }

internal fun filter(query: FlightQuery): List<Flight> = filterFlights(fixtureFlights, query, fixtureBounds.airportCodes)

internal fun date(iso: String): CalendarDate = CalendarDate.parse(iso)
