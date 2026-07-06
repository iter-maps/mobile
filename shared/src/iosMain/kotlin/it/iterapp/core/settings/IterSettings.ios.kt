package it.iterapp.core.settings

/** The iOS simulator shares the host loopback. */
actual fun defaultGatewayOrigin(): String = "http://localhost:8090"
