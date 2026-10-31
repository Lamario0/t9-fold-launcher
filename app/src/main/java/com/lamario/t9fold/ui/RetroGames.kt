package com.lamario.t9fold.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.random.Random

/** The idle screen: self-playing pixel game demos on an 84x48 grid. Tap to switch game. */
private const val GW = 84
private const val GH = 48

private class Frame {
    val px = BooleanArray(GW * GH)
    fun clear() = px.fill(false)
    fun set(x: Int, y: Int) {
        if (x in 0 until GW && y in 0 until GH) px[y * GW + x] = true
    }
    fun rect(x: Int, y: Int, w: Int, h: Int) {
        for (yy in y until y + h) for (xx in x until x + w) set(xx, yy)
    }
    fun outline(x: Int, y: Int, w: Int, h: Int) {
        for (xx in x until x + w) { set(xx, y); set(xx, y + h - 1) }
        for (yy in y until y + h) { set(x, yy); set(x + w - 1, yy) }
    }
    /** Draws a sprite given as rows of '#' and '.'. */
    fun sprite(x: Int, y: Int, rows: List<String>) {
        rows.forEachIndexed { dy, row -> row.forEachIndexed { dx, c -> if (c == '#') set(x + dx, y + dy) } }
    }
}

private interface Demo {
    val tickMs: Long
    fun step()
    fun draw(f: Frame)
}

@Composable
fun RetroGames(modifier: Modifier = Modifier) {
    val frame = remember { Frame() }
    var game by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    val demo = remember(game) {
        when (game % 3) {
            0 -> SnakeDemo()
            1 -> BrickDemo()
            else -> ShooterDemo()
        }
    }

    // Driven by the frame clock, so it pauses by itself when the screen is off or the app is hidden.
    LaunchedEffect(demo) {
        var last = withFrameMillis { it }
        val started = last
        while (true) {
            val now = withFrameMillis { it }
            if (now - last >= demo.tickMs) {
                demo.step()
                last = now
                tick++
            }
            if (now - started > 30_000) {
                game++
                break
            }
        }
    }

    val ink = Retro.Ink
    val faint = Retro.InkFaint
    Canvas(
        modifier.pointerInput(Unit) { detectTapGestures { game++ } },
    ) {
        if (tick < 0) return@Canvas // reading tick redraws the canvas on every game step
        frame.clear()
        demo.draw(frame)
        val cell = minOf(size.width / GW, size.height / GH)
        val ox = (size.width - cell * GW) / 2
        val oy = (size.height - cell * GH) / 2
        val dot = Size(cell * 0.86f, cell * 0.86f)
        drawRect(faint, Offset(ox, oy), Size(cell * GW, cell * GH), alpha = 0.25f)
        for (y in 0 until GH) for (x in 0 until GW) {
            if (frame.px[y * GW + x]) drawRect(ink, Offset(ox + x * cell, oy + y * cell), dot)
        }
    }
}

// ---- Snake ---------------------------------------------------------------------------------

private class SnakeDemo : Demo {
    override val tickMs = 85L
    private val cols = 27
    private val rows = 15
    private val rnd = Random(System.nanoTime())
    private val body = ArrayDeque<Pair<Int, Int>>()
    private var dir = 1 to 0
    private var food = 0 to 0

    init { reset() }

    private fun reset() {
        body.clear()
        for (i in 0 until 4) body.addFirst(3 + i to rows / 2)
        dir = 1 to 0
        placeFood()
    }

    private fun placeFood() {
        do {
            food = rnd.nextInt(cols) to rnd.nextInt(rows)
        } while (food in body)
    }

    private fun free(c: Pair<Int, Int>) = c.first in 0 until cols && c.second in 0 until rows && c !in body.dropLast(1)

