package ipn.mobileapp.model.service;

import android.content.Context;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.net.ssl.SSLContext;

import ipn.mobileapp.debug.DebugMode;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import ipn.mobileapp.model.enums.RequestType;
import ipn.mobileapp.model.enums.Servlets;
import ipn.mobileapp.R;

public class ServletRequest {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private String scheme;
    private String host;
    private int port;
    private Context context;

    public ServletRequest(Context context) {
        this.context = context;
        int resourceId = (DebugMode.ON ? R.array.localhost : R.array.server);
        String[] confValues = this.context.getResources().getStringArray(resourceId);
        scheme = confValues[0];
        host = confValues[1];
        port = Integer.parseInt(confValues[2]);
    }

    public Request create(Servlets servlet, RequestType requestType, Map<String, String> params) {
        HttpUrl url = null;
        String servletPath = context.getResources().getStringArray(R.array.servlets)[servlet.ordinal()];

        HttpUrl.Builder urlBuilder = new HttpUrl.Builder().scheme(scheme).host(host).port(port).addPathSegment(servletPath);
        RequestBody body = null;
        if (requestType.equals(RequestType.GET) || requestType.equals(RequestType.DELETE)) {
            for (Map.Entry map : params.entrySet()) {
                urlBuilder.addQueryParameter(map.getKey().toString(), map.getValue().toString());
            }
        } else {
            FormBody.Builder builder = new FormBody.Builder();

            for (Map.Entry map : params.entrySet()) {
                builder.add(map.getKey().toString(), map.getValue().toString());
            }
            body = builder.build();
        }
        url = urlBuilder.build();

        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (body != null) {
            switch (requestType) {
                case POST:
                    requestBuilder.post(body);
                    break;
                case PUT:
                    requestBuilder.put(body);
                    break;
            }
        }
        return requestBuilder.build();
    }

    public String execute(Request request) {
        String result = null;

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            OkHttpClient client = new OkHttpClient().newBuilder().followRedirects(false).sslSocketFactory(sslContext.getSocketFactory()).build();
            Response response = client.newCall(request).execute();
            result = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        return result;
    }
}