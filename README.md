# cql-maven-plugin

[![Build Status](https://travis-ci.org/bric3/cql-maven-plugin.svg)](https://travis-ci.org/bric3/cql-maven-plugin) [![Apache License](http://img.shields.io/badge/license-Apache-blue.svg) ](https://github.com/bric3/cql-maven-plugin/blob/master/LICENSE)


This plugin allows one to execute CQL statements on a cassandra cluster.

**Note this plugin is not deployed on any public maven repo, at the moment it is necessary to deploy it on a privately owned maven repository.**

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

Deploy it with the following command line : 

```bash
mvn versions:set -DnewVersion=0.1
git commit --all --message="Version 0.1"
mvn deploy scm:tag
```

As this plugin is not released on central, it would be preferable to use a suffix to the version it avoid possible collision if this project ever get published on central. That means that version `0.1-myproject` should be used instead of a _raw_ `0.1`.


## Requirements

 * Java 8
 * Maven 3.3.3 (maybe older version work, but this has not been tested)
 * Cassandra 2.0 to 2.1 (driver is 2.1.6)

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

```xml
<plugin>
    <groupId>io.bric3.maven</groupId>
    <artifactId>cql-maven-plugin</artifactId>
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

