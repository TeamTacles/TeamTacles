package br.com.teamtacles.task.validator;

import br.com.teamtacles.common.exception.InvalidTaskStateException;
import br.com.teamtacles.task.enumeration.ETaskStatus;
import org.springframework.stereotype.Component;

@Component
public class TaskStateTransitionValidator {

    public void validate(ETaskStatus currentStatus, ETaskStatus newStatus) {

        if (currentStatus == ETaskStatus.DONE) {
            throw new InvalidTaskStateException("Not allowed to change status of a completed task.");
        }

        if (currentStatus == newStatus) {
            throw new InvalidTaskStateException("The new status cannot be the same as the current status.");
        }

        if (newStatus == ETaskStatus.OVERDUE) {
            throw new InvalidTaskStateException("You cannot manually set the task status to OVERDUE.");
        }
    }
}