    override fun step() {
        val head = body.first()
        val options = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
            .filter { it.first != -dir.first || it.second != -dir.second }
            .map { d -> d to (head.first + d.first to head.second + d.second) }
            .filter { free(it.second) }
        if (options.isEmpty()) return reset()
        // Head for the food, but sometimes wander so it doesn't look robotic.
        val pick = if (rnd.nextInt(10) == 0) options.random(rnd) else options.minBy { (_, c) ->
            abs(c.first - food.first) + abs(c.second - food.second) - freedom(c) * 0.5
        }
        dir = pick.first
        body.addFirst(pick.second)
        if (pick.second == food) {
            if (body.size > 60) reset() else placeFood()
        } else {
            body.removeLast()
        }
    }

    private fun freedom(c: Pair<Int, Int>) =
        listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1).count { free(c.first + it.first to c.second + it.second) }.toDouble()

    override fun draw(f: Frame) {
        f.outline(0, 0, GW - 1, GH - 1)
        val ox = 1
        val oy = 1
        for ((x, y) in body) f.rect(ox + x * 3, oy + y * 3, 3, 3)
        val (hx, hy) = body.first()
        f.px[(oy + hy * 3 + 1) * GW + ox + hx * 3 + 1] = false // an eye
        val (fx, fy) = food
        f.set(ox + fx * 3 + 1, oy + fy * 3)
        f.set(ox + fx * 3, oy + fy * 3 + 1)
        f.set(ox + fx * 3 + 2, oy + fy * 3 + 1)
        f.set(ox + fx * 3 + 1, oy + fy * 3 + 2)
    }
}

// ---- Brick breaker -------------------------------------------------------------------------

private class BrickDemo : Demo {
    override val tickMs = 28L
    private val brickCols = 10
    private val brickRows = 4
    private val bricks = BooleanArray(brickCols * brickRows)
    private var ballX = 0f
    private var ballY = 0f
    private var vx = 0f
    private var vy = 0f
    private var paddle = 36f
    private val rnd = Random(System.nanoTime())

    init { reset() }

    private fun reset() {
        bricks.fill(true)
        ballX = 42f; ballY = 36f
        vx = if (rnd.nextBoolean()) 0.9f else -0.9f
        vy = -0.9f
    }

    override fun step() {
        ballX += vx; ballY += vy
        if (ballX <= 1f || ballX >= GW - 3f) { vx = -vx; ballX = ballX.coerceIn(1f, GW - 3f) }
        if (ballY <= 1f) { vy = -vy; ballY = 1f }
        // Bricks: 7x3 with a 1px gap, starting at (2, 4).
        val col = ((ballX + 1 - 2) / 8).toInt()
        val row = ((ballY + 1 - 4) / 4).toInt()
        if (row in 0 until brickRows && col in 0 until brickCols && bricks[row * brickCols + col]) {
            bricks[row * brickCols + col] = false
            vy = -vy
        }
        // The paddle chases the ball at a limited speed.
        val target = ballX - 6 + (if (vx > 0) 2 else -2)
        paddle += (target - paddle).coerceIn(-1.1f, 1.1f)
        paddle = paddle.coerceIn(0f, GW - 12f)
        if (ballY >= 43f && vy > 0) {
            if (ballX + 2 >= paddle && ballX <= paddle + 12) {
                vy = -vy
                vx = ((ballX + 1 - (paddle + 6)) / 6f * 1.2f).coerceIn(-1.2f, 1.2f).let { if (abs(it) < 0.4f) 0.5f * (if (vx >= 0) 1 else -1) else it }
            } else if (ballY > GH) {
                reset()
            }
        }
        if (bricks.none { it }) reset()
    }

    override fun draw(f: Frame) {
        for (r in 0 until brickRows) for (c in 0 until brickCols) {
            if (bricks[r * brickCols + c]) f.rect(2 + c * 8, 4 + r * 4, 7, 3)
        }
        f.rect(ballX.toInt(), ballY.toInt(), 2, 2)
        f.rect(paddle.toInt(), 45, 12, 2)
    }
}

