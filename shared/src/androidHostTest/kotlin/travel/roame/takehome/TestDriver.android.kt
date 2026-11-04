package travel.roame.takehome

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import travel.roame.takehome.db.FlightDatabase

internal actual fun createTestDriver(): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FlightDatabase.Schema.create(it) }
