package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Migration to add party_leader_id column to user table.
 * When set, the user is following that player (party leader).
 * Null means the user is solo or is a leader themselves.
 */
class V010_AddPartyFields : Migration(10, "AddPartyFields") {
    override fun up() {
        transaction {
            exec("ALTER TABLE user ADD COLUMN party_leader_id TEXT DEFAULT NULL")
        }
    }

    override fun down() {
        transaction {
            throw UnsupportedOperationException("Cannot drop column in SQLite")
        }
    }
}
