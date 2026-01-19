package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object AuditLogTable : Table("audit_log") {
    val id = varchar("id", 36)
    val recordId = varchar("record_id", 36)
    val recordType = varchar("record_type", 50)
    val recordName = text("record_name")
    val action = varchar("action", 20)
    val userId = varchar("user_id", 36)
    val userName = text("user_name")
    val timestamp = long("timestamp")

    override val primaryKey = PrimaryKey(id)
}
