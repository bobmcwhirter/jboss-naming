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

import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * This class exists because the JNDI API set wisely uses java.util.Properties
 * which extends Hashtable, a threadsafe class. The NamingParser uses a static
 * instance, making it a global source of contention. This results in
 * a huge scalability problem, which can be seen in ECPerf, as sometimes half
 * of the worker threads are stuck waiting for this stupid lock, sometimes
 * themselves holdings global locks, e.g. to the AbstractInstanceCache.
 *
 * @version <tt>$Revision$</tt>
 * @author <a href="mailto:sreich@apple.com">Stefan Reich</a>
 */
class FastNamingProperties extends Properties
{
   /** serialVersionUID */
   private static final long serialVersionUID = 190486940953472275L;

   FastNamingProperties()
   {
   }

   public Object setProperty(String s1, String s2)
   {
      throw new UnsupportedOperationException();
   }

   public void load(InputStream is) throws java.io.IOException
   {
      throw new UnsupportedOperationException();
   }

   public String getProperty(String s)
   {
      if (s.equals("jndi.syntax.direction"))
      {
         return "left_to_right";
      }
      else if (s.equals("jndi.syntax.ignorecase"))
      {
         return "false";
      }
      else if (s.equals("jndi.syntax.separator"))
      {
         return "/";
      }
      else
      {
         return null;
      }
   }

   public String getProperty(String name, String defaultValue)
   {
      String ret = getProperty(name);
      if (ret == null)
      {
         ret = defaultValue;
      }
      return ret;
   }

   public Enumeration propertyNames()
   {
      throw new UnsupportedOperationException();
   }

   public void list(PrintStream ps)
   {
      throw new UnsupportedOperationException();
   }

   public void list(PrintWriter ps)
   {
      throw new UnsupportedOperationException();
   }

   // methods from Hashtable

   public int size()
   {
      throw new UnsupportedOperationException();
   }

   public boolean isEmpty()
   {
      throw new UnsupportedOperationException();
   }

   public Enumeration keys()
   {
      throw new UnsupportedOperationException();
   }

   public Enumeration elements()
   {
      throw new UnsupportedOperationException();
   }

   public boolean contains(Object o)
   {
      throw new UnsupportedOperationException();
   }

   public boolean containsValue(Object o)
   {
      throw new UnsupportedOperationException();
   }

   public boolean containsKey(Object o)
   {
      throw new UnsupportedOperationException();
   }

   public Object get(Object o)
   {
      throw new UnsupportedOperationException();
   }

   public Object put(Object o1, Object o2)
   {
      throw new UnsupportedOperationException();
   }

   public Object remove(Object o)
   {
      throw new UnsupportedOperationException();
   }

   public void putAll(Map m)
   {
      throw new UnsupportedOperationException();
   }

   public void clear()
   {
      throw new UnsupportedOperationException();
   }

   public Object clone()
   {
      throw new UnsupportedOperationException();
   }

   public String toString()
   {
      throw new UnsupportedOperationException();
   }

   public java.util.Set keySet()
   {
      throw new UnsupportedOperationException();
   }

   public java.util.Set entrySet()
   {
      throw new UnsupportedOperationException();
   }

   public java.util.Collection values()
   {
      throw new UnsupportedOperationException();
   }

   public boolean equals(Object o)
   {
      throw new UnsupportedOperationException();
   }

   public int hashCode()
   {
      throw new UnsupportedOperationException();
   }
}
