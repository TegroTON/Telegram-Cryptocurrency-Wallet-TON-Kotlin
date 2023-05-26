package money.tegro.bot.objects

import kotlinx.serialization.Serializable

@Serializable
enum class NftCollection(
    val displayName: String,
    val address: String
) {
    CAT_METAVERSE("Cat Metaverse", "0:a2a0615cf95932dc66db5074c3515bcd1f1287b10a68107b03ff165e4ee1859a"),
    DOG_METAVERSE("Dog Metaverse", "0:1a76bb0778f6c793ebfb2ad338fe3ad7d074e53187cff591ad94b827e7dd03ef"),
    META_PANTHERS("Meta Panthers", "0:a330a115875af87b83a381c76e449d43683c7b74a6de237f3e2cf82643bae3e0"),
    CRYPTO_SHERLOCK("Crypto Sherlock", "0:81bf0c6259ca38bd7dba86d20be8d366b9e348a79149893170b7431d3b532866"),
}