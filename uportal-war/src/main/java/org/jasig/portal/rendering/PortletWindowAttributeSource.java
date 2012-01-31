/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.portal.rendering;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.portlet.PortletMode;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import org.apache.pluto.container.om.portlet.PortletDefinition;
import org.apache.pluto.container.om.portlet.Supports;
import org.jasig.portal.layout.IUserLayoutManager;
import org.jasig.portal.portlet.PortletUtils;
import org.jasig.portal.portlet.om.IPortletDefinition;
import org.jasig.portal.portlet.om.IPortletDefinitionId;
import org.jasig.portal.portlet.om.IPortletEntity;
import org.jasig.portal.portlet.om.IPortletWindow;
import org.jasig.portal.portlet.om.IPortletWindowId;
import org.jasig.portal.portlet.registry.IPortletDefinitionRegistry;
import org.jasig.portal.portlet.registry.IPortletWindowRegistry;
import org.jasig.portal.portlet.rendering.IPortletRenderer;
import org.jasig.portal.utils.cache.CacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Adds information about the portlet window to the layout xml data
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class PortletWindowAttributeSource implements AttributeSource, BeanNameAware {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final XMLEventFactory xmlEventFactory = XMLEventFactory.newFactory();
    private String name;
    private IPortletWindowRegistry portletWindowRegistry;
    private IPortletDefinitionRegistry portletDefinitionRegistry;

    @Autowired
    public void setPortletWindowRegistry(IPortletWindowRegistry portletWindowRegistry) {
        this.portletWindowRegistry = portletWindowRegistry;
    }

    @Override
    public void setBeanName(String name) {
        this.name = name;
    }
    

    @Override
    public final Iterator<Attribute> getAdditionalAttributes(HttpServletRequest request, HttpServletResponse response, StartElement event) {
        final QName eventName = event.getName();
        final String localEventName = eventName.getLocalPart();
        
        //Only pay attention to channel events
        if (!IUserLayoutManager.CHANNEL.equals(localEventName)) {
            return null;
        }
        
        //Grab the element's ID
        final Attribute idAttribute = event.getAttributeByName(IUserLayoutManager.ID_ATTR_NAME);
        if (idAttribute == null) {
            this.logger.warn("StartElement " + IUserLayoutManager.CHANNEL + " does not have an " + IUserLayoutManager.ID_ATTR_NAME + " attribute, it will be ignored. From event: " + event);
            return null;
        }
        
        //Lookup the portlet window for the layout node
        final String layoutNodeId = idAttribute.getValue();
        final IPortletWindow portletWindow = this.portletWindowRegistry.getOrCreateDefaultPortletWindowByLayoutNodeId(request, layoutNodeId);
        if (portletWindow == null) {
            this.logger.warn("No IPortletWindow could be found or created for layout node id " + layoutNodeId + ". From element: " + event);
            return null;
        }
        
        //Create the attributes
        final Collection<Attribute> attributes = new LinkedList<Attribute>();
        
        //Add window state data
        final WindowState windowState = portletWindow.getWindowState();
        final Attribute windowStateAttribute = xmlEventFactory.createAttribute("windowState", windowState.toString());
        attributes.add(windowStateAttribute);
        
        //Add portlet mode data
        final PortletMode portletMode = portletWindow.getPortletMode();
        final Attribute portletModeAttribute = xmlEventFactory.createAttribute("portletMode", portletMode.toString());
        attributes.add(portletModeAttribute);
        
        if (supportsDashboard(portletWindow)) {
            final Attribute dashboardAttribute = xmlEventFactory.createAttribute("supportsDashboard", Boolean.TRUE.toString());
            attributes.add(dashboardAttribute);
        }
        
        return attributes.iterator();
    }

    protected boolean supportsDashboard(final IPortletWindow portletWindow) {
        final IPortletEntity portletEntity = portletWindow.getPortletEntity();
        final IPortletDefinition portletDefinition = portletEntity.getPortletDefinition();
        final IPortletDefinitionId portletDefinitionId = portletDefinition.getPortletDefinitionId();
        final PortletDefinition portletDescriptor = this.portletDefinitionRegistry.getParentPortletDescriptor(portletDefinitionId);
        
        for (final Supports supports : portletDescriptor.getSupports()) {
            for (String state : supports.getWindowStates()) {
                if (PortletUtils.getWindowState(state).equals(IPortletRenderer.DETACHED)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public final CacheKey getCacheKey(HttpServletRequest request, HttpServletResponse response) {
        final Set<IPortletWindow> portletWindows = this.portletWindowRegistry.getAllLayoutPortletWindows(request);

        final LinkedHashSet<List<Serializable>> cacheKey = new LinkedHashSet<List<Serializable>>(portletWindows.size());
        
        for (final IPortletWindow portletWindow : portletWindows) {
        	if(portletWindow != null) {
        		final IPortletWindowId portletWindowId = portletWindow.getPortletWindowId();
        		final WindowState windowState = portletWindow.getWindowState();
        		final PortletMode portletMode = portletWindow.getPortletMode();
        		final List<Serializable> portletWindowKey = Arrays.asList(portletWindowId, windowState.toString(), portletMode.toString());
        		cacheKey.add(portletWindowKey);
        	} else {
        		this.logger.warn("portletWindowRegistry#getAllLayoutPortletWindows() returned a null portletWindow"); 
        	}
        }
        
        return new CacheKey(this.name, cacheKey);
    }
}