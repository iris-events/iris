package io.smallrye.asyncapi.spec.annotations.channels;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.asyncapi.spec.annotations.Operation;
import io.smallrye.asyncapi.spec.annotations.Parameter;
import io.smallrye.asyncapi.spec.annotations.bindings.ChannelBindings;

/**
 * A representation of a channel. Describes the operation available on a single channel.
 *
 * @see io.smallrye.asyncapi.spec.models.channels.ChannelItem
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ChannelItems.class)
@Inherited
public @interface ChannelItem {
    /**
     * The name of the channel item property.
     * <p>
     * The name is REQUIRED when the channel is defined within
     * {@link io.smallrye.asyncapi.spec.annotations.channels.ChannelItems}.
     * The name will be used as the key to add this channel to the channels map for reuse.
     * </p>
     *
     * @return the name of the channel
     */
    String name() default "";

    /**
     * An optional description of this channel item.
     * <a href="http://spec.commonmark.org/">CommonMark</a> syntax can be used for rich text representation.
     *
     * @return a description of this channel
     */
    String description() default "";

    /**
     * Definition of the SUBSCRIBE operation, which defines the messages produced by the application
     * and sent to the channel.
     *
     * @return Subscribe operation of this channel
     */
    Operation subscribe() default @Operation();

    /**
     * Definition of the PUBLISH operation, which defines the messages produced by the application
     * and sent to the channel.
     *
     * @return Publish operation of this channel
     */
    Operation publish() default @Operation();

    /**
     * A list of parameters included in a channel name.
     * This map MUST contain all the parameters used in the parent channel name.
     *
     * @return a map containing channel name parameters
     */
    Parameter[] parameters() default {};

    /**
     * A list of protocol-specific definitions for the channel.
     *
     * @return a list of channel protocol bindings
     */
    ChannelBindings bindings() default @ChannelBindings();

}
