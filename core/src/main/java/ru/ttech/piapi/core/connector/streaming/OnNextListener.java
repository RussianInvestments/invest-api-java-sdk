package ru.ttech.piapi.core.connector.streaming;

public interface OnNextListener<RespT> {

  void onNext(RespT response);
}
