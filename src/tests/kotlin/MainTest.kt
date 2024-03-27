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
import org.ton.cell.Cell
import org.ton.cell.buildCell
import org.ton.contract.wallet.MessageData
import org.ton.contract.wallet.WalletTransfer
import org.ton.contract.wallet.WalletV3R2Contract


@Disabled
class MainTest {
//        val lite_server = LiteServerDesc(
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
//            println(this)
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
        val walletAddress = WalletV3R2Contract.address(pk, 0) // WalletAddress == kQCFQbtdp14pw7XKfKw67MtXXr4ZRssxqILlkTXXodfNvPyg
        val wallet = WalletV3R2Contract(client, walletAddress)
        /*
            each transfer seqno += 1
            for example: seqno = 13 --- wallet.transfer --- seqno = 14 --- wallet.transfer --- seqno = 15 ......
         */
        wallet.transfer(pk, WalletTransfer {
            destination = AddrStd("kQCFQbtdp14pw7XKfKw67MtXXr4ZRssxqILlkTXXodfNvPyg") // sending from my wallet address to my wallet address
            coins = Coins(2000000000) // = 2 TON
            messageData = MessageData.raw(
                body = buildCell {// empty body
//                    storeUInt(0, 32)
//                    storeBytes("Comment".toByteArray())
                }
            )
            sendMode = 1 // pay separate
        })

        Thread.sleep(20_000)

        val actual = wallet.getWalletData().seqno
        val expected = 34

        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun `test transaction`() = runBlocking {
        val client = connect()
        val mnemo: List<String> = (File("mnemo.txt").toString()).split(", ")
        val pk = PrivateKeyEd25519(Mnemonic.toSeed(mnemo, "1234"))
        val walletAddress = WalletV3R2Contract.address(pk, 0)

        val lastTransactionInfo = client.getTransactions(walletAddress, client.getAccountState(walletAddress).lastTransactionId!!, 2)
        val lastTransactionFieldsDeserialized = lastTransactionInfo[1].transaction.getValue(null, null) // [0] == last transaction // [1] == before last transaction
        val lastTransactionBodyCell = lastTransactionFieldsDeserialized.r1.value.inMsg.value?.value?.body?.y?.value!!
//          println(lastTransactionBodyCell)

        val actual = lastTransactionBodyCell
        val expected = Cell(
            "2B47315A2AEE65D584E12D9FBB5BE6346F9A36E06E0E8FBD8F5A4378092CFE7C2090B41F369985E33C662DEE42BBFC47D13824AF2CB38B23113EE8FF85EEC10229A9A3177FFFFFFF0000002001",
            Cell("620042A0DDAED3AF14E1DAE53E561D7665ABAF5F0CA36598D44172C89AEBD0EBE6DE23B9ACA00000000000000000000000000000")
        )

        Assertions.assertEquals(expected, actual)
    }
}