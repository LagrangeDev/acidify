package org.ntqqrev.acidify.internal.crypto.ecdh

import org.ntqqrev.acidify.internal.crypto.hash.MD5
import org.ntqqrev.acidify.internal.math.BigInt
import org.ntqqrev.acidify.internal.math.ModularOps
import kotlin.random.Random

/**
 * ECDH (Elliptic Curve Diffie-Hellman) key exchange provider
 */
internal class EcdhProvider {
    private val curve: EllipticCurve
    private val secret: BigInt
    val publicPoint: EllipticPoint

    companion object {
        private val random = Random.Default

        /**
         * Create a random secret in the range [1, n-1]
         */
        private fun createSecret(curve: EllipticCurve): BigInt {
            val randomBytes = ByteArray(curve.size)
            var result: BigInt

            do {
                // Fill with random bytes
                random.nextBytes(randomBytes)

                // Convert to BigInt
                result = BigInt.fromBytes(randomBytes, false)

                // Ensure it's in the valid range [1, n-1]
                result = (result % (curve.n - BigInt.ONE)) + BigInt.ONE

            } while (result.isZero() || result >= curve.n)

            return result
        }

        /**
         * Unpack secret key from byte array
         */
        private fun unpackSecret(ecSec: ByteArray): BigInt {
            if (ecSec.size < 4) {
                throw IllegalArgumentException("Invalid secret key format")
            }

            // Read length from byte at index 3
            val length = ecSec[3].toInt() and 0xFF

            if (ecSec.size - 4 != length) {
                throw IllegalArgumentException("Length does not match")
            }

            // Extract the actual secret bytes (big-endian, unsigned)
            return BigInt.fromBytes(ecSec.sliceArray(4 until ecSec.size), true)
        }
    }

    /**
     * Constructor with curve - generates new key pair
     */
    constructor(curve: EllipticCurve) {
        this.curve = curve
        this.secret = createSecret(curve)
        this.publicPoint = createPublic()
    }

    /**
     * Constructor with curve and existing secret
     */
    constructor(curve: EllipticCurve, secret: ByteArray) {
        this.curve = curve
        this.secret = unpackSecret(secret)
        this.publicPoint = createPublic()
    }

    /**
     * Create public key from secret: public = secret * G
     */
    private fun createPublic(): EllipticPoint {
        val result = createShared(secret, curve.g)

        // Validate that the generated public key is on the curve
        if (!result.isInfinity() && !curve.checkOn(result)) {
            throw RuntimeException("Generated public key is not on the curve")
        }

        return result
    }

    /**
     * Point addition on elliptic curve
     * Fixed implementation with proper modular arithmetic
     */
    fun pointAdd(p1: EllipticPoint, p2: EllipticPoint): EllipticPoint {
        // Handle point at infinity cases
        if (p1.isInfinity()) return p2
        if (p2.isInfinity()) return p1

        // Use coordinates directly - they should already be in proper range
        val x1 = p1.x
        val x2 = p2.x
        val y1 = p1.y
        val y2 = p2.y

        val m: BigInt

        if (x1 == x2) {
            if (y1 == y2) {
                // Point doubling: m = (3*x1^2 + a) / (2*y1)
                val x1Squared = ModularOps.modMul(x1, x1, curve.p)
                val threeX1Squared = ModularOps.modMul(
                    BigInt(3), x1Squared, curve.p)
                val numerator = ModularOps.modAdd(threeX1Squared, curve.a, curve.p)
                val denominator = ModularOps.modMul(
                    BigInt.TWO, y1, curve.p)

                if (denominator.isZero()) {
                    return EllipticPoint()  // Point at infinity (vertical tangent)
                }

                val invOpt = ModularOps.modInverse(denominator, curve.p)
                if (invOpt == null) {
                    return EllipticPoint()  // Point at infinity
                }
                m = ModularOps.modMul(numerator, invOpt, curve.p)
            } else {
                // Points are inverses (x1 == x2 but y1 != y2) - return infinity
                return EllipticPoint()
            }
        } else {
            // Point addition: m = (y2 - y1) / (x2 - x1)
            val numerator = ModularOps.modSub(y2, y1, curve.p)
            val denominator = ModularOps.modSub(x2, x1, curve.p)

            if (denominator.isZero()) {
                return EllipticPoint()  // Point at infinity
            }

            val invOpt = ModularOps.modInverse(denominator, curve.p)
            if (invOpt == null) {
                return EllipticPoint()  // Point at infinity
            }
            m = ModularOps.modMul(numerator, invOpt, curve.p)
        }

        // Calculate new point: xr = m^2 - x1 - x2
        val mSquared = ModularOps.modMul(m, m, curve.p)
        val xr = ModularOps.modSub(
            ModularOps.modSub(mSquared, x1, curve.p),
            x2,
            curve.p
        )

        // yr = m*(x1 - xr) - y1
        val xDiff = ModularOps.modSub(x1, xr, curve.p)
        val yr = ModularOps.modSub(
            ModularOps.modMul(m, xDiff, curve.p),
            y1,
            curve.p
        )

        // Normalize result coordinates
        val finalX = normalizeCoordinate(xr, curve.p)
        val finalY = normalizeCoordinate(yr, curve.p)

        return EllipticPoint(finalX, finalY)
    }

