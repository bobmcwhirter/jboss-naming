/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
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
package org.jnp.server.test.jbname49;

import org.jboss.naming.Util;
import org.jnp.interfaces.LocalOnlyContextFactory;
import org.jnp.server.NamingBeanImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MoreInterestingNameNotFoundExceptionTestCase
{
   private static NamingBeanImpl naming;
   private static InitialContext ctx;

   @AfterClass
   public static void afterClass() throws NamingException
   {
      if(ctx != null)
         ctx.close();
      ctx = null;
      if(naming != null)
         naming.stop();
      naming = null;
   }

   @BeforeClass
   public static void beforeClass() throws Exception
   {
      naming = new NamingBeanImpl();
      naming.start();

      Hashtable<String, String> env = new Hashtable<String, String>();
      env.put(InitialContext.INITIAL_CONTEXT_FACTORY, LocalOnlyContextFactory.class.getName());
      ctx = new InitialContext(env);

      Util.bind(ctx, "a/b/c", "Hello world");
      Util.createLinkRef(ctx, "good", "a/b/c");
      Util.createLinkRef(ctx, "bad", "a/b/d");
   }

   @Test
   public void testBad() throws NamingException
   {
      try
      {
         ctx.lookup("bad");
         fail("Looking up bad must result in a NamingException, because a/b/d does not exist");
      }
      catch(NamingException e)
      {
         while(e.getCause() != null)
            e = (NamingException) e.getCause();
         String msg = e.getMessage();
         // I'll leave out of scope whether 'a/b/d not bound' is better than 'd not bound in a/b'
         assertTrue("Message '" + msg + "' did not contain 'a/b'", msg.contains("a/b"));
      }
   }

   @Test
   public void testGood() throws NamingException
   {
      String result = (String) ctx.lookup("good");
      assertEquals("Hello world", result);
   }
}
