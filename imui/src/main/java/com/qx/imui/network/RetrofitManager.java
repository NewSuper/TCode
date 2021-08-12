package com.qx.imui.network;


import com.qx.imui.Constans;

import java.util.HashMap;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

//retrofit封装
public class RetrofitManager {
    private Retrofit mRetrofitClient;
    private OkHttpClient mOkHttpClient; //httpclient
    private HashMap<String, Object> mServiceMap;


    private volatile static RetrofitManager instance = null;

    private RetrofitManager() {
        mServiceMap = new HashMap<>();
        mOkHttpClient = createOkHttpClient();
        mRetrofitClient = createRetrofitClient(mOkHttpClient);
    }

    /**
     * 创建默认的OkHttpClient
     *
     * @return
     */
    public OkHttpClient createOkHttpClient() {
        return OkHttpClientProvider.INSTANCE.getDefaultOkHttpClient();
    }

    /**
     * 创建RetrofitClient
     *
     * @param httpClient
     * @return
     */
    private Retrofit createRetrofitClient(OkHttpClient httpClient) {
        return new Retrofit.Builder()
                .client(httpClient)
                .baseUrl(Constans.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

    /**
     * 获取实例对象
     *
     * @return
     */
    public static RetrofitManager getInstance() {
        if (instance == null) {
            synchronized (RetrofitManager.class) {
                if (instance == null) {
                    instance = new RetrofitManager();
                }
            }
        }
        return instance;
    }


    /**
     * 获取ApiService
     *
     * @param serviceClass
     * @param <T>
     * @return
     */
    public <T> T getApiService(Class<T> serviceClass) {
        T result = null;
        if (mServiceMap != null) {
            synchronized (mServiceMap) {
                if (mServiceMap.containsKey(serviceClass.getName())) {
                    result = (T) mServiceMap.get(serviceClass.getName());
                } else {
                    result = createService(serviceClass, null);
                    mServiceMap.put(serviceClass.getName(), result);
                }
            }
        }
        return result;
    }


    /**
     * 移除某一个ApiService
     *
     * @param retrofitClass
     * @param <T>
     */
    public <T> void removeApiService(Class<T> retrofitClass) {
        if (mServiceMap != null) {
            synchronized (mServiceMap) {
                if (mServiceMap.containsKey(retrofitClass.getName())) {
                    mServiceMap.remove(retrofitClass.getName());
                }
            }
        }
    }

    /**
     * 清除所有的ApiService
     */
    public void clearApiService() {
        if (mServiceMap != null) {
            synchronized (mServiceMap) {
                mServiceMap.clear();
            }
        }
    }

    /**
     * 创建新的APiservices
     *
     * @param serviceClass
     * @param client
     * @param <T>
     * @return
     */
    public <T> T createService(Class<T> serviceClass, OkHttpClient client) {
        if (client == null) {
            return mRetrofitClient.create(serviceClass);
        } else {
            return createRetrofitClient(client).create(serviceClass);
        }
    }

    /**
     * 获取OkHttpClient
     *
     * @return
     */
    public OkHttpClient getmOkHttpClient() {
        return mOkHttpClient;
    }

    /**
     * 获取默认的RetrofitClient
     *
     * @return
     */
    public Retrofit getRetrofitClient() {
        return mRetrofitClient;
    }
}
