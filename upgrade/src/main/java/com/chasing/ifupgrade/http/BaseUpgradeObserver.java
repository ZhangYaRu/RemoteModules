package com.chasing.ifupgrade.http;

import android.content.Context;
import android.support.annotation.NonNull;

import com.chasing.ifupgrade.bean.ResponseBean;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public abstract class BaseUpgradeObserver<T> implements Observer<ResponseBean<T>> {


    Context mContext;
    public BaseUpgradeObserver(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void onNext(ResponseBean<T> result) {
        if (result.getStatus() == 0) {
            onSuccess(result.getData());
        }
        else {
//            onFailure("","");
        }
//        else if (result.getStatus() == 1001){
//            onFailure(new Exception(mContext.getString(R.string.operation_failed)), result.getMsg());
//        } else if (result.getStatus() == 4001){
//            onFailure(new Exception(mContext.getString(R.string.wrong_arg)), result.getMsg());
//        } else if (result.getStatus() == 4002){
//            onFailure(new Exception(mContext.getString(R.string.file_not_exist)), result.getMsg());
//        } else if (result.getStatus() == 5001){
//            onFailure(new Exception(mContext.getString(R.string.sd_card_not_mounted)), result.getMsg());
//        } else {
//            onFailure(new Exception(result.getMsg()), result.getMsg());
//        }
    }

    @Override
    public void onError(@NonNull Throwable e) {
        onFailure(e, "");
    }

    @Override
    public void onComplete() {

    }

    @Override
    public void onSubscribe(Disposable d) {

    }

    public abstract void onSuccess(T result);

    public abstract void onFailure(Throwable e, String errorMsg);

}
