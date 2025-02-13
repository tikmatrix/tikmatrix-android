package com.github.tikmatrix.util;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class OkhttpManager {
    private static final String TAG = "OkhttpManager";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client;

    private volatile static OkhttpManager singleton;

    private OkhttpManager() {
        client = new OkHttpClient.Builder().proxy(null).build();
    }

    public static OkhttpManager getSingleton() {
        if (singleton == null) {
            synchronized (OkhttpManager.class) {
                if (singleton == null) {
                    singleton = new OkhttpManager();
                }
            }
        }
        return singleton;
    }

    public void newCall(Request request, Callback callback) {
        client.newCall(request).enqueue(callback);
    }

    public void post(final String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(JSON, json);
        final Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
    }

    public void delete(final String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .delete(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
    }

}
