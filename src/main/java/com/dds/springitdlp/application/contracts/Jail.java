package com.dds.springitdlp.application.contracts;

import java.security.AccessControlContext;
import java.security.Permission;
import java.security.Permissions;
import java.security.ProtectionDomain;

public class Jail {

    private final SecurityManager jailManager;
    private final SecurityManager systemManager;

    public Jail(Permissions permissions) {
        this.systemManager = System.getSecurityManager();
        this.jailManager = new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                assert perm != null;

                AccessControlContext a = new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, permissions)});

                for (Class<?> clasS : this.getClassContext()) {
                    for (Class<?> inter : clasS.getInterfaces())
                        if (inter == SmartContract.class) {
                            a.checkPermission(perm);
                        }
                }
            }
        };
    }

    public void lock() {
        System.setSecurityManager(this.jailManager);
    }

    public void unlock() {
        System.setSecurityManager(this.systemManager);
    }
}
