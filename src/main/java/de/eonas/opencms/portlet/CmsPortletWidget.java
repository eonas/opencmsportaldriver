/*
 * File   : $Source: /usr/local/cvs/opencms/src/org/opencms/widgets/CmsInputWidget.java,v $
 * Date   : $Date: 2008-02-27 12:05:44 $
 * Version: $Revision: 1.12 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) 2002 - 2008 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package de.eonas.opencms.portlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pluto.container.PortletContainer;
import org.apache.pluto.container.driver.DriverPortletContext;
import org.apache.pluto.container.om.portlet.PortletApplicationDefinition;
import org.apache.pluto.container.om.portlet.PortletDefinition;
import org.apache.pluto.driver.AttributeKeys;
import org.apache.pluto.driver.container.PortalDriverServicesImpl;
import org.apache.pluto.driver.services.portal.PortletWindowConfig;
import org.jetbrains.annotations.NotNull;
import org.opencms.file.CmsObject;
import org.opencms.util.CmsMacroResolver;
import org.opencms.util.CmsStringUtil;
import org.opencms.widgets.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Provides a standard HTML form input widget, for use on a widget dialog.
 * <p/>
 *
 * @author Anton Seemann
 * @author Helmut Manck
 * @version 0.1 $
 * @since 7.0.0
 */
public class CmsPortletWidget extends CmsSelectWidget {
    public static javax.servlet.ServletContext m_context;

    private static final Log LOG = LogFactory.getLog(CmsPortletWidget.class);

    /**
     * Creates a new input widget.
     * <p/>
     */
    @SuppressWarnings("UnusedDeclaration")
    public CmsPortletWidget() {
        super(construct());
    }

    @SuppressWarnings("UnusedDeclaration")
    public CmsPortletWidget(java.util.List<org.opencms.widgets.CmsSelectWidgetOption> configuration) {
        super(configuration);
    }

    /**
     * Creates a new input widget with the given configuration.
     * <p/>
     *
     * @param configuration the configuration to use
     */
    @SuppressWarnings("UnusedParameters")
    public CmsPortletWidget(String configuration) {
        super();
    }

