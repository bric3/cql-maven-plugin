# cql-maven-plugin

[![Build Status](https://travis-ci.org/bric3/cql-maven-plugin.svg)](https://travis-ci.org/bric3/cql-maven-plugin) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.bric3.maven/cql-maven-plugin/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.bric3.maven/cql-maven-plugin) [![Apache License](http://img.shields.io/badge/license-Apache-blue.svg) ](https://github.com/bric3/cql-maven-plugin/blob/master/LICENSE)


This plugin allows one to execute CQL statements on a cassandra cluster.

## Requirements

 * Java 8
 * Maven 3.3.3 (maybe older version work, but this has not been tested)
 * Apache Cassandra 1.2, 2.0, 2.1, 2.2 or 3.0 (driver is 3.1.0, see [driver compatibility matrix](http://docs.datastax.com/en/developer/java-driver//3.1/manual/native_protocol/))

## Configuration

This plugin has a single goal (`execute`) and runs by default in the `pre-integration-test` phase.

#### Mandatory parameters :

 * `localDatacenter` : the datacenter name
 * `fileset` : include / exclude CQL files

#### Optional parameters :

 * `username` : login, default is `cassandra`
 * `password` : password, default is `cassandra`
 * `keyspace` : the cassandra keyspace on which statement will be applied
 * `contactPoint` : single contact point, default is `127.0.0.1`
 * `port` : CQL3 or native port, default is `9042`
 * `ignoreMissingFile` : should ignore when a file is not found, if `false` mojo will fail, default is `true`
 * `logStatements` : print every executed statement, default is `false`
 * `skip` : skip plugin execution, default is of course `false`

### Example

If you have CQL files like (note the semi-column `;` otherwise statements are not correctly identified) :

* ```cql
  DROP KEYSPACE IF EXISTS my_keyspace;
  ```

* ```cql
  CREATE KEYSPACE my_keyspace 
  WITH replication = { 
      'class':'NetworkTopologyStrategy', 
      'dc1': 1} 
  AND durable_writes = 'false';
  ```

* ```cql
  CREATE TABLE user_credentials (
     email text,
     password text,
     userid uuid,
     PRIMARY KEY (email)
  );
  
  CREATE TABLE users (
     userid uuid,
     firstname varchar,
     lastname varchar,
     email text,
     created_date timestamp,
     PRIMARY KEY (userid)
  );
  
  CREATE TYPE video_metadata (
     height int,
     width int,
     video_bit_rate set<text>,
     encoding text
  );
  ``` 

And you have the following plugin definition :

```xml
<plugin>
    <groupId>com.github.bric3</groupId>
    <artifactId>cql-maven-plugin</artifactId>
    <version>0.4</version>
    <configuration>
        <username>ajax</username>
        <password>******************</password>
        <localDatacenter>DC1</localDatacenter>
        <contactPoint>cassandra.dc1.local</contactPoint>
        <port>9043</port>
        <ignoreMissingFile>true</ignoreMissingFile>
        <logStatements>true</logStatements>
    </configuration>
    <executions>
        <execution>
            <id>generate_keyspaces</id>
            <phase>install</phase>
            <goals>
                <goal>execute</goal>
            </goals>
            <configuration>
                <keyspace>system</keyspace>
                <fileset>
                    <directory>${project.build.directory}/classes/cassandra/all-keyspaces</directory>
                    <includes>
                        <include>*.cql3</include>
                    </includes>
                    <excludes>
                        <exclude>${cassandra.cql3.exclude}</exclude>
                    </excludes>
                </fileset>
            </configuration>
        </execution>
        <execution>
            <id>rebuild_model_keyspace_cql3</id>
            <phase>install</phase>
            <goals>
                <goal>execute</goal>
            </goals>
            <configuration>
                <keyspace>${cassandra.model.keyspace}</keyspace>
                <fileset>
                    <directory>${project.build.directory}/classes/cassandra/model</directory>
                    <includes>
                        <include>*.cql3</include>
                    </includes>
                    <excludes>
                        <exclude>${cassandra.cql3.exclude}</exclude>
                    </excludes>
                </fileset>
            </configuration>
        </execution>
    </executions>
</plugin>

```


### Notes

* The driver may warn users when a name returns multiple IP addresses, due to a change in 3.0.0. See : [JAVA-975](https://datastax-oss.atlassian.net/browse/JAVA-975)

  > §13. If a DNS name resolves to multiple A-records, `Cluster.Builder#addContactPoint(String)` will now use all of these addresses as contact points. This gives you the possibility of maintaining contact points in DNS configuration, and having a single, static contact point in your Java code.


## For cql-maven-plugin developers 

### To install locally :

```bash
mvn clean install
```

### To deploy a private maven repo :

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

### To deploy on central

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

Or use `./maven-central-deploy.sh`

Make sure env is set up properly, more info in OSSRH.md file.
