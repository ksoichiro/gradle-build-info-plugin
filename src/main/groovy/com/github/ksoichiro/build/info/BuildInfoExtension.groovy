package com.github.ksoichiro.build.info

class BuildInfoExtension {
    public static final NAME = "buildInfo"
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z"
    String committerDateFormat = DEFAULT_DATE_FORMAT
    String buildDateFormat = DEFAULT_DATE_FORMAT
    boolean manifestEnabled = true
    boolean gitPropertiesEnabled = false
    boolean attributeGitBranchEnabled = true
    boolean attributeGitCommitEnabled = true
    boolean attributeGitCommitterDateEnabled = true
    boolean attributeBuildDateEnabled = true
    boolean attributeBuildJavaVersionEnabled = true
    boolean attributeBuildJavaVendorEnabled = true
    boolean attributeBuildOsNameEnabled = true
    boolean attributeBuildOsVersionEnabled = true
}
