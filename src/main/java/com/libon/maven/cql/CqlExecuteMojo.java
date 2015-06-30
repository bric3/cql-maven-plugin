package com.libon.maven.cql;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PRE_INTEGRATION_TEST;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;

/**
 * Executes CQL statements;
 *
 * @threadSafe
 */
@Mojo(name = "execute",
        defaultPhase = PRE_INTEGRATION_TEST,
        threadSafe = true)
public class CqlExecuteMojo extends AbstractMojo {

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
             Session session = cluster.connect(keyspace)) {
            cqlFiles(fileset).stream()
                             .flatMap(Sneaky.function(this::readContent))
                             .flatMap(this::toStatements)
                             .peek(this::logStatement)
                             .forEach(new StatementExecutor(session)::execute);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

    private void logStatement(String statement) {
        if(logStatements) {
            getLog().info("Applying : '" + statement.replaceAll("[\\t\\r\\n ]+", " ") + "'");
        }
    }

    private Stream<String> toStatements(String content) {

        return Pattern.compile(";")
                      .splitAsStream(content)
                      .map(String::trim)
                      .filter((s) -> !s.isEmpty());
    }

    private Stream<String> readContent(File file) throws MojoFailureException, IOException {
        if (!file.exists()) {
            return onMissingFile(file);
        }

        getLog().info("Executing file: '" + file.toPath() + "'");
        return Stream.of(new String(Files.readAllBytes(file.toPath())));
    }

    private Stream<String> onMissingFile(File file) throws MojoFailureException {
        if (ignoreMissingFile) {
            getLog().error("Specified script " + file.getAbsolutePath() + " does not exist. Ignoring as loadFailureIgnore is true");
            return Stream.empty();
        } else {
            throw new MojoFailureException("Specified script " + file.getAbsolutePath() + " does not exist.");
        }
    }

    private Cluster cluster() {
        SocketOptions socketOptions = new SocketOptions();
        socketOptions.setKeepAlive(true);

        return Cluster.builder().addContactPoints(contactPoint).withPort(port)
                      .withCredentials(username, password)
                      .withSocketOptions(socketOptions)
                      .withLoadBalancingPolicy(new DCAwareRoundRobinPolicy(localDatacenter))
                      .build();
    }


    public static List<File> cqlFiles(FileSet fileSet) throws IOException {
        File directory = new File(fileSet.getDirectory());
        String includes = toString(fileSet.getIncludes());
        String excludes = toString(fileSet.getExcludes());

        @SuppressWarnings("unchecked")
        List<File> files = FileUtils.getFiles(directory, includes, excludes);

        Collections.sort(files);
        return files;
    }

    private static String toString(List<String> strings) {
        StringBuilder sb = new StringBuilder();
        for (String string : strings) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(string);
        }
        return sb.toString();
    }

    public class StatementExecutor {
        private Session session;

        public StatementExecutor(Session session) {
            this.session = session;
        }

        public void execute(String statement) {
            try {
                session.execute(statement);
            } catch (Exception e) {
                CqlExecuteMojo.this.getLog().error("Failing statement '" + statement + "'", e);
            }
        }
    }
}
