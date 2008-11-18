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

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;

import javax.naming.CompositeName;
import javax.naming.Name;

import junit.framework.Test;

import org.jboss.naming.JndiPermission;
import org.jboss.test.BaseTestCase;

/**
 * Tests of the JndiPermission/JndiPermissionCollection behavior
 * 
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision$
 */
public class JndiPermissionUnitTest extends BaseTestCase
{
   private static final String ALL_BINDINGS = "<<ALL BINDINGS>>";
   
   private static final String[] ACTION_ARRAY = new String[] 
   {
      JndiPermission.BIND_ACTION,
      JndiPermission.REBIND_ACTION,
      JndiPermission.UNBIND_ACTION,
      JndiPermission.LOOKUP_ACTION,
      JndiPermission.LIST_ACTION,
      JndiPermission.LIST_BINDINGS_ACTION,
      JndiPermission.CREATE_SUBCONTEXT_ACTION,
   };

   private static final String ALL_ACTIONS;
   
   static
   {
      StringBuilder builder = new StringBuilder();
      boolean comma = false;
      for (String action : ACTION_ARRAY)
      {
         if (comma)
            builder.append(',');
         builder.append(action);
         comma = true;
      }
      ALL_ACTIONS = builder.toString();
   }
   
   public static Test suite()
   {
      return suite(JndiPermissionUnitTest.class);
   }
   
   public JndiPermissionUnitTest(String name)
   {
      super(name);
   }
   
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
   
   public void testConstants()
   {
      assertEquals(0, JndiPermission.NONE);
      assertEquals(1, JndiPermission.BIND);
      assertEquals(2, JndiPermission.REBIND);
      assertEquals(4, JndiPermission.UNBIND);
      assertEquals(8, JndiPermission.LOOKUP);
      assertEquals(16, JndiPermission.LIST);
      assertEquals(32, JndiPermission.LIST_BINDINGS);
      assertEquals(64, JndiPermission.CREATE_SUBCONTEXT);
      assertEquals(127, JndiPermission.ALL);

      assertEquals("bind", JndiPermission.BIND_ACTION);
      assertEquals("rebind", JndiPermission.REBIND_ACTION);
      assertEquals("unbind", JndiPermission.UNBIND_ACTION);
      assertEquals("lookup", JndiPermission.LOOKUP_ACTION);
      assertEquals("list", JndiPermission.LIST_ACTION);
      assertEquals("listBindings", JndiPermission.LIST_BINDINGS_ACTION);
      assertEquals("createSubcontext", JndiPermission.CREATE_SUBCONTEXT_ACTION);
      assertEquals("*", JndiPermission.ALL_ACTION);
   }
   
   public void testBasicConstructorAllBindings() throws Exception
   {
      JndiPermission test = new JndiPermission(ALL_BINDINGS, JndiPermission.BIND_ACTION);
      assertEquals(ALL_BINDINGS, test.getName());
      assertEquals(JndiPermission.BIND_ACTION, test.getActions());
   }
   
   public void testBasicConstructorSimplePathString() throws Exception
   {
      JndiPermission test = new JndiPermission("simple", JndiPermission.BIND_ACTION);
      assertEquals("simple", test.getName());
      assertEquals(JndiPermission.BIND_ACTION, test.getActions());
   }
   
   /*
   public void testBasicConstructorSimplePathString2() throws Exception
   {
      JndiPermission test = new JndiPermission("/simple", JndiPermission.BIND_ACTION);
      assertEquals("simple", test.getName());
      assertEquals(JndiPermission.BIND_ACTION, test.getActions());
   }
   */
   
   public void testBasicConstructorHierarchcicalPathString() throws Exception
   {
      JndiPermission test = new JndiPermission("1/2/3", JndiPermission.BIND_ACTION);
      assertEquals("1/2/3", test.getName());
      assertEquals(JndiPermission.BIND_ACTION, test.getActions());
   }
   
