package com.ez2bg.anotherthread.worldgen

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Simplex noise implementation for terrain generation.
 * Based on Stefan Gustavson's Java implementation.
 */
object SimplexNoise {
    // Gradient vectors for 2D
    private val grad2 = arrayOf(
        floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f), floatArrayOf(1f, -1f), floatArrayOf(-1f, -1f),
        floatArrayOf(1f, 0f), floatArrayOf(-1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(0f, -1f)
    )

    // Permutation table
    private val perm = IntArray(512)
    private val permMod8 = IntArray(512)

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
        }
    }

    // Skewing factors for 2D
    private val F2 = 0.5f * (sqrt(3f) - 1f)
    private val G2 = (3f - sqrt(3f)) / 6f

    private fun dot2(g: FloatArray, x: Float, y: Float): Float {
        return g[0] * x + g[1] * y
    }

    private fun fastFloor(x: Float): Int {
        val xi = x.toInt()
        return if (x < xi) xi - 1 else xi
    }

    /**
     * 2D Simplex noise function.
     * @return Noise value in the range [-1, 1]
     */
    fun noise2D(x: Float, y: Float): Float {
        val n0: Float
        val n1: Float
        val n2: Float

        val s = (x + y) * F2
        val i = fastFloor(x + s)
        val j = fastFloor(y + s)

        val t = (i + j) * G2
        val X0 = i - t
        val Y0 = j - t
        val x0 = x - X0
        val y0 = y - Y0

        val i1: Int
        val j1: Int
        if (x0 > y0) {
            i1 = 1; j1 = 0
        } else {
            i1 = 0; j1 = 1
        }

        val x1 = x0 - i1 + G2
        val y1 = y0 - j1 + G2
        val x2 = x0 - 1f + 2f * G2
        val y2 = y0 - 1f + 2f * G2

        val ii = i and 255
        val jj = j and 255
        val gi0 = permMod8[ii + perm[jj]]
        val gi1 = permMod8[ii + i1 + perm[jj + j1]]
        val gi2 = permMod8[ii + 1 + perm[jj + 1]]

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

        return 70f * (n0 + n1 + n2)
    }

    /**
     * Fractal noise by combining multiple octaves.
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
     * Normalized noise in [0, 1] range.
     */
    fun noise2DNormalized(x: Float, y: Float): Float {
        return (noise2D(x, y) + 1f) / 2f
    }

    /**
     * Seeded noise - offset coordinates by seed for different patterns.
     */
    fun seededNoise2D(x: Float, y: Float, seed: Long): Float {
        val seedOffset = (seed % 10000).toFloat()
        return noise2D(x + seedOffset, y + seedOffset * 1.5f)
    }

    fun seededFractalNoise2D(
        x: Float,
        y: Float,
        seed: Long,
        octaves: Int = 4,
        persistence: Float = 0.5f,
        scale: Float = 1f
    ): Float {
        val seedOffset = (seed % 10000).toFloat()
        return fractalNoise2D(x + seedOffset, y + seedOffset * 1.5f, octaves, persistence, scale)
    }
}
