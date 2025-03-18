package ru.ttech.piapi.example.cmd;

import com.google.protobuf.MessageOrBuilder;

public interface ResponseFileWriter {

  void writeResponseToFile(MessageOrBuilder response);
}
