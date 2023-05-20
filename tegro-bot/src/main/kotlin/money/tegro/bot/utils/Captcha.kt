package money.tegro.bot.utils

import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.random.Random.Default.nextBoolean
import kotlin.random.Random.Default.nextDouble
import kotlin.random.Random.Default.nextInt

class Captcha {

    var answer = ""
    var image: InputStream? = null

    private val captcha = object {
        var width = 250
        var height = 38
        var path: String? = null
        var background: Color? = Color.black
    }

    private val noise = object {
        var isAdded = false
        var fill = false
        var color: Color? = null
    }

    private val font = object {
        var size = 24
        var name = "Serif"
        var style = Font.BOLD
    }

    private val text = object {
        var length = 6
        var color: Color? = Color.black
        var chars = ('a'..'z').toList() + ('0'..'9').toList()
    }

    private val line = object {
        var isAdded = false
        var vertical = 5
        var horizontal = 5
        var width = 1
        var color: Color? = text.color
    }

    fun build(): Captcha {
        fun randCaptcha() = (0 until text.length).map { text.chars.random() }.joinToString("")
        fun randColor() = Color(nextInt(16581375))
        answer = randCaptcha()

        if (text.length <= 0) throw Exception("The text size must not be less than a character")
        if (captcha.width < font.size * text.length / 4.0) throw Exception("The captcha width should not be smaller than the text size")
        if (captcha.height < font.size) throw Exception("The captcha height should not be smaller than the text size")

        val positionX = captcha.width / 2.0 - (font.size * (text.length / 4.0))
        val positionY = captcha.height / 2.0 + (font.size / 2.0)

        val buffer = BufferedImage(captcha.width, captcha.height, BufferedImage.TYPE_INT_ARGB)
        val graphic = buffer.createGraphics()

        graphic.color = captcha.background
        graphic.fillRect(0, 0, captcha.width, captcha.height)

        if (noise.isAdded) {
            for (y in 0 until captcha.height) {
                for (x in 0 until captcha.width) {
                    val color = noise.color ?: randColor()
                    val noiseColor = Color(
                        color.red,
                        color.green,
                        color.blue,
                        nextInt(256)
                    )
                    if (noise.fill) buffer.setRGB(x, y, noiseColor.rgb)
                    else if (nextInt(100) in 0..10) buffer.setRGB(x, y, noiseColor.rgb)
                }
            }
        }

        if (line.isAdded) {
            repeat(line.vertical) {
                graphic.color = line.color ?: randColor()
                val x = nextInt(captcha.width) to nextInt(captcha.width)
                repeat(line.width) { graphic.drawLine(x.first + it, 0, x.second + it, captcha.height) }
            }
            repeat(line.horizontal) {
                graphic.color = line.color ?: randColor()
                val y = nextInt(captcha.height) to nextInt(captcha.height)
                repeat(line.width) { graphic.drawLine(0, y.first + it, captcha.width, y.second + it) }
            }
        }

        graphic.font = Font(font.name, font.style, font.size)
        //val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("fonts/ArialRegular.ttf")
        //graphic.font = Font.createFont(Font.TRUETYPE_FONT, javaClass.getResourceAsStream("fonts/ArialRegular.ttf")).deriveFont(font.size.toLong().toFloat())
        graphic.color = text.color ?: randColor()
        answer.forEachIndexed { i, c ->
            val x = positionX + (i * (font.size / 2))
            val y = positionY - font.size * 0.2 + nextDouble(font.size * 0.2) * if (nextBoolean()) -1 else 1
            graphic.drawString(c.toString(), x.roundToInt(), y.roundToInt())
        }

        val baos = ByteArrayOutputStream()
        ImageIO.write(buffer, "png", baos)
        image = ByteArrayInputStream(baos.toByteArray())

        return this
    }

    fun builder(
        width: Int = captcha.width,
        height: Int = captcha.height,
        path: String? = captcha.path,
        background: Color? = captcha.background
    ): Captcha {
        captcha.width = width
        captcha.height = height
        captcha.path = path
        captcha.background = background
        return this
    }

    fun setFont(
        name: String = font.name,
        size: Int = font.size,
        style: Int = font.style
    ): Captcha {
        font.name = name
        font.size = size
        font.style = style
        return this
    }

    fun addLines(
        vertical: Int = line.vertical,
        horizontal: Int = line.horizontal,
        width: Int = line.width,
        color: Color? = line.color
    ): Captcha {
        line.isAdded = true
        line.vertical = vertical
        line.horizontal = horizontal
        line.width = width
        line.color = color
        return this
    }

    fun removeLines(): Captcha {
        line.isAdded = false
        return this
    }

    fun setText(length: Int = text.length, color: Color? = text.color) = setText(length, color, text.chars)

    fun setText(
        length: Int = text.length,
        color: Color? = text.color,
        chars: List<Char> = text.chars
    ): Captcha {
        text.length = length
        text.color = color
        text.chars = chars
        return this
    }

    fun addNoise(
        fill: Boolean = noise.fill,
        color: Color? = noise.color
    ): Captcha {
        noise.isAdded = true
        noise.fill = fill
        noise.color = color
        return this
    }

    fun removeNoises(): Captcha {
        noise.isAdded = false
        return this
    }
}