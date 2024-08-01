package com.hillayes.rail.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.rail.domain.CategoryGroup;

public class CategoryGroupAlreadyExistsException extends MensaException {
    public CategoryGroupAlreadyExistsException(CategoryGroup categoryGroup) {
        super(RailsErrorCodes.CATEGORY_GROUP_ALREADY_EXISTS);
        addParameter("id", categoryGroup.getId());
        addParameter("name", categoryGroup.getName());
    }
}
