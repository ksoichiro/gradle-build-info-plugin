package com.github.ksoichiro.build.info

class BuildInfoExtension {
    public static final NAME = "buildInfo"
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z"

    /**
     * When the plugin cannot read .git directory,
     * set the values of branch, commit, and committer date to "unknown",
     * then proceed task.
     */
    public static final int MODE_DEFAULT = 0

    /**
     * When the plugin cannot read .git directory,
     * ignore it and proceed task.
     */
    public static final int MODE_IGNORE = 1

    /**
     * When the plugin cannot read .git directory,
     * throw an exception to stop build.
     */
    public static final int MODE_ERROR = 2

    String committerDateFormat = DEFAULT_DATE_FORMAT
    String buildDateFormat = DEFAULT_DATE_FORMAT
    boolean manifestEnabled = true
    boolean gitPropertiesEnabled = false
    int gitInfoMode = MODE_DEFAULT
    boolean warnIfGitDirectoryIsMissing = true
    boolean attributeGitBranchEnabled = true
    boolean attributeGitCommitEnabled = true
    boolean attributeGitCommitterDateEnabled = true
    boolean attributeBuildDateEnabled = true
    boolean attributeBuildJavaVersionEnabled = true
    boolean attributeBuildJavaVendorEnabled = true
    boolean attributeBuildOsNameEnabled = true
    boolean attributeBuildOsVersionEnabled = true
    File destinationDir
}
