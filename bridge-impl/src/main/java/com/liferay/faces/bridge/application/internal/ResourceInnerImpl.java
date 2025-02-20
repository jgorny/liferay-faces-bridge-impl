/**
 * Copyright (c) 2000-2021 Liferay, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.liferay.faces.bridge.application.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceWrapper;
import javax.faces.context.FacesContext;
import javax.portlet.faces.BridgeConfig;

import com.liferay.faces.bridge.internal.BridgeConfigAttributeMap;
import com.liferay.faces.bridge.util.internal.RequestMapUtil;
import com.liferay.faces.util.config.ConfiguredServletMapping;
import com.liferay.faces.util.logging.Logger;
import com.liferay.faces.util.logging.LoggerFactory;


/**
 * This class decorates the resource implementation from the JSF implementation.
 *
 * @author  Neil Griffin
 */
public class ResourceInnerImpl extends ResourceWrapper implements Serializable {

	// serialVersionUID
	private static final long serialVersionUID = 827821821511052062L;

	// Logger
	private static final Logger logger = LoggerFactory.getLogger(ResourceInnerImpl.class);

	// Private Constants
	private static final String HTTP_SPEC_DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";

	// Private Constants: Resources that can't be cached.
	private static final String EXTENSION_FACES = ".faces";
	private static final String LIBRARY_NAME_JAVAX_FACES = "javax.faces";
	private static final Set<String> NON_CACHED_RESOURCES;

	static {

		Set<String> nonCachedResources = new HashSet<String>();
		nonCachedResources.add("jsf.js");
		nonCachedResources.add("bridge.js");
		nonCachedResources.add("bridge.uncompressed.js");
		nonCachedResources.add("compat.js");
		nonCachedResources.add("compat.uncompressed.js");
		nonCachedResources.add("icefaces-compat.js");
		nonCachedResources.add("icefaces-compat.uncompressed.js");
		nonCachedResources.add("icepush.js");
		nonCachedResources.add("icepush.uncompressed.js");
		nonCachedResources.add("compat.js");
		nonCachedResources.add("icefaces-compat.js");
		NON_CACHED_RESOURCES = Collections.unmodifiableSet(nonCachedResources);
	}

	// Private Data Members
	private Long lastModifiedInSeconds;
	private Resource wrappedResource;

	/**
	 * This constructor is used by Mojarra via reflection during state saving.
	 */
	public ResourceInnerImpl() {
	}

	public ResourceInnerImpl(Resource wrappedResource) {
		this.wrappedResource = wrappedResource;
	}

	/**
	 * Since this method is not supplied by the {@link ResourceWrapper} class it has to be implemented here.
	 */
	@Override
	public String getContentType() {
		return wrappedResource.getContentType();
	}

	/**
	 * Since this method is not supplied by the {@link ResourceWrapper} class it has to be implemented here.
	 */
	@Override
	public String getLibraryName() {
		return wrappedResource.getLibraryName();
	}

