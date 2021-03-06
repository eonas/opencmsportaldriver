package de.eonas.opencms.authentication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsUser;
import org.opencms.i18n.CmsLocaleManager;
import org.opencms.main.CmsException;
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

    public OpenCmsAuthenticatedRequest(@NotNull HttpServletRequest r, CmsObject cmsobject, CmsSessionInfo sesinfo, @Nullable CmsUser user, String guest, List<Locale> availableLocales) {
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
        if (sesinfo == null) {
            // when logging in via ocee-webauth, the first getRemoteUser call fetches user info from the pre-login state (guest login)
            // so, for every guest login, we look into the request object to find out if a login happened in the meantime
            try {
                sesinfo = OpenCms.getSessionManager().getSessionInfo((HttpServletRequest) ((HttpServletRequestWrapper) getRequest()).getRequest());
                if (sesinfo != null) user = cmsobject.readUser(sesinfo.getUserId());
            } catch (CmsException e) {
                LOG.warn("Tried to upgrade OpenCms session from guest to user, but was not successful.", e);
            }
        }

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

    @NotNull
    @Override
    public Enumeration getLocales() {
        List<Locale> feasibleLocales = new ArrayList<Locale>();

        if (this.availableLocales == null || this.availableLocales.size() == 0) {
            CmsLocaleManager localeManager = OpenCms.getLocaleManager();
            this.availableLocales = localeManager.getAvailableLocales();
        }
        @SuppressWarnings("unchecked") Enumeration<Locale> requestedLocales = super.getLocales();
        while (requestedLocales.hasMoreElements()) {
            Locale requestedLocale = requestedLocales.nextElement();
            if (this.availableLocales.contains(requestedLocale)) {
                // direct full match, prio 1
                feasibleLocales.add(requestedLocale);
            }

            // language comparison
            String requestedLanguage = requestedLocale.getLanguage();
            for (Locale availableLocale : this.availableLocales) {
                if (availableLocale.getLanguage().equals(requestedLanguage)) {
                    // partial match, just the language ist equal
                    feasibleLocales.add(availableLocale);
                }
            }
        }

        if (feasibleLocales.size() == 0)
        {
            feasibleLocales.addAll(availableLocales);
        }

        List<Locale> onlyFirstEntry = feasibleLocales.subList(0, 1);
        return Collections.enumeration(onlyFirstEntry);
    }
}
