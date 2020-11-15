package com.philite.readertest

import android.text.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.regex.Pattern

class CardReader(
    private val scope: CoroutineScope,
    private val callback: (CardReaderStates) -> Unit
) : TextWatcher {
    companion object {
        // %B = 2, CardNumber = 16, ^ = 1, REGEX = 39 -> 58
        private const val LEAST_REGEX_CHAR = 58
        private const val CARD_NUMBER_REGEX = ";([0-9]{16})=([0-9]{20})\\?"
        private const val WAIT_LAST_TYPE = 350L

        private const val NONE = 0
        private const val EN = 1
        private const val TH = 2

        private const val ENG_ALPHABET_REGEX =
            "[A-Za-z0-9~`!@#$%^&*()_+=-\\|\\{\\}\\[\\];:\"\'?>.<,]"
    }

    private var acceptCharFlag = false
    private var cardNumberStringBuilder = StringBuilder()
    private val cardNumberPattern = Pattern.compile(CARD_NUMBER_REGEX)
    private val engAlphabetPattern = Pattern.compile(ENG_ALPHABET_REGEX)
    private var language = NONE
    private var reading = false
    private var debounceJob: Job? = null
    // Remove
    private var readTime = 0L
    private var lastReadTime = 0L

    override fun beforeTextChanged(
        s: CharSequence,
        start: Int,
        count: Int,
        after: Int
    ) {
        acceptCharFlag = false
    }

    override fun onTextChanged(
        s: CharSequence,
        start: Int,
        before: Int,
        count: Int
    ) {
        if (count == 2) {
            val tempCardNumber = s.subSequence(start, start + count).toString()
            Timber.d("cardNumber onTextChanged = %s", tempCardNumber)

            if (tempCardNumber.contains("ฐ")) {
                Timber.d("tempCardNumber contains ฐ replace it with empty string")
                acceptCharFlag = true
                cardNumberStringBuilder.append(tempCardNumber.replace("ฐ", ""))
            }
        }
        // debug
        else {
            Timber.d("onTextChanged count != 2")
        }
    }

    override fun afterTextChanged(s: Editable) {
        if (acceptCharFlag) {

            acceptCharFlag = false

            callback(TextChanged)

            val cardNumber = cardNumberStringBuilder.toString()
            Timber.d("cardNumber from StringBuilder = %s", cardNumber)

            // Remove
            lastReadTime = System.currentTimeMillis()

            if (cardNumber.length >= LEAST_REGEX_CHAR) {
                val matcher = cardNumberPattern.matcher(cardNumber)

                if (matcher.find()) {
                    debounceJob?.cancel()

                    Timber.d("afterTextChanged matcher.find() found")
                    Timber.d("Card length = %d", cardNumber.length)

                    // Remove this line
                    Timber.d("Total read time before match = %d", (System.currentTimeMillis() - readTime))

                    callback(ReadCompleted(cardNumber))
                }
            }
        }
    }

    private fun debounceOnStopTyping(doFunction: () -> Unit) {
        debounceJob?.cancel()
        // Remove this line
        Timber.d("Each character read time = %d", (System.currentTimeMillis() - lastReadTime))

        debounceJob = scope.launch {
            delay(WAIT_LAST_TYPE)
            doFunction()
        }
    }

    fun resetCardBuilder() {
        cardNumberStringBuilder = StringBuilder()
    }

    inner class CardFilter : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            spanned: Spanned?,
            dStart: Int,
            dEnd: Int
        ): CharSequence {

            if (!reading) {
                // Remove
                readTime = System.currentTimeMillis()

                reading = true
                callback(StartReading)
            }

            if (source is SpannableStringBuilder) {
                for (i in end - 1 downTo start) {
                    val currentChar = source[i]
                    if (!Character.isLetterOrDigit(currentChar) && !Character.isSpaceChar(
                            currentChar
                        )
                    ) {
                        source.delete(i, i + 1)
                    }
                }
                return source
            } else {
                // Always start = 0, end = 1
                Timber.d("CardReadingFilter = %s, start = %d, end = %d", "Else case", start, end)
                val filteredStringBuilder = StringBuilder()
                for (index in start until end) {
                    val currentChar = source?.get(index)
                    Timber.d("currentChar in filter = %s", currentChar.toString())

                    if (language == NONE) {
                        language = if (engAlphabetPattern.matcher(currentChar.toString()).find()) {
                            EN
                        } else {
                            TH
                        }
                        Timber.d(
                            "filter else case = %s", "language == NONE, check language = $language"
                        )
                    }

                    when (language) {
                        EN -> {
                            Timber.d("filter else case = %s", "language == 1, append ฐ")
                            filteredStringBuilder.append(currentChar).append('ฐ')
                        }
                        TH -> {
                            val engChar = currentChar?.let { KeyboardThaiToEngMapper.toEng(it) }

                            Timber.d(
                                "filter else case = %s", "language == 2 (TH), engChar is $engChar"
                            )

                            if (engChar != '\u0000') {
                                filteredStringBuilder.append(engChar).append('ฐ')
                                Timber.d(
                                    "filter else case = %s", "engChar != blank, append ฐ"
                                )
                            }
                        }
                    }
                }

                debounceOnStopTyping {
                    Timber.d("Stopped Typing")
                    reading = false
                    language = NONE
                    callback(ReadFail)
                }

                if (filteredStringBuilder.isEmpty()) {
                    Timber.d("filter else case = %s", "stringBuilder = empty")
                    return ""
                }
                Timber.d("filter end of else = %s", filteredStringBuilder.toString())
                return filteredStringBuilder.toString()
            }
        }
    }
}