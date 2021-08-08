package com.example.androidkotlincoroutines

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

object KotlinFlow {
    fun tryFlow() {
        val flow = flow<String> {
            for( i in 1..10) {
                emit("Hello world")
                delay(1000L) // delay each emit 1 second
            }
        }

        val flow2 = flow<String> {
            for( i in 1..10) {
                emit("Hallo Dunia")
                delay(500L) // delay each emit 1 second
            }
        }

        val flow3 = flow<String> {
            for( i in 1..10) {
                emit("Hallo Dunia Buffered")
                delay(500L) // delay each emit 1 second
            }
        }

        GlobalScope.launch {
            flow.collect {
                println(it)
            }
            flow2.collect {
                println(it)
                delay(2000L) // somekind of simple backpressure, which also able to delay the
                                       // producer which emits value, because the consumer and producer have common coroutine
            }
            flow3.buffer().collect {
                // buffer() has a role to buffer the downstream (consumer)
                // It runs on different coroutine with producer
                println(it)
                delay(2000L)
            }
        }
    }
}