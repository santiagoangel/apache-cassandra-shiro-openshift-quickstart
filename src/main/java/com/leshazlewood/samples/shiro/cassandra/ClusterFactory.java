/*
 * Copyright (C) 2013 Les Hazlewood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.leshazlewood.samples.shiro.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;
import org.apache.shiro.ShiroException;
import org.apache.shiro.util.Destroyable;
import org.apache.shiro.util.Factory;
import org.apache.shiro.util.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Obtains and returns a Cassandra Driver {@link Cluster} object to be used for acquiring Cassandra Driver
 * {@link Session} instances to perform CQL3 queries.
 *
 * @since 2013-06-09
 */
public class ClusterFactory implements Factory<Cluster>, Initializable, Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterFactory.class);

    private String embeddedCassandraConfigPath; //null means don't start embedded cassandra

    private Cluster cluster;

    private Set<String> contactPoints;
    private int port;

    public ClusterFactory() {
        this.contactPoints = new HashSet<String>();
        //this.contactPoints.add("127.0.0.1");
        String host = System.getenv("OPENSHIFT_JBOSSEWS_IP");
        this.contactPoints.add(host);        
        this.port = 19042; //cassandra on openshift
    }

    public String getEmbeddedCassandraConfigPath() {
        return embeddedCassandraConfigPath;
    }

    public void setEmbeddedCassandraConfigPath(String embeddedCassandraConfigPath) {
        this.embeddedCassandraConfigPath = embeddedCassandraConfigPath;
    }

    public Set<String> getContactPoints() {
        return contactPoints;
    }

    public void setContactPoints(Set<String> contactPoints) {
        this.contactPoints = contactPoints;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    protected boolean isEmbedded() {
        return this.embeddedCassandraConfigPath != null;
    }

    public Cluster getInstance() {
        if (cluster == null) {
            init();
        }
        return cluster;
    }

    public void init() throws ShiroException {
        if (cluster == null) {
            try {
                doInit();
            } catch (Exception e) {
                throw new ShiroException(e);
            }
        }
    }

    protected void doInit() throws Exception {
        if (cluster == null) {
            //if (isEmbedded()) {
            //    EmbeddedCassandraServerHelper.startEmbeddedCassandra(this.embeddedCassandraConfigPath);
            //}
            cluster = createCluster();
        }
    }

    protected Cluster createCluster() {
        Cluster.Builder builder = Cluster.builder();

        if (this.contactPoints != null && !this.contactPoints.isEmpty()) {
            String[] values = new String[this.contactPoints.size()];
            this.contactPoints.toArray(values);
            builder.addContactPoints(values);
        }

        builder.withPort(this.port);

        Cluster cluster = builder.build();

        Metadata metadata = cluster.getMetadata();

        LOG.info("Connected to Cassandra cluster: " + metadata.getClusterName());
        for (Host host : metadata.getAllHosts()) {
            LOG.info("DataCenter: {}, Rack: {}, Host: {}",
                    new Object[]{host.getDatacenter(), host.getRack(), host.getAddress()});
        }

        return cluster;
    }

    public void destroy() throws Exception {
        try {
            if (cluster != null) {
                cluster.close();
            }
        } finally {
            cluster = null;
        }
    }
}
