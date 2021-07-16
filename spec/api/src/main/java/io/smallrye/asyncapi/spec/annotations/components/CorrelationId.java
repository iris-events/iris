package io.smallrye.asyncapi.spec.annotations.components;

/**
 * An object that specifies an identifier at design time that can used for message tracing and correlation.
 *
 * For specifying and computing the location of a Correlation ID, a runtime expression is used.
 */
public @interface CorrelationId {
    /**
     * Optional description of the identifier. <a href="http://spec.commonmark.org/">CommonMark syntax</a> can be
     * used for rich text representation.
     *
     * @return description property
     */
    String description() default "";

    /**
     * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#runtimeExpression">Runtime expression</a>
     * that specifies the location of the correlation ID. This field is REQUIRED.
     *
     * @return location property
     */
    String location() default "";
}
