package org.nuxeo.lib.stream.tests;

public class TestUtils {


    /** @since 11.1 */
    protected static final String CUSTOM_ENVIRONMENT_SYSTEM_PROPERTY = "custom.environment";

    /** @since 11.1 */
    protected static final String DEFAULT_BUILD_DIRECTORY = "target";

    public static String getBuildDirectory() {
        String customEnvironment = System.getProperty(CUSTOM_ENVIRONMENT_SYSTEM_PROPERTY);
        return customEnvironment == null ? DEFAULT_BUILD_DIRECTORY
                : String.format("%s-%s", DEFAULT_BUILD_DIRECTORY, customEnvironment);
    }

}
