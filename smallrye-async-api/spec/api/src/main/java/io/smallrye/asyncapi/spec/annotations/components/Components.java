package io.smallrye.asyncapi.spec.annotations.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.asyncapi.spec.annotations.Parameter;
import io.smallrye.asyncapi.spec.annotations.bindings.ChannelBindings;
import io.smallrye.asyncapi.spec.annotations.bindings.MessageBindings;
import io.smallrye.asyncapi.spec.annotations.bindings.OperationBindings;
import io.smallrye.asyncapi.spec.annotations.bindings.ServerBindings;
import io.smallrye.asyncapi.spec.annotations.media.Schema;
import io.smallrye.asyncapi.spec.annotations.security.SecurityScheme;

/**
 * Holds a set of reusable objects for different aspects of the AsyncAPI specification.
 * All objects defined within the components object will have no effect on the API unless they are explicitly referenced
 * from properties outside the components object.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Components {
    /**
     * Schemas property from a Components instance.
     *
     * @return a copy Map (potentially immutable) containing the keys and the reusable schemas for this AsyncAPI document
     **/
    Schema[] schemas() default {};

    /**
     * Messages property from a Components instance.
     *
     * @return a copy Map containing the keys and the defined messages for this AsyncAPI document
     */
    Message[] messages() default {};

    /**
     * SecuritySchemes property from a Components instance.
     *
     * @return a copy Map (potentially immutable) containing the keys and the reusable security schemes for this AsyncAPI
     *         document
     **/
    SecurityScheme[] securitySchemes() default {};

    /**
     * Parameters property from a Components instance.
     *
     * @return a copy Map (potentially immutable) containing the keys and the reusable parameters of API operations for this
     *         AsyncAPI document
     **/
    Parameter[] parameters() default {};

    /**
     * CorrelationIds property from a Components instance.
     *
     * @return a copy Map (potentially immutable) containing the keys and the reusable correlationIds of API operations for
     *         this AsyncAPI document
     **/
    CorrelationId[] correlationIds() default {};

    /**
     * OperationTraits property from a Components instance.
     *
     * @return a copy Map (potentially immutable) containing the keys and the reusable operationTraits of API operations
     *         for this AsyncAPI document
     **/
    OperationTrait[] operationTraits() default {};

    /**
     * MessageTraits property from a Components instance.
     *
     * @return a copy Map (potentially immutable) containing the keys and the reusable messageTraits of API operations for
     *         this AsyncAPI document
     **/
    MessageTrait[] messageTraits() default {};

    /**
     * List that holds reusable Server Bindings Objects
     *
     * @return Map of ServerBinding objects
     */
    ServerBindings[] serverBindings() default {};

    /**
     * List that holds reusable Server Bindings Objects
     *
     * @return Map of ChannelBinding objects
     */
    ChannelBindings[] channelBindings() default {};

    /**
     * List that holds reusable Server Bindings Objects
     *
     * @return Map of OperationBinding objects
     */
    OperationBindings[] operationBindigns() default {};

    /**
     * List that holds reusable Server Bindings Objects
     *
     * @return Map of MessageBinding objects
     */
    MessageBindings[] messageBindings() default {};
}
