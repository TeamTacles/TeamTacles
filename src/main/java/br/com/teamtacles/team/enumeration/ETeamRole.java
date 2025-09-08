package br.com.teamtacles.team.enumeration;

public enum ETeamRole {
    OWNER,
    ADMIN,
    MEMBER;

    public boolean isPrivileged() {
        return this == OWNER || this == ADMIN;
    }

    public boolean isOwner() {
        return this == OWNER;
    }
}
