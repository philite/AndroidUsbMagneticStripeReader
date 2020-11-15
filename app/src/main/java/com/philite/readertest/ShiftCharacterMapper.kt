package com.philite.readertest

class ShiftCharacterMapper {
    companion object {
        private val characterMap = HashMap<Char, Char>().apply {
            put('`', '~')
            put('1', '!')
            put('2', '@')
            put('3', '#')
            put('4', '$')
            put('5', '%')
            put('6', '^')
            put('7', '&')
            put('8', '*')
            put('9', '(')
            put('0', ')')
            put('-', '_')
            put('=', '+')
            put('q', 'Q')
            put('w', 'W')
            put('e', 'E')
            put('r', 'R')
            put('t', 'T')
            put('y', 'Y')
            put('u', 'U')
            put('i', 'I')
            put('o', 'O')
            put('p', 'P')
            put('[', '{')
            put(']', '}')
            put('a', 'A')
            put('s', 'S')
            put('d', 'D')
            put('f', 'F')
            put('g', 'G')
            put('h', 'H')
            put('j', 'J')
            put('k', 'K')
            put('l', 'L')
            put(';', ';')
            put('\'', '\"') // Map from ' to "
            put('z', 'Z')
            put('x', 'X')
            put('c', 'C')
            put('v', 'V')
            put('b', 'B')
            put('n', 'N')
            put('m', 'M')
            put(',', '<')
            put('.', '>')
            put('/', '?')
            put('\\', '|')
        }
    }
    // Inject so it can be load and unload
    // Use HashMap
    fun map(input: Char): Char? {
        return characterMap[input]
    }
}
