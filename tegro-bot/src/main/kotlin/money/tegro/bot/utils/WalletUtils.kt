package money.tegro.bot.utils

import org.ton.api.pk.PrivateKeyEd25519
import org.ton.crypto.digest.Digest
import org.ton.crypto.mac.hmac.HMac
import java.util.*

fun UserPrivateKey(uuid: UUID, masterKey: ByteArray, nonce: ByteArray = byteArrayOf()): PrivateKeyEd25519 {
    val hMac = HMac(Digest.sha512(), masterKey)
    hMac.update(uuid.toByteArray())
    hMac.update(nonce)

    return PrivateKeyEd25519(hMac.build().copyOf(32))
}
