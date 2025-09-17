package br.com.teamtacles.task.enumeration;

public enum ETaskRole {
    OWNER,
    ASSIGNEE;

    public boolean isOwner() {
        return this == OWNER;
    }
}
