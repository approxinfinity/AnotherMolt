import at.favre.lib.crypto.bcrypt.BCrypt

fun main() {
    val hash = BCrypt.withDefaults().hashToString(12, "password1".toCharArray())
    println("HASH_OUTPUT: $hash")
}