   public void testBasicConstructorWildcardPathString() throws Exception
   {
      JndiPermission test = new JndiPermission("1/2/3/*", JndiPermission.BIND_ACTION);
      assertEquals("1/2/3/*", test.getName());
      assertEquals(JndiPermission.BIND_ACTION, test.getActions());
   }
   
   public void testBasicConstructorRecursivePathString() throws Exception
   {
      JndiPermission test = new JndiPermission("1/2/3/-", JndiPermission.BIND_ACTION);
      assertEquals("1/2/3/-", test.getName());
      assertEquals(JndiPermission.BIND_ACTION, test.getActions());
   }
   
   public void testBasicConstructorSimplePathName() throws Exception
   {
      JndiPermission test = new JndiPermission(new CompositeName("simple"), JndiPermission.BIND_ACTION);
      assertEquals("simple", test.getName());
      assertEquals(JndiPermission.BIND_ACTION, test.getActions());
   }
   
   public void testBasicConstructorHierarchcicalPathName() throws Exception
   {
      JndiPermission test = new JndiPermission(new CompositeName("1/2/3"), JndiPermission.BIND_ACTION);
      assertEquals("1/2/3", test.getName());
      assertEquals(JndiPermission.BIND_ACTION, test.getActions());
   }
   
   public void testBasicConstructorWildcardPathName() throws Exception
   {
      JndiPermission test = new JndiPermission(new CompositeName("1/2/3/*"), JndiPermission.BIND_ACTION);
      assertEquals("1/2/3/*", test.getName());
      assertEquals(JndiPermission.BIND_ACTION, test.getActions());
   }
   
   public void testBasicConstructorRecursivePathName() throws Exception
   {
      JndiPermission test = new JndiPermission(new CompositeName("1/2/3/-"), JndiPermission.BIND_ACTION);
      assertEquals("1/2/3/-", test.getName());
      assertEquals(JndiPermission.BIND_ACTION, test.getActions());
   }
   
   public void testBasicConstructorBadString() throws Exception
   {
      try
      {
         new JndiPermission((String) null, JndiPermission.ALL_ACTION);
         fail("Should not be here!");
      }
      catch (Exception expected)
      {
         checkThrowable(NullPointerException.class, expected);
      }
   }
   
   public void testBasicConstructorBadName() throws Exception
   {
      try
      {
         new JndiPermission((Name) null, JndiPermission.ALL_ACTION);
         fail("Should not be here!");
      }
      catch (Exception expected)
      {
         checkThrowable(NullPointerException.class, expected);
      }
   }
   
   public void testBasicConstructorActions() throws Exception
   {
      HashSet<String> previous = new HashSet<String>();
      testBasicConstructorActions(previous);
   }
   
   public void testBasicConstructorActions(HashSet<String> previous) throws Exception
   {
      if (previous.size() == ACTION_ARRAY.length)
         return;
      
      for (String current : ACTION_ARRAY)
      {
         if (previous.contains(current) == false)
         {
            HashSet<String> newTest = new HashSet<String>(previous);
            newTest.add(current);

            StringBuilder builder = new StringBuilder();
            boolean comma = false;
            for (String action : newTest)
            {
               if (comma)
                  builder.append(',');
               builder.append(action);
               comma = true;
            }
            
            JndiPermission test = new JndiPermission("simple", builder.toString());
            assertEquals("simple", test.getName());

            String foundActions = test.getActions();
            assertNotNull(foundActions);
            HashSet<String> actual = new HashSet<String>(Arrays.asList(foundActions.split(",")));
            assertEquals(newTest, actual);

            // Recurse
            testBasicConstructorActions(newTest);
         }
      }
   }
   
   /* public void testBasicConstructorActionsSpaces() throws Exception
   {
      for (String current : ACTION_ARRAY)
      {
         JndiPermission test = new JndiPermission("simple", " " + current);
         assertEquals("simple", test.getName());
         assertEquals(current, test.getActions());
         test = new JndiPermission("simple", current + " ");
         assertEquals("simple", test.getName());
         assertEquals(current, test.getActions());
         test = new JndiPermission("simple", " " + current + " ");
         assertEquals("simple", test.getName());
         assertEquals(current, test.getActions());
      }
   } */
   
