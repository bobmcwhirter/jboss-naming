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
import java.util.Properties;

import javax.naming.InitialContext;

import junit.framework.Test;

import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.test.kernel.junit.MicrocontainerTest;

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
    * @see org.jboss.naming.InitialContextFactory
    * 
    * @param ctx
    */
   @Inject(bean="InitialContextFactory", property="ctx")
   public void setInitialContext(InitialContext ctx)
   {
      this.ctx = ctx;
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
    * 
    * @param ic
    * @throws Exception
    */
   protected void validateCtx(InitialContext ic)
      throws Exception
   {
      Integer i1 = (Integer) ctx.lookup("ints/1");
      assertEquals("ints/1", new Integer(1), i1);
      String s1 = (String) ctx.lookup("strings/1");
      assertEquals("strings/1", "String1", s1);
      BigInteger bi1 = (BigInteger) ctx.lookup("bigint/1");
      assertEquals("bigint/1", new BigInteger("123456789"), bi1);
      Properties env = (Properties) ctx.lookup("env-props");
      Properties expected = new Properties();
      expected.setProperty("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
      expected.setProperty("java.naming.factory.url", "org.jboss.naming:org.jnp.interfaces");
      assertEquals("env-props", expected, env);
   }
}
