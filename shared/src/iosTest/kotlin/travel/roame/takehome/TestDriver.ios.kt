package travel.roame.takehome

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import travel.roame.takehome.db.FlightDatabase

internal actual fun createTestDriver(): SqlDriver = inMemoryDriver(FlightDatabase.Schema)
