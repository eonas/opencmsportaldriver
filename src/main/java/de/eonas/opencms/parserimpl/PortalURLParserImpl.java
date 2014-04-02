package de.eonas.opencms.parserimpl;

import com.sun.xml.messaging.saaj.util.Base64;
import de.eonas.opencms.util.Encryption;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pluto.driver.url.PortalURL;
import org.apache.pluto.driver.url.PortalURLParameter;
import org.apache.pluto.driver.url.PortalURLParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;

public class PortalURLParserImpl implements PortalURLParser {
    public static final int MAX_STATE_LENGTH_RESSOURCE_REQUESTS = 50;
    public static final int MAX_STATE_LENGTH = 50;
    public static final String EHCACHE_PORTALURL_XML = "/ehcache-portalurl.xml";
    public static final String LINK_CACHE = "linkCache";
    public static final String STATE_CACHE = "stateCache";
    private static final String REVERSE_LINK_CACHE = "reverseLinkCache";


    private static final String PORTAL_PARAM = "p";

    private static Cache linkCache;
    private static Cache stateCache;
    private static CacheManager manager;
    private static Cache reverseLinkCache;

    @Nullable
    private static Encryption encryption = null;
    private static final PortalURLParser PARSER = new PortalURLParserImpl();
    private static final Log LOG = LogFactory.getLog(PortalURLParserImpl.class);
    private static SecureRandom sr;
    private static final boolean stateCacheEnable = false;


