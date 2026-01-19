package com.ez2bg.anotherthread.util

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Simplex noise implementation for terrain variation.
 * Based on Stefan Gustavson's Java implementation of the improved Perlin noise algorithm.
 */
object SimplexNoise {
    // Gradient vectors for 2D
    private val grad2 = arrayOf(
        floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f), floatArrayOf(1f, -1f), floatArrayOf(-1f, -1f),
        floatArrayOf(1f, 0f), floatArrayOf(-1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(0f, -1f)
    )

    // Gradient vectors for 3D
    private val grad3 = arrayOf(
        floatArrayOf(1f, 1f, 0f), floatArrayOf(-1f, 1f, 0f), floatArrayOf(1f, -1f, 0f), floatArrayOf(-1f, -1f, 0f),
        floatArrayOf(1f, 0f, 1f), floatArrayOf(-1f, 0f, 1f), floatArrayOf(1f, 0f, -1f), floatArrayOf(-1f, 0f, -1f),
        floatArrayOf(0f, 1f, 1f), floatArrayOf(0f, -1f, 1f), floatArrayOf(0f, 1f, -1f), floatArrayOf(0f, -1f, -1f)
    )

    // Permutation table
    private val perm = IntArray(512)
    private val permMod8 = IntArray(512)
    private val permMod12 = IntArray(512)

    init {
        val p = intArrayOf(
            151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225,
            140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148,
            247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32,
            57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175,
            74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122,
            60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54,
            65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169,
            200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64,
            52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212,
            207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213,
            119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9,
            129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104,
            218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241,
            81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157,
            184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236, 205, 93,
            222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180
        )
        for (i in 0 until 512) {
            perm[i] = p[i and 255]
            permMod8[i] = perm[i] and 7
            permMod12[i] = perm[i] % 12
        }
    }

    // Skewing and unskewing factors for 2D
    private val F2 = 0.5f * (sqrt(3f) - 1f)
    private val G2 = (3f - sqrt(3f)) / 6f

    // Skewing and unskewing factors for 3D
    private val F3 = 1f / 3f
    private val G3 = 1f / 6f

    private fun dot2(g: FloatArray, x: Float, y: Float): Float {
        return g[0] * x + g[1] * y
    }

    private fun dot3(g: FloatArray, x: Float, y: Float, z: Float): Float {
        return g[0] * x + g[1] * y + g[2] * z
    }

    private fun fastFloor(x: Float): Int {
        val xi = x.toInt()
        return if (x < xi) xi - 1 else xi
    }

    /**
     * 2D Simplex noise function.
     * @param x X coordinate
     * @param y Y coordinate
     * @return Noise value in the range [-1, 1]
     */
    fun noise2D(x: Float, y: Float): Float {
        val n0: Float
        val n1: Float
        val n2: Float

        // Skew the input space to determine which simplex cell we're in
        val s = (x + y) * F2
        val i = fastFloor(x + s)
        val j = fastFloor(y + s)

        val t = (i + j) * G2
        val X0 = i - t // Unskew the cell origin back to (x,y) space
        val Y0 = j - t
        val x0 = x - X0 // The x,y distances from the cell origin
        val y0 = y - Y0

        // For the 2D case, the simplex shape is an equilateral triangle.
        // Determine which simplex we are in.
        val i1: Int
        val j1: Int // Offsets for second (middle) corner of simplex in (i,j) coords
        if (x0 > y0) {
            i1 = 1
            j1 = 0
        } else {
            i1 = 0
            j1 = 1
        }

        // A step of (1,0) in (i,j) means a step of (1-c,-c) in (x,y), and
        // a step of (0,1) in (i,j) means a step of (-c,1-c) in (x,y), where
        // c = (3-sqrt(3))/6
        val x1 = x0 - i1 + G2 // Offsets for middle corner in (x,y) unskewed coords
        val y1 = y0 - j1 + G2
        val x2 = x0 - 1f + 2f * G2 // Offsets for last corner in (x,y) unskewed coords
        val y2 = y0 - 1f + 2f * G2

        // Work out the hashed gradient indices of the three simplex corners
        val ii = i and 255
        val jj = j and 255
        val gi0 = permMod8[ii + perm[jj]]
        val gi1 = permMod8[ii + i1 + perm[jj + j1]]
        val gi2 = permMod8[ii + 1 + perm[jj + 1]]

        // Calculate the contribution from the three corners
        var t0 = 0.5f - x0 * x0 - y0 * y0
        if (t0 < 0) {
            n0 = 0f
        } else {
            t0 *= t0
            n0 = t0 * t0 * dot2(grad2[gi0], x0, y0)
        }

        var t1 = 0.5f - x1 * x1 - y1 * y1
        if (t1 < 0) {
            n1 = 0f
        } else {
            t1 *= t1
            n1 = t1 * t1 * dot2(grad2[gi1], x1, y1)
        }

        var t2 = 0.5f - x2 * x2 - y2 * y2
        if (t2 < 0) {
            n2 = 0f
        } else {
            t2 *= t2
            n2 = t2 * t2 * dot2(grad2[gi2], x2, y2)
        }

        // Add contributions from each corner to get the final noise value.
        // The result is scaled to return values in the interval [-1,1].
        return 70f * (n0 + n1 + n2)
    }

