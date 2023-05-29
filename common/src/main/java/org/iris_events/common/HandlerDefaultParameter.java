package org.iris_events.common;

@SuppressWarnings("unused")
public class HandlerDefaultParameter {

    public static class MessageHandler {
        public static final boolean DURABLE = true;
        public static final boolean AUTO_DELETE = false;
        public static final boolean PER_INSTANCE = false;
        public static final int PREFETCH_COUNT = 1;
    }

    public static class SnapshotMessageHandler {
        public static final boolean DURABLE = false;
        public static final boolean AUTO_DELETE = true;
        public static final boolean PER_INSTANCE = false;
        public static final int PREFETCH_COUNT = 1;
    }
}
