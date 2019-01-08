package cy.agorise.bitsybitshareswallet.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class ServiceGenerator{
    public static String API_BASE_URL;
    private static HttpLoggingInterceptor logging;
    private static OkHttpClient.Builder httpClient;
    private static Retrofit.Builder builder;

    private static HashMap<Class<?>, Object> Services;

    public ServiceGenerator(String apiBaseUrl) {
        API_BASE_URL= apiBaseUrl;
        logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
        httpClient = new OkHttpClient.Builder().addInterceptor(logging);
        builder = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(getGson()))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create());
        Services = new HashMap<Class<?>, Object>();
    }

    /**
     * Customizes the Gson instance with specific de-serialization logic
     */
    private Gson getGson(){
        GsonBuilder builder = new GsonBuilder();

        return builder.create();
    }

    public void setCallbackExecutor(Executor executor){
        builder.callbackExecutor(executor);
    }

    public static <T> void setService(Class<T> klass, T thing) {
        Services.put(klass, thing);
    }

    public <T> T getService(Class<T> serviceClass) {

        T service = serviceClass.cast(Services.get(serviceClass));
        if (service == null) {
            service = createService(serviceClass);
            setService(serviceClass, service);
        }
        return service;
    }

    public static <S> S createService(Class<S> serviceClass) {

        httpClient.interceptors().add(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
                okhttp3.Request original = chain.request();
                // Request customization: add request headers
                okhttp3.Request.Builder requestBuilder = original.newBuilder().method(original.method(), original.body());

                okhttp3.Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        });
        httpClient.readTimeout(5, TimeUnit.MINUTES);
        httpClient.connectTimeout(5, TimeUnit.MINUTES);
        OkHttpClient client = httpClient.build();
        Retrofit retrofit = builder.client(client).build();
        return retrofit.create(serviceClass);
    }
}
