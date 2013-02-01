package de.eonas.opencms.authentication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsUser;
import org.opencms.i18n.CmsLocaleManager;
import org.opencms.main.CmsSessionInfo;
import org.opencms.main.OpenCms;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.util.*;

public class OpenCmsAuthenticatedRequest extends HttpServletRequestWrapper {
    public static final String SESSION_CMSOBJECT = "OpenCmsCmsObject";
    public static final String SESSION_SESSIONINFO = "OpenCmsCmsSession";

    private static final Log LOG = LogFactory.getLog(OpenCmsAuthenticatedRequest.class);

    @Nullable
    private CmsUser user = null;
    private String guest = "guest";
    private CmsObject cmsobject;
    private CmsSessionInfo sesinfo;
    private List<Locale> availableLocales;

    public OpenCmsAuthenticatedRequest(@NotNull HttpServletRequest r, CmsObject cmsobject, CmsSessionInfo sesinfo, CmsUser user, String guest, List<Locale> availableLocales) {
        super(r);
        this.user = user;
        this.guest = guest;
        this.cmsobject = cmsobject;
        this.sesinfo = sesinfo;
        this.availableLocales = availableLocales;

        HttpSession session = r.getSession();
        if (session != null) {
            session.setAttribute(SESSION_CMSOBJECT, cmsobject);
            session.setAttribute(SESSION_SESSIONINFO, sesinfo);
        }

    }

    public String getRemoteUser() {
        String username = guest;
        if (user != null)
            username = user.getName();
        LOG.debug("OpenCms Remote User: " + username);
        return username;
    }

    // TODO
    public boolean isUserInRole(java.lang.String role) {
        return false;
    }

    @SuppressWarnings("UnusedDeclaration")
    public CmsObject getCmsobject() {
        return cmsobject;
    }

    @SuppressWarnings("UnusedDeclaration")
    public CmsSessionInfo getSesinfo() {
        return sesinfo;
    }

    @Override
    public Locale getLocale() {
        Enumeration locales = getLocales();
        if (locales.hasMoreElements()) {
            return (Locale) locales.nextElement();
        }
        return super.getLocale();
    }

    @Override
    public Enumeration getLocales() {
        CmsLocaleManager localeManager = OpenCms.getLocaleManager();
        List<Locale> defaultLocales = localeManager.getDefaultLocales();
        List<Locale> feasibleLocales = new ArrayList<Locale>();

        if (availableLocales != null) {
            @SuppressWarnings("unchecked") Enumeration<Locale> requestedLocales = super.getLocales();
            while (requestedLocales.hasMoreElements()) {
                Locale requestedLocale = requestedLocales.nextElement();
                if (availableLocales.contains(requestedLocale)) {
                    // direct match, prio 1
                    feasibleLocales.add(requestedLocale);
                }
            }

            // language comparison
            //noinspection unchecked
            requestedLocales = super.getLocales();
            while (requestedLocales.hasMoreElements()) {
                Locale requestedLocale = requestedLocales.nextElement();
                String requestedLanguage = requestedLocale.getLanguage();
                for (Locale availableLocale : availableLocales) {
                    if (availableLocale.getLanguage().equals(requestedLanguage)) {
                        feasibleLocales.add(availableLocale);
                    }
                }
            }
        }

        if (feasibleLocales.size() == 0) {
            feasibleLocales.addAll(defaultLocales);
        }

        List<Locale> onlyFirstEntry = feasibleLocales.subList(0, 1);
        return Collections.enumeration(onlyFirstEntry);
    }
}
