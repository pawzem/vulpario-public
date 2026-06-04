package com.example.booking.modulith;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.mapping.Table;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Rich aggregates expose intention-revealing methods, never JavaBean setters.
 * This ArchUnit rule fails the build on any {@code public void setXxx(arg)}
 * method on a class annotated with {@link Table} (the Spring Data JDBC
 * aggregate-root marker).
 *
 * <p>Why this rule exists: a setter lets any caller put an aggregate into any
 * state, bypassing its invariants and — crucially — bypassing the domain
 * event it should have emitted. A multi-argument command like
 * {@code Tenant.rename(String newName, Instant at)} stays legal: it's not a
 * bean setter, it enforces a rule and emits an event. The single-arg
 * {@code void setName(String)} is exactly what we want to forbid.
 *
 * <p>It's the kind of convention that's easy to state in a code review and
 * easy to forget under deadline — or for a model to reintroduce. Encoded
 * here, it holds the line on every build instead.
 */
class NoPublicSettersInAggregatesTest {

    private static JavaClasses aggregateClasses;

    @BeforeAll
    static void importMainSources() {
        aggregateClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.example.booking");
    }

    @Test
    void no_public_javabean_setters_on_aggregates() {
        classes()
            .that().areAnnotatedWith(Table.class)
            .should(haveNoPublicJavaBeanSetters())
            .check(aggregateClasses);
    }

    private static ArchCondition<JavaClass> haveNoPublicJavaBeanSetters() {
        return new ArchCondition<>("have no public JavaBean setters (single-arg void setX)") {
            @Override
            public void check(JavaClass aggregate, ConditionEvents events) {
                for (JavaMethod method : aggregate.getMethods()) {
                    if (isJavaBeanSetter(method)) {
                        events.add(SimpleConditionEvent.violated(method,
                            "%s exposes JavaBean setter %s — aggregates must use intention-revealing commands"
                                .formatted(aggregate.getName(), method.getFullName())));
                    }
                }
            }
        };
    }

    private static boolean isJavaBeanSetter(JavaMethod method) {
        if (!method.getModifiers().contains(JavaModifier.PUBLIC)) return false;
        if (!method.getRawReturnType().getName().equals("void")) return false;
        if (method.getRawParameterTypes().size() != 1) return false;
        String name = method.getName();
        return name.length() > 3 && name.startsWith("set") && Character.isUpperCase(name.charAt(3));
    }
}
