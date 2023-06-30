import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

import java.lang.Double.MIN_NORMAL as Double_MIN_NORMAL
import java.lang.Double.MIN_VALUE  as Double_MIN_VALUE
import java.lang.Float.MIN_NORMAL  as Float_MIN_NORMAL
import java.lang.Float.MIN_VALUE   as Float_MIN_VALUE

import java.nio.ByteOrder

import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.math.pow

import com.muimuiz.kotlin.fpconvexact.FPConvExact

internal class FPConvExactTest {

    companion object {

        private inline fun <reified T : Any> _dataEquals(thisObj: T, otherObj: Any?, block: (T, T) -> Boolean) =
            ((thisObj == otherObj) || (otherObj is T) && block(thisObj, otherObj))

        private fun _assert(expectedQ: Double?, actualQ: Double?): Double? {
            println("expected: $expectedQ, actual: $actualQ")
            when {
                expectedQ == null || actualQ == null -> assertEquals(expectedQ, actualQ)
                expectedQ == 0.0  -> assertTrue(actualQ == 0.0)
                expectedQ.isNaN() -> assertTrue(actualQ.isNaN())
                else -> assertEquals(expectedQ, actualQ)
            }
            return actualQ
        }

        private fun _assert(expectedQ: Float?, actualQ: Float?): Float? {
            println("expected: $expectedQ, actual: $actualQ")
            when {
                expectedQ == null || actualQ == null -> assertEquals(expectedQ, actualQ)
                expectedQ == 0.0F -> assertTrue(actualQ == 0.0F)
                expectedQ.isNaN() -> assertTrue(actualQ.isNaN())
                else -> assertEquals(expectedQ, actualQ)
            }
            return actualQ
        }


        private infix fun Double._pow(n: Int): Double = this.pow(n)

        private infix fun Float._pow(n: Int): Float = this.pow(n)

        private fun _po2(n: Int): Long = 1L shl n

        private val _A_63        = FPConvExact.DOUBLE_FOR_MIN_LONG                       // -2^63
        private val _B_53_LONG   = FPConvExact.MIN_LONG_FOR_DOUBLE_EXACT_INTEGER_RANGE   // -2^53 (Long)
        private val _B_53        = FPConvExact.MIN_DOUBLE_FOR_DOUBLE_EXACT_INTEGER_RANGE // -2^53 (Double)
        private val _C_31_LONG   = Int.MIN_VALUE.toLong()                                // -2^31   (Long)
        private val _C_31        = Int.MIN_VALUE.toDouble()                              // -2^31   (Double)
        private val _D_31M1_LONG = Int.MAX_VALUE.toLong()                                // +2^31-1 (Long)
        private val _D_31M1      = Int.MAX_VALUE.toDouble()                              // +2^31-1 (Double)
        private val _E_53_LONG   = FPConvExact.MAX_LONG_FOR_DOUBLE_EXACT_INTEGER_RANGE   // +2^53 (Long)
        private val _E_53        = FPConvExact.MAX_DOUBLE_FOR_DOUBLE_EXACT_INTEGER_RANGE // +2^53 (Double)
        private val _F_63PREV    = FPConvExact.MAX_DOUBLE_LESS_THAN_MAX_LONG             // prev of +2^63

        private val _a_31     = FPConvExact.FLOAT_FOR_MIN_INT                       // -2^31
        private val _b_24_INT = FPConvExact.MIN_INT_FOR_FLOAT_EXACT_INTEGER_RANGE   // -2^24 (Int)
        private val _b_24     = FPConvExact.MIN_FLOAT_FOR_FLOAT_EXACT_INTEGER_RANGE // -2^24 (Double)
        private val _e_24_INT = FPConvExact.MAX_INT_FOR_FLOAT_EXACT_INTEGER_RANGE   // +2^24 (Int)
        private val _e_24     = FPConvExact.MAX_FLOAT_FOR_FLOAT_EXACT_INTEGER_RANGE // +2^24 (Double)
        private val _f_31PREV = FPConvExact.MAX_FLOAT_LESS_THAN_MAX_INT             // prev of +2^31

        private fun _fpc(s: Int, m: Long, e: Int) = FPConvExact.FloatingPointComponents(s, m, e)

        private fun <T> _iaf(i: T, f: T) = FPConvExact.IntegralAndFractional(i, f)

        val INVERSE_NATIVE_BYTE_ORDER = when (FPConvExact.NATIVE_BYTE_ORDER) {
            ByteOrder.BIG_ENDIAN    -> ByteOrder.LITTLE_ENDIAN
            ByteOrder.LITTLE_ENDIAN -> ByteOrder.BIG_ENDIAN
            else                    -> throw RuntimeException("unknown byte order (should not occur)")
        }

        //--------
        // test cases

        //----
        // hexadecimal string -> byte array

        val HEXSTRING_TO_BYTEARRAY_Q_CASES = listOf( // correspondence is not one-to-one
            ("01"               to listOf(0x01)),
            ("0123"             to listOf(0x01, 0x23)),
            ("01234567"         to listOf(0x01, 0x23, 0x45, 0x67)),
            ("3FC00000"         to listOf(0x3F, 0xC0, 0x00, 0x00)),
            ("0123456789abcdef" to listOf(0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF)),
            ("3FF8000000000000" to listOf(0x3F, 0xF8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)),
            ("ABCDEF"           to listOf(0xAB, 0xCD, 0xEF)),
            // prefixed
            ("0x0123"           to listOf(0x01, 0x23)),
            ("0X01234567"       to listOf(0x01, 0x23, 0x45, 0x67)),
            // underscores
            ("01_23_45_67"      to listOf(0x01, 0x23, 0x45, 0x67)),
            ("0_1__2___3"       to listOf(0x01, 0x23)),
            ("_abc_def_"        to listOf(0xAB, 0xCD, 0xEF)),
            // empty
            (""                 to listOf()),
            ("0x"               to listOf()),
            // odd number
            ("0"                to null),
            ("01234"            to null),
            ("0x0_1_2_3_4"      to null),
            // malformed
            ("f00 ba7"          to null),
            ("hello world!"     to null),
        ).map{ (hexstring, listintQ) -> hexstring to listintQ?.map{ it.toByte() }?.toByteArray() }

        //----
        // hexadecimal string -> double/float

        val HEXSTRING_TO_DOUBLE_CASES = listOf(
            ("0xFFF_0_0000_0000_0000" to   Double.NEGATIVE_INFINITY),
            ("0xFFE_F_FFFF_FFFF_FFFF" to  -Double.MAX_VALUE),
            ("0xC30_F_9465_B8AB_8E38" to  -1_111_111_111_111_111.0),
            ("0xC00_0_0000_0000_0000" to  -2.0),
            ("0xBFF_F_FFFF_FFFF_FFFF" to (-2.0).nextUp()),
            ("0xBFF_8_0000_0000_0000" to  -1.5),
            ("0xBFF_0_0000_0000_0001" to (-1.0).nextDown()),
            ("0xBFF_0_0000_0000_0000" to  -1.0),
            ("0xBFE_0_0000_0000_0000" to  -0.5),
            ("0x801_0_0000_0000_0000" to  -Double_MIN_NORMAL), // the greatest negative normal
            ("0x800_0_0000_0000_0001" to  -Double_MIN_VALUE),  // the greatest negative subnormal
            //----
            ("0x800_0_0000_0000_0000" to   0.0), // the negative zero
            ("0x000_0_0000_0000_0000" to   0.0), // the positive zero
            //----
            ("0x000_0_0000_0000_0001" to  +Double_MIN_VALUE),  // the least positive subnormal
            ("0x001_0_0000_0000_0000" to  +Double_MIN_NORMAL), // the least positive normal
            ("0x3FE_0_0000_0000_0000" to  +0.5),
            ("0x3FF_0_0000_0000_0000" to  +1.0),
            ("0x3FF_0_0000_0000_0001" to (+1.0).nextUp()),
            ("0x3FF_8_0000_0000_0000" to  +1.5),
            ("0x3FF_F_FFFF_FFFF_FFFF" to (+2.0).nextDown()),
            ("0x400_0_0000_0000_0000" to  +2.0),
            ("0x430_F_9465_B8AB_8E38" to  +1_111_111_111_111_111.0),
            ("0x7FE_F_FFFF_FFFF_FFFF" to  +Double.MAX_VALUE),
            ("0x7FF_0_0000_0000_0000" to   Double.POSITIVE_INFINITY),
            //----
            ("0x7FF_F_FFFF_FFFF_FFFF" to   Double.NaN), // one of IEEE 754-2008 qNaN representations
            ("0x7FF_8_0000_0000_0000" to   Double.NaN), // another qNaN representations
            ("0x7FF_7_FFFF_FFFF_FFFF" to   Double.NaN), // one of IEEE 754-2008 sNaN representations
        )

        val HEXSTRING_TO_FLOAT_CASES = listOf(
            ("0xFF_80_00_00" to   Float.NEGATIVE_INFINITY),
            ("0xFF_7F_FF_FF" to  -Float.MAX_VALUE),
            ("0xC7_D9_03_80" to  -111_111.0F),
            ("0xC0_00_00_00" to  -2.0F),
            ("0xBF_FF_FF_FF" to (-2.0F).nextUp()),
            ("0xBF_C0_00_00" to  -1.5F),
            ("0xBF_80_00_01" to (-1.0F).nextDown()),
            ("0xBF_80_00_00" to  -1.0F),
            ("0xBF_00_00_00" to  -0.5F),
            ("0x80_80_00_00" to  -Float_MIN_NORMAL), // the greatest negative normal
            ("0x80_00_00_01" to  -Float_MIN_VALUE),  // the greatest negative subnormal
            //----
            ("0x80_00_00_00" to   0.0F), // the negative zero
            ("0x00_00_00_00" to   0.0F), // the positive zero
            //----
            ("0x00_00_00_01" to  +Float_MIN_VALUE),  // the greatest negative subnormal
            ("0x00_80_00_00" to  +Float_MIN_NORMAL), // the greatest negative normal
            ("0x3F_00_00_00" to  +0.5F),
            ("0x3F_80_00_00" to  +1.0F),
            ("0x3F_80_00_01" to (+1.0F).nextUp()),
            ("0x3F_C0_00_00" to  +1.5F),
            ("0x3F_FF_FF_FF" to (+2.0F).nextDown()),
            ("0x40_00_00_00" to  +2.0F),
            ("0x47_D9_03_80" to  +111_111.0F),
            ("0x7F_7F_FF_FF" to  +Float.MAX_VALUE),
            ("0x7F_80_00_00" to   Float.POSITIVE_INFINITY),
            //----
            ("0x7F_FF_FF_FF" to   Float.NaN), // one of IEEE 754-2008 qNaN representations
            ("0x7F_C0_00_00" to   Float.NaN), // another qNaN representations
            ("0x7F_BF_FF_FF" to   Float.NaN), // one of IEEE 754-2008 sNaN representations
        )

        //----
        // floating point components -> double/float

        val COMPONENTS_TO_DOUBLE_CASES = listOf( // one-to-one correspondence
            (_fpc(-1, 0x00_0000_0000_0000L, +1024) to   Double.NEGATIVE_INFINITY),
            (_fpc(-1, 0x1F_FFFF_FFFF_FFFFL, +1023) to  -Double.MAX_VALUE),
            (_fpc(-1, 0x1F_9465_B8AB_8E38L,   +49) to  -1_111_111_111_111_111.0),
            (_fpc(-1, 0x10_0000_0000_0000L,    +1) to  -2.0),
            (_fpc(-1, 0x1F_FFFF_FFFF_FFFFL,     0) to (-2.0).nextUp()),
            (_fpc(-1, 0x18_0000_0000_0000L,     0) to  -1.5),
            (_fpc(-1, 0x10_0000_0000_0001L,     0) to (-1.0).nextDown()),
            (_fpc(-1, 0x10_0000_0000_0000L,     0) to  -1.0),
            (_fpc(-1, 0x10_0000_0000_0000L,    -1) to  -0.5),
            (_fpc(-1, 0x10_0000_0000_0000L, -1022) to  -Double_MIN_NORMAL), // max negative normal
            (_fpc(-1, 0x00_0000_0000_0001L, -1022) to  -Double_MIN_VALUE),  // max negative subnormal
            //----
            (_fpc(-1, 0x00_0000_0000_0000L, -1022) to   0.0), // the negative zero
            (_fpc(+1, 0x00_0000_0000_0000L, -1022) to   0.0), // the positive zero
            //----
            (_fpc(+1, 0x00_0000_0000_0001L, -1022) to  +Double_MIN_VALUE),  // min positive subnormal
            (_fpc(+1, 0x10_0000_0000_0000L, -1022) to  +Double_MIN_NORMAL), // min positive normal
            (_fpc(+1, 0x10_0000_0000_0000L,    -1) to  +0.5),
            (_fpc(+1, 0x10_0000_0000_0000L,     0) to  +1.0),
            (_fpc(+1, 0x10_0000_0000_0001L,     0) to (+1.0).nextUp()),
            (_fpc(+1, 0x18_0000_0000_0000L,     0) to  +1.5),
            (_fpc(+1, 0x1F_FFFF_FFFF_FFFFL,     0) to (+2.0).nextDown()),
            (_fpc(+1, 0x10_0000_0000_0000L,    +1) to  +2.0),
            (_fpc(+1, 0x1F_9465_B8AB_8E38L,   +49) to  +1_111_111_111_111_111.0),
            (_fpc(+1, 0x1F_FFFF_FFFF_FFFFL, +1023) to  +Double.MAX_VALUE),
            (_fpc(+1, 0x00_0000_0000_0000L, +1024) to   Double.POSITIVE_INFINITY),
            //----
            (_fpc(+1, 0x0F_FFFF_FFFF_FFFFL, +1024) to   Double.NaN), // one of qNaN
            (_fpc(+1, 0x08_0000_0000_0000L, +1024) to   Double.NaN), // another qNaN
            (_fpc(+1, 0x07_FFFF_FFFF_FFFFL, +1024) to   Double.NaN), // one of sNaN
        )

        val COMPONENTS_DOUBLE_INVALID_CASES = listOf(
            (_fpc(-2, 0x10_0000_0000_0000L,     0)),
            (_fpc( 0, 0x10_0000_0000_0000L,     0)),
            (_fpc(+2, 0x10_0000_0000_0000L,     0)),
            (_fpc(+1, 0x0F_FFFF_FFFF_FFFFL, -1021)),
            (_fpc(+1,                  -1L,     0)),
            (_fpc(+1,                  +1L,     0)),
            (_fpc(+1, 0x20_0000_0000_0000L,     0)),
            (_fpc(+1, 0x0F_FFFF_FFFF_FFFFL, +1023)),
            (_fpc(+1, 0x10_0000_0000_0000L, -1024)),
            (_fpc(+1, 0x10_0000_0000_0000L, -1023)),
            (_fpc(+1, 0x10_0000_0000_0000L, +1025)),
        )

        val COMPONENTS_TO_FLOAT_CASES = listOf( // one-to-one correspondence
            (_fpc(-1, 0x00_00_00, +128) to   Float.NEGATIVE_INFINITY),
            (_fpc(-1, 0xFF_FF_FF, +127) to  -Float.MAX_VALUE),
            (_fpc(-1, 0xD9_03_80,  +16) to  -111_111.0F),
            (_fpc(-1, 0x80_00_00,   +1) to  -2.0F),
            (_fpc(-1, 0xFF_FF_FF,    0) to (-2.0F).nextUp()),
            (_fpc(-1, 0xC0_00_00,    0) to  -1.5F),
            (_fpc(-1, 0x80_00_01,    0) to (-1.0F).nextDown()),
            (_fpc(-1, 0x80_00_00,    0) to  -1.0F),
            (_fpc(-1, 0x80_00_00,   -1) to  -0.5F),
            (_fpc(-1, 0x80_00_00, -126) to  -Float_MIN_NORMAL), // max negative normal
            (_fpc(-1, 0x00_00_01, -126) to  -Float_MIN_VALUE),  // max negative subnormal
            //----
            (_fpc(-1, 0x00_00_00, -126) to   0.0F), // the negative zero
            (_fpc(+1, 0x00_00_00, -126) to   0.0F), // the positive zero
            //----
            (_fpc(+1, 0x00_00_01, -126) to  +Float_MIN_VALUE),  // min negative subnormal
            (_fpc(+1, 0x80_00_00, -126) to  +Float_MIN_NORMAL), // min negative normal
            (_fpc(+1, 0x80_00_00,   -1) to  +0.5F),
            (_fpc(+1, 0x80_00_00,    0) to  +1.0F),
            (_fpc(+1, 0x80_00_01,    0) to (+1.0F).nextUp()),
            (_fpc(+1, 0xC0_00_00,    0) to  +1.5F),
            (_fpc(+1, 0xFF_FF_FF,    0) to (+2.0F).nextDown()),
            (_fpc(+1, 0x80_00_00,   +1) to  +2.0F),
            (_fpc(+1, 0xD9_03_80,  +16) to  +111_111.0F),
            (_fpc(+1, 0xFF_FF_FF, +127) to  +Float.MAX_VALUE),
            (_fpc(+1, 0x00_00_00, +128) to   Float.POSITIVE_INFINITY),
            //----
            (_fpc(+1, 0x7F_FF_FF, +128) to   Float.NaN), // one of qNaN
            (_fpc(+1, 0x40_00_00, +128) to   Float.NaN), // another qNaN
            (_fpc(+1, 0x3F_FF_FF, +128) to   Float.NaN), // one of sNaN
        )

        val COMPONENTS_FLOAT_INVALID_CASES = listOf(
            (_fpc(-2,   0x80_00_00,    0)),
            (_fpc( 0,   0x80_00_00,    0)),
            (_fpc(+2,   0x80_00_00,    0)),
            (_fpc(+1,   0x7F_FF_FF, -125)),
            (_fpc(+1,           -1,    0)),
            (_fpc(+1,           +1,    0)),
            (_fpc(+1, 0x1_00_00_00,    0)),
            (_fpc(+1,   0x7F_FF_FF, +127)),
            (_fpc(+1,   0x80_00_00, -128)),
            (_fpc(+1,   0x80_00_00, -127)),
            (_fpc(+1,   0x80_00_00, +129)),
        )

        //----
        // double/float -> integral and fractional

        val DOUBLE_TO_INTEGRAL_AND_FLACTIONAL_TOWARDS_NEGATIVE_CASES = listOf(
            (  Double.NEGATIVE_INFINITY to _iaf(  Double.NEGATIVE_INFINITY, Double.NaN)),
            ( -Double.MAX_VALUE         to _iaf( -Double.MAX_VALUE, 0.0)),
            ( _B_53.nextDown()          to _iaf( _B_53.nextDown(),  0.0)),  // prev of -2^53
            ( _B_53                     to _iaf( _B_53,             0.0)),  // -2^53
            ( _B_53.nextUp()            to _iaf((_B_53 + 1.0),      0.0)),  // next of -2^53
            ((_B_53 / 2).nextUp()       to _iaf((_B_53 / 2),        0.5)),  // next of -2^52
            ((_B_53 / 4).nextUp()       to _iaf((_B_53 / 4),        0.25)), // next of -2^51
            ( -1.5                      to _iaf(-2.0, 0.5)),
            ((-1.0).nextDown()          to _iaf(-2.0, 1.0 - (2.0 _pow -52))), // prev of -1.0
            ( -1.0                      to _iaf(-1.0, 0.0)),
            ((-1.0).nextUp()            to _iaf(-1.0, 0.0 + (2.0 _pow -53))), // next of -1.0
            ( -0.5                      to _iaf(-1.0, 0.5)),
            ( -(2.0 _pow -53)           to _iaf(-1.0, 1.0 - (2.0 _pow -53))), // -2^-53
            ( -Double_MIN_NORMAL        to _iaf(-1.0, 1.0)), // max negative normal (digit loss)
            ( -Double_MIN_VALUE         to _iaf(-1.0, 1.0)), // max negative subnormal (digit loss)
            (  0.0                      to _iaf( 0.0, 0.0)),
            ( +Double_MIN_VALUE         to _iaf( 0.0, Double_MIN_VALUE)),  // min positive subnormal
            ( +Double_MIN_NORMAL        to _iaf( 0.0, Double_MIN_NORMAL)), // min positive normal
            ( +(2.0 _pow -53)           to _iaf( 0.0, 0.0 + (2.0 _pow -53))), // -2^-53
            ( +0.5                      to _iaf( 0.0, 0.5)),
            ((+1.0).nextDown()          to _iaf( 0.0, 1.0 - (2.0 _pow -53))), // prev of +1.0
            ( +1.0                      to _iaf(+1.0, 0.0)),
            ((+1.0).nextUp()            to _iaf(+1.0, 0.0 + (2.0 _pow -52))), // next of +1.0
            ( +1.5                      to _iaf(+1.0, 0.5)),
            ((_E_53 / 4).nextDown()     to _iaf((_E_53 / 4 - 1.0),   0.75)), // prev of +2^51
            ((_E_53 / 2).nextDown()     to _iaf((_E_53 / 2 - 1.0),   0.5)),  // prev of +2^52
            ( _E_53.nextDown()          to _iaf((_E_53 - 1.0),       0.0)),  // prev of 2^53
            ( _E_53                     to _iaf( _E_53,              0.0)),  // 2^53
            ( _E_53.nextUp()            to _iaf( _E_53.nextUp(),     0.0)),  // next of 2^53
            ( +Double.MAX_VALUE         to _iaf(+Double.MAX_VALUE,   0.0)),
            (  Double.POSITIVE_INFINITY to _iaf( Double.POSITIVE_INFINITY, Double.NaN)),
            (  Double.NaN               to _iaf( Double.NaN,               Double.NaN)),
        )

        val FLOAT_TO_INTEGRAL_AND_FLACTIONAL_TOWARDS_NEGATIVE_CASES = listOf(
            (  Float.NEGATIVE_INFINITY to _iaf(  Float.NEGATIVE_INFINITY, Float.NaN)),
            ( -Float.MAX_VALUE         to _iaf( -Float.MAX_VALUE, 0.0F)),
            ( _b_24.nextDown()         to _iaf( _b_24.nextDown(), 0.0F)),  // prev of -2^24
            ( _b_24                    to _iaf( _b_24,            0.0F)),  // -2^24
            ( _b_24.nextUp()           to _iaf((_b_24 + 1.0F),    0.0F)),  // next of -2^24
            ((_b_24 / 2).nextUp()      to _iaf((_b_24 / 2),       0.5F)),  // next of -2^23
            ((_b_24 / 4).nextUp()      to _iaf((_b_24 / 4),       0.25F)), // next of -2^22
            ( -1.5F                    to _iaf(-2.0F, 0.5F)),
            ((-1.0F).nextDown()        to _iaf(-2.0F, 1.0F - (2.0F _pow -23))), // prev of -1.0
            ( -1.0F                    to _iaf(-1.0F, 0.0F)),
            ((-1.0F).nextUp()          to _iaf(-1.0F, 0.0F + (2.0F _pow -24))), // next of -1.0
            ( -0.5F                    to _iaf(-1.0F, 0.5F)),
            ( -(2.0F _pow -24)         to _iaf(-1.0F, 1.0F - (2.0F _pow -24))), // -2^-24
            ( -Float_MIN_NORMAL        to _iaf(-1.0F, 1.0F)), // max negative normal (digit loss)
            ( -Float_MIN_VALUE         to _iaf(-1.0F, 1.0F)), // max negative subnormal (digit loss)
            (  0.0F                    to _iaf( 0.0F, 0.0F)),
            ( +Float_MIN_VALUE         to _iaf( 0.0F, Float_MIN_VALUE)),  // min positive subnormal
            ( +Float_MIN_NORMAL        to _iaf( 0.0F, Float_MIN_NORMAL)), // min positive normal
            ( +(2.0F _pow -24)         to _iaf( 0.0F, 0.0F + (2.0F _pow -24))), // +2^-24
            ( +0.5F                    to _iaf( 0.0F, 0.5F)),
            ((+1.0F).nextDown()        to _iaf( 0.0F, 1.0F - (2.0F _pow -24))), // prev of +1.0
            ( +1.0F                    to _iaf(+1.0F, 0.0F)),
            ((+1.0F).nextUp()          to _iaf(+1.0F, 0.0F + (2.0F _pow -23))), // next of +1.0
            ( +1.5F                    to _iaf(+1.0F, 0.5F)),
            ((_e_24 / 4).nextDown()    to _iaf((_e_24 / 4 - 1.0F), 0.75F)), // prev of 2^22
            ((_e_24 / 2).nextDown()    to _iaf((_e_24 / 2 - 1.0F), 0.5F)),  // prev of 2^23
            ( _e_24.nextDown()         to _iaf((_e_24 - 1.0F),     0.0F)),  // prev of 2^24
            ( _e_24                    to _iaf( _e_24,             0.0F)),  // 2^24
            ( _e_24.nextUp()           to _iaf( _e_24.nextUp(),    0.0F)),  // next of 2^24
            ( +Float.MAX_VALUE         to _iaf( +Float.MAX_VALUE,  0.0F)),
            (  Float.POSITIVE_INFINITY to _iaf(  Float.POSITIVE_INFINITY, Float.NaN)),
            (  Float.NaN               to _iaf(  Float.NaN,               Float.NaN)),
        )

        //----
        // double/float -> long/int

        val DOUBLE_TO_LONG_CD_INT_RANGE_CASES = listOf(
            ((_C_31 - 1.0).nextUp()     to Pair(_C_31_LONG, true)),
            ( _C_31                     to Pair(_C_31_LONG, false)),
            ((-1.0).nextDown()          to Pair(-1L, true)),
            ( -1.0                      to Pair(-1L, false)),
            ((-1.0).nextUp()            to Pair( 0L, true)),
            ( -(2.0 _pow -53)           to Pair( 0L, true)),
            ( -Double_MIN_NORMAL        to Pair( 0L, true)),
            ( -Double_MIN_VALUE         to Pair( 0L, true)),
            (  0.0                      to Pair( 0L, false)),
            ( +Double_MIN_VALUE         to Pair( 0L, true)),
            ( +Double_MIN_NORMAL        to Pair( 0L, true)),
            ( +(2.0 _pow -53)           to Pair( 0L, true)),
            ((+1.0).nextDown()          to Pair( 0L, true)),
            ( +1.0                      to Pair(+1L, false)),
            ((+1.0).nextUp()            to Pair(+1L, true)),
            ( _D_31M1                   to Pair(_D_31M1_LONG, false)),
            ((_D_31M1 + 1.0).nextDown() to Pair(_D_31M1_LONG, true)),
        )
        val DOUBLE_TO_LONG_BE_EXACT_INTEGER_RANGE_CASES = listOf(
            ( _B_53                 to Pair(_B_53_LONG,          false)),
            ( _B_53.nextUp()        to Pair(_B_53_LONG + 1L,     false)), // next of -2^53
            ((_B_53 / 2).nextUp()   to Pair(_B_53_LONG / 2 + 1L, true)),  // next of -2^52
            ( _C_31   - 1.0         to Pair(_C_31_LONG   - 1L,   false)),
            ( _D_31M1 + 1.0         to Pair(_D_31M1_LONG + 1L,   false)),
            ((_E_53 / 2).nextDown() to Pair(_E_53_LONG / 2 - 1L, true)),  // prev of +2^52
            ( _E_53.nextDown()      to Pair(_E_53_LONG - 1L,     false)), // prev of +2^53
            ( _E_53                 to Pair(_E_53_LONG,          false)),
        )
        val DOUBLE_TO_LONG_AF_LONG_RANGE_CASES = listOf(
            (_A_63            to  Long.MIN_VALUE),
            (_B_53.nextDown() to _B_53_LONG - 2L),
            (_E_53.nextUp()   to _E_53_LONG + 2L),
            (_F_63PREV        to  Long.MAX_VALUE - 1023L),
        )
        val DOUBLE_TO_LONG_OUT_OF_RANGE_CASES = listOf(
            ( Double.NEGATIVE_INFINITY),
            (-Double.MAX_VALUE),
            (_A_63.nextDown()),
            (_F_63PREV.nextUp()),
            (+Double.MAX_VALUE),
            ( Double.POSITIVE_INFINITY),
            ( Double.NaN),
        )

        val FLOAT_TO_INT_be_EXACT_INTEGER_RANGE_CASES = listOf(
            ( _b_24                 to Pair(_b_24_INT,         false)),
            ( _b_24.nextUp()        to Pair(_b_24_INT + 1,     false)), // next of -2^24
            ((_b_24 / 2).nextUp()   to Pair(_b_24_INT / 2 + 1, true)),  // next of -2^23
            ((-1.0F).nextDown()     to Pair(-1, true)),
            ( -1.0F                 to Pair(-1, false)),
            ((-1.0F).nextUp()       to Pair( 0, true)),
            ( -(2.0F _pow -24)      to Pair( 0, true)),
            ( -Float_MIN_NORMAL     to Pair( 0, true)),
            ( -Float_MIN_VALUE      to Pair( 0, true)),
            (  0.0F                 to Pair( 0, false)),
            ( +Float_MIN_VALUE      to Pair( 0, true)),
            ( +Float_MIN_NORMAL     to Pair( 0, true)),
            ( +(2.0F _pow -24)      to Pair( 0, true)),
            ((+1.0F).nextDown()     to Pair( 0, true)),
            ( +1.0F                 to Pair(+1, false)),
            ((+1.0F).nextUp()       to Pair(+1, true)),
            ((_e_24 / 2).nextDown() to Pair(_e_24_INT / 2 - 1, true)),  // prev of +2^52
            ( _e_24.nextDown()      to Pair(_e_24_INT - 1,     false)), // prev of +2^53
            ( _e_24                 to Pair(_e_24_INT,         false)),
        )
        val FLOAT_TO_INT_af_INT_RANGE_CASES = listOf(
            ( _a_31            to  Int.MIN_VALUE),
            ( _b_24.nextDown() to _b_24_INT - 2),
            ( _e_24.nextUp()   to _e_24_INT + 2),
            ( _f_31PREV        to  Int.MAX_VALUE - 127),
        )
        val FLOAT_TO_INT_OUT_OF_RANGE_CASES = listOf(
            ( Float.NEGATIVE_INFINITY),
            (-Float.MAX_VALUE),
            (_a_31.nextDown()),
            (_f_31PREV.nextUp()),
            (+Float.MAX_VALUE),
            ( Float.POSITIVE_INFINITY),
            ( Float.NaN),
        )

    }

