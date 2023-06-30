/**
 * @copyright Copyright © 2022, 2023 HATAKEYAMA Motohiko
 * @license Released under the MIT license
 * https://opensource.org/licenses/mit-license.php
 * Absolutely no warranty
 */
package com.muimuiz.kotlin.fpconvexact

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor

/**
 * __Object for handling and converting 64-bit/32-bit floating-point numbers__
 *
 * The object (singleton class) provides:
 * - Constants associated with integers that can be represented by floating-point numbers
 * - Interconversion between byte arrays, hexadecimal strings, and floating-point numbers
 * - Constants associated with floating-point representations of boundary values
 * - Type conversions: Provides extended methods for "exact" conversions
 * that can accurately detect conversion failures.
 *
 * N.B.: The names and the relative order of constants
 * pertaining to the floating-point representation of the Int/Long ranges.
 * ```
 * Long.MIN_VALUE   Int.MIN_VALUE             Int.MAX_VALUE   Long.MAX_VALUE
 *      -2^63   -2^53   -2^31          0         +2^31-1  +2^53  +2^63-1
 * -------|-------|-------|---- ... ---|--- ... ----|-------|-------|------->
 *        A       B       C                         D       E      F|
 *                ●<=========== ... ======= ... ===========>●
 *            the integer range that Double can exactly represent
 *             =  the range that Double may have fractional part
 * ```
 * - A: DOUBLE_FOR_MIN_LONG = Long.MIN_VALUE (= -2^63)
 * - B: MIN_DOUBLE_FOR_DOUBLE_EXACT_INTEGER_RANGE = MIN_LONG_FOR_DOUBLE_EXACT_INTEGER_RANGE (= -2^53)
 * - C: (private) _DOUBLE_FOR_MIN_INT = Int.MIN_VALUE (= -2^31)
 * - D: (private) _DOUBLE_FOR_MAX_INT = Int.MAX_VALUE (= +2^31-1)
 * - E: MAX_DOUBLE_FOR_DOUBLE_EXACT_INTEGER_RANGE = MAX_LONG_FOR_DOUBLE_EXACT_INTEGER_RANGE (= +2^53)
 * - F: MAX_DOUBLE_LESS_THAN_MAX_LONG (= the prev of +2^63 < +2^63-1)
 * ```
 * Int.MIN_VALUE                              Int.MAX_VALUE
 *      -2^31   -2^24          0          +2^24  +2^31-1
 * -------|-------|---- ... ---|--- ... ----|-------|------->
 *        a       b                         e      f|
 *                ●<=== ... ======= ... ===>●
 *     the integer range that Float can exactly represent
 *      =  the range that Float may have fractional part
 * ```
 * - a: FLOAT_FOR_MIN_INT = Int.MIN_VALUE (= -2^31)
 * - b: MIN_FLOAT_FOR_FLOAT_EXACT_INTEGER_RANGE = MIN_INT_FOR_FLOAT_EXACT_INTEGER_RANGE (= -2^24)
 * - e: MAX_FLOAT_FOR_FLOAT_EXACT_INTEGER_RANGE = MAX_INT_FOR_FLOAT_EXACT_INTEGER_RANGE (= +2^24)
 * - f: MAX_FLOAT_LESS_THAN_MAX_INT (= the prev of +2^31 < +2^31-1)
 */
object FPConvExact {

    //----------------
    // Constants associated with integers that can be represented by floating-point numbers

    /** The number of bits in the significand of IEEE 754 binary64, _excluding_ the hidden bit. 52 bits. */
    const val DOUBLE_SIGNIFICAND_SIZE_BITS = 52

    /** The number of bits in the significand of IEEE 754 binary64, _including_ the hidden bit. 53 bits. */
    const val DOUBLE_SIGNIFICAND_SIZE_BITS_WITH_HIDDEN_BIT = DOUBLE_SIGNIFICAND_SIZE_BITS + 1

    /** The number of bits in the significand of IEEE 754 binary32, _excluding_ the hidden bit. 23 bits. */
    const val FLOAT_SIGNIFICAND_SIZE_BITS = 23

    /** The number of bits in the significand of IEEE 754 binary32, _including_ the hidden bit. 24 bits. */
    const val FLOAT_SIGNIFICAND_SIZE_BITS_WITH_HIDDEN_BIT = FLOAT_SIGNIFICAND_SIZE_BITS + 1

    /** The minimum Long representation of the integer range that Double can exactly represent. -2^53. */
    const val MIN_LONG_FOR_DOUBLE_EXACT_INTEGER_RANGE = - (1L shl DOUBLE_SIGNIFICAND_SIZE_BITS_WITH_HIDDEN_BIT)

    /** The maximum Long representation of the integer range that Double can exactly represent. +2^53. */
    const val MAX_LONG_FOR_DOUBLE_EXACT_INTEGER_RANGE = + (1L shl DOUBLE_SIGNIFICAND_SIZE_BITS_WITH_HIDDEN_BIT)

    /** The minimum Int representation of the integer range that Float can exactly represent. -2^24. */
    const val MIN_INT_FOR_FLOAT_EXACT_INTEGER_RANGE = - (1 shl FLOAT_SIGNIFICAND_SIZE_BITS_WITH_HIDDEN_BIT)

    /** The maximum Int representation of the integer range that Float can exactly represent. +2^24. */
    const val MAX_INT_FOR_FLOAT_EXACT_INTEGER_RANGE = + (1 shl FLOAT_SIGNIFICAND_SIZE_BITS_WITH_HIDDEN_BIT)

    //----------------

    /** wrapping in a try clause to deal with unexpected exceptions caused by the NIO Buffer API operations. */
    private inline fun <R> _tryBuffer(block: () -> R): R {
        try {
            return block()
        } catch (e: RuntimeException) {
            throw RuntimeException("code error", e)
        }
    }

    //----------------
    // Interconversion between byte arrays, hexadecimal strings, and floating-point numbers

    /** The native byte order of the underlying platform. Same as static value: ByteOrder.nativeOrder() */
    val NATIVE_BYTE_ORDER: ByteOrder = ByteOrder.nativeOrder()

    /**
     * __Converts a ByteArray representation to a Double value.__
     * _N.B._ The validity of the resulting representation is not verified.
     * Depending on the byte sequence, the constructed value may be incorrect as a 64-bit FP number representation.
     * @param byteOrder the byte order of the _byte array_.
     * By default, the byte order of the byte array is interpreted as big-endian.
     * @return The converted Double value.
     * @throws UnsupportedOperationException (runtime) The size of ByteArray is not 8.
     * @throws RuntimeException code errors (should not occur), propagated from the NIO Buffer API.
     */
    fun ByteArray.toDouble(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): Double {
        if (size != Double.SIZE_BYTES) {
            throw UnsupportedOperationException("size not 8: $size")
        }
        return _tryBuffer{ ByteBuffer.wrap(this).order(byteOrder).position(0).double }
    }

