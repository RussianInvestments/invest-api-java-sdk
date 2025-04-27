package ru.ttech.piapi.storage.jdbc.repository;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class Containers {

  public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
  public static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:9.2.0");


}