    //----------------
    // Interconversion between byte arrays, hexadecimal strings, and floating-point numbers

    @Test
    fun test_ByteArray_toDouble() {
        // test size checking
        FPConvExact.run{
            for (size in 0 .. 2 * Double.SIZE_BYTES) {
                val bytearray = ByteArray(size){ 0x00.toByte() }
                if (size == Double.SIZE_BYTES) {
                    _assert(0.0, bytearray.toDouble())
                } else {
                    assertThrows<UnsupportedOperationException>{ bytearray.toDouble() }
                }
            }
        }
        // test cases
        FPConvExact.run{
            for ((hexstring, value) in HEXSTRING_TO_DOUBLE_CASES) {
                val bytearray = hexstring.hexToByteArray()
                _assert(value, bytearray.toDouble())
            }
        }
        // test byte order
        FPConvExact.run{
            for ((hexstring, value) in HEXSTRING_TO_DOUBLE_CASES) {
                val reversedbytearray = hexstring.hexToByteArray().reversedArray()
                _assert(value, reversedbytearray.toDouble(ByteOrder.LITTLE_ENDIAN))
            }
        }
    }

    @Test
    fun test_ByteArray_toFloat() {
        // test size checking
        FPConvExact.run{
            for (size in 0 .. 2 * Float.SIZE_BYTES) {
                val bytearray = ByteArray(size){ 0x00.toByte() }
                if (size == Float.SIZE_BYTES) {
                    _assert(0.0F, bytearray.toFloat())
                } else {
                    assertThrows<UnsupportedOperationException>{ bytearray.toFloat() }
                }
            }
        }
        // test cases
        FPConvExact.run{
            for ((hexstring, value) in HEXSTRING_TO_FLOAT_CASES) {
                val bytearray = hexstring.hexToByteArray()
                _assert(value, bytearray.toFloat())
            }
        }
        // test byte order
        FPConvExact.run{
            for ((hexstring, value) in HEXSTRING_TO_FLOAT_CASES) {
                val reversedbytearray = hexstring.hexToByteArray().reversedArray()
                _assert(value, reversedbytearray.toFloat(ByteOrder.LITTLE_ENDIAN))
            }
        }
    }

