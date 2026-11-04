package travel.roame.takehome

import app.cash.sqldelight.db.SqlDriver

/** A fresh in-memory database with the schema created. */
internal expect fun createTestDriver(): SqlDriver
