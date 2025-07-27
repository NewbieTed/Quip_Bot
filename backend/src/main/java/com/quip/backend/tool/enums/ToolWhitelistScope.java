package com.quip.backend.tool.enums;

/**
 * Enumeration representing the scope of tool whitelist permissions.
 * <p>
 * This enum defines the different levels at which a tool can be approved for use:
 * - GLOBAL: Tool is approved for use in all contexts
 * - SERVER: Tool is approved for use within a specific server
 * - CONVERSATION: Tool is approved for use within a specific conversation only
 * </p>
 */
public enum ToolWhitelistScope {
    /**
     * Tool is approved for use in all contexts globally.
     */
    GLOBAL("global"),

    /**
     * Tool is approved for use within a specific server.
     */
    SERVER("server"),

    /**
     * Tool is approved for use within a specific conversation only.
     */
    CONVERSATION("conversation");

    private final String value;

    ToolWhitelistScope(String value) {
        this.value = value;
    }

    /**
     * Gets the string value of the enum as stored in the database.
     *
     * @return the string representation of the scope
     */
    public String getValue() {
        return value;
    }

    /**
     * Converts a string value to the corresponding enum value.
     *
     * @param value the string value to convert
     * @return the corresponding ToolWhitelistScope enum value
     * @throws IllegalArgumentException if the value doesn't match any enum value
     */
    public static ToolWhitelistScope fromValue(String value) {
        for (ToolWhitelistScope scope : ToolWhitelistScope.values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown ToolWhitelistScope value: " + value);
    }
}