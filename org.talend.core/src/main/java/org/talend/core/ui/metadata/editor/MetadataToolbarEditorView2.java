// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.core.ui.metadata.editor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.talend.core.model.action.IAction;
import org.talend.core.model.metadata.MetadataSchema;
import org.talend.core.model.metadata.builder.connection.ConnectionFactory;
import org.talend.core.model.metadata.builder.connection.MetadataColumn;
import org.talend.core.model.metadata.editor.MetadataEditor2;
import org.talend.core.model.metadata.editor.MetadataEditorActionFactory2;
import org.talend.core.model.metadata.editor.MetadataEditorEvent;
import org.talend.core.ui.ImageProvider;
import org.talend.core.ui.ImageProvider.EImage;
import org.xml.sax.SAXException;

/**
 * DOC amaumont class global comment. Detailled comment <br/>
 * 
 * TGU same purpose as MetadataToolbarEditorView but uses EMF model directly $Id: MetadataToolbarEditorView2.java,v 1.1
 * 2006/08/02 19:43:45 tguiu Exp $
 * 
 */
public class MetadataToolbarEditorView2 {

    private Composite toolbar;

    private Button addButton;

    private Button removeButton;

    private Button copyButton;

    // private Button cutButton;

    private Button pasteButton;

    private Button moveUpButton;

    private Button moveDownButton;

    private Button loadButton;

    private Button exportButton;

    private MetadataTableEditorView2 metadataEditorView;

    private String defaultLabel = "newColumn";

    private static int indexNewColumn;

    /**
     * DOC amaumont MatadataToolbarEditor constructor comment.
     * 
     * @param parent
     * @param style
     * @param metadataEditorView
     */
    public MetadataToolbarEditorView2(Composite parent, int style, MetadataTableEditorView2 metadatEditorView) {
        toolbar = new Composite(parent, style);
        toolbar.setLayout(new RowLayout(SWT.HORIZONTAL));
        // Force the height of the toolbars
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.minimumHeight = 40;
        gridData.heightHint = 40;
        toolbar.setLayoutData(gridData);

        this.metadataEditorView = metadatEditorView;
        createComponents();
        addListeners();
    }

