package onl.gcm.hermes.db.repository;

import org.springframework.data.repository.CrudRepository;

import onl.gcm.hermes.db.model.LogEntry;

public interface LogEntryRepository extends CrudRepository<LogEntry, Long> {
}
