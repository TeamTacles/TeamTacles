package br.com.teamtacles.common.mapper;

import org.springframework.data.domain.Page;

import br.com.teamtacles.common.dto.response.page.PagedResponse;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PagedResponseMapper {
    private final ModelMapper modelMapper;

    public PagedResponseMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public <S, T> PagedResponse<T> toPagedResponse (Page<S> sourcePage, Class<T> targetClass) {
        List<T> mappedContent = sourcePage.getContent()
                .stream()
                .map(source -> modelMapper.map(source, targetClass))
                .toList();

        return new PagedResponse<>(
                mappedContent,
                sourcePage.getNumber(),
                sourcePage.getSize(),
                sourcePage.getTotalElements(),
                sourcePage.getTotalPages(),
                sourcePage.isLast()
        );
    }
}
