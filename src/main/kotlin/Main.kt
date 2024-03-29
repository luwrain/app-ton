package org.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.ton.api.liteclient.config.LiteClientConfigGlobal
import org.ton.api.pk.PrivateKeyEd25519
import org.ton.block.AddrStd
import org.ton.block.Coins
import org.ton.cell.buildCell
import org.ton.contract.wallet.MessageData
import org.ton.contract.wallet.WalletTransfer
import org.ton.contract.wallet.WalletV3R2Contract
import org.ton.lite.client.LiteClient
import org.ton.mnemonic.Mnemonic.toSeed
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.coroutines.CoroutineContext

suspend fun main() {
    val json = Json { ignoreUnknownKeys = true }
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("https://ton.org/testnet-global.config.json")) // https://ton.org/global-config.json
        .build()
    val response = withContext(Dispatchers.IO) {
        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    //println(response.body())
    val config = json.decodeFromString<LiteClientConfigGlobal>(response.body()).apply {
        println(this)
    }

    val mnemo: List<String> = (File("mnemo.txt").toString()).split(", ")
    val pk = PrivateKeyEd25519(toSeed(mnemo, readln()))
    println(pk.publicKey().key)

    val context: CoroutineContext = Dispatchers.Default
    try {
//        val mnemonic = Mnemonic.generate(password = readln())
//        println(mnemonic)
        val liteClient = LiteClient(context, config)
        val walletAddress = WalletV3R2Contract.address(pk, 0)
        println(
            walletAddress.toString(
                userFriendly = true,
                bounceable = true,
                testOnly = true
            )
        ) // WalletAddress == kQCFQbtdp14pw7XKfKw67MtXXr4ZRssxqILlkTXXodfNvPyg
        val wallet = WalletV3R2Contract(liteClient, walletAddress)

        wallet.transfer(pk, WalletTransfer {
            destination = AddrStd("EQAarqdYDQxX5qsduNjHLdg7M96Dfh6VTAaCWSIz3UrMxbf-")
            coins = Coins(2000000000) // = 2 TON
            messageData = MessageData.raw(
                body = buildCell {// empty body
//                    storeUInt(0, 32)
//                    storeBytes("Comment".toByteArray())
                }
            )
            sendMode = 1 // pay separate
        })

    } catch(e: Exception) {
        e.printStackTrace()
    }
}
