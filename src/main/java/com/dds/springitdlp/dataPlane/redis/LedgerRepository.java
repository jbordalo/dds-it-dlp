package com.dds.springitdlp.dataPlane.redis;

import com.dds.springitdlp.application.ledger.Ledger;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerRepository extends CrudRepository<Ledger, String> {
}