    /**
     * DOC amaumont Comment method "addListeners".
     */
    private void addListeners() {
        addButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (metadataEditorView.getMetadataEditor() != null) {
                    metadataEditorView.getTableViewerCreator().getTable().setFocus();
                    MetadataEditorEvent metadataEditorEvent = new MetadataEditorEvent();
                    metadataEditorEvent.type = MetadataEditorEvent.TYPE.ADD;
                    MetadataColumn metadataColumn = ConnectionFactory.eINSTANCE.createMetadataColumn();
                    metadataColumn.setLabel(metadataEditorView.getMetadataEditor().getValidateColumnName(defaultLabel,
                            (++indexNewColumn)));
                    metadataEditorEvent.entries.add(metadataColumn);
                    Table metadataEditorTable = metadataEditorView.getTableViewerCreator().getTable();
                    metadataEditorEvent.entriesIndices = metadataEditorTable.getSelectionIndices();
                    IAction action = MetadataEditorActionFactory2.getInstance()
                            .getAction(metadataEditorView, metadataEditorEvent);
                    action.run(metadataEditorEvent);
                }
            }
        });

        removeButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (metadataEditorView.getMetadataEditor() != null) {
                    int index = metadataEditorView.getTableViewerCreator().getTable().getSelectionIndex();
                    metadataEditorView.getTableViewerCreator().getTable().setFocus();
                    MetadataEditorEvent metadataEditorEvent = new MetadataEditorEvent();
                    metadataEditorEvent.type = MetadataEditorEvent.TYPE.REMOVE;
                    metadataEditorEvent.entriesIndices = metadataEditorView.getTableViewerCreator().getTable()
                            .getSelectionIndices();
                    IAction action = MetadataEditorActionFactory2.getInstance()
                            .getAction(metadataEditorView, metadataEditorEvent);
                    action.run(metadataEditorEvent);
                    if ((index) < metadataEditorView.getTableViewerCreator().getTable().getItemCount()) {
                        metadataEditorView.getTableViewerCreator().getTable().setSelection(index);
                    } else if (metadataEditorView.getTableViewerCreator().getTable().getItemCount() != 0) {
                        metadataEditorView.getTableViewerCreator().getTable().setSelection(
                                metadataEditorView.getTableViewerCreator().getTable().getItemCount() - 1);
                    }
                    metadataEditorView.getTableViewerCreator().getTableViewer().refresh();
                }
            }
        });

        copyButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (metadataEditorView.getMetadataEditor() != null) {
                    metadataEditorView.getTableViewerCreator().getTable().setFocus();
                    MetadataEditorEvent metadataEditorEvent = new MetadataEditorEvent();
                    metadataEditorEvent.type = MetadataEditorEvent.TYPE.COPY;
                    metadataEditorEvent.entriesIndices = metadataEditorView.getTableViewerCreator().getTable()
                            .getSelectionIndices();
                    IAction action = MetadataEditorActionFactory2.getInstance()
                            .getAction(metadataEditorView, metadataEditorEvent);
                    action.run(metadataEditorEvent);
                    metadataEditorView.getTableViewerCreator().getTableViewer().refresh();
                }
            }
        });

        pasteButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (metadataEditorView.getMetadataEditor() != null) {
                    metadataEditorView.getTableViewerCreator().getTable().setFocus();
                    MetadataEditorEvent metadataEditorEvent = new MetadataEditorEvent();
                    metadataEditorEvent.type = MetadataEditorEvent.TYPE.PASTE;
                    metadataEditorEvent.entriesIndices = metadataEditorView.getTableViewerCreator().getTable()
                            .getSelectionIndices();
                    IAction action = MetadataEditorActionFactory2.getInstance()
                            .getAction(metadataEditorView, metadataEditorEvent);
                    action.run(metadataEditorEvent);
                    metadataEditorView.getTableViewerCreator().getTableViewer().refresh();
                }
            }
        });

        moveUpButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (metadataEditorView.getMetadataEditor() != null) {
                    metadataEditorView.getTableViewerCreator().getTable().setFocus();
                    MetadataEditorEvent metadataEditorEvent = new MetadataEditorEvent();
                    metadataEditorEvent.type = MetadataEditorEvent.TYPE.MOVE_UP;
                    metadataEditorEvent.entriesIndices = metadataEditorView.getTableViewerCreator().getTable()
                            .getSelectionIndices();
                    IAction action = MetadataEditorActionFactory2.getInstance()
                            .getAction(metadataEditorView, metadataEditorEvent);
                    action.run(metadataEditorEvent);
                    metadataEditorView.getTableViewerCreator().getTableViewer().refresh();
                }
            }
        });

        moveDownButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (metadataEditorView.getMetadataEditor() != null) {
                    metadataEditorView.getTableViewerCreator().getTable().setFocus();
                    MetadataEditorEvent metadataEditorEvent = new MetadataEditorEvent();
                    metadataEditorEvent.type = MetadataEditorEvent.TYPE.MOVE_DOWN;
                    metadataEditorEvent.entriesIndices = metadataEditorView.getTableViewerCreator().getTable()
                            .getSelectionIndices();
                    IAction action = MetadataEditorActionFactory2.getInstance()
                            .getAction(metadataEditorView, metadataEditorEvent);
                    action.run(metadataEditorEvent);
                    metadataEditorView.getTableViewerCreator().getTableViewer().refresh();
                }
            }
        });

        loadButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                FileDialog dial = new FileDialog(toolbar.getShell(), SWT.OPEN);
                dial.setFilterExtensions(new String[] { "*.xml" });
                String fileName = dial.open();
                if ((fileName != null) && (!fileName.equals(""))) {
                    File file = new File(fileName);
                    if (file != null) {
                        MetadataEditor2 newEditor = metadataEditorView.getMetadataEditor();
                        if (newEditor != null) {
                            try {
                                // remove all the columns from the table
                                MetadataEditorEvent metadataEditorEvent = new MetadataEditorEvent();
                                metadataEditorEvent.type = MetadataEditorEvent.TYPE.REMOVE;
                                int[] index = new int[metadataEditorView.getTableViewerCreator().getTable().getItemCount()];
                                for (int i = 0; i < metadataEditorView.getTableViewerCreator().getTable().getItemCount(); i++) {
                                    index[i] = i;
                                }
                                metadataEditorEvent.entriesIndices = index;
                                IAction action = MetadataEditorActionFactory2.getInstance().getAction(metadataEditorView,
                                        metadataEditorEvent);
                                action.run(metadataEditorEvent);

                                // load the schema
                                List<MetadataColumn> loadMetadataColumn = MetadataSchema.loadMetadataColumnFromFile(file);

                                // Add Columnsof Schema to the table
                                if (!loadMetadataColumn.isEmpty()) {
                                    metadataEditorEvent.type = MetadataEditorEvent.TYPE.ADD;
                                    List<MetadataColumn> newList = new ArrayList<MetadataColumn>();
                                    for (int i = 0; i < loadMetadataColumn.size(); i++) {
                                        // check the unicity of label
                                        String label = loadMetadataColumn.get(i).getLabel();
                                        label = metadataEditorView.getMetadataEditor().getValidateColumnName(label, i, newList);
                                        loadMetadataColumn.get(i).setLabel(label);
                                        newList.add(loadMetadataColumn.get(i));
                                        metadataEditorEvent.entries.add(loadMetadataColumn.get(i));
                                    }
                                    Table metadataEditorTable = metadataEditorView.getTableViewerCreator().getTable();
                                    metadataEditorEvent.entriesIndices = metadataEditorTable.getSelectionIndices();
                                    action = MetadataEditorActionFactory2.getInstance().getAction(metadataEditorView,
                                            metadataEditorEvent);
                                    action.run(metadataEditorEvent);
                                }

                                // Refresh the table
                                metadataEditorView.getTableViewerCreator().getTable().setFocus();
                                metadataEditorView.getTableViewerCreator().getTable().deselectAll();
                                metadataEditorView.getTableViewerCreator().getTableViewer().refresh();
                                metadataEditorView.getTableViewerCreator().layout();

                            } catch (ParserConfigurationException e) {
                                openMessageError("Parsing XML Error : " + e.getMessage());
                            } catch (SAXException e) {
                                openMessageError("Parsing XML Error : " + e.getMessage());
                            } catch (IOException e) {
                                openMessageError("File Error : " + e.getMessage());
                            }
                        }
                    }
                }
            }

            private void openMessageError(String errorText) {
                MessageBox msgBox = new MessageBox(toolbar.getShell());
                msgBox.setText("Error Occurred");
                msgBox.setMessage(errorText);
                msgBox.open();
            }
        });

        exportButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                FileDialog dial = new FileDialog(toolbar.getShell(), SWT.SAVE);
                dial.setFilterExtensions(new String[] { "*.xml" });
                String fileName = dial.open();
                if ((fileName != null) && (!fileName.equals(""))) {
                    File file = new File(fileName);
                    try {
                        file.createNewFile();
                        if (file != null) {
                            MetadataEditor2 newEditor = metadataEditorView.getMetadataEditor();
                            if (newEditor != null) {
                                // get all the columns from the table
                                org.talend.core.model.metadata.builder.connection.MetadataTable oldTable = newEditor
                                        .getMetadataTable();

                                MetadataSchema.saveMetadataColumnToFile(file, oldTable);
                            }
                        }
                    } catch (IOException e) {
                        openMessageError("File Error : " + e.getMessage());
                    } catch (ParserConfigurationException e) {
                        openMessageError("Parsing XML Error : " + e.getMessage());
                    }
                }
            }

            private void openMessageError(String errorText) {
                MessageBox msgBox = new MessageBox(toolbar.getShell());
                msgBox.setText("Error Occurred");
                msgBox.setMessage(errorText);
                msgBox.open();
            }
        });
    }

    /**
     * DOC amaumont Comment method "createComponents".
     */
    private void createComponents() {

        addButton = new Button(toolbar, SWT.PUSH);
        addButton.setToolTipText("Add");
        addButton.setImage(ImageProvider.getImage(EImage.ADD_ICON));

        removeButton = new Button(toolbar, SWT.PUSH);
        removeButton.setToolTipText("Remove");
        removeButton.setImage(ImageProvider.getImage(EImage.MINUS_ICON));

        copyButton = new Button(toolbar, SWT.PUSH);
        copyButton.setToolTipText("Copy");
        copyButton.setImage(ImageProvider.getImage(EImage.COPY_ICON));

        // cutButton = new Button(toolbar, SWT.PUSH);
        // cutButton.setText("Cut");
        // cutButton.setImage(ImageProvider.getImage(EImage.CUT).createImage());

        pasteButton = new Button(toolbar, SWT.PUSH);
        pasteButton.setToolTipText("Paste");
        pasteButton.setImage(ImageProvider.getImage(EImage.PASTE_ICON));

        moveUpButton = new Button(toolbar, SWT.PUSH);
        moveUpButton.setToolTipText("Move up");
        moveUpButton.setImage(ImageProvider.getImage(EImage.UP_ICON));

        moveDownButton = new Button(toolbar, SWT.PUSH);
        moveDownButton.setToolTipText("Move down");
        moveDownButton.setImage(ImageProvider.getImage(EImage.DOWN_ICON));

        loadButton = new Button(toolbar, SWT.PUSH);
        loadButton.setToolTipText("Import");
        loadButton.setImage(ImageProvider.getImage(EImage.IMPORT_ICON));

        exportButton = new Button(toolbar, SWT.PUSH);
        exportButton.setToolTipText("Export");
        exportButton.setImage(ImageProvider.getImage(EImage.EXPORT_ICON));
    }

    /**
     * DOC ocarbone Comment method "setReadOnly". setReadOnly(boolean) is equivalent to setEnabled(boolean) all the
     * buttons.
     * 
     * @param b
     */
    public void setReadOnly(boolean b) {
        addButton.setEnabled(!b);
        removeButton.setEnabled(!b);
        copyButton.setEnabled(!b);
        pasteButton.setEnabled(!b);
        moveUpButton.setEnabled(!b);
        moveDownButton.setEnabled(!b);
        loadButton.setEnabled(!b);
        exportButton.setEnabled(!b);
    }

    /**
     * DOC ocarbone Comment method "setDefaultLabel". setDefaultLabel determine the label to use when Add button is
     * used.
     * 
     * @param string
     */
    public void setDefaultLabel(String label) {
        this.defaultLabel = label;
    }
}