    @Test
    fun test_String_hexToByteArray() {
        // test cases
        FPConvExact.run{
            for ((hexstring, bytearrayQ) in HEXSTRING_TO_BYTEARRAY_Q_CASES) {
                if (bytearrayQ != null) {
                    assertArrayEquals(bytearrayQ, hexstring.hexToByteArray())
                } else {
                    assertThrows<UnsupportedOperationException>{ hexstring.hexToByteArray() }
                }
            }
        }
        // test maxSize
        FPConvExact.run{
            val MAX_SIZE_AND_VALIDITY_CASES =
                listOf((-1 to false), (0 to false), (1 to true), (2 to true), (4 to true), (8 to true), (64 to true))
            for ((maxsize, valid) in MAX_SIZE_AND_VALIDITY_CASES) {
                for ((hexstring, bytearrayQ) in HEXSTRING_TO_BYTEARRAY_Q_CASES) {
                    if (bytearrayQ == null) continue
                    if (valid) {
                        if (bytearrayQ.size <= maxsize) {
                            assertDoesNotThrow{ hexstring.hexToByteArray(maxsize) }
                        } else { // exceeds maxsize
                            assertThrows<UnsupportedOperationException>{ hexstring.hexToByteArray(maxsize) }
                        }
                    } else { // invalid maxsize
                        assertThrows<IllegalArgumentException>{ hexstring.hexToByteArray(maxsize) }
                    }
                }
            }
        }
    }

