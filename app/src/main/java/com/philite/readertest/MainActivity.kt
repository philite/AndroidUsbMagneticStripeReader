package com.philite.readertest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.philite.readertest.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber
import java.util.regex.Pattern


class MainActivity : AppCompatActivity() {
    companion object {
        private const val USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        private const val USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"

        private const val ENG_ALPHABET_REGEX =
            "[A-Za-z0-9~`!@#$%^&*()_+=-\\|\\{\\}\\[\\];:\"\'?>.<,]"

        // 0 to 9 and * and #
        private const val NUMBER_START = 7
        private const val NUMBER_END = 18

        // NOTE: CASE SENSITIVE
        // a to z and , and .
        private const val ALPHABET_START = 29
        private const val ALPHABET_END = 56

        // Symbols = `-=[]\;'/@
        private const val SYMBOL_START = 68
        private const val SYMBOL_END = 77

        private const val HANDLED = true
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val usbReceiver = UsbIntentReceiver()
    private val filter = IntentFilter().apply {
        addAction(USB_ATTACHED)
        addAction(USB_DETACHED)
    }

    private lateinit var binding: ActivityMainBinding

    //    private lateinit var cardReader: CardReader
//    private lateinit var cardFilter: CardReader.CardFilter
    private val engAlphabetPattern = Pattern.compile(ENG_ALPHABET_REGEX)
    private val mapper = ShiftCharacterMapper()
    private var cardNumberStringBuilder = StringBuilder()
    private var isShiftPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.hideKeyboard()

//        binding.cardEditText.run {
//            setRawInputType(InputType.TYPE_CLASS_TEXT)
//            setTextIsSelectable(true)
//            setOnKeyListener(object : View.OnKeyListener {
//                override fun onKey(p0: View?, keyCode: Int, event: KeyEvent?): Boolean {
//                    Timber.d("onKey keyCode = %d, event = %s", keyCode, event.toString())
//                    Timber.d("keyboard is virtual = %s", event?.device?.isVirtual)
//                    return true
//                }
//            })
//        }

        // val ic: InputConnection = binding.cardEditText.onCreateInputConnection(EditorInfo())
        //binding.keyboard.setInputConnection(ic)


        // This will make no Thai typing
        //binding.cardEditText.inputType = 0

//        cardReader = CardReader(coroutineScope) {
//            renderUi(it)
//        }
//        cardFilter = cardReader.CardFilter()
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        Timber.d(
            "onKeyLongPress keyCode = %d, event = %s, unicodeNumber = %d, unicodeChar = %c, displayLabel = %c, flags = %d",
            keyCode,
            event.toString(),
            event.unicodeChar,
            event.unicodeChar.toChar(),
            event.displayLabel,
            event.flags,
        )
        return !HANDLED
    }

    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent): Boolean {
        Timber.d(
            "onKeyMultiple keyCode = %d, event = %s, unicodeNumber = %d, unicodeChar = %c, displayLabel = %c, flags = %d",
            keyCode,
            event.toString(),
            event.unicodeChar,
            event.unicodeChar.toChar(),
            event.displayLabel,
            event.flags,
        )
        Timber.d("RepeatCount = %d", repeatCount)
        return !HANDLED
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
        Timber.d(
            "onKeyShortcut keyCode = %d, event = %s, unicodeNumber = %d, unicodeChar = %c, displayLabel = %c, flags = %d",
            keyCode,
            event.toString(),
            event.unicodeChar,
            event.unicodeChar.toChar(),
            event.displayLabel,
            event.flags,
        )
        return !HANDLED
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        Timber.d(
            "onKeyUp keyCode = %d, event = %s, unicodeNumber = %d, unicodeChar = %c, displayLabel = %c, flags = %d",
            keyCode,
            event.toString(),
            event.unicodeChar,
            event.unicodeChar.toChar(),
            event.displayLabel,
            event.flags,
        )
        Timber.d(
            "onKeyUp isFunctionPressed = %s, isCtrlPressed = %s, isMetaPressed = %s, isSymPressed = %s, metaState = %d, modifiers = %d, keyCharacterMap = %s",
            event.isFunctionPressed,
            event.isCtrlPressed,
            event.isMetaPressed,
            event.isSymPressed,
            event.metaState,
            event.modifiers,
            event.keyCharacterMap.toString()
        )
        Timber.d("characterMap = %s", event.keyCharacterMap.get(event.keyCode, event.metaState).toString())
        //return false

        Timber.d("StringBuilder = %s", cardNumberStringBuilder.toString())

        if (!event.device.isVirtual) {
//            if (event.isShiftPressed) {
//                Timber.d("Shift Pressed")
//            }
            //return false

            if (event.keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || event.keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
                Timber.d("Shift Pressed")
                isShiftPressed = true
                return true
            } else {
                when {
                    isShiftPressed -> {
                        Timber.d("Shift Pressed Before")
                        mapper.map(event.unicodeChar.toChar())?.let {
                            Timber.d("Appending %c", it)
                            cardNumberStringBuilder.append(it)
                            isShiftPressed = false
                            return true
                        } ?: run {
                            Timber.d("Unable to map")
                            return false
                        }
                    }
                    // TOD(): Add debounce timer
                    event.keyCode == KeyEvent.KEYCODE_ENTER -> {
                        Timber.d(cardNumberStringBuilder.toString())
                        return true
                    }
                    isAcceptableCharacter(event.keyCode) -> {
                        cardNumberStringBuilder.append(event.unicodeChar.toChar())
                        return true
                    }
                    else -> {
                        return false
                    }
                    // TOD(): Fix this
                    // Can't use physical back button for some reason

                    // TOD(): Check Memory usage of ShiftCharacterMapper
                }
            }
        } else {
            return false
        }
    }

    private fun isAcceptableCharacter(charInt: Int): Boolean {
        return if (charInt >= NUMBER_START || charInt <= NUMBER_END) {
            true
        } else if (charInt >= ALPHABET_START || charInt <= ALPHABET_END) {
            true
        } else if (charInt >= SYMBOL_START || charInt <= SYMBOL_END) {
            true
        } else if (charInt == KeyEvent.KEYCODE_SPACE) {
            true
        } else charInt == KeyEvent.KEYCODE_PLUS
    }


