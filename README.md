# cql-maven-plugin

[![Build Status](https://travis-ci.org/bric3/cql-maven-plugin.svg)](https://travis-ci.org/bric3/cql-maven-plugin) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.bric3.maven/cql-maven-plugin/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.bric3.maven/cql-maven-plugin) [![Apache License](http://img.shields.io/badge/license-Apache-blue.svg) ](https://github.com/bric3/cql-maven-plugin/blob/master/LICENSE)


This plugin allows one to execute CQL statements on a cassandra cluster.

## Requirements

* Java 11
* Maven 3.3.3 (maybe older version work, but this has not been tested)
* Apache Cassandra 2.1, 2.2, 3.x, 4.x (driver is 4.11+, see [driver compatibility matrix](https://docs.datastax.com/en/driver-matrix/docs/java-drivers.html))

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
 * `javaDriverClientConfig` : Datastax Java Driver Client configuration file, default is null. Setting this parameters will tell the plugin that the configuration is handled by this file and will ignore `username`,`password`,`keyspace`,`contactPoint`,`port` parameters

#### Current limitations

* Comments are not supported [#5](https://github.com/bric3/cql-maven-plugin/issues/5)
* Batch statements are not supported [#6](https://github.com/bric3/cql-maven-plugin/issues/6)

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
    <groupId>com.github.bric3.maven</groupId>
    <artifactId>cql-maven-plugin</artifactId>
    <version>1.0.0</version>
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

#### Using configuration file

The driver can be configured with a configuration [file](https://docs.datastax.com/en/developer/java-driver/4.14/manual/core/configuration/) (it uses the [lightbend config format](https://github.com/lightbend/config)).

**Known limitation** : Within an execution, you have to specify a dedicated configuration file if you want to override the global plugin configuration. This is a known issue and we're working on this.

Given a Datastax Java Driver Client at path `${project.basedir}/cassandra.conf` and `${project.basedir}/cassandra-install-phase2.conf` defining the previous example plugin configuration :

`${project.basedir}/cassandra.conf`
```
datastax-java-driver {
    basic.contact-points = [ "cassandra.dc1.local:9043" ]
    basic.request.basic.request.timeout = 40000
    basic.load-balancing-policy.local-datacenter = DC1
    basic.session-keyspace = system
    advanced.auth-provider {
        class = PlainTextAuthProvider
        username = ajax
        password = ******************
    }
}
```

`${project.basedir}/cassandra-install-phase2.conf` (provided you use maven resource filtering to replace `${cassandra.model.keyspace}` with the actual value)
```
datastax-java-driver {
    basic.contact-points = [ "cassandra.dc1.local:9043" ]
    basic.request.basic.request.timeout = 40000
    basic.load-balancing-policy.local-datacenter = DC1
    basic.session-keyspace = ${cassandra.model.keyspace}
    advanced.auth-provider {
        class = PlainTextAuthProvider
        username = ajax
        password = ******************
    }
}
```

The plugin config should be updated like this :

```xml
<plugin>
    <groupId>com.github.bric3.maven</groupId>
    <artifactId>cql-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <ignoreMissingFile>true</ignoreMissingFile>
        <logStatements>true</logStatements>
        <javaDriverClientConfig>${project.basedir}/cassandra.conf</javaDriverClientConfig>
    </configuration>
    <executions>
        <execution>
            <id>generate_keyspaces</id>
            <phase>install</phase>
            <goals>
                <goal>execute</goal>
            </goals>
            <configuration>
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
                <javaDriverClientConfig>${project.basedir}/cassandra-install-phase2.conf</javaDriverClientConfig>
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

* The driver may warn users when a name returns multiple IP addresses,
  due to a change in 3.0.0. See : [JAVA-975](https://datastax-oss.atlassian.net/browse/JAVA-975)

  > §13. If a DNS name resolves to multiple A-records, `Cluster.Builder#addContactPoint(String)`
  > will now use all of these addresses as contact points. This gives you the possibility of
  > maintaining contact points in DNS configuration, and having a single, static contact point
  > in your Java code.

