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

import java.io.FilePermission;
import java.io.SerializablePermission;
import java.lang.reflect.ReflectPermission;
import java.security.Permission;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;

import junit.framework.Test;

import org.jboss.naming.JndiPermission;
import org.jboss.test.BaseTestCase;
import org.jnp.server.ExecutorEventMgr;
import org.jnp.server.NamingBeanImpl;
import org.jnp.test.support.QueueSecurityManager;
import org.jnp.test.support.TestSecurityManager;

/**
 * Test using the NamingServer with a SecurityManager
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 */
public class NamingServerSecurityManagerUnitTest extends BaseTestCase
{
   /** The actual namingMain service impl bean */
   private NamingBeanImpl namingBean;
   private InitialContext ic;
   
   public static Test suite()
   {
      return suite(NamingServerSecurityManagerUnitTest.class);
   }
   
   public NamingServerSecurityManagerUnitTest(String name)
   {
      super(name);
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      getLog().info("+++ setUp, creating NamingBean");
      namingBean = new NamingBeanImpl();
      namingBean.setInstallGlobalService(true);
      namingBean.setUseGlobalService(false);
      namingBean.setEventMgr(new ExecutorEventMgr());
      namingBean.start();


      Properties env = new Properties();
      env.setProperty("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
      env.setProperty("java.naming.factory.url.pkgs", "org.jnp.interfaces");
      ic = new InitialContext(env);
   }
   protected void tearDown() throws Exception
   {
      System.setSecurityManager(null);
      super.tearDown();
   }

   /**
    * Test that the expected JndiPermission are passed to the SecurityManager
    * @throws Exception
    */
   public void testOpPermissions()
      throws Exception
   {
      QueueSecurityManager qsm = new QueueSecurityManager();
      qsm.clearPerms();
      System.setSecurityManager(qsm);

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
      doOps();
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
      doBadOps(false);

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
    * Run with a TestSecurityManager that has a fixed set of allowed permissions
    * @throws Exception
    */
   public void testSecurityManager()
      throws Exception
   {
      TestSecurityManager tsm = new TestSecurityManager();
      /*
       The permissions that should be needed 
       */
      tsm.addPermission(new JndiPermission("path1", "createSubcontext,lookup,list,listBindings,unbind"));
      tsm.addPermission(new JndiPermission("path1x", "createSubcontext,unbind"));
      tsm.addPermission(new JndiPermission("path1/*", "list,listBindings,lookup"));
      tsm.addPermission(new JndiPermission("path1/x", "bind,rebind,unbind"));
      tsm.addPermission(new RuntimePermission("setSecurityManager"));
      tsm.addPermission(new FilePermission("<<ALL FILES>>", "read"));
      tsm.addPermission(new RuntimePermission("createClassLoader"));
      tsm.addPermission(new ReflectPermission("suppressAccessChecks"));
      tsm.addPermission(new SerializablePermission("enableSubstitution"));
      System.setSecurityManager(tsm);

      doOps();
      doBadOps(true);
   }

   /**
    * Validate that the JndiPermission("<<ALL BINDINGS>>", "*") allows all
    * naming operations.
    * 
    * @throws Exception
    */
   public void testAllPermission()
      throws Exception
   {
      TestSecurityManager tsm = new TestSecurityManager();
      tsm.addPermission(new JndiPermission("<<ALL BINDINGS>>", "*"));
      tsm.addPermission(new RuntimePermission("setSecurityManager"));
      tsm.addPermission(new FilePermission("<<ALL FILES>>", "read"));
      tsm.addPermission(new RuntimePermission("createClassLoader"));
      tsm.addPermission(new ReflectPermission("suppressAccessChecks"));
      tsm.addPermission(new SerializablePermission("enableSubstitution"));
      System.setSecurityManager(tsm);

      doOps();
      doBadOps(false);  
   }

   protected void doOps()
      throws Exception
   {
      SecurityUtil.doOps(ic);
   }
   protected void doBadOps(boolean expectFailure)
      throws Exception
   {
      SecurityUtil.doBadOps(ic, expectFailure);
   }
}
