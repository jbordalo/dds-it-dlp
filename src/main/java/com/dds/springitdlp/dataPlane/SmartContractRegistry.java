package com.dds.springitdlp.dataPlane;

import com.dds.springitdlp.application.contracts.SmartContract;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@RedisHash("SmartContractRegistry")
@Getter
public class SmartContractRegistry implements Serializable {
    @SuppressWarnings("FieldMayBeFinal")
    private Map<String, SmartContract> smartContractRegistry;

    @Id
    private String id = System.getenv("REPLICA_ID");

    public SmartContractRegistry() {
        this.smartContractRegistry = new HashMap<>();
    }

    public SmartContract getSmartContract(String uuid) {
        return smartContractRegistry.get(uuid);
    }

    public void registerContract(SmartContract smartContract) {
        smartContractRegistry.put(smartContract.getUuid(), smartContract);
    }

}