    @Test
    fun test_String_hexToDouble() {
        // test size checking
        FPConvExact.run{
            for (size in 0 .. 2 * Double.SIZE_BYTES) {
                val hexstring = "00".repeat(size)
                if (size == Double.SIZE_BYTES) {
                    _assert(0.0, hexstring.hexToDouble())
                } else {
                    assertThrows<UnsupportedOperationException>{ hexstring.hexToDouble() }
                }
            }
        }
        // test cases
        FPConvExact.run{
            for ((hexstring, value) in HEXSTRING_TO_DOUBLE_CASES) {
                _assert(value, hexstring.hexToDouble())
            }
        }
    }

    @Test
    fun test_String_hexToFloat() {
        // test size checking
        FPConvExact.run{
            for (size in 0 .. 2 * Float.SIZE_BYTES) {
                val hexstring = "00".repeat(size)
                if (size == Float.SIZE_BYTES) {
                    _assert(0.0F, hexstring.hexToFloat())
                } else {
                    assertThrows<UnsupportedOperationException>{ hexstring.hexToFloat() }
                }
            }
        }
        // test cases
        FPConvExact.run{
            for ((hexstring, value) in HEXSTRING_TO_FLOAT_CASES) {
                _assert(value, hexstring.hexToFloat())
            }
        }
    }

