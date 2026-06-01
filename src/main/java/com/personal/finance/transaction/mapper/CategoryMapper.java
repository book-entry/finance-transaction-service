package com.personal.finance.transaction.mapper;

import com.personal.finance.transaction.dto.response.CategoryRefResponse;
import com.personal.finance.transaction.dto.response.CategoryResponse;
import com.personal.finance.transaction.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    default CategoryRefResponse toRef(Category category, boolean isNew) {
        if (category == null) return null;
        return CategoryRefResponse.builder()
                .id(category.getCategoryId())
                .name(category.getName())
                .isNew(isNew)
                .build();
    }
}