   public void testBasicConstructorAllAction() throws Exception
   {
      JndiPermission all = new JndiPermission("simple", JndiPermission.ALL_ACTION);
      JndiPermission test = new JndiPermission("simple", ALL_ACTIONS);
      assertEquals(test, all);
   }

   public void testBasicConstructorBadActions() throws Exception
   {
      try
      {
         new JndiPermission("simple", "rubbish");
         fail("Should not be here!");
      }
      catch (Exception expected)
      {
         checkThrowable(IllegalArgumentException.class, expected);
      }

      try
      {
         new JndiPermission("simple", "bind,rubbish");
         fail("Should not be here!");
      }
      catch (Exception expected)
      {
         checkThrowable(IllegalArgumentException.class, expected);
      }
   }
   
   public void testImpliesSimplePath() throws Exception
   {
      JndiPermission test1 = new JndiPermission("simple", JndiPermission.ALL_ACTION);
      JndiPermission test2 = new JndiPermission("simple", JndiPermission.ALL_ACTION);
      assertTrue(test2.implies(test1));

      test2 = new JndiPermission("notsimple", JndiPermission.ALL_ACTION);
      assertFalse(test2.implies(test1));

      test2 = new JndiPermission(ALL_BINDINGS, JndiPermission.ALL_ACTION);
      assertTrue(test2.implies(test1));
   }
   
   public void testImpliesHierarchy() throws Exception
   {
      JndiPermission test1 = new JndiPermission("1/2/3", JndiPermission.ALL_ACTION);
      JndiPermission test2 = new JndiPermission("1/2/3", JndiPermission.ALL_ACTION);
      assertTrue(test2.implies(test1));

      test2 = new JndiPermission("1", JndiPermission.ALL_ACTION);
      assertFalse(test2.implies(test1));

      test2 = new JndiPermission("1/2", JndiPermission.ALL_ACTION);
      assertFalse(test2.implies(test1));

      test2 = new JndiPermission("1/2/4", JndiPermission.ALL_ACTION);
      assertFalse(test2.implies(test1));

      test2 = new JndiPermission("1/2/3/4", JndiPermission.ALL_ACTION);
      assertFalse(test2.implies(test1));

      test2 = new JndiPermission(ALL_BINDINGS, JndiPermission.ALL_ACTION);
      assertTrue(test2.implies(test1));
   }
   
   public void testImpliesWildcard() throws Exception
   {
      JndiPermission test1 = new JndiPermission("1/2/3", JndiPermission.ALL_ACTION);
      JndiPermission test2 = new JndiPermission("1/2/*", JndiPermission.ALL_ACTION);
      assertTrue(test2.implies(test1));

      test2 = new JndiPermission("1/2/3/*", JndiPermission.ALL_ACTION);
      assertFalse(test2.implies(test1));

      test2 = new JndiPermission("1/4/*", JndiPermission.ALL_ACTION);
      assertFalse(test2.implies(test1));

      test2 = new JndiPermission("1/*", JndiPermission.ALL_ACTION);
      assertFalse(test2.implies(test1));

      /*
      test2 = new JndiPermission("*", JndiPermission.ALL_ACTION);
      assertFalse(test2.implies(test1));

      test1 = new JndiPermission("1", JndiPermission.ALL_ACTION);
      assertTrue(test2.implies(test1));
      */

      test1 = new JndiPermission("1/2/34", JndiPermission.ALL_ACTION);
      test2 = new JndiPermission("1/2/3*", JndiPermission.ALL_ACTION);
      assertFalse(test2.implies(test1));
   }
   
