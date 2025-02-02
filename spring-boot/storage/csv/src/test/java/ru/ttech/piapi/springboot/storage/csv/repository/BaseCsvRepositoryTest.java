package ru.ttech.piapi.springboot.storage.csv.repository;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseCsvRepositoryTest<T> {

  protected CsvRepository<T> repository;
  protected Path tempDir;

  @SneakyThrows
  @BeforeEach
  void setUp(@TempDir Path tempDirectory) {
    tempDir = tempDirectory;
    Path tempFile = tempDir.resolve("filename.csv");
    repository = createRepository(new CsvConfiguration(tempFile));
  }

  @SneakyThrows
  @AfterEach
  void tearDown() {
    repository.close();
  }

  protected abstract CsvRepository<T> createRepository(CsvConfiguration config);

  protected abstract T createEntity();

  @Test
  @DisplayName("Should save entity and find with filter")
  public void saveEntityAndFindByTimeAndInstrumentUid_success() {
    var entity = createEntity();
    var time = TimeMapper.timestampToLocalDateTime(getEntityTime(entity));
    var instrumentUid = getEntityInstrumentUid(entity);

    repository.save(entity);
    var foundEntities = repository.findAllByTimeAndInstrumentUid(time, instrumentUid);

    assertThat(foundEntities).contains(entity);
  }

  @Test
  @DisplayName("Should save several entities and find them")
  void saveThreeEntitiesAndFindAll_success() {
    var entityOne = createEntity();
    var entityTwo = createEntity();
    var entityThree = createEntity();
    var entities = List.of(entityOne, entityTwo, entityThree);

    repository.saveBatch(entities);
    var foundEntities = repository.findAll();

    assertThat(foundEntities).containsAll(entities);
  }

  protected abstract Timestamp getEntityTime(T entity);

  protected abstract String getEntityInstrumentUid(T entity);
}

