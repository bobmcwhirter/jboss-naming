/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.naming.microcontainer;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import org.jboss.test.kernel.junit.MicrocontainerTest;
import org.jboss.util.naming.Util;

/**
 * BootstrapNamingTest.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public abstract class BootstrapNamingTest extends MicrocontainerTest
{
   InitialContext ic;
   
   public static BootstrapNamingTestDelegate getDelegate(Class<?> clazz) throws Exception
   {
      return new BootstrapNamingTestDelegate(clazz);
   }
   
   public BootstrapNamingTest(String name)
   {
      super(name);
   }

   protected BootstrapNamingTestDelegate getDelegate()
   {
      return (BootstrapNamingTestDelegate) super.getDelegate();
   }
   
   protected InitialContext getInitialContext() throws Exception
   {
      if (ic == null)
         ic = new InitialContext();
      return ic;
   }
   
   protected void bind(String name, Object binding) throws Exception
   {
      Util.bind(getInitialContext(), name, binding);
   }
   
   protected void unbind(String name) throws Exception
   {
      Util.unbind(getInitialContext(), name);
   }
   
   protected Object assertBinding(String name) throws Exception
   {
      return getInitialContext().lookup(name);
   }
   
   protected <T> T assertBinding(String name, Class<T> expectedType) throws Exception
   {
      Object result = assertBinding(name);
      return assertInstanceOf(result, expectedType);
   }
   
   protected void assertBinding(String name, Object expected) throws Exception
   {
      Object actual = assertBinding(name);
      assertEquals(expected, actual);
   }
   
   protected void assertNoBinding(String name) throws Exception
   {
      Context ctx = getInitialContext();
      try
      {
         Object result = ctx.lookup(name);
         fail("Did not expect a binding for " + name + " : " + result);
      }
      catch (Throwable t)
      {
         checkThrowable(NameNotFoundException.class, t);
      }
   }
}
