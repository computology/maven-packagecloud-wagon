# Packagecloud Maven Wagon

### NOTE: this is currently in beta

## Usage

**Visit [packagecloud.io/api_token](packagecloud.io/api_token) and get your API token before proceeding.**

#### Install the wagon under `build/extensions` in your `pom.xml`
```xml
  <build>
    <extensions>
      <extension>
        <groupId>io.packagecloud.maven.wagon</groupId>
        <artifactId>maven-packagecloud-wagon</artifactId>
        <version>0.0.4</version>
      </extension>
    </extensions>
    ...
  </build>
```

#### Set up your `distributionManagment` to point to your packagecloud repository, like so:

(We are setting our snapshot and release repositories to the same, feel free to make them different)

```xml
  <distributionManagement>
    <repository>
      <id>packagecloud.my_repo_releases</id>
      <url>packagecloud+http://packagecloud.dev:3000/testy/my_repo</url>
    </repository>
    <snapshotRepository>
      <id>packagecloud.my_repo_snapshots</id>
      <url>packagecloud+http://packagecloud.dev:3000/testy/my_repo</url>
    </snapshotRepository>
  </distributionManagement>
```

### Configure your password in `~/.m2/settings.xml`

Make sure the `id` matches up with your `distributionManagement` id's

```xml
<settings>
  <servers>
    <server>
      <id>packagecloud.my_repo_releases</id>
      <password>47fd797eff2bbc3b4ea1879de3020269071f6cb006515e93</password>
    </server>
    <server>
      <id>packagecloud.my_repo_snapshots</id>
      <password>47fd797eff2bbc3b4ea1879de3020269071f6cb006515e93</password>
    </server>
  </servers>
</settings>
```

You can encrypt these passwords by following the instructions for [Apache Maven Password Encryption](https://maven.apache.org/guides/mini/guide-encryption.html)

### `mvn deploy`
That's it!