	@Override
	public String getRequestPath() {

		// Get the requestPath value from the wrapped resource.
		String wrappedRequestPath = wrappedResource.getRequestPath();
		FacesContext facesContext = FacesContext.getCurrentInstance();

		// For each extension-mapped servlet-mapping found in web.xml, remove the extension from the wrapped requestPath
		// value. This is necessary because both Mojarra and MyFaces assume a servlet environment and automatically
		// append extension-mapped suffixes which have no meaning in a portlet environment.
		if (wrappedRequestPath != null) {

			if (wrappedRequestPath.contains(ResourceHandler.RESOURCE_IDENTIFIER)) {

				// If this resource request was initiated from a ResourceURL (not via the FacesServlet), then
				if (facesContext != null) {

					BridgeConfig bridgeConfig = RequestMapUtil.getBridgeConfig(facesContext);

					List<ConfiguredServletMapping> configuredFacesServletMappings = (List<ConfiguredServletMapping>)
						bridgeConfig.getAttributes().get(BridgeConfigAttributeMap.CONFIGURED_FACES_SERVLET_MAPPINGS);

					if (configuredFacesServletMappings != null) {

						for (ConfiguredServletMapping configuredServletMapping : configuredFacesServletMappings) {

							if (configuredServletMapping.isExtensionMapped()) {
								String extension = configuredServletMapping.getExtension();

								// Note: Both Mojarra and MyFaces construct a requestPath that looks something like
								// "/javax.faces.resource/jsf.js.faces?ln=javax.faces" and so we look for the
								// ".faces?" as an indicator that ".faces" needs to be removed from the requestPath.
								String token = extension + "?";
								int pos = wrappedRequestPath.indexOf(token);

								// If the servlet-mapping extension is found before the query string, then remove it
								// since this is an implicit Servlet-API dependency on the FacesServlet that has no
								// meaning in a portlet environment.
								if (pos > 0) {

									wrappedRequestPath = wrappedRequestPath.substring(0, pos) +
										wrappedRequestPath.substring(pos + extension.length());
									logger.debug("Removed extension=[{0}] from requestPath=[{1}]", extension,
										wrappedRequestPath);

									break;
								}

								// Otherwise, if the servlet-mapping extension is found at the end of the request
								// path, then
								else if (wrappedRequestPath.endsWith(extension)) {

									if (extension.equals(EXTENSION_FACES) &&
											wrappedRequestPath.endsWith(LIBRARY_NAME_JAVAX_FACES)) {

										// Special case: Don't remove ".faces" if request path ends with "javax.faces"
										// http://issues.liferay.com/browse/FACES-1202
									}
									else {

										// Sometimes resources like the ICEfaces bridge.js file don't have a library
										// name (ln=) parameter and simply look like this:
										// /my-portlet/javax.faces.resource/bridge.js.faces
										wrappedRequestPath = wrappedRequestPath.substring(0,
												wrappedRequestPath.lastIndexOf(extension));
										logger.debug("Removed extension=[{0}] from requestPath=[{1}]", extension,
											wrappedRequestPath);

										break;
									}
								}
							}
						}
					}
				}
			}

			// If the wrapped request path ends with "org.richfaces" then
			if (wrappedRequestPath.endsWith(ResourceRichFacesImpl.ORG_RICHFACES)) {

				// Check to see if the resource physically exists in the META-INF/resources/org.richfaces folder of the
				// RichFaces JAR. If it does, then this qualifies as a special case in which the
				// ResourceHandlerImpl#fixRichFacesImageURLs(FacesContext, String) method is unable to handle resources
				// such as "node_icon.gif" and the library name must be "org.richfaces.images" instead of
				// "org.richfaces".
				String resourcePath = "META-INF/resources/org.richfaces/" + getResourceName();
				URL resourceURL = getClass().getClassLoader().getResource(resourcePath);

				if (resourceURL != null) {
					wrappedRequestPath = wrappedRequestPath + ".images";
				}
			}
		}

		return wrappedRequestPath;
	}

	/**
	 * Since this method is not supplied by the {@link ResourceWrapper} class it has to be implemented here.
	 */
	@Override
	public String getResourceName() {
		return wrappedResource.getResourceName();
	}

	@Override
	public Resource getWrapped() {
		return wrappedResource;
	}

	/**
	 * Since this method is not supplied by the {@link ResourceWrapper} class it has to be implemented here.
	 */
	@Override
	public void setContentType(String contentType) {
		wrappedResource.setContentType(contentType);
	}

	/**
	 * Since this method is not supplied by the {@link ResourceWrapper} class it has to be implemented here.
	 */
	@Override
	public void setLibraryName(String libraryName) {
		wrappedResource.setLibraryName(libraryName);
	}

	/**
	 * Since this method is not supplied by the {@link ResourceWrapper} class it has to be implemented here.
	 */
	@Override
	public void setResourceName(String resourceName) {
		wrappedResource.setResourceName(resourceName);
	}

	/**
	 * Since this method is not supplied by the {@link ResourceWrapper} class it has to be implemented here.
	 */
	@Override
	public String toString() {
		return wrappedResource.toString();
	}

