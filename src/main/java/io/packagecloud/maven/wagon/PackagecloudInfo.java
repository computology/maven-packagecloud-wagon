package io.packagecloud.maven.wagon;

public class PackagecloudInfo {
    private final String name;
    private final String user;

    public PackagecloudInfo(String user, String repo) {
        this.name = repo;
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public String getUser() {
        return user;
    }
}
