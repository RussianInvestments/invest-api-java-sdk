package ru.tinkoff.piapi.core.stream;

@Deprecated(since = "1.30", forRemoval = true)
public interface StreamProcessor<T> {

  void process(T response);
}