    /**
     * Normalize a coordinate to the range [0, p)
     */
    private fun normalizeCoordinate(coord: BigInt, p: BigInt): BigInt {
        var result = coord % p
        if (result.isNegative()) {
            result = result + p
        }
        return result
    }


    /**
     * Scalar multiplication using double-and-add algorithm
     * Computes k * P where k is a scalar and P is a point
     */
    fun createShared(ecSec: BigInt, ecPub: EllipticPoint): EllipticPoint {
        // Handle special cases
        if (ecPub.isInfinity()) {
            return EllipticPoint()  // k * O = O for any k
        }

        // Reduce scalar modulo curve order to avoid unnecessary operations
        var scalar = ecSec % curve.n
        if (scalar.isZero()) {
            return EllipticPoint()  // 0 * P = O
        }

        // Handle negative scalars
        if (scalar.isNegative()) {
            scalar = scalar + curve.n  // Convert to positive equivalent
        }

        // Verify input point is on the curve (re-enable validation)
        if (!curve.checkOn(ecPub)) {
            throw IllegalArgumentException("Input point is not on the curve")
        }

        var result =
            EllipticPoint()  // Start with point at infinity (identity)
        var addend =
            EllipticPoint(ecPub.x, ecPub.y)  // Copy the input point
        var currentScalar = scalar

        // Double-and-add algorithm (binary method)
        while (!currentScalar.isZero()) {
            if (currentScalar.isOdd()) {
                result = pointAdd(result, addend)
            }

            // Double the addend for the next bit
            addend = pointAdd(addend, addend)

            // Shift scalar right by 1 bit
            currentScalar = currentScalar shr 1
        }

        // Validate that the result is on the curve
        if (!result.isInfinity() && !curve.checkOn(result)) {
            throw RuntimeException("Calculated point is not on the curve - this indicates a bug in point addition")
        }

        return result
    }

    /**
     * Pack shared secret (with optional MD5 hash)
     */
    private fun packShared(ecShared: EllipticPoint, isHash: Boolean): ByteArray {
        val xBytes = ecShared.x.toFixedBytes(curve.size)
        return if (isHash) MD5.hash(xBytes.copyOfRange(0, curve.packSize)) else xBytes
    }

    private fun BigInt.toFixedBytes(size: Int): ByteArray {
        val bytes = toBytes(true)
        if (bytes.size == size) return bytes

        return ByteArray(size).also { result ->
            if (bytes.size < size) {
                bytes.copyInto(result, size - bytes.size)
            } else {
                bytes.copyInto(result, 0, bytes.size - size, bytes.size)
            }
        }
    }

    /**
     * Key exchange with another party's public key
     */
    fun keyExchange(ecPub: ByteArray, isHash: Boolean): ByteArray {
        val bobPublic = unpackPublic(ecPub)
        val shared = createShared(secret, bobPublic)
        return packShared(shared, isHash)
    }

