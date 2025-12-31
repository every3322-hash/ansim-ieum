package com.example.myapplication

import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import java.io.ByteArrayInputStream
import java.io.InputStream

class InputStreamRequest(
    method: Int,
    url: String,
    private val listener: Response.Listener<InputStream>,
    errorListener: Response.ErrorListener
) : Request<InputStream>(method, url, errorListener) {

    override fun deliverResponse(response: InputStream) {
        listener.onResponse(response)
    }

    override fun parseNetworkResponse(response: com.android.volley.NetworkResponse): Response<InputStream> {
        return try {
            val inputStream = ByteArrayInputStream(response.data)
            Response.success(inputStream, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: Exception) {
            Response.error(com.android.volley.VolleyError(e))
        }
    }
}