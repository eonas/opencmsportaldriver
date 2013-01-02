package de.eonas.opencms.portlet;

import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class CmsContextListener implements ServletContextListener{

	public void contextDestroyed(ServletContextEvent arg0) {
	}

	public void contextInitialized(@NotNull ServletContextEvent arg0) {
		CmsPortletWidget.m_context = arg0.getServletContext();
	}

}
