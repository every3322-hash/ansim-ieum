import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class UTF8StringRequest(
    method: Int,
    url: String,
    listener: Response.Listener<String>,
    errorListener: Response.ErrorListener?
) : StringRequest(method, url, listener, errorListener) {

    override fun parseNetworkResponse(response: NetworkResponse?): Response<String> {
        return try {
            val utf8String = String(response?.data ?: ByteArray(0), Charsets.UTF_8)
            Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: UnsupportedEncodingException) {
            Response.error(ParseError(e))
        }
    }
}