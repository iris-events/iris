package id.global.common.auth.jwt;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    AUTHENTICATED("AUTHENTICATED"),
    GROUP_OWNER("GROUP_OWNER"),
    ADMIN_WALLET("admin.wallet"),
    ADMIN_REWARD("admin.reward"),
    ADMIN_MERCHANT("admin.merchant"),
    ADMIN_GROUP_CAPABILITIES_GROUPS_OF_GROUPS("admin.group-capabilities.groups-of-groups"),
    ADMIN_GROUP_CARDS("admin.group_cards"),
    ADMIN_IDENTITY_RESTORE("admin.identity-restore"),
    ADMIN_IDENTITY_RESTORE_SUPERVISOR("admin.identity-restore-supervisor"),
    ADMIN_ATTESTATION_AGENCY("admin.attestation-agency"),
    ADMIN_WHITELISTED_PHONE_NUMBERS("admin.whitelisted-phone-numbers"),
    ADMIN_RESERVED_NAMES("admin.reserved-names"),
    ADMIN_RESERVED_NAMES_CLAIM("admin.reserved-names.claim"),
    ADMIN_NOTIFICATION_TEMPLATE_SETS("admin.notification-template-sets"),
    ADMIN_NOTIFICATIONS("admin.notifications"),
    ADMIN_ATTESTATION_APP_MANAGER("admin.attestation-app-manager"),
    ADMIN_TOS("admin.tos"),
    ADMIN_WHITELISTED_GROUP_LIMIT("admin.whitelisted-group-limit"),
    ADMIN_WHITELISTED_RECOVERY_IDENTITY("admin.whitelisted-recovery-identity"),
    ADMIN_HARDWIRED_BADGES("admin.hardwired-badges");

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
