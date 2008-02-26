/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jnp.interfaces;

import java.util.Collection;
import java.util.Iterator;

import javax.naming.NamingEnumeration;

/**
 *  A NamingEnumeration for the Context list/listBindings methods.
 * 
 * @author oberg
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 */
public class NamingEnumerationImpl
   implements NamingEnumeration
{
   // Constants -----------------------------------------------------
 
   // Attributes ----------------------------------------------------
   Iterator iter;
    
   // Static --------------------------------------------------------
   
   // Constructors --------------------------------------------------
   NamingEnumerationImpl(Collection list)
   {
      iter = list.iterator();
   }
   
   // Public --------------------------------------------------------

   // Enumeration implementation ------------------------------------
   public boolean hasMoreElements()
   {
      return iter.hasNext();
   }
   
   public Object nextElement()
   {
      return iter.next();
   }

   // NamingEnumeration implementation ------------------------------
   public boolean hasMore()
   {
      return iter.hasNext();
   }
   
   public Object next()
   {
      return iter.next();
   }
   
   public void close()
   {
      iter = null;
   }

   // Y overrides ---------------------------------------------------

   // Package protected ---------------------------------------------
    
   // Protected -----------------------------------------------------
    
   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}