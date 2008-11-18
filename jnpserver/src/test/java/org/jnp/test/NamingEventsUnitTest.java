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

import java.io.IOException;
import java.util.Properties;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.event.EventContext;
import javax.naming.event.NamingEvent;

import junit.framework.Test;

import org.jboss.test.BaseTestCase;
import org.jnp.interfaces.MarshalledValuePair;
import org.jnp.server.ExecutorEventMgr;
import org.jnp.server.NamingBeanImpl;
import org.jnp.test.support.QueueEventListener;

/**
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class NamingEventsUnitTest extends BaseTestCase
{
   /** The actual namingMain service impl bean */
   private static NamingBeanImpl namingBean;
   private QueueEventListener listener = new QueueEventListener();
   
   public static Test suite()
   {
      return suite(NamingEventsUnitTest.class);
   }
   
   public NamingEventsUnitTest(String name)
   {
      super(name);
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      namingBean = new NamingBeanImpl();
      namingBean.setInstallGlobalService(true);
      namingBean.setEventMgr(new ExecutorEventMgr());
      namingBean.start();
   }
   
   @Override
   protected void tearDown() throws Exception
   {
      namingBean.stop();
      super.tearDown();
   }

   public void testAddRemoveOneLevel() throws Exception
   {
      Properties env = new Properties();
      env.setProperty("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
      env.setProperty("java.naming.factory.url.pkgs", "org.jnp.interfaces");
      InitialContext ic = new InitialContext(env);
      System.out.println("Created InitialContext");
      Context ctx = (Context) ic.lookup("");
      assertTrue("Context is an EventContext", ctx instanceof EventContext);
      EventContext ectx = (EventContext) ctx;
      ectx.addNamingListener("", EventContext.ONELEVEL_SCOPE, listener);
      System.out.println("Added NamingListener");
      ctx.bind("testAddObject", "testAddObject.bind");
      assertTrue("Saw bind event", listener.waitOnEvent());
      NamingEvent event = listener.getEvent(0);
      assertEquals("OBJECT_ADDED", NamingEvent.OBJECT_ADDED, event.getType());
      assertNull("getOldBinding", event.getOldBinding());      
      assertEquals("testAddObject.bind", getValue(event.getNewBinding()));

      ctx.rebind("testAddObject", "testAddObject.rebind");
      assertTrue("Saw rebind event", listener.waitOnEvent());
      event = listener.getEvent(1);
      assertEquals("OBJECT_CHANGED", NamingEvent.OBJECT_CHANGED, event.getType());
      assertEquals("testAddObject.bind", getValue(event.getOldBinding()));
      assertEquals("testAddObject.rebind", getValue(event.getNewBinding()));

      ctx.unbind("testAddObject");
      assertTrue("Saw unbind event", listener.waitOnEvent());
      event = listener.getEvent(2);
      assertEquals("OBJECT_REMOVED", NamingEvent.OBJECT_REMOVED, event.getType());
      assertEquals("testAddObject.rebind", getValue(event.getOldBinding()));
      assertNull("getNewBinding", event.getNewBinding());

      // Create a subcontext
      Context subctx = ctx.createSubcontext("subctx");
      listener.waitOnEvent();
      assertEquals("Should be 4 events", 4, listener.getEventCount());
      event = listener.getEvent(3);
      assertEquals("OBJECT_ADDED", NamingEvent.OBJECT_ADDED, event.getType());
      assertNull("getOldBinding", event.getOldBinding());      
      assertEquals("getNewBinding", subctx, getValue(event.getNewBinding()));

      // Creating a binding under subctx should not produce an event
      subctx.bind("subctx.testAddObject", "testAddObject.subctx.bind");
      assertFalse("Wait on subctx bind", listener.waitOnEvent());
      assertEquals("Still should be 4 events", 4, listener.getEventCount());
   }

   public void testAddRemoveSubtree() throws Exception
   {
      Properties env = new Properties();
      env.setProperty("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
      env.setProperty("java.naming.factory.url.pkgs", "org.jnp.interfaces");
      InitialContext ic = new InitialContext(env);
      System.out.println("Created InitialContext");
      Context ctx = (Context) ic.lookup("");
      assertTrue("Context is an EventContext", ctx instanceof EventContext);
      EventContext ectx = (EventContext) ctx;
      ectx.addNamingListener("", EventContext.SUBTREE_SCOPE, listener);
      System.out.println("Added NamingListener");
      ctx.bind("testAddObject", "testAddObject.bind");
      assertTrue("Saw bind event", listener.waitOnEvent());
      NamingEvent event = listener.getEvent(0);
      assertEquals("OBJECT_ADDED", NamingEvent.OBJECT_ADDED, event.getType());
      assertNull("getOldBinding", event.getOldBinding());      
      assertEquals("testAddObject.bind", getValue(event.getNewBinding()));

      ctx.rebind("testAddObject", "testAddObject.rebind");
      assertTrue("Saw rebind event", listener.waitOnEvent());
      event = listener.getEvent(1);
      assertEquals("OBJECT_CHANGED", NamingEvent.OBJECT_CHANGED, event.getType());
      assertEquals("testAddObject.bind", getValue(event.getOldBinding()));
      assertEquals("testAddObject.rebind", getValue(event.getNewBinding()));

      ctx.unbind("testAddObject");
      assertTrue("Saw unbind event", listener.waitOnEvent());
      event = listener.getEvent(2);
      assertEquals("OBJECT_REMOVED", NamingEvent.OBJECT_REMOVED, event.getType());
      assertEquals("testAddObject.rebind", getValue(event.getOldBinding()));
      assertNull("getNewBinding", event.getNewBinding());

      // Create a subcontext
      Context subctx = ctx.createSubcontext("subctx");
      listener.waitOnEvent();
      assertEquals("Should be 4 events", 4, listener.getEventCount());
      event = listener.getEvent(3);
      assertEquals("OBJECT_ADDED", NamingEvent.OBJECT_ADDED, event.getType());
      assertNull("getOldBinding", event.getOldBinding());      
      assertEquals("getNewBinding", subctx, getValue(event.getNewBinding()));

      // Creating a binding under subctx should produce an event
      subctx.bind("subctx.testAddObject", "testAddObject.subctx.bind");
      assertTrue("Wait on subctx bind", listener.waitOnEvent());
      event = listener.getEvent(4);
      assertEquals("OBJECT_ADDED", NamingEvent.OBJECT_ADDED, event.getType());      
   }

   protected Object getValue(Binding binding)
      throws ClassNotFoundException, IOException
   {
      Object obj = binding.getObject();
      if(obj instanceof MarshalledValuePair)
      {
         MarshalledValuePair mvp = (MarshalledValuePair) obj;
         obj = mvp.get();
      }
      return obj;
   }
}
