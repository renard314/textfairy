package com.renard.ocr;

/**
 * @author renard
 */
public class PermissionGrantedEvent {
    private final String mPermission;

    public PermissionGrantedEvent(String permission) {
        mPermission = permission;
    }

    public String getPermission() {
        return mPermission;
    }
}
