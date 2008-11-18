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

import java.math.BigInteger;
import java.net.InetAddress;
import java.security.Permission;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.spi.InitialContextFactory;

import junit.framework.Test;

import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.beans.metadata.api.model.InjectOption;
import org.jboss.naming.JndiPermission;
import org.jboss.test.kernel.junit.MicrocontainerTest;
import org.jnp.interfaces.NamingContext;
import org.jnp.interfaces.TimedSocketFactory;
import org.jnp.test.support.QueueSecurityManager;

/**
 * Test bootstraping the naming service using the mc
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class NamingMCUnitTest extends MicrocontainerTest
{
   public static Test suite()
   {
      return suite(NamingMCUnitTest.class);
   }
   /** */
   private InitialContext ctx;
   private QueueSecurityManager qsm;
   private InitialContextFactory ctxFactory;

   /**
    * 
    * @param name
    */
   public NamingMCUnitTest(String name)
   {
      super(name, true);
   }

   /**
    * Obtain the InitialContext from the InitialContextFactory bean ctx property.
    * Each test expects an InitialContextFactory bean
    * @see org.jboss.naming.NamingContextFactory
    * 
    * @param ctx
    */
   @Inject(bean="InitialContextFactory", property="ctx")
   public void setInitialContext(InitialContext ctx)
   {
      this.ctx = ctx;
   }
   @Inject(bean="QueueSecurityManager", option=InjectOption.OPTIONAL)
   public void setQueueSecurityManager(QueueSecurityManager qsm)
   {
      this.qsm = qsm;
   }
   @Inject(bean="InitialContextFactory#3", option=InjectOption.OPTIONAL)
   public void setCtxFactory(InitialContextFactory ctxFactory)
   {
      this.ctxFactory = ctxFactory;
   }

   /**
    * Validate that a NamingBeanImpl mc bean is accessible via the
    * LocalOnlyContextFactory
    * 
    * @throws Exception
    */
   public void testLocaNamingBeanImpl()
      throws Exception
   {
      assertNotNull(ctx);
      validateCtx(ctx);
   }
   /**
    * Validate that the NamingBeanImpl mc bean is accessible via the
    * InitialContext(env) using the LocalOnlyContextFactory
    * @throws Exception
    */
   public void testLocaNamingBeanImplViaInitialContextFactory()
      throws Exception
   {
      Properties env = new Properties();
      env.setProperty("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
      env.setProperty("java.naming.factory.url", "org.jboss.naming:org.jnp.interfaces");
      InitialContext ic = new InitialContext(env);
      validateCtx(ic);
   }

   /**
    * Validate that a SingletonNamingServer mc bean is accessible via the
    * LocalOnlyContextFactory
    * 
    * @throws Exception
    */
   public void testSingletonNamingServer()
      throws Exception
   {
      assertNotNull(ctx);
      validateCtx(ctx);
   }

   /**
    * Test the org.jnp.server.Main bean that wraps a NamingBean with remote
    * access via an rmi proxy
    * 
    * @throws Exception
    */
   public void testMainBean()
      throws Exception
   {
      Properties env = new Properties();
      env.setProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
      env.setProperty("java.naming.provider.url", "localhost:1099");
      env.setProperty("java.naming.factory.url", "org.jboss.naming:org.jnp.interfaces");
      env.setProperty(TimedSocketFactory.JNP_TIMEOUT, "10000");
      env.setProperty(TimedSocketFactory.JNP_SO_TIMEOUT, "10000");
      InitialContext ic = new InitialContext(env);
      validateCtx(ic);
   }
   /**
    * Test the org.jnp.server.Main bean that wraps a NamingBean with remote
    * access via an rmi proxy using custom socket factories
    * 
    * @throws Exception
    */
   public void testMainBeanSFs()
      throws Exception
   {
      InetAddress localAddr = InetAddress.getLocalHost();
      getLog().debug("InetAddress.getLocalHost(): "+localAddr);
      Properties env = new Properties();
      env.setProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
      env.setProperty("java.naming.provider.url", "localhost:2099");
      env.setProperty("java.naming.factory.url", "org.jboss.naming:org.jnp.interfaces");
      env.setProperty(TimedSocketFactory.JNP_TIMEOUT, "1000");
      env.setProperty(TimedSocketFactory.JNP_SO_TIMEOUT, "1000");
      InitialContext ic = new InitialContext(env);
      validateCtx(ic);
   }

   /**
    * Test two Naming instances with one LocalOnlyContextFactory using
    * the NamingContext.local instance, and the other using the non-global
    * Naming instance that was injected.
    * @throws Exception
    */
   public void testMultipleLocalOnlyContextFactory()
      throws Exception
   {
      // The InitialContextFactory
      assertNotNull(ctx);
      validateCtx(ctx);

      // The InitialContextFactory#2
      Properties env = new Properties();
      env.setProperty("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
      env.setProperty("java.naming.factory.url", "org.jboss.naming:org.jnp.interfaces");
      env.setProperty(NamingContext.JNP_NAMING_INSTANCE_NAME, "testLocaNamingBeanImpl#2");
      InitialContext ic = new InitialContext(env);

      // Validate the second naming context bindings created by JndiBindings#2
      Integer i2 = (Integer) ic.lookup("ints/2");
      assertEquals("ints/1", new Integer(2), i2);
      String s2 = (String) ic.lookup("strings/2");
      assertEquals("strings/2", "String2", s2);
      BigInteger bi2 = (BigInteger) ic.lookup("bigint/2");
      assertEquals("bigint/2", new BigInteger("987654321"), bi2);
      Properties envp = (Properties) ic.lookup("env-props");
      Properties expected = new Properties();
      expected.setProperty("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory#2");
      expected.setProperty("java.naming.factory.url", "factory#2");
      assertEquals("env-props", expected, envp);

      // The InitialContextFactory#3
      assertNotNull(ctxFactory);
      // Validate the third naming context bindings created by JndiBindings#3
      Context ctx3 = ctxFactory.getInitialContext(null);
      Integer i3 = (Integer) ctx3.lookup("ints/3");
      assertEquals("ints/1", new Integer(3), i3);
      String s3 = (String) ctx3.lookup("strings/3");
      assertEquals("strings/3", "String3", s3);
      BigInteger bi3 = (BigInteger) ctx3.lookup("bigint/3");
      assertEquals("bigint/2", new BigInteger("333333333"), bi3);
      Properties envp3 = (Properties) ctx3.lookup("env-props");
      Properties expected3 = new Properties();
      expected3.setProperty("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory#3");
      expected3.setProperty("java.naming.factory.url", "factory#3");
      assertEquals("env-props", expected3, envp3);
   }

   public void testInjectedSecurityManager()
      throws Exception
   {
      qsm.clearPerms();

      HashSet<JndiPermission> expectedPerms = new HashSet<JndiPermission>();
      // expected doOps() permissions
      expectedPerms.add(new JndiPermission("path1", "createSubcontext"));
      expectedPerms.add(new JndiPermission("path1", "lookup"));
      expectedPerms.add(new JndiPermission("path1", "list"));
      expectedPerms.add(new JndiPermission("path1", "listBindings"));
      expectedPerms.add(new JndiPermission("path1/x", "bind"));
      expectedPerms.add(new JndiPermission("path1/x", "rebind"));
      expectedPerms.add(new JndiPermission("path1/x", "unbind"));
      expectedPerms.add(new JndiPermission("path1", "unbind"));
      SecurityUtil.doOps(ctx);
      // expected doBadOps() permissions
      expectedPerms.add(new JndiPermission("path2", "createSubcontext"));
      expectedPerms.add(new JndiPermission("path1x", "createSubcontext"));
      expectedPerms.add(new JndiPermission("path1x", "rebind"));
      expectedPerms.add(new JndiPermission("path1x", "lookup"));
      expectedPerms.add(new JndiPermission("path1x", "list"));
      expectedPerms.add(new JndiPermission("path1x", "listBindings"));
      expectedPerms.add(new JndiPermission("path1x/x", "bind"));
      expectedPerms.add(new JndiPermission("path1x/x", "rebind"));
      expectedPerms.add(new JndiPermission("path1x", "unbind"));
      SecurityUtil.doBadOps(ctx, false);

      List<Permission> perms = qsm.getPerms();
      for(Permission p : perms)
      {
         if(p instanceof JndiPermission)
         {
            getLog().info(p);
            assertTrue(p+" is in expectedPerms", expectedPerms.contains(p));
         }
      }
   }

   /**
    * 
    * @param ic
    * @throws Exception
    */
   protected void validateCtx(InitialContext ic)
      throws Exception
   {
      Integer i1 = (Integer) ic.lookup("ints/1");
      assertEquals("ints/1", new Integer(1), i1);
      String s1 = (String) ic.lookup("strings/1");
      assertEquals("strings/1", "String1", s1);
      BigInteger bi1 = (BigInteger) ic.lookup("bigint/1");
      assertEquals("bigint/1", new BigInteger("123456789"), bi1);
      Properties env = (Properties) ic.lookup("env-props");
      Properties expected = new Properties();
      expected.setProperty("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
      expected.setProperty("java.naming.factory.url", "org.jboss.naming:org.jnp.interfaces");
      assertEquals("env-props", expected, env);
   }
}
