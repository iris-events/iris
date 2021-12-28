package id.global.common.auth.jwt;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    AUTHENTICATED("AUTHENTICATED"),
    GROUP_OWNER("GROUP_OWNER"),
    ADMIN_WALLET("admin.wallet"),
    ADMIN_REWARD("admin.reward"),
    ADMIN_MERCHANT("admin.merchant");

    private final String value;

    Role(final String value) {
        this.value = value;
    }

    public static Role fromValue(String value) {
        for (Role assetType : Role.values()) {
            if (String.valueOf(assetType.value).equals(value)) {
                return assetType;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    public static Optional<Role> findFromValue(String value) {
        try {
            return Optional.of(fromValue(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
