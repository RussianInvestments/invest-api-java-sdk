package ru.ttech.piapi.storage.jdbc.repository;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.storage.jdbc.config.JdbcConfiguration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseJdbcRepositoryTest<T, R extends JdbcRepository<T>> extends BaseIntegrationTest {

  protected R repository;

  @BeforeEach
  void setUpRepository() {
    repository = createRepository(createJdbcConfiguration(postgresDataSource));
  }

  protected abstract R createRepository(JdbcConfiguration configuration);

  protected abstract T createEntity(String instrumentUid, Instant time);

  protected final T createEntity() {
    return createEntity(UUID.randomUUID().toString(), Instant.now());
  }

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

  @Test
  @DisplayName("Should save several entities and find them by period")
  void saveThreeEntitiesAndFindByPeriod_success() {
    var instrumentUid = UUID.randomUUID().toString();
    var startTime = Instant.now();
    var entityOne = createEntity(instrumentUid, startTime.plusMillis(10));
    var entityTwo = createEntity(instrumentUid, startTime.plusMillis(20));
    var timeTwo = TimeMapper.timestampToLocalDateTime(getEntityTime(entityTwo));
    var entityThree = createEntity(instrumentUid, startTime.plusMillis(30));
    var timeThree = TimeMapper.timestampToLocalDateTime(getEntityTime(entityThree));
    var entities = List.of(entityOne, entityTwo, entityThree);

    repository.saveBatch(entities);
    var foundEntities = repository.findAllByPeriodAndInstrumentUid(timeTwo, timeThree, instrumentUid);

    assertThat(foundEntities).hasSize(2);
    assertThat(foundEntities).containsExactly(entityTwo, entityThree);
  }

  protected abstract Timestamp getEntityTime(T entity);

  protected abstract String getEntityInstrumentUid(T entity);
}
