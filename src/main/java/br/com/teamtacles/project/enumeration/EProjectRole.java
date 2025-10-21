package br.com.teamtacles.project.enumeration;

public enum EProjectRole {
    OWNER(1),
    ADMIN(2),
    MEMBER(3);

    private final int value;

    EProjectRole(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public boolean isPrivileged() {
        return this == OWNER || this == ADMIN;
    }
}