   public void testImpliesRecursive() throws Exception
   {
      JndiPermission test1 = new JndiPermission("1/2/3", JndiPermission.ALL_ACTION);
      JndiPermission test2 = new JndiPermission("1/2/-", JndiPermission.ALL_ACTION);
      assertTrue(test2.implies(test1));

      test2 = new JndiPermission("1/-", JndiPermission.ALL_ACTION);
      assertTrue(test2.implies(test1));

      /*
      test2 = new JndiPermission("-", JndiPermission.ALL_ACTION);
      assertTrue(test2.implies(test1));
      */

      test2 = new JndiPermission("1/2/3/-", JndiPermission.ALL_ACTION);
      assertFalse(test2.implies(test1));

      test2 = new JndiPermission("1/4/-", JndiPermission.ALL_ACTION);
      assertFalse(test2.implies(test1));

      test1 = new JndiPermission("1/2/34", JndiPermission.ALL_ACTION);
      test2 = new JndiPermission("1/2/3-", JndiPermission.ALL_ACTION);
      assertFalse(test2.implies(test1));
   }
   
   public void testImpliesActions() throws Exception
   {
      JndiPermission all = new JndiPermission("simple", JndiPermission.ALL_ACTION);
      
      for (String current : ACTION_ARRAY)
      {
         HashSet<String> actions = new HashSet<String>();
         actions.add(current);
         
         JndiPermission test = new JndiPermission("simple", current);
         assertTrue("All implies " + current, all.implies(test));
         testImpliesActions(actions, test);
      }
   }
   
   public void testImpliesActions(HashSet<String> previous, JndiPermission current) throws Exception
   {
      if (previous.size() == ACTION_ARRAY.length)
         return;
      
      for (String action : ACTION_ARRAY)
      {
         if (previous.contains(action) == false)
         {
            HashSet<String> newTest = new HashSet<String>(previous);
            newTest.add(action);

            StringBuilder builder = new StringBuilder();
            boolean comma = false;
            for (String element : newTest)
            {
               if (comma)
                  builder.append(',');
               builder.append(element);
               comma = true;
            }
            String actions = builder.toString();
            
            JndiPermission test = new JndiPermission("simple", actions);
            assertTrue(actions + " implies " + current.getActions(), test.implies(current));

            // Recurse
            testImpliesActions(newTest, current);
         }
      }
   }
   
   public void testEqualsPath() throws Exception
   {
      JndiPermission one = new JndiPermission("1/2/3", JndiPermission.ALL_ACTION);
      JndiPermission two = new JndiPermission("1/2/3", JndiPermission.ALL_ACTION);
      assertEquals(one, two);
      assertEquals(two, one);
      
      two = new JndiPermission(new CompositeName("1/2/3"), JndiPermission.ALL_ACTION);
      assertEquals(one, two);
      assertEquals(two, one);
      
      two = new JndiPermission("1/2/4", JndiPermission.ALL_ACTION);
      assertNotSame(one, two);
      assertNotSame(two, one);
      
      two = new JndiPermission("1/2/*", JndiPermission.ALL_ACTION);
      assertNotSame(one, two);
      assertNotSame(two, one);
      
      two = new JndiPermission("1/2/-", JndiPermission.ALL_ACTION);
      assertNotSame(one, two);
      assertNotSame(two, one);
      
      two = new JndiPermission(ALL_BINDINGS, JndiPermission.ALL_ACTION);
      assertNotSame(one, two);
      assertNotSame(two, one);
      
      one = new JndiPermission("1/2/*", JndiPermission.ALL_ACTION);
      two = new JndiPermission("1/2/*", JndiPermission.ALL_ACTION);
      assertEquals(one, two);
      assertEquals(two, one);
      
      one = new JndiPermission("1/2/-", JndiPermission.ALL_ACTION);
      two = new JndiPermission("1/2/-", JndiPermission.ALL_ACTION);
      assertEquals(one, two);
      assertEquals(two, one);
      
      one = new JndiPermission(ALL_BINDINGS, JndiPermission.ALL_ACTION);
      two = new JndiPermission(ALL_BINDINGS, JndiPermission.ALL_ACTION);
      assertEquals(one, two);
      assertEquals(two, one);
   }
   
