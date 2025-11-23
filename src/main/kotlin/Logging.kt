package com.sev4ikwasd

import io.ktor.server.application.*
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import org.slf4j.LoggerFactory

fun Application.configureLogging() {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

    val encoder = PatternLayoutEncoder().apply {
        this.context = loggerContext
        pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
        start()
    }

    val consoleAppender = ConsoleAppender<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
        this.context = loggerContext
        name = "console"
        this.encoder = encoder
        start()
    }

    val fileAppender = FileAppender<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
        this.context = loggerContext
        name = "file"
        file = "logs/app.log"
        this.encoder = encoder
        start()
    }

    val asyncAppender = AsyncAppender().apply {
        this.context = loggerContext
        name = "async"
        queueSize = 512
        discardingThreshold = 0
        maxFlushTime = 1000
        addAppender(consoleAppender)
        addAppender(fileAppender)
        start()
    }

    rootLogger.addAppender(asyncAppender)
    rootLogger.level = ch.qos.logback.classic.Level.INFO
    rootLogger.isAdditive = false
}