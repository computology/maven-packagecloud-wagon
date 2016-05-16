package io.packagecloud.maven.wagon;

import org.apache.maven.wagon.repository.Repository;

class PackagecloudRepository {

    private String userName;
    private String repoName;

    PackagecloudRepository(Repository wagonRepository) {
        String baseDir = wagonRepository.getBasedir();
        String[] strings = baseDir.split("/");

        if(strings[1] != null){
            this.userName = strings[1];
        } else {
            throw new IllegalArgumentException(String.format("Cant parse userName from %s", baseDir));
        }

        if(strings[2] != null){
            this.repoName = strings[2];
        } else {
            throw new IllegalArgumentException(String.format("Cant parse repoName from %s", baseDir));
        }
    }

    String getRepoName() {
        return repoName;
    }

    String getUserName() {
        return userName;
    }
}