    /**
     * __Converts a ByteArray representation to a Float value.__
     * _N.B._ The validity of the resulting representation is not verified.
     * Depending on the byte sequence, the constructed value may be incorrect as a 32-bit FP number representation.
     * @param byteOrder the byte order of the _byte array_.
     * By default, the byte order of the byte array is interpreted as big-endian.
     * @return The converted Float value.
     * @throws UnsupportedOperationException (runtime) The size of ByteArray is not 4.
     * @throws RuntimeException code errors (that should not occur), propagated from the NIO Buffer API.
     */
    fun ByteArray.toFloat(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): Float {
        if (size != Float.SIZE_BYTES) {
            throw UnsupportedOperationException("size not 4: $size")
        }
        return _tryBuffer{ ByteBuffer.wrap(this).order(byteOrder).position(0).float }
    }

    /**
     * __Converts a hexadecimal string to a ByteArray.__
     * The string consists of hexadecimal characters, i.e. “0-9” and case-insensitive “A-F”.
     * The prefix “0x” can be placed before a hexadecimal string.
     * Underscores “_” may be included anywhere in the string, and they will be disregarded during conversion.
     * Note that the number of hexadecimal characters must be even.
     * @param maxSize The maximum allowable number of bytes for the resulting array. By default, no restrictions.
     * @return The converted ByteArray.
     * @throws IllegalArgumentException (runtime) maxSize is not positive.
     * @throws UnsupportedOperationException (runtime) The string is incorrect as the specified hexadecimal number.
     */
    fun String.hexToByteArray(maxSize: Int? = null): ByteArray {
        require(maxSize == null || maxSize > 0){ "maxSize invalid: $maxSize" }
        val RE_PREFIXED = Regex("""^0[xX](.*)$""")
        val RE_HEXBYTE  = Regex("""[0-9A-F]{2}""", RegexOption.IGNORE_CASE)
        // assigned only once at the first time
        val trimmed = (RE_PREFIXED.find(this)?.groupValues?.get(1) ?: this).replace("_", "")
        if (trimmed.length % 2 != 0) {
            throw UnsupportedOperationException("hexadecimal count not even: ${trimmed.length}")
        }
        if (maxSize != null && 2 * maxSize < trimmed.length) { // check size before chunking
            throw UnsupportedOperationException("exceeds max size $maxSize: ${trimmed.length / 2}")
        }
        val barray = trimmed.chunked(2){
            val bytestr = it.toString() // CharSequence to String
            if (! RE_HEXBYTE.matches(bytestr)) {
                throw UnsupportedOperationException("malformed hex: $bytestr")
            }
            bytestr.toInt(radix = 16).toByte()
        }.toByteArray()
        if (maxSize != null && maxSize < barray.size) { // redundant check, should not happen
            throw UnsupportedOperationException("exceeds max size $maxSize: ${barray.size}")
        }
        return barray
    }

    /**
     * __Converts a hexadecimal string to a Double value.__
     * The string consists of hexadecimal characters, i.e. “0-9” and case-insensitive “A-F”.
     * It may include prefix “0x” and underscores “_”, which is disregarded during conversion.
     * The number of hexadecimal characters must be 16, appropriate for representing a Double value.
     * The byte-order representation in the string is always 'big-endian,'
     * i.e., the most significant bit (MSB) is represented by the first part of the string.
     * @return The converted Double value. _N.B._ The validity of the resulting representation is not verified.
     * @throws UnsupportedOperationException (runtime) The string is incorrect as the specified hexadecimal number,
     * propagated from [String.hexToByteArray].
     * Or, the size is incorrect, propagated from [ByteArray.toDouble].
     * @throws RuntimeException code errors (should not occur), propagated from the NIO Buffer API.
     */
    fun String.hexToDouble(): Double {
        return hexToByteArray(maxSize = Double.SIZE_BYTES).toDouble()
    }
    /**
     * __Converts a hexadecimal string to a Float value.__
     * The string consists of hexadecimal characters, i.e. “0-9” and case-insensitive “A-F”.
     * It may include prefix “0x” and underscores “_”, which is disregarded during conversion.
     * The number of hexadecimal characters must be 8, appropriate for representing a Float value.
     * The byte-order representation in the string is always 'big-endian,'
     * i.e., the most significant bit (MSB) is represented by the first part of the string.
     * @return The converted Float value. _N.B._ The validity of the resulting representation is not verified.
     * @throws UnsupportedOperationException (runtime) The string is incorrect as the specified hexadecimal number,
     * propagated from [String.hexToByteArray].
     * Or, the size is incorrect, propagated from [ByteArray.toDouble].
     * @throws RuntimeException code errors (should not occur), propagated from the NIO Buffer API.
     */
    fun String.hexToFloat(): Float {
        return hexToByteArray(maxSize = Float.SIZE_BYTES).toFloat()
    }