    @Test
    fun test_Double_toByteArray() {
        // test cases
        FPConvExact.run{
            for ((hexstring, value) in HEXSTRING_TO_DOUBLE_CASES) {
                when {
                    value == 0.0  -> _assert((0.0),      value.toByteArray().toDouble())
                    value.isNaN() -> _assert(Double.NaN, value.toByteArray().toDouble())
                    else -> assertArrayEquals(hexstring.hexToByteArray(), value.toByteArray())
                }
            }
        }
        // test byte order
        FPConvExact.run{
            for ((hexstring, value) in HEXSTRING_TO_DOUBLE_CASES){
                if (value == 0.0 || value.isNaN()) continue
                val reversedbytearray = hexstring.hexToByteArray().reversedArray()
                assertArrayEquals(reversedbytearray, value.toByteArray(ByteOrder.LITTLE_ENDIAN))
            }
        }
    }

    @Test
    fun test_Float_toByteArray() {
        // test cases
        FPConvExact.run{
            for ((hexstring, value) in HEXSTRING_TO_FLOAT_CASES) {
                when {
                    value == 0.0F -> _assert((0.0F),    value.toByteArray().toFloat())
                    value.isNaN() -> _assert(Float.NaN, value.toByteArray().toFloat())
                    else -> assertArrayEquals(hexstring.hexToByteArray(), value.toByteArray())
                }
            }
        }
        // test byte order
        FPConvExact.run{
            for ((hexstring, value) in HEXSTRING_TO_FLOAT_CASES){
                if (value == 0.0F || value.isNaN()) continue
                val reversedbytearray = hexstring.hexToByteArray().reversedArray()
                assertArrayEquals(reversedbytearray, value.toByteArray(ByteOrder.LITTLE_ENDIAN))
            }
        }
    }

