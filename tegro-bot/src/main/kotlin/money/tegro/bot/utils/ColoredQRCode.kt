package money.tegro.bot.utils

import io.github.g0dkar.qrcode.QRCode
import java.io.FileOutputStream

class ColoredQRCode {
    fun createQRCode(
        content: String,
        squareColor: Int,
        backgroundColor: Int,
        filename: String = "kotlin-colored.png"
    ) {
        val fileOut = FileOutputStream(filename)
        val qrCodeCanvas = QRCode(content).render(darkColor = squareColor, brightColor = backgroundColor)

        qrCodeCanvas.writeImage(fileOut)
    }
}