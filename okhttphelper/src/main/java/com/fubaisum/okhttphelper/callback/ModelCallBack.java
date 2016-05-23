package com.fubaisum.okhttphelper.callback;

import android.accounts.NetworkErrorException;

import com.fubaisum.okhttphelper.ModelParser;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by sum on 15-12-3.
 */
public abstract class ModelCallBack<T> extends Callback<T> {

    private Type modelType;
    private ModelParser<T> modelParser;

    public ModelCallBack() {
        modelType = genericModelType();
        modelParser = new ModelParser<>();
    }

    protected Type genericModelType() {
        return getGenericTypeParameter(getClass());
    }

    protected static Type getGenericTypeParameter(Class<?> thisClass) {
        Type genericSuperclass = thisClass.getGenericSuperclass();
        if (genericSuperclass instanceof Class) {
            throw new RuntimeException("Missing modelType parameter.");
        }
        ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
        return parameterizedType.getActualTypeArguments()[0];
    }

    @Override
    public void onFailure(Call call, IOException e) {
        sendFailureCallback(e);
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        if (!response.isSuccessful()) {
            sendFailureCallback(new NetworkErrorException(response.toString()));
        } else {
            ResponseBody responseBody = response.body();
            try {
                if (modelType == String.class) {
                    //noinspection unchecked
                    sendSuccessCallback((T) responseBody.string());
                } else {
                    T result = modelParser.parseResponse(responseBody, modelType);
                    sendSuccessCallback(result);
                }
            } catch (Exception e) {
                sendFailureCallback(e);
            } finally {
                responseBody.close();
            }
        }
    }
}