    @Test
    fun test_ByteArray_toHexString() {
        // test cases
        FPConvExact.run{
            for ((_, bytearrayQ) in HEXSTRING_TO_BYTEARRAY_Q_CASES) { // not one-to-one
                if (bytearrayQ == null) continue
                assertArrayEquals(bytearrayQ, bytearrayQ.toHexString().hexToByteArray())
            }
        }
        // test withPrefix
        FPConvExact.run{
            val RE_PREFIXED = Regex("""^0x.*$""")
            for ((_, bytearrayQ) in HEXSTRING_TO_BYTEARRAY_Q_CASES) {
                if (bytearrayQ == null) continue
                assertTrue(! RE_PREFIXED.matches(bytearrayQ.toHexString(withPrefix = false)))
                assertTrue(  RE_PREFIXED.matches(bytearrayQ.toHexString(withPrefix = true)))
            }
        }
        // test lowercase
        FPConvExact.run{
            val RE_NO_UPPERCASE = Regex("""^[^A-F]*$""")
            val RE_NO_LOWERCASE = Regex("""^[^a-f]*$""")
            for ((_, bytearrayQ) in HEXSTRING_TO_BYTEARRAY_Q_CASES) {
                if (bytearrayQ == null) continue
                assertTrue(RE_NO_LOWERCASE.matches(bytearrayQ.toHexString(lowercase = false)))
                assertTrue(RE_NO_UPPERCASE.matches(bytearrayQ.toHexString(lowercase = true)))
            }
        }
        // test delimitEvery
        FPConvExact.run{
            val DELIMIT_EVERY_AND_VALIDITY_CASES =
                listOf((-1 to false), (0 to false), (1 to true), (2 to true), (4 to true), (8 to true), (64 to true))
            for ((delimitevery, valid) in DELIMIT_EVERY_AND_VALIDITY_CASES) {
                for ((_, bytearrayQ) in HEXSTRING_TO_BYTEARRAY_Q_CASES) {
                    if (bytearrayQ == null) continue
                    if (valid) {
                        val re_delimited =
                            Regex("^(?:(?:[0-9A-F]{2}){$delimitevery}_)*(?:[0-9A-F]{2}){0,$delimitevery}$")
                        assertTrue(re_delimited.matches(bytearrayQ.toHexString(delimitEvery = delimitevery)))
                    } else {
                        assertThrows<IllegalArgumentException>{ bytearrayQ.toHexString(delimitEvery = delimitevery) }
                    }
                }
            }
        }
    }

    @Test
    fun test_Double_toHexString() {
        // test cases
        FPConvExact.run{
            for ((_, value) in HEXSTRING_TO_DOUBLE_CASES) { // not one-to-one
                _assert(value, value.toHexString().hexToDouble())
            }
        }
        // test withPrefix, lowercase, delimitEvery ... omitted
    }

    @Test
    fun test_Float_toHexString() {
        // test cases
        FPConvExact.run{
            for ((_, value) in HEXSTRING_TO_FLOAT_CASES) { // not one-to-one
                _assert(value, value.toHexString().hexToFloat())
            }
        }
        // test withPrefix, lowercase, delimitEvery ... omitted
    }

    @Test
    fun test_FloatingPointComponents_toDouble() {
        // test cases
        FPConvExact.run{
            for ((components, value) in COMPONENTS_TO_DOUBLE_CASES) {
                _assert(value, components.toDouble())
            }
        }
        // test invalid cases
        FPConvExact.run{
            for (components in COMPONENTS_DOUBLE_INVALID_CASES) {
                assertThrows<IllegalStateException>{ components.toDouble() }
            }
        }
    }

    @Test
    fun test_FloatingPointComponents_toFloat() {
        // test cases
        FPConvExact.run{
            for ((components, value) in COMPONENTS_TO_FLOAT_CASES) {
                _assert(value, components.toFloat())
            }
        }
        // test invalid cases
        FPConvExact.run{
            for (components in COMPONENTS_FLOAT_INVALID_CASES) {
                assertThrows<IllegalStateException>{ components.toFloat() }
            }
        }
    }

    @Test
    fun test_Double_toFloatingPointComponents() {
        // test cases
        FPConvExact.run{
            for ((components, value) in COMPONENTS_TO_DOUBLE_CASES) {
                when {
                    value == 0.0  -> _assert((0.0),      value.toFloatingPointComponents().toDouble())
                    value.isNaN() -> _assert(Double.NaN, value.toFloatingPointComponents().toDouble())
                    else -> assertEquals(components, value.toFloatingPointComponents())
                }
            }
        }
    }

    @Test
    fun test_Float_toFloatingPointComponents() {
        // test cases
        FPConvExact.run{
            for ((components, value) in COMPONENTS_TO_FLOAT_CASES) {
                when {
                    value == 0.0F -> _assert((0.0F),    value.toFloatingPointComponents().toFloat())
                    value.isNaN() -> _assert(Float.NaN, value.toFloatingPointComponents().toFloat())
                    else -> assertEquals(components, value.toFloatingPointComponents())
                }
            }
        }
    }

