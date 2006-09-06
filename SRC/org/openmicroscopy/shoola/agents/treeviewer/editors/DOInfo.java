/*
 * org.openmicroscopy.shoola.agents.treeviewer.editors.DOInfo
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.agents.treeviewer.editors;


//Java imports
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.util.ui.UIUtilities;
import pojos.PermissionData;

/** 
 * A UI component displaying owner's information, image's information etc.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
class DOInfo
    extends JPanel
{

    /** Text displaying before the owner's permissions. */
    private static final String    OWNER = "Owner: ";
    
    /** Text displaying before the group's permissions. */
    private static final String     GROUP = "Group: ";
    
    /** Text displaying before the world's permissions. */
    private static final String     WORLD = "Others: ";
    
    /** Text describing the <code>Read</code> permission. */
    private static final String     READ = "Read";
    
    /** Text describing the <code>Write</code> permission. */
    private static final String     WRITE = "Write";
    
    /** The text displayed before the group's details. */
    private static final String		GROUP_TEXT = "Group's information: ";
    
    /**
     * A reduced size for the invisible components used to separate widgets
     * vertically.
     */
    private static final Dimension  SMALL_V_SPACER_SIZE = new Dimension(1, 6);
    
    /** The panel hosting the group's details. */
    private JPanel          groupPanel;
    
    /** Reference to the Model. */
    private EditorModel     model;
    
    /**
     * Builds the panel hosting the information
     * 
     * @param details The information to display.
     * @return See above.
     */
    private JPanel buildContentPanel(Map details)
    {
        JPanel content = new JPanel();
        content.setLayout(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 3, 3, 3);
        Iterator i = details.keySet().iterator();
        JLabel label;
        JTextField area;
        String key, value;
        while (i.hasNext()) {
            ++c.gridy;
            c.gridx = 0;
            key = (String) i.next();
            value = (String) details.get(key);
            label = UIUtilities.setTextFont(key);
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE;      //reset to default
            c.weightx = 0.0;  
            content.add(label, c);
            area = new JTextField(value);
            area.setEditable(false);
            label.setLabelFor(area);
            c.gridx = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            content.add(area, c);  
        }
        /*
        if (groups != null) {
           ++c.gridy;
           c.gridx = 0;
           JPanel bar = new JPanel();
           bar.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
           i = groups.iterator();
           JComponent component;
           while (i.hasNext()) {
               component = new GroupComponent((GroupData) i.next(), this);
               bar.add(component);
           }	
           label = UIUtilities.setTextFont(EditorUtil.GROUPS);
           c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
           c.fill = GridBagConstraints.NONE;      //reset to default
           c.weightx = 0.0;  
           content.add(label, c);
           label.setLabelFor(bar);
           c.gridx = 1;
           c.gridwidth = GridBagConstraints.REMAINDER;     //end row
           c.fill = GridBagConstraints.HORIZONTAL;
           c.weightx = 1.0;
           content.add(bar, c); 
       }
       */
       return content;
    }
    
    /**
     * Builds and lays out the panel displaying the permissions of the edited
     * file.
     * 
     * @param permissions   The permissions of the edited object.
     * @return See above.
     */
    private JPanel buildPermissions(final PermissionData permissions)
    {
        JPanel content = new JPanel();
        content.setLayout(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 3, 3, 3);
        //The owner is the only person allowed to modify the permissions.
        boolean isOwner = model.isObjectOwner();
        //Owner
        JLabel label = UIUtilities.setTextFont(OWNER);
        JPanel p = new JPanel();
        JCheckBox box =  new JCheckBox(READ);
        box.setSelected(permissions.isUserRead());
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
               JCheckBox source = (JCheckBox) e.getSource();
               permissions.setUserRead(source.isSelected());
            }
        });
        box.setEnabled(isOwner);
        p.add(box);
        box =  new JCheckBox(WRITE);
        box.setSelected(permissions.isUserWrite());
        box.addActionListener(new ActionListener() {
        
            public void actionPerformed(ActionEvent e)
            {
               JCheckBox source = (JCheckBox) e.getSource();
               permissions.setUserWrite(source.isSelected());
            }
        
        });
        box.setEnabled(isOwner);
        p.add(box);
        c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
        c.fill = GridBagConstraints.NONE;      //reset to default
        c.weightx = 0.0;  
        content.add(label, c);
        label.setLabelFor(p);
        c.gridx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;     //end row
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        content.add(UIUtilities.buildComponentPanel(p), c);  
        //Group
        label = UIUtilities.setTextFont(GROUP);
        p = new JPanel();
        box =  new JCheckBox(READ);
        box.setSelected(permissions.isGroupRead());
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
               JCheckBox source = (JCheckBox) e.getSource();
               permissions.setGroupRead(source.isSelected());
            }
        });
        box.setEnabled(isOwner);
        p.add(box);
        box =  new JCheckBox(WRITE);
        box.setSelected(permissions.isGroupWrite());
        box.addActionListener(new ActionListener() {
        
            public void actionPerformed(ActionEvent e)
            {
               JCheckBox source = (JCheckBox) e.getSource();
               permissions.setGroupWrite(source.isSelected());
            }
        
        });
        box.setEnabled(isOwner);
        p.add(box);
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
        c.fill = GridBagConstraints.NONE;      //reset to default
        c.weightx = 0.0;  
        content.add(label, c);
        label.setLabelFor(p);
        c.gridx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;     //end row
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        content.add(UIUtilities.buildComponentPanel(p), c);  
        //OTHER
        label = UIUtilities.setTextFont(WORLD);
        p = new JPanel();
        box =  new JCheckBox(READ);
        box.setSelected(permissions.isWorldRead());
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
               JCheckBox source = (JCheckBox) e.getSource();
               permissions.setWorldRead(source.isSelected());
            }
        });
        box.setEnabled(isOwner);
        p.add(box);
        box =  new JCheckBox(WRITE);
        box.setSelected(permissions.isWorldWrite());
        box.addActionListener(new ActionListener() {
        
            public void actionPerformed(ActionEvent e)
            {
               JCheckBox source = (JCheckBox) e.getSource();
               permissions.setWorldWrite(source.isSelected());
            }
        
        });
        box.setEnabled(isOwner);
        p.add(box);
        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
        c.fill = GridBagConstraints.NONE;      //reset to default
        c.weightx = 0.0;  
        content.add(label, c);
        label.setLabelFor(p);
        c.gridx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;     //end row
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        content.add(UIUtilities.buildComponentPanel(p), c);  
        return content;
    }
    
    /** 
     * Builds and lays out the GUI.
     *
     * @param details       The visualization map.
     * @param permission    Pass <code>true</code> to display the permission,
     *                      <code>false</code> otherwise.
     */
    private void buildGUI(Map details, boolean permission)
    {
        groupPanel = new JPanel();
        groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.Y_AXIS));
        groupPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        JPanel contentPanel = buildContentPanel(details);
        setLayout(new BorderLayout());
        setMaximumSize(contentPanel.getPreferredSize());
        setBorder(new EtchedBorder());
        add(contentPanel, BorderLayout.NORTH);
        if (model.getObjectPermissions() != null && permission) {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            p.add(new JSeparator());
            p.add(Box.createRigidArea(EditorUI.SMALL_V_SPACER_SIZE));
            p.add(buildPermissions(model.getObjectPermissions()));
            p.add(Box.createVerticalGlue());
            add(p);
        }
        //add(groupPanel, BorderLayout.CENTER);
    }
    
    /**
     * Creates a new instance.
     * 
     * @param details       The visualization map. Mustn't be <code>null</code>.
     * @param model         Reference to the Model. 
     *                      Mustn't be <code>null</code>.
     * @param permission    Pass <code>true</code> to display the permission,
     *                      <code>false</code> otherwise.
     */
    DOInfo(Map details, EditorModel model, boolean permission)
    {
        if (details == null) 
            throw new IllegalArgumentException("Visualization map cannot be" +
                    " null");
        if (model == null)
            throw new IllegalArgumentException("No model.");
        this.model = model;
        buildGUI(details, permission);
    }

    /**
     * Shows the specified group's details.
     * 
     * @param details The details to display.
     */
    void showGroupDetails(Map details)
    {
        groupPanel.removeAll();
        groupPanel.add(UIUtilities.buildComponentPanel(new JLabel(GROUP_TEXT)));
        groupPanel.add(new JSeparator());
        groupPanel.add(Box.createRigidArea(SMALL_V_SPACER_SIZE));
        JPanel content = new JPanel();
        content.setLayout(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 3, 3, 3);
        Iterator i = details.keySet().iterator();
        JLabel label;
        JTextField area;
        String key, value;
        while (i.hasNext()) {
            ++c.gridy;
            c.gridx = 0;
            key = (String) i.next();
            value = (String) details.get(key);
            label = UIUtilities.setTextFont(key);
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE;      //reset to default
            c.weightx = 0.0;  
            content.add(label, c);
            area = new JTextField(value);
            area.setEditable(false);
            label.setLabelFor(area);
            c.gridx = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            content.add(area, c);  
        }
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(content, BorderLayout.NORTH);
        groupPanel.add(p);
        validate();
        repaint();
    }

    /** Hides the details of the group. */
    void hideGroupDetails()
    {
        groupPanel.removeAll();
        validate();
        repaint();
    }
    
}
