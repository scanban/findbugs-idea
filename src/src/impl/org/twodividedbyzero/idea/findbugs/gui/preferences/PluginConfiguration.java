/*
 * Copyright 2009 Andre Pfeiler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.twodividedbyzero.idea.findbugs.gui.preferences;

import com.intellij.openapi.ui.Messages;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.Project;
import info.clearthought.layout.TableLayout;
import org.twodividedbyzero.idea.findbugs.gui.preferences.BrowseAction.BrowseActionCallback;
import org.twodividedbyzero.idea.findbugs.gui.toolwindow.view.ToolWindowPanel;
import org.twodividedbyzero.idea.findbugs.preferences.FindBugsPreferences;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;


/**
 * $Date$
 *
 * @author Andre Pfeiler<andrepdo@dev.java.net>
 * @version $Revision$
 * @since 0.9.9
 */
public class PluginConfiguration implements ConfigurationPage {

	private final FindBugsPreferences _preferences;
	private final ConfigurationPanel _parent;
	private Component _component;
	private JPanel _pluginsPanel;
	private JPanel _checkboxPanel;


	public PluginConfiguration(final ConfigurationPanel parent, final FindBugsPreferences preferences) {
		//noinspection AssignmentToCollectionOrArrayFieldFromParameter
		_preferences = preferences;
		_parent = parent;
	}


	public Component getComponent() {
		if (_component == null) {
			final double border = 5;
			final double[][] size = {{border, TableLayout.FILL, border}, // Columns
									 {border, TableLayout.PREFERRED, border}};// Rows
			final TableLayout tbl = new TableLayout(size);

			final Container mainPanel = new JPanel(tbl);
			mainPanel.add(getPluginPanel(), "1, 1, 1, 1");

			_component = mainPanel;
		}
		//updatePreferences();
		return _component;
	}


	public void updatePreferences() {
		rebuildCheckboxes();
	}


	private void rebuildCheckboxes() {
		if (_checkboxPanel == null) {
			return;
		}
		_checkboxPanel.removeAll();
		final Project currentProject = getCurrentProject();
		for (final Plugin plugin : Plugin.getAllPlugins()) {
			if (plugin.isCorePlugin()) {
				continue;
			}
			String text = plugin.getShortDescription();
			String id = plugin.getPluginId();
			if (text == null) {
				text = id;
			}
			String pluginUrl = plugin.getPluginLoader().getURL().toExternalForm();
			text = String.format("<html>%s<br><font style='font-weight:normal;font-style:italic'>%s", text, pluginUrl);

			boolean enabled = isEnabled(currentProject, plugin);
			final JCheckBox checkbox = new JCheckBox(text, enabled);
			checkbox.setVerticalTextPosition(SwingConstants.TOP);
			String longText = plugin.getDetailedDescription();
			if (longText != null) {
				checkbox.setToolTipText("<html>" + longText + "</html>");
			}
			checkbox.setSelected(!_preferences.isPluginDisabled(plugin.getPluginId()));
			checkbox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					_preferences.enablePlugin(plugin.getPluginId(), checkbox.isSelected());
					_preferences.setModified(true);
				}
			});
			_checkboxPanel.add(checkbox);
		}
	}


	private boolean isEnabled(Project project, Plugin plugin) {
		if (project == null) {
			return plugin.isGloballyEnabled();
		}
		return project.getPluginStatus(plugin);
	}


	JPanel getPluginPanel() {
		if (_pluginsPanel == null) {

			final double border = 5;
			final double colsGap = 10;
			final double[][] size = {{border, TableLayout.FILL, colsGap, TableLayout.PREFERRED, border}, // Columns
									 {border, TableLayout.FILL, border}};// Rows
			final TableLayout tbl = new TableLayout(size);
			_pluginsPanel = new JPanel(tbl);
			_pluginsPanel.setBorder(BorderFactory.createTitledBorder("Plugins"));

			_checkboxPanel = new JPanel();
			_checkboxPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
			_checkboxPanel.setLayout(new BoxLayout(_checkboxPanel, BoxLayout.Y_AXIS));
			rebuildCheckboxes();

			final Component scrollPane = new JScrollPane(_checkboxPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			_pluginsPanel.add(scrollPane, "1, 1, 1, 1"); // col ,row, col, row


			final double rowsGap = 5;
			final double[][] bPanelSize = {{border, TableLayout.PREFERRED}, // Columns
										   {border, TableLayout.PREFERRED, rowsGap, TableLayout.PREFERRED, border}};// Rows
			final TableLayout tableLayout = new TableLayout(bPanelSize);

			final Container buttonPanel = new JPanel(tableLayout);
			_pluginsPanel.add(buttonPanel, "3, 1, 3, 1");

			final AbstractButton addButton = new JButton();
			final Action action = new BrowseAction(_parent, "Install New Plugin...", new FileNameExtensionFilter("FindBugs Plugins (*.jar)", "jar"), new BrowseActionCallback() {
				public void addSelection(final File selectedFile) {
					try {
						Plugin.loadPlugin(selectedFile, getCurrentProject());
						try {
							_preferences.addPlugin(selectedFile.toURI().toURL().toExternalForm());
						} catch (MalformedURLException e) {
							Messages.showErrorDialog(e.getMessage(), e.getClass().getSimpleName());
						}
						updatePreferences();
					} catch (PluginException e) {
						Messages.showErrorDialog(_parent, "Error loading " + selectedFile.getPath() + ":\n\n" + e.getClass().getSimpleName() + ": " + e.getMessage(), "Error loading plugin");
					}
					_preferences.setModified(true);
				}
			});
			addButton.setAction(action);
			buttonPanel.add(addButton, "1, 1, 1, 1");

		}

		return _pluginsPanel;
	}


	private Project getCurrentProject() {
		ToolWindowPanel panel = _parent.getFindBugsPlugin().getToolWindowPanel();
		if (panel == null) {
			return null;
		}
		BugCollection bugCollection = panel.getBugCollection();
		if (bugCollection == null) {
			return null;
		}
		return bugCollection.getProject();
	}


	public void setEnabled(boolean enabled) {
		_pluginsPanel.setEnabled(enabled);
	}


	public boolean showInModulePreferences() {
		return false;
	}

}