    @Test
    fun test_Double_toIntegralAndFractional() {
        fun _assert(
            expected: FPConvExact.IntegralAndFractional<Double>,
            actual:   FPConvExact.IntegralAndFractional<Double>
        ): FPConvExact.IntegralAndFractional<Double> {
            _assert(expected.integral,   actual.integral)
            _assert(expected.fractional, actual.fractional)
            return actual
        }
        // test cases
        FPConvExact.run{
            for ((value, intandfrac) in DOUBLE_TO_INTEGRAL_AND_FLACTIONAL_TOWARDS_NEGATIVE_CASES) {
                if (value < 0.0) continue
                _assert(-intandfrac, (-value).toIntegralAndFractional())
                _assert(+intandfrac, (+value).toIntegralAndFractional())
            }
        }
        // test towardsNegative
        FPConvExact.run{
            for ((value, intandfrac) in DOUBLE_TO_INTEGRAL_AND_FLACTIONAL_TOWARDS_NEGATIVE_CASES) {
                _assert(intandfrac, value.toIntegralAndFractional(towardsNegative = true))
            }
        }
    }

    @Test
    fun test_Double_integralPart() {
        // test cases
        FPConvExact.run{
            for ((value, intandfrac) in DOUBLE_TO_INTEGRAL_AND_FLACTIONAL_TOWARDS_NEGATIVE_CASES) {
                if (value < 0.0) continue
                _assert(intandfrac.integral, value.integralPart())
            }
        }
        // test towardsNegative ... omitted
    }

    @Test
    fun test_Double_fractionalPart() {
        // test cases
        FPConvExact.run{
            for ((value, intandfrac) in DOUBLE_TO_INTEGRAL_AND_FLACTIONAL_TOWARDS_NEGATIVE_CASES) {
                if (value < 0.0) continue
                _assert(intandfrac.fractional, value.fractionalPart())
            }
        }
        // test towardsNegative ... omitted
    }

    @Test
    fun test_Double_hasNonZeroFraction() {
        // test cases
        FPConvExact.run{
            for ((value, intandfrac) in DOUBLE_TO_INTEGRAL_AND_FLACTIONAL_TOWARDS_NEGATIVE_CASES) {
                if (value < 0.0) continue
                assertEquals(intandfrac.fractional != 0.0, value.hasNonZeroFraction())
            }
        }
        // test towardsNegative ... omitted
    }

    @Test
    fun test_Float_toIntegralAndFractional() {
        fun _assert(
            expected: FPConvExact.IntegralAndFractional<Float>,
            actual:   FPConvExact.IntegralAndFractional<Float>
        ): FPConvExact.IntegralAndFractional<Float> {
            _assert(expected.integral,   actual.integral)
            _assert(expected.fractional, actual.fractional)
            return actual
        }
        // test cases
        FPConvExact.run{
            for ((value, intandfrac) in FLOAT_TO_INTEGRAL_AND_FLACTIONAL_TOWARDS_NEGATIVE_CASES) {
                if (value < 0.0F) continue
                _assert(-intandfrac, (-value).toIntegralAndFractional())
                _assert(+intandfrac, (+value).toIntegralAndFractional())
            }
        }
        // test towardsNegative
        FPConvExact.run{
            for ((value, intandfrac) in FLOAT_TO_INTEGRAL_AND_FLACTIONAL_TOWARDS_NEGATIVE_CASES) {
                _assert(intandfrac, value.toIntegralAndFractional(towardsNegative = true))
            }
        }
    }

    @Test
    fun test_Float_integralPart() {
        // test cases
        FPConvExact.run{
            for ((value, intandfrac) in FLOAT_TO_INTEGRAL_AND_FLACTIONAL_TOWARDS_NEGATIVE_CASES) {
                if (value < 0.0F) continue
                _assert(intandfrac.integral, value.integralPart())
            }
        }
        // test towardsNegative ... omitted
    }

    @Test
    fun test_Float_fractionalPart() {
        // test cases
        FPConvExact.run{
            for ((value, intandfrac) in FLOAT_TO_INTEGRAL_AND_FLACTIONAL_TOWARDS_NEGATIVE_CASES) {
                if (value < 0.0F) continue
                _assert(intandfrac.fractional, value.fractionalPart())
            }
        }
        // test towardsNegative ... omitted
    }

    @Test
    fun test_Float_hasNonZeroFraction() {
        // test cases
        FPConvExact.run{
            for ((value, intandfrac) in FLOAT_TO_INTEGRAL_AND_FLACTIONAL_TOWARDS_NEGATIVE_CASES) {
                if (value < 0.0F) continue
                assertEquals(intandfrac.fractional != 0.0F, value.hasNonZeroFraction())
            }
        }
        // test towardsNegative ... omitted
    }

    //----------------
    // Type conversions

    // double -> int

    @Test
    fun test_Double_toIntExact() {
        // test cases
        FPConvExact.run{
            for ((value, long_withfrac) in DOUBLE_TO_LONG_CD_INT_RANGE_CASES) {
                val (long, _) = long_withfrac
                assertEquals(long, value.toIntExact().toLong())
            }
            for ((value, _) in DOUBLE_TO_LONG_BE_EXACT_INTEGER_RANGE_CASES) {
                assertThrows<ArithmeticException>{ value.toIntExact() }
            }
            for ((value, _) in DOUBLE_TO_LONG_AF_LONG_RANGE_CASES) {
                assertThrows<ArithmeticException>{ value.toIntExact() }
            }
            for (value in DOUBLE_TO_LONG_OUT_OF_RANGE_CASES) {
                assertThrows<ArithmeticException>{ value.toIntExact() }
            }
        }
        // test preventTruncation
        FPConvExact.run{
            for ((value, long_withfrac) in DOUBLE_TO_LONG_CD_INT_RANGE_CASES) {
                val (long, withfrac) = long_withfrac
                if (! withfrac) {
                    assertEquals(long, value.toIntExact(preventTruncation = true).toLong())
                } else {
                    assertThrows<ArithmeticException>{ value.toIntExact(preventTruncation = true) }
                }
            }
        }
    }

    @Test
    fun test_Double_toIntOrNull() {
        // test cases
        FPConvExact.run{
            for ((value, long_withfrac) in DOUBLE_TO_LONG_CD_INT_RANGE_CASES) {
                val (long, _) = long_withfrac
                assertEquals(long, value.toIntOrNull()?.toLong())
            }
            for ((value, _) in DOUBLE_TO_LONG_BE_EXACT_INTEGER_RANGE_CASES) {
                assertNull(value.toIntOrNull())
            }
            for ((value, _) in DOUBLE_TO_LONG_AF_LONG_RANGE_CASES) {
                assertNull(value.toIntOrNull())
            }
            for (value in DOUBLE_TO_LONG_OUT_OF_RANGE_CASES) {
                assertNull(value.toIntOrNull())
            }
        }
        // test preventTruncation ... omitted
    }

    // double -> long

