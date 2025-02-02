package ru.ttech.piapi.springboot.storage.jdbc.repository;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public abstract class BaseJdbcRepositoryTest<T, R extends JdbcRepository<T>> extends BaseIntegrationTest {

  protected R repository;

  @BeforeEach
  void setUpRepository() {
    repository = createRepository(createJdbcConfiguration(postgresDataSource));
  }

  @AfterEach
  @SneakyThrows
  void tearDown() {
    if (repository != null) {
      repository.close();
    }
  }

  protected abstract R createRepository(JdbcConfiguration configuration);

  protected abstract T createEntity();

  @Test
  @DisplayName("Should save entity and find with filter")
  void saveEntityAndFindByTimeAndInstrumentUid_success() {
    var entity = createEntity();
    var time = TimeMapper.timestampToLocalDateTime(getEntityTime(entity));
    var instrumentUid = getEntityInstrumentUid(entity);

    repository.save(entity);
    var entities = repository.findAllByTimeAndInstrumentUid(time, instrumentUid);

    assertThat(entities).contains(entity);
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
