package com.dds.springitdlp.infrastructure.repositories;

import com.dds.springitdlp.application.repositories.Storage;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryStorage implements Storage {

    public MemoryStorage() {

    }
}
