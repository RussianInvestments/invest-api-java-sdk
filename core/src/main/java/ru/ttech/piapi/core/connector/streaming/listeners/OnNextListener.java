package ru.ttech.piapi.core.connector.streaming.listeners;

public interface OnNextListener<RespT> {

  void onNext(RespT response);
}
