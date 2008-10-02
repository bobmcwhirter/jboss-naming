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
package org.jnp.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Binding;
import javax.naming.CannotProceedException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.Reference;
import javax.naming.event.EventContext;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingListener;
import javax.naming.spi.ResolveResult;

import org.jboss.logging.Logger;
import org.jboss.naming.JndiPermission;
import org.jnp.interfaces.Naming;
import org.jnp.interfaces.NamingContext;
import org.jnp.interfaces.NamingEvents;
import org.jnp.interfaces.NamingParser;

/**
 * The in memory JNDI naming server implementation class.
 * 
 * @author Rickard Oberg
 * @author patriot1burke
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 */
public class NamingServer
   implements Naming, NamingEvents, java.io.Serializable
{
   private static Logger log = Logger.getLogger(NamingServer.class);

   /** @since 1.12 at least */
   private static final long serialVersionUID = 4183855539507934373L;
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------
   /** */
   protected Map<String, Binding> table = createTable();
   protected Name prefix;
   protected NamingParser parser = new NamingParser();
   protected NamingServer parent;
   /** The NamingListeners registered with this context */
   private transient EventListeners listeners;
   /** The manager for EventContext listeners */
   private transient EventMgr eventMgr;
   private transient boolean trace;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   public NamingServer()
      throws NamingException
   {
      this(null, null);
   }
   
   public NamingServer(Name prefix, NamingServer parent)
      throws NamingException
   {
      this(null, null, null);   
   }
   public NamingServer(Name prefix, NamingServer parent, EventMgr eventMgr)
      throws NamingException
   {
      if (prefix == null)
         prefix = parser.parse("");
      this.prefix = prefix;      
      this.parent = parent;
      this.eventMgr = eventMgr;
      this.trace = log.isTraceEnabled();
   }


   // Public --------------------------------------------------------

   // NamingListener registration
   public synchronized void addNamingListener(EventContext context, Name target, int scope, NamingListener l) 
      throws NamingException
   {
      if(listeners == null)
         listeners = new EventListeners(context);
      if(trace)
         log.trace("addNamingListener, target: "+target+", scope: "+scope);
      listeners.addNamingListener(context, target, scope, l);
   }

   public void removeNamingListener(NamingListener l) throws NamingException
   {
      if(listeners != null)
      {
         listeners.removeNamingListener(l);
      }
   }

    /**
     * We don't need targets to exist?
     * @return false
     * @throws NamingException
     */
    public boolean targetMustExist() throws NamingException
    {
       return false;
    }

   // Naming implementation -----------------------------------------
   public synchronized void bind(Name name, Object obj, String className)
      throws NamingException
   {
      if (name.isEmpty())
      {
         // Empty names are not allowed
         throw new InvalidNameException("An empty name cannot be passed to bind");
      }
      else if (name.size() > 1) 
      {
         // Recurse to find correct context
//         System.out.println("bind#"+name+"#");
         
         Object ctx = getObject(name);
         if (ctx != null)
         {
            if (ctx instanceof NamingServer)
            {
               NamingServer ns = (NamingServer) ctx;
               ns.bind(name.getSuffix(1),obj, className);
            }
            else if (ctx instanceof Reference)
            {
               // Federation
               if (((Reference)ctx).get("nns") != null)
               {
                  CannotProceedException cpe = new CannotProceedException();
                  cpe.setResolvedObj(ctx);
                  cpe.setRemainingName(name.getSuffix(1));
                  throw cpe;
               }
               else
               {
                  throw new NotContextException();
               }
            }
            else
            {
               throw new NotContextException();
            }
         }
         else
         {
            throw new NameNotFoundException(name.toString()+" in: "+prefix);
         }
      }
      else
      {
         // Bind object
         if (name.get(0).equals(""))
         {
            throw new InvalidNameException("An empty name cannot be passed to bind");
         }
         else
         {
            if(trace)
               log.trace("bind "+name+"="+obj+", "+className);
            try
            {
               getBinding(name);
               // Already bound
               throw new NameAlreadyBoundException(name.toString());
            }
            catch (NameNotFoundException e)
            {
               Name fullName = (Name) prefix.clone();
               fullName.addAll(name);
               SecurityManager sm = System.getSecurityManager();
               if(sm != null)
               {
                  JndiPermission perm = new JndiPermission(fullName, JndiPermission.BIND);
                  sm.checkPermission(perm);
               }

               Binding newb = setBinding(name,obj,className);
               // Notify event listeners
               Binding oldb = null;
               this.fireEvent(fullName, oldb, newb, NamingEvent.OBJECT_ADDED, "bind");
            }
         }
      }
   }

   public synchronized void rebind(Name name, Object obj, String className)
      throws NamingException
   {
      if (name.isEmpty())
      {
         // Empty names are not allowed
         throw new InvalidNameException("An empty name cannot be passed to rebind");
      }
      else if (name.size() > 1) 
      {
         // Recurse to find correct context
//         System.out.println("rebind#"+name+"#");
         
         Object ctx = getObject(name);
         if (ctx instanceof NamingServer)
         {
            ((NamingServer)ctx).rebind(name.getSuffix(1),obj, className);
         }
         else if (ctx instanceof Reference)
         {
            // Federation
            if (((Reference)ctx).get("nns") != null)
            {
               CannotProceedException cpe = new CannotProceedException();
               cpe.setResolvedObj(ctx);
               cpe.setRemainingName(name.getSuffix(1));
               throw cpe;
            }
            else
            {
               throw new NotContextException();
            }
         }
         else
         {
            throw new NotContextException();
         }
      }
      else
      {
         // Bind object
         if (name.get(0).equals(""))
         {
            throw new InvalidNameException("An empty name cannot be passed to rebind");
         }
         else
         {
            SecurityManager sm = System.getSecurityManager();
            Name fullName = (Name) prefix.clone();
            String comp = name.get(0);
            fullName.add(comp);
            if(sm != null)
            {
               JndiPermission perm = new JndiPermission(fullName, JndiPermission.REBIND);
               sm.checkPermission(perm);
            }
            
            Binding oldb = table.get(comp);
            Binding newb = setBinding(name,obj,className);
            // Notify event listeners
            if(listeners != null)
            {
               int type = NamingEvent.OBJECT_CHANGED;
               if(oldb == null)
                  type = NamingEvent.OBJECT_ADDED;
               this.fireEvent(fullName, oldb, newb, type, "rebind");
            }
         }
      }
   }
   
   public synchronized void unbind(Name name)
      throws NamingException
   {
      if (name.isEmpty())
      {
         // Empty names are not allowed
         throw new InvalidNameException();
      }
      else if (name.size() > 1) 
      {
         // Recurse to find correct context
//         System.out.println("unbind#"+name+"#");
         
         Object ctx = getObject(name);
         if (ctx instanceof NamingServer)
         {
            ((NamingServer)ctx).unbind(name.getSuffix(1));
         }
         else if (ctx instanceof Reference)
         {
            // Federation
            if (((Reference)ctx).get("nns") != null)
            {
               CannotProceedException cpe = new CannotProceedException();
               cpe.setResolvedObj(ctx);
               cpe.setRemainingName(name.getSuffix(1));
               throw cpe;
            }
            else
            {
               throw new NotContextException();
            }
         }
         else
         {
            throw new NotContextException();
         }
      } else
      {
         // Unbind object
         if (name.get(0).equals(""))
         {
            throw new InvalidNameException();
         }
         else
         {
//            System.out.println("unbind "+name+"="+getBinding(name));
            if (getBinding(name) != null)
            {
               SecurityManager sm = System.getSecurityManager();
               Name fullName = (Name) prefix.clone();
               fullName.addAll(name);
               if(sm != null)
               {
                  JndiPermission perm = new JndiPermission(fullName, JndiPermission.UNBIND);
                  sm.checkPermission(perm);
               }
               
               Binding newb = null;
               Binding oldb = removeBinding(name);
               // Notify event listeners
               int type = NamingEvent.OBJECT_REMOVED;
               this.fireEvent(fullName, oldb, newb, type, "unbind");
            }
            else
            {
               throw new NameNotFoundException();
            }
         }
      }
   }

//   public synchronized Object lookup(Name name)
   public Object lookup(Name name)
      throws NamingException
   {
		Object result;
      if (name.isEmpty())
      {
         SecurityManager sm = System.getSecurityManager();
         if(sm != null)
         {
            JndiPermission perm = new JndiPermission(prefix, JndiPermission.LOOKUP);
            sm.checkPermission(perm);
         }
         
         // Return this
         result = new NamingContext(null, (Name)(prefix.clone()), getRoot());
      }
      else if (name.size() > 1)
      {
         // Recurse to find correct context
//         System.out.println("lookup#"+name+"#");
         
         Object ctx = getObject(name);
         if (ctx instanceof NamingServer)
         {
            result = ((NamingServer)ctx).lookup(name.getSuffix(1));
         }
         else if (ctx instanceof Reference)
         {
            // Federation
            if (((Reference)ctx).get("nns") != null)
            {
               CannotProceedException cpe = new CannotProceedException();
               cpe.setResolvedObj(ctx);
               cpe.setRemainingName(name.getSuffix(1));
               throw cpe;
            }
            
            result = new ResolveResult(ctx, name.getSuffix(1));
         } else
         {
            throw new NotContextException();
         }
      }
      else
      {
         // Get object to return
         if (name.get(0).equals(""))
         {
            SecurityManager sm = System.getSecurityManager();
            if(sm != null)
            {
               JndiPermission perm = new JndiPermission(prefix, JndiPermission.LOOKUP);
               sm.checkPermission(perm);
            }
            result = new NamingContext(null, (Name)(prefix.clone()), getRoot());
         }
         else
         {
//            System.out.println("lookup "+name);
            SecurityManager sm = System.getSecurityManager();
            Name fullName = (Name)(prefix.clone());
            fullName.addAll(name);
            if(sm != null)
            {
               JndiPermission perm = new JndiPermission(fullName, JndiPermission.LOOKUP);
               sm.checkPermission(perm);
            }
            
            Object res = getObject(name);
            
            if (res instanceof NamingServer)
            {
               result = new NamingContext(null, fullName, getRoot());
            }
            else
               result = res;
         }
      }
		
		return result;
   }
   
   public Collection<NameClassPair> list(Name name)
      throws NamingException
   {
      if (name.isEmpty())
      {
         SecurityManager sm = System.getSecurityManager();
         if(sm != null)
         {
            JndiPermission perm = new JndiPermission(prefix, JndiPermission.LIST);
            sm.checkPermission(perm);
         }

         ArrayList<NameClassPair> list = new ArrayList<NameClassPair>();
         for(Binding b : table.values())
         {
            NameClassPair ncp = new NameClassPair(b.getName(),b.getClassName(), true);
            list.add(ncp);
         }
         return list;
      }
      else
      {  
         Object ctx = getObject(name);
         if (ctx instanceof NamingServer)
         {
            return ((NamingServer)ctx).list(name.getSuffix(1));
         }
         else if (ctx instanceof Reference)
         {
            // Federation
            if (((Reference)ctx).get("nns") != null)
            {
               CannotProceedException cpe = new CannotProceedException();
               cpe.setResolvedObj(ctx);
               cpe.setRemainingName(name.getSuffix(1));
               throw cpe;
            }
            else
            {
               throw new NotContextException();
            }
         }
         else
         {
            throw new NotContextException();
         }
      } 
   }
    
   public Collection<Binding> listBindings(Name name)
      throws NamingException
   {
      if (name.isEmpty())
      {
         SecurityManager sm = System.getSecurityManager();
         if(sm != null)
         {
            JndiPermission perm = new JndiPermission(prefix, JndiPermission.LIST_BINDINGS);
            sm.checkPermission(perm);
         }

         Collection<Binding> bindings = table.values();
         Collection<Binding> newBindings = new ArrayList<Binding>(bindings.size());
         for(Binding b : bindings)
         {
            if (b.getObject() instanceof NamingServer)
            {
               Name n = (Name)prefix.clone();
               n.add(b.getName());
               newBindings.add(new Binding(b.getName(), 
                                           b.getClassName(),
                                           new NamingContext(null, n, getRoot())));
            }
            else
            {
               newBindings.add(b);
            }
         }
         
         return newBindings;
      } else
      {
         Object ctx = getObject(name);
         if (ctx instanceof NamingServer)
         {
            return ((NamingServer)ctx).listBindings(name.getSuffix(1));
         } else if (ctx instanceof Reference)
         {
            // Federation
            if (((Reference)ctx).get("nns") != null)
            {
               CannotProceedException cpe = new CannotProceedException();
               cpe.setResolvedObj(ctx);
               cpe.setRemainingName(name.getSuffix(1));
               throw cpe;
            } else
            {
               throw new NotContextException();
            }
         } else
         {
            throw new NotContextException();
         }
      } 
   }
   
   public Context createSubcontext(Name name)
      throws NamingException
   {
       if( name.size() == 0 )
          throw new InvalidNameException("Cannot pass an empty name to createSubcontext");

      NamingException ex = null;
      Context subCtx = null;
      if (name.size() > 1)
      {         
         Object ctx = getObject(name);
         if (ctx != null)
         {
            Name subCtxName = name.getSuffix(1);
            if (ctx instanceof NamingServer)
            {
               subCtx = ((NamingServer)ctx).createSubcontext(subCtxName);
            }
            else if (ctx instanceof Reference)
            {
               // Federation
               if (((Reference)ctx).get("nns") != null)
               {
                  CannotProceedException cpe = new CannotProceedException();
                  cpe.setResolvedObj(ctx);
                  cpe.setRemainingName(subCtxName);
                  throw cpe;
               }
               else
               {
                  ex = new NotContextException();
                  ex.setResolvedName(name.getPrefix(0));
                  ex.setRemainingName(subCtxName);
                  throw ex;
               }
            }
            else
            {
               ex = new NotContextException();
               ex.setResolvedName(name.getPrefix(0));
               ex.setRemainingName(subCtxName);
               throw ex;
            }
         }
         else
         {
            ex = new NameNotFoundException();
            ex.setRemainingName(name);
            throw ex;
         }
      }
      else
      {
         Object binding = table.get(name.get(0));
         if( binding != null )
         {
            ex = new NameAlreadyBoundException();
            ex.setResolvedName(prefix);
            ex.setRemainingName(name);
            throw ex;
         }
         else
         {
            Name fullName = (Name) prefix.clone();
            fullName.addAll(name);
            SecurityManager sm = System.getSecurityManager();
            if(sm != null)
            {
               JndiPermission perm = new JndiPermission(fullName, JndiPermission.CREATE_SUBCONTEXT);
               sm.checkPermission(perm);
            }
            NamingServer subContext = createNamingServer(fullName, this);
            subCtx = new NamingContext(null, fullName, getRoot());
            setBinding(name, subContext, NamingContext.class.getName());
            // Return the NamingContext as the binding value
            Binding newb = new Binding(name.toString(), NamingContext.class.getName(), subCtx, true);
            // Notify event listeners
            if(listeners != null)
            {
               Binding oldb = null;
               this.fireEvent(fullName, oldb, newb, NamingEvent.OBJECT_ADDED, "createSubcontext");
            }
         }
      }
      return subCtx;
   }

   public Naming getRoot()
   {
      if (parent == null)
         return this;
      else
         return parent.getRoot();
   }

   // Y overrides ---------------------------------------------------

   // Package protected ---------------------------------------------
    
   // Protected -----------------------------------------------------

   protected Map<String, Binding> createTable()
   {
      return new ConcurrentHashMap<String, Binding>();  
   }

   /**
    * Create sub naming.
    *
    * @param prefix the prefix
    * @param parent the parent naming server
    * @return new sub instance
    * @throws NamingException for any error
    */
   protected NamingServer createNamingServer(Name prefix, NamingServer parent)
      throws NamingException
   {
      return new NamingServer(prefix, parent, eventMgr);
   }

   protected void fireEvent(Name fullName, Binding oldb, Binding newb, int type,
         String changeInfo)
      throws NamingException
   {
      if(eventMgr == null)
      {
         if(trace)
            log.trace("Skipping event dispatch because there is no EventMgr");
         return;
      }

      if(listeners != null)
      {
         if(trace)
            log.trace("fireEvent, type: "+type+", fullName: "+fullName);
         HashSet<Integer> scopes = new HashSet<Integer>();
         scopes.add(EventContext.OBJECT_SCOPE);
         scopes.add(EventContext.ONELEVEL_SCOPE);
         scopes.add(EventContext.SUBTREE_SCOPE);
         eventMgr.fireEvent(fullName, oldb, newb, type, changeInfo, listeners, scopes);
      }
      else if(trace)
      {
         log.trace("fireEvent, type: "+type+", fullName: "+fullName);
      }
      // Traverse to parent for SUBTREE_SCOPE
      HashSet<Integer> scopes = new HashSet<Integer>();
      scopes.add(EventContext.SUBTREE_SCOPE);
      NamingServer nsparent = parent;
      while(nsparent != null)
      {
         if(nsparent.listeners != null)
         {
            eventMgr.fireEvent(fullName, oldb, newb, type, changeInfo, nsparent.listeners, scopes);
         }
         nsparent = nsparent.parent;
      }
   }

   // Private -------------------------------------------------------

   private Binding setBinding(Name name, Object obj, String className)
   {
      String n = name.toString();
      Binding b = new Binding(n, className, obj, true);
      table.put(n, b);
      if( trace )
      {
         StringBuffer tmp = new StringBuffer(super.toString());
         tmp.append(", setBinding: name=");
         tmp.append(name);
         tmp.append(", obj=");
         tmp.append(obj);
         tmp.append(", className=");
         tmp.append(className);
         log.trace(tmp.toString());
      }
      return b;
   }

   private Binding getBinding(String key)
      throws NameNotFoundException
   {
      Binding b = table.get(key);
      if (b == null)
      {
         if( log.isTraceEnabled() )
         {
            StringBuffer tmp = new StringBuffer(super.toString());
            tmp.append(", No binding for: ");
            tmp.append(key);
            tmp.append(" in context ");
            tmp.append(this.prefix);
            tmp.append(", bindings:\n");
            Iterator<Binding> bindings = table.values().iterator();
            while( bindings.hasNext() )
            {
               Binding value = bindings.next();
               tmp.append(value.getName());
               tmp.append('=');
               if( value.getObject() != null )
                  tmp.append(value.getObject().toString());
               else
                  tmp.append("null");
               tmp.append('\n');
            }
            log.trace(tmp.toString());
         }
         throw new NameNotFoundException(key + " not bound");
      }
      return b;
   }

   private Binding getBinding(Name key)
      throws NameNotFoundException
   {
      return getBinding(key.get(0));
   }
   
   private Object getObject(Name key)
      throws NameNotFoundException
   {
      return getBinding(key).getObject();
   }

   private Binding removeBinding(Name name)
   {
      return table.remove(name.get(0));
   }
   
}
