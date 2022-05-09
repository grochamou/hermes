package onl.gcm.hermes.db.service.impl;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import onl.gcm.hermes.db.model.LogEntry;
import onl.gcm.hermes.db.repository.LogEntryRepository;
import onl.gcm.hermes.db.service.LogEntryService;

@Service
public class LogEntryServiceImpl implements LogEntryService {

    @Resource
    private LogEntryRepository repository;

    @Transactional
    public LogEntry create(LogEntry logEntry) {
        return repository.save(logEntry);
    }

}