//    private fun renderUi(state: CardReaderStates) {
//        Timber.d("Ui State = %s", state.toString())
//        when (state) {
//            is StartReading -> {
//                binding.stateTextView.text = "Reading"
//            }
//            is ReadFail -> {
//                binding.stateTextView.text = "Credit card is invalid, please try again"
//                binding.cardEditText.removeReader(cardReader)
//                binding.cardEditText.initReader(cardReader, cardFilter)
//            }
//            is WaitForReading -> binding.stateTextView.text = "Swipe card to pay"
//            is TextChanged -> binding.cardEditText.textChanged(cardReader, cardFilter)
//            is ReadCompleted -> {
//                binding.cardEditText.removeReader(cardReader)
//                binding.stateTextView.text = state.card
//            }
//        }
//    }

    //    private fun EditText.textChanged(textWatcher: CardReader, cardFilter: CardReader.CardFilter) {
//        Timber.d("TextChanged")
//        this.removeTextChangedListener(textWatcher)
//        this.filters = arrayOf()
//        this.addTextChangedListener(textWatcher)
//        this.filters = arrayOf(cardFilter)
//    }
//
//    private fun EditText.initReader(textWatcher: CardReader, cardFilter: CardReader.CardFilter) {
//        Timber.d("InitReader")
//        textWatcher.resetCardBuilder()
//        this.isFocusableInTouchMode = true
//        this.setText("")
//        this.requestFocus()
//        this.requestFocusFromTouch()
//        this.hideKeyboard()
//        this.addTextChangedListener(textWatcher)
//        this.filters = arrayOf(cardFilter)
//    }
//
//    private fun EditText.removeReader(textWatcher: CardReader) {
//        Timber.d("RemoveReader")
//        this.removeTextChangedListener(textWatcher)
//        this.filters = arrayOf()
//        this.clearFocus()
//        this.hideKeyboard()
//    }
//
    private fun usbAlreadyAttached(): Boolean {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.deviceList.size > 0
    }

    //
    private fun View.hideKeyboard() {
        //val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        //imm.hideSoftInputFromWindow(windowToken, 0)
//        Timber.d("DEBUG")
//        Timber.d(imm.currentInputMethodSubtype.toString())
//        Timber.d(imm.inputMethodList.toString())
//        Timber.d(imm.enabledInputMethodList.toString())
//        val inputConnection = BaseInputConnection(binding.cardEditText, true)
////        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT))
////        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ALT_LEFT))
//        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT))
//        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
//
////        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT))
////        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ALT_LEFT))
//        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT))
//        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SPACE))
    }

    override fun onResume() {
        this.registerReceiver(usbReceiver, filter)
//        if (usbAlreadyAttached()) {
//            binding.cardEditText.removeReader(cardReader)
//            binding.cardEditText.initReader(cardReader, cardFilter)
//        }
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        this.unregisterReceiver(usbReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    inner class UsbIntentReceiver : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?
        ) {
            intent?.action?.let { action ->
                if (action == USB_ATTACHED) {

                    Timber.d("USB = %s", "Attached")
//                    binding.cardEditText.removeReader(cardReader)
//                    binding.cardEditText.initReader(cardReader, cardFilter)
//                    renderUi(WaitForReading)
                }

                if (action == USB_DETACHED) {
                    Timber.d("USB = %s", "Detached")
//                    binding.cardEditText.removeReader(cardReader)
                }
            }
        }
    }
}
