package com.philite.readertest

sealed class CardReaderStates

object StartReading: CardReaderStates()
object TextChanged: CardReaderStates()
data class ReadCompleted(
    val card: String
): CardReaderStates()
object ReadFail: CardReaderStates()
object WaitForReading: CardReaderStates()