	/**
	 * This method determines whether or not the browser (user agent) requesting this resource needs an update, which
	 * can ultimately save bandwidth and be a big performance improvement.
	 */
	@Override
	public boolean userAgentNeedsUpdate(FacesContext facesContext) {

		String resourceName = getResourceName();

		// If the wrapped resource indicates that it needs to be updated, then go ahead and trust that and return true.
		boolean needsUpdate = wrappedResource.userAgentNeedsUpdate(facesContext);

		// Otherwise, don't trust what the wrapped resource says! There is a portlet-lifecycle incompatibility in
		// Mojarra's ResourceImpl.userAgentNeedsUpdate() such that the instance of the ResourceInfo is cached from when
		// the resource was first created in order to create the ResourceURL during the RenderRequest phase. Instead,
		// need to figure out for ourselves whether or not it needs to be updated.
		if (!needsUpdate) {

			// If it's a JavaScript resource that can't be cached (like ICEfaces JavaScript resources that will not
			// initialize properly if cached), then return true for needsUpdate so that the browser will re-retrieve
			// the resource.
			if (NON_CACHED_RESOURCES.contains(resourceName)) {

				needsUpdate = true;
			}

			// Otherwise,
			else {

				if (lastModifiedInSeconds == null) {
					URL url = wrappedResource.getURL();

					if (url != null) {
						InputStream inputStream = null;

						try {
							URLConnection urlConnection = url.openConnection();
							urlConnection.setUseCaches(false);
							urlConnection.connect();
							inputStream = urlConnection.getInputStream();

							long lastModifiedInMilliSeconds = urlConnection.getLastModified();
							lastModifiedInSeconds = (lastModifiedInMilliSeconds / 1000);
						}
						catch (IOException e) {
							lastModifiedInSeconds = 0L;
						}
						finally {

							if (inputStream != null) {

								try {
									inputStream.close();
								}
								catch (IOException e) {
									// ignore
								}
							}
						}
					}
					else {
						logger.warn(
							"Unable to determine if user agent needs update because resource URL was null for resourceName=[{0}].",
							resourceName);
					}
				}

				if (lastModifiedInSeconds != null) {

					long ifModifiedHeaderInSeconds = 0L;
					Map<String, String> requestHeaderMap = facesContext.getExternalContext().getRequestHeaderMap();

					// http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.25
					String requestHeaderValue = requestHeaderMap.get("If-Modified-Since");

					if (requestHeaderValue != null) {

						try {

							// Note that SimpleDateFormat is not thread-safe so an instance variable has to be used
							// instead of a static variable.
							// http://www.codefutures.com/weblog/andygrove/2007/10/simpledateformat-and-thread-safety.html
							SimpleDateFormat httpSpecDateFormat = new SimpleDateFormat(HTTP_SPEC_DATE_PATTERN,
									Locale.US);
							httpSpecDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

							long ifModifiedHeaderInMilliSeconds = httpSpecDateFormat.parse(requestHeaderValue)
								.getTime();
							ifModifiedHeaderInSeconds = (ifModifiedHeaderInMilliSeconds / 1000L);

							if (logger.isDebugEnabled()) {
								logger.debug(
									"resourceName=[{0}] requestHeaderValue=[{1}] ifModifiedHeaderInSeconds=[{2}]",
									resourceName, requestHeaderValue, Long.toString(ifModifiedHeaderInSeconds));
							}
						}
						catch (ParseException e) {
							logger.error("Unable to parse request-header=[{0}] value=[{1}]", "If-Modified-Since",
								requestHeaderValue);
						}
					}

					if (logger.isDebugEnabled()) {
						logger.debug("resourceName=[{0}] lastModified=[{1}] ifModifiedHeaderInSeconds=[{2}]",
							resourceName, Long.toString(lastModifiedInSeconds),
							Long.toString(ifModifiedHeaderInSeconds));
					}

					// FACES-62: Only compare seconds rather than milliseconds since LastModified header typically only
					// contains seconds. This will avoid unnecessary updates.
					needsUpdate = (lastModifiedInSeconds > ifModifiedHeaderInSeconds);
				}
			}
		}

		logger.debug("resourceName=[{0}] needsUpdate=[{1}]", resourceName, needsUpdate);

		return needsUpdate;
	}
}
