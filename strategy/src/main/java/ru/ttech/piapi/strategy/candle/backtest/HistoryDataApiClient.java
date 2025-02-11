package ru.ttech.piapi.strategy.candle.backtest;

import com.google.common.net.HttpHeaders;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

public class HistoryDataApiClient {

  private static final Logger logger = LoggerFactory.getLogger(HistoryDataApiClient.class);
  private static final String HISTORY_DATA_URL = "https://invest-public-api.tinkoff.ru/history-data?instrumentId=%s&year=%d";
  private static final String FILENAME_PATTERN = "%s_%d.zip";
  private static final OkHttpClient client = new OkHttpClient();
  private final ConnectorConfiguration configuration;

  public HistoryDataApiClient(ConnectorConfiguration configuration) {
    this.configuration = configuration;
  }

  public void downloadHistoricalDataArchive(String instrumentUid, int year) {
    String fileName = String.format(FILENAME_PATTERN, instrumentUid, year);
    if (year < LocalDate.now().getYear()) {
      File file = new File(fileName);
      if (file.exists() && !file.isDirectory()) {
        logger.info("File {} already exists, skipping download", fileName);
        return;
      }
    }
    Request request = new Request.Builder()
      .url(String.format(HISTORY_DATA_URL, instrumentUid, year))
      .addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", configuration.getToken()))
      .build();
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Unexpected code " + response);
      }
      saveResponseToFile(response, fileName);
    } catch (IOException e) {
      logger.error("Error occurred while downloading data: {}", e.getMessage());
    }
  }

  private void saveResponseToFile(Response response, String filename) throws IOException {
    if (response.body() == null) {
      throw new IOException("Response body is null " + response);
    }
    File zipFile = new File(filename);
    try (ResponseBody responseBody = response.body();
         InputStream inputStream = responseBody.byteStream();
         FileOutputStream fileOutputStream = new FileOutputStream(zipFile)) {
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        fileOutputStream.write(buffer, 0, bytesRead);
      }
    }
    logger.info("ZIP file downloaded successfully!");
  }
}
