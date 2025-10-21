package br.com.teamtacles.task.enumeration;

public enum ETaskRole {
    OWNER(1),
    ASSIGNEE(2);

    private final int value;

    ETaskRole(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public boolean isOwner() {
        return this == OWNER;
    }
}
