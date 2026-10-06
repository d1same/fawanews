package sc.fawanews.app.data

object StreamRequestHeaders {
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    val properties: Map<String, String> = mapOf(
        "Referer" to FawaRepository.BASE_URL,
        "Origin" to "http://www.fawanews.sc",
        "User-Agent" to USER_AGENT,
        "Accept" to "*/*",
    )
}
