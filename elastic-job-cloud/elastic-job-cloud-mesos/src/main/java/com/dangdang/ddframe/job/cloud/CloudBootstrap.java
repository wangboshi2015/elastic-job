/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.cloud;

import com.dangdang.ddframe.job.cloud.mesos.SchedulerEngine;
import com.dangdang.ddframe.job.cloud.rest.RestfulApi;
import com.dangdang.ddframe.reg.base.CoordinatorRegistryCenter;
import com.dangdang.ddframe.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.reg.zookeeper.ZookeeperRegistryCenter;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * 作业云启动入口.
 */
public final class CloudBootstrap {
    
    // CHECKSTYLE:OFF
    public static void main(final String[] args) throws Exception {
    // CHECKSTYLE:ON
        Properties properties = new Properties();
        properties.load(new FileInputStream("conf/server.properties"));
        String zKServers = properties.getProperty("zkServers", "localhost:2181");
        String zKNameSpace = properties.getProperty("zKNameSpace", "elastic-job-cloud");
        String userName = properties.getProperty("userName", "");
        String frameWorkName = properties.getProperty("frameWorkName", "Elastic-Job-Cloud");
        String mesosUrl = properties.getProperty("mesosUrl", "zk://localhost:2181/mesos");
        ZookeeperConfiguration zkConfig = new ZookeeperConfiguration(zKServers, zKNameSpace);
        CoordinatorRegistryCenter regCenter = new ZookeeperRegistryCenter(zkConfig);
        regCenter.init();
        Protos.FrameworkInfo frameworkInfo = Protos.FrameworkInfo.newBuilder().setUser(userName).setName(frameWorkName).build();
        MesosSchedulerDriver schedulerDriver = new MesosSchedulerDriver(new SchedulerEngine(regCenter), frameworkInfo, mesosUrl);
        startRestfulServer(regCenter);
        Protos.Status status = schedulerDriver.run();
        schedulerDriver.stop();
        System.exit(Protos.Status.DRIVER_STOPPED == status ? 0 : -1);
    }
    
    private static void startRestfulServer(final CoordinatorRegistryCenter regCenter) throws Exception {
        RestfulApi.init(regCenter);
        // TODO 可配置
        int port = 8899;
        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(getServletHolder(), "/*");
        server.start();
    }
    
    private static ServletHolder getServletHolder() {
        ServletHolder result = new ServletHolder(ServletContainer.class);
        result.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
        result.setInitParameter("com.sun.jersey.config.property.packages", "com.dangdang.ddframe.job.cloud.schedule");
        result.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        result.setInitParameter("resteasy.scan.providers", "true");
        result.setInitParameter("resteasy.use.builtin.providers", "false");
        return result;
    }
}
