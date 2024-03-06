package org.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import org.ton.api.liteclient.config.LiteClientConfigGlobal
import org.ton.api.pk.PrivateKeyEd25519
import org.ton.crypto.hex
import org.ton.lite.client.LiteClient
import org.ton.mnemonic.Mnemonic
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.ton.block.AddrStd
import org.ton.block.Coins
import org.ton.cell.buildCell
import org.ton.contract.wallet.MessageData
import org.ton.contract.wallet.WalletTransfer
import org.ton.contract.wallet.WalletV3R2Contract

class MainTest {
    //    val lite_server = LiteServerDesc(
//        id = PublicKey,
//        ip = 1592601963,
//        port = 13833
//    )
    private suspend fun connect(): LiteClient {
        val json = Json { ignoreUnknownKeys = true }
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://ton.org/testnet-global.config.json")) // https://ton.org/global-config.json
            .build()
        val response = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
        println(response)
        val config = json.decodeFromString<LiteClientConfigGlobal>(response.body()).apply {
            println(this)
        }
        return LiteClient(Dispatchers.Default, config)
    }
    @Test
    fun `test private key`() {
        val mnemo: List<String> = (File("mnemo.txt").toString()).split(", ")
        val pk = PrivateKeyEd25519(Mnemonic.toSeed(mnemo, "1234"))
        val actual = hex(pk.key)
        val expected = "db26c0b0c2b73861e311568e984eae1df8a3030d7ccc74de2ca4cc54a66aa4b1"
        Assertions.assertEquals(expected, actual)
    }
    @Test
    fun `test public key`() {
        val mnemo: List<String> = (File("mnemo.txt").toString()).split(", ")
        val pk = PrivateKeyEd25519(Mnemonic.toSeed(mnemo, "1234"))
        val actual = hex(pk.publicKey().key)
        val expected = "fcf55d9c499a158ebd21432a66e74ed235ea59a3ca11b22e08011d426bf36a50"
        Assertions.assertEquals(expected, actual)
    }
    @Test
    fun `test raw address`() {
        val mnemo: List<String> = (File("mnemo.txt").toString()).split(", ")
        val pk = PrivateKeyEd25519(Mnemonic.toSeed(mnemo, "1234"))
        val walletAddress = WalletV3R2Contract.address(pk, 0)
        val actual = walletAddress.toString(userFriendly = false, testOnly = true)
        val expected = "0:8541BB5DA75E29C3B5CA7CAC3AECCB575EBE1946CB31A882E59135D7A1D7CDBC"
        Assertions.assertEquals(expected, actual)
    }
    @Test
    fun `test user-friendly address`() {
        val mnemo: List<String> = (File("mnemo.txt").toString()).split(", ")
        val pk = PrivateKeyEd25519(Mnemonic.toSeed(mnemo, "1234"))
        val walletAddress = WalletV3R2Contract.address(pk, 0)
        val actual = walletAddress.toString(userFriendly = true, testOnly = true)
        val expected = "kQCFQbtdp14pw7XKfKw67MtXXr4ZRssxqILlkTXXodfNvPyg"
        Assertions.assertEquals(expected, actual)
    }
    @Test
    fun `test bounceable address`() {
        val mnemo: List<String> = (File("mnemo.txt").toString()).split(", ")
        val pk = PrivateKeyEd25519(Mnemonic.toSeed(mnemo, "1234"))
        val walletAddress = WalletV3R2Contract.address(pk, 0)
        val actual = walletAddress.toString(userFriendly = true, bounceable = true, testOnly = true)
        val expected = "kQCFQbtdp14pw7XKfKw67MtXXr4ZRssxqILlkTXXodfNvPyg"
        Assertions.assertEquals(expected, actual)
    }
    @Test
    fun `test non-bounceable address`() {
        val mnemo: List<String> = (File("mnemo.txt").toString()).split(", ")
        val pk = PrivateKeyEd25519(Mnemonic.toSeed(mnemo, "1234"))
        val walletAddress = WalletV3R2Contract.address(pk, 0)
        val actual = walletAddress.toString(userFriendly = true, bounceable = false, testOnly = true)
        val expected = "0QCFQbtdp14pw7XKfKw67MtXXr4ZRssxqILlkTXXodfNvKFl"
        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun `test seqno`() = runBlocking { // Before Running this test increment expected value
        val client = connect()
        val mnemo: List<String> = (File("mnemo.txt").toString()).split(", ")
        val pk = PrivateKeyEd25519(Mnemonic.toSeed(mnemo, "1234"))
        val walletAddress = WalletV3R2Contract.address(pk, 0)
        val wallet = WalletV3R2Contract(client, walletAddress)
        /*
            each transfer seqno += 1
            for example: seqno = 13 --- wallet.transfer --- seqno = 14 --- wallet.transfer --- seqno = 15 ......
         */
        wallet.transfer(pk, WalletTransfer {
            destination = AddrStd("EQBjn44FF5Dg4mQtlFCzW4w4AC9SE0bhBG04lpYo8ytXk26L")
            coins = Coins(2000000000) // = 2 TON
            messageData = MessageData.raw(
                body = buildCell {// empty body
//                    storeUInt(0, 32)
//                    storeBytes("Comment".toByteArray())
                }
            )
            sendMode = 1 // pay separate
        }) // WalletAddress == kQCFQbtdp14pw7XKfKw67MtXXr4ZRssxqILlkTXXodfNvPyg
        Thread.sleep(10_000)
        val actual = wallet.getWalletData().seqno
        val expected = 31
        Assertions.assertEquals(expected, actual)
    }

//    Coming Soon

//    @Test
//    fun `test transaction`() = runBlocking {
//        val client = connect()
//        val mnemo: List<String> = (File("mnemo.txt").toString()).split(", ")
//        val pk = PrivateKeyEd25519(Mnemonic.toSeed(mnemo, "1234"))
//        val walletAddress = WalletV3R2Contract.address(pk, 0)
//        val wallet = WalletV3R2Contract(client, walletAddress)
//
//        wallet.transfer(pk, WalletTransfer {
//            destination = AddrStd("EQAj2sqA8wrH6X3H6WkUexZlsSstnGKgyyKQmL_JoRpv8K_A")
//            coins = Coins(2000000000) // = 2 TON
//            messageData = MessageData.raw(
//                body = buildCell {// empty body
////                    storeUInt(0, 32)
////                    storeBytes("Comment".toByteArray())
//                }
//            )
//            sendMode = 1 // pay separate
//        })
//
//        Thread.sleep(20_000)
//        client.getLastBlockId(???)
//        client.getTransactions(walletAddress, ???, 1)
//    }
//0xfcf55d9c499a158ebd21432a66e74ed235ea59a3ca11b22e08011d426bf36a50
}