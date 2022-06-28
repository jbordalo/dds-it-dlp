package com.dds.springitdlp.dataPlane.redis;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmartContractRegistryRepository extends CrudRepository<SmartContractRegistry, String> {
}
