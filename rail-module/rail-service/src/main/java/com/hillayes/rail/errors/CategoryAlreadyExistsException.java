package com.hillayes.rail.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.rail.domain.Category;

public class CategoryAlreadyExistsException extends MensaException {
    public CategoryAlreadyExistsException(Category category) {
        super(RailsErrorCodes.CATEGORY_ALREADY_EXISTS);
        addParameter("id", category.getId());
        addParameter("name", category.getName());
    }
}
