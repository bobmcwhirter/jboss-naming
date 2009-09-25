/*
* JBoss, Home of Professional Open Source
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
package org.jboss.naming.microcontainer;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.naming.InitialContext;

import org.jboss.annotations.spi.naming.JNDI;
import org.jboss.beans.info.spi.BeanInfo;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.metadata.spi.MetaData;
import org.jboss.util.naming.NonSerializableFactory;
import org.jboss.util.naming.Util;

/**
 * AbstractJNDILifecycleCallback.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractJNDILifecycleCallback
{
   /** Keep track of non serializable entries */
   private Set<String> nonSerializableEntries = new CopyOnWriteArraySet<String>();

   /**
    * At stop clear non serializable entries.
    */
   public void stop()
   {
      nonSerializableEntries.clear();
   }

   /**
    * Installation of the context
    * 
    * @param context the context
    * @throws Exception for any error
    */
   public abstract void install(ControllerContext context) throws Exception;

   /**
    * Uninstall of the context
    * 
    * @param context the context
    * @throws Exception for any error
    */
   public abstract void uninstall(ControllerContext context) throws Exception;
   
   /**
    * Get the JNDI annotation
    * 
    * @param context the context
    * @return the jndi annotation or null if not found
    * @throws Exception for any error
    */
   protected JNDI getJNDIAnnotation(ControllerContext context) throws Exception
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");

      MetaData metaData = context.getScopeInfo().getMetaData();
      if (metaData == null)
         return null;
      return metaData.getAnnotation(JNDI.class);
   }
   
   /**
    * Bind into jndi
    * 
    * @param context the context
    * @param jndi the jndi annotation
    * @throws Exception for any error
    */
   protected void bind(ControllerContext context, JNDI jndi) throws Exception
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      if (jndi == null)
         jndi = getJNDIAnnotation(context);

      if (jndi.bindDirect())
      {
         Object object = context.getTarget();
         if (object == null)
            throw new IllegalStateException("No object associated with context: " + context.getName());

         if (jndi.serializable() == false || object instanceof Serializable == false)
         {
            String binding = jndi.binding();
            NonSerializableFactory.rebind(new InitialContext(), binding, object);
            nonSerializableEntries.add(binding);
         }
         else
         {
            Util.bind(new InitialContext(), jndi.binding(), object);
         }
      }
      else
      {
         String className = determineClassName(context, jndi);
         MicrocontainerObjectFactory.bind(jndi.binding(), className, null, context);
      }
   }
   
   /**
    * Unbind from jndi
    * 
    * @param context the context
    * @param jndi the jndi annotation
    * @throws Exception for any error
    */
   protected void unbind(ControllerContext context, JNDI jndi) throws Exception
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      if (jndi == null)
         jndi = getJNDIAnnotation(context);

      if (jndi.bindDirect())
      {
         String binding = jndi.binding();
         if (nonSerializableEntries.remove(binding))
         {
            NonSerializableFactory.unbind(binding);
            new InitialContext().unbind(binding);
         }
         else
         {
            Util.unbind(new InitialContext(), binding);
         }
      }
      else
      {
         MicrocontainerObjectFactory.unbind(jndi.binding(), null);
      }
   }

   /**
    * Determine the class name
    * 
    * @param context the context
    * @param jndi the jndi binding
    * @return the class name
    */
   protected String determineClassName(ControllerContext context, JNDI jndi)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");

      String className = jndi.className();
      if (className.length() == 0)
      {
         Object object = context.getTarget();
         if (object != null)
         {
            className = object.getClass().getName();
         }
         // This is a bit hacky
         else if (context instanceof KernelControllerContext)
         {
            KernelControllerContext kcc = (KernelControllerContext) context;
            BeanInfo beanInfo = kcc.getBeanInfo();
            if (beanInfo != null)
            {
               className = beanInfo.getName();
            }
            else
            {
               BeanMetaData bmd = kcc.getBeanMetaData();
               if (bmd != null)
                  className = bmd.getBean();
            }
         }
      }
      return className;
   }
}
