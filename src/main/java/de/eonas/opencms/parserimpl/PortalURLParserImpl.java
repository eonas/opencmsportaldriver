package de.eonas.opencms.parserimpl;

import de.eonas.opencms.util.Encryption;
import de.eonas.opencms.util.EncryptionException;
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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PortalURLParserImpl implements PortalURLParser {
    public static final int MAX_STATE_LENGTH_RESSOURCE_REQUESTS = 50;
    public static final int MAX_STATE_LENGTH = 50;
    public static final String LINK_CACHE = "linkCache";
    public static final String STATE_CACHE = "stateCache";
    public static final String REVERSE_LINK_CACHE = "reverseLinkCache";
    public static final String SHAREDLIBRARIES_PROPERTIES = "/sharedlibraries.properties";
    public static final String EHCACHE_PORTALURL_XML = "/ehcache-portalurl.xml";

    private static final String PORTAL_PARAM = "p";
    public static final String SHARED = "shared";

    public static final String OPENCMSPORTALDRIVER_SHARED_ENABLE = "opencmsportaldriver.shared.enable";

    @SuppressWarnings("FieldCanBeLocal")
    private final boolean stateCacheEnable = false;

    private Cache linkCache;
    private Cache stateCache;
    private CacheManager manager;
    private Cache reverseLinkCache;
    private List<Pattern> libraryRegExp;
    @NotNull
    private Encryption encryption;
    private SecureRandom sr;

    private static PortalURLParser PARSER;
    private static final Log LOG = LogFactory.getLog(PortalURLParserImpl.class);

    @NotNull
    private HashMap<String, RelativePortalURLImpl> sharedMapping = new HashMap<String, RelativePortalURLImpl>();


    static {
    }

    // Constructor -------------------------------------------------------------

    /**
     * Private constructor that prevents external instantiation.
     */
    private PortalURLParserImpl() throws NoSuchAlgorithmException, EncryptionException, IOException {
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
                if (manager != null) {
                    manager.shutdown();
                }
            }
        });

        manager.addCacheIfAbsent(LINK_CACHE);
        linkCache = manager.getCache(LINK_CACHE);
        if (linkCache == null) {
            throw new IllegalArgumentException("linkCache is missing.");
        }
        manager.addCacheIfAbsent(STATE_CACHE);
        stateCache = manager.getCache(STATE_CACHE);
        if (stateCache == null) {
            throw new IllegalArgumentException("stateCache is missing.");
        }
        manager.addCacheIfAbsent(REVERSE_LINK_CACHE);
        reverseLinkCache = manager.getCache(REVERSE_LINK_CACHE);
        if (reverseLinkCache == null) {
            throw new IllegalArgumentException("reverseLinkCache is missing.");
        }

        URL parameterProperties = PortalURLParserImpl.class.getResource(SHAREDLIBRARIES_PROPERTIES);
        if (parameterProperties == null) {
            throw new IllegalArgumentException(SHAREDLIBRARIES_PROPERTIES + " is missing.");
        }

        Properties parameter = new Properties();
        parameter.load(parameterProperties.openStream());
        String sharedLibRegExp = parameter.getProperty("shared", "");
        String[] expressions = sharedLibRegExp.split(",");
        libraryRegExp = new ArrayList<Pattern>();

        String sharedEnable = System.getProperty(OPENCMSPORTALDRIVER_SHARED_ENABLE);
        if ((sharedEnable != null) && Boolean.parseBoolean(sharedEnable)) {
            for (String expression : expressions) {
                Pattern compiledPattern = Pattern.compile(expression);
                libraryRegExp.add(compiledPattern);
            }
        }
    }

    /**
     * Returns the singleton parser instance.
     *
     * @return the singleton parser instance.
     */
    @SuppressWarnings("UnusedDeclaration")
    @NotNull
    public synchronized static PortalURLParser getParser() {
        try {
            if (PARSER == null) {
                PARSER = new PortalURLParserImpl();
            }
        } catch (Exception e) {
            LOG.error(e, e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return PARSER;
    }

    // Public Methods ----------------------------------------------------------

    /**
     * Parse a servlet request to a portal URL.
     *
     * @param request the servlet request to parse.
     * @return the portal URL.
     */
    @SuppressWarnings("ConstantConditions")
    public PortalURL parse(@NotNull HttpServletRequest request) {
        // Construct portal URL using info retrieved from servlet request.
        String httpSessionId = "noSession";
        final HttpSession session = request.getSession();
        if (session != null) {
            httpSessionId = session.getId();
        }

        LOG.debug(httpSessionId + ": Parsing URL: " + request.getRequestURL());

        String contextPath = request.getContextPath();
        String servletName = request.getServletPath();
        String pathInfo = request.getPathInfo();

        if (pathInfo != null) {
            servletName += pathInfo;
        }

        String param = request.getParameter(PORTAL_PARAM);

        return getPortalURL(httpSessionId, contextPath, servletName, param);
    }

    @SuppressWarnings("ConstantConditions")
    public PortalURL getPortalURL(String httpSessionId, String contextPath, @NotNull String url, String param) {
        if (encryption == null) {
            throw new IllegalStateException("Encryption is null");
        }

        RelativePortalURLImpl portalURL;
        LOG.debug(httpSessionId + ": Parameter " + PORTAL_PARAM + ": " + param);

        String sharedContextPath = contextPath + "/" + PortalURLParserImpl.SHARED + "/";
        if (url.startsWith(sharedContextPath)) {
            String paramAndPathInfo = url.substring(sharedContextPath.length());
            int pos = paramAndPathInfo.indexOf('/');
            if (pos != -1) {
                paramAndPathInfo = paramAndPathInfo.substring(0, pos);
            }
            pos = paramAndPathInfo.indexOf('?');
            if (pos != -1) {
                paramAndPathInfo = paramAndPathInfo.substring(0, pos);
            }
            param = paramAndPathInfo;
        }

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
                //noinspection PointlessBooleanExpression,ConstantConditions
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

            portalURL.setTransients(url, url, this, httpSessionId, contextPath);

            if (isSharedResource(portalURL)) {
                RelativePortalURLImpl newPortalUrl = reverseMapSharedResource(portalURL);
                if (newPortalUrl == null) {
                    throw new IllegalStateException("Reverse map for portal url failed: " + portalURL);
                }
                portalURL = newPortalUrl;
            }
        } catch (Exception ex) {
            LOG.warn(ex, ex);
            if (purgeCacheOnException) {
                stateCache.remove(httpSessionId);
            }
            portalURL = new RelativePortalURLImpl();
        }


        final String actionWindowId = portalURL.getActionWindow();
        if (actionWindowId != null) {
            // die parameter eines action-requests werden ohnehin URL-encoded übertragen
            // daher muss an dieser stelle gelöscht werden
            portalURL.clearParameters(actionWindowId);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(httpSessionId + ": Decoded URL to " + portalURL.toDebugString());
        }
        return portalURL;
    }

    /**
     * Converts a portal URL to a URL string.
     *
     * @param inPortalURL the portal URL to convert.
     * @return a URL string representing the portal URL.
     */
    @NotNull
    public String toString(@NotNull PortalURL inPortalURL) {
        RelativePortalURLImpl portalUrl = (RelativePortalURLImpl) inPortalURL;
        boolean isShared = false;

        StringBuilder fullBuffer = new StringBuilder();
        final String httpSessionId = portalUrl.getHttpSessionId();
        String encoded = "";
        try {

            if (shouldBeShared(portalUrl)) {
                portalUrl = rewriteToSharedAndStore(portalUrl);
                isShared = true;
            }

            fullBuffer.append(portalUrl.getUrlBase());

            LOG.debug(httpSessionId + ": Encoding...." + portalUrl.toDebugString());
            // Append the server URI and the servlet path.

            int maxStateLength = MAX_STATE_LENGTH;
            if (isResourceRequest(portalUrl)) {
                maxStateLength = MAX_STATE_LENGTH_RESSOURCE_REQUESTS;
            }

            String codedPortalUrl = serializePortalURL(portalUrl);
            byte[] bytes = codedPortalUrl.getBytes("utf-8");

            if (bytes.length > maxStateLength) {
                String encodedBytes = de.eonas.opencms.util.Base64.encodeToString(bytes, false);
                Element link = reverseLinkCache.get(encodedBytes);
                if (link == null) {
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
        } catch (Exception ex) {
            LOG.warn(ex, ex);
        }

        boolean isFirst = true;
        if (isShared) {
            fullBuffer.append("/").append(encoded);
        } else {
            if (encoded != null && encoded.length() > 0) {
                fullBuffer.append("?" + PORTAL_PARAM + "=").append(encoded);
                //noinspection UnusedAssignment
                isFirst = false;
            }
        }

        // für pluto 1.1.7 müssen wir die render-parameter mit in die http-parameter aufnehmen. für pluto 2.0.3 nicht mehr notwendig
        // offenbar für die JSR286-Bridge von Liferay notwendig, leider, primär für renderrequests
        final String[] windows = {portalUrl.getActionWindow(), portalUrl.getResourceWindow()};
        for (String window : windows) {
            for (PortalURLParameter param : portalUrl.getParameters()) {
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

    private RelativePortalURLImpl reverseMapSharedResource(RelativePortalURLImpl portalURL) throws IOException {
        String codedPortalUrl = serializePortalURL(portalURL);
        return sharedMapping.get(codedPortalUrl);
    }

    private boolean isSharedResource(@NotNull RelativePortalURLImpl portalURL) {
        return SHARED.equals(portalURL.getResourceWindow());
    }

    @NotNull
    private RelativePortalURLImpl rewriteToSharedAndStore(@NotNull RelativePortalURLImpl portalURL) throws IOException {
        RelativePortalURLImpl clone = (RelativePortalURLImpl) portalURL.clone();
        clone.convertToSharedResource();
        /* store the mapping somewhere */
        String url = serializePortalURL(clone);
        sharedMapping.put(url, portalURL);
        return clone;
    }

    private boolean shouldBeShared(@NotNull RelativePortalURLImpl portalURL) {
        if (!isResourceRequest(portalURL)) {
            return false;
        }

        if (isSharedResource(portalURL)) {
            // to prevent double sharing
            return false;
        }

        String libraryName = null;
        String resourceName = null;

        Collection<PortalURLParameter> parameters = portalURL.getParameters();
        for (PortalURLParameter parameter : parameters) {
            String name = parameter.getName();
            if ("ln".equals(name)) {
                libraryName = getValue(parameter);
            }
            if ("javax.faces.resource".equals(name)) {
                resourceName = getValue(parameter);
            }
        }

        if (resourceName != null && libraryName != null) {
            for (Pattern pattern : libraryRegExp) {
                Matcher matcher = pattern.matcher(libraryName);
                if (matcher.matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private String getValue(@Nullable PortalURLParameter parameter) {
        if (parameter == null) return null;
        String[] values = parameter.getValues();
        if (values != null && values.length > 0) {
            return values[0];
        }
        return null;
    }

    private boolean isResourceRequest(@NotNull PortalURL portalURL) {
        return portalURL.getResourceWindow() != null;
    }

    @NotNull
    public RelativePortalURLImpl deSerializePortalUrl(@NotNull byte[] data) throws IOException, ClassNotFoundException {
        String s = new String(data, "utf-8");
        RelativePortalURLImpl impl = new RelativePortalURLImpl();
        StringReader inReader = new StringReader(s);
        impl.readFromStream(inReader);
        return impl;
    }

    public String serializePortalURL(PortalURL portalURL) throws IOException {
        StringWriter writer = new StringWriter();
        RelativePortalURLImpl impl = (RelativePortalURLImpl) portalURL;
        impl.writeToStream(writer);
        writer.close();
        return writer.toString();
    }

}
