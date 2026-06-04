package com.example.booking.modulith;

import com.example.booking.shared.web.BoundedList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mechanically enforces: <em>every list-returning controller method must
 * paginate, or be explicitly marked {@link BoundedList}.</em> An endpoint that
 * returns a raw {@code List}/{@code Set}/{@code Collection} (even wrapped in
 * {@code ResponseEntity}) is an unbounded query waiting to happen the day a
 * tenant gets big. {@code Page<T>} returns are compliant by construction.
 *
 * <p>Pagination is the canonical "everyone agrees, everyone forgets" rule —
 * and exactly the sort of thing an AI will happily return a bare {@code List}
 * for, because the prompt didn't mention paging. So it's a build-failing test:
 * the regression can't reach {@code main}.
 */
class PaginationVerificationTest {

    private static final List<Class<? extends Annotation>> MAPPING_ANNOTATIONS = List.of(
        GetMapping.class, PostMapping.class, PutMapping.class,
        PatchMapping.class, DeleteMapping.class, RequestMapping.class);

    @Test
    void controller_list_methods_must_paginate_or_be_bounded() throws ClassNotFoundException {
        List<String> violations = new ArrayList<>();

        for (Class<?> controller : findRestControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!isHandlerMethod(method)) continue;
                if (!returnsRawCollection(method.getGenericReturnType())) continue;

                BoundedList bounded = method.getAnnotation(BoundedList.class);
                if (bounded != null) {
                    if (bounded.reason().isBlank()) {
                        violations.add(controller.getSimpleName() + "#" + method.getName()
                            + " — @BoundedList requires a non-blank reason()");
                    }
                    continue;
                }
                if (hasPageableParameter(method)) continue;

                violations.add(controller.getName() + "#" + method.getName()
                    + " returns a raw collection — add a Pageable (return Page<T>) or"
                    + " annotate @BoundedList(reason=\"…\") if the result set is bounded by domain rules");
            }
        }

        assertThat(violations)
            .as("Controllers must paginate growing lists; see %s", BoundedList.class.getName())
            .isEmpty();
    }

    private static List<Class<?>> findRestControllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition bean : scanner.findCandidateComponents("com.example.booking")) {
            String name = bean.getBeanClassName();
            if (name != null) classes.add(Class.forName(name));
        }
        return classes;
    }

    private static boolean isHandlerMethod(Method method) {
        for (Class<? extends Annotation> a : MAPPING_ANNOTATIONS) {
            if (method.isAnnotationPresent(a)) return true;
        }
        return false;
    }

    /** True for a raw {@code List/Set/Iterable/Collection}, including wrapped in {@code ResponseEntity<…>}. */
    private static boolean returnsRawCollection(Type returnType) {
        Type unwrapped = unwrapResponseEntity(returnType);
        if (!(unwrapped instanceof ParameterizedType pt)) return false;
        if (!(pt.getRawType() instanceof Class<?> cls)) return false;
        return List.class.isAssignableFrom(cls)
            || Set.class.isAssignableFrom(cls)
            || cls == Iterable.class
            || cls == Collection.class;
    }

    private static Type unwrapResponseEntity(Type type) {
        if (type instanceof ParameterizedType pt
            && pt.getRawType() instanceof Class<?> cls
            && cls.getName().equals("org.springframework.http.ResponseEntity")) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length == 1) return args[0];
        }
        return type;
    }

    private static boolean hasPageableParameter(Method method) {
        for (Class<?> p : method.getParameterTypes()) {
            if (Pageable.class.isAssignableFrom(p)) return true;
        }
        return false;
    }
}