    /**
     * 3D Simplex noise function.
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate (can be used for time-based animation)
     * @return Noise value in the range [-1, 1]
     */
    fun noise3D(x: Float, y: Float, z: Float): Float {
        val n0: Float
        val n1: Float
        val n2: Float
        val n3: Float

        // Skew the input space to determine which simplex cell we're in
        val s = (x + y + z) * F3
        val i = fastFloor(x + s)
        val j = fastFloor(y + s)
        val k = fastFloor(z + s)

        val t = (i + j + k) * G3
        val X0 = i - t
        val Y0 = j - t
        val Z0 = k - t
        val x0 = x - X0
        val y0 = y - Y0
        val z0 = z - Z0

        // Determine which simplex we are in
        val i1: Int
        val j1: Int
        val k1: Int
        val i2: Int
        val j2: Int
        val k2: Int

        if (x0 >= y0) {
            if (y0 >= z0) {
                i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0
            } else if (x0 >= z0) {
                i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1
            } else {
                i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1
            }
        } else {
            if (y0 < z0) {
                i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1
            } else if (x0 < z0) {
                i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1
            } else {
                i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0
            }
        }

        val x1 = x0 - i1 + G3
        val y1 = y0 - j1 + G3
        val z1 = z0 - k1 + G3
        val x2 = x0 - i2 + 2f * G3
        val y2 = y0 - j2 + 2f * G3
        val z2 = z0 - k2 + 2f * G3
        val x3 = x0 - 1f + 3f * G3
        val y3 = y0 - 1f + 3f * G3
        val z3 = z0 - 1f + 3f * G3

        val ii = i and 255
        val jj = j and 255
        val kk = k and 255
        val gi0 = permMod12[ii + perm[jj + perm[kk]]]
        val gi1 = permMod12[ii + i1 + perm[jj + j1 + perm[kk + k1]]]
        val gi2 = permMod12[ii + i2 + perm[jj + j2 + perm[kk + k2]]]
        val gi3 = permMod12[ii + 1 + perm[jj + 1 + perm[kk + 1]]]

        var t0 = 0.6f - x0 * x0 - y0 * y0 - z0 * z0
        if (t0 < 0) {
            n0 = 0f
        } else {
            t0 *= t0
            n0 = t0 * t0 * dot3(grad3[gi0], x0, y0, z0)
        }

        var t1 = 0.6f - x1 * x1 - y1 * y1 - z1 * z1
        if (t1 < 0) {
            n1 = 0f
        } else {
            t1 *= t1
            n1 = t1 * t1 * dot3(grad3[gi1], x1, y1, z1)
        }

        var t2 = 0.6f - x2 * x2 - y2 * y2 - z2 * z2
        if (t2 < 0) {
            n2 = 0f
        } else {
            t2 *= t2
            n2 = t2 * t2 * dot3(grad3[gi2], x2, y2, z2)
        }

        var t3 = 0.6f - x3 * x3 - y3 * y3 - z3 * z3
        if (t3 < 0) {
            n3 = 0f
        } else {
            t3 *= t3
            n3 = t3 * t3 * dot3(grad3[gi3], x3, y3, z3)
        }

        return 32f * (n0 + n1 + n2 + n3)
    }

    /**
     * Fractal/Turbulence noise by combining multiple octaves.
     * @param x X coordinate
     * @param y Y coordinate
     * @param octaves Number of noise layers to combine (typically 4-8)
     * @param persistence How much each octave contributes (typically 0.5)
     * @param scale Base scale/frequency of the noise
     * @return Combined noise value in range approximately [-1, 1]
     */
    fun fractalNoise2D(
        x: Float,
        y: Float,
        octaves: Int = 4,
        persistence: Float = 0.5f,
        scale: Float = 1f
    ): Float {
        var total = 0f
        var frequency = scale
        var amplitude = 1f
        var maxValue = 0f

        for (i in 0 until octaves) {
            total += noise2D(x * frequency, y * frequency) * amplitude
            maxValue += amplitude
            amplitude *= persistence
            frequency *= 2f
        }

        return total / maxValue
    }

    /**
     * Normalized noise that returns values in [0, 1] range.
     */
    fun noise2DNormalized(x: Float, y: Float): Float {
        return (noise2D(x, y) + 1f) / 2f
    }

    /**
     * Normalized fractal noise that returns values in [0, 1] range.
     */
    fun fractalNoise2DNormalized(
        x: Float,
        y: Float,
        octaves: Int = 4,
        persistence: Float = 0.5f,
        scale: Float = 1f
    ): Float {
        return (fractalNoise2D(x, y, octaves, persistence, scale) + 1f) / 2f
    }
}
