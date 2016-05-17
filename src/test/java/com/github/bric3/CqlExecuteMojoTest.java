package com.github.bric3;

import static org.mockito.Mockito.verify;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import com.datastax.driver.core.Cluster;
import com.github.bric3.CqlExecuteMojo;

public class CqlExecuteMojoTest {

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private Cluster cluster;

    @Test
    public void should_connect_to_cluster_with_a_keyspace() {
        CqlExecuteMojo mojo = new CqlExecuteMojo();
        mojo.keyspace = "keyspace-name";

        mojo.connectTo(cluster);

        verify(cluster).connect("keyspace-name");
    }

    @Test
    public void should_connect_to_cluster_without_any_keyspace() {
        CqlExecuteMojo mojo = new CqlExecuteMojo();
        mojo.keyspace = null;

        mojo.connectTo(cluster);

        verify(cluster).connect();
    }

    @Test
    public void should_connect_to_cluster_without_any_keyspace_when_it_is_empty() {
        CqlExecuteMojo mojo = new CqlExecuteMojo();
        mojo.keyspace = "";

        mojo.connectTo(cluster);

        verify(cluster).connect();
    }

}
