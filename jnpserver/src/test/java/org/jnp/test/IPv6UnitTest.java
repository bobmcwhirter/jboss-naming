/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jnp.test;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Properties;

import javax.naming.Name;

import junit.framework.Test;

import org.jboss.test.BaseTestCase;
import org.jnp.interfaces.Naming;
import org.jnp.interfaces.NamingContext;
import org.jnp.server.Main;
import org.jnp.server.NamingBeanImpl;

/**
 * Tests of IPv6 addresses
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 81238 $
 */
public class IPv6UnitTest extends BaseTestCase
{
   /** The actual namingMain service impl bean */
   private static NamingBeanImpl namingBean;
   /** */
   private static Main namingMain = new Main("org.jnp.server");

   static int serverPort;
   public static Test suite()
   {
      return suite(IPv6UnitTest.class);
   }
   
   public IPv6UnitTest(String name)
   {
      super(name);
   }

   protected void setUp() throws Exception
   {
      super.setUp();
      if (namingBean != null)
         return;

      InetAddress localhost = InetAddress.getByName("localhost");
      InetAddress localhostIPv6 = InetAddress.getByName("::ffff:127.0.0.1");

      // Set the java.rmi.server.hostname to the bind address if not set
      if(System.getProperty("java.rmi.server.hostname") == null)
      {
         log.debug("Set java.rmi.server.hostname to localhost");
         System.setProperty("java.rmi.server.hostname", "localhost");
      }
      namingBean = new NamingBeanImpl();
      namingBean.start();
      namingMain.setPort(0);
      namingMain.setBindAddress("localhost");
      InetAddress[] addresses = {
            localhost,
            localhostIPv6,
            };
      namingMain.setBindAddresses(Arrays.asList(addresses));
      namingMain.setNamingInfo(namingBean);
      namingMain.start();
      serverPort = namingMain.getPort();
   }
   protected void tearDown() throws Exception
   {
      super.tearDown();
   }

   /**
    * Access the naming instance over the ipv4 localhost address
    * @throws Exception
    */
   public void testNamingContextIPv4Localhost()
      throws Exception
   {
      Properties env = new Properties();
      env.setProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
      env.setProperty("java.naming.provider.url", "localhost:"+serverPort);
      env.setProperty("java.naming.factory.url", "org.jboss.naming:org.jnp.interfaces");
      Name baseName = null;
      Naming server = null;
      NamingContext nc = new NamingContext(env, baseName, server);
      nc.list("");
   }
   /**
    * Access the naming instance over the ipv6 localhost address
    * @throws Exception
    */
   public void testNamingContextIPv6Localhost()
      throws Exception
   {
      Properties env = new Properties();
      InetAddress localhost = InetAddress.getByName("localhost");
      InetAddress localhostIPv6 = InetAddress.getByName("::ffff:"+localhost.getHostAddress());

      env.setProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
      env.setProperty("java.naming.provider.url", localhostIPv6.getHostAddress()+"@"+serverPort);
      env.setProperty("java.naming.factory.url", "org.jboss.naming:org.jnp.interfaces");
      Name baseName = null;
      Naming server = null;
      NamingContext nc = new NamingContext(env, baseName, server);
      nc.list("");
   }
}
