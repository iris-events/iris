package id.global.asyncapi.runtime.io.channel;

import org.jboss.jandex.DotName;

import id.global.asyncapi.spec.annotations.channels.ChannelItem;
import id.global.asyncapi.spec.annotations.channels.ChannelItems;

public class ChannelConstant {

    public static final DotName DOTNAME_CHANNEL = DotName.createSimple(ChannelItem.class.getName());
    public static final DotName DOTNAME_CHANNELS = DotName.createSimple(ChannelItems.class.getName());
    public static final String PROP_NAME = "name";
    public static final String PROP_DESCRIPTION = "description";
}
