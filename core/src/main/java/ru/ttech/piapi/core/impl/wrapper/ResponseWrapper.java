package ru.ttech.piapi.core.impl.wrapper;

public abstract class ResponseWrapper<T> {

  protected final T response;

  protected ResponseWrapper(T response) {
    this.response = response;
  }

  public T getOriginal() {
    return response;
  }
}
