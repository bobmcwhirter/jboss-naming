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

import java.security.AccessControlException;

import javax.naming.InitialContext;

import org.jboss.test.AbstractTestCase;

import junit.framework.AssertionFailedError;

/**
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class SecurityUtil
{
   /**
    * Naming ops that should succeed.
    * @throws Exception
    */
   static void doOps(InitialContext ic)
      throws Exception
   {
      ic.createSubcontext("path1");
      ic.lookup("path1");
      ic.list("path1");
      ic.listBindings("path1");
      ic.bind("path1/x", "x.bind");
      ic.rebind("path1/x", "x.rebind");
      ic.unbind("path1/x");
      ic.unbind("path1");
   }
   /**
    * Naming ops that should fail.
    * @throws Exception
    */
   static void doBadOps(InitialContext ic, boolean expectFailure)
      throws Exception
   {
      try
      {
         ic.createSubcontext("path2");
         if(expectFailure)
            fail("Was able to create path2 subcontext");
      }
      catch(Exception e)
      {
         AbstractTestCase.checkThrowable(AccessControlException.class, e);
      }
      ic.createSubcontext("path1x");
      try
      {
         if(expectFailure)
         {
            ic.rebind("path1x", "path1x.rebind");
            fail("Was able to rebind path1x subcontext");
         }
      }
      catch(Exception e)
      {
         AbstractTestCase.checkThrowable(AccessControlException.class, e);
      }
      
      try
      {
         ic.lookup("path1x");
         if(expectFailure)
            fail("Was able to lookup path1x subcontext");
      }
      catch(Exception e)
      {
         AbstractTestCase.checkThrowable(AccessControlException.class, e);
      }

      try
      {
         ic.list("path1x");
         if(expectFailure)
            fail("Was able to list path1x subcontext");
      }
      catch(Exception e)
      {
         AbstractTestCase.checkThrowable(AccessControlException.class, e);
      }

      try
      {
         ic.listBindings("path1x");
         if(expectFailure)
            fail("Was able to listBindings path1x subcontext");
      }
      catch(Exception e)
      {
         AbstractTestCase.checkThrowable(AccessControlException.class, e);
      }

      try
      {
         ic.bind("path1x/x", "x.bind");
         if(expectFailure)
            fail("Was able to bind path1x/x");
      }
      catch(Exception e)
      {
         AbstractTestCase.checkThrowable(AccessControlException.class, e);
      }

      try
      {
         ic.rebind("path1x/x", "x.rebind");
         if(expectFailure)
            fail("Was able to rebind path1x/x");
      }
      catch(Exception e)
      {
         AbstractTestCase.checkThrowable(AccessControlException.class, e);
      }

      ic.unbind("path1x");
   }

   static void fail(String message)
   {
      throw new AssertionFailedError(message);
   }
}
