package com.app.administradorfarmadon.compartido

import java.security.MessageDigest

fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256").digest(toByteArray())
        .joinToString("") { "%02x".format(it) }
