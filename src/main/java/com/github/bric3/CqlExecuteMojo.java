package com.github.bric3;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.google.common.base.Stopwatch;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.datastax.driver.core.SocketOptions.DEFAULT_READ_TIMEOUT_MILLIS;
import static java.nio.file.Files.readAllBytes;
import static java.util.stream.Collectors.joining;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PRE_INTEGRATION_TEST;

/**
 * Executes CQL statements;
 */
@Mojo(name = "execute",
      defaultPhase = PRE_INTEGRATION_TEST,
      threadSafe = true)
public class CqlExecuteMojo extends AbstractMojo {

    public static final Pattern STATEMENT_END = Pattern.compile(";");
    /**
     * Skip the execution.
     */
    @Parameter(property = "cassandra.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * User name for cassandra authentication. Default value <code>cassandra</code>
     */
    @Parameter(property = "cassandra.username", defaultValue = "cassandra")
    protected String username = "cassandra";

    /**
     * Password for cassandra authentication. Default value <code>cassandra</code>
     */
    @Parameter(property = "cassandra.password", defaultValue = "cassandra")
    protected String password = "cassandra";

    /**
     * Address to use for the RPC interface. Do not change this unless you really know what you are doing.
     */
    @Parameter(property = "cassandra.host", defaultValue = "127.0.0.1")
    protected String contactPoint = "127.0.0.1";

    /**
     * Port on which contact points are to be connected.
     */
    @Parameter(property = "cassandra.port", defaultValue = "9042")
    private int port = 9042;

    /**
     * Local cassandra datacenter
     */
    @Parameter(property = "cassandra.datacenter")
    private String localDatacenter;

    /**
     * The keyspace against which individual operations will be executed
     */
    @Parameter(property = "cassandra.keyspace")
    protected String keyspace;

    /**
     * {@link org.apache.maven.model.FileSet}
     */
    @Parameter
    private FileSet fileset;

    /**
     * Ignore missing file
     */
    @Parameter(property = "cassandra.ignoreMissingFile", defaultValue = "true")
    private boolean ignoreMissingFile = true;

    /**
     * Trace statements
     */
    @Parameter(property = "cassandra.logStatements", defaultValue = "false")
    private boolean logStatements = false;

    /**
     * Default timeout longer than usual because this plugin may execute rare but long running
     * statements like dropping keyspace. The execution can be even longer when the node is running on a VM.
     */
    @Parameter(property = "cassandra.readTimeoutMillis")
    private int readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS * 4;


    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping cassandra: cassandra.skip=true");
            return;
        }
        if (fileset == null) {
            getLog().info("No scripts specified, nothing to do");
            return;
        }

        try (Cluster cluster = cluster();
             Session session = connectTo(cluster)) {
            cqlFiles(fileset).stream()
                             .flatMap(this::readContent)
                             .flatMap(this::toStatements)
                             .peek(this::logStatement)
                             .forEach(new StatementExecutor(session)::execute);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
    
    Session connectTo(Cluster cluster) {
        if (Objects.isNull(keyspace) || keyspace.isEmpty()) {
            return cluster.connect();
        }
        return cluster.connect(keyspace);
    }

    private void logStatement(String statement) {
        if (logStatements) {
            getLog().info("Applying : '" + statement.replaceAll("[\\t\\r\\n ]+", " ") + ";'");
        }
    }

    private Stream<String> toStatements(String content) {
        return STATEMENT_END.splitAsStream(content)
                            .map(String::trim)
                            .filter((s) -> !s.isEmpty());
    }

    private Stream<String> readContent(File file) {
        if (!file.exists()) {
            return onMissingFile(file);
        }

        getLog().info("Executing file: '" + file.toPath() + "'");
        try {
            return Stream.of(new String(readAllBytes(file.toPath())));
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    private Stream<String> onMissingFile(File file) {
        if (ignoreMissingFile) {
            getLog().error("Specified script " + file.getAbsolutePath() + " does not exist. Ignoring as 'ignoreMissingFile' is true");
            return Stream.empty();
        } else {
            throw new RuntimeException(new MojoFailureException("Specified script " + file.getAbsolutePath() + " does not exist."));
        }
    }

    private Cluster cluster() {
        return Cluster.builder()
                      .addContactPoints(contactPoint).withPort(port)
                      .withCredentials(username, password)
                      .withSocketOptions(new SocketOptions().setKeepAlive(true)
                                                            .setReadTimeoutMillis(readTimeoutMillis))
                      .withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder()
                                                                      .withLocalDc(localDatacenter)
                                                                      .build())
                      .build();
    }


    public static List<File> cqlFiles(FileSet fileSet) throws IOException {
        File directory = new File(fileSet.getDirectory());
        String includes = fileSet.getIncludes().stream().collect(joining(", "));
        String excludes = fileSet.getExcludes().stream().collect(joining(", "));

        @SuppressWarnings("unchecked")
        List<File> files = FileUtils.getFiles(directory, includes, excludes);

        Collections.sort(files);
        return files;
    }

    public class StatementExecutor {
        private Session session;

        public StatementExecutor(Session session) {
            this.session = session;
        }

        public void execute(String statement) {
            Stopwatch watch = Stopwatch.createUnstarted();
            try {
                watch.start();
                session.execute(statement);
            } catch (Exception e) {
                getLog().error("Failing statement '" + statement + "'", e);
            } finally {
                if (logStatements) {
                    getLog().info("Statement took : " + watch);
                }
            }
        }
    }
}
