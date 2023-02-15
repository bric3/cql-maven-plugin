# CONTRIBUTING

## To install locally :

```bash
mvn clean install
```

## Via Github releases (which will trigger a workflow)

Repo is has to be configured with the following secrets

* `OSSRH_USERNAME`, which should be fed in environment variable `MAVEN_USERNAME`
* `OSSRH_TOKEN`, which should be fed in environment variable `MAVEN_PASSWORD`
* `MAVEN_GPG_PRIVATE_KEY`, which should be extracted from my GPG keyring using `gpg --armor --export-secret-keys <KEY ID>`
* `MAVEN_GPG_PASSPHRASE`, which should be fed in the environment variable `MAVEN_GPG_PASSPHRASE`


## To deploy a private maven repo :

Assuming the `pom.xml` is patched with a `distributionManagement` element like

```xml
<distributionManagement>
    <repository>
        <id>our-thirdparty</id>
        <name>Our Third Party Repository</name>
        <url>https://host/nexus/content/repositories/thirdparty/</url>
    </repository>
</distributionManagement>
```

Deploy it locally with the following command line :

```bash
mvn versions:set -DnewVersion=0.x-myproject
git commit --all --message="Version 0.x-myproject"
mvn deploy scm:tag
```

This plugin is released on central, but if crafting your own version it would be preferable to use a suffix to the version to avoid possible collision with an the coordinate of an artifact deployed on central. That means that version `0.1-myproject` should be used instead of a _raw_ `0.1`.

## To manually deploy on central

Make sure the `settings.xml` have the following information

```xml
<servers>
    <server>
        <id>ossrh</id>
        <username>login</username>
        <password>password</password>
    </server>
</servers>
```

```xml
<profiles>
    <profile>
        <id>ossrh</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <gpg.keyname>keyname</gpg.keyname>
            <gpg.executable>gpg2</gpg.executable>
            <gpg.passphrase>passphrase</gpg.passphrase>
        </properties>
    </profile>
</profiles>
```

And perform manual steps, like :

```bash
mvn versions:set -DnewVersion=0.4
git commit --all --message="Version 0.4"
git tag cql-maven-plugin-0.4
mvn -Prelease deploy
```

Or use `./maven-central-deploy.sh`.

Make sure env is set up properly, more info in OSSRH.md file.
