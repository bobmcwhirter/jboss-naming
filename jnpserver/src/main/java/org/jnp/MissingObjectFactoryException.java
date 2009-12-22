/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jnp;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

/**
 * Indicates that a client has looked up a {@link Reference}
 * pointing to an {@link ObjectFactory} that is not available 
 * on the classpath or cannot otherwise be found.
 * 
 * JBNAME-42
 * 
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class MissingObjectFactoryException extends NamingException
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 1L;

   /*
    * Strings used in generating the error message; hold the static parts here for better performance 
    * and reduced String instance creation
    */
   private static final String ERROR_MESSAGE_PREFIX;

   private static final String ERROR_MESSAGE_SUFFIX;

   static
   {
      ERROR_MESSAGE_PREFIX = "Could not obtain " + ObjectFactory.class.getName()
            + " implementation referenced from JNDI at \"";
      ERROR_MESSAGE_SUFFIX = "\"; perhaps missing from the ClassPath?: ";
   }

   //-------------------------------------------------------------------------------------||
   // Constructor ------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Creates a new instance with the specified explanation;
    * internal only, use {@link MissingObjectFactoryException#create(String, Name)}
    * from the outside
    * @param explanation
    */
   private MissingObjectFactoryException(final String explanation)
   {
      super(explanation);
   }

   //-------------------------------------------------------------------------------------||
   // Public Factory Methods -------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Creates a new instance explaining the error with the
    * specified context
    * 
    * @param missingObjectFactoryClassName The name of the Class that could not be found
    * @param targetName The JNDI target requested
    * @throws IllegalArgumentException If either the missingObj
    */
   public static MissingObjectFactoryException create(final String missingObjectFactoryClassName, final Name targetName)
         throws IllegalArgumentException
   {
      // Precondition checks
      if (missingObjectFactoryClassName == null || missingObjectFactoryClassName.length() == 0)
      {
         throw new IllegalArgumentException("missing object factory class name is required");
      }
      if (targetName == null)
      {
         throw new IllegalArgumentException("target name is required");
      }

      // Construct the message
      final StringBuilder sb = new StringBuilder();
      sb.append(ERROR_MESSAGE_PREFIX);
      sb.append(targetName);
      sb.append(ERROR_MESSAGE_SUFFIX);
      sb.append(missingObjectFactoryClassName);

      // Create and return a new Exception
      return new MissingObjectFactoryException(sb.toString());
   }
}