    /**
     * Diese Methode holt sich vom PortletContainer die regestrierten
     * PortletApplikationen und baut sich aus Ihnen die benoetigten PortletId.
     * Diese werden in ein <select> Element eingebaut und im Template zur
     * Auswahl angeboten.
     *
     * @see org.opencms.widgets.I_CmsWidget#getDialogWidget(org.opencms.file.CmsObject,
     *      org.opencms.widgets.I_CmsWidgetDialog,
     *      org.opencms.widgets.I_CmsWidgetParameter)
     */
    public String getDialogWidget(CmsObject cms, I_CmsWidgetDialog widgetDialog, @NotNull I_CmsWidgetParameter param) {

        String id = param.getId();
        StringBuilder result = new StringBuilder(16);

        result.append("<td class=\"xmlTd\" style=\"height: 25px;\"><select class=\"xmlInput");
        if (param.hasError()) {
            result.append(" xmlInputError");
        }
        result.append("\" name=\"");
        result.append(id);
        result.append("\" id=\"");
        result.append(id);
        result.append("\">");


        String selected = getSelectedValue(cms, param);
        ArrayList<String> listPortlets = fetchRegisteredPortlets(selected);

        for (String option : listPortlets) {
            result.append("<option \"");
            try {
                // Um die Sonderzeichen aus der Id zu kriegen, werden Sie mit
                // utf-8 encodet
                result.append(java.net.URLEncoder.encode(option, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                LOG.error(e);
            }
            result.append("\"");

            if ((selected != null) && selected.equals(option)) {
                result.append(" selected=\"selected\"");
            }
            result.append(">");

            // Hier wird das encoding wieder in html encoded

            String htmlOption = org.apache.commons.lang.StringEscapeUtils.escapeHtml(option);

            result.append(htmlOption);
            result.append("</option>");
        }

        return result.toString();

    }

    static private ArrayList<String> fetchRegisteredPortlets(String selected) {
        String metainfo = null;
        if (!CmsStringUtil.isEmpty(selected)) {
            PortletWindowConfig selectedConfig = PortletWindowConfig.fromId(selected);
            metainfo = selectedConfig.getMetaInfo();
        }

        if (metainfo == null) {
            metainfo = createPlacementId();
        }

        if ( m_context == null ) {
            LOG.warn("ServletContext still unset. Maybe the listener is missing?");
            return new ArrayList<String>();
        }
        // Es wird auf die PortletContainer Instanz zugegriffen
        PortletContainer container = (PortletContainer) m_context.getAttribute(AttributeKeys.PORTLET_CONTAINER);

        PortalDriverServicesImpl driverserviceimpl = (PortalDriverServicesImpl) container.getContainerServices();
        Iterator<DriverPortletContext> iter = driverserviceimpl.getPortletContextService().getPortletContexts();

        ArrayList<String> liste = new ArrayList<String>();
        // Das Select Element wird mit Werten der PortletApplikationen gefuellt
        while (iter.hasNext()) {
            DriverPortletContext portletcontext = iter.next();
            PortletApplicationDefinition portletAppDd = portletcontext.getPortletApplicationDefinition();

            String contextPath = portletcontext.getApplicationName();
            if (contextPath.length() > 0) {
                contextPath = "/" + contextPath;
            }

            for (PortletDefinition portlet : portletAppDd.getPortlets()) {
                String portletName = portlet.getPortletName();

                String portletId = PortletWindowConfig.createPortletId(contextPath, portletName, metainfo);

                liste.add(portletId);
            }
        }

        // Pruefung damit ein Portlet, das selektiert ist, aber zur Zeit nicht
        // verfuegbar ist, troztdem in der Liste bleibt
        // und nicht ausgelassen wird.
        if (selected != null && selected.length() > 0 && !liste.contains(selected)) {
            liste.add(selected);
        }

        Collections.sort(liste, String.CASE_INSENSITIVE_ORDER);
        return liste;
    }

    @NotNull
    static private String createPlacementId() {
        return "";
    }

    /**
     * @see org.opencms.widgets.A_CmsWidget#getWidgetStringValue(org.opencms.file.CmsObject,
     *      org.opencms.widgets.I_CmsWidgetDialog,
     *      org.opencms.widgets.I_CmsWidgetParameter)
     */
    public String getWidgetStringValue(CmsObject cms, @NotNull I_CmsWidgetDialog widgetDialog, @NotNull I_CmsWidgetParameter param) {

        String result = super.getWidgetStringValue(cms, widgetDialog, param);
        String configuration = CmsMacroResolver.resolveMacros(getConfiguration(), cms, widgetDialog.getMessages());
        if (configuration == null) {
            configuration = param.getDefault(cms);
        }
        List<CmsSelectWidgetOption> options = CmsSelectWidgetOption.parseOptions(configuration);
        for (CmsSelectWidgetOption option : options) {
            if (result.equals(option.getValue())) {
                result = option.getOption();
                break;
            }
        }
        return result;
    }


    @Override
    protected List<CmsSelectWidgetOption> parseSelectOptions(CmsObject cms, I_CmsWidgetDialog widgetDialog, I_CmsWidgetParameter param) {
        List<CmsSelectWidgetOption> list = super.parseSelectOptions(cms, widgetDialog, param);
        List<CmsSelectWidgetOption> portletList = construct();
        list.addAll(portletList);
        return list;
    }

    /**
     * @see org.opencms.widgets.I_CmsWidget#newInstance()
     */
    @NotNull
    public I_CmsWidget newInstance() {

        return new CmsPortletWidget(getConfiguration());
    }

    static private List<CmsSelectWidgetOption> construct() {

        String selected = null;
        ArrayList<String> portlets = fetchRegisteredPortlets(selected);
        List<CmsSelectWidgetOption> list = new ArrayList<CmsSelectWidgetOption>();
        for (String portlet : portlets) {
            CmsSelectWidgetOption option = new CmsSelectWidgetOption(portlet);
            list.add(option);
        }
        CmsSelectWidgetOption option = new CmsSelectWidgetOption("");
        list.add(option);
        return list;
    }

}