    @Test
    fun test_Double_toLongExact() {
        // test cases
        FPConvExact.run{
            for ((value, long_withfrac) in DOUBLE_TO_LONG_CD_INT_RANGE_CASES) {
                val (long, _) = long_withfrac
                assertEquals(long, value.toLongExact())
            }
            for ((value, long_withfrac) in DOUBLE_TO_LONG_BE_EXACT_INTEGER_RANGE_CASES) {
                val (long, _) = long_withfrac
                assertEquals(long, value.toLongExact())
            }
            for ((value, _) in DOUBLE_TO_LONG_AF_LONG_RANGE_CASES) {
                assertThrows<ArithmeticException>{ value.toLongExact() }
            }
            for (value in DOUBLE_TO_LONG_OUT_OF_RANGE_CASES) {
                assertThrows<ArithmeticException>{ value.toLongExact() }
            }
        }
        // test preventTruncation
        FPConvExact.run{
            for ((value, long_withfrac) in DOUBLE_TO_LONG_CD_INT_RANGE_CASES) {
                val (long, withfrac) = long_withfrac
                if (! withfrac) {
                    assertEquals(long, value.toLongExact(preventTruncation = true))
                } else {
                    assertThrows<ArithmeticException>{ value.toLongExact(preventTruncation = true) }
                }
            }
            for ((value, long_withfrac) in DOUBLE_TO_LONG_BE_EXACT_INTEGER_RANGE_CASES) {
                val (long, withfrac) = long_withfrac
                if (! withfrac) {
                    assertEquals(long, value.toLongExact(preventTruncation = true))
                } else {
                    assertThrows<ArithmeticException>{ value.toLongExact(preventTruncation = true) }
                }
            }
        }
        // test enableExtendedRange
        FPConvExact.run{
            for ((value, long) in DOUBLE_TO_LONG_AF_LONG_RANGE_CASES) {
                assertEquals(long, value.toLongExact(enableExtendedRange = true))
            }
        }
    }

    @Test
    fun test_Double_toLongOrNull() {
        // test cases
        FPConvExact.run{
            for ((value, long_withfrac) in DOUBLE_TO_LONG_CD_INT_RANGE_CASES) {
                val (long, _) = long_withfrac
                assertEquals(long, value.toLongOrNull())
            }
            for ((value, long_withfrac) in DOUBLE_TO_LONG_BE_EXACT_INTEGER_RANGE_CASES) {
                val (long, _) = long_withfrac
                assertEquals(long, value.toLongOrNull())
            }
            for ((value, _) in DOUBLE_TO_LONG_AF_LONG_RANGE_CASES) {
                assertNull(value.toLongOrNull())
            }
            for (value in DOUBLE_TO_LONG_OUT_OF_RANGE_CASES) {
                assertNull(value.toLongOrNull())
            }
        }
        // test preventTruncation/enableExtendedRange ... omitted
    }

    // float -> int

    @Test
    fun test_Float_toIntExact() {
        // test cases
        FPConvExact.run{
            for ((value, int_withfrac) in FLOAT_TO_INT_be_EXACT_INTEGER_RANGE_CASES) {
                val (int, _) = int_withfrac
                assertEquals(int, value.toIntExact())
            }
            for ((value, _) in FLOAT_TO_INT_af_INT_RANGE_CASES) {
                assertThrows<ArithmeticException>{ value.toIntExact() }
            }
            for (value in FLOAT_TO_INT_OUT_OF_RANGE_CASES) {
                assertThrows<ArithmeticException>{ value.toIntExact() }
            }
        }
        // test preventTruncation
        FPConvExact.run{
            for ((value, int_withfrac) in FLOAT_TO_INT_be_EXACT_INTEGER_RANGE_CASES) {
                val (int, withfrac) = int_withfrac
                if (! withfrac) {
                    assertEquals(int, value.toIntExact(preventTruncation = true))
                } else {
                    assertThrows<ArithmeticException>{ value.toIntExact(preventTruncation = true) }
                }
            }
        }
        // test enableExtendedRange
        FPConvExact.run{
            for ((value, int) in FLOAT_TO_INT_af_INT_RANGE_CASES) {
                assertEquals(int, value.toIntExact(enableExtendedRange = true))
            }
        }
    }

    @Test
    fun test_Float_toIntOrNull() {
        // test cases
        FPConvExact.run{
            for ((value, int_withfrac) in FLOAT_TO_INT_be_EXACT_INTEGER_RANGE_CASES) {
                val (int, _) = int_withfrac
                assertEquals(int, value.toIntOrNull())
            }
            for ((value, _) in FLOAT_TO_INT_af_INT_RANGE_CASES) {
                assertNull(value.toIntOrNull())
            }
            for (value in FLOAT_TO_INT_OUT_OF_RANGE_CASES) {
                assertNull(value.toIntOrNull())
            }
        }
        // test preventTruncation/enableExtendedRange ... omitted
    }

    // long -> double

    @Test
    fun test_Long_toDoubleExact() {
        // test cases
        FPConvExact.run{
            for ((value, long_withfrac) in DOUBLE_TO_LONG_CD_INT_RANGE_CASES) {
                val (long, withfrac) = long_withfrac
                if (withfrac) continue
                _assert(value, long.toDoubleExact())
            }
            for ((value, long_withfrac) in DOUBLE_TO_LONG_BE_EXACT_INTEGER_RANGE_CASES) {
                val (long, withfrac) = long_withfrac
                if (withfrac) continue
                _assert(value, long.toDoubleExact())
            }
            for ((_, long) in DOUBLE_TO_LONG_AF_LONG_RANGE_CASES) {
                assertThrows<ArithmeticException>{ long.toDoubleExact() }
            }
        }
    }

    @Test
    fun test_Long_toDoubleOrNull() {
        // test cases
        FPConvExact.run{
            for ((value, long_withfrac) in DOUBLE_TO_LONG_CD_INT_RANGE_CASES) {
                val (long, withfrac) = long_withfrac
                if (withfrac) continue
                _assert(value, long.toDoubleOrNull())
            }
            for ((value, long_withfrac) in DOUBLE_TO_LONG_BE_EXACT_INTEGER_RANGE_CASES) {
                val (long, withfrac) = long_withfrac
                if (withfrac) continue
                _assert(value, long.toDoubleOrNull())
            }
            for ((_, long) in DOUBLE_TO_LONG_AF_LONG_RANGE_CASES) {
                assertNull(long.toDoubleOrNull())
            }
        }
    }

    // int -> float

    @Test
    fun test_Int_toFloatExact() {
        // test cases
        FPConvExact.run{
            for ((value, int_withfrac) in FLOAT_TO_INT_be_EXACT_INTEGER_RANGE_CASES) {
                val (int, withfrac) = int_withfrac
                if (withfrac) continue
                _assert(value, int.toFloatExact())
            }
            for ((_, int) in FLOAT_TO_INT_af_INT_RANGE_CASES) {
                assertThrows<ArithmeticException>{ int.toFloatExact() }
            }
        }
    }

    @Test
    fun test_Int_toFloatOrNull() {
        // test cases
        FPConvExact.run{
            for ((value, int_withfrac) in FLOAT_TO_INT_be_EXACT_INTEGER_RANGE_CASES) {
                val (int, withfrac) = int_withfrac
                if (withfrac) continue
                _assert(value, int.toFloatOrNull())
            }
            for ((_, int) in FLOAT_TO_INT_af_INT_RANGE_CASES) {
                assertNull(int.toFloatOrNull())
            }
        }
    }

}

////