    /**
     * Pack public key (compressed or uncompressed format)
     */
    fun packPublic(compress: Boolean): ByteArray {
        return if (compress) {
            // Compressed format: 0x02/0x03 + x-coordinate
            val result = ByteArray(curve.size + 1)

            // Determine prefix based on y-coordinate parity
            // 0x02 if y is even, 0x03 if y is odd
            val yIsEven = publicPoint.y.isEven()
            val yIsNegative = publicPoint.y.isNegative()
            result[0] = if (yIsEven != yIsNegative) 0x02 else 0x03

            // Add x-coordinate bytes
            var xBytes = publicPoint.x.toBytes(true)
            if (xBytes.size > curve.size) {
                xBytes = xBytes.sliceArray(0 until curve.size)
            }

            // Copy with padding if necessary
            val offset = 1 + (curve.size - xBytes.size)
            for (i in 1 until offset) {
                result[i] = 0
            }
            xBytes.copyInto(result, offset, 0, xBytes.size)

            result
        } else {
            // Uncompressed format: 0x04 + x-coordinate + y-coordinate
            val result = ByteArray(curve.size * 2 + 1)
            result[0] = 0x04

            // Add x-coordinate
            var xBytes = publicPoint.x.toBytes(true)
            if (xBytes.size > curve.size) {
                xBytes = xBytes.sliceArray(0 until curve.size)
            }
            val xOffset = 1 + (curve.size - xBytes.size)
            for (i in 1 until xOffset) {
                result[i] = 0
            }
            xBytes.copyInto(result, xOffset, 0, xBytes.size)

            // Add y-coordinate
            var yBytes = publicPoint.y.toBytes(true)
            if (yBytes.size > curve.size) {
                yBytes = yBytes.sliceArray(0 until curve.size)
            }
            val yOffset = 1 + curve.size + (curve.size - yBytes.size)
            for (i in (1 + curve.size) until yOffset) {
                result[i] = 0
            }
            yBytes.copyInto(result, yOffset, 0, yBytes.size)

            result
        }
    }

    /**
     * Pack secret key in the format: [4 bytes header] + secret bytes
     */
    fun packSecret(): ByteArray {
        val secretBytes = secret.toBytes(true)
        val rawLength = secretBytes.size

        val result = ByteArray(rawLength + 4)
        // First 3 bytes are padding (0)
        result[0] = 0
        result[1] = 0
        result[2] = 0
        // 4th byte is the length
        result[3] = rawLength.toByte()

        // Copy secret bytes
        secretBytes.copyInto(result, 4, 0, secretBytes.size)

        return result
    }

    /**
     * Unpack public key from byte array
     */
    fun unpackPublic(publicKey: ByteArray): EllipticPoint {
        val length = publicKey.size

        if (length != curve.size * 2 + 1 && length != curve.size + 1) {
            throw IllegalArgumentException("Length does not match")
        }

        return when (publicKey[0]) {
            0x04.toByte() -> {
                // Uncompressed format
                if (length != curve.size * 2 + 1) {
                    throw IllegalArgumentException("Invalid uncompressed public key length")
                }

                val px = BigInt.fromBytes(publicKey.sliceArray(1..curve.size), true)
                val py = BigInt.fromBytes(
                    publicKey.sliceArray((1 + curve.size)..(curve.size * 2)),
                    true
                )

                EllipticPoint(px, py)
            }

            0x02.toByte(), 0x03.toByte() -> {
                // Compressed format - need to recover y from x
                if (length != curve.size + 1) {
                    throw IllegalArgumentException("Invalid compressed public key length")
                }

                val px = BigInt.fromBytes(publicKey.sliceArray(1..curve.size), true)

                // Calculate y^2 = x^3 + ax + b (mod p)
                val x3 = ModularOps.modMul(px, ModularOps.modMul(px, px, curve.p), curve.p)
                val ax = ModularOps.modMul(curve.a, px, curve.p)
                var right = ModularOps.modAdd(x3, ax, curve.p)
                right = ModularOps.modAdd(right, curve.b, curve.p)

                // For p ≡ 3 (mod 4), we can use: y = right^((p+1)/4) mod p
                val tmp = (curve.p + BigInt.ONE) shr 2
                var py = ModularOps.modExp(right, tmp, curve.p)

                // Check if we need to negate y based on the prefix byte
                val shouldBeEven = (publicKey[0] == 0x02.toByte())
                if (py.isEven() != shouldBeEven) {
                    py = curve.p - py
                }

                EllipticPoint(px, py)
            }

            else -> throw IllegalArgumentException("Invalid public key format")
        }
    }
}
