package io.smallrye.asyncapi.spec.annotations.bindings.operation;

public @interface HttpBindings {
    /**
     * Required type of operation. Its value MUST be either {@code request} or {@code response}.
     *
     * @return type property
     */
    String type() default "";

    /**
     * When {@code type} is {@code request}, the HTTP method, otherwise it MUST be ignored.
     * Its value MUST be one of {@code GET}, {@code POST}, {@code PUT}, {@code PATCH}, {@code DELETE}, {@code HEAD},
     * {@code OPTIONS}, {@code CONNECT}, and {@code TRACE}.
     *
     * @return method property
     */
    String method() default "";

    /**
     * Version of this binding. If omitted, "latest" MUST be assumed.
     *
     * @return binding version property
     */
    String bindingVersion() default "latest";
}
