package xyqb.net;

import java.io.File;
import java.util.HashMap;

import xyqb.net.callback.OnRequestListener;
import xyqb.net.callback.OnRequestResultListener;
import xyqb.net.model.RequestConfig;
import xyqb.net.model.RequestItem;
import xyqb.net.xml.RequestConfigReader;

/**
 * Created by cz on 8/23/16.
 */
public class NetManager {
    private static NetManager instance=new NetManager();
    private final RequestConfig requestConfig;
    private final RequestConfigReader configReader;
    private final HashMap<String,RequestItem> cacheItems;


    private NetManager(){
        cacheItems =new HashMap<>();
        requestConfig =new RequestConfig();
        configReader=new RequestConfigReader(requestConfig);
    }

    public static NetManager getInstance(){
        return instance;
    }



    public NetManager setRawName(String rawId){
        requestConfig.rawName=rawId;
        return this;
    }

    public NetManager setConfigPath(String path){
        requestConfig.path=path;
        return this;
    }

    public NetManager setRequestUrl(String url){
        requestConfig.url=url;
        return this;
    }

    public NetManager setDebug(boolean debug){
        requestConfig.debug=debug;
        return this;
    }

    public NetManager setConnectTimeout(int second){
        this.requestConfig.connectTimeout=second;
        return this;
    }

    public NetManager setReadTimeout(int second){
        this.requestConfig.readTimeout=second;
        return this;
    }

    public NetManager setWriteTimeout(int second){
        this.requestConfig.writeTimeout=second;
        return this;
    }

    public NetManager setCacheFile(File cacheFile){
        this.requestConfig.cachedFile=cacheFile;
        return this;
    }

    public NetManager setMaxCacheSize(long maxCacheSize){
        this.requestConfig.maxCacheSize=maxCacheSize;
        return this;
    }

    public NetManager setRetryOnConnectionFailure(boolean retryOnConnectionFailure){
        this.requestConfig.retryOnConnectionFailure=retryOnConnectionFailure;
        return this;
    }

    public NetManager setOnRequestListener(OnRequestListener listener){
        this.requestConfig.listener=listener;
        return this;
    }

    public NetManager setOnRequestResultListener(OnRequestResultListener requestResultListener){
        this.requestConfig.requestResultListener=requestResultListener;
        return this;
    }

    public RequestItem getRequestItem(String action){
        if(cacheItems.isEmpty()){
            cacheItems.putAll(configReader.readerRequestItems());
        }
        return cacheItems.get(action);
    }


    public RequestConfig getRequestConfig(){
        return requestConfig;
    }





}
