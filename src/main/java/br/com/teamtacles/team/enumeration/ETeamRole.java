package br.com.teamtacles.team.enumeration;

public enum ETeamRole {
    OWNER(1),
    ADMIN(2),
    MEMBER(3);

    private final int value;

    ETeamRole(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public boolean isPrivileged() {
        return this == OWNER || this == ADMIN;
    }

    public boolean isOwner() {
        return this == OWNER;
    }
}
