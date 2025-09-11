package br.com.teamtacles.project.enumeration;

public enum EProjectRole {
    OWNER,
    ADMIN,
    MEMBER,
    GUEST;

    public boolean isPrivileged() {
        return this == OWNER || this == ADMIN;
    }
}
