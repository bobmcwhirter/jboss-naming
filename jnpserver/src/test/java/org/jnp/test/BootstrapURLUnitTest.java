/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import junit.framework.Test;

import org.jboss.test.BaseTestCase;
import org.jnp.server.Main;
import org.jnp.server.NamingBeanImpl;

/**
 *
 *
 * @author Brian Stansberry
 * 
 * @version $Revision: $
 */
public class BootstrapURLUnitTest extends BaseTestCase
{
   
   /** The actual namingMain service impl bean */
   private static NamingBeanImpl namingBean;

   private Main namingMain;
   
   /**
    * Create a new BootstrapURLUnitTest.
    * 
    * @param name
    */
   public BootstrapURLUnitTest(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(BootstrapURLUnitTest.class);
   }
   
   
   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      if (namingBean == null)
      {
         namingBean = new NamingBeanImpl();
         namingBean.start();
      }
      namingMain = new Main("org.jnp.server");
      namingMain.setNamingInfo(namingBean);
   }
   
   

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      
      if (namingMain != null)
      {
         namingMain.stop();
      }
   }

   public void testLocalhost() throws Exception
   {
      namingMain.setPort(0);
      namingMain.setBindAddress("127.0.0.1");
      namingMain.start();
      int port = namingMain.getPort();
      assertTrue(port > 0);
      assertEquals("jnp://127.0.0.1:" + port, namingMain.getBootstrapURL());
   }

   public void testIPv6Localhost() throws Exception
   {
      namingMain.setPort(0);
      namingMain.setBindAddress("::1");
      namingMain.start();
      int port = namingMain.getPort();
      assertTrue(port > 0);
      assertEquals("jnp://[0:0:0:0:0:0:0:1]:" + port, namingMain.getBootstrapURL());
   }
   
   public void testAnyAddress() throws Exception
   {
      namingMain.setPort(0);
      namingMain.setBindAddress("0.0.0.0");
      namingMain.start();
      int port = namingMain.getPort();
      assertTrue(port > 0);
      assertEquals("jnp://0.0.0.0:" + port, namingMain.getBootstrapURL());      
   }
   
   public void testNoServerSocket() throws Exception
   {
      namingMain.setPort(-1);
      namingMain.setBindAddress("localhost");
      namingMain.start(); 
      assertNull(namingMain.getBootstrapURL());
   }

}
