package org.example

//suspend fun getConfig(): LiteClientConfigGlobal {
//        val client = HttpClient.newBuilder().build()
//        val request = HttpRequest.newBuilder()
//            .uri(URI.create("https://ton.org/testnet-global.config.json"))
//            .build()
//        val response = withContext(Dispatchers.IO) {
//            client.send(request, HttpResponse.BodyHandlers.ofString())
//        }
////        println(response.body())
//        val config = json.decodeFromString<LiteClientConfigGlobal>(response.body()).apply {
//            println(this)
//        }
//        return config;
//    }