    static {
        try {
            sr = SecureRandom.getInstance("SHA1PRNG");
            encryption = new Encryption();
            URL url = PortalURLParserImpl.class.getResource(EHCACHE_PORTALURL_XML);
            if (url == null) {
                throw new IllegalArgumentException(EHCACHE_PORTALURL_XML + " is missing.");
            }
            manager = new CacheManager(url);
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    if ( manager != null ) {
                        manager.shutdown();
                    }
                }
            });

            manager.addCacheIfAbsent(LINK_CACHE);
            manager.addCacheIfAbsent(STATE_CACHE);
            manager.addCacheIfAbsent(REVERSE_LINK_CACHE);
            linkCache = manager.getCache(LINK_CACHE);
            stateCache = manager.getCache(STATE_CACHE);
            reverseLinkCache = manager.getCache(REVERSE_LINK_CACHE);
            if (stateCache == null) {
                throw new IllegalArgumentException("stateCache is missing.");
            }
            if (linkCache == null) {
                throw new IllegalArgumentException("linkCache is missing.");
            }
        } catch (Exception e) {
            LOG.error(e, e);
            e.printStackTrace();

        }
    }

    // Constructor -------------------------------------------------------------

    /**
     * Private constructor that prevents external instantiation.
     */
    private PortalURLParserImpl() {
        // Do nothing.
    }

    /**
     * Returns the singleton parser instance.
     *
     * @return the singleton parser instance.
     */
    @SuppressWarnings("UnusedDeclaration")
    @NotNull
    public static PortalURLParser getParser() {
        return PARSER;
    }

    // Public Methods ----------------------------------------------------------

    /**
     * Parse a servlet request to a portal URL.
     *
     * @param request the servlet request to parse.
     * @return the portal URL.
     */
    public PortalURL parse(@NotNull HttpServletRequest request) {
        // Construct portal URL using info retrieved from servlet request.
        String httpSessionId = "noSession";
        final HttpSession session = request.getSession();
        if (session != null) {
            httpSessionId = session.getId();
        }

        LOG.debug(httpSessionId + ": Parsing URL: " + request.getRequestURL());

        //String contextPath = request.getContextPath();
        String servletName = request.getServletPath();
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            servletName += pathInfo;
        }

        String urlBase = servletName; // request.getRequestURL().toString();

        RelativePortalURLImpl portalURL;

        String param = request.getParameter(PORTAL_PARAM);
        LOG.debug(httpSessionId + ": Parameter " + PORTAL_PARAM + ": " + param);

        boolean purgeCacheOnException = false;
        try {
            byte[] data;
            if (param != null && param.length() > 0) {
                Element cacheElement = linkCache.get(param);
                if (cacheElement != null) {
                    data = (byte[]) cacheElement.getObjectValue();
                    LOG.debug(httpSessionId + ": Fetched state from linkCache: " + data.length + " bytes.");
                } else {
                    data = encryption.decrypt(param);
                    if (data != null) {
                        LOG.debug(httpSessionId + ": Decrypt ok: " + data.length + " bytes.");
                    } else {
                        LOG.debug(httpSessionId + ": Decrypt returned null");
                    }
                }
            } else {
                @SuppressWarnings("UnusedAssignment") Element cacheElement = stateCache.get(httpSessionId);
                //noinspection PointlessBooleanExpression
                if (stateCacheEnable == true && cacheElement != null) {
                    data = (byte[]) cacheElement.getObjectValue();
                    purgeCacheOnException = true;
                    LOG.debug(httpSessionId + ": Fetched from stateCache: " + data.length + " bytes.");
                } else {
                    // we don't have any previous state
                    data = null;
                    LOG.debug(httpSessionId + ": No state present.");
                }
            }

            if (data != null) {
                LOG.debug(httpSessionId + ": Decoding state.");
                portalURL = deSerializePortalUrl(data);

                if (param != null && portalURL.getActionWindow() == null && portalURL.getResourceWindow() == null) {
                    LOG.debug(httpSessionId + ": Saving this state, discarding link linkCache.");
                    Element state = new Element(httpSessionId, data);
                    stateCache.put(state);
                }
            } else {
                portalURL = new RelativePortalURLImpl();
            }
        } catch (Exception ex) {
            LOG.warn(ex, ex);
            if (purgeCacheOnException) {
                stateCache.remove(httpSessionId);
            }
            portalURL = new RelativePortalURLImpl();
        }

        final String actionWindowId = portalURL.getActionWindow();
        if ( actionWindowId != null ) {
            // die parameter eines action-requests werden ohnehin URL-encoded übertragen
            // daher muss an dieser stelle gelöscht werden
            portalURL.clearParameters(actionWindowId);
        }

        portalURL.setTransients(urlBase, servletName, this, httpSessionId);
        if (LOG.isDebugEnabled()) {
            LOG.debug(httpSessionId + ": Decoded URL to " + portalURL.toDebugString());
        }
        return portalURL;
    }

    /**
     * Converts a portal URL to a URL string.
     *
     * @param portalURL the portal URL to convert.
     * @return a URL string representing the portal URL.
     */
    public String toString(@NotNull PortalURL portalURL) {
        StringBuilder fullBuffer = new StringBuilder();
        final RelativePortalURLImpl relativePortalUrl = (RelativePortalURLImpl) portalURL;
        final String httpSessionId = relativePortalUrl.getHttpSessionId();

        LOG.debug(httpSessionId + ": Encoding...." + relativePortalUrl.toDebugString());
        // Append the server URI and the servlet path.
        fullBuffer.append(relativePortalUrl.getUrlBase());

        int maxStateLength = MAX_STATE_LENGTH;
        if ( isResourceRequest ( portalURL )) {
            maxStateLength = MAX_STATE_LENGTH_RESSOURCE_REQUESTS;
        }

        String encoded = "";
        try {
            byte[] bytes = serializePortalURL(portalURL);

            if (bytes != null) {
                if (bytes.length > maxStateLength) {
                    String encodedBytes = de.eonas.opencms.util.Base64.encodeToString(bytes, false);
                    Element link = reverseLinkCache.get(encodedBytes);
                    if ( link == null ) {
                        String key = String.format("%08X", sr.nextLong());
                        link = new Element(key, bytes);
                        linkCache.put(link);
                        Element reverseLink = new Element(encodedBytes, key);
                        reverseLinkCache.put(reverseLink);
                        encoded = key;
                        LOG.debug(httpSessionId + ": Placed entry in session linkCache: " + key + " = " + bytes.length);
                    } else {
                        encoded = (String) link.getValue();
                    }
                } else {
                    final String base64CodedString = encryption.encrypt(bytes);
                    LOG.debug(httpSessionId + ": Base64 coded String: " + base64CodedString);
                    encoded = URLEncoder.encode(base64CodedString, "utf-8");
                    LOG.debug(httpSessionId + ": URLEncoded Base64 coded String: " + encoded);
                }
            }
        } catch (Exception ex) {
            LOG.warn(ex, ex);
        }

        boolean isFirst = true;
        if (encoded != null && encoded.length() > 0) {
            fullBuffer.append("?" + PORTAL_PARAM + "=").append(encoded);
            //noinspection UnusedAssignment
            isFirst = false;
        }

        // für pluto 1.1.7 müssen wir die render-parameter mit in die http-parameter aufnehmen. für pluto 2.0.3 nicht mehr notwendig
        // offenbar für die JSR286-Bridge von Liferay notwendig, leider, primär für renderrequests
        final String[] windows = { portalURL.getActionWindow(), portalURL.getResourceWindow() };
        for ( String window: windows ) {
            for (PortalURLParameter param : portalURL.getParameters()) {
                if (param.getWindowId().equals(window)) {
                    String key = param.getName();
                    for (String value : param.getValues()) {
                        if (isFirst) {
                            fullBuffer.append("?");
                            isFirst = false;
                        } else {
                            fullBuffer.append("&");
                        }
                        try {
                            fullBuffer.append(URLEncoder.encode(key, "utf-8"));
                            fullBuffer.append("=");
                            fullBuffer.append(URLEncoder.encode(value, "utf-8"));
                        } catch (UnsupportedEncodingException e) {
                            LOG.warn(String.format("Couldn't URLEncode %s/%s to utf-8", key, value), e);
                        }
                    }
                }
            }
        }

        LOG.debug(httpSessionId + ": Generated " + fullBuffer.toString());
        // Construct the string representing the portal URL.
        return fullBuffer.toString();
    }

    private boolean isResourceRequest(PortalURL portalURL) {
        return portalURL.getResourceWindow() != null;
    }

    @NotNull
    public RelativePortalURLImpl deSerializePortalUrl(byte[] data) throws IOException, ClassNotFoundException {
        String s = new String(data, "utf-8");
        RelativePortalURLImpl impl = new RelativePortalURLImpl();
        StringReader inReader = new StringReader(s);
        impl.readFromStream(inReader);
        return impl;
    }

    public byte[] serializePortalURL(PortalURL portalURL) throws IOException {
        StringWriter writer = new StringWriter();
        RelativePortalURLImpl impl = (RelativePortalURLImpl) portalURL;
        impl.writeToStream(writer);
        writer.close();
        return writer.toString().getBytes("utf-8");
    }

}
