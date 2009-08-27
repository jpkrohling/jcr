/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.core.nodetype;

import java.util.Arrays;

import org.exoplatform.services.jcr.datamodel.InternalQName;

/**
 * Created by The eXo Platform SAS. Define base abstraction for NodeType data
 * used in core. <br/>Date: 25.11.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: NodeTypeData.java 34801 2009-07-31 15:44:50Z dkatayev $
 */

public class NodeTypeData
{
   protected InternalQName name;

   protected InternalQName primaryItemName;

   protected InternalQName[] declaredSupertypeNames;

   protected PropertyDefinitionData[] declaredPropertyDefinitions;

   protected NodeDefinitionData[] declaredChildNodeDefinitions;

   protected boolean hasOrderableChildNodes;

   protected boolean mixin;

   public NodeTypeData(InternalQName name, InternalQName primaryItemName, boolean mixin,
            boolean hasOrderableChildNodes, InternalQName[] declaredSupertypeNames,
            PropertyDefinitionData[] declaredPropertyDefinitions, NodeDefinitionData[] declaredChildNodeDefinitions)
   {

      this.name = name;
      this.primaryItemName = primaryItemName;
      this.mixin = mixin;
      this.hasOrderableChildNodes = hasOrderableChildNodes;
      this.declaredSupertypeNames = declaredSupertypeNames;
      this.declaredPropertyDefinitions = declaredPropertyDefinitions;
      this.declaredChildNodeDefinitions = declaredChildNodeDefinitions;
   }

   public NodeDefinitionData[] getDeclaredChildNodeDefinitions()
   {
      return declaredChildNodeDefinitions;
   }

   public PropertyDefinitionData[] getDeclaredPropertyDefinitions()
   {
      return declaredPropertyDefinitions;
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(declaredChildNodeDefinitions);
      result = prime * result + Arrays.hashCode(declaredPropertyDefinitions);
      result = prime * result + Arrays.hashCode(declaredSupertypeNames);
      result = prime * result + (hasOrderableChildNodes ? 1231 : 1237);
      result = prime * result + (mixin ? 1231 : 1237);
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((primaryItemName == null) ? 0 : primaryItemName.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (obj == null)
      {
         return false;
      }
      if (getClass() != obj.getClass())
      {
         return false;
      }
      NodeTypeData other = (NodeTypeData) obj;
      if (!Arrays.equals(declaredChildNodeDefinitions, other.declaredChildNodeDefinitions))
      {
         return false;
      }
      if (!Arrays.equals(declaredPropertyDefinitions, other.declaredPropertyDefinitions))
      {
         return false;
      }
      if (!Arrays.equals(declaredSupertypeNames, other.declaredSupertypeNames))
      {
         return false;
      }
      if (hasOrderableChildNodes != other.hasOrderableChildNodes)
      {
         return false;
      }
      if (mixin != other.mixin)
      {
         return false;
      }
      if (name == null)
      {
         if (other.name != null)
         {
            return false;
         }
      }
      else if (!name.equals(other.name))
      {
         return false;
      }
      if (primaryItemName == null)
      {
         if (other.primaryItemName != null)
         {
            return false;
         }
      }
      else if (!primaryItemName.equals(other.primaryItemName))
      {
         return false;
      }
      return true;
   }

   public InternalQName[] getDeclaredSupertypeNames()
   {
      return declaredSupertypeNames;
   }

   public InternalQName getPrimaryItemName()
   {
      return primaryItemName;
   }

   public InternalQName getName()
   {
      return name;
   }

   public boolean hasOrderableChildNodes()
   {
      return hasOrderableChildNodes;
   }

   public boolean isMixin()
   {
      return mixin;
   }

}
