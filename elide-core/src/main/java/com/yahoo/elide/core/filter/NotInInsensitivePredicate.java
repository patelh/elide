/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.Path.PathElement;

import java.util.Arrays;
import java.util.List;

/**
 * NOT Insensitive Predicate class.
 */
public class NotInInsensitivePredicate extends FilterPredicate {

    public NotInInsensitivePredicate(Path path, List<Object> values) {
        super(path, Operator.NOT_INSENSITIVE, values);
    }

    @SafeVarargs
    public <T> NotInInsensitivePredicate(Path path, T... a) {
        this(path, Arrays.asList(a));
    }

    public NotInInsensitivePredicate(PathElement pathElement, List<Object> values) {
        super(pathElement, Operator.NOT_INSENSITIVE, values);
    }

    @SafeVarargs
    public <T> NotInInsensitivePredicate(PathElement pathElement, T... a) {
        this(pathElement, Arrays.asList(a));
    }
}