package com.dds.springitdlp.application.contracts;

import java.security.AccessControlContext;
import java.security.Permission;
import java.security.Permissions;
import java.security.ProtectionDomain;

public class Jail {

    private final SecurityManager sm;
    private final SecurityManager hold;

    public Jail(Permissions permissions) {
        this.hold = System.getSecurityManager();
        this.sm = new SecurityManager() {
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

    public void toggle() {
        System.setSecurityManager(System.getSecurityManager() != this.sm ? this.sm : this.hold);
    }
}
