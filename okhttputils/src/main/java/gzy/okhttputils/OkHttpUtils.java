package gzy.okhttputils;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by GZY.com on 18/1/26.
 */

public class OkHttpUtils {
    private static OkHttpUtils mInstance;
    private OkHttpClient mHttpClient;
    private Gson mGson;
    private final static String Autor = "Gzy";
    private static final String TAG = "OkHttpUtils";

    static {
        mInstance = new OkHttpUtils();
    }

    private OkHttpUtils() {
        mHttpClient = new OkHttpClient();
        OkHttpClient.Builder builder = mHttpClient.newBuilder();
        builder.connectTimeout(10, TimeUnit.SECONDS);
        builder.readTimeout(10, TimeUnit.SECONDS);
        builder.writeTimeout(30, TimeUnit.SECONDS);
        mGson = new Gson();
    }

    public static OkHttpUtils getInstance() {
        return mInstance;
    }

    public void get(String url, BaseCallback<Object> callback) {
        Request request = buildRequest(url, HttpMethodType.GET, null);
        doRequest(request, callback, HttpMethodType.GET);
    }

    public void downLoad(String url, BaseCallback<Object> callback) {
        Request request = buildRequest(url, HttpMethodType.DOWNLOAG, null);
        doRequest(request, callback, HttpMethodType.DOWNLOAG);
    }

    public void post(String url, Map<String, String> params, BaseCallback<Object> callback) {
        Request request = buildRequest(url, HttpMethodType.POST, params);
        doRequest(request, callback, HttpMethodType.POST);
    }

    public void upLoad(String url, Map<String, String> params) {
        Request request = buildRequest(url, HttpMethodType.UPLOAD, params);

    }

    private RequestBody builderFormData(Map<String, String> params, HttpMethodType methodTyp) {
        if (methodTyp == HttpMethodType.POST) {
            FormBody.Builder builder = new FormBody.Builder();
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    builder.add(entry.getKey(), entry.getValue());
                }
            }
            return builder.build();
        } else {
            //1 获取文件
            File file = new File(Environment.getExternalStorageDirectory() + "test.txt");
//            参数 说明
//            text / html HTML格式
//            text / plain 纯文本格式
//            text / xml XML格式
//            image / gif gif图片格式
//            image / jpeg jpg图片格式
//            image / png png图片格式
//            application / xhtml + xml XHTML格式
//            application / xml XML数据格式
//            application / atom + xml Atom XML聚合格式
//            application / json JSON数据格式
//            application / pdf pdf格式
//            application / msword Word文档格式
//            application / octet - stream	二进制流数据
            //2 创建 MediaType 设置上传文件类型
            MediaType MEDIATYPE = MediaType.parse("text/plain; charset=utf-8");
            //3 获取请求体
            RequestBody requestBody = RequestBody.create(MEDIATYPE, file);
            return requestBody;
        }
    }

    private Request buildRequest(String url, HttpMethodType methodType, Map<String, String> params) {
        Request.Builder builder = new Request.Builder()
                .url(url);
        if (methodType == HttpMethodType.POST) {
            RequestBody body = builderFormData(params, methodType);
            builder.post(body);
        } else if (methodType == HttpMethodType.GET) {
            builder.get();
        } else if (methodType == HttpMethodType.DOWNLOAG) {
            builder.get();
        } else if (methodType == HttpMethodType.UPLOAD) {
            RequestBody body = builderFormData(params, methodType);
            builder.post(body);
        }
        return builder.build();
    }

    private void callbackSuccess(final BaseCallback<Object> callback, final Response response, final Object obj) {
        callback.onSuccess(response, obj);
    }

    private void callbackError(final BaseCallback<Object> callback, final Response response, final Exception e) {
        callback.onError(response, response.code(), e);
    }

    private void doRequest(final Request request, final BaseCallback<Object> baseCallback, final HttpMethodType methodType) {
        baseCallback.onBeforeRequest(request);
        mHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, IOException e) {
                Log.i(TAG, "onFailure: " + e.getMessage());
                baseCallback.onFailure(request, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    if (methodType == HttpMethodType.DOWNLOAG) {
                        InputStream inputStream = response.body().byteStream();
                        FileOutputStream fileOutputStream = null;
                        try {
                            File file = new File(Environment.getExternalStorageDirectory() + "大狮子.jpg");
                            fileOutputStream = new FileOutputStream(file);
                            byte[] buffer = new byte[2048];
                            int len = 0;
                            while ((len = inputStream.read(buffer)) != -1) {
                                fileOutputStream.write(buffer, 0, len);
                            }
                            fileOutputStream.flush();
                        } catch (Exception e) {
                            Log.i(TAG, "onResponse: IO exception " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        String resultStr = response.body().string();
                        Log.i(TAG, "onResponse: success " + resultStr);
                        if (baseCallback.mType == String.class) {
                            callbackSuccess(baseCallback, response, resultStr);
                        } else {
                            try {
                                Object obj = mGson.fromJson(resultStr, baseCallback.mType);
                                callbackSuccess(baseCallback, response, obj);
                            } catch (Exception e) { // Json解析的错误
                                callbackError(baseCallback, response, e);
                            }
                        }
                    }
                } else {
                    callbackError(baseCallback, response, null);
                }
            }
        });
    }
}

