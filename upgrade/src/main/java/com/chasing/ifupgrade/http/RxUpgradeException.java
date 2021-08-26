package com.chasing.ifupgrade.http;

public class RxUpgradeException {
//    public static String exceptionHandler(Throwable e){
//        String errorMsg = App.getContext().getString(R.string.server_error);
//        if (e instanceof UnknownHostException) {
//            errorMsg = App.getContext().getString(R.string.network_not_available);
//        } else if (e instanceof SocketTimeoutException) {
//            errorMsg = App.getContext().getString(R.string.network_timeout);
//        } else if (e instanceof HttpException) {
//            HttpException httpException = (HttpException) e;
//            errorMsg = convertStatusCode(httpException);
//        } else if (e instanceof ParseException || e instanceof JSONException) {
//            errorMsg = App.getContext().getString(R.string.data_parse_error);
//        }
//        return errorMsg;
//    }
//
//    private static String convertStatusCode(HttpException httpException) {
//        String msg;
//        if (httpException.code() >= 300 && httpException.code() < 600) {
//            msg = App.getContext().getString(R.string.server_error);
//        } else {
//            msg = httpException.message();
//        }
//        return msg;
//    }
}
