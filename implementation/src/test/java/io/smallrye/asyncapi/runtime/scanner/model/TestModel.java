package io.smallrye.asyncapi.runtime.scanner.model;

import java.util.List;
import java.util.Map;

import io.smallrye.asyncapi.spec.annotations.media.Schema;

@Schema(name = "TestModel")
public class TestModel {
    private List<User> userList;
    private Map<String, User> userMap;
    private Status status;

    public TestModel(List<User> userList, Map<String, User> userMap, Status status) {
        this.userList = userList;
        this.userMap = userMap;
        this.status = status;
    }

    public List<User> getUserList() {
        return userList;
    }

    public Map<String, User> getUserMap() {
        return userMap;
    }

    public Status getStatus() {
        return status;
    }
}
