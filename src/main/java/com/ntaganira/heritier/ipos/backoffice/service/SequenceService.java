package com.ntaganira.heritier.ipos.backoffice.service;

import com.ntaganira.heritier.ipos.backoffice.entity.DocumentSequence;
import com.ntaganira.heritier.ipos.backoffice.repository.DocumentSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SequenceService {

    private final DocumentSequenceRepository repository;

    public SequenceService(DocumentSequenceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String next(String prefix) {
        DocumentSequence sequence = repository.lockByName(prefix).orElseGet(() -> {
            DocumentSequence created = new DocumentSequence();
            created.setName(prefix);
            created.setValue(0);
            return repository.save(created);
        });
        sequence.setValue(sequence.getValue() + 1);
        repository.save(sequence);
        return prefix + String.format("%06d", sequence.getValue());
    }

    public String format(String prefix, long value) {
        return prefix + String.format("%06d", value);
    }
}
