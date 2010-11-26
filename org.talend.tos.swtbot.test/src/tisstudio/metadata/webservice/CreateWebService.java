// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package tisstudio.metadata.webservice;

import junit.framework.Assert;

import org.eclipse.swt.widgets.Tree;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.talend.swtbot.TalendSwtBotForTos;

/**
 * DOC Administrator class global comment. Detailled comment
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class CreateWebService extends TalendSwtBotForTos {

    private SWTBotView view;

    private SWTBotTree tree;

    private static String WEBSERVICENAME = "webService2"; //$NON-NLS-1$

    @Before
    public void InitialisePrivateFields() {
        view = gefBot.viewByTitle("Repository");
        view.setFocus();
        tree = new SWTBotTree((Tree) gefBot.widget(WidgetOfType.widgetOfType(Tree.class), view.getWidget()));
    }

    @Test
    public void createWebService() {
        tree.setFocus();

        tree.expandNode("Metadata").getNode("Web Service").contextMenu("Create Web Service schema").click();
        gefBot.waitUntil(Conditions.shellIsActive("Create new Web Service schema"));
        gefBot.shell("Create new Web Service schema").activate();

        /* step 1 of 4 */
        gefBot.textWithLabel("Name").setText(WEBSERVICENAME);
        gefBot.button("Next >").click();

        /* step 2 of 4 */
        gefBot.radio("Advanced WebService").click();
        gefBot.button("Next >").click();

        /* step 3 of 4 */
        gefBot.button(1).click();// click refresh button
        gefBot.tableWithLabel("Operation:").click(0, 0);

        // set input mapping
        // left schema
        gefBot.cTabItem(" Input mapping ").activate();
        gefBot.button("Schema Management").click();
        gefBot.shell("Schema").activate();
        gefBot.buttonWithTooltip("Add").click();
        gefBot.button("OK").click();
        // right schema
        gefBot.table(1).click(0, 1);
        gefBot.buttonWithTooltip("Add list element").click();
        gefBot.shell("ParameterTree").activate();
        gefBot.tree().select("City");
        gefBot.button("OK").click();

        gefBot.table(1).click(1, 0);
        gefBot.text().setText("input.newColumn");

        // set output mapping
        // left schema
        gefBot.cTabItem(" Output mapping ").activate();
        gefBot.table(0).click(0, 0);
        gefBot.buttonWithTooltip("Add List element").click();
        gefBot.shell("ParameterTree").activate();
        gefBot.tree().select("GetWeatherResult");
        gefBot.button("OK").click();
        // right schema
        gefBot.button("...").click();
        gefBot.buttonWithTooltip("Add").click();
        gefBot.button("OK").click();

        gefBot.table(1).click(0, 0);
        gefBot.text().setText("parameters.GetWeatherResult ");

        gefBot.button("Next >").click();

        /* step 4 of 4 */
        gefBot.button("Finish").click();

        SWTBotTreeItem newWebServiceItem = tree.expandNode("Metadata", "Web Service").select(WEBSERVICENAME + " 0.1");
        Assert.assertNotNull(newWebServiceItem);
    }

    @After
    public void removePreviouslyCreateItems() {
        tree.expandNode("Metadata", "Web Service").getNode(WEBSERVICENAME + " 0.1").contextMenu("Delete").click();

        tree.select("Recycle bin").contextMenu("Empty recycle bin").click();
        gefBot.waitUntil(Conditions.shellIsActive("Empty recycle bin"));
        gefBot.button("Yes").click();
    }
}
