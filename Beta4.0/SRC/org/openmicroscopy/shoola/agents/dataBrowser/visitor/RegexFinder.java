/*
 * org.openmicroscopy.shoola.agents.dataBrowser.visitor.RegexFinder 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2008 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.agents.dataBrowser.visitor;



//Java importss
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.dataBrowser.browser.ImageDisplay;
import org.openmicroscopy.shoola.agents.dataBrowser.browser.ImageDisplayVisitor;
import org.openmicroscopy.shoola.agents.dataBrowser.browser.ImageNode;
import org.openmicroscopy.shoola.agents.dataBrowser.browser.ImageSet;
import org.openmicroscopy.shoola.util.ui.RegExFactory;
import pojos.DatasetData;
import pojos.ImageData;
import pojos.ProjectData;

/** 
 * Finds the nodes matching the specified pattern.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
public class RegexFinder 
	implements ImageDisplayVisitor
{

    /** The pattern to find. */
    private Pattern				pattern;

    /** The collection of found nodes. */
    private List<ImageDisplay>	foundNodes;
    
    /**
     * Returns the name of the specified object.
     * 
     * @param userObject The <code>DataObject</code> hosted by the visited node.
     * @return See above.
     */
    private String getName(Object userObject)
    {
        if (userObject instanceof ProjectData) 
            return ((ProjectData) userObject).getName();
        else if (userObject instanceof DatasetData) 
            return ((DatasetData) userObject).getName();
        else if (userObject instanceof ImageData) 
            return ((ImageData) userObject).getName();
        return null;
    }
    
    /**
     * Returns the description of the specified object.
     * 
     * @param userObject The <code>DataObject</code> hosted by the visited node.
     * @return See above.
     */
    private String getDescription(Object userObject)
    {
        if (userObject instanceof ProjectData) 
            return ((ProjectData) userObject).getDescription();
        else if (userObject instanceof DatasetData) 
            return ((DatasetData) userObject).getDescription();
        else if (userObject instanceof ImageData) 
            return ((ImageData) userObject).getDescription();
        return null;
    }
    
    /**
     * Finds the pattern.
     * 
     * @param node The node to visit.
     */
    private void foundNode(ImageDisplay node)
    {
        Object userObject = node.getHierarchyObject();
        String name = getName(userObject);
        if (name != null) {
        	name = name.trim();
        	if (RegExFactory.find(pattern, name) && !foundNodes.contains(node))
        		foundNodes.add(node);
        }
        	
        String description = getDescription(userObject);
        if (description != null) {
        	description = description.trim();
        	if (RegExFactory.find(pattern, description) 
        			&& !foundNodes.contains(node))
        		foundNodes.add(node);
        } 
    }
    
    /**
     * Creates a new instance.
     * 
     * @param pattern The pattern to search for.
     */
    public RegexFinder(Pattern pattern)
    {
    	if (pattern == null)
    		throw new IllegalArgumentException("No pattern specified.");
    	this.pattern = pattern;
    	foundNodes = new ArrayList<ImageDisplay>();
    }
    
	/**
	 * Returns the collection of found nodes.
	 * 
	 * @return See above.
	 */
    public List<ImageDisplay> getFoundNodes() { return foundNodes; }
    
    /** 
     * Implemented as specified by {@link ImageDisplayVisitor}. 
     * @see ImageDisplayVisitor#visit(ImageNode)
     */
	public void visit(ImageNode node) { foundNode(node); }

	 /** 
     * Required by the {@link ImageDisplayVisitor} I/F but no-op in our case. 
     * @see ImageDisplayVisitor#visit(ImageSet)
     */
	public void visit(ImageSet node) { foundNode(node); }
    
}
