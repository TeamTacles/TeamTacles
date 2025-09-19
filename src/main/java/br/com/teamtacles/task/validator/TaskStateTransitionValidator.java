package br.com.teamtacles.task.validator;

import br.com.teamtacles.common.exception.InvalidTaskStateException;
import br.com.teamtacles.task.enumeration.ETaskStatus;
import org.springframework.stereotype.Component;

@Component
public class TaskStateTransitionValidator {

    public void validate(ETaskStatus currentStatus, ETaskStatus newStatus) {

        if (currentStatus == newStatus) {
            throw new InvalidTaskStateException("The new status cannot be the same as the current status.");
        }

        switch (currentStatus) {
            case TO_DO:
                if (newStatus != ETaskStatus.IN_PROGRESS && newStatus != ETaskStatus.DONE) {
                    throw new InvalidTaskStateException("A task with status TO_DO can only be moved to IN_PROGRESS or DONE.");
                }
                break;

            case IN_PROGRESS:
                if (newStatus == ETaskStatus.TO_DO) {
                    throw new InvalidTaskStateException("A task that is IN_PROGRESS cannot be moved back to TO_DO.");
                }
                break;

            case DONE:
                throw new InvalidTaskStateException("A task that is already completed cannot have its status changed.");
        }
    }

}