// ---- Space shooter -------------------------------------------------------------------------

private class ShooterDemo : Demo {
    override val tickMs = 45L
    private val rnd = Random(System.nanoTime())
    private val aliens = mutableListOf<Pair<Int, Int>>() // grid col,row
    private var fleetX = 4
    private var fleetY = 3
    private var fleetDir = 1
    private var ship = 40
    private val shots = mutableListOf<IntArray>() // x, y
    private val bombs = mutableListOf<IntArray>()
    private val booms = mutableListOf<IntArray>() // x, y, ttl
    private var t = 0

    private val alienA = listOf(".#...#.", "..###..", ".#####.", "##.#.##", "#.....#")
    private val alienB = listOf("..###..", ".#####.", "#.#.#.#", ".#####.", ".#...#.")
    private val shipSprite = listOf("...#...", "..###..", "#######")

    init { reset() }

    private fun reset() {
        aliens.clear()
        for (r in 0 until 3) for (c in 0 until 6) aliens += c to r
        fleetX = 4; fleetY = 3; fleetDir = 1
        shots.clear(); bombs.clear()
    }

    private fun alienPos(a: Pair<Int, Int>) = (fleetX + a.first * 10) to (fleetY + a.second * 7)

    override fun step() {
        t++
        if (t % 6 == 0) {
            val minX = aliens.minOfOrNull { alienPos(it).first } ?: 0
            val maxX = aliens.maxOfOrNull { alienPos(it).first + 7 } ?: GW
            if ((fleetDir > 0 && maxX >= GW - 1) || (fleetDir < 0 && minX <= 1)) {
                fleetDir = -fleetDir
                fleetY++
            } else {
                fleetX += fleetDir
            }
        }
        // Ship lines up under the nearest alien and fires.
        val target = aliens.minByOrNull { abs(alienPos(it).first + 3 - (ship + 3)) }
        if (target != null) {
            val tx = alienPos(target).first
            ship += (tx - ship).coerceIn(-1, 1)
            if (t % 9 == 0 && shots.size < 2) shots += intArrayOf(ship + 3, 40)
        }
        if (t % 23 == 0 && aliens.isNotEmpty()) {
            val (x, y) = alienPos(aliens.random(rnd))
            bombs += intArrayOf(x + 3, y + 6)
        }
        shots.forEach { it[1] -= 2 }
        bombs.forEach { it[1] += 1 }
        shots.removeAll { s ->
            val hit = aliens.firstOrNull { a ->
                val (x, y) = alienPos(a)
                s[0] in x until x + 7 && s[1] in y until y + 5
            }
            if (hit != null) {
                aliens.remove(hit)
                val (x, y) = alienPos(hit)
                booms += intArrayOf(x, y, 5)
            }
            hit != null || s[1] < 0
        }
        bombs.removeAll { it[1] > GH }
        booms.forEach { it[2]-- }
        booms.removeAll { it[2] <= 0 }
        val lowest = aliens.maxOfOrNull { alienPos(it).second + 5 } ?: 0
        if (aliens.isEmpty() || lowest >= 38) reset()
    }

    override fun draw(f: Frame) {
        val frameB = (t / 8) % 2 == 0
        for (a in aliens) {
            val (x, y) = alienPos(a)
            f.sprite(x, y, if (frameB) alienA else alienB)
        }
        for (b in booms) {
            f.set(b[0] + 1, b[1]); f.set(b[0] + 5, b[1]); f.set(b[0] + 3, b[1] + 2)
            f.set(b[0], b[1] + 4); f.set(b[0] + 6, b[1] + 4)
        }
        shots.forEach { f.rect(it[0], it[1], 1, 2) }
        bombs.forEach { f.set(it[0], it[1]); f.set(it[0] + (if ((it[1] / 2) % 2 == 0) 1 else -1), it[1] + 1) }
        f.sprite(ship, 44, shipSprite)
    }
}
