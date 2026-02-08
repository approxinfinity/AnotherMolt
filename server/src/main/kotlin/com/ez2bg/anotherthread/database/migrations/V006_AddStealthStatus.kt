package com.ez2bg.anotherthread.database.migrations

import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Migration to add stealth status fields to user table.
 * - is_hidden: Boolean for when character is hiding in place
 * - is_sneaking: Boolean for when character is moving stealthily
 */
class V006_AddStealthStatus : Migration(version = 6, name = "AddStealthStatus") {
    override fun up() {
        transaction {
            exec("ALTER TABLE user ADD COLUMN is_hidden INTEGER DEFAULT 0")
            exec("ALTER TABLE user ADD COLUMN is_sneaking INTEGER DEFAULT 0")
        }
    }
}