   public void testEqualsActions() throws Exception
   {
      for (String action1 : ACTION_ARRAY)
      {
         for (String action2 : ACTION_ARRAY)
         {
            JndiPermission one = new JndiPermission("1/2/3", action1);
            JndiPermission two = new JndiPermission("1/2/3", action1);
            if (action1.equals(action2))
            {
               assertEquals(one, two);
               assertEquals(two, one);
            }
            else
            {
               assertNotSame(one, two);
               assertNotSame(two, one);
            }
         }
      }
      
      JndiPermission one = new JndiPermission("1/2/3", ALL_ACTIONS);
      JndiPermission two = new JndiPermission("1/2/3", JndiPermission.ALL_ACTION);
      assertEquals(one, two);
      assertEquals(two, one);

      one = new JndiPermission("1/2/3", "bind,unbind");
      two = new JndiPermission("1/2/3", "unbind,bind");
      assertEquals(one, two);
      assertEquals(two, one);

      one = new JndiPermission("1/2/3", "bind,unbind");
      two = new JndiPermission("1/2/3", "unbind");
      assertNotSame(one, two);
      assertNotSame(two, one);
   }
   
   public void testSerialization() throws Exception
   {
      testSerialization(new JndiPermission("simple", JndiPermission.ALL_ACTION));
      testSerialization(new JndiPermission(new CompositeName("simple"), JndiPermission.ALL_ACTION));
      testSerialization(new JndiPermission("1/2/3", JndiPermission.ALL_ACTION));
      testSerialization(new JndiPermission("1/2/*", JndiPermission.ALL_ACTION));
      testSerialization(new JndiPermission("1/2/-", JndiPermission.ALL_ACTION));
      testSerialization(new JndiPermission(ALL_BINDINGS, JndiPermission.ALL_ACTION));
      for (String action : ACTION_ARRAY)
         testSerialization(new JndiPermission("simple", action));
   }
   
   public void testSerialization(JndiPermission expected) throws Exception
   {
      JndiPermission actual = serializeDeserialize(expected, JndiPermission.class);
      assertEquals(expected, actual);
   }
   
   public void testPermissionsCollection() throws Exception
   {
      JndiPermission one = new JndiPermission("1/2/3/*", "bind");
      JndiPermission two = new JndiPermission("1/2/3/4", "unbind");
      
      PermissionCollection permissions = one.newPermissionCollection();
      assertFalse(permissions.implies(new JndiPermission("1/2/3/4", "bind")));
      assertFalse(permissions.implies(new JndiPermission("1/2/3/4", "unbind")));
      assertFalse(permissions.elements().hasMoreElements());
      
      permissions.add(one);
      permissions.add(two);
      
      JndiPermission test = new JndiPermission("1/2/3/4", "bind");
      assertTrue(permissions.implies(test));

      test = new JndiPermission("1/2/3/5", "bind");
      assertTrue(permissions.implies(test));

      test = new JndiPermission("1/2/3/4", "unbind");
      assertTrue(permissions.implies(test));

      test = new JndiPermission("1/2/3/5", "unbind");
      assertFalse(permissions.implies(test));

      test = new JndiPermission("1/2/3", "bind");
      assertFalse(permissions.implies(test));
      
      try
      {
         permissions.add(null);
         fail("Should not be here");
      }
      catch (Exception exception)
      {
         checkThrowable(IllegalArgumentException.class, exception);
      }
      
      try
      {
         permissions.add(new RuntimePermission("createClassLoader"));
         fail("Should not be here");
      }
      catch (Exception exception)
      {
         checkThrowable(IllegalArgumentException.class, exception);
      }
      
      HashSet<Permission> expected = new HashSet<Permission>();
      expected.add(one);
      expected.add(two);
      
      HashSet<Permission> actual = new HashSet<Permission>();
      for (Enumeration<Permission> enumeration = permissions.elements(); enumeration.hasMoreElements();)
         actual.add(enumeration.nextElement());
      assertEquals(expected, actual);
   }
}
