package xyqb.net.impl;

import android.text.TextUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import xyqb.net.IRequest;
import xyqb.net.NetManager;
import xyqb.net.exception.HttpException;
import xyqb.net.model.HttpResponse;
import xyqb.net.model.RequestConfig;
import xyqb.net.model.RequestItem;
import xyqb.net.resultfilter.JsonParamsResultFilter;

/**
 * Created by cz on 8/23/16.
 */
public class OKHttp3 implements IRequest {
    private static final OkHttpClient httpClient;
    private static final RequestConfig requestConfig;

    static {
        requestConfig = NetManager.getInstance().getRequestConfig();
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(requestConfig.connectTimeout, TimeUnit.SECONDS)
                .readTimeout(requestConfig.readTimeout, TimeUnit.SECONDS)
                .writeTimeout(requestConfig.writeTimeout, TimeUnit.SECONDS)
                .retryOnConnectionFailure(requestConfig.retryOnConnectionFailure);
        if(null!=requestConfig.cachedFile){
            clientBuilder.cache(new Cache(requestConfig.cachedFile, requestConfig.maxCacheSize));
        }
        httpClient=clientBuilder.build();
    }
    public OKHttp3() {
    }

    /**
     * 设置HTTPS认证
     */
    private void setSslSocketFactory(OkHttpClient.Builder clientBuilder){
        clientBuilder.hostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            sc.init(null,new TrustManager[]{trustManager}, new SecureRandom());
            clientBuilder.sslSocketFactory(sc.getSocketFactory(),trustManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public Observable<HttpResponse> call(String tag,final RequestItem item,final Object... values) {
        final HashMap<String,String> params=new HashMap<>();
        int length=Math.min(null==item.param?0:item.param.length, values.length);
        for(int i=0;i<length;i++){
            Object value = values[i];
            if(null!=value){
                params.put(item.param[i],value.toString());
            }
        }
        return call(tag,item,params);
    }


    public Observable<HttpResponse> call(final String tag,final RequestItem item, final HashMap<String,String> params){

        return Observable.create(new Observable.OnSubscribe<HttpResponse>() {
            @Override
            public void call(Subscriber<? super HttpResponse> subscriber) {
                try {
                    final Request request = getRequest(tag,item, params);
                    final Call call = httpClient.newCall(request);
                    Response response = call.execute();
                    Headers headers = request.headers();
                    HttpResponse httpResponse = new HttpResponse();
                    if (null != headers&&0<headers.size()) {
                        Set<String> names = headers.names();
                        for (String item : names) {
                            httpResponse.headers.put(item, headers.get(item));
                        }
                    }
                    String result = response.body().string();
                    if (response.isSuccessful()) {
                        httpResponse.result = result;
                        subscriber.onNext(httpResponse);
                    }else{
                        //request success but content is fail
                        HashMap<String, String> params = new JsonParamsResultFilter().result(result);
                        HttpException exception = new HttpException();
                        exception.code = Integer.valueOf(params.get("code"));
                        exception.message = params.get("message");
                        exception.headers = httpResponse.headers;
                        exception.result = result;
                    }
            }
            catch(IOException e){
                //request failed
                HttpException exception = new HttpException();
                exception.code = IRequest.REQUEST_ERROR;
                exception.message = e.getMessage();
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }



    private Request getRequest(String tag,RequestItem item,Map<String, String> params){
        Request request;
        //add extras param
        if(null!=requestConfig&&null!=requestConfig.listener){
            HashMap<String, String> extraItems = requestConfig.listener.requestExtraItems();
            if(null!=extraItems&&!extraItems.isEmpty()){
                params.putAll(extraItems);
            }
        }
        String requestUrl = getRequestUrl(item);
        if(POST.equals(item.method)){
            FormBody.Builder builder = new FormBody.Builder();
            if(null!=params){
                for (Map.Entry<String,String> entry:params.entrySet()) {
                    builder.add(entry.getKey(),entry.getValue());
                }
            }
            Request.Builder requestBuilder = new Request.Builder()
                    .url(requestUrl)
                    .post(builder.build());
            //add global header items
            if(null!=requestConfig&&null!=requestConfig.listener){
                HashMap<String, String> headerItems = requestConfig.listener.requestHeaderItems();
                if(null!=headerItems){
                    for(Map.Entry<String,String> entry:headerItems.entrySet()){
                        requestBuilder.addHeader(entry.getKey(),entry.getValue());
                    }
                }
            }
            if(null!=tag){
                requestBuilder.tag(tag);
            }
            request=requestBuilder.build();
        } else {
            StringBuilder fullUrl = new StringBuilder();
            fullUrl.append(requestUrl);
            if(null!=params){
                int index=0;
                int length=params.size();
                for (Map.Entry<String,String> entry:params.entrySet()) {
                    fullUrl.append(entry.getKey() + "=" + entry.getValue() + (index++ == length - 1 ? "" : "&"));
                }
            }
            Request.Builder requestBuilder = new Request.Builder().url(fullUrl.toString());
            //add global header items
            if(null!=requestConfig&&null!=requestConfig.listener){
                HashMap<String, String> headerItems = requestConfig.listener.requestHeaderItems();
                if(null!=headerItems){
                    for(Map.Entry<String,String> entry:headerItems.entrySet()){
                        requestBuilder.addHeader(entry.getKey(),entry.getValue());
                    }
                }
            }
            if(null!=tag){
                requestBuilder.tag(tag);
            }
            request=requestBuilder.build();
        }
        return request;
    }

    private String getRequestUrl(RequestItem item){
        String absoluteUrl;
        if(!item.url.startsWith("http")){
            absoluteUrl=requestConfig.url+item.url;
        } else if(!TextUtils.isEmpty(item.dynamicUrl)){
            absoluteUrl=item.dynamicUrl+item.url;
        } else {
            absoluteUrl=item.url;
        }
        return absoluteUrl;
    }
}
