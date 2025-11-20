package br.com.teamtacles.config;

import br.com.teamtacles.task.dto.response.TaskResponseDTO;
import br.com.teamtacles.task.dto.response.TaskUpdateStatusResponseDTO;
import br.com.teamtacles.task.dto.response.UserAssignmentResponseDTO;
import br.com.teamtacles.task.dto.response.UserTaskResponseDTO;
import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.task.model.TaskAssignment;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Bean;
import org.modelmapper.convention.MatchingStrategies;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfiguration {

    @Bean
    public ModelMapper modelMapper(){
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.getConfiguration()
                .setSkipNullEnabled(true)
                .setMatchingStrategy(MatchingStrategies.STRICT);

        TypeMap<Task, TaskResponseDTO> taskToDtoTypeMap = modelMapper.createTypeMap(Task.class, TaskResponseDTO.class);

        taskToDtoTypeMap.addMappings(mapper -> {
            mapper.map(src -> src.getOwner().getId(), TaskResponseDTO::setOwnerId);
            mapper.map(src -> src.getProject().getId(), TaskResponseDTO::setProjectId);
            mapper.map(Task::getEffectiveStatus, TaskResponseDTO::setStatus);
            mapper.map(Task::getStatus, TaskResponseDTO::setOriginalStatus);
        });

        modelMapper.createTypeMap(Task.class, UserTaskResponseDTO.class)
                .addMappings(mapper -> {
                    mapper.map(Task::getEffectiveStatus, UserTaskResponseDTO::setStatus);
                    mapper.map(Task::getStatus, UserTaskResponseDTO::setOriginalStatus);
                });

        modelMapper.createTypeMap(Task.class, TaskUpdateStatusResponseDTO.class)
                .addMappings(mapper -> {
                    mapper.map(Task::getEffectiveStatus, TaskUpdateStatusResponseDTO::setStatus);
                    mapper.map(Task::getStatus, TaskUpdateStatusResponseDTO::setOriginalStatus);
                });

        modelMapper.createTypeMap(TaskAssignment.class, UserAssignmentResponseDTO.class)
                .addMapping(src -> src.getUser().getId(), UserAssignmentResponseDTO::setUserId)
                .addMapping(src -> src.getUser().getUsername(), UserAssignmentResponseDTO::setUsername);

        return modelMapper;
    }
}
