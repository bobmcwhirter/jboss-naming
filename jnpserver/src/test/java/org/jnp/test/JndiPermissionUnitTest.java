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

import org.jnp.server.JndiPermission;

import junit.framework.TestCase;

/**
 * Tests of the JndiPermission/JndiPermissionCollection behavior
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 */
public class JndiPermissionUnitTest extends TestCase
{
   public void testBind()
   {
      JndiPermission any = new JndiPermission("<<ALL BINDINGS>>", "*");
      JndiPermission path1 = new JndiPermission("/path1/*", "bind");
      JndiPermission path1Recursive = new JndiPermission("/path1/-", "bind");

      assertTrue("<<ALL BINDINGS>> implies /path1;bind", any.implies(path1));
      assertTrue("<<ALL BINDINGS>> implies /path1;bind", any.implies(path1Recursive));
      JndiPermission p = new JndiPermission("/path1/", "bind");
      assertTrue(path1+" implies /path1/ bind", path1.implies(p));
      assertTrue(path1Recursive+" implies /path1/ bind", path1Recursive.implies(p));
      // A directory permission does not imply access to the unqualified path
      p = new JndiPermission("/path1", "bind");
      assertFalse(path1+" implies /path1;bind", path1.implies(p));
      assertFalse(path1Recursive+" implies /path1;bind", path1Recursive.implies(p));
      
   }
}
