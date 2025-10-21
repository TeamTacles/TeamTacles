package br.com.teamtacles.task.enumeration;

public enum ETaskStatus {
    TO_DO(1),
    IN_PROGRESS(2),
    DONE(3),
    OVERDUE(4);

    private final int value;

    ETaskStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