    /**
     * __Converts a Double value to a ByteArray representation.__
     * To get the byte representation of a Double (64-bit FP number) value, returns a ByteArray instance.
     * @param byteOrder the byte order of the resulting array. The big-endian is assumed if omitted.
     * @return The converted ByteArray with the size 8.
     * @throws RuntimeException code errors (should not occur), propagated from the NIO Buffer API.
     */
    fun Double.toByteArray(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray {
        val bbuffer = _tryBuffer{ ByteBuffer.allocate(Double.SIZE_BYTES).order(byteOrder) } // has a backing array
        _tryBuffer{ bbuffer.asDoubleBuffer().position(0).put(this) }
        return _tryBuffer{ bbuffer.array() } // the backing array
    }

    /**
     * __Converts a Float value to a ByteArray representation.__
     * To get the byte representation of a Float (32-bit FP number) value, returns a ByteArray instance.
     * @param byteOrder the byte order of the resulting array. The big-endian is assumed if omitted.
     * @return The converted ByteArray with the size 4.
     * @throws RuntimeException code errors (should not occur), propagated from the NIO Buffer API.
     */
    fun Float.toByteArray(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray {
        val bbuffer = _tryBuffer{ ByteBuffer.allocate(Float.SIZE_BYTES).order(byteOrder) } // has a backing array
        _tryBuffer{ bbuffer.asFloatBuffer().position(0).put(this) }
        return _tryBuffer{ bbuffer.array() } // the backing array
    }

    /**
     * __Converts a ByteArray to a hexadecimal string.__
     * @param withPrefix Appends leading sequence “0x”.
     * @param lowercase Uses lowercase “a-f” instead of “A-F” for hexadecimals. By default, uses uppercase.
     * @param delimitEvery Inserts an underscore “_” every specified byte. 1 or larger is considered valid.
     * If null or omitted, no delimiter is inserted.
     * @throws IllegalArgumentException (runtime) delimitEvery is less than 1.
     * @return The converted hexadecimal string.
     */
    fun ByteArray.toHexString(
        withPrefix: Boolean = false,
        lowercase:  Boolean = false,
        delimitEvery: Int? = null
    ): String {
        require(delimitEvery == null || delimitEvery >= 1){ "delimitEvery less than 1: $delimitEvery" }
        val fstring = (if (! lowercase) "%02X" else "%02x")
        val vstring = StringBuilder()
        if (withPrefix) { vstring.append("0x") }
        for ((index, byte) in this.withIndex()) {
            if (delimitEvery != null && index > 0 && index % delimitEvery == 0) vstring.append('_')
            vstring.append(String.format(fstring, byte))
        }
        return vstring.toString()
    }

    /**
     * __Converts a Double value to a hexadecimal string.__
     * Returns a hexadecimal string of the byte representation of a Double (64-bit FP number) value.
     * @param withPrefix Appends leading sequence “0x”.
     * @param lowercase Uses lowercase “a-f” instead of “A-F” for hexadecimals. By default, uses uppercase.
     * @param delimitEvery Inserts an underscore “_” every specified byte. 1 or larger is considered valid.
     * By default, every 2 bytes. If null, no delimiter is inserted.
     * @return The resulting hexadecimal string.
     * @throws IllegalArgumentException (runtime) delimitEvery is less than 1,
     * propagated from [ByteArray.toHexString].
     * @throws RuntimeException code errors (should not occur), propagated from the NIO Buffer API.
     */
    fun Double.toHexString(
        withPrefix: Boolean = false,
        lowercase:  Boolean = false,
        delimitEvery: Int? = 2
    ): String {
        return toByteArray().toHexString(withPrefix, lowercase, delimitEvery)
    }

    /**
     * __Converts a Float value to a hexadecimal string.__
     * Returns a hexadecimal string of the byte representation of a Float (32-bit FP number) value.
     * @param withPrefix Appends leading sequence “0x”.
     * @param lowercase Uses lowercase “a-f” instead of “A-F” for hexadecimals. By default, uses uppercase.
     * @param delimitEvery Inserts an underscore “_” every specified byte. 1 or larger is considered valid.
     * By default, every 1 byte. If null, no delimiter is inserted.
     * @return The resulting hexadecimal string.
     * @throws IllegalArgumentException (runtime) When delimitEvery is less than 1,
     * propagated from [ByteArray.toHexString].
     * @throws RuntimeException code errors (that should not occur), propagated from the NIO Buffer API.
     */
    fun Float.toHexString(
        withPrefix: Boolean = false,
        lowercase:  Boolean = false,
        delimitEvery: Int? = 1
    ): String {
        return toByteArray().toHexString(withPrefix, lowercase, delimitEvery)
    }

    /**
     * __Decomposition of a floating-point number into its elements.__
     * Three elements (sign, exponent, and significand [[or mantissa]]) that a floating-point number consists of
     * are represented as integer values.
     * Both Double and Float types are handled, but their correspondence to numerical values differs.
     * The sign is represented by an integer value of ±1.
     * The exponent is expressed in two's complement.
     * The significand is a non-negative integer with the least significant bit as its unit.
     */
    data class FloatingPointComponents(val sign: Int, val significand: Long, val exponent: Int) {

        constructor(sign: Int, significand: Int, exponent: Int) : this(sign, significand.toLong(), exponent)

        companion object {

            /** The bias value used in the representation of the exponent part of Double. 0x3FF = 1023. */
            const val DOUBLE_EXPONENT_BIAS = 0x3FF

            /**
             * The value of the minimum exponential part of Double expressed in two's complement. -1022.
             * Also, used for positive and negative zeros and subnormal numbers of the original value.
             */
            const val DOUBLE_MIN_EXPONENT = 1 - DOUBLE_EXPONENT_BIAS

            /**
             * The value of the special exponential part of Double to indicate that
             * the original value is a non-finite number, i.e., positive and negative infinities or NaN.
             * 0x400 = 1024.
             */
            const val DOUBLE_NONFINITE_EXPONENT = 0x7FF - DOUBLE_EXPONENT_BIAS

            /**
             * The value corresponding the hidden bit of the Double significand. 2^52.
             */
            const val DOUBLE_HIDDEN_BIT_VALUE = (1L shl DOUBLE_SIGNIFICAND_SIZE_BITS)

            /** The bias value used in the representation of the exponent part of Float. 0x7F = 127. */
            const val FLOAT_EXPONENT_BIAS = 0x7F

            /**
             * The value of the minimum exponential part of Float expressed in two's complement. -126.
             * Also, used for positive and negative zeros and subnormal numbers of the original value.
             */
            const val FLOAT_MIN_EXPONENT = 1 - FLOAT_EXPONENT_BIAS

            /**
             * The value of the special exponential part of Float to indicate that
             * the original value is a non-finite number, i.e., positive and negative infinities or NaN.
             * 0x80 = 128.
             */
            const val FLOAT_NONFINITE_EXPONENT = 0xFF - FLOAT_EXPONENT_BIAS

            /**
             * The value corresponding the hidden bit of the Double significand. 2^23.
             */
            const val FLOAT_HIDDEN_BIT_VALUE = (1 shl FLOAT_SIGNIFICAND_SIZE_BITS)

        }

        /**
         * __Reconstruct a Double value from the decomposed FloatingPointComponents.__
         * The method rigorously examines the component values and throws an exception
         * if they deviate from the standards that compose a Double value.
         * The sign must be ±1, the exponent within a proper range, and, for normal values,
         * the significand must be positive and properly normalized.
         * As for the valid ranges, see [Double.toFloatingPointComponents].
         * @return The reconstructed Double value.
         * @throws IllegalStateException (runtime) The components are not in the valid ranges.
         * @throws RuntimeException code errors (should not occur), propagated from the NIO Buffer API.
         */
        fun toDouble(): Double {
            check(sign == +1 || sign == -1){ "sign not ±1: $sign" }
            check(exponent in DOUBLE_MIN_EXPONENT .. DOUBLE_NONFINITE_EXPONENT)
            { "exponent out of range of -1022 .. 1024: $exponent" }
            check(significand in 0L until MAX_LONG_FOR_DOUBLE_EXACT_INTEGER_RANGE)
            { "significand out of range of 0 .. 2^53-1: $significand" }
            val issubnormal = (exponent == DOUBLE_MIN_EXPONENT) && (significand < DOUBLE_HIDDEN_BIT_VALUE)
            val isnonfinite = (exponent == DOUBLE_NONFINITE_EXPONENT)
            check(issubnormal || isnonfinite || DOUBLE_HIDDEN_BIT_VALUE <= significand)
            { "significand not normalized (< 2^52): $significand" }
            val expbiased = when {
                issubnormal -> 0x000
                isnonfinite -> 0x7FF
                else        -> exponent + DOUBLE_EXPONENT_BIAS
            }
            val sbuffer = _tryBuffer{ ByteBuffer.allocate(Long.SIZE_BYTES) }
            _tryBuffer{ sbuffer.asLongBuffer().position(0).put(significand) }
            val sarray = _tryBuffer{ sbuffer.array() }
            val barray = ByteArray(Double.SIZE_BYTES)
            barray[0] = ((if (sign > 0) 0x00 else 0x80) or (expbiased ushr 4)).toByte()
            barray[1] = ((expbiased shl 4) or (sarray[1].toInt() and 0x0F)).toByte()
            sarray.copyInto(barray, 2, 2)
            //for (i in 2 until Double.SIZE_BYTES) barray[i] = sarray[i]
            return _tryBuffer{ ByteBuffer.wrap(barray).double }
        }

        /**
         * __Reconstruct a Float value from the decomposed FloatingPointComponents.__
         * The method rigorously examines the component values and throws an exception
         * if they deviate from the standards that compose a Float value.
         * The sign must be ±1, the exponent within a proper range, and, for normal values,
         * the significand must be positive and properly normalized.
         * As for the valid ranges, see [Float.toFloatingPointComponents].
         * @return The reconstructed Double value.
         * @throws IllegalStateException (runtime) The components are not in the valid ranges.
         * @throws RuntimeException code errors (should not occur), propagated from the NIO Buffer API.
         */
        fun toFloat(): Float {
            check(sign == +1 || sign == -1){ "sign not ±1: $sign" }
            check(significand in 0L until MAX_INT_FOR_FLOAT_EXACT_INTEGER_RANGE.toLong())
            { "significand out of range of 0 .. 2^24-1: $significand" }
            check(exponent in FLOAT_MIN_EXPONENT .. FLOAT_NONFINITE_EXPONENT)
            { "exponent out of range of -126 .. 128: $exponent" }
            val issubnormal = (exponent == FLOAT_MIN_EXPONENT) && (significand < FLOAT_HIDDEN_BIT_VALUE.toLong())
            val isnonfinite = (exponent == FLOAT_NONFINITE_EXPONENT)
            check(issubnormal || isnonfinite || FLOAT_HIDDEN_BIT_VALUE.toLong() <= significand)
            { "significand not normalized (< 2^23): $significand" }
            val expbiased = when {
                issubnormal -> 0x00
                isnonfinite -> 0xFF
                else        -> exponent + FLOAT_EXPONENT_BIAS
            }
            val sbuffer = _tryBuffer{ ByteBuffer.allocate(Int.SIZE_BYTES) }
            _tryBuffer{ sbuffer.asIntBuffer().position(0).put(significand.toInt()) }
            val sarray = _tryBuffer{ sbuffer.array() }
            val barray = ByteArray(Float.SIZE_BYTES)
            barray[0] = ((if (sign > 0) 0x00 else 0x80) or (expbiased ushr 1)).toByte()
            barray[1] = ((expbiased shl 7) or (sarray[1].toInt() and 0x7F)).toByte()
            sarray.copyInto(barray, 2, 2)
            //for (i in 2 until Float.SIZE_BYTES) barray[i] = sarray[i] // not to use copyInto()
            return _tryBuffer{ ByteBuffer.wrap(barray).float }
        }

    }

    /**
     * __Decomposes a Double value to its sign, significand, and exponent.__
     * For normalized values, the significand (mantissa) part is a positive number
     * equal to or greater than 2^52 but less than 2^53 due to the completion of the hidden bit.
     * The unit of the least significant bit of the significand part corresponds to 2^-52.
     * Subnormals and zeros have a minimum exponent value of [FloatingPointComponents.DOUBLE_MIN_EXPONENT] (= -1022),
     * and the significand part has non-negative values less than 2^52.
     * The sign is stored separately as ±1, which allows distinguishing between positive and negative zeros.
     * Non-finite numbers (±Inf and NaN) are identified
     * by a special exponent value of [FloatingPointComponents.DOUBLE_NONFINITE_EXPONENT] (= 1024).
     * ```
     *                        sign    exponent     significand
     *     normal numbers:     ±1  -1022 .. 1023  2^52 .. 2^53-1
     *     subnormal numbers:  ±1      -1022         1 .. 2^52-1
     *     zeros:              ±1      -1022            0
     *     infinities:         ±1      +1024            0
     *     NaNs                ±1      +1024           > 0
     * ```
     * For normal, subnormal numbers and zeros, the values are expressed as:
     * ```
     *     sign × significand × 2^(exponent - 52).
     * ```
     * @return The resulting instance of [FloatingPointComponents].
     * @throws RuntimeException code errors (should not occur), propagated from the NIO Buffer API.
     */
    fun Double.toFloatingPointComponents(): FloatingPointComponents {
        //
        // The bit configuration of a Double (64-bit floating-point) number:
        //  byte0    byte1    byte2    byte3    byte4    byte5    byte6    byte7
        // |76543210|76543210|76543210|76543210|76543210|76543210|76543210|76543210|
        // |SEEEEEEE|EEEE....|........|........|........|........|........|........|
        //  S: sign bit; E: biased exponent; .: significand (mantissa)
        //
        val barray = toByteArray()
        val byte0 = barray[0].toInt()
        val byte1 = barray[1].toInt()
        // sign
        val sign = (if ((0x80 and byte0) == 0) +1 else -1)
        // exponent
        val expbiased = ((0x7F and byte0) shl 4) or ((0xF0 and byte1) ushr 4)
        val issubnormal = (expbiased == 0x000)
        val isnonfinite = (expbiased == 0x7FF)
        val exponent = when {
            issubnormal -> FloatingPointComponents.DOUBLE_MIN_EXPONENT
            isnonfinite -> FloatingPointComponents.DOUBLE_NONFINITE_EXPONENT
            else        -> expbiased - FloatingPointComponents.DOUBLE_EXPONENT_BIAS
        }
        // significand
        val sarray = ByteArray(Long.SIZE_BYTES) // initialized by 0's
        val byte1hiddenbit = (if (issubnormal || isnonfinite) 0x00 else 0x10)
        sarray[1] = ((0x0F and byte1) or byte1hiddenbit).toByte()
        barray.copyInto(sarray, 2, 2)
        val significand = _tryBuffer{ ByteBuffer.wrap(sarray).position(0).long }
        // ByteArray (sarray.array) --wrap--> ByteBuffer ==get==> Long (significand)
        return FloatingPointComponents(sign, significand, exponent)
    }

    /**
     * __Decomposes a Float value to its sign, significand, and exponent.__
     * For normalized values, the significand (mantissa) part is a positive number
     * equal to or greater than 2^23 but less than 2^24 due to the completion of the hidden bit.
     * The unit of the least significant bit of the significand part corresponds to 2^-23.
     * Subnormals and zeros have a minimum exponent value of [FloatingPointComponents.FLOAT_MIN_EXPONENT] (= -126),
     * and the significand part has non-negative values less than 2^23.
     * The sign is stored separately as ±1, which allows distinguishing between positive and negative zeros.
     * Non-finite numbers (±Inf and NaN) are identified
     * by a special exponent value of [FloatingPointComponents.FLOAT_NONFINITE_EXPONENT] (= 128).
     * ```
     *                        sign   exponent    significand
     *     normal numbers:     ±1  -126 .. 127  2^23 .. 2^24-1
     *     subnormal numbers:  ±1      -126        1 .. 2^23-1
     *     zeros:              ±1      -126           0
     *     infinities:         ±1      +128           0
     *     NaNs                ±1      +128          > 0
     * ```
     * For normal, subnormal numbers and zeros, the values are expressed as:
     * ```
     *     sign × significand × 2^(exponent - 23).
     * ```
     * @return The resulting instance of [FloatingPointComponents],
     * @throws RuntimeException code errors (should not occur), propagated from the NIO Buffer API.
     */
    fun Float.toFloatingPointComponents(): FloatingPointComponents {
        //
        // The bit configuration of a Double (32-bit floating-point) number:
        //  byte0    byte1    byte2    byte3
        // |76543210|76543210|76543210|76543210|
        // |SEEEEEEE|E.......|........|........|
        //  S: sign bit; E: biased exponent; .: significand (mantissa)
        //
        val barray = toByteArray()
        val byte0 = barray[0].toInt()
        val byte1 = barray[1].toInt()
        // sign
        val sign = (if ((0x80 and byte0) == 0) +1 else -1)
        // exponent
        val expbiased = ((0x7F and byte0) shl 1) or ((0x80 and byte1) ushr 7)
        val issubnormal = (expbiased == 0x00)
        val isnonfinite = (expbiased == 0xFF)
        val exponent = when {
            issubnormal -> FloatingPointComponents.FLOAT_MIN_EXPONENT
            isnonfinite -> FloatingPointComponents.FLOAT_NONFINITE_EXPONENT
            else        -> expbiased - FloatingPointComponents.FLOAT_EXPONENT_BIAS
        }
        // significand
        val sarray = ByteArray(Int.SIZE_BYTES) // initialized by 0's
        val byte1hiddenbit = (if (issubnormal || isnonfinite) 0x00 else 0x80)
        sarray[1] = ((0x7F and byte1) or byte1hiddenbit).toByte()
        barray.copyInto(sarray, 2, 2)
        val significand = _tryBuffer{ ByteBuffer.wrap(sarray).position(0).int }
        // ByteArray (sarray.array) --wrap--> ByteBuffer ==get==> Int (significand)
        return FloatingPointComponents(sign, significand, exponent)
    }

    /** The pair of the integral and the fractional part as return type of [toIntegralAndFractional] */
    data class IntegralAndFractional<T>(val integral: T, val fractional: T) {

        /** The unary plus operator. */
        operator fun unaryPlus(): IntegralAndFractional<T> = this

    }

    /** The unary minus operator for the instance. Both integral and fractional are negated. */
    @JvmName("IntegralAndFractional_Double_unaryMinus")
    operator fun IntegralAndFractional<Double>.unaryMinus(): IntegralAndFractional<Double> {
        return IntegralAndFractional(-integral, -fractional)
    }
    /** The unary minus operator for the instance. Both integral and fractional are negated. */
    @JvmName("IntegralAndFractional_Float_unaryMinus")
    operator fun IntegralAndFractional<Float>.unaryMinus(): IntegralAndFractional<Float> {
        return IntegralAndFractional(-integral, -fractional)
    }

    /**
     * __Returns the integral part of a Double value.__
     * The truncation direction is determined by towardsNegative flag.
     * If the value is ±Inf or NaN, returns itself.
     * @param towardsNegative By default, for negative values, truncation occurs towards zero.
     * If this flag is set to true, truncation happens towards the negative infinity (the same behavior as floor()).
     * @return The integral (integer) part as a Double value.
     */
    fun Double.integralPart(towardsNegative: Boolean = false): Double {
        return (if (towardsNegative || this >= 0.0) floor(this) else -floor(-this))
    }

    /**
     * __Splits a Double value into its integral and fractional parts.__
     * The truncation direction is determined by towardsNegative flag.
     * If the value is ±Inf, (integral, fractional) will be (±Inf, NaN), respectively.
     * And if the value is NaN, they will be (NaN, NaN).
     * @param towardsNegative By default, for negative values, truncation occurs towards zero,
     * and the fractional part is non-positive.
     * If this flag is set to true, truncation happens towards the negative infinity (the same behavior as floor())
     * and the fractional part is non-negative.
     * @return The resulting values as an IntegralAndFractional instance.
     */
    fun Double.toIntegralAndFractional(towardsNegative: Boolean = false): IntegralAndFractional<Double> {
        val ipart = this.integralPart(towardsNegative)
        val fpart = this - ipart
        return IntegralAndFractional(ipart, fpart)
    }

    /**
     * __Returns the fractional part of a Double value.__
     * The truncation direction is determined by towardsNegative flag.
     * If the value is ±Inf or NaN, returns NaN.
     * @param towardsNegative By default, for negative values, truncation occurs towards zero,
     * and the fractional part is non-positive.
     * If this flag is set to true, the fractional part is always non-negative.
     * @return The fractional (decimal) part as a Double value.
     */
    fun Double.fractionalPart(towardsNegative: Boolean = false): Double {
        return this.toIntegralAndFractional(towardsNegative).fractional
        // calculates both of the integral and the fractional first.
    }

    /**
     * __Determines if a Double value has a non-zero fractional part.__
     * That is, if the value is technically not an integer, returns true.
     * @return True if it has a non-zero fraction.
     */
    fun Double.hasNonZeroFraction(): Boolean {
        return (this.fractionalPart() != 0.0)
    }

    /**
     * __Returns the integral part of a Float value.__
     * The truncation direction is determined by towardsNegative flag.
     * If the value is ±Inf or NaN, returns itself.
     * @param towardsNegative By default, for negative values, truncation occurs towards zero.
     * If this flag is set to true, truncation happens towards the negative infinity (the same behavior as floor()).
     * @return The integral (integer) part as a Float value.
     */
    fun Float.integralPart(towardsNegative: Boolean = false): Float {
        return (if (towardsNegative || this >= 0.0F) floor(this) else -floor(-this))
    }

    /**
     * __Splits a Float value into its integral and fractional parts.__
     * The truncation direction is determined by towardsNegative flag.
     * If the value is ±Inf, (integral, fractional) will be (±Inf, NaN), respectively.
     * And if the value is NaN, they will be (NaN, NaN).
     * @param towardsNegative By default, for negative values, truncation occurs towards zero,
     * and the fractional part is non-positive.
     * If this flag is set to true, truncation happens towards the negative infinity (the same behavior as floor())
     * and the fractional part is non-negative.
     * @return The resulting values as an IntegralAndFractional instance.
     */
    fun Float.toIntegralAndFractional(towardsNegative: Boolean = false): IntegralAndFractional<Float> {
        val ipart = this.integralPart(towardsNegative)
        val fpart = this - ipart
        return IntegralAndFractional(ipart, fpart)
    }

    /**
     * __Returns the fractional part of a Float value.__
     * The truncation direction is determined by towardsNegative flag.
     * If the value is ±Inf or NaN, returns NaN.
     * @param towardsNegative By default, for negative values, truncation occurs towards zero,
     * and the fractional part is non-positive.
     * If this flag is set to true, the fractional part is always non-negative.
     * @return The fractional (decimal) part as a Float value.
     */
    fun Float.fractionalPart(towardsNegative: Boolean = false): Float {
        return this.toIntegralAndFractional(towardsNegative).fractional
        // calculates both of the integral and the fractional first.
    }

    /**
     * __Determines if a Float value has a non-zero fractional part.__
     * That is, if the value is technically not an integer, returns true.
     * @return True if it has a non-zero fraction.
     */
    fun Float.hasNonZeroFraction(): Boolean {
        return (this.fractionalPart() != 0.0F)
    }

    //----------------
    // Constants associated with floating-point representations of boundary values

    // A
    /**
     * The Double value representation of the minimum value of the Long type.
     * Long.MIN_VALUE = -2^63 can be exactly represented in Double.
     */
    val DOUBLE_FOR_MIN_LONG: Double = "C3E_0_0000_0000_0000".hexToDouble()
    // = -2^63 × 1.00...₂; offset binary of exponent: 0x3FF + 63 = 0x43E

    // B
    /**
     * The Double value representation of the minimum value of the integer interval
     * that the Double type can exactly represent.
     * Its negative value is the maximum value of the interval.
     */
    val MIN_DOUBLE_FOR_DOUBLE_EXACT_INTEGER_RANGE: Double = "C34_0_0000_0000_0000".hexToDouble()
    // = -2^53 × 1.00...₂; offset binary of exponent: 0x3FF + 53 = 0x434

    // C
    private const val _DOUBLE_FOR_MIN_INT = Int.MIN_VALUE.toDouble() // = -2^31
    // D
    private const val _DOUBLE_FOR_MAX_INT = Int.MAX_VALUE.toDouble() // = +2^31-1

    // E
    /**
     * The Double value representation of the maximum value of the integer interval
     * that the Double type can exactly represent.
     * Its negative value is the minimum value of the interval.
     */
    val MAX_DOUBLE_FOR_DOUBLE_EXACT_INTEGER_RANGE: Double = "434_0_0000_0000_0000".hexToDouble()
    // = 2^53 × 1.00...₂; offset binary of exponent: 0x3FF + 53 = 0x434

    // F
    /**
     * The maximum value that can be represented by the Double type
     * among values that are smaller than the maximum value of the Long type.
     * Long.MAX_VALUE = 2^63-1 (63 ones in binary) itself cannot be represented in Double.
     * This value is the largest one that is smaller than it.
     */
    val MAX_DOUBLE_LESS_THAN_MAX_LONG: Double = "43D_F_FFFF_FFFF_FFFF".hexToDouble()
    // = 2^62 × 1.11...1₂ (53 ones); offset binary of exponent: 0x3FF + 62 = 0x43D

    // a
    /**
     * The Float value representation of the minimum value of the Int type.
     * Int.MIN_VALUE = -2^31 can be exactly represented in Float.
     */
    val FLOAT_FOR_MIN_INT: Float = "CF_0_0_00_00".hexToFloat()
    // = -2^31 × 1.00...₂; 0x7F + 31 = 0x9E = 0b100_1111_0

    // b
    /**
     * The Float value representation of the minimum value of the integer interval
     * that the Float type can exactly represent.
     * Its negative value is the maximum value of the interval.
     */
    val MIN_FLOAT_FOR_FLOAT_EXACT_INTEGER_RANGE: Float = "CB_8_0_00_00".hexToFloat()
    // = -2^24 × 1.00...₂ 0x7F + 24 = 0x97 = 0b100_1011_1

    // e
    /**
     * The Float value representation of the maximum value of the integer interval
     * that the Float type can exactly represent.
     * Its negative value is the minimum value of the interval.
     */
    val MAX_FLOAT_FOR_FLOAT_EXACT_INTEGER_RANGE: Float = "4B_8_0_00_00".hexToFloat()
    // = 2^24 × 1.00...₂; 0x7F + 24 = 0x97 = 0b100_1011_1

    // f
    /**
     * The maximum value that can be represented by the Float type
     * among values that are smaller than the maximum value of the Int type.
     * Int.MAX_VALUE = 2^31-1 (31 ones in binary) itself cannot be represented in Float.
     * This value is the largest one that is smaller than it.
     */
    val MAX_FLOAT_LESS_THAN_MAX_INT: Float = "4E_F_F_FF_FF".hexToFloat()
    // = 2^30 × 1.11...1₂ (24 ones); 0x7F + 30 = 0x9D = 0b100_1110_1

    //----------------
    // Type conversions:
    // Provides extended methods for "exact" conversions that can accurately detect conversion failures.
    // For each conversion from type T to R, two type-T extension methods are provided.
    // The two methods differ in their handling when the conversion cannot be done exactly:
    // T.toRToExact() throws an exception, while T.toROrNull() returns a null value (instead of an exception).
    //
    // The implementation follows a somewhat convoluted approach:
    // The actual conversion from type T to type R is performed by a common private function called _TToRExact(),
    // which returns either the result or an error message as a Pair.
    // By using _TToRExact() as a block, T.toRToExact() and T.toROrNull() invoke the type-generic functions
    // _ConvertTypeExact<T, R>() and _ConvertTypeOrNull<T, R>() respectively.
    // These generic functions are utilized to achieve the difference in behavior
    // between throwing an exception and returning null,
    // by evaluating the result pair (result or error) provided by the given block.
    // Zero or more flags are processed as a Boolean array using vararg.
    //
    //     T.toRExact(): R   ---> _ConvertTypeExact<T, R>()  ---+---> _TToRExact()
    //     T.toROrNull(): R? ---> _ConvertTypeOrNull<T, R>() ---/

    // throws ArithmeticException if fails
    private inline fun <T, R> _ConvertTypeExact(
        value: T,
        vararg flags: Boolean,
        conversionBlock: (T, BooleanArray) -> Pair<R, String?>
    ) : R {
        val (result, error) = conversionBlock(value, flags)
        if (error != null) { throw ArithmeticException("$error: $value") }
        return result
    }

    // returns null if fails
    private inline fun <T, R> _ConvertTypeOrNull(
        value: T,
        vararg flags: Boolean,
        conversionBlock: (T, BooleanArray) -> Pair<R, String?>
    ): R? {
        val (result, error) = conversionBlock(value, flags)
        if (error != null) return null
        return result
    }

    //----
    // Double to Int

    private fun _DoubleToIntExact(
        value: Double,
        preventTruncation: Boolean
    ): Pair<Int, String?> {
        if (! value.isFinite()) {
            return Pair(0, "not finite")
        }
        if (!(_DOUBLE_FOR_MIN_INT - 1.0 < value && value < _DOUBLE_FOR_MAX_INT + 1.0)) {
            return Pair(0, "out of range of (-2^31-1, 2^31)")
        }
        if (preventTruncation && value.hasNonZeroFraction()) {
            return Pair(0, "fraction non-zero")
        }
        return Pair(value.toInt(), null)
    }

    /**
     * __Converts a Double value to an Int value exactly.__
     * The conversion is performed only if it falls within the range (-2^31-1, 2^31) = (C, D)
     * where Int can represent the integer part.
     * @param preventTruncation If true, a non-zero fraction causes an exception.
     * If false or omitted, the fractional part is truncated towards zero.
     * @return The converted Int value.
     * @throws ArithmeticException (runtime) If the value exceeds the range expressible by Int.
     * Or the fractional part is non-zero when preventTruncation is set.
     */
    fun Double.toIntExact(preventTruncation: Boolean = false): Int {
        return _ConvertTypeExact(this, preventTruncation){ value, flags ->
            _DoubleToIntExact(value, flags[0])
        }
    }

    /**
     * __Converts a Double value to an Int value exactly.__
     * The conversion is performed only if it falls within the range (-2^31-1, 2^31) = (C, D)
     * where Int can represent the integer part.
     * @param preventTruncation If true, a non-zero fraction causes the method to return null.
     * If false or omitted, the fractional part is truncated towards zero.
     * @return The converted Int value.
     * Returns null if the value exceeds the range expressible by Int
     * or if the fractional part is non-zero when preventTruncation is set.
     */
    fun Double.toIntOrNull(preventTruncation: Boolean = false): Int? {
        return _ConvertTypeOrNull(this, preventTruncation){ value, flags ->
            _DoubleToIntExact(value, flags[0])
        }
    }

    //----
    // Double to Long

    private fun _DoubleToLongExact(
        value: Double,
        preventTruncation: Boolean,
        enableExtendedRange: Boolean
    ): Pair<Long, String?> {
        if (! value.isFinite()) {
            return Pair(0L, "not finite")
        }
        if (! enableExtendedRange) { // narrower range but exact conversion
            if (value !in MIN_DOUBLE_FOR_DOUBLE_EXACT_INTEGER_RANGE .. MAX_DOUBLE_FOR_DOUBLE_EXACT_INTEGER_RANGE) {
                return Pair(0L, "out of significand range of -2^53 .. 2^53")
            }
        } else { // wider range but may lose precision
            if (value !in DOUBLE_FOR_MIN_LONG .. MAX_DOUBLE_LESS_THAN_MAX_LONG) {
                return Pair(0L, "out of Long range of [-2^63, 2^63-1)")
            }
        }
        if (preventTruncation && value.hasNonZeroFraction()) {
            return Pair(0L, "fraction non-zero")
        }
        return Pair(value.toLong(), null)
    }

    /**
     * __Converts a Double value to a Long value exactly.__
     * The conversion is performed only if it falls within the range [[-2^53, 2^53]] = [[B, E]]
     * where Double can hold the integer part up to the last digit (by default),
     * or within a wider range [-2^63, 2^63-1) ([[A, F]]) where Long can represent the value with decreased precision.
     * @param preventTruncation If true, the conversion does not allow truncating non-zero decimal digits.
     * By default, the decimal part is truncated towards zero.
     * @param enableExtendedRange If true, allows a wider range of conversions,
     * within the range that Long can represent. This may result in a loss of the precision of the integer value.
     * @return The converted Long value.
     * @throws ArithmeticException (runtime) If the value exceeds the range.
     * Or the fractional part is non-zero when preventTruncation is set.
     */
    fun Double.toLongExact(preventTruncation: Boolean = false, enableExtendedRange: Boolean = false): Long {
        return _ConvertTypeExact(this, preventTruncation, enableExtendedRange){ value, flags ->
            _DoubleToLongExact(value, flags[0], flags[1])
        }
    }

    /**
     * __Converts a Double value to a Long value exactly.__
     * The conversion is performed only if it falls within the range [[-2^53, 2^53]] = [[B, E]]
     * where Double can hold the integer part up to the last digit (by default),
     * or within a wider range [-2^63, 2^63-1) ([[A, F]]) where Long can represent the value with decreased precision.
     * @param preventTruncation If true, the conversion does not allow truncating non-zero decimal digits.
     * By default, the decimal part is truncated towards zero.
     * @param enableExtendedRange If true, allows a wider range of conversions,
     * within the range that Long can represent. This may result in a loss of the precision of the integer value.
     * @return The converted Long value.
     * Returns null if the value exceeds the range
     * or if the fractional part is non-zero when preventTruncation is set.
     */
    fun Double.toLongOrNull(preventTruncation: Boolean = false, enableExtendedRange: Boolean = false): Long? {
        return _ConvertTypeOrNull(this, preventTruncation, enableExtendedRange){ value, flags ->
            _DoubleToLongExact(value, flags[0], flags[1])
        }
    }

    //----
    // Float to Int

    private fun _FloatToIntExact(
        value: Float,
        preventTruncation: Boolean,
        enableExtendedRange: Boolean
    ): Pair<Int, String?> {
        if (! value.isFinite()) {
            return Pair(0, "not finite")
        }
        if (! enableExtendedRange) { // narrower range but exact conversion
            if (value !in MIN_FLOAT_FOR_FLOAT_EXACT_INTEGER_RANGE .. MAX_FLOAT_FOR_FLOAT_EXACT_INTEGER_RANGE) {
                return Pair(0, "out of significand range of -2^24 .. 2^24")
            }
        } else { // wider range but may lose precision
            if (value !in FLOAT_FOR_MIN_INT .. MAX_FLOAT_LESS_THAN_MAX_INT) {
                return Pair(0, "out of Int range of [-2^31, 2^31-1)")
            }
        }
        if (preventTruncation && value.hasNonZeroFraction()) {
            return Pair(0, "fraction non-zero")
        }
        return Pair(value.toInt(), null)
    }

    /**
     * __Converts a Float value to an Int value exactly.__
     * The conversion is performed only if it falls within the range [[-2^24, 2^24]] = [[b, e]]
     * where Float can hold the integer part up to the last digit (by default),
     * or within a wider range [-2^31, 2^31-1) ([[a, f]]) where Int can represent the value with decreased precision.
     * @param preventTruncation If true, the conversion does not allow truncating non-zero decimal digits.
     * By default, the decimal part is truncated towards zero.
     * @param enableExtendedRange If true, allows a wider range of conversions,
     * within the range that Int can represent. This may result in a loss of the precision of the integer value.
     * @return The converted Int value.
     * @throws ArithmeticException (runtime) If the value exceeds the range.
     * Or the fractional part is non-zero when preventTruncation is set.
     */
    fun Float.toIntExact(preventTruncation: Boolean = false, enableExtendedRange: Boolean = false): Int {
        return _ConvertTypeExact(this, preventTruncation, enableExtendedRange){ value, flags ->
            _FloatToIntExact(value, flags[0], flags[1])
        }
    }

    /**
     * __Converts a Float value to an Int value exactly.__
     * The conversion is performed only if it falls within the range [[-2^24, 2^24]] = [[b, e]]
     * where Float can hold the integer part up to the last digit (by default),
     * or within a wider range [-2^31, 2^31-1) ([[a, f]]) where Int can represent the value with decreased precision.
     * @param preventTruncation If true, the conversion does not allow truncating non-zero decimal digits.
     * By default, the decimal part is truncated towards zero.
     * @param enableExtendedRange If true, allows a wider range of conversions,
     * within the range that Int can represent. This may result in a loss of the precision of the integer value.
     * @return The converted Int value.
     * Returns null if the value exceeds the range
     * or if the fractional part is non-zero when preventTruncation is set.
     */
    fun Float.toIntOrNull(preventTruncation: Boolean = false, enableExtendedRange: Boolean = false): Int? {
        return _ConvertTypeOrNull(this, preventTruncation, enableExtendedRange){ value, flags ->
            _FloatToIntExact(value, flags[0], flags[1])
        }
    }

    //----
    // Long to Double

    private fun _LongToDoubleExact(value: Long): Pair<Double, String?> {
        if (value !in MIN_LONG_FOR_DOUBLE_EXACT_INTEGER_RANGE .. MAX_LONG_FOR_DOUBLE_EXACT_INTEGER_RANGE) {
            return Pair(.0, "out of significant range of -2^53 .. 2^53")
        }
        return Pair(value.toDouble(), null)
    }

    /**
     * __Converts a Long value to a Double value exactly.__
     * In usual conversion to 64-bit floating-point number,
     * only 53 bits from the largest binary digit are preserved (when the number is positive).
     * The conversion is preformed only if the value is within the range of -2^53 .. +2^53
     * that can represent the value as an integer.
     * @return A Double value converted.
     * @throws ArithmeticException (runtime) The value exceeds the range.
     */
    fun Long.toDoubleExact(): Double {
        return _ConvertTypeExact(this){ value, _ -> _LongToDoubleExact(value) }
    }

    /**
     * __Converts a Long value to a Double value exactly.__
     * In usual conversion to 64-bit floating-point number,
     * only 53 bits from the largest binary digit are preserved (when the number is positive).
     * The conversion is preformed only if the value is within the range of -2^53 .. +2^53
     * that can represent the value as an integer.
     * @return A Double value converted. Returns null if the value exceeds the range
     */
    fun Long.toDoubleOrNull(): Double? {
        return _ConvertTypeOrNull(this){ value, _ -> _LongToDoubleExact(value) }
    }

    //----
    // Int to Float

    private fun _IntToFloatExact(value: Int): Pair<Float, String?> {
        if (value !in MIN_INT_FOR_FLOAT_EXACT_INTEGER_RANGE .. MAX_INT_FOR_FLOAT_EXACT_INTEGER_RANGE) {
            return Pair(0F, "out of significant range of -2^24 .. 2^24")
        }
        return Pair(value.toFloat(), null)
    }

    /**
     * __Converts an Int value to a Float value exactly.__
     * In usual conversion to 32-bit floating-point number,
     * only 24 bits from the largest binary digit are preserved (when the number is positive).
     * The conversion is preformed only if the value is within the range of -2^24 .. +2^24
     * that can represent the value as an integer.
     * @return A Float value converted.
     * @throws ArithmeticException (runtime) The value exceeds the range.
     */
    fun Int.toFloatExact(): Float {
        return _ConvertTypeExact(this){ value, _ -> _IntToFloatExact(value) }
    }

    /**
     * __Converts an Int value to a Float value exactly.__
     * In usual conversion to 64-bit floating-point number,
     * only 24 bits from the largest binary digit are preserved (when the number is positive).
     * The conversion is preformed only if the value is within the range of -2^24 .. +2^24
     * that can represent the value as an integer.
     * @return A Float value converted. Returns null if the value exceeds the range
     */
    fun Int.toFloatOrNull(): Float? {
        return _ConvertTypeOrNull(this){ value, _ -> _IntToFloatExact(value) }
    }

}

////