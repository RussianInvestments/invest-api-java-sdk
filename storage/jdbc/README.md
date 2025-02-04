# Модуль java-sdk-storage-jdbc
Содержит в себе имплементации репозиториев для хранения рыночных данных в БД.

На данный момент полностью поддерживаются следующие СУБД:

* [PostgreSQL](https://www.postgresql.org/)
* [MySQL](https://www.mysql.com/)

Для работы с модулем требуется также добавить в зависимости jdbc драйвер вашей СУБД:

* PostgreSQL:
    * Maven
      ```xml
          <dependency>
              <groupId>org.postgresql</groupId>
              <artifactId>postgresql</artifactId>
              <version>42.7.5</version>
          </dependency>
      ```
    * Gradle
      ```groovy
      implementation 'org.postgresql:postgresql:42.7.5'
      ```
* MySQL:
    * Maven
      ```xml
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>9.2.0</version>
        </dependency>
      ```
    * Gradle
      ```groovy
      implementation 'com.mysql:mysql-connector-j:9.2.0'
      ```
